package de.rwth.i9.palm.interestmining.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.helper.comparator.AuthorInterestByDateComparator;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorInterest;
import de.rwth.i9.palm.model.AuthorInterestProfile;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.InterestProfile;
import de.rwth.i9.palm.model.InterestProfileType;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.ApplicationService;

@Service
public class InterestMiningService
{

	final Logger logger = Logger.getLogger( InterestMiningService.class );

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private ApplicationService applicationService;

	@Autowired
	private CValueInterestProfileOld cValueInterestProfileOld;

	@Autowired
	private CValueInterestProfile cValueInterestProfile;

	@Autowired
	private CorePhraseInterestProfile corePhraseInterestProfile;

	@Autowired
	private WordFreqInterestProfile wordFreqInterestProfile;

	/**
	 * Get author interest from active author profiles
	 * 
	 * @param responseMap
	 * @param author
	 * @param updateAuthorInterest
	 * @return
	 * @throws ParseException
	 */
	public Map<String, Object> getInterestFromAuthor( Map<String, Object> responseMap, Author author, boolean updateAuthorInterest ) throws ParseException
	{
		logger.info( "start mining interest " );
		// get default interest profile
		List<InterestProfile> interestProfilesDefault = persistenceStrategy.getInterestProfileDAO().getAllActiveInterestProfile( InterestProfileType.DEFAULT );

		// get default interest profile
		List<InterestProfile> interestProfilesDerived = persistenceStrategy.getInterestProfileDAO().getAllActiveInterestProfile( InterestProfileType.DERIVED );

		if ( interestProfilesDefault.isEmpty() && interestProfilesDerived.isEmpty() )
		{
			logger.warn( "No active interest profile found" );
			return responseMap;
		}

		if ( author.getPublications() == null || author.getPublications().isEmpty() )
		{
			logger.warn( "No publication found" );
			return responseMap;
		}

		// update for all author interest profile
		if ( !updateAuthorInterest )
		{
			// get interest profile from author
			Set<AuthorInterestProfile> authorInterestProfiles = author.getAuthorInterestProfiles();
			if ( authorInterestProfiles != null && !authorInterestProfiles.isEmpty() )
			{
				// check for missing default interest profile in author
				// only calculate missing one
				for ( Iterator<InterestProfile> interestProfileIterator = interestProfilesDefault.iterator(); interestProfileIterator.hasNext(); )
				{
					InterestProfile interestProfileDefault = interestProfileIterator.next();
					for ( AuthorInterestProfile authorInterestProfile : authorInterestProfiles )
					{
						if ( authorInterestProfile.getInterestProfile() != null && authorInterestProfile.getInterestProfile().equals( interestProfileDefault ) )
						{
							interestProfileIterator.remove();
							break;
						}
					}
				}

				// check for missing derivative interest profile
				for ( Iterator<InterestProfile> interestProfileIterator = interestProfilesDerived.iterator(); interestProfileIterator.hasNext(); )
				{
					InterestProfile interestProfileDerived = interestProfileIterator.next();
					for ( AuthorInterestProfile authorInterestProfile : authorInterestProfiles )
					{
						if ( authorInterestProfile.getInterestProfile().equals( interestProfileDerived ) )
						{
							interestProfileIterator.remove();
							break;
						}
					}
				}
			}
		}

		// if defaultInterestProfile not null,
		// means interest calculation from beginning is needed
		if ( !interestProfilesDefault.isEmpty() )
		{
			// first create publication cluster
			// prepare the cluster container
			Map<String, PublicationClusterHelper> publicationClustersMap = new HashMap<String, PublicationClusterHelper>();
			// construct the cluster
			logger.info( "Construct publication cluster " );
			constructPublicationClusterByLanguageAndYear( author, publicationClustersMap );

			// cluster is ready
			if ( !publicationClustersMap.isEmpty() )
			{
				// calculate default interest profile
				calculateInterestProfilesDefault( author, publicationClustersMap, interestProfilesDefault );
			}
		}

		// get and put author interest profile into map or list
		getInterestFromDatabase( author, responseMap );

		return responseMap;
	}

	public void calculateInterestProfilesDefault( Author author, Map<String, PublicationClusterHelper> publicationClustersMap, List<InterestProfile> interestProfilesDefault )
	{
		// calculate frequencies of term in cluster
		for ( Map.Entry<String, PublicationClusterHelper> publicationClusterEntry : publicationClustersMap.entrySet() )
			publicationClusterEntry.getValue().calculateTermProperties();

		// loop through all interest profiles default
		for ( InterestProfile interestProfileDefault : interestProfilesDefault )
			calculateEachInterestProfileDefault( author, interestProfileDefault, publicationClustersMap );
	}

	public void calculateEachInterestProfileDefault( Author author, InterestProfile interestProfileDefault, Map<String, PublicationClusterHelper> publicationClustersMap )
	{
		// get author interest profile
		Calendar calendar = Calendar.getInstance();
		// default profile name [USERID]+[DEFAULT_PROFILENAME]
		String authorInterestProfileName = author.getId() + "+" + interestProfileDefault.getName();

		// create new author interest profile for c-value
		AuthorInterestProfile authorInterestProfile = new AuthorInterestProfile();
		authorInterestProfile.setCreated( calendar.getTime() );
		authorInterestProfile.setDescription( "Interest mining using " + interestProfileDefault.getName() + " algorithm" );
		authorInterestProfile.setName( authorInterestProfileName );
		
		// CorePhrase and WordFreq specific, according to Svetoslav Evtimov thesis
		// yearFactor Map format Map< Language-Year , value >
		// totalYearsFactor Map< Language, value >
		
		Map<String, Double> yearFactorMap = new HashMap<String, Double>();
		Map<String, Double> totalYearsFactorMap = new HashMap<String, Double>();
		
		// calculate some weighting factors
		if ( interestProfileDefault.getName().toLowerCase().equals( "corephrase" ) ||
				interestProfileDefault.getName().toLowerCase().equals( "wordfreq" )	)
		{
			yearFactorMap = CorePhraseAndWordFreqHelper.calculateYearFactor( publicationClustersMap, 0.25 );
			totalYearsFactorMap = CorePhraseAndWordFreqHelper.calculateTotalYearsFactor( publicationClustersMap );
		}

		// get the number of active extraction services
		int numberOfExtractionService = applicationService.getExtractionServices().size();

		// loop to each cluster and calculate default profiles
		for ( Map.Entry<String, PublicationClusterHelper> publicationClusterEntry : publicationClustersMap.entrySet() )
		{
			PublicationClusterHelper publicationCluster = publicationClusterEntry.getValue();

			if ( publicationCluster.getTermMap() == null || publicationCluster.getTermMap().isEmpty() )
				continue;

			// prepare variables
			AuthorInterest authorInterest = new AuthorInterest();

			// assign author interest method
			if ( interestProfileDefault.getName().toLowerCase().equals( "cvalue" ) )
			{
				cValueInterestProfile.doCValueCalculation( authorInterest, publicationCluster, numberOfExtractionService );
			}
			else if ( interestProfileDefault.getName().toLowerCase().equals( "corephrase" ) )
			{
				Double yearFactor = yearFactorMap.get( publicationCluster.getLanguage() + publicationCluster.getYear() );
				Double totalYearFactor = totalYearsFactorMap.get( publicationCluster.getLanguage() );
				corePhraseInterestProfile.doCorePhraseCalculation( authorInterest, publicationCluster, yearFactor, totalYearFactor, numberOfExtractionService );
			}
			else if ( interestProfileDefault.getName().toLowerCase().equals( "wordfreq" ) )
			{
				Double yearFactor = yearFactorMap.get( publicationCluster.getLanguage() + publicationCluster.getYear() );
				Double totalYearFactor = totalYearsFactorMap.get( publicationCluster.getLanguage() );
				wordFreqInterestProfile.doWordFreqCalculation( authorInterest, publicationCluster, yearFactor, totalYearFactor, numberOfExtractionService );
			}
			// Put other default interest profiles

			// check author interest calculation result
			if ( authorInterest.getTermWeights() != null && !authorInterest.getTermWeights().isEmpty() )
			{
				authorInterest.setAuthorInterestProfile( authorInterestProfile );
				authorInterestProfile.addAuthorInterest( authorInterest );
				authorInterestProfile.setInterestProfile( interestProfileDefault );
			}
		}

		// at the end persist
		if ( authorInterestProfile.getAuthorInterests() != null || !authorInterestProfile.getAuthorInterests().isEmpty() )
		{
			authorInterestProfile.setAuthor( author );
			author.addAuthorInterestProfiles( authorInterestProfile );
			persistenceStrategy.getAuthorDAO().persist( author );
		}
	}

	public void constructPublicationClusterByLanguageAndYear( Author author, Map<String, PublicationClusterHelper> publicationClustersMap )
	{
		// fill publication clusters
		// prepare calendar for publication year
		Calendar calendar = Calendar.getInstance();
		// get all publications from specific author and put it into cluster
		for ( Publication publication : author.getPublications() )
		{
			// only proceed publication that have date, language and abstract
			if ( publication.getAbstractText() == null || publication.getAbstractText().equals( "" ) )
				continue;
			if ( publication.getPublicationDate() == null )
				continue;
			if ( publication.getLanguage() == null )
				continue;

			// get publication year
			calendar.setTime( publication.getPublicationDate() );

			// construct clusterMap key
			String clusterMapKey = publication.getLanguage() + calendar.get( Calendar.YEAR );

			// construct publication map
			if ( publicationClustersMap.get( clusterMapKey ) == null )
			{
				// not exist create new cluster
				PublicationClusterHelper publicationCluster = new PublicationClusterHelper();
				publicationCluster.setLangauge( publication.getLanguage() );
				publicationCluster.setYear( calendar.get( Calendar.YEAR ) );
				publicationCluster.addPublicationAndUpdate( publication );

				// add into map
				publicationClustersMap.put( clusterMapKey, publicationCluster );

			}
			else
			{
				// exist on map, get the cluster
				PublicationClusterHelper publicationCluster = publicationClustersMap.get( clusterMapKey );
				publicationCluster.addPublicationAndUpdate( publication );
			}

		}
	}


	/**
	 * Collect the author interest result as JSON object
	 * 
	 * @param author
	 * @param responseMap
	 * @return
	 */
	private Map<String, Object> getInterestFromDatabase( Author author, Map<String, Object> responseMap )
	{
		Set<AuthorInterestProfile> authorInterestProfiles = author.getAuthorInterestProfiles();

		// the whole result related to interest
		List<Object> authorInterestResult = new ArrayList<Object>();

		for ( AuthorInterestProfile authorInterestProfile : authorInterestProfiles )
		{
			// put profile on map
			Map<String, Object> authorInterestResultProfilesMap = new HashMap<String, Object>();

			// get interest profile name and description
			String interestProfileName = authorInterestProfile.getName().substring( author.getId().length() + 1 );
			String interestProfileDescription = authorInterestProfile.getName().substring( author.getId().length() );

			// get authorInterest set on profile
			Set<AuthorInterest> authorInterests = authorInterestProfile.getAuthorInterests();

			// if profile contain no authorInterest just skip
			if ( authorInterests == null || authorInterests.isEmpty() )
				continue;

			// a map for storing authorInterst based on language
			Map<String, List<AuthorInterest>> authorInterestLanguageMap = new HashMap<String, List<AuthorInterest>>();

			// split authorinterest based on language and put it on the map
			for ( AuthorInterest authorInterest : authorInterests )
			{
				if ( authorInterestLanguageMap.get( authorInterest.getLanguage() ) != null )
				{
					authorInterestLanguageMap.get( authorInterest.getLanguage() ).add( authorInterest );
				}
				else
				{
					List<AuthorInterest> authorInterestList = new ArrayList<AuthorInterest>();
					authorInterestList.add( authorInterest );
					authorInterestLanguageMap.put( authorInterest.getLanguage(), authorInterestList );
				}
			}

			// prepare calendar for extractind year from date
			Calendar calendar = Calendar.getInstance();

			// result author interest based on language
			List<Object> authorInterestResultLanguageList = new ArrayList<Object>();

			// sort authorinterest based on year
			for ( Map.Entry<String, List<AuthorInterest>> authorInterestLanguageIterator : authorInterestLanguageMap.entrySet() )
			{
				// result container
				Map<String, Object> authorInterestResultLanguageMap = new LinkedHashMap<String, Object>();
				// hashmap value
				String interestLanguage = authorInterestLanguageIterator.getKey();
				List<AuthorInterest> interestList = authorInterestLanguageIterator.getValue();

				// sort based on year
				Collections.sort( interestList, new AuthorInterestByDateComparator() );

				// term values based on year result container
				List<Object> authorInterestResultYearList = new ArrayList<Object>();

				// get interest year, term and value
				for ( AuthorInterest authorInterest : interestList )
				{
					if ( authorInterest.getTermWeights() == null || authorInterest.getTermWeights().isEmpty() )
						continue;

					// result container
					Map<String, Object> authorInterestResultYearMap = new LinkedHashMap<String, Object>();

					// get year
					calendar.setTime( authorInterest.getYear() );
					String year = Integer.toString( calendar.get( Calendar.YEAR ) );

					List<Object> termValueResult = new ArrayList<Object>();

					// put term and value
					for ( Map.Entry<Interest, Double> termWeightMap : authorInterest.getTermWeights().entrySet() )
					{
						List<Object> termWeightObjects = new ArrayList<Object>();
						termWeightObjects.add( termWeightMap.getKey().getId() );
						termWeightObjects.add( termWeightMap.getKey().getTerm() );
						termWeightObjects.add( termWeightMap.getValue() );
						termValueResult.add( termWeightObjects );
					}
					authorInterestResultYearMap.put( "year", year );
					authorInterestResultYearMap.put( "termvalue", termValueResult );
					authorInterestResultYearList.add( authorInterestResultYearMap );
				}

				authorInterestResultLanguageMap.put( "language", interestLanguage );
				authorInterestResultLanguageMap.put( "interestyears", authorInterestResultYearList );
				if ( interestLanguage.equals( "english" ) )
					authorInterestResultLanguageList.add( 0, authorInterestResultLanguageMap );
				else
					authorInterestResultLanguageList.add( authorInterestResultLanguageMap );
			}

			// put profile map
			authorInterestResultProfilesMap.put( "profile", interestProfileName );
			authorInterestResultProfilesMap.put( "description", interestProfileDescription );
			authorInterestResultProfilesMap.put( "interestlanguages", authorInterestResultLanguageList );
			authorInterestResult.add( authorInterestResultProfilesMap );
		}

		responseMap.put( "interest", authorInterestResult );

		// put also publication

		return responseMap;
	}

}

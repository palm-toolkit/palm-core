package de.rwth.i9.palm.interestmining.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tartarus.snowball.ext.PorterStemmer;

import de.rwth.i9.palm.helper.comparator.AuthorInterestByDateComparator;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorInterest;
import de.rwth.i9.palm.model.AuthorInterestProfile;
import de.rwth.i9.palm.model.ExtractionServiceType;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.InterestProfile;
import de.rwth.i9.palm.model.InterestProfileType;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationTopic;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.utils.Inflector;

@Service
public class InterestMiningService
{

	final Logger logger = Logger.getLogger( InterestMiningService.class );

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private CValueInterestProfileOld cValueInterestProfileOld;

	@Autowired
	private CValueInterestProfile cValueInterestProfile;

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

		for ( Map.Entry<String, PublicationClusterHelper> publicationClusterEntry : publicationClustersMap.entrySet() )
		{
			PublicationClusterHelper publicationCluster = publicationClusterEntry.getValue();

			if ( publicationCluster.getTermMap() == null || publicationCluster.getTermMap().isEmpty() )
				continue;

			// prepare variables
			AuthorInterest authorInterest = new AuthorInterest();

			if ( interestProfileDefault.getName().toLowerCase().equals( "cvalue" ) )
			{
				// assign author interest here
				cValueInterestProfile.doCValueCalculation( authorInterest, publicationCluster );
			}
			// TODO other interest profiles

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


	/* OLD IMPLEMENTATION */
	public Map<String, Object> getInterestFromAuthor( Author author, boolean updateAuthorInterest, Map<String, Object> responseMap ) throws ParseException
	{

		logger.info( "start mining interest " );
		// get all active authorinterestprofiles
		List<AuthorInterestProfile> authorInterestProfiles = persistenceStrategy.getAuthorInterestProfileDAO().getDefaultAuthorInterestProfile();
		
		// interest profile is needed before interests are calculated
		if( !authorInterestProfiles.isEmpty() ){
			if ( (author.getAuthorInterestProfiles() == null && author.getAuthorInterestProfiles().isEmpty())  ||
				 author.getAuthorInterestProfiles().size() != authorInterestProfiles.size() || updateAuthorInterest)
			{
				calculateAuthorInterestBasedOnActiveInterestProfile( author, authorInterestProfiles);
			}
		}
		else
		{
			logger.info( "Something went wrong, author interest profile is empty" );
			return Collections.emptyMap();
		}
		// get the author interest on json format (Map)
		getInterestFromDatabase( author, responseMap );
		
		return responseMap;
	}

	/**
	 * COllect the author interest result as JSON object
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
			String interestProfileName = authorInterestProfile.getName().substring( author.getId().length() );
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
	
	/**
	 * Calculate the author interests based on active interest profile
	 * 
	 * @param author
	 * @throws ParseException
	 */
	private void calculateAuthorInterestBasedOnActiveInterestProfile( Author author, List<AuthorInterestProfile> authorInterestProfiles ) throws ParseException
	{
		// calculate the author interest
		// first create clusters of publication based on year

		// Map of the entire corpus ( publication information )
		// based on language and year
		Map<String, Map<Integer, String>> languageYearCorpusMap = new HashMap<String, Map<Integer, String>>();

		// First, Categorize publication based on language and year,
		// then get publication topic from publication
		// and get unique interest from publication topic
		// put it into a Map<Language, Map< Year, Set <Interest>>>
		Map<String, Map<Integer, Set<Interest>>> languageYearInterestMap = constructInterestMapByLanguageAndYear( author, languageYearCorpusMap );

		// Second, count occurrence between unique term and entire corpus
		// based on language and year
		Map<String, Map<Integer, Map<Interest, Integer>>> languageYearInterestOccurrenceMap = countInterestOccurrenceInCorpus( languageYearInterestMap, languageYearCorpusMap );

		// Third, calculate interests based on active profile
		if( !authorInterestProfiles.isEmpty() ){
			for( AuthorInterestProfile authorInterestProfile : authorInterestProfiles ){
				// c-value profile
				if( authorInterestProfile.getName().equals( "cvalue" )){
					// since persistenceStrategy is a singleton object,
					// and hibernate still tracking the
					cValueInterestProfileOld.doCValueCalculation( languageYearInterestOccurrenceMap, author );
				} else if( authorInterestProfile.getName().equals( "corephrase" )){
					
				} else if( authorInterestProfile.getName().equals( "wordfreq" )){
					
				}
				// ADD OTHER PROFILES HERE
			}
		}

	}

	/**
	 * Count the occurrence of unique terms/topic in corpus
	 * 
	 * @param languageYearInterestMap
	 * @param languageYearCorpusMap
	 * @return
	 */
	private Map<String, Map<Integer, Map<Interest, Integer>>> countInterestOccurrenceInCorpus( Map<String, Map<Integer, Set<Interest>>> languageYearInterestMap, Map<String, Map<Integer, String>> languageYearCorpusMap )
	{
		Map<String, Map<Integer, Map<Interest, Integer>>> languageYearInterestOccurrenceOnCorpusMap = new HashMap<String, Map<Integer, Map<Interest, Integer>>>();

		// iterate based on language
		for ( Entry<String, Map<Integer, Set<Interest>>> languageYearInterestMapEntry : languageYearInterestMap.entrySet() )
		{
			String languageKey = languageYearInterestMapEntry.getKey();
			Map<Integer, String> yearCorpusMap = languageYearCorpusMap.get( languageKey );

			// target map
			Map<Integer, Map<Interest, Integer>> yearInterestOccurrenceOnCorpusMap = new HashMap<Integer, Map<Interest, Integer>>();
			languageYearInterestOccurrenceOnCorpusMap.put( languageKey, yearInterestOccurrenceOnCorpusMap );

			// iterate based on year
			for ( Entry<Integer, Set<Interest>> yearInterestMapEntry : languageYearInterestMapEntry.getValue().entrySet() )
			{
				int yearKey = yearInterestMapEntry.getKey();
				Set<Interest> interests = yearInterestMapEntry.getValue();
				String corpus = yearCorpusMap.get( yearKey );

				// target map
				Map<Interest, Integer> interestOccurrenceOnCorpusMap = new HashMap<Interest, Integer>();
				yearInterestOccurrenceOnCorpusMap.put( yearKey, interestOccurrenceOnCorpusMap );

				// iterate base on interest
				for ( Interest interest : interests )
				{
					String term = interest.getTerm();
					if ( ( term.split( " " ) ).length == 1 ) // contain single
																// word
						term = " " + term + " "; // give white spaces

					// count frequencies unique term in a year occurred
					interestOccurrenceOnCorpusMap.put( interest, StringUtils.countMatches( corpus, term ) );
				}
			}

		}
		return languageYearInterestOccurrenceOnCorpusMap;
	}

	/**
	 * Categorize publication on specific author based on language and year
	 * 
	 * @param author
	 * @return
	 */
	private Map<String, Map<Integer, Set<Interest>>> constructInterestMapByLanguageAndYear( Author author, Map<String, Map<Integer, String>> languageYearCorpusMap )
	{
		Calendar calendar = Calendar.getInstance();
		Map<String, Map<Integer, Set<Interest>>> languageYearInterestMap = new HashMap<String, Map<Integer, Set<Interest>>>();

		for ( Publication publication : author.getPublications() )
		{
			if ( publication.getPublicationTopics() != null && publication.getLanguage() != null && publication.getPublicationDate() != null )
			{
				// check whether map with specific language already exist
				if ( languageYearInterestMap.get( publication.getLanguage() ) != null )
				{
					String language = publication.getLanguage();

					Map<Integer, Set<Interest>> yearInterestMap = languageYearInterestMap.get( publication.getLanguage() );
					Map<Integer, String> yearCorpusMap = languageYearCorpusMap.get( publication.getLanguage() );
					// check if on map language already exist map based on year
					calendar.setTime( publication.getPublicationDate() );
					if ( yearInterestMap.get( calendar.get( Calendar.YEAR ) ) != null )
					{
						/* For the unique terms map */
						// map year exist, then get the set and add publication
						Set<Interest> interestSets = yearInterestMap.get( calendar.get( Calendar.YEAR ) );
						// merge 2 set
						interestSets.addAll( constructInterestFromPublication( publication, language ) );

						/* For the corpus map */
						yearCorpusMap.put( calendar.get( Calendar.YEAR ), yearCorpusMap.get( calendar.get( Calendar.YEAR ) ) + " " + normalizeText( getPublicationText( publication ), language ) );
					}
					else
					{
						/* For the unique terms map */
						// map year not exist, then create new one
						// added interest into set
						Set<Interest> interestSets = constructInterestFromPublication( publication, language );
						// put into new map
						yearInterestMap.put( calendar.get( Calendar.YEAR ), interestSets );

						/* For the corpus map */
						yearCorpusMap.put( calendar.get( Calendar.YEAR ), normalizeText( getPublicationText( publication ), language ) );
					}
				}
				else
				{
					String language = publication.getLanguage();
					calendar.setTime( publication.getPublicationDate() );
					/* For the unique terms map */
					// added interests into set
					Set<Interest> interestSets = constructInterestFromPublication( publication, language );
					// put publication set to map based on year
					Map<Integer, Set<Interest>> yearInterestMap = new HashMap<Integer, Set<Interest>>();
					yearInterestMap.put( calendar.get( Calendar.YEAR ), interestSets );
					// put publication year map into language map
					languageYearInterestMap.put( publication.getLanguage(), yearInterestMap );

					/* For the corpus map */
					String publicationInformation = normalizeText( getPublicationText( publication ), language );
					// put corpus map into a year map
					Map<Integer, String> yearCorpusMap = new HashMap<Integer, String>();
					yearCorpusMap.put( calendar.get( Calendar.YEAR ), publicationInformation );
					// put corpus year map into language map
					languageYearCorpusMap.put( language, yearCorpusMap );
				}
			}
		}
		return languageYearInterestMap;
	}

	/**
	 * Construct set of interest based on publication topics
	 * @param publication
	 * @return
	 */
	private Set<Interest> constructInterestFromPublication( Publication publication, String language )
	{
		Map<String, Integer> uniqueTermAndOccurence = new HashMap<String, Integer>();
		Set<Interest> interestSets = new HashSet<Interest>();
		
		// threshold times a term appears from different publication topic extraction
		// e.g. term "learning analytics" appears on term extraction from AlchemyAPI,
		// and YCA, but not in TextWise. The term "learning analytics" thus appears
		// 2 times and will be consider as important term, since the threshold is 2,
		// from number of topic extraction ( 3 ) - 1
		int interestOccurenceThreshold = publication.getPublicationTopics().size();
		if( interestOccurenceThreshold > 2)
			interestOccurenceThreshold--;
		
		// loop through all topics, and collect the occurrence
		for( PublicationTopic publicationTopic : publication.getPublicationTopics()){
			Map<String, Double> termValues = publicationTopic.getTermValues();
			
			// if term values not valid
			if ( termValues == null || termValues.isEmpty() )
				continue;

			// iterate over hashmap
			if ( language.equals( "english" ) )
			{
				for ( Map.Entry<String, Double> entry : termValues.entrySet() )
				{
					String term = normalizeText( entry.getKey(), language );
					// count occurrence
					if ( term.length() < 50 )
					{
						if ( uniqueTermAndOccurence.get( term ) != null )
							uniqueTermAndOccurence.put( term, uniqueTermAndOccurence.get( term ) + 1 );
						else
							uniqueTermAndOccurence.put( term, 1 );
					}
				}
			}
			else
			{
				// only alchemyAPI that capable to extract
				// topic correctly on other languages
				if ( publicationTopic.getExtractionServiceType().equals( ExtractionServiceType.ALCHEMYAPI ) )
				{
					for ( Map.Entry<String, Double> entry : termValues.entrySet() )
					{
						String term = normalizeText( entry.getKey(), language );
						// count occurrence
						if ( term.length() < 50 )
							uniqueTermAndOccurence.put( term, 1 );
					}
				}
			}
		}

		// find significant term, term that occurred multiple times
		for ( Map.Entry<String, Integer> entry : uniqueTermAndOccurence.entrySet() )
		{
			String term = entry.getKey();
			int occurence = entry.getValue();
			if ( language.equals( "english" ) )
			{
				if ( occurence >= interestOccurenceThreshold )
				{
					Interest interest = persistenceStrategy.getInterestDAO().getInterestByTerm( term );

					if ( interest == null )
					{
						interest = new Interest();
						interest.setTerm( term );
						persistenceStrategy.getInterestDAO().persist( interest );
					}
					interestSets.add( interest );
				}
			}
			else
			{
				Interest interest = persistenceStrategy.getInterestDAO().getInterestByTerm( term );

				if ( interest == null )
				{
					interest = new Interest();
					interest.setTerm( term );
					persistenceStrategy.getInterestDAO().persist( interest );
				}
				interestSets.add( interest );
			}
		}
		
		return interestSets;
	}

	/**
	 * Construct text input for publication information ( title, abstract,
	 * keyword, content)
	 * 
	 * @param publication
	 * @return
	 */
	private String getPublicationText( Publication publication )
	{
		String text = "";
		// title most significant, multiply 3 times
		for ( int i = 0; i < 2; i++ )
			text +=	publication.getTitle() + ". ";
		// title most quite significant, multiply 2 times
		for ( int i = 0; i < 2; i++ )
			if ( publication.getKeywords() != null )
				text += publication.getKeywords() + ". ";
		if ( publication.getAbstractText() != null )
			text += publication.getAbstractText() + " ";
		
//		if ( publication.getContentText() != null )
//			text += publication.getContentText() + " ";
		return text;
	}

	private String normalizeText( String text, String language )
	{
		// to lower case
		// remove all number
		// remove -_()
		// remove s on word end
		text = text.toLowerCase().replaceAll( "\\d", "" ).replaceAll( "[_\\-()]", " " );
		if ( language.equals( "english" ) )
			text = singularizeString( text );
		return text;
	}

	private String singularizeString( String text )
	{
		Inflector inflector = new Inflector();
		return inflector.singularize( text );
	}

	/**
	 * Result from porter steamer really bad for the term
	 * 
	 * @param text
	 * @return
	 */
	private String applyPorterStemmer( String text )
	{
		PorterStemmer porterStemmer = new PorterStemmer();
		porterStemmer.setCurrent( text );
		porterStemmer.stem();

		return porterStemmer.getCurrent();
	}
}

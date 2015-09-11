package de.rwth.i9.palm.interestmining.service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.analytics.algorithm.cvalue.CValue;
import de.rwth.i9.palm.analytics.algorithm.cvalue.TermCandidate;
import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorInterest;
import de.rwth.i9.palm.model.AuthorInterestProfile;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.InterestAuthor;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Service
public class CValueInterestProfileOld
{
	final Logger logger = Logger.getLogger( CValueInterestProfileOld.class );
	// final private String PROFILENAME = "cvalue";

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private PalmAnalytics palmAnalytics;

	public void doCValueCalculation( Map<String, Map<Integer, Map<Interest, Integer>>> languageYearInterestOccurrenceMap, Author author ) throws ParseException
	{
		// prepare the variables to saving the result
		Calendar calendar = Calendar.getInstance();
		AuthorInterestProfile authorInterestProfile = null;

		// default profile name [USERID][PROFILENAME]
		String authorInteresProfileName = author.getId() + "cvalue";

		// check whether similar profile has already saved in the database
		// if already exist do update
		Set<AuthorInterestProfile> authorInterestProfiles = author.getAuthorInterestProfiles();
		if ( authorInterestProfiles != null && !authorInterestProfiles.isEmpty() )
		{
			for ( AuthorInterestProfile eachAuthorInterestProfile : authorInterestProfiles )
				if ( eachAuthorInterestProfile.getName().equals( authorInteresProfileName ) )
					authorInterestProfile = eachAuthorInterestProfile;
		}

		// prepare list of authorInterest
		Set<AuthorInterest> authorInterests = new HashSet<>();

		// if there is still no profile, create new one
		if ( authorInterestProfile == null )
		{
			authorInterestProfile = new AuthorInterestProfile();
			authorInterestProfile.setAuthor( author );
			authorInterestProfile.setCreated( calendar.getTime() );
			authorInterestProfile.setDescription( "Interest mining using C-Value algorithm" );
			authorInterestProfile.setName( authorInteresProfileName );
			authorInterestProfile.setAuthorInterests( authorInterests );

//			persistenceStrategy.getAuthorInterestProfileDAO().persist( authorInterestProfile );

			// added interest profile to author
			author.addAuthorInterestProfiles( authorInterestProfile );
//			persistenceStrategy.getAuthorDAO().persist( author );
		}
		else
		{
			authorInterestProfile.setCreated( calendar.getTime() );
			// replacing authorInterest
			authorInterestProfile.setAuthorInterests( authorInterests );

//			persistenceStrategy.getAuthorInterestProfileDAO().persist( authorInterestProfile );
		}

		DateFormat dateFormat = new SimpleDateFormat( "yyyy", Locale.ENGLISH );

		// store interest and weighting of author lifelong
		Map<Interest, Double> interestWeightLifelongMap = new HashMap<Interest, Double>();

		for ( Entry<String, Map<Integer, Map<Interest, Integer>>> languageYearInterestOccurrenceEntry : languageYearInterestOccurrenceMap.entrySet() )
		{
			String language = languageYearInterestOccurrenceEntry.getKey();

			for ( Entry<Integer, Map<Interest, Integer>> yearInterestOccurrenceEntry : languageYearInterestOccurrenceEntry.getValue().entrySet() )
			{
				// create author interest
				AuthorInterest authorInterest = new AuthorInterest();
				authorInterest.setLanguage( language );
				authorInterest.setYear( dateFormat.parse( Integer.toString( yearInterestOccurrenceEntry.getKey() ) ) );
				authorInterest.setAuthorInterestProfile( authorInterestProfile );

				authorInterestProfile.addAuthorInterest( authorInterest );

				// store interest and weighting based on year
				Map<Interest, Double> interestWeightMap = new LinkedHashMap<Interest, Double>();

				Map<Interest, Integer> interestOccurrenceMap = yearInterestOccurrenceEntry.getValue();

				// helper for author interest map, term as the key
				Map<String, Interest> interestHelperMap = new HashMap<String, Interest>();

				List<String> terms = new ArrayList<>();
				for ( Entry<Interest, Integer> interestOccurrenceEntry : interestOccurrenceMap.entrySet() )
				{
					int occurrence = interestOccurrenceEntry.getValue();
					String term = interestOccurrenceEntry.getKey().getTerm();

					interestHelperMap.put( term, interestOccurrenceEntry.getKey() );

					if ( occurrence > 0 )
					{
						for ( int i = 0; i < occurrence; i++ )
						{
							terms.add( term );
						}
					}
				}

				// calculate c-value
				CValue cValue = palmAnalytics.getCValueAlgorithm();
				cValue.setTerms( terms );
				cValue.calculateCValue();

				List<TermCandidate> termCandidates = cValue.getTermCandidates();

				// put calculated term value to container
				for ( TermCandidate termCandidate : termCandidates )
				{
					if ( termCandidate.getCValue() >= 2 )
					{
						interestWeightMap.put( interestHelperMap.get( termCandidate.getCandidateTerm() ), termCandidate.getCValue() );
						// calculate lifelong maps
						if ( interestWeightLifelongMap.get( interestHelperMap.get( termCandidate.getCandidateTerm() ) ) != null )
							interestWeightLifelongMap.put( interestHelperMap.get( termCandidate.getCandidateTerm() ), interestWeightLifelongMap.get( interestHelperMap.get( termCandidate.getCandidateTerm() ) ) + termCandidate.getCValue() );
						else
							interestWeightLifelongMap.put( interestHelperMap.get( termCandidate.getCandidateTerm() ), termCandidate.getCValue() );
					}
				}

				if ( !interestWeightMap.isEmpty() )
					authorInterest.setTermWeights( interestWeightMap );

			}
		}

		Set<InterestAuthor> interestAuthors = author.getInterestAuthors();

		// store the lifelong learning interest
		for ( Entry<Interest, Double> interestWeightLifelongMapEntry : interestWeightLifelongMap.entrySet() )
		{
			Interest interest = interestWeightLifelongMapEntry.getKey();

			InterestAuthor interestAuthor = null;

			if ( interestAuthors != null && !interestAuthors.isEmpty() )
			{
				for ( InterestAuthor eachInterestAuthor : interestAuthors )
				{
					if ( eachInterestAuthor.getInterest().equals( interest ) )
					{
						interestAuthor = eachInterestAuthor;
						break;
					}
				}
			}

			if ( interestAuthor == null )
			{
				interestAuthor = new InterestAuthor();
				interestAuthor.setAuthor( author );
				interestAuthor.setInterest( interest );
			}
			interestAuthor.setValue( interestWeightLifelongMapEntry.getValue() );

			author.addInterestAuthor( interestAuthor );
			interest.addInterestAuthor( interestAuthor );
		}

		// at the end persist
		persistenceStrategy.getAuthorDAO().persist( author );
		persistenceStrategy.getAuthorInterestProfileDAO().persist( authorInterestProfile );
	}
}

package de.rwth.i9.palm.interestmining.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.helper.comparator.AuthorTopicModelingByDateComparator;
import de.rwth.i9.palm.helper.comparator.CircleTopicModelingByDateComparator;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorTopicModeling;
import de.rwth.i9.palm.model.AuthorTopicModelingProfile;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.CircleTopicModeling;
import de.rwth.i9.palm.model.CircleTopicModelingProfile;
import de.rwth.i9.palm.model.InterestProfileType;
import de.rwth.i9.palm.model.TopicModelingAlgorithmAuthor;
import de.rwth.i9.palm.model.TopicModelingAlgorithmCircle;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Service
public class TopicModelingService
{

	private final static Logger logger = LoggerFactory.getLogger( TopicModelingService.class );

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private PalmAnalytics palmAnalytics;

	public void calculateAuthorTopicModeling( Author author, boolean isReplaceExistingResult )
	{

		// First get active algorithm profile
		// There are 2 type of algorithm DEFAULT (need to be calculated from beginning)
		// and DERIVED (intersection or union between two or more DEFAULT algorithms)
		List<TopicModelingAlgorithmAuthor> activeDefaultAlgorithms = persistenceStrategy.getTopicModelingAlgorithmAuthorDAO().getAllActiveInterestProfile( InterestProfileType.DEFAULT );
		List<TopicModelingAlgorithmAuthor> activeDerivedAlgorithms = persistenceStrategy.getTopicModelingAlgorithmAuthorDAO().getAllActiveInterestProfile( InterestProfileType.DERIVED );
		
		// if no algorithms found or no topic modeling results found
		if ( ( activeDefaultAlgorithms == null || activeDefaultAlgorithms.isEmpty() ) && ( activeDerivedAlgorithms == null || activeDerivedAlgorithms.isEmpty() ) && ( author.getAuthorTopicModelingProfiles() == null && author.getAuthorTopicModelingProfiles().isEmpty() ) )
		{
			logger.warn( "status", "error - no active topic modeling algorithms found" );
		}
		// Note: Actually we will only consider DefaultAngorithm first, we just skip the derived results
		
		// now check whether we need to replace existing results
		if ( isReplaceExistingResult )
		{
			// First remove all results from previous calculation
			// by removing links between Author and AlgorithmProfile
			if ( author.getAuthorTopicModelingProfiles() != null && !author.getAuthorTopicModelingProfiles().isEmpty() )
			{
				// for( AuthorTopicModelingProfile atmp :
				// author.getAuthorTopicModelingProfiles() ){
				// atmp.setAuthor( null );
				// }
				author.getAuthorTopicModelingProfiles().clear();
			}

			// calculate interest with active default algorithms
			calculateDefaultTopicModelingAlgorithmAuthor( author, activeDefaultAlgorithms );
		}
		else
		{
			// first time running, profile is still empty
			if ( author.getAuthorTopicModelingProfiles() == null || author.getAuthorTopicModelingProfiles().isEmpty() )
			{
				// calculate interest with active default algorithms
				calculateDefaultTopicModelingAlgorithmAuthor( author, activeDefaultAlgorithms );
			}
			// else, check if there is missing profile
			else
			{
				for ( Iterator<TopicModelingAlgorithmAuthor> it = activeDefaultAlgorithms.iterator(); it.hasNext(); )
				{
					TopicModelingAlgorithmAuthor topicModelingAlgorithmAuthor = it.next();

					// check if algorithm profile is already exist on author
					boolean isAlgoritmProfileAlreadyExist = false;
					for ( AuthorTopicModelingProfile atmp : author.getAuthorTopicModelingProfiles() )
					{
						if ( atmp.getTopicModelingAlgorithmAuthor().equals( topicModelingAlgorithmAuthor ) )
						{
							isAlgoritmProfileAlreadyExist = true;
							break;
						}
					}

					// remove if algorithm is exist
					if ( isAlgoritmProfileAlreadyExist )
						it.remove();
				}

				// at the end, if there is still missing algorithm profile
				// run the calculation
				if ( !activeDefaultAlgorithms.isEmpty() )
					// calculate interest with active default algorithms
					calculateDefaultTopicModelingAlgorithmAuthor( author, activeDefaultAlgorithms );
			}
		}
	}

	public void calculateCircleTopicModeling( Circle circle, boolean isReplaceExistingResult )
	{

		// First get active algorithm profile
		// There are 2 type of algorithm DEFAULT (need to be calculated from
		// beginning)
		// and DERIVED (intersection or union between two or more DEFAULT
		// algorithms)
		List<TopicModelingAlgorithmCircle> activeDefaultAlgorithms = persistenceStrategy.getTopicModelingAlgorithmCircleDAO().getAllActiveInterestProfile( InterestProfileType.DEFAULT );
		List<TopicModelingAlgorithmCircle> activeDerivedAlgorithms = persistenceStrategy.getTopicModelingAlgorithmCircleDAO().getAllActiveInterestProfile( InterestProfileType.DERIVED );

		// if no algorithms found or no topic modeling results found
		if ( ( activeDefaultAlgorithms == null || activeDefaultAlgorithms.isEmpty() ) && ( activeDerivedAlgorithms == null || activeDerivedAlgorithms.isEmpty() ) && ( circle.getCircleInterestProfiles() == null && circle.getCircleTopicModelingProfiles().isEmpty() ) )
		{
			logger.warn( "status", "error - no active topic modeling algorithms found" );
		}
		// Note: Actually we will only consider DefaultAngorithm first, we just
		// skip the derived results

		// now check whether we need to replace existing results
		if ( isReplaceExistingResult )
		{
			// First remove all results from previous calculation
			// by removing links between Author and AlgorithmProfile
			if ( circle.getCircleTopicModelingProfiles() != null && !circle.getCircleTopicModelingProfiles().isEmpty() )
			{
				// for( AuthorTopicModelingProfile atmp :
				// author.getAuthorTopicModelingProfiles() ){
				// atmp.setAuthor( null );
				// }
				circle.getCircleTopicModelingProfiles().clear();
			}

			// calculate interest with active default algorithms
			calculateDefaultTopicModelingAlgorithmCircle( circle, activeDefaultAlgorithms );
		}
		else
		{
			// first time running, profile is still empty
			if ( circle.getCircleTopicModelingProfiles() == null || circle.getCircleTopicModelingProfiles().isEmpty() )
			{
				// calculate interest with active default algorithms
				calculateDefaultTopicModelingAlgorithmCircle( circle, activeDefaultAlgorithms );
			}
			// else, check if there is missing profile
			else
			{
				for ( Iterator<TopicModelingAlgorithmCircle> it = activeDefaultAlgorithms.iterator(); it.hasNext(); )
				{
					TopicModelingAlgorithmCircle topicModelingAlgorithmCircle = it.next();

					// check if algorithm profile is already exist on author
					boolean isAlgoritmProfileAlreadyExist = false;
					for ( CircleTopicModelingProfile atmp : circle.getCircleTopicModelingProfiles() )
					{
						if ( atmp.getTopicModelingAlgorithmCircle().equals( topicModelingAlgorithmCircle ) )
						{
							isAlgoritmProfileAlreadyExist = true;
							break;
						}
					}

					// remove if algorithm is exist
					if ( isAlgoritmProfileAlreadyExist )
						it.remove();
				}

				// at the end, if there is still missing algorithm profile
				// run the calculation
				if ( !activeDefaultAlgorithms.isEmpty() )
					// calculate interest with active default algorithms
					calculateDefaultTopicModelingAlgorithmCircle( circle, activeDefaultAlgorithms );
			}
		}
	}

	/**
	 * Calculate active default topic modeling from specific author
	 * 
	 * @param author
	 * @param activeDefaultAlgorithms
	 */
	private void calculateDefaultTopicModelingAlgorithmAuthor( Author author, List<TopicModelingAlgorithmAuthor> activeDefaultAlgorithms )
	{

		for ( TopicModelingAlgorithmAuthor activeDefaultAlgorithm : activeDefaultAlgorithms )
		{

			// ==Set Author TopicModeling profile==

			Calendar calendar = Calendar.getInstance();
			// default profile name [DEFAULT_PROFILENAME]
			String authorTopicModelingProfileName = activeDefaultAlgorithm.getName();

			// create new author topicModeling profile for basic dummy
			AuthorTopicModelingProfile authorTopicModelingProfile = new AuthorTopicModelingProfile();
			authorTopicModelingProfile.setCreated( calendar.getTime() );
			authorTopicModelingProfile.setDescription( "Topic Model mining using " + activeDefaultAlgorithm.getName() + " algorithm" );
			authorTopicModelingProfile.setName( authorTopicModelingProfileName );
			authorTopicModelingProfile.setTopicModelingAlgorithmAuthor( activeDefaultAlgorithm );

			// calculate dummy lda
			// this is the actual implementation of NGrams N=1
			if ( activeDefaultAlgorithm.getName().toLowerCase().equals( "basic dummy lda" ) )
			{
				calculateBasicDummyLDAAuthor( author, authorTopicModelingProfile );
			}

			// calculate dummy ngram
			else if ( activeDefaultAlgorithm.getName().toLowerCase().equals( "basic dummy ngram" ) )
			{
				calculateBasicDummyNgramAuthor( author, authorTopicModelingProfile );
			}
			// put other algorithm selection here, such as ngram etc

			// add TopicModelingProfile to author
			if ( authorTopicModelingProfile.getAuthorTopicModelings() != null && !authorTopicModelingProfile.getAuthorTopicModelings().isEmpty() )
			{
				// link author with authorTopicModelingProfile
				authorTopicModelingProfile.setAuthor( author );
				author.addAuthorTopicModelingProfiles( authorTopicModelingProfile );
			}
		}

		// at the end persist author
		persistenceStrategy.getAuthorDAO().persist( author );
	}

	/**
	 * Calculate active default topic modeling from specific author
	 * 
	 * @param author
	 * @param activeDefaultAlgorithms
	 */
	private void calculateDefaultTopicModelingAlgorithmCircle( Circle circle, List<TopicModelingAlgorithmCircle> activeDefaultAlgorithms )
	{

		for ( TopicModelingAlgorithmCircle activeDefaultAlgorithm : activeDefaultAlgorithms )
		{

			// ==Set Circle TopicModeling profile==

			Calendar calendar = Calendar.getInstance();
			// default profile name [DEFAULT_PROFILENAME]
			String circleTopicModelingProfileName = activeDefaultAlgorithm.getName();

			// create new author topicModeling profile for basic dummy
			CircleTopicModelingProfile circleTopicModelingProfile = new CircleTopicModelingProfile();
			circleTopicModelingProfile.setCreated( calendar.getTime() );
			circleTopicModelingProfile.setDescription( "Topic Model mining using " + activeDefaultAlgorithm.getName() + " algorithm" );
			circleTopicModelingProfile.setName( circleTopicModelingProfileName );
			circleTopicModelingProfile.setTopicModelingAlgorithmCircle( activeDefaultAlgorithm );

			// calculate dummy lda
			if ( activeDefaultAlgorithm.getName().toLowerCase().equals( "basic dummy lda" ) )
			{
				calculateBasicDummyLDACircle( circle, circleTopicModelingProfile );
			}

			// calculate dummy ngram
			else if ( activeDefaultAlgorithm.getName().toLowerCase().equals( "basic dummy ngram" ) )
			{
				calculateBasicDummyNgramCircle( circle, circleTopicModelingProfile );
			}
			// put other algorithm selection here, such as ngram etc

			// add TopicModelingProfile to author
			if ( circleTopicModelingProfile.getCircleTopicModelings() != null && !circleTopicModelingProfile.getCircleTopicModelings().isEmpty() )
			{
				// link author with authorTopicModelingProfile
				circleTopicModelingProfile.setCircle( circle );
				circle.addAuthorTopicModelingProfiles( circleTopicModelingProfile );
			}
		}

		// at the end persist author
		persistenceStrategy.getCircleDAO().persist( circle );
	}

	/**
	 * Dummy method to show how to convert result on map into
	 * authorTopicModelingProfile and authorTopic>Modeling
	 * 
	 */
	private void calculateBasicDummyLDAAuthor( Author author, AuthorTopicModelingProfile authorTopicModelingProfile )
	{	
		// Normally, here you call palmAnalytics.getLDA....
		// to calculate the LDA and get the result as Map / List
		
		// get dummy results based on years cluster
		List<Object> dummyClusterResults = getUnigramResultsAuthor( author );
		
		// now put this cluster list into authorTopicModeling object
		transformTopicModelingResultIntoAuthorTopicModeling( authorTopicModelingProfile, dummyClusterResults );

	}

	/**
	 * Dummy method to show how to convert result on map into
	 * authorTopicModelingProfile and authorTopic>Modeling
	 * 
	 */
	private void calculateBasicDummyLDACircle( Circle circle, CircleTopicModelingProfile circleTopicModelingProfile )
	{
		// Normally, here you call palmAnalytics.getLDA....
		// to calculate the LDA and get the result as Map / List

		// get dummy results based on years cluster
		List<Object> dummyClusterResults = getDummyClusterResultsCircle( circle );

		// now put this cluster list into authorTopicModeling object
		transformTopicModelingResultIntoCircleTopicModeling( circleTopicModelingProfile, dummyClusterResults );

	}

	/**
	 * Dummy method to show how to convert result on map into
	 * authorTopicModelingProfile and authorTopic>Modeling
	 * 
	 */
	private void calculateBasicDummyNgramAuthor( Author author, AuthorTopicModelingProfile authorTopicModelingProfile )
	{	
		// Normally, here you call palmAnalytics.getNgram....
		// to calculate the LDA and get the result as Map / List
		
		// get dummy results based on years cluster
		List<Object> dummyClusterResults = getDummyClusterResultsAuthor( author );

		// now put this cluster list into authorTopicModeling object
		transformTopicModelingResultIntoAuthorTopicModeling( authorTopicModelingProfile, dummyClusterResults );
	}

	/**
	 * Dummy method to show how to convert result on map into
	 * authorTopicModelingProfile and authorTopic>Modeling
	 * 
	 */
	private void calculateBasicDummyNgramCircle( Circle circle, CircleTopicModelingProfile circleTopicModelingProfile )
	{
		// Normally, here you call palmAnalytics.getNgram....
		// to calculate the LDA and get the result as Map / List

		// get dummy results based on years cluster
		List<Object> dummyClusterResults = getDummyClusterResultsCircle( circle );

		// now put this cluster list into authorTopicModeling object
		transformTopicModelingResultIntoCircleTopicModeling( circleTopicModelingProfile, dummyClusterResults );
	}

	/**
	 * Dummy method to show how to transform topic modeling results from
	 * Collections to authorTopicModeling object
	 * 
	 */
	private void transformTopicModelingResultIntoAuthorTopicModeling( AuthorTopicModelingProfile authorTopicModelingProfile, List<Object> dummyClusterResults )
	{
		DateFormat dateFormat = new SimpleDateFormat( "yyyy", Locale.ENGLISH );

		// loop based on clusters
		for ( Object dummyClusterResult : dummyClusterResults )
		{
			@SuppressWarnings( "unchecked" )
			Map<String, Object> clusterResultMap = (Map<String, Object>) dummyClusterResult;

			// check for result, is it empty or null?
			@SuppressWarnings( "unchecked" )
			Map<String, Double> termValueMap = (Map<String, Double>) clusterResultMap.get( "termvalues" );

			// if null or empty just skip
			if ( termValueMap == null || termValueMap.isEmpty() )
				continue;

			// get date
			Date clusterYear = null;
			try
			{
				clusterYear = dateFormat.parse( clusterResultMap.get( "year" ).toString() );
			}
			catch ( Exception e )
			{
			}

			// create new AuthorTopicModeling (year cluster) and fill attributes
			AuthorTopicModeling atm = new AuthorTopicModeling();
			atm.setYear( clusterYear );
			atm.setLanguage( clusterResultMap.get( "language" ).toString() );
			atm.setTermWeightsString( termValueMap );
			atm.setAuthorTopicModelingProfile( authorTopicModelingProfile );

			authorTopicModelingProfile.addAuthorTopicModeling( atm );
		}

	}

	/**
	 * Dummy method to show how to transform topic modeling results from
	 * Collections to authorTopicModeling object
	 * 
	 */
	private void transformTopicModelingResultIntoCircleTopicModeling( CircleTopicModelingProfile circleTopicModelingProfile, List<Object> dummyClusterResults )
	{
		DateFormat dateFormat = new SimpleDateFormat( "yyyy", Locale.ENGLISH );

		// loop based on clusters
		for ( Object dummyClusterResult : dummyClusterResults )
		{
			@SuppressWarnings( "unchecked" )
			Map<String, Object> clusterResultMap = (Map<String, Object>) dummyClusterResult;

			// check for result, is it empty or null?
			@SuppressWarnings( "unchecked" )
			Map<String, Double> termValueMap = (Map<String, Double>) clusterResultMap.get( "termvalues" );

			// if null or empty just skip
			if ( termValueMap == null || termValueMap.isEmpty() )
				continue;

			// get date
			Date clusterYear = null;
			try
			{
				clusterYear = dateFormat.parse( clusterResultMap.get( "year" ).toString() );
			}
			catch ( Exception e )
			{
			}

			// create new AuthorTopicModeling (year cluster) and fill attributes
			CircleTopicModeling atm = new CircleTopicModeling();
			atm.setYear( clusterYear );
			atm.setLanguage( clusterResultMap.get( "language" ).toString() );
			atm.setTermWeightsString( termValueMap );
			atm.setCircleTopicModelingProfile( circleTopicModelingProfile );

			circleTopicModelingProfile.addCircleTopicModeling( atm );
		}

	}

	/**
	 * This method generate dummy data
	 * 
	 * @param author
	 * @param topicModelingAlgorithmAuthor
	 * @return
	 */
	private List<Object> getDummyClusterResultsAuthor( Author author )
	{
		// Note: usually you need author object, to get author publications, etc
		// but author object is unused in this dummy method

		// list of cluster container
		List<Object> dummyClusterResults = new ArrayList<Object>();

		Random random = new Random();

		// dummy start year array
		int[] startYearArray = { 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 };

		// dummy end year array
		int[] endYearArray = { 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 };

		// randomly pick start and end year
//		int startYear = startYearArray[random.nextInt( 11 )];
//		int endYear = endYearArray[random.nextInt( 11 )];
// but this is dummy years  It doesnt change that much from yours. I use the same range. as 2005 to 2015 the loop is the same
		//ok
		for ( int i = startYearArray[0]; i < endYearArray[endYearArray.length - 1]; i++ )
		{
			// store dummy information regarding cluster information
			Map<String, Object> clusterResultMap = new LinkedHashMap<String, Object>();

			// store dummy information regarding result information
			Map<String, Double> termValueMap = new HashMap<String, Double>();

			clusterResultMap.put( "year", i );
			clusterResultMap.put( "language", "english" );
			clusterResultMap.put( "termvalues", termValueMap );

			// generate dummy terms
//			int numberOfWords = random.nextInt( 5 ) + 5;
//			int numberOfTermValues = random.nextInt( 10 ) + 10;
			for ( int j = 0; j < 11; j++ )
			{
//				String[] dummyWords = generateRandomWords( numberOfWords );
//				StringBuilder dummyWordString = new StringBuilder();
//				for ( int k = 0; k < dummyWords.length; k++ )
//				{
//					if ( k > 0 )
//						dummyWordString.append( ", " );
//					dummyWordString.append( dummyWords[k] );
//				}
				// add into termValueMap
				termValueMap.put(palmAnalytics.getDynamicTopicModel().getListTopics( 10 ).get( j ), palmAnalytics.getDynamicTopicModel().getTopicProportion2(0.0, i-2005, 11, 11 ).get( j ) );//dummyWordString.toString(), Math.random() );
			}
			dummyClusterResults.add( clusterResultMap );
		}

		// return dummy results
		return dummyClusterResults;
	}

	/**
	 * This method is supposed to use the GetUnigrams from Ngram Algorithm
	 * 
	 * @param author
	 * @return
	 */
	public List<Object> getUnigramResultsAuthor( Author author )
	{
		// Note: This time the author object will be used to identify the
		// document we are referring to.

		// list of Maps<String, Double> container
		List<Object> unigramsResults = new ArrayList<Object>();

		// store information regarding results (used as a structure for JSON)
		Map<String, Object> clusterResultMap = new LinkedHashMap<String, Object>();

		// store term-proportions from NGrams 
		Map<String, Double> termValueMap = new HashMap<String, Double>();
		
		Map<String, List<String>> topicNgrams = palmAnalytics.getNGrams().getTopicUnigramsDocument( palmAnalytics.getNGrams().maptoRealDatabaseID( (String) author.getId() ), -1, 0.0, 100, 10, false );

		// ASK SIGIT IF THERE IS A WAY TO PASS THE NUMBER OF TOPICS Here
		for ( Entry<String, List<String>> topicngrams : topicNgrams.entrySet() )
		{
			for ( String topicproportion : topicngrams.getValue() )
			{
				termValueMap.put( topicproportion.split( "  _-_ " )[0], Double.parseDouble( topicproportion.split( "  _-_ " )[1] ) );
			}
		}
		clusterResultMap.put( "years", 2005 );
		clusterResultMap.put( "language", "english" );
		clusterResultMap.put( "termvalues", termValueMap );
		unigramsResults.add( clusterResultMap );

	// return dummy results
		return unigramsResults;
	}

	/**
	 * This method generate dummy data
	 * 
	 * @param author
	 * @param topicModelingAlgorithmAuthor
	 * @return
	 */
	private List<Object> getDummyClusterResultsCircle( Circle circle )
	{
		// Note: usually you need author object, to get author publications, etc
		// but author object is unused in this dummy method

		// list of cluster container
		List<Object> dummyClusterResults = new ArrayList<Object>();

		Random random = new Random();

		// dummy start year array
		int[] startYearArray = { 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 };

		// dummy end year array
		int[] endYearArray = { 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 };

		// randomly pick start and end year
		// int startYear = startYearArray[random.nextInt( 11 )];
		// int endYear = endYearArray[random.nextInt( 11 )];
		// but this is dummy years It doesnt change that much from yours. I use
		// the same range. as 2005 to 2015 the loop is the same
		// ok
		for ( int i = startYearArray[0]; i < endYearArray[endYearArray.length - 1]; i++ )
		{
			// store dummy information regarding cluster information
			Map<String, Object> clusterResultMap = new LinkedHashMap<String, Object>();

			// store dummy information regarding result information
			Map<String, Double> termValueMap = new HashMap<String, Double>();

			clusterResultMap.put( "year", i );
			clusterResultMap.put( "language", "english" );
			clusterResultMap.put( "termvalues", termValueMap );

			// generate dummy terms
			// int numberOfWords = random.nextInt( 5 ) + 5;
			// int numberOfTermValues = random.nextInt( 10 ) + 10;
			for ( int j = 0; j < 11; j++ )
			{
				// String[] dummyWords = generateRandomWords( numberOfWords );
				// StringBuilder dummyWordString = new StringBuilder();
				// for ( int k = 0; k < dummyWords.length; k++ )
				// {
				// if ( k > 0 )
				// dummyWordString.append( ", " );
				// dummyWordString.append( dummyWords[k] );
				// }
				// add into termValueMap
				termValueMap.put( palmAnalytics.getDynamicTopicModel().getListTopics( 10 ).get( j ), palmAnalytics.getDynamicTopicModel().getTopicProportion2( 0.0, i - 2005, 11, 11 ).get( j ) );// dummyWordString.toString(),
																																																	// Math.random()
																																																	// );
			}
			dummyClusterResults.add( clusterResultMap );
		}

		// return dummy results
		return dummyClusterResults;

	}

	/**
	 * Generate random arbitrary words
	 */
	private String[] generateRandomWords( int numberOfWords )
	{
		String[] randomStrings = new String[numberOfWords];
		Random random = new Random();
		for ( int i = 0; i < numberOfWords; i++ )
		{
			char[] word = new char[random.nextInt( 8 ) + 3];
			for ( int j = 0; j < word.length; j++ )
			{
				word[j] = (char) ( 'a' + random.nextInt( 26 ) );
			}
			randomStrings[i] = new String( word );
		}
		return randomStrings;
	}

	/**
	 * Collect the author interest result as JSON object
	 * 
	 * @param author
	 * @param responseMap
	 * @return
	 */
	public List<Object> getAuthorTopicModeliFromDatabase( Author author )
	{
		List<AuthorTopicModelingProfile> authorTopicModelingProfiles = new ArrayList<AuthorTopicModelingProfile>();
		authorTopicModelingProfiles.addAll( author.getAuthorTopicModelingProfiles() );

		// the whole result related to interest
		List<Object> authorTopicModelingResult = new ArrayList<Object>();

		for ( AuthorTopicModelingProfile authorTopicModelingProfile : authorTopicModelingProfiles )
		{
			// put profile on map
			Map<String, Object> authorTopicModelingResultProfilesMap = new HashMap<String, Object>();

			// get interest profile name and description
			String interestProfileName = authorTopicModelingProfile.getName();
			String interestProfileDescription = authorTopicModelingProfile.getDescription();

			// get authorTopicModeling set on profile
			Set<AuthorTopicModeling> authorTopicModelings = authorTopicModelingProfile.getAuthorTopicModelings();

			// if profile contain no authorTopicModeling just skip
			if ( authorTopicModelings == null || authorTopicModelings.isEmpty() )
				continue;

			// a map for storing authorInterst based on language
			Map<String, List<AuthorTopicModeling>> authorTopicModelingLanguageMap = new HashMap<String, List<AuthorTopicModeling>>();

			// split authorinterest based on language and put it on the map
			for ( AuthorTopicModeling authorTopicModeling : authorTopicModelings )
			{
				if ( authorTopicModelingLanguageMap.get( authorTopicModeling.getLanguage() ) != null )
				{
					authorTopicModelingLanguageMap.get( authorTopicModeling.getLanguage() ).add( authorTopicModeling );
				}
				else
				{
					List<AuthorTopicModeling> authorTopicModelingList = new ArrayList<AuthorTopicModeling>();
					authorTopicModelingList.add( authorTopicModeling );
					authorTopicModelingLanguageMap.put( authorTopicModeling.getLanguage(), authorTopicModelingList );
				}
			}

			// prepare calendar for extractind year from date
			Calendar calendar = Calendar.getInstance();

			// result author interest based on language
			List<Object> authorTopicModelingResultLanguageList = new ArrayList<Object>();

			// sort authorinterest based on year
			for ( Map.Entry<String, List<AuthorTopicModeling>> authorTopicModelingLanguageIterator : authorTopicModelingLanguageMap.entrySet() )
			{
				// result container
				Map<String, Object> authorTopicModelingResultLanguageMap = new LinkedHashMap<String, Object>();
				// hashmap value
				String interestLanguage = authorTopicModelingLanguageIterator.getKey();
				List<AuthorTopicModeling> interestList = authorTopicModelingLanguageIterator.getValue();

				// sort based on year
				Collections.sort( interestList, new AuthorTopicModelingByDateComparator() );

				// term values based on year result container
				List<Object> authorTopicModelingResultYearList = new ArrayList<Object>();

				// get interest year, term and value
				for ( AuthorTopicModeling authorTopicModeling : interestList )
				{
					if ( authorTopicModeling.getTermWeightsString() == null || authorTopicModeling.getTermWeightsString().isEmpty() )
						continue;

					// result container
					Map<String, Object> authorTopicModelingResultYearMap = new LinkedHashMap<String, Object>();

					// get year
					calendar.setTime( authorTopicModeling.getYear() );
					String year = Integer.toString( calendar.get( Calendar.YEAR ) );

					List<Object> termValueResult = new ArrayList<Object>();

					// put term and value
					for ( Map.Entry<String, Double> termWeightMap : authorTopicModeling.getTermWeightsString().entrySet() )
					{
						List<Object> termWeightObjects = new ArrayList<Object>();
						termWeightObjects.add( termWeightMap.getKey() );
						termWeightObjects.add( termWeightMap.getValue() );
						termValueResult.add( termWeightObjects );
					}
					authorTopicModelingResultYearMap.put( "year", year );
					authorTopicModelingResultYearMap.put( "termvalue", termValueResult );
					authorTopicModelingResultYearList.add( authorTopicModelingResultYearMap );
				}

				authorTopicModelingResultLanguageMap.put( "language", interestLanguage );
				authorTopicModelingResultLanguageMap.put( "interestyears", authorTopicModelingResultYearList );
				if ( interestLanguage.equals( "english" ) )
					authorTopicModelingResultLanguageList.add( 0, authorTopicModelingResultLanguageMap );
				else
					authorTopicModelingResultLanguageList.add( authorTopicModelingResultLanguageMap );
			}

			// put profile map
			authorTopicModelingResultProfilesMap.put( "profile", interestProfileName );
			authorTopicModelingResultProfilesMap.put( "description", interestProfileDescription );
			authorTopicModelingResultProfilesMap.put( "interestlanguages", authorTopicModelingResultLanguageList );
			authorTopicModelingResult.add( authorTopicModelingResultProfilesMap );
		}

		// put also publication

		return authorTopicModelingResult;
	}

	/**
	 * Collect the author interest result as JSON object
	 * 
	 * @param author
	 * @param responseMap
	 * @return
	 */
	public List<Object> getCircleTopicModeliFromDatabase( Circle circle )
	{
		List<CircleTopicModelingProfile> circleTopicModelingProfiles = new ArrayList<CircleTopicModelingProfile>();
		circleTopicModelingProfiles.addAll( circle.getCircleTopicModelingProfiles() );

		// the whole result related to interest
		List<Object> circleTopicModelingResult = new ArrayList<Object>();

		for ( CircleTopicModelingProfile circleTopicModelingProfile : circleTopicModelingProfiles )
		{
			// put profile on map
			Map<String, Object> circleTopicModelingResultProfilesMap = new HashMap<String, Object>();

			// get interest profile name and description
			String interestProfileName = circleTopicModelingProfile.getName();
			String interestProfileDescription = circleTopicModelingProfile.getDescription();

			// get authorTopicModeling set on profile
			Set<CircleTopicModeling> circleTopicModelings = circleTopicModelingProfile.getCircleTopicModelings();

			// if profile contain no authorTopicModeling just skip
			if ( circleTopicModelings == null || circleTopicModelings.isEmpty() )
				continue;

			// a map for storing authorInterst based on language
			Map<String, List<CircleTopicModeling>> circleTopicModelingLanguageMap = new HashMap<String, List<CircleTopicModeling>>();

			// split authorinterest based on language and put it on the map
			for ( CircleTopicModeling circleTopicModeling : circleTopicModelings )
			{
				if ( circleTopicModelingLanguageMap.get( circleTopicModeling.getLanguage() ) != null )
				{
					circleTopicModelingLanguageMap.get( circleTopicModeling.getLanguage() ).add( circleTopicModeling );
				}
				else
				{
					List<CircleTopicModeling> circleTopicModelingList = new ArrayList<CircleTopicModeling>();
					circleTopicModelingList.add( circleTopicModeling );
					circleTopicModelingLanguageMap.put( circleTopicModeling.getLanguage(), circleTopicModelingList );
				}
			}

			// prepare calendar for extractind year from date
			Calendar calendar = Calendar.getInstance();

			// result author interest based on language
			List<Object> circleTopicModelingResultLanguageList = new ArrayList<Object>();

			// sort authorinterest based on year
			for ( Map.Entry<String, List<CircleTopicModeling>> circleTopicModelingLanguageIterator : circleTopicModelingLanguageMap.entrySet() )
			{
				// result container
				Map<String, Object> circleTopicModelingResultLanguageMap = new LinkedHashMap<String, Object>();
				// hashmap value
				String interestLanguage = circleTopicModelingLanguageIterator.getKey();
				List<CircleTopicModeling> interestList = circleTopicModelingLanguageIterator.getValue();

				// sort based on year
				Collections.sort( interestList, new CircleTopicModelingByDateComparator() );

				// term values based on year result container
				List<Object> circleTopicModelingResultYearList = new ArrayList<Object>();

				// get interest year, term and value
				for ( CircleTopicModeling circleTopicModeling : interestList )
				{
					if ( circleTopicModeling.getTermWeightsString() == null || circleTopicModeling.getTermWeightsString().isEmpty() )
						continue;

					// result container
					Map<String, Object> circleTopicModelingResultYearMap = new LinkedHashMap<String, Object>();

					// get year
					calendar.setTime( circleTopicModeling.getYear() );
					String year = Integer.toString( calendar.get( Calendar.YEAR ) );

					List<Object> termValueResult = new ArrayList<Object>();

					// put term and value
					for ( Map.Entry<String, Double> termWeightMap : circleTopicModeling.getTermWeightsString().entrySet() )
					{
						List<Object> termWeightObjects = new ArrayList<Object>();
						termWeightObjects.add( termWeightMap.getKey() );
						termWeightObjects.add( termWeightMap.getValue() );
						termValueResult.add( termWeightObjects );
					}
					circleTopicModelingResultYearMap.put( "year", year );
					circleTopicModelingResultYearMap.put( "termvalue", termValueResult );
					circleTopicModelingResultYearList.add( circleTopicModelingResultYearMap );
				}

				circleTopicModelingResultLanguageMap.put( "language", interestLanguage );
				circleTopicModelingResultLanguageMap.put( "interestyears", circleTopicModelingResultYearList );
				if ( interestLanguage.equals( "english" ) )
					circleTopicModelingResultLanguageList.add( 0, circleTopicModelingResultLanguageMap );
				else
					circleTopicModelingResultLanguageList.add( circleTopicModelingResultLanguageMap );
			}

			// put profile map
			circleTopicModelingResultProfilesMap.put( "profile", interestProfileName );
			circleTopicModelingResultProfilesMap.put( "description", interestProfileDescription );
			circleTopicModelingResultProfilesMap.put( "interestlanguages", circleTopicModelingResultLanguageList );
			circleTopicModelingResult.add( circleTopicModelingResultProfilesMap );
		}

		// put also publication

		return circleTopicModelingResult;
	}

}

package de.rwth.i9.palm.feature.researcher;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.feature.AcademicFeatureImpl;
import de.rwth.i9.palm.interestmining.service.InterestMiningService;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.topicextraction.service.TopicExtractionService;

@Component
public class ResearcherInterestImpl extends AcademicFeatureImpl implements ResearcherInterest
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private TopicExtractionService topicExtractionService;
	
	@Autowired
	private InterestMiningService interestMiningService;

	@Override
	public Map<String, Object> getAuthorInterestById( String authorId, String extractionServiceType, String startDate, String endDate ) throws InterruptedException, UnsupportedEncodingException, URISyntaxException, ParseException, ExecutionException
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		if ( authorId == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "author id missing" );
			return responseMap;
		}

		// get the author
		Author author = persistenceStrategy.getAuthorDAO().getById( authorId );

		if ( author == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "author not found" );
			return responseMap;
		}

		Map<String, Object> targetAuthorMap = new LinkedHashMap<String, Object>();
		targetAuthorMap.put( "id", author.getId() );
		targetAuthorMap.put( "name", author.getName() );
		responseMap.put( "author", targetAuthorMap );

		// check whether publication has been extracted
		// later add extractionServiceType checking
		topicExtractionService.extractTopicFromPublicationByAuthor( author );
		
		
		// mining the author interest
		interestMiningService.getInterestFromAuthor( responseMap, author, false );

//		// put the publication into arrayLiat
//		List<Publication> publications = new ArrayList<Publication>();
//		publications.addAll( author.getPublications() );
//
//		// remove any publication that doesn't have date or abstract
//		for ( Iterator<Publication> iterator = publications.iterator(); iterator.hasNext(); )
//		{
//			Publication publication = iterator.next();
//			if ( publication.getAbstractText() == null || publication.getPublicationDate() == null || publication.getPublicationTopics() == null )
//				iterator.remove();
//		}
//
//		// sort publication based on date
//		Collections.sort( publications, new PublicationByDateComparator() );
//
//		// prepare calendar to get year
//		Calendar calendar = Calendar.getInstance();
//
//		// prepare the date structure
//		Map<String, Map<String, Integer>> interestYearsMap = new LinkedHashMap<String, Map<String, Integer>>();
//		Map<String, Integer> interestSpecificYearMap = null;
//		// create interest based of year
//		String previousYear = "";
//		for ( Publication publication : publications )
//		{
//			// get year
//			calendar.setTime( publication.getPublicationDate() );
//			String currentYear = Integer.toString( calendar.get( Calendar.YEAR ) );
//			// renew the map
//			if ( !currentYear.equals( previousYear ) )
//			{
//				// put to main map
//				if ( interestSpecificYearMap != null )
//					interestYearsMap.put( previousYear, interestSpecificYearMap );
//				// new initialization for next year
//				interestSpecificYearMap = new HashMap<String, Integer>();
//			}
//			// get publication interest
//			for ( PublicationTopic pubTopic : publication.getPublicationTopics() )
//			{
//				if ( pubTopic.getExtractionServiceType().equals( ExtractionServiceType.valueOf( extractionServiceType.toUpperCase() ) ) )
//				{
//					Map<String, Double> termValues = pubTopic.getTermValues();
//
//					if ( termValues != null )
//					{
//						for ( Map.Entry<String, Double> entry : termValues.entrySet() )
//						{
//							if ( entry.getValue() > 0.5 )
//							{
//								if ( interestSpecificYearMap.get( entry.getKey() ) != null )
//									interestSpecificYearMap.put( entry.getKey(), interestSpecificYearMap.get( entry.getKey() ) + 1 );
//								else // found for the first time
//									interestSpecificYearMap.put( entry.getKey(), 1 );
//							}
//						}
//					}
//				}
//			}
//
//			previousYear = currentYear;
//		}
		// put the result
		responseMap.put( "status", "Ok" );
//		responseMap.put( "interest", interestYearsMap );

		return responseMap;
	}

	@Override
	public Map<String, Object> getAuthorInterestByName( String authorName, String extractionServiceType, String startDate, String endDate )
	{
		// TODO Auto-generated method stub
		return null;
	}
	
}

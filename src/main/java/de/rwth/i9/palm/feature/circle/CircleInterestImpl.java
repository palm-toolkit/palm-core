package de.rwth.i9.palm.feature.circle;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.feature.AcademicFeatureImpl;
import de.rwth.i9.palm.interestmining.service.CircleInterestMiningService;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.topicextraction.service.TopicExtractionService;

@Component
public class CircleInterestImpl extends AcademicFeatureImpl implements CircleInterest
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private TopicExtractionService topicExtractionService;
	
	@Autowired
	private CircleInterestMiningService circleInterestMiningService;

	@Override
	public Map<String, Object> getCircleInterestById( String circleId, String extractionServiceType, String startDate, String endDate ) throws InterruptedException, UnsupportedEncodingException, URISyntaxException, ParseException
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		if ( circleId == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "circle id missing" );
			return responseMap;
		}

		// get the author
		Circle circle = persistenceStrategy.getCircleDAO().getById( circleId );

		if ( circle == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "circle not found" );
			return responseMap;
		}

		Map<String, Object> targetCircleMap = new LinkedHashMap<String, Object>();
		targetCircleMap.put( "id", circle.getId() );
		targetCircleMap.put( "name", circle.getName() );
		responseMap.put( "author", targetCircleMap );

		// check whether publication has been extracted
		// later add extractionServiceType checking
		topicExtractionService.extractTopicFromPublicationByCircle( circle );
		
		
		// mining the author interest
		circleInterestMiningService.getInterestFromCircle( responseMap, circle, false );

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
	
}

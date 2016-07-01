package de.rwth.i9.palm.feature.circle;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.feature.researcher.ResearcherTopicModelingImpl;
import de.rwth.i9.palm.interestmining.service.TopicModelingService;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class CircleTopicModelingImpl implements CircleTopicModeling
{
	private final static Logger logger = LoggerFactory.getLogger( ResearcherTopicModelingImpl.class );

	@Autowired
	private TopicModelingService topicModelingService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	/**
	 * This is the first method used to show the widget Simple LDA
	 * implementation String circleId boolean isReplaceExistingResult
	 */
	@Override
	public Map<String, Object> getTopicModeling( String circleId, boolean isReplaceExistingResult )
	{
		// create JSON container for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		Circle circle = persistenceStrategy.getCircleDAO().getById( circleId );

		if ( circle == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "circle not found" );
			return responseMap;
		}

		// calculate and store the result of topic modeling
		// True/False for Static or Dynamic View
		topicModelingService.calculateCircleTopicModeling( circle, isReplaceExistingResult, false );

		// get JSON represent CircleTOpicModelingProfile
		List<Object> topicModelingResults = topicModelingService.getCircleTopicModeliFromDatabase( circle );

		if ( topicModelingResults == null || topicModelingResults.isEmpty() )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "no interest profile found" );
			return responseMap;
		}

		responseMap.put( "status", "ok" );
		responseMap.put( "topicModel", topicModelingResults );

		return responseMap;
	}

	/**
	 * This is the main method that will be used to extract the topic in the
	 * form of Ngrams The result of the method will be <String, String> Where
	 * the second one is composed of topic -_- %
	 */
	@Override
	public Map<String, Object> getStaticTopicModelingNgrams( String circleId, boolean isReplaceExistingResult )
	{
		// create JSON container for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		Circle circle = persistenceStrategy.getCircleDAO().getById( circleId );

		if ( circle == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "circle not found" );
			return responseMap;
		}

		// calculate and store the result of topic modeling
		topicModelingService.calculateCircleTopicModeling( circle, isReplaceExistingResult, true );

		// get JSON represent CircleTOpicModelingProfile
		List<Object> topicModelingResults = topicModelingService.getStaticCircleTopicModelingFromDatabase( circle );

		if ( topicModelingResults == null || topicModelingResults.isEmpty() )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "no topic model profile found" );
			return responseMap;
		}

		responseMap.put( "status", "ok" );
		responseMap.put( "topicModel", topicModelingResults );

		return responseMap;
	}

	// /**
	// *
	// * @param circled
	// * @param maxRetrieve
	// * @return
	// */
	// public Map<String, Object> getResearcherExtractedTopicsById( String
	// circleId, String maxRetrieve, boolean isReplaceExistingResult )
	// {
	// // create JSON mapper for response
	// Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
	//
	// // default term retrieved
	// int maxTermRetrieve = 20;
	//
	// if ( maxRetrieve != null )
	// {
	// try
	// {
	// maxTermRetrieve = Integer.parseInt( maxRetrieve );
	// }
	// catch ( InputMismatchException exception )
	// {
	// logger.error( "Invalid input" );
	// maxTermRetrieve = 20;
	// }
	// }
	//
	// // get circle
	// Circle circle = persistenceStrategy.getCircleDAO().getById( circleId );
	// if ( circle == null )
	// {
	// responseMap.put( "status", "Error - circle not found" );
	// return responseMap;
	// }
	//
	// // calculate and store the result of topic modeling
	// topicModelingService.calculateCircleTopicModelingResults( circle,
	// maxRetrieve, isReplaceExistingResult );
	//
	// // get JSON represent CircleTOpicModelingProfile
	// List<Object> topicModelingResults =
	// topicModelingService.getCircleTopicModeliFromDatabase( circle );
	//
	// if ( topicModelingResults == null || topicModelingResults.isEmpty() )
	// {
	// responseMap.put( "status", "error" );
	// responseMap.put( "statusMessage", "no interest profile found" );
	// return responseMap;
	// }
	//
	// responseMap.put( "status", "ok" );
	// responseMap.put( "topicModel", topicModelingResults );
	//
	// return responseMap;
	// }
}

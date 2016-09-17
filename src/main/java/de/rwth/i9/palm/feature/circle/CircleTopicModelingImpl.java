package de.rwth.i9.palm.feature.circle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.interestmining.service.TopicModelingService;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class CircleTopicModelingImpl implements CircleTopicModeling
{
	@Autowired
	private PalmAnalytics palmAnalytics;

	@Autowired
	private TopicModelingService topicModelingService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	private String path = "C:/Users/Albi/Desktop/";

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

	@Override
	public Map<String, Object> getTopicModelUniCloud( Circle circle, boolean isReplaceResult )
	{
		// researchers list container
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// circle details
		Map<String, String> circledetail = new HashMap<String, String>();
		circledetail.put( "id", circle.getId().toString() );
		circledetail.put( "name", circle.getName().toString() );

		// algorithm result
		HashMap<String, Double> topiccomposition = new LinkedHashMap<String, Double>();
		topiccomposition = palmAnalytics.getNGrams().runweightedTopicComposition( path, "Circle-Test", circle.getId().toString(), 5, 5, 5, true, true );

		if ( topiccomposition.isEmpty() != true )
		{
			responseMap.put( "status", "Ok" );
		}

		responseMap.put( "circle", circledetail );

		// add the circle id to the map
		responseMap.put( "circle", circle.getId().toString() );

		// hold the temporal results from the algorithm

		List<LinkedHashMap<String, Object>> topicList = new ArrayList<LinkedHashMap<String, Object>>();

		for ( Entry<String, Double> topic : topiccomposition.entrySet() )
		{
			Map<String, Object> topicdistribution = new LinkedHashMap<String, Object>();
			topicdistribution.put( topic.getKey().toString(), topic.getValue() );
			// topicdistribution.put( "size", topic.getValue() );
			topicList.add( (LinkedHashMap<String, Object>) topicdistribution );
		}
		responseMap.put( "termvalue", topicList );

		return responseMap;
	}

	@Override
	public Map<String, Object> getTopicModelNCloud( Circle circle, boolean isReplaceResult )
	{
		// researchers list container
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// circle details
		Map<String, String> circledetail = new HashMap<String, String>();
		circledetail.put( "id", circle.getId().toString() );
		circledetail.put( "name", circle.getName().toString() );

		// algorithm result
		HashMap<String, Double> topiccomposition = new LinkedHashMap<String, Double>();
		topiccomposition = palmAnalytics.getNGrams().runweightedTopicComposition( path, "Circle-Test", circle.getId().toString(), 5, 5, 5, true, false );

		if ( topiccomposition.isEmpty() != true )
		{
			responseMap.put( "status", "Ok" );
		}

		responseMap.put( "circle", circledetail );

		// add the circle id to the map
		// responseMap.put( "circle", circle.getId().toString() );

		// hold the temporal results from the algorithm

		List<LinkedHashMap<String, Object>> topicList = new ArrayList<LinkedHashMap<String, Object>>();

		for ( Entry<String, Double> topic : topiccomposition.entrySet() )
		{
			Map<String, Object> topicdistribution = new LinkedHashMap<String, Object>();
			topicdistribution.put( topic.getKey().toString(), topic.getValue() );
			// topicdistribution.put( "size", topic.getValue() );
			topicList.add( (LinkedHashMap<String, Object>) topicdistribution );

		}
		responseMap.put( "termvalue", topicList );

		return responseMap;
	}

	@Override
	public Map<String, Object> getCircleTopicEvolutionTest( Circle circle )
	{
		// researchers list container
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		HashMap<String, List<String>> topicevolution = new LinkedHashMap<String, List<String>>();

		// getEvolutionofTopicOverTime( 0, 5, false );
		topicevolution = (HashMap<String, List<String>>) palmAnalytics.getNGrams().runDiscreteTopicEvolution( path, "Circle-Year-Test", circle.getId().toString(), 5, 5, 5, true, false, false );
		// Prepare set of similarCircle HashSet;
		List<LinkedHashMap<String, Object>> topicList = new ArrayList<LinkedHashMap<String, Object>>();

		// We are interested on the evolution of only the 10 topics
		String[] colors = { "0efff8", "ff7f0e", "0eff7f", "ffa70e", "ff7f5a", "d4991c", "ad937c", "ff430e", "ff0e8e", "0e8eff" };
		int col = 0;
		for ( Entry<String, List<String>> topic : topicevolution.entrySet() )
		{
			LinkedHashMap<String, Object> evolutionMap = new LinkedHashMap<String, Object>();
			LinkedHashMap<String, String> yearproportion = new LinkedHashMap<String, String>();
			for ( String proportions : topic.getValue() )
				yearproportion.put( proportions.split( "_-_" )[0], proportions.split( "_-_" )[1] );
			evolutionMap.put( "values", yearproportion );
			evolutionMap.put( "key", topic.getKey() );
			evolutionMap.put( "color", colors[col] );
			if ( col < colors.length )
			{
				col++;
			}
			else
			{
				col = 0;
			}

			topicList.add( evolutionMap );
		}

		// prepare list of object map containing similarCircle details
		List<Map<String, Object>> idk = new ArrayList<Map<String, Object>>();

		for ( Map<String, Object> individaltopics : topicList )
		{
			idk.add( individaltopics );
		}

		// put similarCircle to responseMap
		responseMap.put( "termvalues", idk );

		return responseMap;
	}

	@Override
	public Map<String, Object> getSimilarCirclesMap( Circle circle, int startPage, int maxresult )
	{
		// researchers list container
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		List<String> similarCircles = new ArrayList<String>();
		similarCircles = palmAnalytics.getNGrams().runSimilarEntities( circle.getId().toString(), "C:/Users/Albi/Desktop/", "Circles", 20, 4, 3, true );
		// similarEntities( circle.getId(), maxresult, 3 );

		// Prepare set of similarCircle HashSet;
		Set<String> similarcircleSet = new HashSet<String>();

		for ( String similar : similarCircles )
		{
			if ( persistenceStrategy.getCircleDAO().getById( similar.split( "->" )[0] ).equals( circle ) )
				continue;

			similarcircleSet.add( similar );
		}

		// prepare list of object map containing similarCircle details
		List<Map<String, Object>> similarCircleList = new ArrayList<Map<String, Object>>();

		for ( String similarCircle : similarcircleSet )
		{
			// only copy necessary attributes
			// persistenceStrategy.( similar.split( "->"
			// )[0] )
			Map<String, Object> similarCircleMap = new LinkedHashMap<String, Object>();
			similarCircleMap.put( "id", similarCircle.split( "->" )[0] );
			similarCircleMap.put( "name", persistenceStrategy.getCircleDAO().getById( similarCircle.split( "->" )[0] ).getName() );
			similarCircleMap.put( "similarity", similarCircle.split( "->" )[1] );
			// add into list
			similarCircleList.add( similarCircleMap );
		}

		// prepare list of object map containing similarCircle details
		List<Map<String, Object>> similarCircleListPaging = new ArrayList<Map<String, Object>>();

		int position = 0;
		for ( Map<String, Object> similarCircle : similarCircleList )
		{
			if ( position >= startPage && similarCircleListPaging.size() < maxresult )
			{
				similarCircleListPaging.add( similarCircle );
			}
		}

		// remove unnecessary result

		// put similarCircle to responseMap
		responseMap.put( "countTotal", similarCircleList.size() );
		responseMap.put( "count", similarCircleListPaging.size() );
		responseMap.put( "similarCircles", similarCircleListPaging );

		return responseMap;
	}
}

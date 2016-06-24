package de.rwth.i9.palm.feature.academicevent;

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
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class EventTopicModelingImpl implements EventTopicModeling
{

	@Autowired
	private PalmAnalytics palmAnalytics;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	private String path = "C:/Users/Piro/Desktop/";

	/**
	 * This is the first method used to show the widget Simple LDA
	 * implementation String eventId boolean isReplaceExistingResult
	 */
	@Override
	public Map<String, Object> getTopicModeling( String eventId, boolean isReplaceExistingResult )
	{
		return null;
	}

	@Override
	public Map<String, Object> getStaticTopicModelingNgrams( String eventId, boolean isReplaceExistingResult )
	{

		// Create JSON map with the responses
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// Handle the status entry on the result
		if ( !eventId.isEmpty() )
		{
			responseMap.put( "status", "ok" );
		}
		else
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "Event not found" );
			return responseMap;
		}

		// create the List with objects which will hold the profiles
		List<Object> topicModel = new ArrayList<Object>();

		// create HashMap to hold the result and profileName for each algorithm
		Map<String, Object> algorithmResultUniGrams = new LinkedHashMap<String, Object>();
		Map<String, Object> algorithmResultNGrams = new LinkedHashMap<String, Object>();

		// add the profile names on the respective map
		algorithmResultUniGrams.put( "profile", "Unigrams" );
		algorithmResultNGrams.put( "profile", "Ngrams" );

		// loop over all the results of algorithm and put the elements in List
		for ( Entry<String, List<String>> topics : palmAnalytics.getNGrams().runTopicComposition( eventId, path, "Event-Test", 5, 5, 5, false, true, true ).entrySet() )
		{
			List<Object> termValueResult = new ArrayList<Object>();
			// Expected only one entry in this map with eventId
			for ( String topicDetails : topics.getValue() )
			{
				List<Object> termvalueUnigram = new ArrayList<Object>();
				termvalueUnigram.add( topicDetails.split( "_-_" )[0] );
				termvalueUnigram.add( Double.parseDouble( topicDetails.split( "_-_" )[1] ) );
				termValueResult.add( termvalueUnigram );
			}
			algorithmResultUniGrams.put( "termvalue", termValueResult );
		}

		// add the unigrams into the topicModel list
		topicModel.add( algorithmResultUniGrams );

		for ( Entry<String, List<String>> topics : palmAnalytics.getNGrams().runTopicComposition( eventId, path, "Event-Test", 5, 5, 5, false, true, false ).entrySet() )
		{
			List<Object> termValueResult = new ArrayList<Object>();
			// expacted only one entry in this map with eventId
			for ( String topicDetails : topics.getValue() )
			{
				List<Object> termvalueNgram = new ArrayList<Object>();
				termvalueNgram.add( topicDetails.split( "_-_" )[0] );
				termvalueNgram.add( Double.parseDouble( topicDetails.split( "_-_" )[1] ) );
				termValueResult.add( termvalueNgram );
			}
			algorithmResultNGrams.put( "termvalue", termValueResult );
		}

		// add the ngrams into the topicModel list
		topicModel.add( algorithmResultNGrams );

		// add the result of topic Modeling into the result Map
		responseMap.put( "topicModel", topicModel );

		return responseMap;
	}

	@Override
	public Map<String, Object> getTopicModelUniCloud( Event event, boolean isReplaceExistingResult )
	{
		// researchers list container
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// event details
		Map<String, String> eventdetail = new HashMap<String, String>();
		eventdetail.put( "id", event.getId().toString() );
		eventdetail.put( "name", event.getName().toString() );

		// algorithm result
		HashMap<String, Double> topiccomposition = new LinkedHashMap<String, Double>();
		topiccomposition = palmAnalytics.getNGrams().runweightedTopicComposition( path, "Event-Test", event.getId().toString(), 5, 5, 5, true, true );

		if ( topiccomposition.isEmpty() != true )
		{
			responseMap.put( "status", "Ok" );
		}
		else
		{
			responseMap.put( "status", "No Topics discovered" );
		}

		responseMap.put( "event", eventdetail );

		// add the event id to the map
		responseMap.put( "event", event.getId().toString() );

		// hold the temporal results from the algorithm

		List<LinkedHashMap<String, Object>> topicList = new ArrayList<LinkedHashMap<String, Object>>();

		for ( Entry<String, Double> topic : topiccomposition.entrySet() )
		{
			Map<String, Object> topicdistribution = new LinkedHashMap<String, Object>();
			topicdistribution.put( topic.getKey().toString(), topic.getValue() );
			// topicdistribution.put( "size", topic.getValue() );
			topicList.add( (LinkedHashMap<String, Object>) topicdistribution );

			// TO DO Unigrams/Ngrams preferences
		}
		responseMap.put( "termvalue", topicList );

		return responseMap;
	}

	@Override
	public Map<String, Object> getTopicModelNCloud( Event event, boolean isReplaceExistingResult )
	{
		// researchers list container
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// event details
		Map<String, String> eventdetail = new HashMap<String, String>();
		eventdetail.put( "id", event.getId().toString() );
		eventdetail.put( "name", event.getName().toString() );

		// algorithm result
		HashMap<String, Double> topiccomposition = new LinkedHashMap<String, Double>();
		topiccomposition = palmAnalytics.getNGrams().runweightedTopicComposition( path, "Event-Test", event.getId().toString(), 5, 5, 5, true, false );

		if ( topiccomposition.isEmpty() != true )
		{
			responseMap.put( "status", "Ok" );
		}
		else
		{
			responseMap.put( "status", "No Topics discovered" );
		}

		responseMap.put( "event", eventdetail );

		// add the event id to the map
		// responseMap.put( "event", event.getId().toString() );

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
	public Map<String, Object> getSimilarEventsMap( Event event, int startPage, int maxresult )
	{
		// researchers list container
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		List<String> similarEvents = new ArrayList<String>();
		similarEvents = palmAnalytics.getNGrams().runSimilarEntities( event.getId().toString(), "C:/Users/Piro/Desktop/", "Conferences", 20, 10, 3, true );
		// similarEntities( event.getId(), maxresult, 3 );

		// Prepare set of similarEvent HashSet;
		Set<String> similareventSet = new HashSet<String>();

		for ( String similar : similarEvents )
		{
			// if ( persistenceStrategy.getEventDAO().getById( similar.split(
			// "->" )[0] ).equals( event ) )
			// continue;

			similareventSet.add( similar );
		}

		// prepare list of object map containing similarEvent details
		List<Map<String, Object>> similarEventList = new ArrayList<Map<String, Object>>();

		for ( String similarEvent : similareventSet )
		{
			// only copy necessary attributes
			// persistenceStrategy.( similar.split( "->"
			// )[0] )
			Map<String, Object> similarEventMap = new LinkedHashMap<String, Object>();
			similarEventMap.put( "id", similarEvent.split( "->" )[0] );
			if ( !persistenceStrategy.getEventDAO().getById( similarEvent.split( "->" )[0] ).getName().isEmpty() )
				similarEventMap.put( "name", persistenceStrategy.getEventDAO().getById( similarEvent.split( "->" )[0] ).getName() );
			similarEventMap.put( "similarity", similarEvent.split( "->" )[1] );
			// add into list
			similarEventList.add( similarEventMap );
		}

		// prepare list of object map containing similarEvent details
		List<Map<String, Object>> similarEventListPaging = new ArrayList<Map<String, Object>>();

		int position = 0;
		for ( Map<String, Object> similarEvent : similarEventList )
		{
			if ( position >= startPage && similarEventListPaging.size() < maxresult )
			{
				similarEventListPaging.add( similarEvent );
			}
		}

		// remove unnecessary result

		// put similarEvent to responseMap
		responseMap.put( "countTotal", similarEventList.size() );
		responseMap.put( "count", similarEventListPaging.size() );
		responseMap.put( "similarEvents", similarEventListPaging );

		return responseMap;
	}

}

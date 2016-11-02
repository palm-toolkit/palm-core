package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;

@Component
public class GroupVisualizationImpl implements GroupVisualization
{
	@Autowired
	private ClusteringService clusteringService;

	private ObjectMapper mapper = new ObjectMapper();

	@SuppressWarnings( "unchecked" )
	@Override
	public Map<String, Object> visualizeResearchersGroup( String type, List<Author> authorList, List<String> idsList, Set<Publication> publications, String startYear, String endYear )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();
		Map<String, Object> clusteringResultMap = clusteringService.clusterAuthors( "xmeans", authorList, idsList, publications, type, startYear, endYear );
		Map<String, List<String>> clusterTerms = (Map<String, List<String>>) clusteringResultMap.get( "clusterTerms" );
		Map<String, List<String>> nodeTerms = (Map<String, List<String>>) clusteringResultMap.get( "nodeTerms" );
		Map<String, Integer> mapClusterAuthor = (Map<String, Integer>) clusteringResultMap.get( "clusterMap" );
		if ( mapClusterAuthor != null )
		{
			Iterator<Integer> clusterIterator = mapClusterAuthor.values().iterator();
			List<Integer> clusters = new ArrayList<Integer>();
			while ( clusterIterator.hasNext() )
			{
				clusters.add( clusterIterator.next() );
			}

			// 1st Level Keys i.e information about author
			Iterator<String> objectsIterator = mapClusterAuthor.keySet().iterator();
			List<String> names = new ArrayList<String>();
			List<String> ids = new ArrayList<String>();
			Object jsonObject;
			String jsonString;
			Map<String, String> mapValues = new LinkedHashMap<String, String>();
			while ( objectsIterator.hasNext() )
			{
				String objectString = objectsIterator.next();
				try
				{
					jsonObject = mapper.readValue( objectString, Object.class );
					jsonString = mapper.writeValueAsString( jsonObject );
					mapValues = (Map<String, String>) mapper.readValue( jsonString, Object.class );
				}
				catch ( Exception e )
				{
					e.printStackTrace();
				}
				Iterator<String> iterator = mapValues.values().iterator();
				while ( iterator.hasNext() )
				{
					ids.add( iterator.next() );
					names.add( iterator.next() );
				}
			}

			List<Map<String, Object>> authors = new ArrayList<Map<String, Object>>();
			for ( int i = 0; i < clusters.size(); i++ )
			{
				Map<String, Object> visMapTemp = new LinkedHashMap<String, Object>();
				visMapTemp.put( "id", ids.get( i ) );
				visMapTemp.put( "name", names.get( i ) );
				visMapTemp.put( "cluster", clusters.get( i ) );
				visMapTemp.put( "nodeTerms", nodeTerms.get( ids.get( i ) ) );
				visMapTemp.put( "clusterTerms", clusterTerms.get( clusters.get( i ) ) );
				authors.add( visMapTemp );
			}
			visMap.put( "coauthors", authors );
			return visMap;
		}
		else
			return (Map<String, Object>) visMap.put( "coauthors", "none" );

	}

	@SuppressWarnings( "unchecked" )
	@Override
	public Map<String, Object> visualizePublicationsGroup( String type, List<Author> authorList, Set<Publication> publications )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();
		Map<String, Object> clusteringResultMap = clusteringService.clusterPublications( "xmeans", publications );
		Map<String, List<String>> clusterTerms = (Map<String, List<String>>) clusteringResultMap.get( "clusterTerms" );
		Map<String, List<String>> nodeTerms = (Map<String, List<String>>) clusteringResultMap.get( "nodeTerms" );
		Map<String, Integer> mapClusterPublication = (Map<String, Integer>) clusteringResultMap.get( "clusterMap" );
		if ( mapClusterPublication != null )
		{
			Iterator<Integer> clusterIterator = mapClusterPublication.values().iterator();
			List<Integer> clusters = new ArrayList<Integer>();
			while ( clusterIterator.hasNext() )
			{
				clusters.add( clusterIterator.next() );
			}

			// 1st Level Keys i.e information about author
			Iterator<String> objectsIterator = mapClusterPublication.keySet().iterator();
			List<String> names = new ArrayList<String>();
			List<String> ids = new ArrayList<String>();
			Object jsonObject;
			String jsonString;
			Map<String, String> mapValues = new LinkedHashMap<String, String>();
			while ( objectsIterator.hasNext() )
			{
				String objectString = objectsIterator.next();
				try
				{
					jsonObject = mapper.readValue( objectString, Object.class );
					jsonString = mapper.writeValueAsString( jsonObject );
					mapValues = (Map<String, String>) mapper.readValue( jsonString, Object.class );
				}
				catch ( Exception e )
				{
					e.printStackTrace();
				}
				Iterator<String> iterator = mapValues.values().iterator();
				while ( iterator.hasNext() )
				{
					ids.add( iterator.next() );
					names.add( iterator.next() );
				}
			}

			List<Map<String, Object>> publicationsList = new ArrayList<Map<String, Object>>();
			for ( int i = 0; i < clusters.size(); i++ )
			{
				Map<String, Object> responseMapTemp = new LinkedHashMap<String, Object>();
				responseMapTemp.put( "id", ids.get( i ) );
				responseMapTemp.put( "name", names.get( i ) );
				responseMapTemp.put( "cluster", clusters.get( i ) );
				responseMapTemp.put( "nodeTerms", nodeTerms.get( ids.get( i ) ) );
				responseMapTemp.put( "clusterTerms", clusterTerms.get( clusters.get( i ) ) );
				publicationsList.add( responseMapTemp );
			}
			// System.out.println( publicationsList.toString() );
			visMap.put( "publications", publicationsList );
			return visMap;
		}
		else
			return (Map<String, Object>) visMap.put( "publications", "none" );

	}

	@SuppressWarnings( "unchecked" )
	@Override
	public Map<String, Object> visualizeConferencesGroup( String type, List<Author> authorList, Set<Publication> publications )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();
		Map<String, Object> clusteringResultMap = clusteringService.clusterConferences( "xmeans", authorList, publications );
		Map<String, List<String>> clusterTerms = (Map<String, List<String>>) clusteringResultMap.get( "clusterTerms" );
		Map<String, List<String>> nodeTerms = (Map<String, List<String>>) clusteringResultMap.get( "nodeTerms" );
		Map<String, Integer> mapClusterConference = (Map<String, Integer>) clusteringResultMap.get( "clusterMap" );
		if ( mapClusterConference != null )
		{
			Iterator<Integer> clusterIterator = mapClusterConference.values().iterator();
			List<Integer> clusters = new ArrayList<Integer>();
			while ( clusterIterator.hasNext() )
			{
				clusters.add( clusterIterator.next() );
			}

			// 1st Level Keys i.e information about author
			Iterator<String> objectsIterator = mapClusterConference.keySet().iterator();
			List<String> names = new ArrayList<String>();
			List<String> ids = new ArrayList<String>();
			List<String> abrs = new ArrayList<String>();
			Object jsonObject;
			String jsonString;
			Map<String, String> mapValues = new LinkedHashMap<String, String>();
			while ( objectsIterator.hasNext() )
			{
				String objectString = objectsIterator.next();
				try
				{
					jsonObject = mapper.readValue( objectString, Object.class );
					jsonString = mapper.writeValueAsString( jsonObject );
					mapValues = (Map<String, String>) mapper.readValue( jsonString, Object.class );
				}
				catch ( Exception e )
				{
					e.printStackTrace();
				}
				Iterator<String> iterator = mapValues.values().iterator();
				System.out.println( mapValues.values().toString() );
				while ( iterator.hasNext() )
				{
					ids.add( iterator.next() );
					names.add( iterator.next() );
					abrs.add( iterator.next() );
				}
			}

			List<Map<String, Object>> conferences = new ArrayList<Map<String, Object>>();
			for ( int i = 0; i < clusters.size(); i++ )
			{
				Map<String, Object> responseMapTemp = new LinkedHashMap<String, Object>();
				responseMapTemp.put( "id", ids.get( i ) );
				responseMapTemp.put( "name", names.get( i ) );
				responseMapTemp.put( "abr", abrs.get( i ) );
				responseMapTemp.put( "cluster", clusters.get( i ) );
				responseMapTemp.put( "nodeTerms", nodeTerms.get( ids.get( i ) ) );
				responseMapTemp.put( "clusterTerms", clusterTerms.get( clusters.get( i ) ) );
				conferences.add( responseMapTemp );
			}
			visMap.put( "conferences", conferences );
			return visMap;
		}
		else
			return (Map<String, Object>) visMap.put( "conferences", "none" );

	}

	@Override
	public Map<String, Object> visualizeTopicsGroup( String type, List<Author> authorList, Set<Publication> publications )
	{
		// TODO Auto-generated method stub
		return null;
	}

}
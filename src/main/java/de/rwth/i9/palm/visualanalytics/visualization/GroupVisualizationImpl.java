package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;

@Component
public class GroupVisualizationImpl implements GroupVisualization
{
	@Autowired
	private ClusteringService clusteringService;

	private ObjectMapper mapper = new ObjectMapper();

	@SuppressWarnings( "unchecked" )
	@Override
	public Map<String, Object> visualizeResearchersGroup( String type, String visType, List<String> idsList, Set<Publication> publications, String startYear, String endYear, String yearFilterPresent, List<Interest> filteredTopic, List<String> repeatCallList, String algo, String seedVal, String noOfClustersVal, String foldsVal, String iterationsVal, HttpServletRequest request )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			Map<String, Object> clusteringResultMap = clusteringService.clusterAuthors( algo, idsList, publications, type, visType, startYear, endYear, request, yearFilterPresent, filteredTopic, repeatCallList, seedVal, noOfClustersVal, foldsVal, iterationsVal );
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
				visMap.put( "dataSet", clusteringResultMap.get( "dataSet" ) );
				visMap.put( "algo", clusteringResultMap.get( "algo" ) );
				visMap.put( "seedVal", clusteringResultMap.get( "seedVal" ) );
				visMap.put( "noOfClustersVal", clusteringResultMap.get( "noOfClustersVal" ) );
				visMap.put( "foldsVal", clusteringResultMap.get( "foldsVal" ) );
				visMap.put( "iterationsVal", clusteringResultMap.get( "iterationsVal" ) );
			}
			return visMap;
		}
		else
			return (Map<String, Object>) visMap.put( "coauthors", "none" );

	}

	@SuppressWarnings( "unchecked" )
	@Override
	public Map<String, Object> visualizePublicationsGroup( String type, String visType, Set<Publication> publications, List<String> repeatCallList, String algo, String seedVal, String noOfClustersVal, String foldsVal, String iterationsVal, HttpServletRequest request )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) )
		{
			Map<String, Object> clusteringResultMap = clusteringService.clusterPublications( algo, publications, type, visType, repeatCallList, seedVal, noOfClustersVal, foldsVal, iterationsVal );
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
				visMap.put( "publications", publicationsList );
				visMap.put( "dataSet", clusteringResultMap.get( "dataSet" ) );
				visMap.put( "algo", clusteringResultMap.get( "algo" ) );
				visMap.put( "seedVal", clusteringResultMap.get( "seedVal" ) );
				visMap.put( "noOfClustersVal", clusteringResultMap.get( "noOfClustersVal" ) );
				visMap.put( "foldsVal", clusteringResultMap.get( "foldsVal" ) );
				visMap.put( "iterationsVal", clusteringResultMap.get( "iterationsVal" ) );
			}
			return visMap;
		}
		else
			return (Map<String, Object>) visMap.put( "publications", "none" );

	}

	@SuppressWarnings( "unchecked" )
	@Override
	public Map<String, Object> visualizeConferencesGroup( String type, String visType, Set<Publication> publications, List<Interest> filteredTopic, List<String> idsList, List<String> repeatCallList, String algo, String seedVal, String noOfClustersVal, String foldsVal, String iterationsVal, HttpServletRequest request )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();
		System.out.println( request.getSession().getAttribute( "objectType" ) );
		System.out.println( type );
		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) )
		{
			System.out.println( "coming in group" );
			Map<String, Object> clusteringResultMap = clusteringService.clusterConferences( algo, publications, filteredTopic, type, visType, idsList, repeatCallList, seedVal, noOfClustersVal, foldsVal, iterationsVal );
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
				visMap.put( "dataSet", clusteringResultMap.get( "dataSet" ) );
				visMap.put( "algo", clusteringResultMap.get( "algo" ) );
				visMap.put( "seedVal", clusteringResultMap.get( "seedVal" ) );
				visMap.put( "noOfClustersVal", clusteringResultMap.get( "noOfClustersVal" ) );
				visMap.put( "foldsVal", clusteringResultMap.get( "foldsVal" ) );
				visMap.put( "iterationsVal", clusteringResultMap.get( "iterationsVal" ) );
			}
			return visMap;
		}
		else
			return (Map<String, Object>) visMap.put( "conferences", "none" );

	}

	@Override
	public Map<String, Object> visualizeTopicsGroup( String type, Set<Publication> publications, HttpServletRequest request )
	{
		return null;
	}

}

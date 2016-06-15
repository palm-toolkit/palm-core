package de.rwth.i9.palm.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.interestmining.service.InterestMiningService;
import de.rwth.i9.palm.model.DataMiningAuthor;
import de.rwth.i9.palm.model.DataMiningPublication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@RestController
@RequestMapping( value = "/analytics" )
public class AnalyticsRestController
{
	@Autowired
	private PalmAnalytics palmAnalytics;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private InterestMiningService interestMiningService;

	private ObjectMapper mapper = new ObjectMapper();


	@RequestMapping( "/clustering/{type}" )
	@Transactional
	public String simpleKMeans( @PathVariable("type") String objectType,
			@RequestParam( value = "algorithm", defaultValue = "kmeans" ) String algorithm, @RequestParam( value = "relatedObjectType", required = false ) String relatedObjectType, @RequestParam( value = "relatedObjectId", required = false ) String relatedObjectId, @RequestParam( value = "algorithmParameters", defaultValue = "4" ) String algorithmParameters
			) throws Exception
	{	
		Map<String, Integer> resultMap;

		switch ( objectType ) {
		case "authors":
			resultMap = clusterAuthors( algorithm, relatedObjectId, relatedObjectType, algorithmParameters );
			break;
		case "publications":
			resultMap = clusterPublications( algorithm, relatedObjectId, relatedObjectType );
			break;
		default:
			resultMap = clusterAuthors( algorithm, relatedObjectId, relatedObjectType, algorithmParameters );
			break;
		}

		return mapper.writeValueAsString( resultMap );
	}

	private Map<String, Integer> clusterPublications( String algorithm, String relatedObjectId, String relatedObjectType ) throws Exception
	{
		Map<DataMiningPublication, Integer> clustering = palmAnalytics.getClustering().clusterPublications( persistenceStrategy, algorithm, relatedObjectId, relatedObjectType );
		Map<String, Integer> resultMap = new HashMap<String, Integer>();
		for(DataMiningPublication p : clustering.keySet()) {
			resultMap.put( mapper.writeValueAsString( p.getJsonStub() ), clustering.get( p ) );
		}
		return resultMap;
	}

	private Map<String, Integer> clusterAuthors( String algorithm, String relatedObjectId, String relatedObjectType, String algorithmParameters ) throws Exception
	{
		Map<DataMiningAuthor, Integer> clustering = palmAnalytics.getClustering().clusterAuthors( persistenceStrategy, algorithm, relatedObjectId, relatedObjectType, algorithmParameters );
		Map<String, Integer> resultMap = new HashMap<String, Integer>();
		for ( DataMiningAuthor a : clustering.keySet() )
		{
			resultMap.put( mapper.writeValueAsString( a.getJsonStub() ), clustering.get( a ) );
		}
		return resultMap;

	}
}
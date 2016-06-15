package de.rwth.i9.palm.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rwth.i9.palm.analytics.algorithm.clustering.WekaSimpleKMeans;
import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.feature.researcher.ResearcherFeature;
import de.rwth.i9.palm.interestmining.service.InterestMiningService;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.DataMiningPublication;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

@RestController
@RequestMapping( value = "/analytics" )
public class AnalyticsRestController
{
	@Autowired
	private PalmAnalytics palmAnalytics;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private ResearcherFeature researcherFeature;

	@Autowired
	private InterestMiningService interestMiningService;

	private ObjectMapper mapper = new ObjectMapper();

	@RequestMapping( "/clustering/{type}" )
	@Transactional
	public String simpleKMeans( @PathVariable("type") String objectType,
			@RequestParam( value = "algorithm", defaultValue = "kmeans" ) String algorithm, @RequestParam( value = "relatedObjectType", required = false ) String relatedObjectType, @RequestParam( value = "relatedObjectId", required = false ) String relatedObjectId
			) throws Exception
	{	
		Map<String, Integer> resultMap;

		switch ( objectType ) {
		case "authors":
			resultMap = clusterAuthors( algorithm, relatedObjectId, relatedObjectType );
			break;
		case "publications":
			resultMap = clusterPublications( algorithm, relatedObjectId, relatedObjectType );
			break;
		default:
			resultMap = clusterAuthors( algorithm, relatedObjectId, relatedObjectType);
			break;
		}

		return mapper.writeValueAsString( resultMap );
	}

	private Map<String, Integer> clusterPublications( String algorithm, String relatedObjectId, String relatedObjectType ) throws Exception
	{
		Map<DataMiningPublication, Integer> clustering = palmAnalytics.getClustering().clusterPublications( persistenceStrategy, algorithm, relatedObjectId, relatedObjectType );
		Map<String, Integer> resultMap = new HashMap<String, Integer>();
		for(DataMiningPublication p : clustering.keySet()) {
			System.out.println( p.getTitle() + ", " + p.getId() );
			resultMap.put( mapper.writeValueAsString( p.getJsonStub() ), clustering.get( p ) );
		}
		return resultMap;
	}

	private Map<String, Integer> clusterAuthors( String algorithm, String relatedObjectId, String relatedObjectType ) throws Exception
	{
		Map<String, Integer> resultMap = new HashMap<String, Integer>();

		List<Author> authors;
		System.out.println( "Clustering Authors" );
		if ( relatedObjectId != null && relatedObjectType != null )
		{
			switch ( relatedObjectType ) {
			case "event":
				authors = persistenceStrategy.getAuthorDAO().getAuthorByEventId( relatedObjectId );
				break;
			case "publication":
				Publication publication = persistenceStrategy.getPublicationDAO().getById( relatedObjectId );
				authors = publication.getAuthors();
				break;
			default:
				authors = persistenceStrategy.getAuthorDAO().getAuthorByEventId( relatedObjectId );
			}
		}
		else
		{
			authors = persistenceStrategy.getAuthorDAO().getAdded();
		}

		if ( authors.size() > 0 )
		{
			ArrayList<Attribute> attributes = new ArrayList<Attribute>();
			Attribute citedBy = new Attribute( "citedBy" );
			attributes.add( citedBy );
			Attribute noPublication = new Attribute( "noPublication" );
			attributes.add( noPublication );

			Instances data = new Instances( "authors", attributes, authors.size() );

			for ( Author a : authors )
			{
				Instance i = new DenseInstance( attributes.size() );
				i.setValue( citedBy, a.getCitedBy() );
				i.setValue( noPublication, a.getNoPublication() );
				data.add( i );
				Map<String, Object> m = new LinkedHashMap<String, Object>();
				interestMiningService.getInterestFromDatabase( a, m );
			}

			if ( algorithm.equals( "kmeans" ) )
			{
				SimpleKMeans result = WekaSimpleKMeans.run( 4, data );
				int i = 0;
				for ( int clusterNum : result.getAssignments() )
				{
					resultMap.put( mapper.writeValueAsString( authors.get( i ).getJsonStub() ), clusterNum );
					i++;
				}
			}
		}

		return resultMap;

	}
}
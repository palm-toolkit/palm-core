package de.rwth.i9.palm.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rwth.i9.palm.analytics.algorithm.clustering.WekaEM;
import de.rwth.i9.palm.analytics.algorithm.clustering.WekaSimpleKMeans;
import de.rwth.i9.palm.analytics.algorithm.clustering.WekaXMeans;
import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.feature.researcher.ResearcherFeature;
import de.rwth.i9.palm.interestmining.service.InterestMiningService;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorInterest;
import de.rwth.i9.palm.model.AuthorInterestProfile;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventInterest;
import de.rwth.i9.palm.model.EventInterestProfile;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationTopic;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import weka.clusterers.EM;
import weka.clusterers.SimpleKMeans;
import weka.clusterers.XMeans;
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
	public String cluster( @PathVariable( "type" ) String objectType, @RequestParam( value = "algorithm", defaultValue = "kmeans" ) String algorithm, @RequestParam( value = "relatedObjectType", required = false ) String relatedObjectType, @RequestParam( value = "relatedObjectId", required = false ) String relatedObjectId ) throws Exception
	{
		Map<String, Integer> resultMap;
		Map<String, Object> combinedMap = new HashMap<String, Object>();

		switch ( objectType ) {
		case "authors":
			combinedMap = clusterAuthors( algorithm, relatedObjectId, relatedObjectType );
			return mapper.writeValueAsString( combinedMap );

		// break;
		case "publications":
			combinedMap = clusterPublications( algorithm, relatedObjectId, relatedObjectType );
			return mapper.writeValueAsString( combinedMap );

		// break;
		default:
			combinedMap = clusterAuthors( algorithm, relatedObjectId, relatedObjectType );
			return mapper.writeValueAsString( combinedMap );

		// break;
		}

	}

	private Map<String, Object> clusterPublications( String algorithm, String relatedObjectId, String relatedObjectType ) throws Exception
	{
		Map<String, Object> combinedMap = new HashMap<String, Object>();
		Map<String, Integer> resultMap = new HashMap<String, Integer>();
		Map<String, List<String>> clusterAttributes = new HashMap<String, List<String>>();

		List<Publication> publications;
		if ( relatedObjectId != null && relatedObjectType != null )
		{
			switch ( relatedObjectType ) {
			case "event":
				publications = persistenceStrategy.getPublicationDAO().getPublicationByEventId( relatedObjectId );
				break;
			case "author":
				publications = persistenceStrategy.getPublicationDAO().getPublicationByAuthorId( relatedObjectId );
				break;
			default:
				publications = persistenceStrategy.getPublicationDAO().getPublicationByEventId( relatedObjectId );
				break;
			}
		}
		else
		{
			publications = persistenceStrategy.getPublicationDAO().getAll();
		}

		if ( publications.size() > 0 )
		{
			ArrayList<Attribute> attributes = new ArrayList<Attribute>();
			System.out.println( "No. of Publications " + publications.size() );

			List<String> allTerms = new ArrayList<String>();

			for ( Publication p : publications )
			{

				Iterator<PublicationTopic> it = p.getPublicationTopics().iterator();
				while ( it.hasNext() )
				{
					PublicationTopic topic = it.next();
					Map<String, Double> topicMap = topic.getTermValues();
					Iterator<String> topicTerms = topicMap.keySet().iterator();
					Iterator<Double> topicTermValues = topicMap.values().iterator();
					while ( topicTerms.hasNext() && topicTermValues.hasNext() )
					{
						String term = topicTerms.next();

						if ( !allTerms.contains( term ) )
						{
							allTerms.add( term );
							attributes.add( new Attribute( term ) );
							// System.out.println( "term: " + term + " " +
							// "weight: " + topicTermValues.next() );

						}
					}
				}
			}

			Instances data = new Instances( "publications", attributes, publications.size() );

			for ( Publication p : publications )
			{
				List<String> publicationTerms = new ArrayList<String>();
				List<Double> publicationTermWeights = new ArrayList<Double>();
				Iterator<PublicationTopic> it = p.getPublicationTopics().iterator();
				Instance i = new DenseInstance( attributes.size() );
				// System.out.println( "\n " + p.getTitle() );
				while ( it.hasNext() )
				{
					PublicationTopic publicationTopic = it.next();
					Map<String, Double> topicMap = publicationTopic.getTermValues();
					Iterator<String> topicTerms = topicMap.keySet().iterator();
					Iterator<Double> topicTermValues = topicMap.values().iterator();
					while ( topicTerms.hasNext() && topicTermValues.hasNext() )
					{
						String term = topicTerms.next();
						// System.out.println( term );
						Double weight = topicTermValues.next();
						if ( !publicationTerms.contains( term ) )
						{
							publicationTerms.add( term );
							publicationTermWeights.add( weight );
						}
					}
				}

				for ( int s = 0; s < allTerms.size(); s++ )
				{
					if ( publicationTerms.contains( allTerms.get( s ) ) )
					{
						i.setValue( attributes.get( s ), publicationTermWeights.get( publicationTerms.indexOf( allTerms.get( s ) ) ) );
					}
					else
					{
						i.setValue( attributes.get( s ), 0 );
					}
				}

				data.add( i );
			}

			if ( algorithm.equals( "kmeans" ) )
			{
				SimpleKMeans result = WekaSimpleKMeans.run( 10, data );
				int i = 0;
				System.out.println( "No. of Publication Clusters : " + result.getNumClusters() );
				for ( int clusterNum : result.getAssignments() )
				{
					resultMap.put( mapper.writeValueAsString( publications.get( i ).getJsonStub() ), clusterNum );
					i++;
				}
			}

			if ( algorithm.equals( "EM" ) )
			{
				EM result = WekaEM.run( 4, data );
				System.out.println( "No. of Publication Clusters : " + result.getNumClusters() );
				for ( int ind = 0; ind < data.size(); ind++ )
				{
					resultMap.put( mapper.writeValueAsString( publications.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}
			}


			if ( algorithm.equals( "xmeans" ) )
			{
				XMeans result = WekaXMeans.run( 10, data );
				System.out.println( "No. of Publication Clusters : " + result.numberOfClusters() );
				Instances clusterCenters = result.getClusterCenters();

				for ( int ind = 0; ind < data.size(); ind++ )
				{
					resultMap.put( mapper.writeValueAsString( publications.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}
				for ( int i = 0; i < clusterCenters.numInstances(); i++ )
				{

					System.out.println( "CLUSTER number :" + i );

				    // for each cluster center
					Instance inst = clusterCenters.instance( i );
					List<Integer> attributeWeights = new ArrayList<Integer>();
					for ( int j = 0; j < inst.numAttributes(); j++ )
					{
						attributeWeights.add( 0 );
					}
					List<Instance> dataInstances = new ArrayList<Instance>();
					for ( int ind = 0; ind < data.size(); ind++ )
					{
						if ( result.clusterInstance( data.get( ind ) ) == i )
						{
							dataInstances.add( data.get( ind ) );
						}
					}

					System.out.println( "size of data instances : : " + dataInstances.size() );

					List<String> attributeNames = new ArrayList<String>();
					for ( int j = 0; j < inst.numAttributes(); j++ )
					{

						for ( int k = 0; k < dataInstances.size(); k++ )
						{
							if ( dataInstances.get( k ).value( j ) > 0.0 )
							{
								if ( attributeWeights.get( j ) > 0 )
								{
									System.out.println( inst.attribute( j ).name() + ": " + attributeWeights.get( j ) + ":weight" );
								}
									attributeWeights.set( j, attributeWeights.get( j ) + 1 );
							}
						}

					}
					System.out.println( attributeWeights );
					int maxIndex = attributeWeights.indexOf( Collections.max( attributeWeights ) );
					attributeNames.add( attributes.get( maxIndex ).name() );

					clusterAttributes.put( "" + i + "", attributeNames );
				}

			}
		}

		combinedMap.put( "clusters", resultMap );
		combinedMap.put( "clusterCenters", clusterAttributes );

		return combinedMap;

	}

	private Map<String, Object> clusterAuthors( String algorithm, String relatedObjectId, String relatedObjectType ) throws Exception
	{
		Map<String, Integer> resultMap = new HashMap<String, Integer>();
		Map<String, Object> combinedMap = new HashMap<String, Object>();

		Map<String, List<String>> clusterAttributes = new HashMap<String, List<String>>();

		List<Author> authors;
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
			System.out.println( "No. of authors " + authors.size() );

			List<Interest> allInterests = new ArrayList<Interest>();

			String eventId = "";

			if ( relatedObjectType.equals( "event" ) )
			{
				eventId = relatedObjectId;
			}

			Event event = persistenceStrategy.getEventDAO().getById( eventId );

			Set<EventInterestProfile> interestProfiles = event.getEventInterestProfiles();
			Iterator<EventInterestProfile> ip = interestProfiles.iterator();
			while ( ip.hasNext() )
			{
				EventInterestProfile interestProfile = ip.next();
				Iterator<EventInterest> eventInterests = interestProfile.getEventInterests().iterator();
				while ( eventInterests.hasNext() )
				{
					EventInterest eventInterest = eventInterests.next();
					Iterator<Interest> interestIterator = eventInterest.getTermWeights().keySet().iterator();
					Iterator<Double> weight = eventInterest.getTermWeights().values().iterator();
					while ( interestIterator.hasNext() && weight.hasNext() )
					{
						Interest interest = interestIterator.next();
						if ( !allInterests.contains( interest ) )
						{
							allInterests.add( interest );
							attributes.add( new Attribute( interest.getTerm() ) );
						}
					}
				}
			}


			System.out.println( "no. of attributes in authors: : " + attributes.size() );

			Instances data = new Instances( "authors", attributes, authors.size() );

			for ( Author a : authors )
			{
				List<Interest> authorInterests = new ArrayList<Interest>();
				List<Double> authorInterestWeights = new ArrayList<Double>();
				Instance i = new DenseInstance( attributes.size() );
				Iterator<AuthorInterestProfile> it = a.getAuthorInterestProfiles().iterator();

				while ( it.hasNext() )
				{
					Iterator<AuthorInterest> j = ( (AuthorInterestProfile) it.next() ).getAuthorInterests().iterator();
					while ( j.hasNext() )
					{
						Map<Interest, Double> p = ( j.next() ).getTermWeights();
						Iterator<Interest> k = p.keySet().iterator();
						Iterator<Double> kw = p.values().iterator();

						while ( k.hasNext() && kw.hasNext() )
						{
							Interest interest = ( (Interest) k.next() );
							Double weight = kw.next();

							if ( !authorInterests.contains( interest ) )
							{
								authorInterests.add( interest );
								authorInterestWeights.add( weight );
							}
						}
					}
				}

				for ( int s = 0; s < allInterests.size(); s++ )
				{
					if ( authorInterests.contains( allInterests.get( s ) ) )
					{
						i.setValue( attributes.get( s ), authorInterestWeights.get( authorInterests.indexOf( allInterests.get( s ) ) ) );
					}
					else
					{
						i.setValue( attributes.get( s ), 0 );
					}
				}

				data.add( i );
			}

			if ( algorithm.equals( "kmeans" ) )
			{
				SimpleKMeans result = WekaSimpleKMeans.run( 10, data );
				int i = 0;
				System.out.println( "No. of clusters: " + result.getNumClusters() );
				for ( int clusterNum : result.getAssignments() )
				{
					resultMap.put( mapper.writeValueAsString( authors.get( i ).getJsonStub() ), clusterNum );
					i++;
				}
			}

			if ( algorithm.equals( "EM" ) )
			{
				EM result = WekaEM.run( 4, data );
				System.out.println( result.numberOfClusters() );
				for ( int ind = 0; ind < data.size(); ind++ )
				{
					resultMap.put( mapper.writeValueAsString( authors.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}
			}

			if ( algorithm.equals( "xmeans" ) )
			{
				XMeans result = WekaXMeans.run( 10, data );
				System.out.println( "No. of Author Clusters : " + result.numberOfClusters() );

				Instances clusterCenters = result.getClusterCenters();

				for ( int ind = 0; ind < data.size(); ind++ )
				{
					resultMap.put( mapper.writeValueAsString( authors.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}
				for ( int i = 0; i < clusterCenters.numInstances(); i++ )
				{

					System.out.println( "CLUSTER number :" + i );

					// for each cluster center
					Instance inst = clusterCenters.instance( i );
					List<Integer> attributeWeights = new ArrayList<Integer>();
					for ( int j = 0; j < inst.numAttributes(); j++ )
					{
						attributeWeights.add( 0 );
					}
					List<Instance> dataInstances = new ArrayList<Instance>();
					for ( int ind = 0; ind < data.size(); ind++ )
					{
						if ( result.clusterInstance( data.get( ind ) ) == i )
						{
							dataInstances.add( data.get( ind ) );
						}
					}

					System.out.println( "size of data instances : : " + dataInstances.size() );

					List<String> attributeNames = new ArrayList<String>();
					for ( int j = 0; j < inst.numAttributes(); j++ )
					{

						for ( int k = 0; k < dataInstances.size(); k++ )
						{
							if ( dataInstances.get( k ).value( j ) > 0.0 )
							{
								if ( attributeWeights.get( j ) > 0 )
								{
									System.out.println( inst.attribute( j ).name() + ": " + attributeWeights.get( j ) + ":weight" );
								}
								attributeWeights.set( j, attributeWeights.get( j ) + 1 );
							}
						}

					}
					System.out.println( attributeWeights );
					int maxIndex = attributeWeights.indexOf( Collections.max( attributeWeights ) );
					attributeNames.add( attributes.get( maxIndex ).name() );

					clusterAttributes.put( "" + i + "", attributeNames );

			}

		}

	}
		combinedMap.put( "clusters", resultMap );
		combinedMap.put( "clusterCenters", clusterAttributes );

		return combinedMap;
	}
}

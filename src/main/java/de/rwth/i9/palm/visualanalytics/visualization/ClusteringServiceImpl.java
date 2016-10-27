package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rwth.i9.palm.analytics.algorithm.clustering.WekaDBSCAN;
import de.rwth.i9.palm.analytics.algorithm.clustering.WekaEM;
import de.rwth.i9.palm.analytics.algorithm.clustering.WekaHierarchichal;
import de.rwth.i9.palm.analytics.algorithm.clustering.WekaXMeans;
import de.rwth.i9.palm.analytics.util.InterestParser;
import de.rwth.i9.palm.helper.MapSorter;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.DataMiningAuthor;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.EventInterest;
import de.rwth.i9.palm.model.EventInterestProfile;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationTopic;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import weka.clusterers.DBSCAN;
import weka.clusterers.EM;
import weka.clusterers.HierarchicalClusterer;
import weka.clusterers.XMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

@Service
public class ClusteringServiceImpl implements ClusteringService
{
	long startTime = System.currentTimeMillis();
	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	WekaDBSCAN wekaDBSCAN;

	@Autowired
	WekaHierarchichal hierarchicalClusterer;

	@Autowired
	WekaEM eM;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Override
	public Map<String, Object> clusterAuthors( String algorithm, List<Author> authorList, Set<Publication> authorPublications, String type )
	{
		long startTime = System.currentTimeMillis();
		Map<String, Integer> clusterMap = new HashMap<String, Integer>();
		Map<String, Object> resultMap = new HashMap<String, Object>();
		Map<Integer, List<String>> clusterTerms = new HashMap<Integer, List<String>>();
		Map<String, List<String>> nodeTerms = new HashMap<String, List<String>>();

		// Now find coauthors from these publications
		List<Author> coAuthorList = new ArrayList<Author>();

		for ( Publication publication : authorPublications )
		{
			if ( type.equals( "publication" ) )
			{
				List<Author> commonAuthors = new ArrayList<Author>();
				List<Integer> count = new ArrayList<Integer>();
				for ( Publication p : authorPublications )
				{
					for ( Author a : p.getAuthors() )
					{
						if ( !commonAuthors.contains( a ) )
						{
							commonAuthors.add( a );
							count.add( 1 );
						}
						else
						{
							int index = commonAuthors.indexOf( a );
							count.set( index, count.get( index ) + 1 );
						}
					}
				}

				for ( int i = 0; i < count.size(); i++ )
				{
					if ( count.get( i ) < authorPublications.size() )
					{
						count.remove( i );
						commonAuthors.remove( i );
						i--;
					}
				}
				coAuthorList = commonAuthors;
			}
			else
			{
				for ( Author a : publication.getAuthors() )
				{
					if ( a.isAdded() )
					{
						// just skip if its one of the authors in consideration
						if ( authorList.contains( a ) )
							continue;

						if ( !coAuthorList.contains( a ) )
							coAuthorList.add( a );
					}
				}
			}
		}

		Long midTime = ( System.currentTimeMillis() - startTime ) / 1000;
		System.out.println( "Step 1: " + midTime );

		List<DataMiningAuthor> authors = persistenceStrategy.getAuthorDAO().getDataMiningObjects(); // all
																									// authors
																									// in
																									// database
		List<DataMiningAuthor> mainAuthors = new ArrayList<DataMiningAuthor>();
		List<DataMiningAuthor> coAuthors = new ArrayList<DataMiningAuthor>();
		for ( DataMiningAuthor dma : authors )
		{
			for ( int i = 0; i < authorList.size(); i++ )
			{
				if ( dma.getName().equals( authorList.get( i ).getName() ) )
					mainAuthors.add( dma );
			}
			for ( int i = 0; i < coAuthorList.size(); i++ )
			{
				if ( dma.getName().equals( coAuthorList.get( i ).getName() ) )
					coAuthors.add( dma );
			}
		}

		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		List<String> allInterests = new ArrayList<String>();

		for ( DataMiningAuthor a : coAuthors )
		{
			Map<String, Double> interests = InterestParser.parseInterestString( a.getAuthor_interest_flat().getInterests() );

			Iterator<String> interestTerm = interests.keySet().iterator();
			Iterator<Double> interestTermWeight = interests.values().iterator();
			while ( interestTerm.hasNext() && interestTermWeight.hasNext() )
			{
				String interest = ( interestTerm.next() );
				if ( !allInterests.contains( interest ) )
				{
					allInterests.add( interest );
					attributes.add( new Attribute( interest ) );
				}
			}
		}

		System.out.println( " All interests: " + allInterests.size() );
		midTime = ( System.currentTimeMillis() - startTime ) / 1000;

		// Assign interests of authors to topics of publications..
		Instances data = new Instances( "authors", attributes, coAuthors.size() );

		System.out.println( "size of coauth: " + coAuthors.size() );
		for ( DataMiningAuthor a : coAuthors )
		{
			System.out.println( "\n " + a.getName() );
			Instance i = new DenseInstance( attributes.size() );
			List<String> authorInterests = new ArrayList<String>();
			List<Double> authorInterestWeights = new ArrayList<Double>();
			Map<String, Double> interests = new HashMap<String, Double>();
			interests = InterestParser.parseInterestString( a.getAuthor_interest_flat().getInterests() );
			interests = MapSorter.sortByValue( interests );

			System.out.println( interests.toString() );

			int count = 0;
			List<String> authorTopInterests = new ArrayList<String>();
			Iterator<String> interestTerm = interests.keySet().iterator();
			Iterator<Double> interestTermWeight = interests.values().iterator();
			while ( interestTerm.hasNext() && interestTermWeight.hasNext() )
			{
				String interest = ( interestTerm.next() );
				Double weight = interestTermWeight.next();
				if ( !authorInterests.contains( interest ) )
				{
					authorInterests.add( interest );
					authorInterestWeights.add( weight );
					if ( count < 8 )
						authorTopInterests.add( interest );
					count++;
				}

			}

			nodeTerms.put( a.getId(), authorTopInterests );

			// check if author interests are present in the topic list, if
			// yes,
			// their weights are taken into account for clustering
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

		System.out.println( " NODE TERMS:  " + nodeTerms.size() );
		System.out.println( "size of data: " + data.size() );
		midTime = ( System.currentTimeMillis() - startTime ) / 1000;
		System.out.println( "Step 3: " + midTime );

		try
		{
			// applying the clustering algorithm
			if ( algorithm.equals( "xmeans" ) )
			{
				XMeans result = WekaXMeans.run( 2, data );
				for ( int ind = 0; ind < data.size(); ind++ )
				{
					clusterMap.put( mapper.writeValueAsString( coAuthors.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}

				Instances instances = result.getClusterCenters();
				for ( int instanceCounter = 0; instanceCounter < instances.size(); instanceCounter++ )
				{
					List<Double> max = new ArrayList<Double>();
					List<Integer> maxIndex = new ArrayList<Integer>();
					List<String> terms = new ArrayList<String>();
					Integer numAttr = instances.get( instanceCounter ).numAttributes();
					for ( int i = 0; i < numAttr; i++ )
					{
						if ( max.size() < 4 )
						{
							max.add( instances.get( instanceCounter ).value( i ) );
							maxIndex.add( i );
							terms.add( instances.get( instanceCounter ).attribute( maxIndex.get( i ) ).name() );

						}
						else
						{
							for ( int j = 0; j < max.size(); j++ )
							{
								if ( instances.get( instanceCounter ).value( i ) > max.get( j ) )
								{
									max.set( j, instances.get( instanceCounter ).value( i ) );
									maxIndex.set( j, i );
									terms.set( j, instances.get( instanceCounter ).attribute( i ).name() );
									break;
								}
							}
						}
					}

					clusterTerms.put( instanceCounter, terms );
				}

			}
			midTime = ( System.currentTimeMillis() - startTime ) / 1000;
			System.out.println( "Step 4: " + midTime );

			// applying the clustering algorithm
			if ( algorithm.equals( "DBSCAN" ) )
			{
				DBSCAN result = wekaDBSCAN.run( data );

				for ( int ind = 0; ind < data.size(); ind++ )
				{
					resultMap.put( mapper.writeValueAsString( coAuthorList.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}
			}

			// applying the clustering algorithm
			if ( algorithm.equals( "Hierarchical" ) )
			{
				HierarchicalClusterer result = hierarchicalClusterer.run( data );

				for ( int ind = 0; ind < data.size(); ind++ )
				{
					resultMap.put( mapper.writeValueAsString( coAuthorList.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}
			}

			// applying the clustering algorithm
			if ( algorithm.equals( "EM" ) )
			{
				EM result = eM.run( data );

				for ( int ind = 0; ind < data.size(); ind++ )
				{
					resultMap.put( mapper.writeValueAsString( coAuthorList.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}
			}
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		resultMap.put( "clusterMap", clusterMap );
		resultMap.put( "clusterTerms", clusterTerms );
		resultMap.put( "nodeTerms", nodeTerms );
		return resultMap;
	}

	// CLUSTER AS PER DATA MINING OBJECT PENDING!!!!!
	@Override
	public Map<String, Object> clusterConferences( String algorithm, List<Author> authorList, Set<Publication> publications )
	{
		Map<String, Integer> clusterMap = new HashMap<String, Integer>();
		Map<String, Object> resultMap = new HashMap<String, Object>();
		Map<Integer, List<String>> clusterTerms = new HashMap<Integer, List<String>>();
		Map<String, List<String>> nodeTerms = new HashMap<String, List<String>>();

		// Find topics from publications - This will govern clustering
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		List<String> allTopics = new ArrayList<String>();
		for ( Publication pub : publications )
		{
			Set<PublicationTopic> publicationTopics = pub.getPublicationTopics();
			for ( PublicationTopic pubTopic : publicationTopics )
			{
				List<Double> topicWeights = new ArrayList<Double>( pubTopic.getTermValues().values() );
				List<String> topics = new ArrayList<String>( pubTopic.getTermValues().keySet() );
				for ( int i = 0; i < topics.size(); i++ )
				{
					if ( !allTopics.contains( topics.get( i ) ) && topicWeights.get( i ) > 0.2 )
					{
						allTopics.add( topics.get( i ) );
						attributes.add( new Attribute( topics.get( i ) ) );
					}
				}
			}
		}

		List<Publication> publicationsList = new ArrayList<Publication>( publications );

		List<Event> events = new ArrayList<Event>();

		for ( int i = 0; i < publicationsList.size(); i++ )
		{
			Event event = publicationsList.get( i ).getEvent();

			if ( event != null )
			{
				if ( !events.contains( event ) )
				{
					events.add( event );
				}
			}
		}

		// Assign interests of authors to topics of publications..
		Instances data = new Instances( "authors", attributes, events.size() );
		List<EventGroup> eventGroups = new ArrayList<EventGroup>();
		List<List<String>> eventGroupInterests = new ArrayList<List<String>>();
		List<List<Double>> eventGroupInterestWeights = new ArrayList<List<Double>>();

		for ( Event e : events )
		{
			Set<EventInterestProfile> eventInterestProfiles = e.getEventInterestProfiles();
			EventGroup eventGroup = e.getEventGroup();
			if ( !eventGroups.contains( eventGroup ) )
			{
				eventGroups.add( eventGroup );
				eventGroupInterests.add( new ArrayList<String>() );
				eventGroupInterestWeights.add( new ArrayList<Double>() );
			}
			for ( EventInterestProfile eip : eventInterestProfiles )
			{
				Set<EventInterest> eis = eip.getEventInterests();
				for ( EventInterest ei : eis )
				{
					Map<Interest, Double> interests = ei.getTermWeights();
					Iterator<Interest> interestTerm = interests.keySet().iterator();
					Iterator<Double> interestTermWeight = interests.values().iterator();

					while ( interestTerm.hasNext() && interestTermWeight.hasNext() )
					{
						String interest = ( interestTerm.next().getTerm() );
						Double weight = interestTermWeight.next();
						Map<String, Double> temp = new HashMap<String, Double>();
						temp.put( interest, weight );

						int groupIndex = eventGroups.indexOf( eventGroup );
						List<String> groupInterests = eventGroupInterests.get( groupIndex );
						List<Double> groupInterestWeights = eventGroupInterestWeights.get( groupIndex );

						if ( !groupInterests.contains( interest ) )
						{
							groupInterests.add( interest );
							groupInterestWeights.add( weight );
						}
						else
							groupInterestWeights.add( groupInterests.indexOf( interest ), groupInterestWeights.get( groupInterests.indexOf( interest ) ) + weight );
					}

				}
			}
		}
		for ( int g = 0; g < eventGroups.size(); g++ )
		{
			Instance i = new DenseInstance( attributes.size() );

			// check if author interests are present in the topic list, if yes,
			// their weights are taken into account for clustering
			for ( int s = 0; s < allTopics.size(); s++ )
			{
				if ( eventGroupInterests.get( g ).contains( allTopics.get( s ) ) )
				{
					i.setValue( attributes.get( s ), 1 );
				}
				else
				{
					i.setValue( attributes.get( s ), 0 );
				}
			}

			data.add( i );

		}

		try
		{
			// applying the clustering algorithm
			if ( algorithm.equals( "xmeans" ) )
			{
				XMeans result = WekaXMeans.run( 2, data );

				for ( int ind = 0; ind < data.size(); ind++ )
				{
					clusterMap.put( mapper.writeValueAsString( eventGroups.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}

				Instances instances = result.getClusterCenters();
				for ( int instanceCounter = 0; instanceCounter < instances.size(); instanceCounter++ )
				{
					List<Double> max = new ArrayList<Double>();
					List<Integer> maxIndex = new ArrayList<Integer>();
					List<String> terms = new ArrayList<String>();
					Integer numAttr = instances.get( instanceCounter ).numAttributes();
					for ( int i = 0; i < numAttr; i++ )
					{

						if ( max.size() < 4 )
						{
							max.add( instances.get( instanceCounter ).value( i ) );
							maxIndex.add( i );
							terms.add( instances.get( instanceCounter ).attribute( maxIndex.get( i ) ).name() );

						}
						else
						{
							for ( int j = 0; j < max.size(); j++ )
							{
								if ( instances.get( instanceCounter ).value( i ) > max.get( j ) )
								{
									max.set( j, instances.get( instanceCounter ).value( i ) );
									maxIndex.set( j, i );
									terms.set( j, instances.get( instanceCounter ).attribute( i ).name() );
									break;
								}
							}
						}
					}

					clusterTerms.put( instanceCounter, terms );
					// for ( int i = 0; i < 4; i++ )
					// System.out.println( max.get( i ) + " : " + instances.get(
					// instanceCounter ).attribute( maxIndex.get( i ) ) );
					// System.out.println( "\n" );
				}

			}
			// applying the clustering algorithm
			if ( algorithm.equals( "DBSCAN" ) )
			{
				DBSCAN result = wekaDBSCAN.run( data );

				for ( int ind = 0; ind < data.size(); ind++ )
				{
					resultMap.put( mapper.writeValueAsString( eventGroups.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}
			}

			// applying the clustering algorithm
			if ( algorithm.equals( "Hierarchical" ) )
			{
				HierarchicalClusterer result = hierarchicalClusterer.run( data );

				for ( int ind = 0; ind < data.size(); ind++ )
				{
					resultMap.put( mapper.writeValueAsString( eventGroups.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}
			}

			// applying the clustering algorithm
			if ( algorithm.equals( "EM" ) )
			{
				EM result = eM.run( data );

				for ( int ind = 0; ind < data.size(); ind++ )
				{
					resultMap.put( mapper.writeValueAsString( eventGroups.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}
			}

		}
		catch ( Exception e )
		{
			System.out.println( e );
		}

		resultMap.put( "clusterMap", clusterMap );
		resultMap.put( "clusterTerms", clusterTerms );
		resultMap.put( "nodeTerms", nodeTerms );
		return resultMap;
	}

	// CLUSTER AS PER DATA MINING OBJECT PENDING!!!!!
	public Map<String, Object> clusterPublications( String algorithm, Set<Publication> publications )
	{
		List<Publication> publicationsList = new ArrayList<Publication>( publications );

		Map<String, Integer> clusterMap = new HashMap<String, Integer>();
		Map<String, Object> resultMap = new HashMap<String, Object>();
		Map<Integer, List<String>> clusterTerms = new HashMap<Integer, List<String>>();
		Map<String, List<String>> nodeTerms = new HashMap<String, List<String>>();
		// Find topics from publications - This will govern clustering
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		List<String> allTopics = new ArrayList<String>();
		for ( Publication pub : publications )
		{
			Set<PublicationTopic> publicationTopics = pub.getPublicationTopics();
			for ( PublicationTopic pubTopic : publicationTopics )
			{
				List<Double> topicWeights = new ArrayList<Double>( pubTopic.getTermValues().values() );
				List<String> topics = new ArrayList<String>( pubTopic.getTermValues().keySet() );
				for ( int i = 0; i < topics.size(); i++ )
				{
					if ( !allTopics.contains( topics.get( i ) ) && topicWeights.get( i ) > 0.2 )
					{
						allTopics.add( topics.get( i ) );
						attributes.add( new Attribute( topics.get( i ) ) );
					}
				}
			}
		}
		// Assign interests of authors to topics of publications..
		Instances data = new Instances( "publications", attributes, publications.size() );

		for ( Publication p : publications )
		{
			List<String> publicationTopics = new ArrayList<String>();
			List<Double> publicationTopicWeights = new ArrayList<Double>();
			Instance i = new DenseInstance( attributes.size() );
			Set<PublicationTopic> authorInterestProfiles = p.getPublicationTopics();
			for ( PublicationTopic pt : authorInterestProfiles )
			{
				List<Double> topicWeights = new ArrayList<Double>( pt.getTermValues().values() );
				List<String> topics = new ArrayList<String>( pt.getTermValues().keySet() );
				for ( int j = 0; j < topics.size(); j++ )
				{
					if ( !publicationTopics.contains( topics.get( j ) ) )
					{
						publicationTopics.add( topics.get( j ) );
						publicationTopicWeights.add( topicWeights.get( j ) );
					}
				}
			}
			// check if author interests are present in the topic list, if yes,
			// their weights are taken into account for clustering
			for ( int s = 0; s < allTopics.size(); s++ )
			{
				if ( publicationTopics.contains( allTopics.get( s ) ) )
				{
					i.setValue( attributes.get( s ), publicationTopicWeights.get( publicationTopics.indexOf( allTopics.get( s ) ) ) );
				}
				else
				{
					i.setValue( attributes.get( s ), 0 );
				}
			}
			data.add( i );
		}
		try
		{
			// applying the clustering algorithm
			if ( algorithm.equals( "xmeans" ) )
			{
				System.out.println( "in xmeans" );
				XMeans result = WekaXMeans.run( 2, data );

				for ( int ind = 0; ind < data.size(); ind++ )
				{
					clusterMap.put( mapper.writeValueAsString( publicationsList.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}

				Instances instances = result.getClusterCenters();
				for ( int instanceCounter = 0; instanceCounter < instances.size(); instanceCounter++ )
				{
					List<Double> max = new ArrayList<Double>();
					List<Integer> maxIndex = new ArrayList<Integer>();
					List<String> terms = new ArrayList<String>();
					Integer numAttr = instances.get( instanceCounter ).numAttributes();
					for ( int i = 0; i < numAttr; i++ )
					{

						if ( max.size() < 4 )
						{
							max.add( instances.get( instanceCounter ).value( i ) );
							maxIndex.add( i );
							terms.add( instances.get( instanceCounter ).attribute( maxIndex.get( i ) ).name() );

						}
						else
						{
							for ( int j = 0; j < max.size(); j++ )
							{
								if ( instances.get( instanceCounter ).value( i ) > max.get( j ) )
								{
									max.set( j, instances.get( instanceCounter ).value( i ) );
									maxIndex.set( j, i );
									terms.set( j, instances.get( instanceCounter ).attribute( i ).name() );
									break;
								}
							}
						}
					}

					clusterTerms.put( instanceCounter, terms );
					// for ( int i = 0; i < 4; i++ )
					// System.out.println( max.get( i ) + " : " + instances.get(
					// instanceCounter ).attribute( maxIndex.get( i ) ) );
					// System.out.println( "\n" );
				}

			}
			// applying the clustering algorithm
			if ( algorithm.equals( "DBSCAN" ) )
			{
				DBSCAN result = wekaDBSCAN.run( data );

				for ( int ind = 0; ind < data.size(); ind++ )
				{
					resultMap.put( mapper.writeValueAsString( publicationsList.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}
			}

			// applying the clustering algorithm
			if ( algorithm.equals( "Hierarchical" ) )
			{
				HierarchicalClusterer result = hierarchicalClusterer.run( data );

				for ( int ind = 0; ind < data.size(); ind++ )
				{
					resultMap.put( mapper.writeValueAsString( publicationsList.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}
			}

			// applying the clustering algorithm
			if ( algorithm.equals( "EM" ) )
			{
				EM result = eM.run( data );

				for ( int ind = 0; ind < data.size(); ind++ )
				{
					resultMap.put( mapper.writeValueAsString( publicationsList.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}
			}
		}
		catch ( Exception e )
		{
			System.out.println( e );
		}

		System.out.println( clusterMap.size() + " .. " + clusterMap.size() );
		resultMap.put( "clusterMap", clusterMap );
		resultMap.put( "clusterTerms", clusterTerms );
		resultMap.put( "nodeTerms", nodeTerms );

		return resultMap;
	}

}

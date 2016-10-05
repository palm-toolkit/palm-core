package de.rwth.i9.palm.cluster.service;

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
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorInterest;
import de.rwth.i9.palm.model.AuthorInterestProfile;
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
	public Map<String, Integer> clusterAuthors( String algorithm, List<Author> authorList, Set<Publication> authorPublications )
	{
		long startTime = System.currentTimeMillis();
		Map<String, Integer> resultMap = new HashMap<String, Integer>();

		// Now find coauthors from these publications
		List<Author> coAuthorList = new ArrayList<Author>();

		// number of collaboration
		for ( Publication publication : authorPublications )
		{
			for ( Author coAuthor : publication.getAuthors() )
			{
				if ( coAuthor.isAdded() )
				{
					// just skip if its one of the authors in consideration
					if ( authorList.contains( coAuthor ) )
						continue;

					if ( !coAuthorList.contains( coAuthor ) )
						coAuthorList.add( coAuthor );
				}
			}
		}

		Long midTime = ( System.currentTimeMillis() - startTime ) / 1000;
		System.out.println( "Step 1: " + midTime );
		// Find topics from publications - This will govern clustering
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		List<String> allTopics = new ArrayList<String>();
		for ( Publication pub : authorPublications )
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
		midTime = ( System.currentTimeMillis() - startTime ) / 1000;
		System.out.println( "Step 2: " + midTime );
		// Assign interests of authors to topics of publications..
		Instances data = new Instances( "authors", attributes, coAuthorList.size() );

		// List<Author> allAuthors =
		// persistenceStrategy.getAuthorDAO().getAllAuthors();

		for ( Author a : coAuthorList )
		{
			List<String> authorInterests = new ArrayList<String>();
			List<Double> authorInterestWeights = new ArrayList<Double>();
			Instance i = new DenseInstance( attributes.size() );
			Set<AuthorInterestProfile> authorInterestProfiles = a.getAuthorInterestProfiles();
			for ( AuthorInterestProfile aip : authorInterestProfiles )
			{
				Set<AuthorInterest> ais = aip.getAuthorInterests();
				for ( AuthorInterest ai : ais )
				{
					Map<Interest, Double> interests = ai.getTermWeights();
					Iterator<Interest> interestTerm = interests.keySet().iterator();
					Iterator<Double> interestTermWeight = interests.values().iterator();
					while ( interestTerm.hasNext() && interestTermWeight.hasNext() )
					{
						String interest = ( interestTerm.next().getTerm() );
						Double weight = interestTermWeight.next();

						if ( !authorInterests.contains( interest ) )
						{
							authorInterests.add( interest );
							authorInterestWeights.add( weight );
						}
					}

				}
			}

			// check if author interests are present in the topic list, if yes,
			// their weights are taken into account for clustering
			for ( int s = 0; s < allTopics.size(); s++ )
			{
				if ( authorInterests.contains( allTopics.get( s ) ) )
				{
					i.setValue( attributes.get( s ), authorInterestWeights.get( authorInterests.indexOf( allTopics.get( s ) ) ) );
				}
				else
				{
					i.setValue( attributes.get( s ), 0 );
				}
			}
			data.add( i );

		}
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
					resultMap.put( mapper.writeValueAsString( coAuthorList.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
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
			System.out.println( e );
		}
		return resultMap;
	}

	@Override
	public Map<String, Integer> clusterConferences( String algorithm, List<Author> authorList, Set<Publication> publications )
	{
		Map<String, Integer> resultMap = new HashMap<String, Integer>();

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
					// i.setValue( attributes.get( s ),
					// eventGroupInterestWeights.get( g ).get(
					// eventGroupInterests.get( g ).indexOf( allTopics.get( s )
					// ) ) );
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
					resultMap.put( mapper.writeValueAsString( eventGroups.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}
			}
		}
		catch ( Exception e )
		{
			System.out.println( e );
		}

		return resultMap;
	}

	public Map<String, Integer> clusterPublications( String algorithm, Set<Publication> publications )
	{
		List<Publication> publicationsList = new ArrayList<Publication>( publications );
		Map<String, Integer> resultMap = new HashMap<String, Integer>();

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
				XMeans result = WekaXMeans.run( 2, data );

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

		return resultMap;
	}

}

package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rwth.i9.palm.analytics.algorithm.clustering.WekaDBSCAN;
import de.rwth.i9.palm.analytics.algorithm.clustering.WekaEM;
import de.rwth.i9.palm.analytics.algorithm.clustering.WekaHierarchichal;
import de.rwth.i9.palm.analytics.algorithm.clustering.WekaXMeans;
import de.rwth.i9.palm.analytics.util.InterestParser;
import de.rwth.i9.palm.helper.MapSorter;
import de.rwth.i9.palm.helper.VADataFetcher;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.DataMiningAuthor;
import de.rwth.i9.palm.model.DataMiningEventGroup;
import de.rwth.i9.palm.model.DataMiningPublication;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationTopicFlat;
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

	@Autowired
	private VADataFetcher dataFetcher;

	@Override
	public Map<String, Object> clusterAuthors( String algorithm, List<String> idsList, Set<Publication> publications, String type, String startYear, String endYear, HttpServletRequest request, String yearFilterPresent )
	{
		Map<String, Object> resultMap = new HashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			Map<String, Integer> clusterMap = new HashMap<String, Integer>();
			Map<Integer, List<String>> clusterTerms = new HashMap<Integer, List<String>>();
			Map<String, List<String>> nodeTerms = new HashMap<String, List<String>>();

			// Now find coauthors from these publications
			Map<String, Object> map = dataFetcher.fetchCommonAuthors( type, publications, idsList, yearFilterPresent );
			@SuppressWarnings( "unchecked" )
			List<Author> commonAuthors = (List<Author>) map.get( "commonAuthors" );
			List<Author> coAuthorList = new ArrayList<Author>();

			// authors from selection
			List<Author> authorList = new ArrayList<Author>();
			for ( String id : idsList )
			{
				authorList.add( persistenceStrategy.getAuthorDAO().getById( id ) );
			}

			// authors apart from selected authors
			for ( Author coAuthor : commonAuthors )
			{
				// just skip if its one of the authors in consideration
				if ( authorList.contains( coAuthor ) )
					continue;
				else
					coAuthorList.add( coAuthor );
			}

			Long midTime = ( System.currentTimeMillis() - startTime ) / 1000;
			System.out.println( "Step 1: " + midTime );

			List<DataMiningAuthor> authors = persistenceStrategy.getAuthorDAO().getDataMiningObjects(); // all
																										// database
			List<DataMiningAuthor> coAuthors = new ArrayList<DataMiningAuthor>();
			for ( DataMiningAuthor dma : authors )
			{
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

			midTime = ( System.currentTimeMillis() - startTime ) / 1000;

			// Assign interests of authors to topics of publications..
			Instances data = new Instances( "authors", attributes, coAuthors.size() );

			for ( DataMiningAuthor a : coAuthors )
			{
				Instance i = new DenseInstance( attributes.size() );
				List<String> authorInterests = new ArrayList<String>();
				List<Double> authorInterestWeights = new ArrayList<Double>();
				Map<String, Double> interests = new HashMap<String, Double>();
				interests = InterestParser.parseInterestString( a.getAuthor_interest_flat().getInterests() );
				interests = MapSorter.sortByValue( interests );

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

			try
			{
				// applying the clustering algorithm
				if ( algorithm.equals( "xmeans" ) && attributes.size() > 0 )
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
		}
		return resultMap;
	}

	@Override
	public Map<String, Object> clusterConferences( String algorithm, Set<Publication> publications )
	{
		Map<String, Integer> clusterMap = new HashMap<String, Integer>();
		Map<String, Object> resultMap = new HashMap<String, Object>();
		Map<Integer, List<String>> clusterTerms = new HashMap<Integer, List<String>>();
		Map<String, List<String>> nodeTerms = new HashMap<String, List<String>>();

		List<DataMiningEventGroup> allConferences = persistenceStrategy.getEventGroupDAO().getDataMiningObjects();
		List<DataMiningEventGroup> conferencesFromSelection = new ArrayList<DataMiningEventGroup>();

		List<Publication> publicationsList = new ArrayList<Publication>( publications );
		List<EventGroup> eventGroups = new ArrayList<EventGroup>();

		for ( int i = 0; i < publicationsList.size(); i++ )
		{
			Event e = publicationsList.get( i ).getEvent();
			if ( e != null )
			{
				EventGroup eventGroup = e.getEventGroup();
				if ( eventGroup != null )
				{
					if ( !eventGroups.contains( eventGroup ) )
					{
						eventGroups.add( eventGroup );
					}
				}
			}
		}

		// Find DataMiningEvent Groups corresponding to the subset of
		// conferences
		for ( DataMiningEventGroup dmc : allConferences )
		{
			for ( EventGroup eg : eventGroups )
			{
				if ( dmc.getName().equals( eg.getName() ) )
					conferencesFromSelection.add( dmc );
			}
		}

		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		List<String> allInterests = new ArrayList<String>();

		for ( DataMiningEventGroup dmeg : conferencesFromSelection )
		{
			Map<String, Double> interests = InterestParser.parseInterestString( dmeg.getEventGroup_interest_flat().getInterests() );

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

		// Assign interests of authors to topics of publications..
		Instances data = new Instances( "eventGroups", attributes, conferencesFromSelection.size() );

		for ( DataMiningEventGroup eg : conferencesFromSelection )
		{
			Instance i = new DenseInstance( attributes.size() );
			List<String> eventGroupInterests = new ArrayList<String>();
			List<Double> eventGroupInterestWeights = new ArrayList<Double>();
			Map<String, Double> interests = new HashMap<String, Double>();
			interests = InterestParser.parseInterestString( eg.getEventGroup_interest_flat().getInterests() );
			interests = MapSorter.sortByValue( interests );

			int count = 0;
			List<String> eventGroupTopInterests = new ArrayList<String>();
			Iterator<String> interestTerm = interests.keySet().iterator();
			Iterator<Double> interestTermWeight = interests.values().iterator();
			while ( interestTerm.hasNext() && interestTermWeight.hasNext() )
			{
				String interest = ( interestTerm.next() );
				Double weight = interestTermWeight.next();
				if ( !eventGroupInterests.contains( interest ) )
				{
					eventGroupInterests.add( interest );
					eventGroupInterestWeights.add( weight );
					if ( count < 8 )
						eventGroupTopInterests.add( interest );
					count++;
				}

			}

			nodeTerms.put( eg.getId(), eventGroupTopInterests );

			// check if author interests are present in the topic list, if
			// yes,
			// their weights are taken into account for clustering
			for ( int s = 0; s < allInterests.size(); s++ )
			{
				if ( eventGroupInterests.contains( allInterests.get( s ) ) )
				{
					i.setValue( attributes.get( s ), eventGroupInterestWeights.get( eventGroupInterests.indexOf( allInterests.get( s ) ) ) );
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
			if ( algorithm.equals( "xmeans" ) && attributes.size() > 0 )
			{
				XMeans result = WekaXMeans.run( 2, data );

				for ( int ind = 0; ind < data.size(); ind++ )
				{
					clusterMap.put( mapper.writeValueAsString( conferencesFromSelection.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
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

	public Map<String, Object> clusterPublications( String algorithm, Set<Publication> publications )
	{
		List<Publication> publicationsList = new ArrayList<Publication>( publications );

		Map<String, Integer> clusterMap = new HashMap<String, Integer>();
		Map<String, Object> resultMap = new HashMap<String, Object>();
		Map<Integer, List<String>> clusterTerms = new HashMap<Integer, List<String>>();
		Map<String, List<String>> nodeTerms = new HashMap<String, List<String>>();

		List<DataMiningPublication> allPublications = persistenceStrategy.getPublicationDAO().getDataMiningObjects();
		List<DataMiningPublication> publicationsFromSelection = new ArrayList<DataMiningPublication>();

		// Find DataMiningEvent Groups corresponding to the subset of
		// conferences
		for ( DataMiningPublication dmp : allPublications )
		{
			for ( Publication p : publications )
			{
				if ( dmp.getId().equals( p.getId() ) )
					publicationsFromSelection.add( dmp );
			}

		}

		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		List<String> allTopics = new ArrayList<String>();

		for ( DataMiningPublication dmp : publicationsFromSelection )
		{
			PublicationTopicFlat ptf = dmp.getPublication_topic_flat();
			if ( ptf != null )
			{
				Map<String, Double> topics = InterestParser.parseInterestString( ptf.getTopics() );

				Iterator<String> term = topics.keySet().iterator();
				Iterator<Double> termWeight = topics.values().iterator();
				while ( term.hasNext() && termWeight.hasNext() )
				{
					String topic = ( term.next() );
					if ( !allTopics.contains( topic ) )
					{
						allTopics.add( topic );
						attributes.add( new Attribute( topic ) );
					}
				}
			}
		}

		// Assign interests of authors to topics of publications..
		Instances data = new Instances( "publications", attributes, publicationsFromSelection.size() );

		for ( DataMiningPublication p : publicationsFromSelection )
		{
			Instance i = new DenseInstance( attributes.size() );
			List<String> publicationTopics = new ArrayList<String>();
			List<Double> publicationTopicWeights = new ArrayList<Double>();
			Map<String, Double> topics = new HashMap<String, Double>();
			PublicationTopicFlat ptf = p.getPublication_topic_flat();
			if ( ptf != null )
			{
				topics = InterestParser.parseInterestString( ptf.getTopics() );
				topics = MapSorter.sortByValue( topics );
				int count = 0;
				List<String> publicationTopTopics = new ArrayList<String>();
				Iterator<String> term = topics.keySet().iterator();
				Iterator<Double> termWeight = topics.values().iterator();
				while ( term.hasNext() && termWeight.hasNext() )
				{
					String interest = ( term.next() );
					Double weight = termWeight.next();
					if ( !publicationTopics.contains( interest ) )
					{
						publicationTopics.add( interest );
						publicationTopicWeights.add( weight );
						if ( count < 8 )
							publicationTopTopics.add( interest );
						count++;
					}

				}

				nodeTerms.put( p.getId(), publicationTopTopics );

				// check if author interests are present in the topic list, if
				// yes,
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
		}

		try
		{
			// applying the clustering algorithm
			if ( algorithm.equals( "xmeans" ) && attributes.size() > 0 )
			{
				XMeans result = WekaXMeans.run( 2, data );

				for ( int ind = 0; ind < data.size(); ind++ )
				{
					clusterMap.put( mapper.writeValueAsString( publicationsFromSelection.get( ind ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
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

		resultMap.put( "clusterMap", clusterMap );
		resultMap.put( "clusterTerms", clusterTerms );
		resultMap.put( "nodeTerms", nodeTerms );

		return resultMap;
	}

}

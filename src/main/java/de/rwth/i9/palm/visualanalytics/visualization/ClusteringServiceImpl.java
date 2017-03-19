package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rwth.i9.palm.analytics.algorithm.clustering.WekaEM;
import de.rwth.i9.palm.analytics.algorithm.clustering.WekaFarthestFirst;
import de.rwth.i9.palm.analytics.algorithm.clustering.WekaHierarchichal;
import de.rwth.i9.palm.analytics.algorithm.clustering.WekaKMeans;
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
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationTopicFlat;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import weka.clusterers.EM;
import weka.clusterers.FarthestFirst;
import weka.clusterers.HierarchicalClusterer;
import weka.clusterers.SimpleKMeans;
import weka.clusterers.XMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

@Service
public class ClusteringServiceImpl implements ClusteringService
{
	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	WekaFarthestFirst wekaFarthestFirst;

	@Autowired
	WekaHierarchichal hierarchicalClusterer;

	@Autowired
	WekaEM eM;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private VADataFetcher dataFetcher;

	@Override
	public Map<String, Object> clusterAuthors( String algorithm, List<String> idsList, Set<Publication> publications, String type, String visType, String startYear, String endYear, HttpServletRequest request, String yearFilterPresent, List<Interest> filteredTopic, List<String> repeatCallList, String seedVal, String noOfClustersVal, String foldsVal, String iterationsVal )
	{

		if ( seedVal == "" )
			seedVal = "2";
		if ( noOfClustersVal == "" )
			noOfClustersVal = "3";
		if ( foldsVal == "" )
			foldsVal = "2";
		if ( iterationsVal == "" )
			iterationsVal = "10";
		if ( algorithm == "" )
			algorithm = "K-Means";

		Map<String, Object> resultMap = new HashMap<String, Object>();
		List<DataMiningAuthor> coAuthors = new ArrayList<DataMiningAuthor>();
		List<DataMiningEventGroup> conferencesFromSelection = new ArrayList<DataMiningEventGroup>();
		List<DataMiningPublication> publicationsFromSelection = new ArrayList<DataMiningPublication>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			Map<String, Integer> clusterMap = new HashMap<String, Integer>();
			Map<Integer, List<String>> clusterTerms = new HashMap<Integer, List<String>>();
			Map<String, List<String>> nodeTerms = new HashMap<String, List<String>>();

			List<String> coAuthorIds = new ArrayList<String>();

			if ( repeatCallList == null || repeatCallList.isEmpty() )
			{
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

				// verify if the researchers also have the selected interests,
				// not
				// just
				// there publications
				if ( !filteredTopic.isEmpty() )
					commonAuthors = dataFetcher.getAuthorsFromInterestFilter( filteredTopic, commonAuthors );

				// shortlisted researchers' publications must also have the
				// corresponding topics
				List<Author> publicationAuthors = new ArrayList<Author>();
				Set<Publication> publicationsWithInterest = new HashSet<Publication>();
				if ( type.equals( "topic" ) && visType.equals( "researchers" ) )
				{
					publicationsWithInterest = dataFetcher.fetchAllPublications( type, idsList, authorList );
					for ( Publication p : publicationsWithInterest )
						for ( Author a : p.getAuthors() )
							if ( !publicationAuthors.contains( a ) )
								publicationAuthors.add( a );
				}

				// authors apart from selected authors
				for ( Author coAuthor : commonAuthors )
				{
					// just skip if its one of the authors in consideration
					if ( authorList.contains( coAuthor ) )
						continue;
					else if ( !publicationsWithInterest.isEmpty() && !publicationAuthors.contains( coAuthor ) )
						continue;
					else
						coAuthorList.add( coAuthor );
				}

				List<DataMiningAuthor> authors = persistenceStrategy.getAuthorDAO().getDataMiningObjects(); // all
																											// database
				for ( DataMiningAuthor dma : authors )
				{
					for ( int i = 0; i < coAuthorList.size(); i++ )
					{
						if ( dma.getName().equals( coAuthorList.get( i ).getName() ) )
						{
							coAuthors.add( dma );
							coAuthorIds.add( dma.getId() );
						}
					}
				}
			}
			else
			{
				List<DataMiningAuthor> authors = persistenceStrategy.getAuthorDAO().getDataMiningObjects(); // all

				for ( DataMiningAuthor dma : authors )
				{
					for ( int i = 0; i < repeatCallList.size(); i++ )
					{
						if ( dma.getId().equals( repeatCallList.get( i ) ) )
						{
							coAuthors.add( dma );
							coAuthorIds.add( dma.getId() );
						}
					}
				}
			}

			ArrayList<Attribute> attributes = new ArrayList<Attribute>();
			List<String> allInterests = new ArrayList<String>();
			for ( DataMiningAuthor a : coAuthors )
			{
				if ( a.getAuthor_interest_flat() != null )
				{
					Map<String, Double> interests = InterestParser.parseInterestString( a.getAuthor_interest_flat().getInterests() );
					int interestCount = 0;
					Iterator<String> interestTerm = interests.keySet().iterator();
					Iterator<Double> interestTermWeight = interests.values().iterator();
					while ( interestTerm.hasNext() && interestTermWeight.hasNext() && interestCount < 11 )
					{
						String interest = ( interestTerm.next() );
						Double weight = ( interestTermWeight.next() );
						if ( !allInterests.contains( interest ) && weight > 1.0 )
						{
							allInterests.add( interest );
							attributes.add( new Attribute( interest ) );
							interestCount++;
						}
					}
				}
			}

			// Assign interests of authors to topics of publications..
			Instances data = new Instances( "authors", attributes, coAuthors.size() );
			int counter = 0;
			for ( DataMiningAuthor a : coAuthors )
			{
				Instance i = new DenseInstance( attributes.size() );
				List<String> authorInterests = new ArrayList<String>();
				List<Double> authorInterestWeights = new ArrayList<Double>();
				Map<String, Double> interests = new HashMap<String, Double>();

				if ( a.getAuthor_interest_flat() != null )
				{
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
				}
				for ( int s = 0; s < allInterests.size(); s++ )
				{
					if ( authorInterests.contains( allInterests.get( s ) ) )
					{
						if ( authorInterestWeights.get( authorInterests.indexOf( allInterests.get( s ) ) ) > 1 )
							i.setValue( attributes.get( s ), authorInterestWeights.get( authorInterests.indexOf( allInterests.get( s ) ) ) );
						else
							i.setValue( attributes.get( s ), 0 );
					}
					else
					{
						i.setValue( attributes.get( s ), 0 );
					}
				}
				data.add( i );
				counter++;
			}

			Map<String, Object> map = applyAlgorithm( type, visType, algorithm, attributes, seedVal, noOfClustersVal, foldsVal, iterationsVal, data, clusterMap, clusterTerms, coAuthors, conferencesFromSelection, publicationsFromSelection );
			resultMap.put( "clusterMap", map.get( "clusterMap" ) );
			resultMap.put( "clusterTerms", map.get( "clusterTerms" ) );
			resultMap.put( "nodeTerms", nodeTerms );
			resultMap.put( "dataSet", coAuthorIds );
			resultMap.put( "algo", algorithm );
			resultMap.put( "seedVal", seedVal );
			resultMap.put( "noOfClustersVal", noOfClustersVal );
			resultMap.put( "foldsVal", foldsVal );
			resultMap.put( "iterationsVal", iterationsVal );
		}
		return resultMap;
	}

	@Override
	public Map<String, Object> clusterConferences( String algorithm, Set<Publication> publications, List<Interest> filteredTopic, String type, String visType, List<String> idsList, List<String> repeatCallList, String seedVal, String noOfClustersVal, String foldsVal, String iterationsVal )
	{
		if ( seedVal == "" )
			seedVal = "10";
		if ( noOfClustersVal == "" )
			noOfClustersVal = "3";
		if ( foldsVal == "" )
			foldsVal = "2";
		if ( iterationsVal == "" )
			iterationsVal = "10";
		if ( algorithm == "" )
			algorithm = "K-Means";

		Map<String, Integer> clusterMap = new HashMap<String, Integer>();
		Map<String, Object> resultMap = new HashMap<String, Object>();
		Map<Integer, List<String>> clusterTerms = new HashMap<Integer, List<String>>();
		Map<String, List<String>> nodeTerms = new HashMap<String, List<String>>();
		List<DataMiningAuthor> coAuthors = new ArrayList<DataMiningAuthor>();
		List<DataMiningEventGroup> conferencesFromSelection = new ArrayList<DataMiningEventGroup>();
		List<DataMiningPublication> publicationsFromSelection = new ArrayList<DataMiningPublication>();
		List<String> conferenceIds = new ArrayList<String>();
		List<DataMiningEventGroup> allConferences = persistenceStrategy.getEventGroupDAO().getDataMiningObjects();

		if ( repeatCallList == null || repeatCallList.isEmpty() )
		{
			List<Publication> publicationsList = new ArrayList<Publication>( publications );
			List<EventGroup> eventGroups = new ArrayList<EventGroup>();

			for ( int i = 0; i < publicationsList.size(); i++ )
			{
				Event e = publicationsList.get( i ).getEvent();
				if ( e != null )
				{
					EventGroup eventGroup = e.getEventGroup();
					if ( eventGroup != null )
						if ( !eventGroups.contains( eventGroup ) )
							eventGroups.add( eventGroup );
				}
			}

			List<String> interestStrings = new ArrayList<String>();
			if ( !filteredTopic.isEmpty() )
			{
				for ( Interest i : filteredTopic )
					interestStrings.add( i.getTerm() );
			}
			if ( type.equals( "topic" ) )
			{
				for ( String id : idsList )
					interestStrings.add( persistenceStrategy.getInterestDAO().getById( id ).getTerm() );
			}

			// Find DataMiningEvent Groups corresponding to the subset of
			// conferences
			for ( DataMiningEventGroup dmc : allConferences )
			{
				for ( EventGroup eg : eventGroups )
				{
					if ( dmc.getName().equals( eg.getName() ) )
					{
						Boolean flag = true;

						// add conference only if it has the topics from the
						// filter
						if ( !filteredTopic.isEmpty() )
						{
							if ( dmc.getEventGroup_interest_flat() != null )
							{
								Map<String, Double> interests = InterestParser.parseInterestString( dmc.getEventGroup_interest_flat().getInterests() );
								Set<String> interestTerms = interests.keySet();
								if ( interestTerms.containsAll( interestStrings ) )
									flag = true;
								else
									flag = false;
							}
						}
						// add conference only if it has the selected interests
						if ( type.equals( "topic" ) )
						{
							if ( dmc.getEventGroup_interest_flat() != null )
							{
								Map<String, Double> interests = InterestParser.parseInterestString( dmc.getEventGroup_interest_flat().getInterests() );
								Set<String> interestTerms = interests.keySet();
								if ( interestTerms.containsAll( interestStrings ) )
									flag = true;
								else
									flag = false;
							}
						}

						if ( flag )
						{
							conferencesFromSelection.add( dmc );
							conferenceIds.add( dmc.getId() );
						}
					}
				}
			}
		}
		else
		{
			for ( DataMiningEventGroup dmc : allConferences )
			{
				for ( int i = 0; i < repeatCallList.size(); i++ )
				{
					if ( dmc.getId().equals( repeatCallList.get( i ) ) )
					{
						conferencesFromSelection.add( dmc );
						conferenceIds.add( dmc.getId() );
					}
				}
			}
		}

		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		List<String> allInterests = new ArrayList<String>();

		for ( DataMiningEventGroup dmeg : conferencesFromSelection )
		{
			if ( dmeg.getEventGroup_interest_flat() != null )
			{
				Map<String, Double> interests = InterestParser.parseInterestString( dmeg.getEventGroup_interest_flat().getInterests() );

				Iterator<String> interestTerm = interests.keySet().iterator();
				Iterator<Double> interestTermWeight = interests.values().iterator();
				while ( interestTerm.hasNext() && interestTermWeight.hasNext() )
				{
					String interest = ( interestTerm.next() );
					Double interestWeight = ( interestTermWeight.next() );
					if ( !allInterests.contains( interest ) && interestWeight > 1.0 )
					{
						allInterests.add( interest );
						attributes.add( new Attribute( interest ) );
					}
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
			if ( eg.getEventGroup_interest_flat() != null )
			{
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
			}
			// check if author interests are present in the topic list, if
			// yes,
			// their weights are taken into account for clustering
			for ( int s = 0; s < allInterests.size(); s++ )
			{
				if ( eventGroupInterests.contains( allInterests.get( s ) ) )
					if ( eventGroupInterestWeights.get( eventGroupInterests.indexOf( allInterests.get( s ) ) ) > 1 )
						i.setValue( attributes.get( s ), eventGroupInterestWeights.get( eventGroupInterests.indexOf( allInterests.get( s ) ) ) );
					else
						i.setValue( attributes.get( s ), 0 );
				else
					i.setValue( attributes.get( s ), 0 );
			}
			data.add( i );
		}

		Map<String, Object> map = applyAlgorithm( type, visType, algorithm, attributes, seedVal, noOfClustersVal, foldsVal, iterationsVal, data, clusterMap, clusterTerms, coAuthors, conferencesFromSelection, publicationsFromSelection );
		resultMap.put( "clusterMap", map.get( "clusterMap" ) );
		resultMap.put( "clusterTerms", map.get( "clusterTerms" ) );
		resultMap.put( "nodeTerms", nodeTerms );
		resultMap.put( "dataSet", conferenceIds );
		resultMap.put( "algo", algorithm );
		resultMap.put( "seedVal", seedVal );
		resultMap.put( "noOfClustersVal", noOfClustersVal );
		resultMap.put( "foldsVal", foldsVal );
		resultMap.put( "iterationsVal", iterationsVal );
		return resultMap;
	}

	public Map<String, Object> clusterPublications( String algorithm, Set<Publication> publications, String type, String visType, List<String> repeatCallList, String seedVal, String noOfClustersVal, String foldsVal, String iterationsVal )
	{
		if ( seedVal == "" )
			seedVal = "10";
		if ( noOfClustersVal == "" )
			noOfClustersVal = "3";
		if ( foldsVal == "" )
			foldsVal = "2";
		if ( iterationsVal == "" )
			iterationsVal = "10";
		if ( algorithm == "" )
			algorithm = "K-Means";

		Map<String, Object> resultMap = new HashMap<String, Object>();
		Map<String, Integer> clusterMap = new HashMap<String, Integer>();
		Map<Integer, List<String>> clusterTerms = new HashMap<Integer, List<String>>();
		Map<String, List<String>> nodeTerms = new HashMap<String, List<String>>();
		List<String> publicationIds = new ArrayList<String>();
		List<DataMiningPublication> allPublications = persistenceStrategy.getPublicationDAO().getDataMiningObjects();
		List<DataMiningAuthor> coAuthors = new ArrayList<DataMiningAuthor>();
		List<DataMiningEventGroup> conferencesFromSelection = new ArrayList<DataMiningEventGroup>();
		List<DataMiningPublication> publicationsFromSelection = new ArrayList<DataMiningPublication>();

		if ( repeatCallList == null || repeatCallList.isEmpty() )
		{
			// Find DataMiningEvent Groups corresponding to the subset of
			// conferences
			for ( DataMiningPublication dmp : allPublications )
			{
				for ( Publication p : publications )
				{
					if ( dmp.getId().equals( p.getId() ) )
					{
						publicationsFromSelection.add( dmp );
						publicationIds.add( dmp.getId() );
					}
				}
			}
		}
		else
		{
			for ( DataMiningPublication dmp : allPublications )
			{
				for ( int i = 0; i < repeatCallList.size(); i++ )
				{
					if ( dmp.getId().equals( repeatCallList.get( i ) ) )
					{
						publicationsFromSelection.add( dmp );
						publicationIds.add( dmp.getId() );
					}
				}
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
					String topic = term.next();
					Double topicWeight = termWeight.next();
					if ( !allTopics.contains( topic ) && topicWeight > 0.3 )
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

						if ( publicationTopicWeights.get( publicationTopics.indexOf( allTopics.get( s ) ) ) > 1 )
							i.setValue( attributes.get( s ), publicationTopicWeights.get( publicationTopics.indexOf( allTopics.get( s ) ) ) );
						else
							i.setValue( attributes.get( s ), 0 );
					}
					else
					{
						i.setValue( attributes.get( s ), 0 );
					}
				}
				data.add( i );
			}
		}
		Map<String, Object> map = applyAlgorithm( type, visType, algorithm, attributes, seedVal, noOfClustersVal, foldsVal, iterationsVal, data, clusterMap, clusterTerms, coAuthors, conferencesFromSelection, publicationsFromSelection );
		resultMap.put( "clusterMap", map.get( "clusterMap" ) );
		resultMap.put( "clusterTerms", map.get( "clusterTerms" ) );
		resultMap.put( "nodeTerms", nodeTerms );
		resultMap.put( "dataSet", publicationIds );
		resultMap.put( "algo", algorithm );
		resultMap.put( "seedVal", seedVal );
		resultMap.put( "noOfClustersVal", noOfClustersVal );
		resultMap.put( "foldsVal", foldsVal );
		resultMap.put( "iterationsVal", iterationsVal );
		return resultMap;
	}

	public <T> Map<String, Object> applyAlgorithm( String type, String visType, String algorithm, List<Attribute> attributes, String seedVal, String noOfClustersVal, String foldsVal, String iterationsVal, Instances data, Map<String, Integer> clusterMap, Map<Integer, List<String>> clusterTerms, List<DataMiningAuthor> coAuthors, List<DataMiningEventGroup> conferencesFromSelection, List<DataMiningPublication> publicationsFromSelection )
	{
		try
		{
			// applying the clustering algorithm
			if ( algorithm.equals( "X-Means" ) && attributes.size() > 0 )
			{
				XMeans result = WekaXMeans.run( seedVal, noOfClustersVal, iterationsVal, data );
				for ( int ind = 0; ind < data.size(); ind++ )
				{
					if ( visType.equals( "researchers" ) )
						clusterMap.put( mapper.writeValueAsString( ( coAuthors.get( ind ) ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
					if ( visType.equals( "conferences" ) )
						clusterMap.put( mapper.writeValueAsString( ( conferencesFromSelection.get( ind ) ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
					if ( visType.equals( "publications" ) )
						clusterMap.put( mapper.writeValueAsString( ( publicationsFromSelection.get( ind ) ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
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
						if ( max.size() < 10 )
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

			if ( algorithm.equals( "K-Means" ) && attributes.size() > 0 )
			{
				SimpleKMeans result = WekaKMeans.run( seedVal, noOfClustersVal, data );

				for ( int ind = 0; ind < data.size(); ind++ )
				{
					if ( visType.equals( "researchers" ) )
						clusterMap.put( mapper.writeValueAsString( ( coAuthors.get( ind ) ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
					if ( visType.equals( "conferences" ) )
						clusterMap.put( mapper.writeValueAsString( ( conferencesFromSelection.get( ind ) ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
					if ( visType.equals( "publications" ) )
						clusterMap.put( mapper.writeValueAsString( ( publicationsFromSelection.get( ind ) ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}

				Instances instances = result.getClusterCentroids();
				for ( int instanceCounter = 0; instanceCounter < instances.size(); instanceCounter++ )
				{
					List<Double> max = new ArrayList<Double>();
					List<Integer> maxIndex = new ArrayList<Integer>();
					List<String> terms = new ArrayList<String>();
					Integer numAttr = instances.get( instanceCounter ).numAttributes();
					for ( int i = 0; i < numAttr; i++ )
					{
						if ( max.size() < 10 )
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
			if ( algorithm.equals( "FarthestFirst" ) )
			{
				FarthestFirst result = wekaFarthestFirst.run( seedVal, noOfClustersVal, data );

				for ( int ind = 0; ind < data.size(); ind++ )
				{
					if ( visType.equals( "researchers" ) )
						clusterMap.put( mapper.writeValueAsString( ( coAuthors.get( ind ) ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
					if ( visType.equals( "conferences" ) )
						clusterMap.put( mapper.writeValueAsString( ( conferencesFromSelection.get( ind ) ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
					if ( visType.equals( "publications" ) )
						clusterMap.put( mapper.writeValueAsString( ( publicationsFromSelection.get( ind ) ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}
				Instances instances = result.getClusterCentroids();
				for ( int instanceCounter = 0; instanceCounter < instances.size(); instanceCounter++ )
				{
					List<Double> max = new ArrayList<Double>();
					List<Integer> maxIndex = new ArrayList<Integer>();
					List<String> terms = new ArrayList<String>();
					Integer numAttr = instances.get( instanceCounter ).numAttributes();
					for ( int i = 0; i < numAttr; i++ )
					{
						if ( max.size() < 10 )
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
			if ( algorithm.equals( "Hierarchical" ) )
			{
				HierarchicalClusterer result = hierarchicalClusterer.run( noOfClustersVal, data );
				for ( int ind = 0; ind < data.size(); ind++ )
				{
					if ( visType.equals( "researchers" ) )
						clusterMap.put( mapper.writeValueAsString( ( coAuthors.get( ind ) ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
					if ( visType.equals( "conferences" ) )
						clusterMap.put( mapper.writeValueAsString( ( conferencesFromSelection.get( ind ) ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
					if ( visType.equals( "publications" ) )
						clusterMap.put( mapper.writeValueAsString( ( publicationsFromSelection.get( ind ) ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}
			}

			// applying the clustering algorithm
			if ( algorithm.equals( "EM" ) )
			{
				EM result = eM.run( seedVal, foldsVal, iterationsVal, data );
				for ( int ind = 0; ind < data.size(); ind++ )
				{
					if ( visType.equals( "researchers" ) )
						clusterMap.put( mapper.writeValueAsString( ( coAuthors.get( ind ) ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
					if ( visType.equals( "conferences" ) )
						clusterMap.put( mapper.writeValueAsString( ( conferencesFromSelection.get( ind ) ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
					if ( visType.equals( "publications" ) )
						clusterMap.put( mapper.writeValueAsString( ( publicationsFromSelection.get( ind ) ).getJsonStub() ), result.clusterInstance( data.get( ind ) ) );
				}
			}
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}

		Map<String, Object> mapFromAlgo = new HashMap<String, Object>();
		mapFromAlgo.put( "clusterMap", clusterMap );
		mapFromAlgo.put( "clusterTerms", clusterTerms );
		return mapFromAlgo;
	}
}

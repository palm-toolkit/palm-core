package de.rwth.i9.palm.similarity.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rwth.i9.palm.analytics.util.InterestParser;
import de.rwth.i9.palm.feature.academicevent.AcademicEventFeature;
import de.rwth.i9.palm.helper.MapSorter;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.DataMiningAuthor;
import de.rwth.i9.palm.model.DataMiningEventGroup;
import de.rwth.i9.palm.model.DataMiningPublication;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Service
public class SimilarityServiceImpl implements SimilarityService
{
	long startTime = System.currentTimeMillis();
	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private AcademicEventFeature eventFeature;

	public Map<String, Object> similarAuthors( List<Author> authorList )
	{
		System.out.println( "in similar authors" );
		// all authors in PALM
		List<DataMiningAuthor> authors = persistenceStrategy.getAuthorDAO().getDataMiningObjects();

		List<DataMiningAuthor> mainAuthors = new ArrayList<DataMiningAuthor>();

		List<String> interests = new ArrayList<String>();

		Map<String, Double> othersInterests = new HashMap<String, Double>();
		if ( authorList.size() == 1 )
		{
			Map<String, Double> authorInterests = new HashMap<String, Double>();

			for ( DataMiningAuthor dma : authors )
			{
				if ( dma.getName().equals( authorList.get( 0 ).getName() ) )
				{
					mainAuthors.add( dma );
					authorInterests = InterestParser.parseInterestString( dma.getAuthor_interest_flat().getInterests() );
				}
			}

			interests = new ArrayList<String>( authorInterests.keySet() );
		}

		else
		{

			List<Integer> count = new ArrayList<Integer>();
			for ( DataMiningAuthor dma : authors )
			{
				for ( Author a : authorList )
				{
					if ( dma.getName().equals( a.getName() ) )
					{
						mainAuthors.add( dma );
						Map<String, Double> tempInterests = InterestParser.parseInterestString( dma.getAuthor_interest_flat().getInterests() );

						List<String> keys = new ArrayList<String>( tempInterests.keySet() );
						for ( int i = 0; i < tempInterests.size(); i++ )
						{
							if ( !interests.contains( keys.get( i ) ) )
							{
								interests.add( keys.get( i ) );
								count.add( 1 );
							}
							else
							{
								Integer index = interests.indexOf( keys.get( i ) );
								Integer prevVal = count.get( index );
								count.set( index, prevVal + 1 );

							}
						}
					}
				}
			}

			// System.out.println( count.toString() );

			for ( int i = 0; i < count.size(); i++ )
			{
				if ( count.get( i ) != authorList.size() )
				{
					count.remove( i );
					interests.remove( i );
					i--;
				}
			}

		}

		Map<DataMiningAuthor, Double> scoreMap = new HashMap<DataMiningAuthor, Double>();

		Map<DataMiningAuthor, Map<String, Double>> interestMap = new HashMap<DataMiningAuthor, Map<String, Double>>();
		for ( DataMiningAuthor a : authors )
		{
			if ( !mainAuthors.contains( a ) )
			{
				System.out.println( "\nAuthor: " + a.getName() );
				othersInterests = InterestParser.parseInterestString( a.getAuthor_interest_flat().getInterests() );
				for ( int i = 0; i < interests.size(); i++ )
				{
					String term = interests.get( i );

					if ( othersInterests.containsKey( term ) )
					{
						System.out.println( "interest : " + term );

						if ( scoreMap.containsKey( a ) )
						{
							Double val = scoreMap.get( a );
							scoreMap.remove( a );

							// to score as per number of interests
							scoreMap.put( a, val + 1.0 );
							Map<String, Double> interest = interestMap.get( a );
							interest.put( term, othersInterests.get( term ) );
							interestMap.put( a, interest );

							// to score as per weights
							// scoreMap.put( a, val + othersInterests.get( term
							// ) );
						}
						else
						{
							// to score as per number of interests
							scoreMap.put( a, 1.0 );
							Map<String, Double> interest = new HashMap<String, Double>();
							interest.put( term, othersInterests.get( term ) );
							interestMap.put( a, interest );

							// to score as per weights
							// scoreMap.put( a, othersInterests.get( term ) );
						}
					}
				}
				System.out.println( scoreMap.get( a ) );
			}

		}

		Map<DataMiningAuthor, Double> sortedScoreMap = MapSorter.sortByAuthor( scoreMap );

		Map<DataMiningAuthor, Map<String, Double>> sortedInterestMap = new HashMap<DataMiningAuthor, Map<String, Double>>();
		for ( int i = 0; i < interestMap.size(); i++ )
		{
			sortedInterestMap.put( new ArrayList<DataMiningAuthor>( interestMap.keySet() ).get( i ), MapSorter.sortByTerm( new ArrayList<Map<String, Double>>( interestMap.values() ).get( i ) ) );
		}

		Map<String, Object> finalMap = new HashMap<String, Object>();
		finalMap.put( "scoreMap", sortedScoreMap );
		finalMap.put( "interestMap", sortedInterestMap );

		return finalMap;

	}

	@SuppressWarnings( "unchecked" )
	@Override
	public Map<String, Object> similarConferences( List<String> idsList )
	{
		// all conferences in PALM
		List<DataMiningEventGroup> eventGroups = persistenceStrategy.getEventGroupDAO().getDataMiningObjects();
		List<DataMiningEventGroup> mainEventGroups = new ArrayList<DataMiningEventGroup>();
		List<String> interests = new ArrayList<String>();
		Map<String, Double> othersInterests = new HashMap<String, Double>();

		if ( idsList.size() == 1 )
		{
			Map<String, Double> eventInterests = new HashMap<String, Double>();
			Map<String, Object> eventGroupMap = (Map<String, Object>) eventFeature.getEventMining().fetchEventGroupData( idsList.get( 0 ), null, null ).get( "eventGroup" );
			String mainEvent = (String) eventGroupMap.get( "name" );
			for ( DataMiningEventGroup dmeg : eventGroups )
			{
				if ( dmeg.getName().equalsIgnoreCase( mainEvent ) )
				{
					mainEventGroups.add( dmeg );
					eventInterests = InterestParser.parseInterestString( dmeg.getEventGroup_interest_flat().getInterests() );
				}
			}

			interests = new ArrayList<String>( eventInterests.keySet() );
		}

		else
		{
			List<Integer> count = new ArrayList<Integer>();
			for ( DataMiningEventGroup dmeg : eventGroups )
			{
				for ( String id : idsList )
				{
					Map<String, Object> eventGroupMap = (Map<String, Object>) eventFeature.getEventMining().fetchEventGroupData( id, null, null ).get( "eventGroup" );
					String mainEvent = (String) eventGroupMap.get( "name" );

					if ( dmeg.getName().equalsIgnoreCase( mainEvent ) )
					{
						mainEventGroups.add( dmeg );

						Map<String, Double> tempInterests = InterestParser.parseInterestString( dmeg.getEventGroup_interest_flat().getInterests() );
						System.out.println( tempInterests.toString() );
						List<String> keys = new ArrayList<String>( tempInterests.keySet() );
						for ( int i = 0; i < tempInterests.size(); i++ )
						{
							if ( !interests.contains( keys.get( i ) ) )
							{
								interests.add( keys.get( i ) );
								count.add( 1 );
							}
							else
							{
								Integer index = interests.indexOf( keys.get( i ) );
								Integer prevVal = count.get( index );
								count.set( index, prevVal + 1 );
							}
						}
					}
				}
			}

			for ( int i = 0; i < count.size(); i++ )
			{
				if ( count.get( i ) != idsList.size() )
				{
					count.remove( i );
					interests.remove( i );
					i--;
				}
			}

		}

		Map<DataMiningEventGroup, Double> scoreMap = new HashMap<DataMiningEventGroup, Double>();
		Map<DataMiningEventGroup, Map<String, Double>> interestMap = new HashMap<DataMiningEventGroup, Map<String, Double>>();

		for ( DataMiningEventGroup eg : eventGroups )
		{
			if ( !mainEventGroups.contains( eg ) )
			{
				System.out.println( "\nAuthor: " + eg.getName() );
				if ( !eg.getEventGroup_interest_flat().getInterests().isEmpty() )
					othersInterests = InterestParser.parseInterestString( eg.getEventGroup_interest_flat().getInterests() );
				else
					othersInterests = new HashMap<String, Double>();

				for ( int i = 0; i < interests.size(); i++ )
				{
					String term = interests.get( i );

					if ( othersInterests.containsKey( term ) )
					{
						System.out.println( "interest : " + term );

						if ( scoreMap.containsKey( eg ) )
						{
							Double val = scoreMap.get( eg );
							scoreMap.remove( eg );

							// to score as per number of interests
							scoreMap.put( eg, val + 1.0 );
							Map<String, Double> interest = interestMap.get( eg );
							interest.put( term, othersInterests.get( term ) );
							interestMap.put( eg, interest );

							// to score as per weights
							// scoreMap.put( a, val + othersInterests.get( term
							// ) );
						}
						else
						{
							// to score as per number of interests
							scoreMap.put( eg, 1.0 );
							Map<String, Double> interest = new HashMap<String, Double>();
							interest.put( term, othersInterests.get( term ) );
							interestMap.put( eg, interest );

							// to score as per weights
							// scoreMap.put( a, othersInterests.get( term ) );
						}
					}
				}
				System.out.println( scoreMap.get( eg ) );
			}

		}

		Map<DataMiningEventGroup, Double> sortedScoreMap = MapSorter.sortByEventGroup( scoreMap );

		Map<DataMiningEventGroup, Map<String, Double>> sortedInterestMap = new HashMap<DataMiningEventGroup, Map<String, Double>>();
		for ( int i = 0; i < interestMap.size(); i++ )
		{
			sortedInterestMap.put( new ArrayList<DataMiningEventGroup>( interestMap.keySet() ).get( i ), MapSorter.sortByTerm( new ArrayList<Map<String, Double>>( interestMap.values() ).get( i ) ) );
		}

		Map<String, Object> finalMap = new HashMap<String, Object>();
		finalMap.put( "scoreMap", sortedScoreMap );
		finalMap.put( "interestMap", sortedInterestMap );

		return finalMap;
	}

	public Map<String, Object> similarPublications( List<String> idsList )
	{
		// all publications in PALM
		List<DataMiningPublication> publications = persistenceStrategy.getPublicationDAO().getDataMiningObjects();

		List<DataMiningPublication> mainPublications = new ArrayList<DataMiningPublication>();

		List<String> topics = new ArrayList<String>();

		Map<String, Double> othersTopics = new HashMap<String, Double>();

		if ( idsList.size() == 1 )
		{
			Map<String, Double> publicationTopics = new HashMap<String, Double>();
			Publication p = persistenceStrategy.getPublicationDAO().getById( idsList.get( 0 ) );

			for ( DataMiningPublication dmp : publications )
			{
				if ( dmp.getTitle().equals( p.getTitle() ) )
				{
					System.out.println( "indide 1st check" );
					mainPublications.add( dmp );
					publicationTopics = InterestParser.parseInterestString( dmp.getPublication_topic_flat().getTopics() );
				}
			}

			topics = new ArrayList<String>( publicationTopics.keySet() );
		}

		else
		{

			List<Integer> count = new ArrayList<Integer>();
			for ( DataMiningPublication dmp : publications )
			{
				for ( String id : idsList )
				{
					Publication p = persistenceStrategy.getPublicationDAO().getById( id );
					if ( dmp.getTitle().equals( p.getTitle() ) )
					{
						mainPublications.add( dmp );
						Map<String, Double> tempTopics = InterestParser.parseInterestString( dmp.getPublication_topic_flat().getTopics() );

						List<String> keys = new ArrayList<String>( tempTopics.keySet() );
						for ( int i = 0; i < tempTopics.size(); i++ )
						{
							if ( !topics.contains( keys.get( i ) ) )
							{
								topics.add( keys.get( i ) );
								count.add( 1 );
							}
							else
							{
								Integer index = topics.indexOf( keys.get( i ) );
								Integer prevVal = count.get( index );
								count.set( index, prevVal + 1 );

							}
						}
					}
				}
			}

			// System.out.println( count.toString() );

			for ( int i = 0; i < count.size(); i++ )
			{
				if ( count.get( i ) != idsList.size() )
				{
					count.remove( i );
					topics.remove( i );
					i--;
				}
			}

		}

		Map<DataMiningPublication, Double> scoreMap = new HashMap<DataMiningPublication, Double>();

		Map<DataMiningPublication, Map<String, Double>> topicMap = new HashMap<DataMiningPublication, Map<String, Double>>();
		for ( DataMiningPublication p : publications )
		{
			if ( !mainPublications.contains( p ) )
			{
				System.out.println( "\nAuthor: " + p.getTitle() );
				if ( !p.getPublication_topic_flat().getTopics().isEmpty() )
					othersTopics = InterestParser.parseInterestString( p.getPublication_topic_flat().getTopics() );
				else
					othersTopics = new HashMap<String, Double>();

				for ( int i = 0; i < topics.size(); i++ )
				{
					String term = topics.get( i );

					if ( othersTopics.containsKey( term ) )
					{
						System.out.println( "interest : " + term );

						if ( scoreMap.containsKey( p ) )
						{
							Double val = scoreMap.get( p );
							scoreMap.remove( p );

							// to score as per number of interests
							scoreMap.put( p, val + 1.0 );
							Map<String, Double> interest = topicMap.get( p );
							interest.put( term, othersTopics.get( term ) );
							topicMap.put( p, interest );

							// to score as per weights
							// scoreMap.put( a, val + othersInterests.get( term
							// ) );
						}
						else
						{
							// to score as per number of interests
							scoreMap.put( p, 1.0 );
							Map<String, Double> interest = new HashMap<String, Double>();
							interest.put( term, othersTopics.get( term ) );
							topicMap.put( p, interest );

							// to score as per weights
							// scoreMap.put( a, othersInterests.get( term ) );
						}
					}
				}
				System.out.println( scoreMap.get( p ) );
			}

		}

		Map<DataMiningPublication, Double> sortedScoreMap = MapSorter.sortByPublication( scoreMap );

		Map<DataMiningPublication, Map<String, Double>> sortedInterestMap = new HashMap<DataMiningPublication, Map<String, Double>>();
		for ( int i = 0; i < topicMap.size(); i++ )
		{
			sortedInterestMap.put( new ArrayList<DataMiningPublication>( topicMap.keySet() ).get( i ), MapSorter.sortByTerm( new ArrayList<Map<String, Double>>( topicMap.values() ).get( i ) ) );
		}

		Map<String, Object> finalMap = new HashMap<String, Object>();
		finalMap.put( "scoreMap", sortedScoreMap );
		finalMap.put( "interestMap", sortedInterestMap );

		return finalMap;

	}


}

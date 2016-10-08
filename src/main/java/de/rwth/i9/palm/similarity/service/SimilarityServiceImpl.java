package de.rwth.i9.palm.similarity.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rwth.i9.palm.analytics.util.InterestParser;
import de.rwth.i9.palm.feature.academicevent.AcademicEventFeature;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.DataMiningAuthor;
import de.rwth.i9.palm.model.DataMiningEventGroup;
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
							scoreMap.put( a, val + 0.3 );
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
							scoreMap.put( a, 0.3 );
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

		Map<DataMiningAuthor, Double> sortedScoreMap = sortByAuthor( scoreMap );

		Map<DataMiningAuthor, Map<String, Double>> sortedInterestMap = new HashMap<DataMiningAuthor, Map<String, Double>>();
		for ( int i = 0; i < interestMap.size(); i++ )
		{
			sortedInterestMap.put( new ArrayList<DataMiningAuthor>( interestMap.keySet() ).get( i ), sortByTerm( new ArrayList<Map<String, Double>>( interestMap.values() ).get( i ) ) );
		}

		Map<String, Object> finalMap = new HashMap<String, Object>();
		finalMap.put( "scoreMap", sortedScoreMap );
		finalMap.put( "interestMap", sortedInterestMap );

		// List<DataMiningAuthor> sortedAuthors = new
		// ArrayList<DataMiningAuthor>( sortedScoreMap.keySet() );
		// List<Double> sortedInterestScores = new ArrayList<Double>(
		// sortedScoreMap.values() );
		// // take the 20 top matches
		// for ( int i = 0; i < 20; i++ )
		// {
		// System.out.println( sortedAuthors.get( i ).getName() +
		// sortedInterestScores.get( i ) );
		// }

		return finalMap;

	}

	@SuppressWarnings( "unchecked" )
	@Override
	public Map<DataMiningEventGroup, Double> similarConferences( List<String> idsList )
	{
		System.out.println( "in similar conferences" );

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
				if ( dmeg.getName().equals( mainEvent ) )
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
					if ( dmeg.getName().equals( mainEvent ) )
					{
						// System.out.println( "BLAH BLAH: " + dmeg.getName() );
						mainEventGroups.add( dmeg );
						Map<String, Double> tempInterests = InterestParser.parseInterestString( dmeg.getEventGroup_interest_flat().getInterests() );

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
				if ( count.get( i ) != idsList.size() )
				{
					count.remove( i );
					interests.remove( i );
					i--;
				}
			}

		}

		Map<DataMiningEventGroup, Double> scoreMap = new HashMap<DataMiningEventGroup, Double>();
		for ( DataMiningEventGroup dmeg : eventGroups )
		{
			if ( !mainEventGroups.contains( dmeg ) )
			{
				othersInterests = InterestParser.parseInterestString( dmeg.getEventGroup_interest_flat().getInterests() );
				for ( int i = 0; i < interests.size(); i++ )
				{
					String term = interests.get( i );

					if ( othersInterests.containsKey( term ) )
					{
						if ( scoreMap.containsKey( dmeg ) )
						{
							Double val = scoreMap.get( dmeg );
							scoreMap.remove( dmeg );
							scoreMap.put( dmeg, val + othersInterests.get( term ) );
						}
						else
							scoreMap.put( dmeg, othersInterests.get( term ) );
					}
				}
			}

		}

		Map<DataMiningEventGroup, Double> sortedScoreMap = sortByEventGroup( scoreMap );

		List<DataMiningEventGroup> sortedAuthors = new ArrayList<DataMiningEventGroup>( sortedScoreMap.keySet() );
		List<Double> sortedInterestScores = new ArrayList<Double>( sortedScoreMap.values() );
		// take the 20 top matches
		// for ( int i = 0; i < 20; i++ )
		// {
		// System.out.println( sortedAuthors.get( i ).getName() +
		// sortedInterestScores.get( i ) );
		// }

		return sortedScoreMap;
		// return null;
	}

	// SOURCE: www.mkyong.com
	private static Map<DataMiningAuthor, Double> sortByAuthor( Map<DataMiningAuthor, Double> unsortMap )
	{

		// 1. Convert Map to List of Map
		List<Map.Entry<DataMiningAuthor, Double>> list = new LinkedList<Map.Entry<DataMiningAuthor, Double>>( unsortMap.entrySet() );

		// 2. Sort list with Collections.sort(), provide a custom Comparator
		// Try switch the o1 o2 position for a different order
		Collections.sort( list, new Comparator<Map.Entry<DataMiningAuthor, Double>>()
		{
			public int compare( Map.Entry<DataMiningAuthor, Double> o1, Map.Entry<DataMiningAuthor, Double> o2 )
			{
				return ( o2.getValue() ).compareTo( o1.getValue() );
			}
		} );

		// 3. Loop the sorted list and put it into a new insertion order Map
		// LinkedHashMap
		Map<DataMiningAuthor, Double> sortedMap = new LinkedHashMap<DataMiningAuthor, Double>();
		for ( Map.Entry<DataMiningAuthor, Double> entry : list )
		{
			sortedMap.put( entry.getKey(), entry.getValue() );
		}

		/*
		 * //classic iterator example for (Iterator<Map.Entry<String, Integer>>
		 * it = list.iterator(); it.hasNext(); ) { Map.Entry<String, Integer>
		 * entry = it.next(); sortedMap.put(entry.getKey(), entry.getValue()); }
		 */

		return sortedMap;
	}

	private static Map<DataMiningEventGroup, Double> sortByEventGroup( Map<DataMiningEventGroup, Double> unsortMap )
	{

		// 1. Convert Map to List of Map
		List<Map.Entry<DataMiningEventGroup, Double>> list = new LinkedList<Map.Entry<DataMiningEventGroup, Double>>( unsortMap.entrySet() );

		// 2. Sort list with Collections.sort(), provide a custom Comparator
		// Try switch the o1 o2 position for a different order
		Collections.sort( list, new Comparator<Map.Entry<DataMiningEventGroup, Double>>()
		{
			public int compare( Map.Entry<DataMiningEventGroup, Double> o1, Map.Entry<DataMiningEventGroup, Double> o2 )
			{
				return ( o2.getValue() ).compareTo( o1.getValue() );
			}
		} );

		// 3. Loop the sorted list and put it into a new insertion order Map
		// LinkedHashMap
		Map<DataMiningEventGroup, Double> sortedMap = new LinkedHashMap<DataMiningEventGroup, Double>();
		for ( Map.Entry<DataMiningEventGroup, Double> entry : list )
		{
			sortedMap.put( entry.getKey(), entry.getValue() );
		}

		/*
		 * //classic iterator example for (Iterator<Map.Entry<String, Integer>>
		 * it = list.iterator(); it.hasNext(); ) { Map.Entry<String, Integer>
		 * entry = it.next(); sortedMap.put(entry.getKey(), entry.getValue()); }
		 */

		return sortedMap;
	}

	public Map<String, Double> sortByTerm( Map<String, Double> unsortMap )
	{

		// 1. Convert Map to List of Map
		List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>( unsortMap.entrySet() );

		// 2. Sort list with Collections.sort(), provide a custom Comparator
		// Try switch the o1 o2 position for a different order
		Collections.sort( list, new Comparator<Map.Entry<String, Double>>()
		{
			public int compare( Map.Entry<String, Double> o1, Map.Entry<String, Double> o2 )
			{
				return ( o2.getValue() ).compareTo( o1.getValue() );
			}
		} );

		// 3. Loop the sorted list and put it into a new insertion order Map
		// LinkedHashMap
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for ( Map.Entry<String, Double> entry : list )
		{
			sortedMap.put( entry.getKey(), entry.getValue() );
		}

		/*
		 * //classic iterator example for (Iterator<Map.Entry<String, Integer>>
		 * it = list.iterator(); it.hasNext(); ) { Map.Entry<String, Integer>
		 * entry = it.next(); sortedMap.put(entry.getKey(), entry.getValue()); }
		 */

		return sortedMap;
	}

}

package de.rwth.i9.palm.helper;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.rwth.i9.palm.model.DataMiningAuthor;
import de.rwth.i9.palm.model.DataMiningEventGroup;
import de.rwth.i9.palm.model.DataMiningPublication;

public class MapSorter
{
	// SOURCE: www.mkyong.com
	public static Map<String, Double> sortByValue( Map<String, Double> unsortMap )
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

		return sortedMap;
	}

	public static Map<String, Double> sortByTerm( Map<String, Double> unsortMap )
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

		return sortedMap;
	}

	public static Map<DataMiningAuthor, Double> sortByAuthor( Map<DataMiningAuthor, Double> unsortMap )
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

		return sortedMap;
	}

	public static Map<DataMiningEventGroup, Double> sortByEventGroup( Map<DataMiningEventGroup, Double> unsortMap )
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

		return sortedMap;
	}

	public static Map<DataMiningPublication, Double> sortByPublication( Map<DataMiningPublication, Double> unsortMap )
	{

		// 1. Convert Map to List of Map
		List<Map.Entry<DataMiningPublication, Double>> list = new LinkedList<Map.Entry<DataMiningPublication, Double>>( unsortMap.entrySet() );

		// 2. Sort list with Collections.sort(), provide a custom Comparator
		// Try switch the o1 o2 position for a different order
		Collections.sort( list, new Comparator<Map.Entry<DataMiningPublication, Double>>()
		{
			public int compare( Map.Entry<DataMiningPublication, Double> o1, Map.Entry<DataMiningPublication, Double> o2 )
			{
				return ( o2.getValue() ).compareTo( o1.getValue() );
			}
		} );

		// 3. Loop the sorted list and put it into a new insertion order Map
		// LinkedHashMap
		Map<DataMiningPublication, Double> sortedMap = new LinkedHashMap<DataMiningPublication, Double>();
		for ( Map.Entry<DataMiningPublication, Double> entry : list )
		{
			sortedMap.put( entry.getKey(), entry.getValue() );
		}

		return sortedMap;
	}

}

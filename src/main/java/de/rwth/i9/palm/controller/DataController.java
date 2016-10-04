package de.rwth.i9.palm.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.rwth.i9.palm.feature.academicevent.AcademicEventFeature;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorInterest;
import de.rwth.i9.palm.model.AuthorInterestFlat;
import de.rwth.i9.palm.model.AuthorInterestProfile;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.EventGroupInterestFlat;
import de.rwth.i9.palm.model.EventInterest;
import de.rwth.i9.palm.model.EventInterestProfile;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Controller
@RequestMapping( value = "/data" )
public class DataController
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private AcademicEventFeature eventFeature;

	@Transactional
	@RequestMapping( value = "/reindex/all", method = RequestMethod.GET )
	public @ResponseBody String allReindex()
	{
		try
		{
			persistenceStrategy.getInstitutionDAO().doReindexing();
			persistenceStrategy.getAuthorDAO().doReindexing();
			persistenceStrategy.getPublicationDAO().doReindexing();
			persistenceStrategy.getEventDAO().doReindexing();
			persistenceStrategy.getEventGroupDAO().doReindexing();
			persistenceStrategy.getCircleDAO().doReindexing();
		}
		catch ( InterruptedException e )
		{
			return ( e.getMessage() );
		}

		return "re-indexing institutions, authors, publications, events and circles complete";
	}

	@Transactional
	@RequestMapping( value = "/reindex/institution", method = RequestMethod.GET )
	public @ResponseBody String institutionReindex()
	{
		try
		{
			persistenceStrategy.getInstitutionDAO().doReindexing();
		}
		catch ( InterruptedException e )
		{
			return ( e.getMessage() );
		}
		return "re indexing institution complete";
	}

	@Transactional
	@RequestMapping( value = "/reindex/author", method = RequestMethod.GET )
	public @ResponseBody String authorReindex()
	{
		try
		{
			persistenceStrategy.getAuthorDAO().doReindexing();
		}
		catch ( InterruptedException e )
		{
			return ( e.getMessage() );
		}
		return "re indexing author complete";
	}

	@Transactional
	@RequestMapping( value = "/reindex/publication", method = RequestMethod.GET )
	public @ResponseBody String publicationReindex()
	{
		try
		{
			persistenceStrategy.getPublicationDAO().doReindexing();
		}
		catch ( InterruptedException e )
		{
			return ( e.getMessage() );
		}
		return "re indexing publication complete";
	}

	@Transactional
	@RequestMapping( value = "/reindex/event", method = RequestMethod.GET )
	public @ResponseBody String eventReindex()
	{
		try
		{
			persistenceStrategy.getEventDAO().doReindexing();
			persistenceStrategy.getEventGroupDAO().doReindexing();
		}
		catch ( InterruptedException e )
		{
			return ( e.getMessage() );
		}
		return "re indexing event complete";
	}

	@Transactional
	@RequestMapping( value = "/reindex/circle", method = RequestMethod.GET )
	public @ResponseBody String circleReindex()
	{
		try
		{
			persistenceStrategy.getCircleDAO().doReindexing();
		}
		catch ( InterruptedException e )
		{
			return ( e.getMessage() );
		}
		return "re indexing circle complete";
	}

	@Transactional
	@RequestMapping( value = "/updateflattables/authors", method = RequestMethod.GET )
	public @ResponseBody String updateFlatAuthor()
	{
		System.out.println( "will start updating flat author table" );
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAllAuthors();
		int j = 1;
		for ( Author author : authors )
		{
			if ( persistenceStrategy.getAuthorInterestFlatDAO().authorIdExists( author.getId() ) )
				continue;
			Map<String, Double> authorInterests = new HashMap<String, Double>();
			for ( AuthorInterestProfile aip : author.getAuthorInterestProfiles() )
			{
				for ( AuthorInterest ai : aip.getAuthorInterests() )
				{
					Map<Interest, Double> termWeights = ai.getTermWeights();
					for ( Interest i : termWeights.keySet() )
					{
						Double value = authorInterests.containsKey( i.getTerm() ) ? authorInterests.get( i.getTerm() ) : 0;

						String sValue = (String) String.format( "%.1f", value );
						Double truncValue = Double.parseDouble( sValue );

						String sWeight = (String) String.format( "%.1f", termWeights.get( i ) );
						Double truncWeight = Double.parseDouble( sWeight );

						if ( !authorInterests.keySet().contains( i.getTerm() ) )
							authorInterests.put( i.getTerm(), truncWeight );
						else
						{
							System.out.println( i.getTerm() );
							authorInterests.remove( i );
							authorInterests.put( i.getTerm(), truncValue + truncWeight );
						}
						System.out.println( truncValue + " : " + truncWeight );
					}
				}
			}

			Map<String, Double> sortedMap = sortByValue( authorInterests );
			AuthorInterestFlat aif = new AuthorInterestFlat();
			aif.setAuthor_id( author.getId() );
			aif.setInterests( sortedMap.toString().replaceAll( "[\\{\\}]", "" ) );
			persistenceStrategy.getAuthorInterestFlatDAO().persist( aif );
			System.out.println( "Added no " + j );
			j++;
		}

		return "Updated flat author table.";
	}

	@Transactional
	@RequestMapping( value = "/updateflattables/events", method = RequestMethod.GET )
	public @ResponseBody String updateFlatConference()
	{
		System.out.println( "will start updating flat academic event table" );
		List<EventGroup> eventGroupList = persistenceStrategy.getEventGroupDAO().getEventGroupListWithoutPaging( "", "", "" );
		int j = 1;
		for ( EventGroup eg : eventGroupList )
		{
			if ( persistenceStrategy.getEventGroupInterestFlatDAO().eventIdExists( eg.getId() ) )
				continue;
			List<Event> events = eg.getEvents();
			Map<String, Double> eventGroupTopics = new HashMap<String, Double>();

			for ( Event e : events )
			{
				Set<EventInterestProfile> eips = e.getEventInterestProfiles();

				for ( EventInterestProfile eip : eips )
				{
					Set<EventInterest> eventInterests = eip.getEventInterests();
					for ( EventInterest ei : eventInterests )
					{
						Map<Interest, Double> termWeights = ei.getTermWeights();
						List<Interest> interests = new ArrayList<Interest>( termWeights.keySet() );
						// List<Double> interestWeights = new
						// ArrayList<Double>(termWeights.values());
						for ( Interest i : interests )
						{
							Double value = eventGroupTopics.containsKey( i.getTerm() ) ? eventGroupTopics.get( i.getTerm() ) : 0;

							String sValue = (String) String.format( "%.1f", value );
							Double truncValue = Double.parseDouble( sValue );

							String sWeight = (String) String.format( "%.1f", termWeights.get( i ) );
							Double truncWeight = Double.parseDouble( sWeight );

							if ( !eventGroupTopics.keySet().contains( i.getTerm() ) )
								eventGroupTopics.put( i.getTerm(), truncWeight );
							else
							{
								System.out.println( i.getTerm() );
								eventGroupTopics.remove( i );
								eventGroupTopics.put( i.getTerm(), truncValue + truncWeight );
							}
							System.out.println( truncValue + " : " + truncWeight );
						}

					}

				}
			}
			Map<String, Double> sortedMap = sortByValue( eventGroupTopics );
			EventGroupInterestFlat eif = new EventGroupInterestFlat();
			eif.setEventGroup_id( eg.getId() );
			eif.setInterests( sortedMap.toString().replaceAll( "[\\{\\}]", "" ) );
			persistenceStrategy.getEventGroupInterestFlatDAO().persist( eif );
			System.out.println( "Added no " + j );
			j++;

		}

		return "Updated flat event group table.";
	}

	// SOURCE: www.mkyong.com
	private static Map<String, Double> sortByValue( Map<String, Double> unsortMap )
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

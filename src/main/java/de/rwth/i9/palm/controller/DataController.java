package de.rwth.i9.palm.controller;

import java.util.ArrayList;
import java.util.HashMap;
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
import de.rwth.i9.palm.helper.MapSorter;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorInterest;
import de.rwth.i9.palm.model.AuthorInterestFlat;
import de.rwth.i9.palm.model.AuthorInterestProfile;
import de.rwth.i9.palm.model.DataMiningPublication;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.EventGroupInterestFlat;
import de.rwth.i9.palm.model.EventInterest;
import de.rwth.i9.palm.model.EventInterestProfile;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.PublicationTopic;
import de.rwth.i9.palm.model.PublicationTopicFlat;
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
	@RequestMapping( value = "/createflattables/all", method = RequestMethod.GET )
	public @ResponseBody String createflattables()
	{
		updateFlatAuthor();
		updateFlatConference();
		updateFlatPublication();
		return "creation/updation of flat tables complete";
	}

	@Transactional
	@RequestMapping( value = "/createflattables/authors", method = RequestMethod.GET )
	public void updateFlatAuthor()
	{
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAllAuthors();
		int j = 1;
		for ( Author author : authors )
		{
			if ( persistenceStrategy.getAuthorInterestFlatDAO().authorIdExists( author.getId() ) )
			{
				AuthorInterestFlat authorInterestFlat = persistenceStrategy.getAuthorInterestFlatDAO().getById( author.getId() );
				persistenceStrategy.getAuthorInterestFlatDAO().delete( authorInterestFlat );
			}
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
							authorInterests.remove( i );
							authorInterests.put( i.getTerm(), truncValue + truncWeight );
						}
					}
				}
			}

			Map<String, Double> sortedMap = MapSorter.sortByValue( authorInterests );
			AuthorInterestFlat aif = new AuthorInterestFlat();
			aif.setAuthor_id( author.getId() );
			aif.setInterests( sortedMap.toString().replaceAll( "[\\{\\}]", "" ) );
			persistenceStrategy.getAuthorInterestFlatDAO().persist( aif );
			j++;
		}

		System.out.println( "Updated flat author table." );
	}

	@Transactional
	@RequestMapping( value = "/createflattables/events", method = RequestMethod.GET )
	public void updateFlatConference()
	{
		List<EventGroup> eventGroupList = persistenceStrategy.getEventGroupDAO().getEventGroupListWithoutPaging( "", "", "" );
		int j = 1;
		for ( EventGroup eg : eventGroupList )
		{
			if ( persistenceStrategy.getEventGroupInterestFlatDAO().eventIdExists( eg.getId() ) )
			{
				EventGroupInterestFlat eventGroupInterestFlat = persistenceStrategy.getEventGroupInterestFlatDAO().getById( eg.getId() );
				persistenceStrategy.getEventGroupInterestFlatDAO().delete( eventGroupInterestFlat );
			}
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
								eventGroupTopics.remove( i );
								eventGroupTopics.put( i.getTerm(), truncValue + truncWeight );
							}
						}
					}
				}
			}
			Map<String, Double> sortedMap = MapSorter.sortByValue( eventGroupTopics );
			EventGroupInterestFlat eif = new EventGroupInterestFlat();
			eif.setEventGroup_id( eg.getId() );
			eif.setInterests( sortedMap.toString().replaceAll( "[\\{\\}]", "" ) );
			persistenceStrategy.getEventGroupInterestFlatDAO().persist( eif );
			j++;
		}

		System.out.println( "Updated flat event group table." );
	}

	@Transactional
	@RequestMapping( value = "/createflattables/publications", method = RequestMethod.GET )
	public void updateFlatPublication()
	{
		List<DataMiningPublication> publications = persistenceStrategy.getPublicationDAO().getDataMiningObjects();
		int j = 1;
		for ( DataMiningPublication publication : publications )
		{
			if ( persistenceStrategy.getPublicationTopicFlatDAO().publicationIdExists( publication.getId() ) )
			{
				PublicationTopicFlat publicationTopicFlat = persistenceStrategy.getPublicationTopicFlatDAO().getById( publication.getId() );
				persistenceStrategy.getPublicationTopicFlatDAO().delete( publicationTopicFlat );
			}
			Map<String, Double> publicationTopics = new HashMap<String, Double>();
			for ( PublicationTopic pt : publication.getPublicationTopics() )
			{
				for ( String term : pt.getTermValues().keySet() )
				{
					Double value = publicationTopics.containsKey( term ) ? publicationTopics.get( term ) : 0;
					publicationTopics.put( term.replaceAll( "=", "" ), value + pt.getTermValues().get( term ) );
				}
			}
			Map<String, Double> sortedMap = MapSorter.sortByValue( publicationTopics );
			PublicationTopicFlat ptf = new PublicationTopicFlat();
			ptf.setPublication_id( publication.getId() );
			ptf.setTopics( sortedMap.toString().replaceAll( "[\\{\\}]", "" ) );
			persistenceStrategy.getPublicationTopicFlatDAO().persist( ptf );
			j++;
		}

		System.out.println( "Updated flat publication tables" );
	}

	@Transactional
	@RequestMapping( value = "/updateflattables/authors", method = RequestMethod.GET )
	public void updateFlatNewAuthor()
	{
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
							authorInterests.remove( i );
							authorInterests.put( i.getTerm(), truncValue + truncWeight );
						}
					}
				}
			}

			Map<String, Double> sortedMap = MapSorter.sortByValue( authorInterests );
			AuthorInterestFlat aif = new AuthorInterestFlat();
			aif.setAuthor_id( author.getId() );
			aif.setInterests( sortedMap.toString().replaceAll( "[\\{\\}]", "" ) );
			persistenceStrategy.getAuthorInterestFlatDAO().persist( aif );
			j++;
		}

		System.out.println( "Updated flat author table." );
	}
}

package de.rwth.i9.palm.controller;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorInterest;
import de.rwth.i9.palm.model.AuthorInterestFlat;
import de.rwth.i9.palm.model.AuthorInterestProfile;
import de.rwth.i9.palm.model.DataMiningPublication;
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
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAdded();
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
						authorInterests.put( i.getTerm(), value + termWeights.get( i ) );
					}
				}
			}
			AuthorInterestFlat aif = new AuthorInterestFlat();
			aif.setAuthor_id( author.getId() );
			aif.setInterests( authorInterests.toString().replaceAll( "[\\{\\}]", "" ) );
			persistenceStrategy.getAuthorInterestFlatDAO().persist( aif );
			System.out.println( "Added no " + j );
			j++;
		}

		return "Updated flat author table.";
	}

	@Transactional
	@RequestMapping( value = "/updateflattables/publications", method = RequestMethod.GET )
	public @ResponseBody String updateFlatPublication()
	{
		List<DataMiningPublication> publications = persistenceStrategy.getPublicationDAO().getDataMiningObjects();
		int j = 1;
		for ( DataMiningPublication publication : publications )
		{
			// if (
			// persistenceStrategy.getPublicationTopicFlatDAO().publicationIdExists(
			// publication.getId() ) )
			// continue;
			Map<String, Double> publicationTopics = new HashMap<String, Double>();
			for ( PublicationTopic pt : publication.getPublicationTopics() )
			{
				for ( String term : pt.getTermValues().keySet() )
				{
					Double value = publicationTopics.containsKey( term ) ? publicationTopics.get( term ) : 0;
					publicationTopics.put( term, value + pt.getTermValues().get( term ) );
				}
			}
			PublicationTopicFlat ptf = new PublicationTopicFlat();
			ptf.setPublication_id( publication.getId() );
			System.out.println( publicationTopics.toString().replaceAll( "[\\{\\}]", "" ) );
			ptf.setTopics( publicationTopics.toString().replaceAll( "[\\{\\}]", "" ) );
			persistenceStrategy.getPublicationTopicFlatDAO().persist( ptf );
			System.out.println( "Added no " + j );
			j++;
		}

		return "Updated flat publication tables";
	}
}

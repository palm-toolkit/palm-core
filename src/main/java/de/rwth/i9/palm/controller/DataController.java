package de.rwth.i9.palm.controller;


import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Controller
@RequestMapping( value = "/data" )
public class DataController
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Transactional
	@RequestMapping( value = "/all/reindex", method = RequestMethod.GET )
	public @ResponseBody String allReindex()
	{
		try
		{
			persistenceStrategy.getInstitutionDAO().doReindexing();
		}
		catch ( InterruptedException e )
		{
			return ( e.getMessage() );
		}

		try
		{
			persistenceStrategy.getAuthorDAO().doReindexing();
		}
		catch ( InterruptedException e )
		{
			return ( e.getMessage() );
		}

		try
		{
			persistenceStrategy.getPublicationDAO().doReindexing();
		}
		catch ( InterruptedException e )
		{
			return ( e.getMessage() );
		}

		return "re-indexing institution, author and publication complete";
	}

	@Transactional
	@RequestMapping( value = "/institution/reindex", method = RequestMethod.GET )
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
	@RequestMapping( value = "/author/reindex", method = RequestMethod.GET )
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
	@RequestMapping( value = "/publication/reindex", method = RequestMethod.GET )
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

}

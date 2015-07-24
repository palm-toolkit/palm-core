package de.rwth.i9.palm.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.SessionDataSet;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetStatus;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.ApplicationContextService;

@Controller
@SessionAttributes( { "sessionDataSet" } )
@RequestMapping( value = "/publication" )
public class PublicationController
{
	private static final String LINK_NAME = "publication";

	@Autowired
	private ApplicationContextService appService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@RequestMapping( method = RequestMethod.GET )
	@Transactional
	public ModelAndView mainPage( 
			@RequestParam( value = "sessionid", required = false ) final String sessionId, 
			final HttpServletResponse response ) throws InterruptedException
	{
		// get current session object
		SessionDataSet sessionDataSet = this.appService.getCurrentSessionDataSet();

		// set model and view
		ModelAndView model = TemplateHelper.createViewWithSessionDataSet( "publication", LINK_NAME, sessionDataSet );

		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getWidget( WidgetType.PUBLICATION, WidgetStatus.DEFAULT );
		// assign the model
		model.addObject( "widgets", widgets );
		return model;
	}

	@Transactional
	@RequestMapping( value = "/search", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getPublicationList( 
			@RequestParam( value = "query", required = false ) String query,
			@RequestParam( value = "conference", required = false ) String conferenceName,
			@RequestParam( value = "conferenceid", required = false ) String conferenceId,
			@RequestParam( value = "page", required = false ) Integer page, 
			@RequestParam( value = "maxresult", required = false ) Integer maxresult, 
			final HttpServletResponse response )
	{
		if ( query == null )
			query = "";

		if ( page == null )
			page = 0;
		
		if ( maxresult == null )
			maxresult = 50;

		// get the publication
		Map<String, Object> publicationMap = persistenceStrategy.getPublicationDAO().getPublicationByFullTextSearchWithPaging( query, page, maxresult );

		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		responseMap.put( "query", query );
		responseMap.put( "page", page );
		responseMap.put( "maxresult", maxresult );

		// create the json structure for publication list
		if ( publicationMap != null )
		{
			responseMap.put( "count", publicationMap.get( "count" ) );

			@SuppressWarnings( "unchecked" )
			List<Publication> publications = (List<Publication>) publicationMap.get( "result" );
			List<Map<String, String>> publicationList = new ArrayList<Map<String, String>>();

			for ( Publication publication : publications )
			{
				Map<String, String> pub = new LinkedHashMap<String, String>();
				pub.put( "id", publication.getId() );
				pub.put( "title", publication.getTitle() );

				publicationList.add( pub );
			}
			responseMap.put( "publication", publicationList );

		}
		else
		{
			responseMap.put( "count", 0 );
		}

		return responseMap;
	}

	@RequestMapping( value = "/detail", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> researcherInterest( 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "uri", required = false ) final String uri, 
			final HttpServletResponse response) throws InterruptedException, IOException, ExecutionException
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// get author
		Publication publication = persistenceStrategy.getPublicationDAO().getById( id );
		if ( publication == null )
		{
			responseMap.put( "status", "Error - publication not found" );
			return responseMap;
		}

		responseMap.put( "status", "OK" );

		// put publication detail
		Map<String, Object> publicationMap = new LinkedHashMap<String, Object>();
		publicationMap.put( "title", publication.getTitle() );
		if ( publication.getAbstractText() != null )
			publicationMap.put( "abstract", publication.getAbstractText() );
		// coauthor
		List<Map<String, Object>> coathorList = new ArrayList<Map<String, Object>>();
		for ( Author author : publication.getCoAuthors() )
		{
			Map<String, Object> authorMap = new LinkedHashMap<String, Object>();
			authorMap.put( "id", author.getId() );
			authorMap.put( "name", author.getName() );
			if ( author.getInstitution() != null )
				authorMap.put( "aff", author.getInstitution().getName() );
			if ( author.getPhotoUrl() != null )
				authorMap.put( "photo", author.getPhotoUrl() );

			coathorList.add( authorMap );
		}
		publicationMap.put( "coauthor", coathorList );
		
		responseMap.put( "publication", publicationMap );

		return responseMap;
	}
}
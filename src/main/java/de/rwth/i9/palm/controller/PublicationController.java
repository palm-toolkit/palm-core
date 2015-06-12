package de.rwth.i9.palm.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetStatus;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Controller
@RequestMapping( value = "/publication" )
public class PublicationController
{

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@RequestMapping( method = RequestMethod.GET )
	@Transactional
	public ModelAndView mainPage( @RequestParam( value = "sessionid", required = false ) final String sessionId, final HttpServletResponse response ) throws InterruptedException
	{
		ModelAndView model = new ModelAndView( "researcher", "link", "researcher" );

		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getWidget( WidgetType.PUBLICATION, WidgetStatus.DEFAULT );
		// assign the model
		model.addObject( "widgets", widgets );
		return model;
	}

	@Transactional
	@RequestMapping( value = "/search", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getPublicationList( 
			@RequestParam( value = "query", required = false ) String query, 
			@RequestParam( value = "page", required = false ) Integer page, 
			@RequestParam( value = "maxresult", required = false ) Integer maxresult, 
			final HttpServletResponse response )
	{
		if ( query == null )
			query = "";

		if ( page == null )
			page = 0;
		
		if ( maxresult == null )
			maxresult = 20;

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
}
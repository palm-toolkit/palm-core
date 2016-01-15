package de.rwth.i9.palm.controller;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.ParseException;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.datasetcollect.service.DblpEventCollection;
import de.rwth.i9.palm.feature.academicevent.AcademicEventFeature;
import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetStatus;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Controller
@RequestMapping( value = "/venue" )
public class AcademicEventController
{
	private static final String LINK_NAME = "venue";

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private AcademicEventFeature academicEventFeature;

	@RequestMapping( method = RequestMethod.GET )
	@Transactional
	public ModelAndView eventPage( 
			@RequestParam( value = "id", required = false ) final String id,
			@RequestParam( value = "eventId", required = false ) final String eventId, 
			@RequestParam( value = "name", required = false ) final String name,
			@RequestParam( value = "type", required = false ) final String type,
			@RequestParam( value = "year", required = false ) final String year,
			@RequestParam( value = "volume", required = false ) final String volume,
			@RequestParam( value = "publicationId", required = false ) final String publicationId,
			final HttpServletResponse response) throws InterruptedException
	{
		// set model and view
		ModelAndView model = TemplateHelper.createViewWithLink( "conference", LINK_NAME );

		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getWidget( WidgetType.CONFERENCE, WidgetStatus.DEFAULT );
		// assign the model
		model.addObject( "widgets", widgets );
		// assign query
		if ( id != null )
			model.addObject( "targetId", id );
		if ( eventId != null )
			model.addObject( "targetEventId", eventId );
		if ( name != null )
			model.addObject( "targetName", name );
		if ( type != null )
			model.addObject( "targetType", type );
		if ( year != null )
			model.addObject( "targetYear", year );
		if ( name != null )
			model.addObject( "targetVolume", volume );
		if ( publicationId != null )
			model.addObject( "publicationId", publicationId );
		return model;
	}

	@Transactional
	@RequestMapping( value = "/search", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getConferenceList( 
			@RequestParam( value = "query", required = false ) String query, 
			@RequestParam( value = "startPage", required = false ) Integer startPage, 
			@RequestParam( value = "maxresult", required = false ) Integer maxresult,
			@RequestParam( value = "type", required = false ) String type,
			@RequestParam( value = "source", required = false ) String source,
			@RequestParam( value = "persist", required = false ) String persist,
			HttpServletRequest request,
			HttpServletResponse response)
	{
		if ( query == null ) 		query = "";
		if ( startPage == null )	startPage = 0;
		if ( maxresult == null )	maxresult = 50;
		if ( type == null )			type = "all";
		if ( source == null )		source = "internal";
		if ( persist == null )		persist = "no";
		
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		boolean persistResult = false;

		responseMap.put( "query", query );
		responseMap.put( "type", type );
		responseMap.put( "startPage", startPage );
		responseMap.put( "maxresult", maxresult );
		responseMap.put( "source", source );

		if ( !persist.equals( "no" ) )
		{
			responseMap.put( "persist", persist );
			persistResult = true;
		}
		
		List<EventGroup> eventGroups = academicEventFeature.getEventSearch().getEventGroupListByQuery( query, startPage, maxresult, source, type, persistResult );

		// put in session
		request.getSession().setAttribute( "eventGroups", eventGroups );

		return academicEventFeature.getEventSearch().printJsonOutput( responseMap, eventGroups );
	}
	
	@Transactional
	@RequestMapping( value = "/fetchGroup", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> fetchEventGroupFromDblp( 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "pid", required = false ) final String pid, 
			@RequestParam( value = "force", required = false ) final String force, 
			HttpServletRequest request, HttpServletResponse response) throws ParseException, 
			IOException, InterruptedException, ExecutionException, java.text.ParseException, TimeoutException, OAuthSystemException, OAuthProblemException
	{
		@SuppressWarnings( "unchecked" )
		List<EventGroup> sessionEventGroups = null;// (List<EventGroup>) request.getSession().getAttribute( "eventGroups" );

		return academicEventFeature.getEventMining().fetchEventGroupData( id, pid, sessionEventGroups );
	}

	@RequestMapping( value = "/fetch", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> fetchEventFromDblp( 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "pid", required = false ) final String pid, 
			@RequestParam( value = "force", required = false ) final String force,
			final HttpServletResponse response ) throws ParseException, IOException, InterruptedException, ExecutionException, java.text.ParseException, TimeoutException, OAuthSystemException, OAuthProblemException 
	{
		return academicEventFeature.getEventMining().fetchEventData( id, pid, force );
	}

	@RequestMapping( value = "/publicationList", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getPublicationList( @RequestParam( value = "id", required = false ) final String authorId, final HttpServletResponse response)
	{
		return academicEventFeature.getEventPublication().getPublicationListByEventId( authorId );
	}

	@RequestMapping( value = "/autocomplete", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody List<Object> getEventAutoComplete( @RequestParam( value = "query", required = false ) final String query, final HttpServletResponse response)
	{
		return DblpEventCollection.getEventFromDBLPSearch( query, "all", null );
	}

}
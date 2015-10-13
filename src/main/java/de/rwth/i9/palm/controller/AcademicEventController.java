package de.rwth.i9.palm.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

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
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.feature.academicevent.AcademicEventFeature;
import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.SessionDataSet;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetStatus;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.ApplicationContextService;

@Controller
@SessionAttributes( { "sessionDataSet" } )
@RequestMapping( value = "/venue" )
public class AcademicEventController
{
	private static final String LINK_NAME = "venue";

	@Autowired
	private ApplicationContextService appService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private AcademicEventFeature academicEventFeature;

	@RequestMapping( method = RequestMethod.GET )
	@Transactional
	public ModelAndView mainPage( @RequestParam( value = "sessionid", required = false ) final String sessionId, final HttpServletResponse response ) throws InterruptedException
	{
		// get current session object
		SessionDataSet sessionDataSet = this.appService.getCurrentSessionDataSet();

		// set model and view
		ModelAndView model = TemplateHelper.createViewWithSessionDataSet( "conference", LINK_NAME, sessionDataSet );

		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getWidget( WidgetType.CONFERENCE, WidgetStatus.DEFAULT );
		// assign the model
		model.addObject( "widgets", widgets );
		return model;
	}

	@Transactional
	@RequestMapping( value = "/search", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getConferenceList( 
			@RequestParam( value = "query", required = false ) String query, 
			@RequestParam( value = "page", required = false ) Integer page, 
			@RequestParam( value = "maxresult", required = false ) Integer maxresult, 
			final HttpServletResponse response)
	{
		return academicEventFeature.getEventSearch().getEventListByQuery( query, page, maxresult );
	}
	
	@RequestMapping( value = "/fetch", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> fetchEventFromDblp( 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "pid", required = false ) final String pid, 
			@RequestParam( value = "force", required = false ) final String force,
			final HttpServletResponse response ) throws ParseException, IOException, InterruptedException, ExecutionException, java.text.ParseException, TimeoutException, OAuthSystemException, OAuthProblemException 
	{
		return academicEventFeature.getEventSearch().fetchEventData( id, pid, force );
	}

	@RequestMapping( value = "/publicationList", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getPublicationList( @RequestParam( value = "id", required = false ) final String authorId, final HttpServletResponse response)
	{
		return academicEventFeature.getEventPublication().getPublicationListByEventId( authorId );
	}

}
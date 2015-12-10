package de.rwth.i9.palm.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.feature.academicevent.AcademicEventFeature;
import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationType;
import de.rwth.i9.palm.model.SessionDataSet;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.ApplicationContextService;

@Controller
@SessionAttributes( "eventGroup" )
@RequestMapping( value = "/venue" )
public class ManageAcademicEventController
{
	private static final String LINK_NAME = "administration";

	@Autowired
	private ApplicationContextService appService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private AcademicEventFeature academicEventFeature;

	/**
	 * Load the add eventGroup form together with eventGroup object
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/add", method = RequestMethod.GET )
	public ModelAndView addNewEventGroup(
			@RequestParam( value = "sessionid", required = false ) final String sessionId,
			@RequestParam( value = "eventId", required = false ) final String eventId, 
			@RequestParam( value = "name", required = false ) final String name,
			@RequestParam( value = "type", required = false ) final String type,
			@RequestParam( value = "year", required = false ) final String year,
			@RequestParam( value = "volume", required = false ) final String volume,
			@RequestParam( value = "publicationId", required = false ) final String publicationId,
			final HttpServletResponse response) throws InterruptedException
	{
		// get current session object
		SessionDataSet sessionDataSet = this.appService.getCurrentSessionDataSet();

		// set model and view
		ModelAndView model = TemplateHelper.createViewWithSessionDataSet( "dialogIframeLayout", LINK_NAME, sessionDataSet );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.CONFERENCE, "add" );

		// EventGroup
		EventGroup eventGroup = null;
		if ( eventId != null )
		{
			Event event = persistenceStrategy.getEventDAO().getById( eventId );

			if ( event != null )
				eventGroup = event.getEventGroup();
		}

		if ( eventGroup == null )
			eventGroup = new EventGroup();

		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "eventGroup", eventGroup );
		
		// assign query
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

	/**
	 * Save changes from Add eventGroup detail, via Spring binding
	 * 
	 * @param extractionServiceListWrapper
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/add", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> saveNewEventGroup( 
			@ModelAttribute( "eventGroup" ) EventGroup eventGroup,
			@RequestParam( value = "eventId" , required= false ) String eventId,
			@RequestParam( value = "publicationId" , required= false ) final String publicationId,
			@RequestParam( value = "type" , required= false ) final String type,
			@RequestParam( value = "volume" , required= false ) final String volume,
			@RequestParam( value = "year" , required= false ) final String year,
			final HttpServletResponse response) throws InterruptedException
	{

		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		
		// set flag added to true
		eventGroup.setAdded( true );
		// set event type, incase changed
		if ( type != null )
		{
			if ( !type.equals( eventGroup.getPublicationType().toString().toLowerCase() ) )
			{
				try
				{
					PublicationType pubType = PublicationType.valueOf( type.toUpperCase() );
					eventGroup.setPublicationType( pubType );
				}
				catch ( Exception e )
				{
				}
			}
		}
		persistenceStrategy.getEventGroupDAO().persist( eventGroup );
		
		// if event exist
		if( eventId != null ){
			Event event = persistenceStrategy.getEventDAO().getById( eventId );
			if( event != null ){
				event.setName( eventGroup.getName() );
				event.setAdded( true );
				persistenceStrategy.getEventDAO().persist( event );
			}
		}
		
		// if publicationId exist
		// this is only for event that manually inserted without DBLP
		Publication publication = null;
		if ( publicationId != null )
			publication = persistenceStrategy.getPublicationDAO().getById( publicationId );
		if( publication != null && eventGroup.getDblpUrl() == null){
			// create new event
			Event event = new Event();
			event.setName( eventGroup.getName() );
			if( volume != null )
				event.setVolume( volume );
			if( year != null )
				event.setYear( year );
			event.setAdded( true );
			event.addPublication( publication );
			event.setEventGroup( eventGroup );
			persistenceStrategy.getEventDAO().persist( event );
			
			publication.setEvent( event );
			persistenceStrategy.getPublicationDAO().persist( publication );
			
			if( eventId == null )
				eventId = event.getId(); 
		}
		
		responseMap.put( "status", "ok" );
		responseMap.put( "statusMessage", eventGroup.getName() + " successfully added to PALM" );
		
		// put study group detail
		Map<String,String> eventGroupMap = new LinkedHashMap<String, String>();
		eventGroupMap.put( "id", eventGroup.getId() );
		eventGroupMap.put( "name", eventGroup.getName() );
		if( eventGroup.getNotation() != null)
			eventGroupMap.put( "notation", eventGroup.getNotation() );
		if( eventGroup.getDblpUrl() != null)
			eventGroupMap.put( "dblpUrl", eventGroup.getDblpUrl() );
		
		responseMap.put( "eventGroup", eventGroupMap );
		
		if( eventId != null )
			responseMap.put( "eventId", eventId );
		if( volume != null )
			responseMap.put( "volume", volume );
		if( year != null )
			responseMap.put( "year", year );
		return responseMap;
	}
	
	/**
	 * Load the add eventGroup form together with eventGroup object
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/edit", method = RequestMethod.GET )
	public ModelAndView editEventGroup( 
			@RequestParam( value = "sessionid", required = false ) final String sessionId,
			@RequestParam( value = "id") final String eventGroupId,
			final HttpServletResponse response) throws InterruptedException
	{
		// get current session object
		SessionDataSet sessionDataSet = this.appService.getCurrentSessionDataSet();

		// set model and view
		ModelAndView model = TemplateHelper.createViewWithSessionDataSet( "dialogIframeLayout", LINK_NAME, sessionDataSet );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.CONFERENCE, "edit" );

		// create blank EventGroup
		EventGroup eventGroup = persistenceStrategy.getEventGroupDAO().getById( eventGroupId );

		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "eventGroup", eventGroup );

		return model;
	}
}
package de.rwth.i9.palm.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
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

import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationType;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.SecurityService;

@Controller
@SessionAttributes( { "eventGroup", "event" } )
@RequestMapping( value = "/venue" )
public class ManageAcademicEventController
{
	private static final String LINK_NAME = "administration";

	@Autowired
	private SecurityService securityService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

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
			@RequestParam( value = "eventId", required = false ) final String eventId, 
			@RequestParam( value = "name", required = false ) final String name,
			@RequestParam( value = "type", required = false ) final String type,
			@RequestParam( value = "year", required = false ) final String year,
			@RequestParam( value = "volume", required = false ) final String volume,
			@RequestParam( value = "publicationId", required = false ) final String publicationId,
			final HttpServletResponse response) throws InterruptedException
	{
		ModelAndView model = null;

		if ( securityService.getUser() == null )
		{
			model = TemplateHelper.createViewWithLink( "401", "error" );
			return model;
		}

		model = TemplateHelper.createViewWithLink( "dialogIframeLayout", LINK_NAME );
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
 @ModelAttribute( "eventGroup" ) EventGroup eventGroupOnSession,
			@RequestParam( value = "eventId" , required= false ) String eventId,
			@RequestParam( value = "publicationId" , required= false ) final String publicationId,
			@RequestParam( value = "type" , required= false ) final String type,
			@RequestParam( value = "volume" , required= false ) final String volume,
			@RequestParam( value = "year" , required= false ) final String year,
			final HttpServletResponse response) throws InterruptedException
	{

		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		
		// check for duplication
		// check dblpurl or notation with existing event group on db
		EventGroup eventGroup = null;
		
		eventGroup = persistenceStrategy.getEventGroupDAO().getSimilarEventGroup( eventGroupOnSession );

		if ( eventGroup != null )
		{
			eventGroup.setDblpUrl( eventGroupOnSession.getDblpUrl() );
			eventGroup.setName( eventGroupOnSession.getName() );
			if ( !eventGroupOnSession.getNotation().isEmpty() )
				eventGroup.setNotation( eventGroupOnSession.getNotation() );
			eventGroup.setDescription( eventGroupOnSession.getDescription() );
		}
		else
			eventGroup = eventGroupOnSession;

		if ( eventGroup == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "error event not found on session" );
		}

		// set flag added to true
		eventGroup.setAdded( true );
		// set event type, incase changed
		if ( type != null )
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
		if ( publication != null )
		{
			// create new event
			Event event = new Event();
			// event.setName( eventGroup.getName() );
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
		if ( publicationId != null )
			responseMap.put( "publicationId", publicationId );
		return responseMap;
	}
	
	/**
	 * Load the add event Edit form together with eventGroup  object
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/eventGroup/edit", method = RequestMethod.GET )
	public ModelAndView editEventGroup( 
			@RequestParam( value = "id") final String eventGroupId,
			final HttpServletResponse response) throws InterruptedException
	{
		ModelAndView model = null;

		if ( securityService.getUser() == null )
		{
			model = TemplateHelper.createViewWithLink( "401", "error" );
			return model;
		}
		
		if( eventGroupId == null ){
			model = TemplateHelper.createViewWithLink( "404", "error" );
			return model;
		}

		model = TemplateHelper.createViewWithLink( "dialogIframeLayout", LINK_NAME );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.CONFERENCE, "edit" );

		// create blank EventGroup
		EventGroup eventGroup = persistenceStrategy.getEventGroupDAO().getById( eventGroupId );

		if( eventGroup == null ){
			model = TemplateHelper.createViewWithLink( "404", "error" );
			return model;
		}
		
		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "eventGroup", eventGroup );

		return model;
	}
	
	/**
	 * Load the add event Edit form together with eventGroup object
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/eventGroup/edit", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> saveEventGroup( 
			@ModelAttribute( "eventGroup" ) EventGroup eventGroup, 
			final HttpServletResponse response)
	{
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		
		if ( eventGroup == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "failed to save, expired session" );
			return responseMap;
		}
		
		// set event type, incase changed
		if ( eventGroup.getType() != null )
		{
			try
			{
				PublicationType pubType = PublicationType.valueOf( eventGroup.getType().toUpperCase() );

				if ( !eventGroup.getPublicationType().equals( pubType ) )
				{
					eventGroup.setPublicationType( pubType );

					for ( Event event : eventGroup.getEvents() )
					{
						if ( event.isAdded() )
						{
							// use autowire, since Publication is Lazy loaded
							List<Publication> publications = persistenceStrategy.getPublicationDAO().getPublicationByEventWithPaging( event, null, null );

							if ( publications != null && !publications.isEmpty() )
								for ( Publication publication : publications )
								{
									publication.setPublicationType( pubType );
									persistenceStrategy.getPublicationDAO().persist( publication );
								}
						}
					}
				}
			}
			catch ( Exception e )
			{
			}
		}

		persistenceStrategy.getEventGroupDAO().persist( eventGroup );

		responseMap.put( "status", "ok" );
		responseMap.put( "statusMessage", "author saved" );

		Map<String, String> eventGroupMap = new LinkedHashMap<String, String>();
		eventGroupMap.put( "id", eventGroup.getId() );
		eventGroupMap.put( "name", eventGroup.getName() );
		responseMap.put( "eventGroup", eventGroupMap );
		
		return responseMap;
	}
	
	
	/**
	 * Load the add event Edit form together with event object
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/event/edit", method = RequestMethod.GET )
	public ModelAndView editEvent( 
			@RequestParam( value = "id") final String eventId,
			final HttpServletResponse response) throws InterruptedException
	{
		ModelAndView model = null;

		if ( securityService.getUser() == null )
		{
			model = TemplateHelper.createViewWithLink( "401", "error" );
			return model;
		}
		
		if ( eventId == null )
		{
			model = TemplateHelper.createViewWithLink( "404", "error" );
			return model;
		}

		model = TemplateHelper.createViewWithLink( "dialogIframeLayout", LINK_NAME );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.CONFERENCE, "edit" );

		// create blank EventGroup
		Event event = persistenceStrategy.getEventDAO().getById( eventId );

		if ( event == null )
		{
			model = TemplateHelper.createViewWithLink( "404", "error" );
			return model;
		}
		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "event", event );

		return model;
	}

	@Transactional
	@RequestMapping( value = "/delete", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> deletePublication( @RequestParam( value = "id" ) final String id, HttpServletRequest request, HttpServletResponse response )
	{
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		if ( id == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "Event Group id missing" );
			return responseMap;
		}

		EventGroup eventGroup = persistenceStrategy.getEventGroupDAO().getById( id );

		if ( eventGroup == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "Event Group not found" );
			return responseMap;
		}

		if ( !securityService.isAuthorizedForRole( "ADMIN" ) )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "error 401 - not authorized" );
			return responseMap;
		}

		// remove eventgroup connection
		eventGroup.getUserEventGroupBookmarks().clear();
		// remove connection with publications
		if( eventGroup.getEvents() != null ){
			for( Event event : eventGroup.getEvents()){
				if( event.getPublications() == null )
					continue;
				
				// remove links with publicatins
				String venue = eventGroup.getName();
				if ( !venue.equals( eventGroup.getNotation() ) )
				{
					venue += " (" + eventGroup.getNotation() + ")";
				}

				for ( Publication publication : event.getPublications() )
				{
					publication.addOrUpdateAdditionalInformation( "venue", venue );
					publication.setEvent( null );
					persistenceStrategy.getPublicationDAO().persist( publication );
				}
				event.getPublications().clear();
				event.setEventGroup( null );
				persistenceStrategy.getEventDAO().delete( event );
			}
		}

		eventGroup.getEvents().clear();
		persistenceStrategy.getEventGroupDAO().delete( eventGroup );

		responseMap.put( "status", "ok" );
		responseMap.put( "statusMessage", "Event Group is deleted" );

		return responseMap;
	}
}
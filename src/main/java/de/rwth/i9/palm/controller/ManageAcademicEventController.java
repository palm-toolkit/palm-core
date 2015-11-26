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

import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.EventGroup;
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
			final HttpServletResponse response) throws InterruptedException
	{
		// get current session object
		SessionDataSet sessionDataSet = this.appService.getCurrentSessionDataSet();

		// set model and view
		ModelAndView model = TemplateHelper.createViewWithSessionDataSet( "dialogIframeLayout", LINK_NAME, sessionDataSet );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.CONFERENCE, "add" );

		// create blank EventGroup
		EventGroup eventGroup = new EventGroup();

		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "eventGroup", eventGroup );

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
			final HttpServletResponse response) throws InterruptedException
	{

		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

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
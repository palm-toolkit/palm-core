package de.rwth.i9.palm.controller.administration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.SessionDataSet;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetSource;
import de.rwth.i9.palm.model.WidgetStatus;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.model.WidgetWidth;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.ApplicationContextService;

@Controller
@RequestMapping( value = "/admin/widget" )
public class ManageWidgetController
{
	private static final String LINK_NAME = "administration";

	@Autowired
	private ApplicationContextService appService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Transactional
	@RequestMapping( value = "/overview", method = RequestMethod.GET )
	public ModelAndView overviewWidget( @RequestParam( value = "sessionid", required = false ) final String sessionId, final HttpServletResponse response ) throws InterruptedException
	{
		// get current session object
		SessionDataSet sessionDataSet = this.appService.getCurrentSessionDataSet();

		// set model and view
		ModelAndView model = TemplateHelper.createViewWithSessionDataSet( "widgetLayout", LINK_NAME, sessionDataSet );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.ADMINISTRATION, "add" );

		// get all widget enums
		List<WidgetType> widgetTypes = new ArrayList<WidgetType>( Arrays.asList( WidgetType.values() ) );
		List<WidgetSource> widgetSources = new ArrayList<WidgetSource>( Arrays.asList( WidgetSource.values() ) );
		List<WidgetWidth> widgetWidths = new ArrayList<WidgetWidth>( Arrays.asList( WidgetWidth.values() ) );

		// TODO: widget group based on widgetTypes

		model.addObject( "widgets", widgets );
		model.addObject( "widgetTypes", widgetTypes );
		model.addObject( "widgetSources", widgetSources );
		model.addObject( "widgetWidths", widgetWidths );

		return model;
	}

	@Transactional
	@RequestMapping( value = "/add", method = RequestMethod.GET )
	public ModelAndView addWidget( 
			@RequestParam( value = "sessionid", required = false ) final String sessionId, 
			final HttpServletResponse response ) throws InterruptedException
	{
		// get current session object
		SessionDataSet sessionDataSet = this.appService.getCurrentSessionDataSet();

		// set model and view
		ModelAndView model = TemplateHelper.createViewWithSessionDataSet( "widgetLayout", LINK_NAME, sessionDataSet );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.ADMINISTRATION, "add" );
		
		// get all widget enums
		List<WidgetType> widgetTypes = new ArrayList<WidgetType>( Arrays.asList( WidgetType.values() ) );
		List<WidgetSource> widgetSources = new ArrayList<WidgetSource>( Arrays.asList( WidgetSource.values() ) );
		List<WidgetWidth> widgetWidths = new ArrayList<WidgetWidth>( Arrays.asList( WidgetWidth.values() ) );
		List<WidgetStatus> widgetStatuss = new ArrayList<WidgetStatus>( Arrays.asList( WidgetStatus.values() ) );

		// TODO: widget group based on widgetTypes

		model.addObject( "widgets" , widgets );
		model.addObject( "widgetTypes", widgetTypes );
		model.addObject( "widgetSources", widgetSources );
		model.addObject( "widgetWidths", widgetWidths );
		model.addObject( "widgetStatuss", widgetStatuss );

		return model;
	}

	@Transactional
	@RequestMapping( value = "/add", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> saveNewWidget( 
			@RequestParam( value="widgetTitle" ) String widgetTitle,
			@RequestParam( value="widgetType" ) String widgetType,
			@RequestParam( value="widgetGroup" ) String widgetGroup,
			@RequestParam( value="widgetSource" ) String widgetSource,
			@RequestParam( value="widgetSourcePath" ) String widgetSourcePath,
			@RequestParam( value="widgetWidth" ) String widgetWidth,
			@RequestParam( value="widgetInfo" ) String widgetInfo,
			@RequestParam( value="widgetClose" ) boolean widgetClose,
			@RequestParam( value="widgetMinimize" ) boolean widgetMinimize,
			@RequestParam( value="widgetStatus" ) String widgetStatus,
			final HttpServletResponse response 
			)
	{
		// create new widget object and set its attributes
		Widget widget = new Widget();
		widget.setTitle( widgetTitle );
		widget.setWidgetType( WidgetType.valueOf( widgetType ) );
		widget.setWidgetGroup( widgetGroup );
		widget.setWidgetSource( WidgetSource.valueOf( widgetSource ) );
		widget.setSourcePath( widgetSourcePath );
		widget.setWidgetWidth( WidgetWidth.valueOf( widgetWidth ) );
		widget.setInformation( widgetInfo );
		widget.setCloseEnabled( widgetClose );
		widget.setMinimizeEnabled( widgetMinimize );
		widget.setWidgetStatus( WidgetStatus.valueOf( widgetStatus ) );
		// save into database
		persistenceStrategy.getWidgetDAO().persist( widget );

		// create JSON mapper for response
		Map<String, Object> responseMap = new HashMap<String, Object>();
		responseMap.put( "format", "json" );
		responseMap.put( "result", "success" );

		return responseMap;
	}

}
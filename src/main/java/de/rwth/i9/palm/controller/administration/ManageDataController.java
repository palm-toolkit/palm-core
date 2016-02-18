package de.rwth.i9.palm.controller.administration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.helper.comparator.SourceByNaturalOrderComparator;
import de.rwth.i9.palm.model.Source;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.SecurityService;

@Controller
@RequestMapping( value = "/admin/data" )
public class ManageDataController
{
	private static final String LINK_NAME = "administration";

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private SecurityService securityService;

	/**
	 * Load the source detail form
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/{pageType}", method = RequestMethod.GET )
	public ModelAndView getSources( 
			@PathVariable String pageType, 
			final HttpServletResponse response 
			) throws InterruptedException
	{
		ModelAndView model = null;

		if ( !securityService.isAuthorizedForRole( "ADMIN" ) )
		{
			model = TemplateHelper.createViewWithLink( "401", "error" );
			return model;
		}

		model = TemplateHelper.createViewWithLink( "widgetLayoutAjax", LINK_NAME );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.ADMINISTRATION, "data-" + pageType );

		// get list of sources and sort
		List<Source> sources = persistenceStrategy.getSourceDAO().getAllSource();
		Collections.sort( sources, new SourceByNaturalOrderComparator() );

		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "header", pageType );

		return model;
	}

	/**
	 * Removing researcher from database or make it not visible
	 * 
	 * @param config
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/remove/researcher", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> removeResearcher( @RequestParam( value = "id", required = false ) String id, final HttpServletResponse response) throws InterruptedException
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		responseMap.put( "status", "ok" );
		responseMap.put( "format", "json" );

		return responseMap;
	}

	/**
	 * Removing publication from database or make it not visible
	 * 
	 * @param config
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/remove/publication", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> removePublication( @RequestParam( value = "id", required = false ) String id, final HttpServletResponse response) throws InterruptedException
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		responseMap.put( "status", "ok" );
		responseMap.put( "format", "json" );

		return responseMap;
	}

	/**
	 * Removing conference from database or make it not visible
	 * 
	 * @param config
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/remove/conference", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> removeConference( @RequestParam( value = "id", required = false ) String id, final HttpServletResponse response) throws InterruptedException
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		responseMap.put( "status", "ok" );
		responseMap.put( "format", "json" );

		return responseMap;
	}

	/**
	 * Removing circle from database or make it not visible
	 * 
	 * @param config
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/remove/circle", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> removeCircle( 
			@RequestParam(  value ="id", required = false ) String id, 
			final HttpServletResponse response ) throws InterruptedException
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();


		responseMap.put( "status", "ok" );
		responseMap.put( "format", "json" );

		return responseMap;
	}


}
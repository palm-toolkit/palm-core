package de.rwth.i9.palm.controller;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.persistence.PersistenceStrategy;
//import de.rwth.i9.palm.analytics.api.PalmAnalytics;

@Controller
@RequestMapping( value = "/analytics" )
public class AnalyticsController
{

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	/*@Autowired
	private PalmAnalytics analytics;*/

	@RequestMapping( value = "/dialog", method = RequestMethod.GET )
	@Transactional
	public ModelAndView landing( @RequestParam( value = "sessionid", required = false ) final String sessionId, final HttpServletResponse response ) throws InterruptedException
	{
		ModelAndView model = new ModelAndView( "getAnalyticsView", "link", "analytics" );

		if ( sessionId != null && sessionId.equals( "0" ) )
			response.setHeader( "SESSION_INVALID", "yes" );

		return model;
	}

}
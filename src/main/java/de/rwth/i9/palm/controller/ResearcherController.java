package de.rwth.i9.palm.controller;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Controller
@RequestMapping( value = "/researcher" )
public class ResearcherController
{

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	/**
	 * 
	 * @param response
	 *            HttpServletResponse
	 * @return the researcher view
	 */
	@RequestMapping( method = RequestMethod.GET )
	public ModelAndView landing( final HttpServletResponse response )
	{
		ModelAndView model = new ModelAndView( "researcher", "link", "researcher" );


		return model;
	}

}
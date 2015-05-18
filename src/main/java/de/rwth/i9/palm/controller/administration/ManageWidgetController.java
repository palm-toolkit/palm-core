package de.rwth.i9.palm.controller.administration;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Controller
@RequestMapping( value = "/admin/widget" )
public class ManageWidgetController
{

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Transactional
	@RequestMapping( value="/{urlType}", method = RequestMethod.GET )
	public ModelAndView addWidget( 
			@PathVariable final String urlType,
			@RequestParam( value = "sessionid", required = false ) final String sessionId, 
			final HttpServletResponse response ) throws InterruptedException
	{
		ModelAndView model = new ModelAndView( "widgetLayout", "link", "widget-add" );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getWidgetByWidgetTypeAndGroup( WidgetType.ADMINISTRATION, urlType );
		
		model.addObject( "widgets" , widgets );
		return model;
	}

}
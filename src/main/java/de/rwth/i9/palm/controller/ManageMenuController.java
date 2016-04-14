package de.rwth.i9.palm.controller;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Controller
@SessionAttributes( "menu" )
@RequestMapping( value = "/menu" )
public class ManageMenuController
{
	private static final String LINK_NAME = "menu";

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	/**
	 * Load introduction menu form together with menu object
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/introduction", method = RequestMethod.GET )
	public ModelAndView addNewmenu(
			final HttpServletResponse response) throws InterruptedException
	{
		ModelAndView model = null;

		model = TemplateHelper.createViewWithLink( "dialogIframeLayout", LINK_NAME );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.MENU, "menu-introduction" );

		// assign the model
		model.addObject( "widgets", widgets );

		return model;
	}

}
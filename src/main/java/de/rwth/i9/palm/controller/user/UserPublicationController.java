package de.rwth.i9.palm.controller.user;

import java.util.ArrayList;
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
import de.rwth.i9.palm.model.User;
import de.rwth.i9.palm.model.UserWidget;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetStatus;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.SecurityService;

@Controller
@SessionAttributes( "sourceListWrapper" )
@RequestMapping( value = "/user/publication" )
public class UserPublicationController
{
	private static final String LINK_NAME = "user";

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
	@RequestMapping( method = RequestMethod.GET )
	public ModelAndView getUserPublicationPage(
			final HttpServletResponse response) throws InterruptedException
	{
		// set model and view
		ModelAndView model = TemplateHelper.createViewWithLink( "widgetLayoutAjax", LINK_NAME);   
		List<Widget> widgets = new ArrayList<Widget>();
		
		User user =  securityService.getUser();
		
		List<UserWidget> userWidgets = persistenceStrategy.getUserWidgetDAO().getWidget( user, WidgetType.USER, WidgetStatus.ACTIVE );
		for ( UserWidget userWidget : userWidgets )
		{
			Widget widget = userWidget.getWidget();
			widget.setColor( userWidget.getWidgetColor() );
			widget.setWidgetHeight( userWidget.getWidgetHeight() );
			widget.setWidgetWidth( userWidget.getWidgetWidth() );
			widget.setPosition( userWidget.getPosition() );

			widgets.add( widget );
		}
		
		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "user", user);
						 
		return model;
	}

}
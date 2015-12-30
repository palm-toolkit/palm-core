package de.rwth.i9.palm.controller;

import java.util.Collections;
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
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.SessionDataSet;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.pdfextraction.service.PdfExtractionService;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.ApplicationContextService;
import de.rwth.i9.palm.service.ApplicationService;

@Controller
@SessionAttributes( "circle" )
@RequestMapping( value = "/circle" )
public class ManageCircleController
{
	private static final String LINK_NAME = "circle";

	@Autowired
	private ApplicationContextService appService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private ApplicationService applicationService;

	@Autowired
	private PdfExtractionService pdfExtractionService;

	/**
	 * Load the add circle form together with circle object
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/add", method = RequestMethod.GET )
	public ModelAndView addNewCircle( 
			@RequestParam( value = "sessionid", required = false ) final String sessionId, 
			final HttpServletResponse response) throws InterruptedException
	{
		// get current session object
		SessionDataSet sessionDataSet = this.appService.getCurrentSessionDataSet();

		// set model and view
		ModelAndView model = TemplateHelper.createViewWithSessionDataSet( "dialogIframeLayout", LINK_NAME, sessionDataSet );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.CIRCLE, "add" );

		// create blank Circle
		Circle circle = new Circle();

		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "circle", circle );

		return model;
	}

	/**
	 * Save changes from Add circle detail, via Spring binding
	 * 
	 * @param extractionServiceListWrapper
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/add", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> saveNewCircle( 
			@ModelAttribute( "circle" ) Circle circle,
			@RequestParam( value = "circleResearcher") final String circleResearcherIds,
			@RequestParam( value = "circlePublication") final String circlePublicationIds,
			final HttpServletResponse response) throws InterruptedException
	{
		if ( circle == null )
			return Collections.emptyMap();

		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		if ( !circleResearcherIds.equals( "" ) )
		{
			// split by underscore
			String[] idsArray = circleResearcherIds.split( "_" );

			for ( String researcherId : idsArray )
			{
				Author author = persistenceStrategy.getAuthorDAO().getById( researcherId );
				if ( author != null )
					circle.addAuthor( author );
			}
		}

		if ( !circlePublicationIds.equals( "" ) )
		{
			// split by underscore
			String[] idsArray = circlePublicationIds.split( "_" );

			for ( String researcherId : idsArray )
			{
				Publication publication = persistenceStrategy.getPublicationDAO().getById( researcherId );
				if ( publication != null )
					circle.addPublication( publication );
			}
		}

		persistenceStrategy.getCircleDAO().persist( circle );

		return responseMap;
	}

	/**
	 * Load the add circle form together with circle object
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/edit", method = RequestMethod.GET )
	public ModelAndView editCircle( 
			@RequestParam( value = "sessionid", required = false ) final String sessionId,
			@RequestParam( value = "id") final String circleId,
			final HttpServletResponse response) throws InterruptedException
	{
		// get current session object
		SessionDataSet sessionDataSet = this.appService.getCurrentSessionDataSet();

		// set model and view
		ModelAndView model = TemplateHelper.createViewWithSessionDataSet( "dialogIframeLayout", LINK_NAME, sessionDataSet );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.CIRCLE, "edit" );

		// create blank Circle
		Circle circle = persistenceStrategy.getCircleDAO().getById( circleId );

		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "circle", circle );

		return model;
	}
}
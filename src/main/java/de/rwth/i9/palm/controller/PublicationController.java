package de.rwth.i9.palm.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.feature.publication.PublicationFeature;
import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.SessionDataSet;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetStatus;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.ApplicationContextService;

@Controller
@SessionAttributes( { "sessionDataSet" } )
@RequestMapping( value = "/publication" )
public class PublicationController
{
	private static final String LINK_NAME = "publication";

	@Autowired
	private ApplicationContextService appService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private PublicationFeature publicationFeature;

	/**
	 * Get the publication page
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@RequestMapping( method = RequestMethod.GET )
	@Transactional
	public ModelAndView publicationPage( 
			@RequestParam( value = "sessionid", required = false ) final String sessionId, 
			@RequestParam( value = "id", required = false ) final String publicationId, 
			final HttpServletResponse response ) throws InterruptedException
	{
		// get current session object
		SessionDataSet sessionDataSet = this.appService.getCurrentSessionDataSet();

		// set model and view
		ModelAndView model = TemplateHelper.createViewWithSessionDataSet( "publication", LINK_NAME, sessionDataSet );

		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getWidget( WidgetType.PUBLICATION, WidgetStatus.DEFAULT );
		// assign the model
		model.addObject( "widgets", widgets );

		if ( publicationId != null )
			model.addObject( "publicationId", publicationId );

		return model;
	}

	/**
	 * Get the list of publications based on the following parameters
	 * 
	 * @param query
	 * @param eventName
	 * @param eventId
	 * @param page
	 * @param maxresult
	 * @param response
	 * @return JSON Map
	 */
	@Transactional
	@RequestMapping( value = "/search", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getPublicationList( 
			@RequestParam( value = "publicationId", required = false ) String publicationId,
			@RequestParam( value = "query", required = false ) String query,
			@RequestParam( value = "event", required = false ) String eventName,
			@RequestParam( value = "eventid", required = false ) String eventId,
			@RequestParam( value = "page", required = false ) Integer page, 
			@RequestParam( value = "maxresult", required = false ) Integer maxresult, 
			final HttpServletResponse response )
	{
		return publicationFeature.getPublicationSearch().getPublicationListByQueryAndEvent( query, eventName, eventId, page, maxresult );
	}

	/**
	 * Get details( publication content ) from a publication
	 * 
	 * @param id
	 *            of publication
	 * @param uri
	 * @param response
	 * @return JSON Map
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ExecutionException
	 */
	@RequestMapping( value = "/detail", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getPublicationDetail( 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "uri", required = false ) final String uri, 
			final HttpServletResponse response) throws InterruptedException, IOException, ExecutionException
	{
		return publicationFeature.getPublicationDetail().getPublicationDetailById( id );
	}
	
	/**
	 * Get the basic statistic (publication type, language, etc) from a
	 * publication
	 * 
	 * @param id
	 *            of publication
	 * @param uri
	 * @param response
	 * @return JSON Map
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ExecutionException
	 */
	@RequestMapping( value = "/basicstatistic", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getPublicationBasicStatistic( 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "uri", required = false ) final String uri, 
			final HttpServletResponse response) throws InterruptedException, IOException, ExecutionException
	{
		return publicationFeature.getPublicationBasicStatistic().getPublicationBasicStatisticById( id );
	}
	
	/**
	 * Extract Pdf on a specific publication
	 * 
	 * @param id
	 *            of publication
	 * @param response
	 * @return JSON Map
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ExecutionException
	 */
	@RequestMapping( value = "/pdfExtract", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> doPdfExtraction( 
			@RequestParam( value = "id", required = false ) final String id, 
			final HttpServletResponse response) throws InterruptedException, IOException, ExecutionException
	{
		return publicationFeature.getPublicationManage().extractPublicationFromPdf( id );
	}
	
	@RequestMapping( value = "/pdfExtractTest", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> doPdfExtractionTest( @RequestParam( value = "url", required = false ) final String url, final HttpServletResponse response) throws InterruptedException, IOException, ExecutionException
	{
		return publicationFeature.getPublicationApi().extractPfdFile( url );
	}

	@RequestMapping( value = "/htmlExtractTest", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, String> doHtmlExtractionTest( @RequestParam( value = "url", required = false ) final String url, final HttpServletResponse response) throws InterruptedException, IOException, ExecutionException
	{
		return publicationFeature.getPublicationApi().extractHtmlFile( url );
	}

	/**
	 * Get list of PuiblicationTopic
	 * 
	 * @param id
	 * @param response
	 * @return
	 */
	@RequestMapping( value = "/topic", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getPublicationTopic( 
			@RequestParam( value = "id", required = false ) final String id, 
 @RequestParam( value = "maxRetrieve", required = false ) final String maxRetrieve, 
			final HttpServletResponse response)
	{
		return publicationFeature.getPublicationMining().getPublicationExtractedTopicsById( id, maxRetrieve );
	}
}
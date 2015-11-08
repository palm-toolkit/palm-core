package de.rwth.i9.palm.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.datasetcollect.service.PublicationCollectionService;
import de.rwth.i9.palm.feature.researcher.ResearcherFeature;
import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.SessionDataSet;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetStatus;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.ApplicationContextService;

@Controller
@RequestMapping( value = "/researcher" )
public class ResearcherController
{

	private static final String LINK_NAME = "researcher";

	@Autowired
	private ApplicationContextService appService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;
	
	@Autowired
	private ResearcherFeature researcherFeature;
	
	@Autowired
	private PublicationCollectionService publicationCollectionService;

	/**
	 * Landing page of researcher page
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@RequestMapping( method = RequestMethod.GET )
	@Transactional
	public ModelAndView mainPage( 
			@RequestParam( value = "sessionid", required = false ) final String sessionId, 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "name", required = false ) final String name,
			final HttpServletResponse response ) throws InterruptedException
	{
		// get current session object
		SessionDataSet sessionDataSet = this.appService.getCurrentSessionDataSet();

		// set model and view
		ModelAndView model = TemplateHelper.createViewWithSessionDataSet( "researcher", LINK_NAME, sessionDataSet );

		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getWidget( WidgetType.RESEARCHER, WidgetStatus.DEFAULT );
		// assign the model
		model.addObject( "widgets", widgets );
		return model;
	}

	/**
	 * Get list of author given query ( author name )
	 * 
	 * @param query
	 * @param page
	 * @param maxresult
	 * @param response
	 * @return JSON Maps of response with researcher list
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws OAuthProblemException 
	 * @throws OAuthSystemException 
	 * @throws org.apache.http.ParseException 
	 */
	@Transactional
	@RequestMapping( value = "/search", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getAuthorList( 
			@RequestParam( value = "query", required = false ) String query, 
			@RequestParam( value = "page", required = false ) Integer page, 
			@RequestParam( value = "maxresult", required = false ) Integer maxresult, 
			final HttpServletResponse response ) throws IOException, InterruptedException, ExecutionException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
	{
		return researcherFeature.getResearcherSearch().getResearcherListByQuery( query, page, maxresult );
	}
	
	/**
	 * Fetch author data, mining author information and publication from
	 * academic network if necessary
	 * 
	 * @param id
	 * @param name
	 * @param uri
	 * @param affiliation
	 * @param force
	 * @param response
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ExecutionException
	 * @throws ParseException
	 * @throws TimeoutException 
	 * @throws org.apache.http.ParseException 
	 * @throws OAuthProblemException 
	 * @throws OAuthSystemException 
	 */
	@RequestMapping( value = "/fetch", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> researcherFetchNetworkDataset( 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "name", required = false ) final String name, 
			@RequestParam( value = "uri", required = false ) final String uri,
			@RequestParam( value = "affiliation", required = false ) final String affiliation,
			@RequestParam( value = "pid", required = false ) final String pid, 
			@RequestParam( value = "force", required = false ) final String force,
			final HttpServletResponse response ) throws InterruptedException, IOException, ExecutionException, ParseException, TimeoutException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
	{
		return researcherFeature.getResearcherSearch().fetchResearcherData( id, name, uri, affiliation, pid, force );
	}
	
	/**
	 * Get author interest
	 * 
	 * @param authorId
	 * @param name
	 * @param extractionServiceType
	 * @param startDate
	 * @param endDate
	 * @param response
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ExecutionException
	 * @throws URISyntaxException
	 * @throws ParseException
	 */
	@RequestMapping( value = "/interest", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> researcherInterest( 
			@RequestParam( value = "id", required = false ) final String authorId, 
			@RequestParam( value = "name", required = false ) final String name, 
			@RequestParam( value = "extractType", required = false ) final String extractionServiceType,
			@RequestParam( value = "startDate", required = false ) final String startDate,
			@RequestParam( value = "endDate", required = false ) final String endDate,
			final HttpServletResponse response ) throws InterruptedException, IOException, ExecutionException, URISyntaxException, ParseException
	{
		if ( name != null )
			return researcherFeature.getResearcherInterest().getAuthorInterestByName( name, extractionServiceType, startDate, endDate );
		else
			return researcherFeature.getResearcherInterest().getAuthorInterestById( authorId, extractionServiceType, startDate, endDate );
		// return Collections.emptyMap();
	}
	
	@RequestMapping( value = "/enrich", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> researcherEnrich( @RequestParam( value = "id", required = false ) final String authorId, final HttpServletResponse response) throws InterruptedException, IOException, ExecutionException, URISyntaxException, ParseException, TimeoutException
	{
		// id=90522536-4717-4fa3-ac43-b3f6300ad6c4
		Author author = persistenceStrategy.getAuthorDAO().getById( authorId );
		publicationCollectionService.enrichPublicationByExtractOriginalSources( new ArrayList<Publication>( author.getPublications() ), author, true );
		return Collections.emptyMap();
	}

	@RequestMapping( value = "/interestEvolution", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> researcherInterestEvolution( 
			@RequestParam( value = "id", required = false ) final String authorId, 
			@RequestParam( value = "name", required = false ) final String name, 
			@RequestParam( value = "extractType", required = false ) final String extractionServiceType,
			@RequestParam( value = "startDate", required = false ) final String startDate,
			@RequestParam( value = "endDate", required = false ) final String endDate,
			final HttpServletResponse response ) 
					throws InterruptedException, IOException, ExecutionException
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		
//		// get author
//		Author author = this.getTargetAuthor( responseMap, id, name, uri, affiliation );
//		if( author == null )
//			return responseMap;

		return null;
	}
	
	@RequestMapping( value = "/getResearcherAutocomplete", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getPublicationBasicStatistic( 
			@RequestParam( value = "name", required = false ) final String name, 
			final HttpServletResponse response) throws InterruptedException, IOException, ExecutionException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
	{
		return researcherFeature.getResearcherApi().getAuthorAutoCompleteFromNetworkAndDb( name );
	}
	
	@RequestMapping( value = "/publicationList", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getPublicationList( @RequestParam( value = "id", required = false ) final String authorId, final HttpServletResponse response)
	{
		return researcherFeature.getResearcherPublication().getPublicationListByAuthorId( authorId );
	}

}
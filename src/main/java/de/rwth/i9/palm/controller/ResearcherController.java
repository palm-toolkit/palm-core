package de.rwth.i9.palm.controller;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;

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
import de.rwth.i9.palm.feature.researcher.ResearcherInterest;
import de.rwth.i9.palm.feature.researcher.ResearcherInterestEvolution;
import de.rwth.i9.palm.helper.DateTimeHelper;
import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorSource;
import de.rwth.i9.palm.model.RequestType;
import de.rwth.i9.palm.model.SessionDataSet;
import de.rwth.i9.palm.model.SourceType;
import de.rwth.i9.palm.model.UserRequest;
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
	private PublicationCollectionService publicationCollectionService;
	
	@Autowired
	private ResearcherFeature researcherFeature;
	
	@RequestMapping( method = RequestMethod.GET )
	@Transactional
	public ModelAndView mainPage( 
			@RequestParam( value = "sessionid", required = false ) final String sessionId, 
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

	@Transactional
	@RequestMapping( value = "/search", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getAuthorList( 
			@RequestParam( value = "query", required = false ) String query, 
			@RequestParam( value = "page", required = false ) Integer page, 
			@RequestParam( value = "maxresult", required = false ) Integer maxresult, 
			final HttpServletResponse response ) throws IOException, InterruptedException, ExecutionException
	{
		boolean collectFromNetwork = false;
		
		if ( query == null )
			query = "";

		if ( page == null )
			page = 0;

		if ( maxresult == null )
			maxresult = 50;

		if ( !query.equals( "" ) && page == 0 )
		{
			// check whether the author query ever executed before
			UserRequest userRequest = persistenceStrategy.getUserRequestDAO().getByTypeAndQuery( RequestType.SEARCHAUTHOR, query );

			// get current timestamp
			java.util.Date date = new java.util.Date();
			Timestamp currentTimestamp = new Timestamp( date.getTime() );

			if ( userRequest == null )
			{ // there is no kind of request before
				// perform fetching data through academic network
				collectFromNetwork = true;
				// persist current request
				userRequest = new UserRequest();
				userRequest.setQueryString( query );
				userRequest.setRequestDate( currentTimestamp );
				userRequest.setRequestType( RequestType.SEARCHAUTHOR );
				persistenceStrategy.getUserRequestDAO().persist( userRequest );
			}
			else
			{
				// check if the existing userRequest obsolete (longer than a
				// week)
				if ( DateTimeHelper.substractTimeStampToHours( currentTimestamp, userRequest.getRequestDate() ) > 24 * 7 )
				{
					// update current timestamp
					userRequest.setRequestDate( currentTimestamp );
					persistenceStrategy.getUserRequestDAO().persist( userRequest );
					collectFromNetwork = true;
				}
			}

			collectFromNetwork = true;
			// persistenceStrategy.getAuthorDAO().doReindexing();
			// collect author from network
			if ( collectFromNetwork )
			{
				// extract dataset from academic network concurrently
				//Stopwatch stopwatch = Stopwatch.createStarted();

				List<Future<List<Map<String, String>>>> authorFutureLists = new ArrayList<Future<List<Map<String, String>>>>();

				// ( if google scholar )
				authorFutureLists.add( publicationCollectionService.getListOfAuthorsGoogleScholar( query ) );

				// if( citeseer )
				authorFutureLists.add( publicationCollectionService.getListOfAuthorsCiteseerX( query ) );

				// Wait until they are all done
				boolean processIsDone = true;
				do
				{
					processIsDone = true;
					for ( Future<List<Map<String, String>>> futureList : authorFutureLists )
					{
						if ( !futureList.isDone() )
						{
							processIsDone = false;
							break;
						}
					}
					// 10-millisecond pause between each check
					Thread.sleep( 10 );
				} while ( !processIsDone );

				// merge the result
				publicationCollectionService.mergeAuthorInformation( authorFutureLists );
			}
		}

		// get the researcher
		Map<String, Object> researcherMap = null;
		if ( collectFromNetwork )
			researcherMap = persistenceStrategy.getAuthorDAO().getAuthorWithPaging( query, page, maxresult );
		else
			researcherMap = persistenceStrategy.getAuthorDAO().getAuthorByFullTextSearchWithPaging( query, page, maxresult );

		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		responseMap.put( "query", query );
		responseMap.put( "page", page );
		responseMap.put( "maxresult", maxresult );

		// create the json structure for researcher list
		if ( researcherMap != null )
		{
			responseMap.put( "count", researcherMap.get( "count" ) );

			@SuppressWarnings( "unchecked" )
			List<Author> researchers = (List<Author>) researcherMap.get( "result" );
			List<Map<String, String>> researcherList = new ArrayList<Map<String, String>>();

			for ( Author researcher : researchers )
			{
				Map<String, String> pub = new LinkedHashMap<String, String>();
				pub.put( "id", researcher.getId() );
				pub.put( "name", researcher.getName() );
				if ( researcher.getPhotoUrl() != null )
					pub.put( "photo", researcher.getPhotoUrl() );

				String otherDetail = "";
				if ( researcher.getOtherDetail() != null )
					otherDetail += researcher.getOtherDetail();
				if ( researcher.getDepartment() != null )
					otherDetail += ", " + researcher.getDepartment();
				if ( !otherDetail.equals( "" ) )
					pub.put( "detail", otherDetail );
				if ( researcher.getInstitution() != null )
					pub.put( "aff", researcher.getInstitution().getName() );
				if ( researcher.getCitedBy() > 0 )
					pub.put( "citedBy", Integer.toString( researcher.getCitedBy() ) );

				researcherList.add( pub );
			}
			responseMap.put( "researcher", researcherList );

		}
		else
		{
			responseMap.put( "count", 0 );
		}

		return responseMap;
	}
	
	@RequestMapping( value = "/fetchNetworkDataset", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> researcherFetchNetworkDataset( 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "name", required = false ) final String name, 
			@RequestParam( value = "uri", required = false ) final String uri,
			@RequestParam( value = "affiliation", required = false ) final String affiliation,
			@RequestParam( value = "force", required = false ) final String force,
			final HttpServletResponse response ) 
 throws InterruptedException, IOException, ExecutionException, ParseException
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		
		// get author
		Author author = this.getTargetAuthor( responseMap, id, name, uri, affiliation );
		if( author == null )
			return responseMap;

		// check whether it is necessary to collect information from network
		if ( this.isFetchDatasetFromNetwork( author) || force.equals( "true" ) )
			this.fetchDatasetFromNetwork( responseMap, author );

		return responseMap;
	}
	
	@RequestMapping( value = "/interest", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> researcherInterest( 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "name", required = false ) final String name, 
			@RequestParam( value = "uri", required = false ) final String uri,
			@RequestParam( value = "affiliation", required = false ) final String affiliation,
			final HttpServletResponse response ) 
					throws InterruptedException, IOException, ExecutionException
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		
		// get author
		Author author = this.getTargetAuthor( responseMap, id, name, uri, affiliation );
		if( author == null )
			return responseMap;
		
		// get the object and set properties
		ResearcherInterest researcherInterest = researcherFeature.getResearcherInterest();
		researcherInterest.setResearcher( author );
		researcherInterest.setResultMap( responseMap );

		return researcherInterest.getResultMap();
	}
	
	@RequestMapping( value = "/interestEvolution", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> researcherInterestEvolution( 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "name", required = false ) final String name, 
			@RequestParam( value = "uri", required = false ) final String uri,
			@RequestParam( value = "affiliation", required = false ) final String affiliation,
			final HttpServletResponse response ) 
					throws InterruptedException, IOException, ExecutionException
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		
		// get author
		Author author = this.getTargetAuthor( responseMap, id, name, uri, affiliation );
		if( author == null )
			return responseMap;
		
		// get the object and set properties
		ResearcherInterestEvolution researcherInterestEvolution = researcherFeature.getResearcherInterestEvolution();
		researcherInterestEvolution.setResearcher( author );
		researcherInterestEvolution.setResultMap( responseMap );

		return researcherInterestEvolution.getResultMap();
	}
	
	/**
	 * Get author object based on query
	 * @param responseMap
	 * @param id
	 * @param name
	 * @param uri
	 * @param affiliation
	 * @return
	 */
	private Author getTargetAuthor(Map<String, Object> responseMap, String id, String name, String uri, String affiliation){
		Author author = null;
		if ( id == null && name == null && uri == null )
		{
			responseMap.put( "result", "error" );
			responseMap.put( "reason", "no author selected" );
		}
		else
		{
			
			if ( id != null )
				author = persistenceStrategy.getAuthorDAO().getById( id );
			else if ( uri != null )
				author = persistenceStrategy.getAuthorDAO().getByUri( uri );
			else if ( name != null && affiliation != null){
				List<Author> authors = persistenceStrategy.getAuthorDAO().getAuthorByNameAndInstitution( name, affiliation );
				if( !authors.isEmpty())
					author = authors.get( 0 );
			}
				 

			if ( author == null )
			{
				responseMap.put( "result", "error" );
				responseMap.put( "reason", "no author found" );
			}
			// add author information
			responseMap.put( "id", author.getId() );
			responseMap.put( "name", author.getName() );
		}
		return author;
	}
	
	/**
	 * Check whether fetching to network is necessary
	 * @param author
	 * @return
	 */
	private boolean isFetchDatasetFromNetwork( Author author ){
		// get current timestamp
		java.util.Date date = new java.util.Date();
		Timestamp currentTimestamp = new Timestamp( date.getTime() );
		if ( author.getRequestDate() != null )
		{
			// check if the existing author publication is obsolete
			if ( DateTimeHelper.substractTimeStampToHours( currentTimestamp, author.getRequestDate() ) > 24 * 7 )
			{
				// update current timestamp
				author.setRequestDate( currentTimestamp );
				persistenceStrategy.getAuthorDAO().persist( author );
				return true;
			}
		}
		else
		{
			// update current timestamp
			author.setRequestDate( currentTimestamp );
			persistenceStrategy.getAuthorDAO().persist( author );
			return true;
		}

		//return false;
		return true;
	}
	
	/**
	 * Fetch author publication from network
	 * 
	 * @param responseMap
	 * @param author
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws ParseException
	 */
	private void fetchDatasetFromNetwork( Map<String, Object> responseMap, Author author ) throws IOException, InterruptedException, ExecutionException, ParseException
	{
		// get author sources
		List<AuthorSource> authorSources = author.getAuthorSources();
		if ( authorSources == null )
		{
			// TODO update author sources
			responseMap.put( "result", "error" );
			responseMap.put( "reason", "no author sources found" );
		}

		// future list for publication list
		// extract dataset from academic network concurrently
		//Stopwatch stopwatch = Stopwatch.createStarted();

		List<Future<List<Map<String, String>>>> publicationFutureLists = new ArrayList<Future<List<Map<String, String>>>>();
		for ( AuthorSource authorSource : authorSources )
		{
			if ( authorSource.getSourceType() == SourceType.GOOGLESCHOLAR )
				publicationFutureLists.add( publicationCollectionService.getListOfPublicationsGoogleScholar( authorSource.getSourceUrl() ) );
			else if ( authorSource.getSourceType() == SourceType.CITESEERX )
				publicationFutureLists.add( publicationCollectionService.getListOfPublicationCiteseerX( authorSource.getSourceUrl() ) );
		}

		// Wait until they are all done
		boolean processIsDone = true;
		do
		{
			processIsDone = true;
			for ( Future<List<Map<String, String>>> futureList : publicationFutureLists )
			{
				if ( !futureList.isDone() )
				{
					processIsDone = false;
					break;
				}
			}
			// 10-millisecond pause between each check
			Thread.sleep( 10 );
		} while ( !processIsDone );

		// merge the result
		publicationCollectionService.mergePublicationInformation( publicationFutureLists , author);
	}
}
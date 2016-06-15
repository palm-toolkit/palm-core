package de.rwth.i9.palm.controller;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rwth.i9.palm.feature.circle.CircleFeature;
import de.rwth.i9.palm.feature.publication.PublicationFeature;
import de.rwth.i9.palm.feature.researcher.ResearcherFeature;
import de.rwth.i9.palm.graph.feature.GraphFeature;
import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorSource;
import de.rwth.i9.palm.model.Color;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.User;
import de.rwth.i9.palm.model.UserWidget;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetSource;
import de.rwth.i9.palm.model.WidgetStatus;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.model.WidgetWidth;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.SecurityService;

@Controller
@RequestMapping( value = "/explore" )
public class ExploreController
{
	private final static Logger log = LoggerFactory.getLogger( ExploreController.class );

	private static final String LINK_NAME = "explore";

	@Autowired
	private SecurityService securityService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private PublicationFeature publicationFeature;

	@Autowired
	private ResearcherFeature researcherFeature;

	@Autowired
	private CircleFeature circleFeature;

	@Autowired
	private GraphFeature graphFeature;

	// @Autowired
	// private PublicationCollectionService publicationCollectionService;

	// Use explore/createVAWidgets to create Visual Analytics Widgets in Explore
	@Transactional
	@RequestMapping( value = "/createVAWidgets", method = RequestMethod.GET )
	public void createVAWidgets( final HttpServletResponse response ) throws InterruptedException
	{
		List<Widget> existingWidgets = persistenceStrategy.getWidgetDAO().getAllWidgets();
		Boolean alreadyExist = false;
		for ( Widget widget : existingWidgets )
		{
			if ( widget.getWidgetType().equals( WidgetType.EXPLORE ) )
			{
				alreadyExist = true;
				break;
			}
		}
		if ( !alreadyExist )
		{
			// create RESEARCHER Widget in Explore
			Widget researchersWidget = new Widget();
			researchersWidget.setTitle( "Researchers" );
			researchersWidget.setUniqueName( "explore_researchers" );
			researchersWidget.setWidgetType( WidgetType.EXPLORE );
			researchersWidget.setWidgetGroup( "content" );
			researchersWidget.setWidgetSource( WidgetSource.INCLUDE );
			researchersWidget.setSourcePath( "../../explore/widget/researcherAnalytics.ftl" );
			researchersWidget.setWidgetWidth( WidgetWidth.HALF );
			researchersWidget.setColor( Color.YELLOW );
			researchersWidget.setInformation( "Visual Analytics widget for researchers/authors" );
			researchersWidget.setCloseEnabled( true );
			researchersWidget.setMinimizeEnabled( true );
			researchersWidget.setMoveableEnabled( true );
			researchersWidget.setHeaderVisible( true );
			researchersWidget.setWidgetStatus( WidgetStatus.DEFAULT );
			researchersWidget.setPosition( 0 );
			persistenceStrategy.getWidgetDAO().persist( researchersWidget );

			// create RESEARCHER Widget in Explore
			Widget topicsWidget = new Widget();
			topicsWidget.setTitle( "Topics/Interests" );
			topicsWidget.setUniqueName( "explore_topics" );
			topicsWidget.setWidgetType( WidgetType.EXPLORE );
			topicsWidget.setWidgetGroup( "content" );
			topicsWidget.setWidgetSource( WidgetSource.INCLUDE );
			topicsWidget.setSourcePath( "../../explore/widget/topicAnalytics.ftl" );
			topicsWidget.setWidgetWidth( WidgetWidth.HALF );
			topicsWidget.setColor( Color.RED );
			topicsWidget.setInformation( "Visual Analytics widget for topics or researchers' interests" );
			topicsWidget.setCloseEnabled( true );
			topicsWidget.setMinimizeEnabled( true );
			topicsWidget.setMoveableEnabled( true );
			topicsWidget.setHeaderVisible( true );
			topicsWidget.setWidgetStatus( WidgetStatus.DEFAULT );
			topicsWidget.setPosition( 0 );
			persistenceStrategy.getWidgetDAO().persist( topicsWidget );

			// create RESEARCHER Widget in Explore
			Widget publicationsWidget = new Widget();
			publicationsWidget.setTitle( "Publications" );
			publicationsWidget.setUniqueName( "explore_publications" );
			publicationsWidget.setWidgetType( WidgetType.EXPLORE );
			publicationsWidget.setWidgetGroup( "content" );
			publicationsWidget.setWidgetSource( WidgetSource.INCLUDE );
			publicationsWidget.setSourcePath( "../../explore/widget/publicationAnalytics.ftl" );
			publicationsWidget.setWidgetWidth( WidgetWidth.HALF );
			publicationsWidget.setColor( Color.GREEN );
			publicationsWidget.setInformation( "Visual Analytics widget for publications" );
			publicationsWidget.setCloseEnabled( true );
			publicationsWidget.setMinimizeEnabled( true );
			publicationsWidget.setMoveableEnabled( true );
			publicationsWidget.setHeaderVisible( true );
			publicationsWidget.setWidgetStatus( WidgetStatus.DEFAULT );
			publicationsWidget.setPosition( 0 );
			persistenceStrategy.getWidgetDAO().persist( publicationsWidget );

			// create RESEARCHER Widget in Explore
			Widget conferencesWidget = new Widget();
			conferencesWidget.setTitle( "Conferences" );
			conferencesWidget.setUniqueName( "explore_researcher" );
			conferencesWidget.setWidgetType( WidgetType.EXPLORE );
			conferencesWidget.setWidgetGroup( "content" );
			conferencesWidget.setWidgetSource( WidgetSource.INCLUDE );
			conferencesWidget.setSourcePath( "../../explore/widget/conferenceAnalytics.ftl" );
			conferencesWidget.setWidgetWidth( WidgetWidth.HALF );
			conferencesWidget.setColor( Color.BLUE );
			conferencesWidget.setInformation( "Visual Analytics widget for conferences" );
			conferencesWidget.setCloseEnabled( true );
			conferencesWidget.setMinimizeEnabled( true );
			conferencesWidget.setMoveableEnabled( true );
			conferencesWidget.setHeaderVisible( true );
			conferencesWidget.setWidgetStatus( WidgetStatus.DEFAULT );
			conferencesWidget.setPosition( 0 );
			persistenceStrategy.getWidgetDAO().persist( conferencesWidget );

		}
	}

	// Use explore/addWidgetToExistingUsers to add explore widgets to PALM
	@RequestMapping( value = "/addWidgetToExistingUsers", method = RequestMethod.GET )
	@Transactional
	public void addWidgetToExistingUsers( final HttpServletResponse response ) throws InterruptedException
	{

		List<User> existingUsers = persistenceStrategy.getUserDAO().allUsers();

		// list of explore widgets
		List<Widget> exploreWidgets = new ArrayList<>();
		// get default explore widget
		exploreWidgets.addAll( persistenceStrategy.getWidgetDAO().getWidget( WidgetType.EXPLORE, WidgetStatus.DEFAULT ) );

		// assign all default explore widgets to existing users
		for ( User user : existingUsers )
		{
			Boolean alreadyAdded = false;
			List<UserWidget> userWidgets = user.getUserWidgets();

			// don't add if already added
			for ( UserWidget userWidget : userWidgets )
			{
				if ( userWidget.getWidgetWidth().equals( WidgetWidth.HALF ) )
				{
					alreadyAdded = true;
					break;
				}
			}
			if ( !alreadyAdded )
			{
				for ( Widget eachWidget : exploreWidgets )
				{
					UserWidget userWidget = new UserWidget();
					userWidget.setWidget( eachWidget );
					userWidget.setWidgetStatus( WidgetStatus.ACTIVE );
					userWidget.setWidgetColor( eachWidget.getColor() );
					userWidget.setWidgetWidth( eachWidget.getWidgetWidth() );
					userWidget.setPosition( eachWidget.getPosition() );
					if ( eachWidget.getWidgetHeight() != null )
						userWidget.setWidgetHeight( eachWidget.getWidgetHeight() );

					user.addUserWidget( userWidget );
				}
			}
			// persist user at the end
			persistenceStrategy.getUserDAO().persist( user );

		}


	}

	@RequestMapping( method = RequestMethod.GET )
	@Transactional
	public ModelAndView explorePage(
			// @RequestParam( value = "id", required = false ) final String id,
			// @RequestParam( value = "name", required = false ) String name,
			// @RequestParam( value = "add", required = false ) final String
			// add,
			final HttpServletResponse response ) throws InterruptedException
	{
		ModelAndView model = TemplateHelper.createViewWithLink( "explore", LINK_NAME );

		List<Widget> widgets = new ArrayList<Widget>();

		User user = securityService.getUser();

		if ( user != null )
		{
			List<UserWidget> userWidgets = persistenceStrategy.getUserWidgetDAO().getWidgetByColor( user, WidgetType.EXPLORE, WidgetStatus.ACTIVE );
			for ( UserWidget userWidget : userWidgets )
			{
				Widget widget = userWidget.getWidget();
				widget.setColor( userWidget.getWidgetColor() );
				widget.setWidgetHeight( userWidget.getWidgetHeight() );
				widget.setWidgetWidth( userWidget.getWidgetWidth() );
				widget.setPosition( userWidget.getPosition() );

				widgets.add( widget );
			}
		}
		else
			widgets.addAll( persistenceStrategy.getWidgetDAO().getWidget( WidgetType.EXPLORE, WidgetStatus.DEFAULT ) );

		// assign the model
		model.addObject( "widgets", widgets );
		return model;
	}

	@SuppressWarnings( "unchecked" )
	@RequestMapping( value = "/coAuthors", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getCoAuthorList(
			// @RequestParam( value = "id", required = false ) final String
			// authorId,
			@RequestParam( value = "startPage", required = false ) Integer startPage, @RequestParam( value = "maxresult", required = false ) Integer maxresult, final HttpServletResponse response )
	{

		// String authorId = "d5bf439f-9a44-4442-addc-034b4d55953d"; // Eva
																	// Altenbernd-giani

		String authorId = "73cda3b1-b2ef-4aa1-a802-2095aebf8901"; // Ulrik
																	// Shroeder

		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		if ( authorId == null || authorId.equals( "" ) )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "authorId null" );
			return responseMap;
		}

		if ( startPage == null )
			startPage = 0;
		if ( maxresult == null )
			maxresult = 30;

		// get author
		Author author = persistenceStrategy.getAuthorDAO().getById( authorId );

		if ( author == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "author not found in database" );
			return responseMap;
		}

		// get coauthor calculation
		responseMap.putAll( researcherFeature.getResearcherCoauthor().getResearcherCoAuthorMap( author, startPage, maxresult ) );



		// commented this now graphFeature.graphData( author );

		// graphFeature.newFunc();

		// graphFeature.testFunction();

		responseMap.put( "filename", graphFeature.anotherFunction().get( "filename" ) );

		// System.out.println( responseMap.get( "coAuthors" ) );


		return responseMap;
	}

	@SuppressWarnings( "unchecked" )
	@RequestMapping( value = "/coAuthorsCluster", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getCoAuthorCluster(
			// @RequestParam( value = "id", required = false ) final String
			// authorId,
			@RequestParam( value = "startPage", required = false ) Integer startPage, @RequestParam( value = "maxresult", required = false ) Integer maxresult, final HttpServletResponse response )
	{

		String authorId = "d5bf439f-9a44-4442-addc-034b4d55953d"; // Eva
																	// Altenbernd-giani

		// String authorId = "73cda3b1-b2ef-4aa1-a802-2095aebf8901"; // Ulrik
		// Shroeder

		// get author
		Author author = persistenceStrategy.getAuthorDAO().getById( authorId );

		ObjectMapper mapper = new ObjectMapper();

		// Convert JSON string from file to Object
		Map<Object, Integer> mapLevel1 = new LinkedHashMap<Object, Integer>();
		try
		{
			Object jsonData = mapper.readValue( new File( "C:/Users/Manpriya/Downloads/clustering_test_results.txt" ), Object.class );
			String jsonInString = mapper.writeValueAsString( jsonData );
			mapLevel1 = (Map<Object, Integer>) mapper.readValue( jsonInString, Object.class );
		}
		catch ( Exception e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// 1st Level Values i.e cluster number
		Iterator<Integer> clusterIterator = mapLevel1.values().iterator();
		List<Integer> clusters = new ArrayList<Integer>();
		while ( clusterIterator.hasNext() )
		{
			clusters.add( clusterIterator.next() );
		}

		// 1st Level Keys i.e information about author
		Iterator<Object> objectsIterator = mapLevel1.keySet().iterator();

		System.out.println( mapLevel1.keySet().size() );

		List<String> names = new ArrayList<String>();
		List<String> ids = new ArrayList<String>();

		while ( objectsIterator.hasNext() )
		{

			String objectString = (String) objectsIterator.next();
			Object jsonObject;
			String jsonString;
			Map<String, String> mapValues = new LinkedHashMap<String, String>();
			try
			{
				jsonObject = mapper.readValue( objectString, Object.class );
				jsonString = mapper.writeValueAsString( jsonObject );
				mapValues = (Map<String, String>) mapper.readValue( jsonString, Object.class );

			}
			catch ( Exception e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Iterator<String> iterator = mapValues.values().iterator();

			while ( iterator.hasNext() )
			{
				names.add( iterator.next() );
				ids.add( iterator.next() );
			}

		}

		// System.out.println( names );
		// System.out.println( ids );

		// Fetch co-authors of a researcher
		Set<Author> coauthorSet = new HashSet<Author>();
		// number of collaboration
		for ( Publication publication : author.getPublications() )
		{
			for ( Author coAuthor : publication.getAuthors() )
			{
				// just skip if its himself
				if ( coAuthor.equals( author ) )
					continue;

				coauthorSet.add( coAuthor );

			}
		}

		List<Integer> indexes = new ArrayList<Integer>();

		for ( String id : ids )
		{
			for ( Author authorVar : coauthorSet )
			{
				if ( id.equals( authorVar.getId() ) )
				{
					// System.out.println( authorVar.getFirstName() );
					indexes.add( ids.indexOf( id ) );
				}
			}

		}

		// System.out.println( "SIZE: " + indexes.size() );
		// System.out.println( "co-author index 1: " + indexes.get( 0 ) );

		Map<String, Object> responseMapTest = new LinkedHashMap<String, Object>();

		List<Map<String, Object>> coAuthorList = new ArrayList<Map<String, Object>>();

		for ( Integer i : indexes )
		{
			Map<String, Object> responseMapTemp = new LinkedHashMap<String, Object>();

			responseMapTemp.put( "id", ids.get( i ) );
			responseMapTemp.put( "name", names.get( i ) );
			responseMapTemp.put( "cluster", clusters.get( i ) );
			// System.out.println( names.get( i ) );
			coAuthorList.add( responseMapTemp );
			// System.out.println( "co-LIST: " + coAuthorList );
		}

		responseMapTest.put( "coauthors", coAuthorList );
//		System.out.println( responseMapTest );

		return responseMapTest;
	}

	@SuppressWarnings( "unchecked" )
	@Transactional
	@RequestMapping( value = "/researchers", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getAuthorList( @RequestParam( value = "query", required = false ) String query, @RequestParam( value = "queryType", required = false ) String queryType, @RequestParam( value = "page", required = false ) Integer startPage, @RequestParam( value = "maxresult", required = false ) Integer maxresult, @RequestParam( value = "source", required = false ) String source, @RequestParam( value = "addedAuthor", required = false ) String addedAuthor, @RequestParam( value = "fulltextSearch", required = false ) String fulltextSearch, @RequestParam( value = "persist", required = false ) String persist, HttpServletRequest request, HttpServletResponse response ) throws IOException, InterruptedException, ExecutionException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
	{

		/* == Set Default Values== */
		if ( query == null )
			query = "";
		if ( queryType == null )
			queryType = "name";
		if ( startPage == null )
			startPage = 0;
		if ( maxresult == null )
			maxresult = 50;
		if ( source == null )
			source = "internal";
		if ( addedAuthor == null )
			addedAuthor = "no";
		if ( fulltextSearch == null )
			fulltextSearch = "no";
		if ( persist == null )
			persist = "no";

		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		boolean persistResult = false;

		responseMap.put( "query", query );
		if ( !queryType.equals( "name" ) )
			responseMap.put( "queryType", queryType );
		responseMap.put( "page", startPage );
		responseMap.put( "maxresult", maxresult );
		responseMap.put( "source", source );
		if ( !fulltextSearch.equals( "no" ) )
			responseMap.put( "fulltextSearch", fulltextSearch );
		if ( !persist.equals( "no" ) )
		{
			responseMap.put( "persist", persist );
			persistResult = true;
		}
		if ( addedAuthor.equals( "yes" ) )
			responseMap.put( "addedAuthor", addedAuthor );

		Map<String, Object> authorsMap = researcherFeature.getResearcherSearch().getResearcherMapByQuery( query, queryType, startPage, maxresult, source, addedAuthor, fulltextSearch, persistResult );

		// store in session
		if ( source.equals( "external" ) || source.equals( "all" ) )
		{
			request.getSession().setAttribute( "researchers", authorsMap.get( "authors" ) );

			// recheck if session really has been updated
			// (there is a bug in spring session, which makes session is
			// not updated sometimes) - a little workaround
			boolean isSessionUpdated = false;
			while ( !isSessionUpdated )
			{
				Object authors = request.getSession().getAttribute( "researchers" );
				if ( authors.equals( authorsMap.get( "authors" ) ) )
					isSessionUpdated = true;
				else
					request.getSession().setAttribute( "researchers", authorsMap.get( "authors" ) );
			}

			log.info( "\nRESEARCHER SESSION SEARCH" );
			@SuppressWarnings( "unchecked" )
			List<Author> sessionAuthors = (List<Author>) request.getSession().getAttribute( "researchers" );
			// get author from session -> just for debug
			if ( sessionAuthors != null && !sessionAuthors.isEmpty() )
			{
				for ( Author sessionAuthor : sessionAuthors )
				{
					for ( AuthorSource as : sessionAuthor.getAuthorSources() )
					{
						log.info( sessionAuthor.getId() + "-" + sessionAuthor.getName() + " - " + as.getSourceType() + " -> " + as.getSourceUrl() );
					}
				}
			}

		}

		if ( authorsMap != null && (Integer) authorsMap.get( "totalCount" ) > 0 )
		{
			responseMap.put( "totalCount", (Integer) authorsMap.get( "totalCount" ) );
			return researcherFeature.getResearcherSearch().printJsonOutput( responseMap, (List<Author>) authorsMap.get( "authors" ) );
		}
		else
		{
			responseMap.put( "totalCount", 0 );
			responseMap.put( "count", 0 );
			return responseMap;
		}
	}



	@RequestMapping( value = "/topic", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getPublicationTopic( @RequestParam( value = "id", required = false ) String id, @RequestParam( value = "pid", required = false ) final String pid, @RequestParam( value = "maxRetrieve", required = false ) final String maxRetrieve, final HttpServletResponse response ) throws UnsupportedEncodingException, InterruptedException, URISyntaxException, ExecutionException
	{
		if ( id == null )
		{
			id = "fd201481-1fe6-498f-9878-7e511e40e236";
		}


		return publicationFeature.getPublicationMining().getPublicationExtractedTopicsById( id, pid, maxRetrieve );
	}
	

	@RequestMapping( value = "/topicList", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getPublicationTopicTest( @RequestParam( value = "id", required = false ) String id, @RequestParam( value = "pid", required = false ) final String pid, @RequestParam( value = "maxRetrieve", required = false ) final String maxRetrieve, final HttpServletResponse response ) throws UnsupportedEncodingException, InterruptedException, URISyntaxException, ExecutionException
	{
		if ( id == null )
		{
			id = "fd201481-1fe6-498f-9878-7e511e40e236";
		}

		// System.out.println( "id: " + id );

		return publicationFeature.getPublicationMining().getPublicationExtractedTopicsById( id, pid, maxRetrieve );
	}

	@SuppressWarnings( "unchecked" )
	@Transactional
	@RequestMapping( value = "/publication", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getPublication( @RequestParam( value = "query", required = false ) String query, @RequestParam( value = "queryType", required = false ) String queryType, @RequestParam( value = "page", required = false ) Integer startPage, @RequestParam( value = "maxresult", required = false ) Integer maxresult, @RequestParam( value = "source", required = false ) String source, @RequestParam( value = "addedAuthor", required = false ) String addedAuthor, @RequestParam( value = "fulltextSearch", required = false ) String fulltextSearch, @RequestParam( value = "persist", required = false ) String persist, HttpServletRequest request, HttpServletResponse response ) throws IOException, InterruptedException, ExecutionException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
	{

		/* == Set Default Values== */
		if ( query == null )
			query = "";
		if ( queryType == null )
			queryType = "name";
		if ( startPage == null )
			startPage = 0;
		if ( maxresult == null )
			maxresult = 50;
		if ( source == null )
			source = "internal";
		if ( addedAuthor == null )
			addedAuthor = "no";
		if ( fulltextSearch == null )
			fulltextSearch = "no";
		if ( persist == null )
			persist = "no";

		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		boolean persistResult = false;

		responseMap.put( "query", query );
		if ( !queryType.equals( "name" ) )
			responseMap.put( "queryType", queryType );
		responseMap.put( "page", startPage );
		responseMap.put( "maxresult", maxresult );
		responseMap.put( "source", source );
		if ( !fulltextSearch.equals( "no" ) )
			responseMap.put( "fulltextSearch", fulltextSearch );
		if ( !persist.equals( "no" ) )
		{
			responseMap.put( "persist", persist );
			persistResult = true;
		}
		if ( addedAuthor.equals( "yes" ) )
			responseMap.put( "addedAuthor", addedAuthor );

		Map<String, Object> authorsMap = researcherFeature.getResearcherSearch().getResearcherMapByQuery( query, queryType, startPage, maxresult, source, addedAuthor, fulltextSearch, persistResult );

		// store in session
		if ( source.equals( "external" ) || source.equals( "all" ) )
		{
			request.getSession().setAttribute( "researchers", authorsMap.get( "authors" ) );

			// recheck if session really has been updated
			// (there is a bug in spring session, which makes session is
			// not updated sometimes) - a little workaround
			boolean isSessionUpdated = false;
			while ( !isSessionUpdated )
			{
				Object authors = request.getSession().getAttribute( "researchers" );
				if ( authors.equals( authorsMap.get( "authors" ) ) )
					isSessionUpdated = true;
				else
					request.getSession().setAttribute( "researchers", authorsMap.get( "authors" ) );
			}

			log.info( "\nRESEARCHER SESSION SEARCH" );
			@SuppressWarnings( "unchecked" )
			List<Author> sessionAuthors = (List<Author>) request.getSession().getAttribute( "researchers" );
			// get author from session -> just for debug
			if ( sessionAuthors != null && !sessionAuthors.isEmpty() )
			{
				for ( Author sessionAuthor : sessionAuthors )
				{
					for ( AuthorSource as : sessionAuthor.getAuthorSources() )
					{
						log.info( sessionAuthor.getId() + "-" + sessionAuthor.getName() + " - " + as.getSourceType() + " -> " + as.getSourceUrl() );
					}
				}
			}

		}

		if ( authorsMap != null && (Integer) authorsMap.get( "totalCount" ) > 0 )
		{
			responseMap.put( "totalCount", (Integer) authorsMap.get( "totalCount" ) );
			return researcherFeature.getResearcherSearch().printJsonOutput( responseMap, (List<Author>) authorsMap.get( "authors" ) );
		}
		else
		{
			responseMap.put( "totalCount", 0 );
			responseMap.put( "count", 0 );
			return responseMap;
		}
	}
}
	
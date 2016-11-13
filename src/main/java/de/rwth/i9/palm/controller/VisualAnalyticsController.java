package de.rwth.i9.palm.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
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

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.feature.academicevent.AcademicEventFeature;
import de.rwth.i9.palm.feature.circle.CircleFeature;
import de.rwth.i9.palm.feature.publication.PublicationFeature;
import de.rwth.i9.palm.feature.researcher.ResearcherFeature;
import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.Color;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.Interest;
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
import de.rwth.i9.palm.visualanalytics.service.FilterFeature;
import de.rwth.i9.palm.visualanalytics.service.VisualizationFeature;

@Controller
@RequestMapping( value = "/explore" )
public class VisualAnalyticsController
{

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
	private AcademicEventFeature academicEventFeature;

	@Autowired
	private FilterFeature filterFeature;

	@Autowired
	private VisualizationFeature visualizationFeature;

	@Autowired
	private PalmAnalytics palmAnalytics;

	// Use explore/createVAWidgets to create Visual Analytics Widgets
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
			// create SEARCH Widget in Explore
			Widget searchWidget = new Widget();
			searchWidget.setTitle( "Search" );
			searchWidget.setUniqueName( "explore_search" );
			searchWidget.setWidgetType( WidgetType.EXPLORE );
			searchWidget.setWidgetGroup( "sidebar" );
			searchWidget.setWidgetSource( WidgetSource.INCLUDE );
			searchWidget.setSourcePath( "../../explore/widget/search.ftl" );
			searchWidget.setWidgetWidth( WidgetWidth.LARGE );
			searchWidget.setColor( Color.YELLOW );
			searchWidget.setInformation( "Visual Analytics widget for searching any item" );
			searchWidget.setCloseEnabled( false );
			searchWidget.setMinimizeEnabled( false );
			searchWidget.setMoveableEnabled( true );
			searchWidget.setHeaderVisible( true );
			searchWidget.setWidgetStatus( WidgetStatus.DEFAULT );
			searchWidget.setPosition( 0 );
			persistenceStrategy.getWidgetDAO().persist( searchWidget );

			// create Visualization Widget in Explore
			Widget visualizationWidget = new Widget();
			visualizationWidget.setTitle( "Visualization" );
			visualizationWidget.setUniqueName( "explore_visualize" );
			visualizationWidget.setWidgetType( WidgetType.EXPLORE );
			visualizationWidget.setWidgetGroup( "content" );
			visualizationWidget.setWidgetSource( WidgetSource.INCLUDE );
			visualizationWidget.setSourcePath( "../../explore/widget/visualize.ftl" );
			visualizationWidget.setWidgetWidth( WidgetWidth.MEDIUM );
			visualizationWidget.setColor( Color.GREEN );
			visualizationWidget.setInformation( "Visual Analytics widget for all visualizations" );
			visualizationWidget.setCloseEnabled( false );
			visualizationWidget.setMinimizeEnabled( true );
			visualizationWidget.setMoveableEnabled( true );
			visualizationWidget.setHeaderVisible( true );
			visualizationWidget.setWidgetStatus( WidgetStatus.DEFAULT );
			visualizationWidget.setPosition( 3 );
			persistenceStrategy.getWidgetDAO().persist( visualizationWidget );

			// create Filter Widget in Explore
			Widget filterWidget = new Widget();
			filterWidget.setTitle( "Filters" );
			filterWidget.setUniqueName( "explore_filter" );
			filterWidget.setWidgetType( WidgetType.EXPLORE );
			filterWidget.setWidgetGroup( "content" );
			filterWidget.setWidgetSource( WidgetSource.INCLUDE );
			filterWidget.setSourcePath( "../../explore/widget/filter.ftl" );
			filterWidget.setWidgetWidth( WidgetWidth.SMALL );
			filterWidget.setColor( Color.BLUE );
			filterWidget.setInformation( "Visual Analytics widget for filters" );
			filterWidget.setCloseEnabled( false );
			filterWidget.setMinimizeEnabled( true );
			filterWidget.setMoveableEnabled( true );
			filterWidget.setHeaderVisible( true );
			filterWidget.setWidgetStatus( WidgetStatus.DEFAULT );
			filterWidget.setPosition( 9 );
			persistenceStrategy.getWidgetDAO().persist( filterWidget );

			// create Setup Widget in Explore
			Widget setupWidget = new Widget();
			setupWidget.setTitle( "Basic Setup" );
			setupWidget.setUniqueName( "explore_setup" );
			setupWidget.setWidgetType( WidgetType.EXPLORE );
			setupWidget.setWidgetGroup( "content" );
			setupWidget.setWidgetSource( WidgetSource.INCLUDE );
			setupWidget.setSourcePath( "../../explore/widget/setup.ftl" );
			setupWidget.setWidgetWidth( WidgetWidth.LARGE );
			setupWidget.setColor( Color.RED );
			setupWidget.setInformation( "Visual Analytics widget for basic setup" );
			setupWidget.setCloseEnabled( false );
			setupWidget.setMinimizeEnabled( false );
			setupWidget.setMoveableEnabled( false );
			setupWidget.setHeaderVisible( false );
			setupWidget.setWidgetStatus( WidgetStatus.DEFAULT );
			setupWidget.setPosition( 3 );
			persistenceStrategy.getWidgetDAO().persist( setupWidget );

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
	public ModelAndView explorePage( final HttpServletResponse response ) throws InterruptedException
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
			widgets.addAll( persistenceStrategy.getWidgetDAO().getWidgetByColor( WidgetType.EXPLORE, WidgetStatus.DEFAULT ) );

		// assign the model
		model.addObject( "widgets", widgets );
		return model;
	}

	@SuppressWarnings( "unchecked" )
	@Transactional
	@RequestMapping( value = "/searchResearchers", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getResearcherList( @RequestParam( value = "query", required = false ) String query, @RequestParam( value = "queryType", required = false ) String queryType, @RequestParam( value = "page", required = false ) Integer startPage, @RequestParam( value = "maxresult", required = false ) Integer maxresult, @RequestParam( value = "source", required = false ) String source, @RequestParam( value = "addedAuthor", required = false ) String addedAuthor, @RequestParam( value = "fulltextSearch", required = false ) String fulltextSearch, @RequestParam( value = "persist", required = false ) String persist, HttpServletRequest request, HttpServletResponse response ) throws IOException, InterruptedException, ExecutionException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
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
			addedAuthor = "yes";
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

	@SuppressWarnings( "unchecked" )
	@Transactional
	@RequestMapping( value = "/searchConferences", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getConferenceList( @RequestParam( value = "query", required = false ) String query, @RequestParam( value = "abbr", required = false ) String notation, @RequestParam( value = "page", required = false ) Integer startPage, @RequestParam( value = "maxresult", required = false ) Integer maxresult, @RequestParam( value = "type", required = false ) String type, @RequestParam( value = "source", required = false ) String source, @RequestParam( value = "addedVenue", required = false ) String addedVenue, @RequestParam( value = "persist", required = false ) String persist, @RequestParam( value = "eventId", required = false ) String eventId, HttpServletRequest request, HttpServletResponse response )
	{
		if ( query == null )
			query = "";
		if ( startPage == null )
			startPage = 0;
		if ( maxresult == null )
			maxresult = 50;
		if ( type == null )
			type = "all";
		if ( source == null )
			source = "all";
		if ( persist == null )
			persist = "no";
		if ( addedVenue == null )
			addedVenue = "yes";

		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		boolean persistResult = false;

		responseMap.put( "query", query );
		responseMap.put( "type", type );
		responseMap.put( "page", startPage );
		responseMap.put( "maxresult", maxresult );
		responseMap.put( "source", source );

		if ( !persist.equals( "no" ) )
		{
			responseMap.put( "persist", persist );
			persistResult = true;
		}

		Map<String, Object> eventGroupsMap = academicEventFeature.getEventSearch().getEventGroupMapByQuery( query, notation, startPage, maxresult, source, type, persistResult, eventId, addedVenue );

		if ( (Integer) eventGroupsMap.get( "totalCount" ) > 0 )
		{
			responseMap.put( "totalCount", (Integer) eventGroupsMap.get( "totalCount" ) );
			return academicEventFeature.getEventSearch().printJsonOutput( responseMap, (List<EventGroup>) eventGroupsMap.get( "eventGroups" ) );
		}
		else
		{
			responseMap.put( "totalCount", 0 );
			responseMap.put( "count", 0 );
			return responseMap;
		}
	}

	@SuppressWarnings( "unchecked" )
	@Transactional
	@RequestMapping( value = "/searchPublications", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getPublicationList( @RequestParam( value = "query", required = false ) String query, @RequestParam( value = "publicationType", required = false ) String publicationType, @RequestParam( value = "authorId", required = false ) String authorId, @RequestParam( value = "eventId", required = false ) String eventId, @RequestParam( value = "page", required = false ) Integer page, @RequestParam( value = "maxresult", required = false ) Integer maxresult, @RequestParam( value = "source", required = false ) String source, @RequestParam( value = "fulltextSearch", required = false ) String fulltextSearch, @RequestParam( value = "year", required = false ) String year, @RequestParam( value = "orderBy", required = false ) String orderBy, final HttpServletResponse response )
	{
		/* == Set Default Values== */
		if ( query == null )
			query = "";
		if ( publicationType == null )
			publicationType = "all";
		if ( page == null )
			page = 0;
		if ( maxresult == null )
			maxresult = 50;
		if ( fulltextSearch == null || ( fulltextSearch != null && fulltextSearch.equals( "yes" ) ) )
			fulltextSearch = "yes";
		else
			fulltextSearch = "no";
		if ( year == null || year.isEmpty() )
			year = "all";
		if ( orderBy == null )
			orderBy = "citation";
		// Currently, system only provides query on internal database
		source = "internal";

		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		responseMap.put( "query", query );
		if ( !publicationType.equals( "all" ) )
			responseMap.put( "publicationType", publicationType );
		if ( !year.equals( "all" ) )
			responseMap.put( "year", year );
		responseMap.put( "page", page );
		responseMap.put( "maxresult", maxresult );
		responseMap.put( "fulltextSearch", fulltextSearch );
		responseMap.put( "orderBy", orderBy );

		Map<String, Object> publicationMap = publicationFeature.getPublicationSearch().getPublicationListByQuery( query, publicationType, authorId, eventId, page, maxresult, source, fulltextSearch, year, orderBy );

		if ( (Integer) publicationMap.get( "totalCount" ) > 0 )
		{
			responseMap.put( "totalCount", (Integer) publicationMap.get( "totalCount" ) );
			return publicationFeature.getPublicationSearch().printJsonOutput( responseMap, (List<Publication>) publicationMap.get( "publications" ) );
		}
		else
		{
			responseMap.put( "totalCount", 0 );
			responseMap.put( "count", 0 );
			return responseMap;
		}
	}

	@SuppressWarnings( "unchecked" )
	@Transactional
	@RequestMapping( value = "/searchTopics", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getTopicList( @RequestParam( value = "query", required = false ) String query, @RequestParam( value = "publicationType", required = false ) String publicationType, @RequestParam( value = "authorId", required = false ) String authorId, @RequestParam( value = "eventId", required = false ) String eventId, @RequestParam( value = "page", required = false ) Integer page, @RequestParam( value = "maxresult", required = false ) Integer maxresult, @RequestParam( value = "source", required = false ) String source, @RequestParam( value = "fulltextSearch", required = false ) String fulltextSearch, @RequestParam( value = "year", required = false ) String year, @RequestParam( value = "orderBy", required = false ) String orderBy, final HttpServletResponse response )
	{
		/* == Set Default Values== */
		if ( query == null )
			query = "";
		if ( publicationType == null )
			publicationType = "all";
		if ( page == null )
			page = 0;
		if ( maxresult == null )
			maxresult = 50;
		if ( fulltextSearch == null || ( fulltextSearch != null && fulltextSearch.equals( "yes" ) ) )
			fulltextSearch = "yes";
		else
			fulltextSearch = "no";
		if ( year == null || year.isEmpty() )
			year = "all";
		if ( orderBy == null )
			orderBy = "citation";

		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		responseMap.put( "query", query );
		responseMap.put( "page", page );
		responseMap.put( "maxresult", maxresult );

		List<Map<String, Object>> mapList = new ArrayList<Map<String, Object>>();
		Map<String, Object> interestMap = persistenceStrategy.getInterestDAO().allTermsByPaging( query, page, maxresult );
		List<Interest> interests = (List<Interest>) interestMap.get( "interests" );
		List<String> allTopics = persistenceStrategy.getPublicationTopicDAO().allTopics();


		// intersection of topics and interests
		List<Interest> combinedInterests = new ArrayList<Interest>();
		for ( int i = 0; i < allTopics.size(); i++ )
		{
			for ( int j = 0; j < interests.size(); j++ )
			{
				float dist = palmAnalytics.getTextCompare().getDistanceByLuceneLevenshteinDistance( allTopics.get( i ), interests.get( j ).getTerm() );

				if ( dist > 0.9f && !combinedInterests.contains( interests.get( j ) ) )
				{
					Map<String, Object> interestMapTemp = new HashMap<String, Object>();

					combinedInterests.add( interests.get( j ) );
					interestMapTemp.put( "id", interests.get( j ).getId() );
					interestMapTemp.put( "name", interests.get( j ).getTerm() );

					mapList.add( interestMapTemp );
				}
			}
		}

		responseMap.put( "totalCount", interestMap.get( "totalCount" ) );
		responseMap.put( "count", mapList.size() );

		responseMap.put( "topicsList", mapList );
		return responseMap;
	}

	@SuppressWarnings( "unchecked" )
	@Transactional
	@RequestMapping( value = "/searchCircles", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getCircleList( @RequestParam( value = "query", required = false ) String query, @RequestParam( value = "queryType", required = false ) String queryType, @RequestParam( value = "page", required = false ) Integer startPage, @RequestParam( value = "maxresult", required = false ) Integer maxresult, @RequestParam( value = "source", required = false ) String source, @RequestParam( value = "addedAuthor", required = false ) String addedAuthor, @RequestParam( value = "fulltextSearch", required = false ) String fulltextSearch, @RequestParam( value = "persist", required = false ) String persist, HttpServletRequest request, HttpServletResponse response ) throws IOException, InterruptedException, ExecutionException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
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
			addedAuthor = "yes";
		if ( fulltextSearch == null )
			fulltextSearch = "no";
		if ( persist == null )
			persist = "no";

		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		responseMap.put( "query", query );
		if ( !queryType.equals( "name" ) )
			responseMap.put( "queryType", queryType );
		responseMap.put( "page", startPage );
		responseMap.put( "maxresult", maxresult );
		responseMap.put( "source", source );
		if ( !fulltextSearch.equals( "no" ) )
			responseMap.put( "fulltextSearch", fulltextSearch );
		if ( !persist.equals( "no" ) )
			responseMap.put( "persist", persist );
		if ( addedAuthor.equals( "yes" ) )
			responseMap.put( "addedAuthor", addedAuthor );

		Map<String, Object> circlesMap = circleFeature.getCircleSearch().getCircleListByQuery( query, null, startPage, maxresult, null, "name" );

		if ( circlesMap != null && (Integer) circlesMap.get( "totalCount" ) > 0 )
		{
			responseMap.put( "totalCount", (Integer) circlesMap.get( "totalCount" ) );
			return circleFeature.getCircleSearch().printJsonOutput( responseMap, (List<Circle>) circlesMap.get( "circles" ) );
		}
		else
		{
			responseMap.put( "totalCount", 0 );
			responseMap.put( "count", 0 );
			return responseMap;
		}
	}

	@Transactional
	@RequestMapping( value = "/setupStage", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> setupStage( @RequestParam( value = "id", required = false ) String id, @RequestParam( value = "type", required = false ) String type, @RequestParam( value = "replace", required = false ) String replace, HttpServletRequest request, HttpServletResponse response ) throws IOException, InterruptedException, ExecutionException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
	{
		/* == Set Default Values== */
		if ( id == null )
			id = "";
		if ( type == null )
			type = "name";
		if ( replace == null )
			replace = "";

		List<String> idsList = new ArrayList<String>( Arrays.asList( id.split( "," ) ) );
		List<String> namesList = namesFromIds( idsList, type );

		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		responseMap.put( "name", namesList );
		responseMap.put( "type", type );
		responseMap.put( "id", idsList );
		responseMap.put( "replace", replace );
		return responseMap;
	}

	@Transactional
	@RequestMapping( value = "/visualize", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> visualize( @RequestParam( value = "id", required = false ) String id, @RequestParam( value = "type", required = false ) String type, @RequestParam( value = "visType", required = false ) String visType, @RequestParam( value = "deleteFlag", required = false ) String deleteFlag, @RequestParam( value = "startYear", required = false ) String startYear, @RequestParam( value = "endYear", required = false ) String endYear, @RequestParam( value = "visTab", required = false ) String visTab, @RequestParam( value = "dataList", required = false ) String dataList, @RequestParam( value = "idList", required = false ) String idList, @RequestParam( value = "dataTransfer", required = false ) String dataTransfer, @RequestParam( value = "checkedPubValues", required = false ) String checkedPubValues, @RequestParam( value = "checkedConfValues", required = false ) String checkedConfValues, @RequestParam( value = "checkedTopValues", required = false ) String checkedTopValues, @RequestParam( value = "checkedCirValues", required = false ) String checkedCirValues, @RequestParam( value = "yearFilterPresent", required = false ) String yearFilterPresent, @RequestParam( value = "authoridForCoAuthors", required = false ) String authoridForCoAuthors, HttpServletRequest request, HttpServletResponse response ) throws IOException, InterruptedException, ExecutionException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
	{
		/* == Set Default Values== */
		if ( id == null )
			id = "";
		if ( type == null )
			type = "name";
		if ( visType == null )
			visType = "";
		if ( deleteFlag == null )
			deleteFlag = "false";
		if ( startYear == null )
			startYear = "0";
		if ( endYear == null )
			endYear = "0";
		if ( visTab == null )
			visTab = "";
		if ( dataTransfer == null )
			dataTransfer = "false";
		if ( checkedPubValues == null )
			checkedPubValues = "";
		if ( checkedConfValues == null )
			checkedConfValues = "";
		if ( checkedTopValues == null )
			checkedTopValues = "";
		if ( checkedCirValues == null )
			checkedCirValues = "";
		if ( yearFilterPresent == null )
			yearFilterPresent = "false";
		if ( authoridForCoAuthors == null )
			authoridForCoAuthors = "";

		List<String> namesList = new ArrayList<String>();
		List<String> idsList = new ArrayList<String>();
		List<String> pubFilterList = new ArrayList<String>();
		List<String> confFilterList = new ArrayList<String>();
		List<String> topFilterList = new ArrayList<String>();
		List<String> cirFilterList = new ArrayList<String>();
		List<Publication> filteredPublication = new ArrayList<Publication>(); // publications
																				// selected
																				// from
																				// the
																				// filter
		List<EventGroup> filteredConference = new ArrayList<EventGroup>(); // conferences
																			// selected
																			// from
																			// the
																			// filter
		List<Interest> filteredTopic = new ArrayList<Interest>(); // topics
																	// selected
																	// from
																	// the
																	// filter
		List<Circle> filteredCircle = new ArrayList<Circle>(); // circles
		// selected
		// from
		// the
		// filter

		Map<String, Object> visMap = new LinkedHashMap<String, Object>();
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		if ( idList != null && !idList.equals( "" ) )
		{
			idsList = new ArrayList<String>( Arrays.asList( idList.split( "," ) ) );
			namesList = namesFromIds( idsList, type );

			// if ( idList.length() == 0 )
			// {
			// idsList = new ArrayList<String>();
			// }
		}

		if ( dataTransfer.equals( "true" ) )
		{
			responseMap.put( "type", type );
			responseMap.put( "visType", visType );
			responseMap.put( "dataList", namesList );
			responseMap.put( "idsList", idsList );
			responseMap.put( "yearFilterPresent", yearFilterPresent );
			responseMap.put( "deleteFlag", deleteFlag );
			responseMap.put( "authoridForCoAuthors", authoridForCoAuthors );

			if ( checkedPubValues != "" )
			{
				responseMap.put( "checkedPubValues", checkedPubValues );
			}
			if ( checkedConfValues != "" )
			{
				responseMap.put( "checkedConfValues", checkedConfValues );
			}
			if ( checkedTopValues != "" )
			{
				responseMap.put( "checkedTopValues", checkedTopValues );
			}
			if ( checkedCirValues != "" )
			{
				responseMap.put( "checkedCirValues", checkedCirValues );
			}

			if ( startYear != "" && endYear != "" )
			{
				responseMap.put( "startYear", startYear );
				responseMap.put( "endYear", endYear );
			}

		}
		else
		{
			if ( visTab == "" || visTab.equals( "" ) )
			{
				if ( visType.equals( "researchers" ) )
				{
					visTab = "Network";
				}
				if ( visType.equals( "conferences" ) )
				{
					visTab = "Locations";
				}
				if ( visType.equals( "publications" ) )
				{
					visTab = "Timeline";
				}
				if ( visType.equals( "topics" ) )
				{
					visTab = "Bubbles";
				}
			}

			if ( checkedPubValues.equals( "" ) )
			{
			}
			else
			{
				pubFilterList = new ArrayList<String>( Arrays.asList( checkedPubValues.split( "," ) ) );
				for ( int i = 0; i < pubFilterList.size(); i++ )
				{
					filteredPublication.add( persistenceStrategy.getPublicationDAO().getById( pubFilterList.get( i ) ) );
				}
			}

			if ( checkedConfValues.equals( "" ) )
			{
			}
			else
			{
				confFilterList = new ArrayList<String>( Arrays.asList( checkedConfValues.split( "," ) ) );
				for ( int i = 0; i < confFilterList.size(); i++ )
				{
					filteredConference.add( persistenceStrategy.getEventGroupDAO().getById( confFilterList.get( i ) ) );
				}
			}

			if ( checkedTopValues.equals( "" ) )
			{
			}
			else
			{
				topFilterList = new ArrayList<String>( Arrays.asList( checkedTopValues.split( "," ) ) );
				for ( int i = 0; i < topFilterList.size(); i++ )
				{
					filteredTopic.add( persistenceStrategy.getInterestDAO().getById( topFilterList.get( i ) ) );
				}
			}
			if ( checkedCirValues.equals( "" ) )
			{
			}
			else
			{
				cirFilterList = new ArrayList<String>( Arrays.asList( checkedCirValues.split( "," ) ) );
				for ( int i = 0; i < cirFilterList.size(); i++ )
				{
					filteredCircle.add( persistenceStrategy.getCircleDAO().getById( cirFilterList.get( i ) ) );
				}
			}

			if ( !startYear.equals( "0" ) )
			{
				if ( !yearFilterPresent.equals( "true" ) && idList.length() != 0 )
				{
					startYear = filterFeature.getDataForFilter().timeFilter( idsList, type ).get( "startYear" ).toString();
					endYear = filterFeature.getDataForFilter().timeFilter( idsList, type ).get( "endYear" ).toString();
				}
			}
			else
			{
				startYear = filterFeature.getDataForFilter().timeFilter( idsList, type ).get( "startYear" ).toString();
				endYear = filterFeature.getDataForFilter().timeFilter( idsList, type ).get( "endYear" ).toString();
			}

			List<Author> authorList = new ArrayList<Author>();
			List<EventGroup> eventGroupList = new ArrayList<EventGroup>();
			List<Publication> publicationList = new ArrayList<Publication>();
			List<Interest> interestList = new ArrayList<Interest>();
			List<Circle> circleList = new ArrayList<Circle>();
			if ( type.equals( "researcher" ) )
				authorList = filterFeature.getFilterHelper().getAuthorsFromIds( idsList );
			if ( type.equals( "conference" ) )
				eventGroupList = filterFeature.getFilterHelper().getConferencesFromIds( idsList );
			if ( type.equals( "publication" ) )
				publicationList = filterFeature.getFilterHelper().getPublicationsFromIds( idsList );
			if ( type.equals( "topic" ) )
				interestList = filterFeature.getFilterHelper().getInterestsFromIds( idsList );
			if ( type.equals( "circle" ) )
				circleList = filterFeature.getFilterHelper().getCirclesFromIds( idsList );

			Set<Publication> publications = new HashSet<Publication>();

			if ( visType.equals( "researchers" ) && !type.equals( "publication" ) && filteredPublication.isEmpty() && filteredConference.isEmpty() && filteredTopic.isEmpty() && filteredCircle.isEmpty() && yearFilterPresent.equals( "false" ) )
			{
				System.out.println( "publications: !! " + publications.size() );
			}
			else
				publications = filterFeature.getFilteredData().getFilteredPublications( type, authorList, eventGroupList, publicationList, interestList, circleList, filteredPublication, filteredConference, filteredTopic, filteredCircle, startYear, endYear );

			visMap = visSwitch( type, idsList, visTab, visType, publications, startYear, endYear, yearFilterPresent, filteredTopic, authoridForCoAuthors );

			responseMap.put( "type", type );
			responseMap.put( "visType", visType );
			responseMap.put( "dataList", namesList );
			responseMap.put( "idsList", idsList );
			responseMap.put( "map", visMap );
		}

		return responseMap;
	}

	@Transactional
	@RequestMapping( value = "/filter", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> filter( @RequestParam( value = "id", required = false ) String id, @RequestParam( value = "type", required = false ) String type, @RequestParam( value = "visType", required = false ) String visType, @RequestParam( value = "deleteFlag", required = false ) String deleteFlag, @RequestParam( value = "startYear", required = false ) String startYear, @RequestParam( value = "endYear", required = false ) String endYear, @RequestParam( value = "visTab", required = false ) String visTab, @RequestParam( value = "dataList", required = false ) String dataList, @RequestParam( value = "idList", required = false ) String idList, @RequestParam( value = "dataTransfer", required = false ) String dataTransfer, @RequestParam( value = "filterList", required = false ) String filterList, HttpServletRequest request, HttpServletResponse response ) throws IOException, InterruptedException, ExecutionException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
	{
		/* == Set Default Values== */
		if ( id == null )
			id = "";
		if ( type == null )
			type = "name";
		if ( visType == null )
			visType = "";
		if ( deleteFlag == null )
			deleteFlag = "false";
		if ( startYear == null )
			startYear = "";
		if ( endYear == null )
			endYear = "";
		if ( visTab == null )
			visTab = "";
		if ( dataTransfer == null )
			dataTransfer = "false";
		if ( idList == null )
			idList = "";
		if ( filterList == null )
			filterList = "";

		List<String> namesList = new ArrayList<String>();
		List<String> idsList = new ArrayList<String>();
		List<String> filters = new ArrayList<String>();

		Map<String, Object> visMap = new LinkedHashMap<String, Object>();
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		if ( dataList != null && idsList != null )
		{
			namesList = new ArrayList<String>( Arrays.asList( dataList.split( "," ) ) );
			idsList = new ArrayList<String>( Arrays.asList( idList.split( "," ) ) );
		}

		if ( idList != null )
		{
			if ( idList.length() == 0 )
			{
				idsList = new ArrayList<String>();
			}
		}

		if ( dataTransfer.equals( "true" ) )
		{

			responseMap.put( "type", type );
			responseMap.put( "visType", visType );
			responseMap.put( "dataList", dataList );
			responseMap.put( "idsList", idsList );

		}
		else
		{
			if ( !idList.equals( "" ) )
			{
				if ( !filterList.equals( "" ) )
				{
					filters = new ArrayList<String>( Arrays.asList( filterList.split( "," ) ) );
				}

				responseMap.put( "type", type );
				responseMap.put( "visType", visType );
				responseMap.put( "dataList", dataList );
				responseMap.put( "idsList", idsList );

				for ( int i = 0; i < filters.size(); i++ )
				{

					if ( filters.get( i ).equals( "Time" ) )
					{
						responseMap.put( "TimeFilter", filterFeature.getDataForFilter().timeFilter( idsList, type ) );
					}
					if ( filters.get( i ).equals( "Publications" ) )
					{
						responseMap.put( "publicationFilter", filterFeature.getDataForFilter().publicationFilter( idsList, type ) );
					}
					if ( filters.get( i ).equals( "Conferences" ) )
					{
						responseMap.put( "conferenceFilter", filterFeature.getDataForFilter().conferenceFilter( idsList, type ) );
					}
					if ( filters.get( i ).equals( "Circles" ) )
					{
						responseMap.put( "circleFilter", filterFeature.getDataForFilter().circleFilter( idsList, type ) );
					}
					if ( filters.get( i ).equals( "Researchers" ) )
					{
					}
					if ( filters.get( i ).equals( "Topics" ) )
					{
						responseMap.put( "topicFilter", filterFeature.getDataForFilter().topicFilter( idsList, type ) );
					}
				}
			}

		}
		return responseMap;
	}

	public Map<String, Object> visSwitch( String type, List<String> idsList, String visTab, String visType, Set<Publication> publications, String startYear, String endYear, String yearFilterPresent, List<Interest> filteredTopic, String authoridForCoAuthors )
	{

		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		switch ( visTab ) {
		case "Network": {
			visMap = visualizationFeature.getVisNetwork().visualizeNetwork( type, publications, idsList, startYear, endYear, authoridForCoAuthors );
			break;
		}
		case "Locations": {
			visMap = visualizationFeature.getVisLocations().visualizeLocations( type, publications, idsList, startYear, endYear, filteredTopic );
			break;
		}
		case "Timeline": {
			visMap = visualizationFeature.getVisTimeline().visualizeTimeline( publications );
			break;
		}
		case "Evolution": {
			visMap = visualizationFeature.getVisEvolution().visualizeEvolution( type, idsList, publications, startYear, endYear, yearFilterPresent );
			break;
		}
		case "Bubbles": {
			visMap = visualizationFeature.getVisBubbles().visualizeBubbles( type, idsList, publications, startYear, endYear, yearFilterPresent );
			break;
		}
		case "Group": {
			if ( visType.equals( "researchers" ) )
			{
				visMap = visualizationFeature.getVisGroup().visualizeResearchersGroup( type, idsList, publications, startYear, endYear );
			}
			if ( visType.equals( "conferences" ) )
			{
				visMap = visualizationFeature.getVisGroup().visualizeConferencesGroup( type, publications );
			}
			if ( visType.equals( "publications" ) )
			{
				visMap = visualizationFeature.getVisGroup().visualizePublicationsGroup( type, publications );
			}
			if ( visType.equals( "topics" ) )
			{
				visMap = visualizationFeature.getVisGroup().visualizeTopicsGroup( type, publications );
			}
			break;
		}

		case "List": {

			if ( visType.equals( "researchers" ) )
			{
				visMap = visualizationFeature.getVisList().visualizeResearchersList( type, publications, startYear, endYear, idsList, yearFilterPresent );
			}
			if ( visType.equals( "conferences" ) )
			{
				visMap = visualizationFeature.getVisList().visualizeConferencesList( type, publications, startYear, endYear, idsList, yearFilterPresent );
			}
			if ( visType.equals( "publications" ) )
			{
				visMap = visualizationFeature.getVisList().visualizePublicationsList( type, publications, startYear, endYear, idsList, yearFilterPresent );
			}
			if ( visType.equals( "topics" ) )
			{
				visMap = visualizationFeature.getVisList().visualizeTopicsList( type, publications, startYear, endYear, idsList, yearFilterPresent );
			}
			break;
		}
		case "Comparison": {
			if ( visType.equals( "researchers" ) )
			{
				visMap = visualizationFeature.getVisComparison().visualizeResearchersComparison( type, idsList, publications, startYear, endYear, yearFilterPresent );
			}
			if ( visType.equals( "conferences" ) )
			{
				visMap = visualizationFeature.getVisComparison().visualizeConferencesComparison( type, idsList, publications, startYear, endYear, yearFilterPresent );
			}
			if ( visType.equals( "publications" ) )
			{
				visMap = visualizationFeature.getVisComparison().visualizePublicationsComparison( type, idsList, publications, startYear, endYear, yearFilterPresent );
			}
			if ( visType.equals( "topics" ) )
			{
				visMap = visualizationFeature.getVisComparison().visualizeTopicsComparison( type, idsList, publications, startYear, endYear, yearFilterPresent );
			}
			break;
		}
		case "Similar": {
			if ( visType.equals( "researchers" ) )
			{
				visMap = visualizationFeature.getVisSimilar().visualizeSimilarResearchers( type, idsList );
			}
			if ( visType.equals( "conferences" ) )
			{
				visMap = visualizationFeature.getVisSimilar().visualizeSimilarConferences( type, idsList );
			}
			if ( visType.equals( "publications" ) )
			{
				visMap = visualizationFeature.getVisSimilar().visualizeSimilarPublications( type, idsList );
			}
			if ( visType.equals( "topics" ) )
			{
				visMap = visualizationFeature.getVisSimilar().visualizeSimilarTopics( type, idsList );
			}

			break;
		}
		}
		return visMap;
	}

	public List<String> namesFromIds( List<String> idsList, String type )
	{
		List<String> namesList = new ArrayList<String>();
		for ( String i : idsList )
		{
			if ( type.equals( "researcher" ) )
			{
				Author author = persistenceStrategy.getAuthorDAO().getById( i );
				namesList.add( author.getName() );
			}
			if ( type.equals( "conference" ) )
			{
				EventGroup conference = persistenceStrategy.getEventGroupDAO().getById( i );
				namesList.add( conference.getName() );
			}
			if ( type.equals( "publication" ) )
			{
				Publication publication = persistenceStrategy.getPublicationDAO().getById( i );
				namesList.add( publication.getTitle() );
			}
			if ( type.equals( "topic" ) )
			{
				Interest interest = persistenceStrategy.getInterestDAO().getById( i );

				// RECHECK TOPIC LIST IN SEARCH BAR!!!!
				System.out.println( interest.getId() );
				namesList.add( interest.getTerm() );
			}
			if ( type.equals( "circle" ) )
			{
				Circle circle = persistenceStrategy.getCircleDAO().getById( i );
				namesList.add( circle.getName() );
			}
		}
		return namesList;
	}

}

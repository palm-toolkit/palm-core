package de.rwth.i9.palm.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

import de.rwth.i9.palm.feature.publication.PublicationFeature;
import de.rwth.i9.palm.feature.researcher.ResearcherFeature;
import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorSource;
import de.rwth.i9.palm.model.User;
import de.rwth.i9.palm.model.UserWidget;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetStatus;
import de.rwth.i9.palm.model.WidgetType;
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

	// @Autowired
	// private PublicationCollectionService publicationCollectionService;

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
			List<UserWidget> userWidgets = persistenceStrategy.getUserWidgetDAO().getWidget( user, WidgetType.EXPLORE, WidgetStatus.DEFAULT );
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

}
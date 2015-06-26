package de.rwth.i9.palm.controller;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.helper.DateTimeHelper;
import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Institution;
import de.rwth.i9.palm.model.RequestType;
import de.rwth.i9.palm.model.SessionDataSet;
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

	@RequestMapping( method = RequestMethod.GET )
	@Transactional
	public ModelAndView mainPage( @RequestParam( value = "sessionid", required = false ) final String sessionId, final HttpServletResponse response ) throws InterruptedException
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
	public @ResponseBody Map<String, Object> getAuthorList( @RequestParam( value = "query", required = false ) String query, @RequestParam( value = "page", required = false ) Integer page, @RequestParam( value = "maxresult", required = false ) Integer maxresult, final HttpServletResponse response )
	{
		if ( query == null )
			query = "";

		if ( page == null )
			page = 0;

		if ( maxresult == null )
			maxresult = 50;

		// check whether the author query ever executed before
		UserRequest userRequest = persistenceStrategy.getUserRequestDAO().getByTypeAndQuery( RequestType.SEARCHAUTHOR, query );
		boolean collectFromNetwork = false;

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
			// check if the existing userRequest obsolete (longer than a week)
			if ( DateTimeHelper.substractTimeStampToHours( currentTimestamp, userRequest.getRequestDate() ) > 24 * 7 )
				collectFromNetwork = true;
		}

		// collect author from network
		if ( collectFromNetwork )
		{

		}

		// get the researcher
		Map<String, Object> researcherMap = persistenceStrategy.getAuthorDAO().getAuthorByFullTextSearchWithPaging( query, page, maxresult );

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
				if ( researcher.getOtherDetail() != null || researcher.getDepartment() != null )
					pub.put( "detail", researcher.getOtherDetail() + " " + researcher.getDepartment() );
				if ( researcher.getInstitution() != null && !researcher.getInstitution().isEmpty() )
				{
					String institut = "";
					int counter = 0;
					for ( Institution institution : researcher.getInstitution() )
					{
						if ( counter > 0 )
							institut += ", ";
						institut += institution.getName();
					}
					pub.put( "aff", institut );
				}

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
}
package de.rwth.i9.palm.controller;

import java.util.ArrayList;
import java.util.Collections;
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
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.helper.comparator.ConferenceByNotationComparator;
import de.rwth.i9.palm.model.Conference;
import de.rwth.i9.palm.model.SessionDataSet;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetStatus;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.ApplicationContextService;

@Controller
@SessionAttributes( { "sessionDataSet" } )
@RequestMapping( value = "/conference" )
public class ConferenceController
{
	private static final String LINK_NAME = "conference";

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
		ModelAndView model = TemplateHelper.createViewWithSessionDataSet( "conference", LINK_NAME, sessionDataSet );

		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getWidget( WidgetType.CONFERENCE, WidgetStatus.DEFAULT );
		// assign the model
		model.addObject( "widgets", widgets );
		return model;
	}

	@Transactional
	@RequestMapping( value = "/search", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getConferenceList( @RequestParam( value = "query", required = false ) String query, @RequestParam( value = "page", required = false ) Integer page, @RequestParam( value = "maxresult", required = false ) Integer maxresult, final HttpServletResponse response )
	{
		if ( query == null )
			query = "";

		if ( page == null )
			page = 0;

		if ( maxresult == null )
			maxresult = 50;

		// get the conference
		Map<String, Object> conferenceMap = persistenceStrategy.getConferenceDAO().getConferenceByFullTextSearchWithPaging( query, page, maxresult );

		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		responseMap.put( "query", query );
		responseMap.put( "page", page );
		responseMap.put( "maxresult", maxresult );

		// create the json structure for conference list
		if ( conferenceMap != null )
		{
			responseMap.put( "count", conferenceMap.get( "count" ) );

			@SuppressWarnings( "unchecked" )
			List<Conference> conferences = (List<Conference>) conferenceMap.get( "result" );
			List<Map<String, String>> conferenceList = new ArrayList<Map<String, String>>();

			// sort conference
			Collections.sort( conferences, new ConferenceByNotationComparator() );

			for ( Conference conference : conferences )
			{
				Map<String, String> conf = new LinkedHashMap<String, String>();
				conf.put( "id", conference.getId() );
				conf.put( "type", conference.getConferenceGroup().getConferenceType().toString() );
				conf.put( "title", conference.getConferenceGroup().getName() );
				conf.put( "year", conference.getYear() );
				conf.put( "notation", conference.getConferenceGroup().getNotation() + conference.getYear() );

				conferenceList.add( conf );
			}
			responseMap.put( "conference", conferenceList );

		}
		else
		{
			responseMap.put( "count", 0 );
		}

		return responseMap;
	}

}
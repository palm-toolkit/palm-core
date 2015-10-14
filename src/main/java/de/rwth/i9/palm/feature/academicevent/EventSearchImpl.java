package de.rwth.i9.palm.feature.academicevent;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.http.ParseException;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.datasetcollect.service.EventPublicationCollectionService;
import de.rwth.i9.palm.helper.DateTimeHelper;
import de.rwth.i9.palm.helper.comparator.EventByNotationComparator;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.util.IdentifierFactory;

@Component
public class EventSearchImpl implements EventSearch
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private EventPublicationCollectionService eventPublicationCollectionService;

	@Override
	public Map<String, Object> getEventListByQuery( String query, Integer page, Integer maxresult )
	{
		if ( query == null )
			query = "";

		if ( page == null )
			page = 0;

		if ( maxresult == null )
			maxresult = 50;

		// get the venue
		Map<String, Object> venueMap = persistenceStrategy.getEventDAO().getEventByFullTextSearchWithPaging( query, page, maxresult );

		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		responseMap.put( "query", query );
		responseMap.put( "page", page );
		responseMap.put( "maxresult", maxresult );

		// create the json structure for conference list
		if ( venueMap != null )
		{
			responseMap.put( "count", venueMap.get( "count" ) );

			@SuppressWarnings( "unchecked" )
			List<Event> events = (List<Event>) venueMap.get( "result" );
			List<Map<String, String>> venueList = new ArrayList<Map<String, String>>();

			// sort conference
			Collections.sort( events, new EventByNotationComparator() );

			for ( Event event : events )
			{
				Map<String, String> conf = new LinkedHashMap<String, String>();
				conf.put( "id", event.getId() );
				conf.put( "type", event.getEventGroup().getPublicationType().toString() );
				conf.put( "title", event.getEventGroup().getName() );
				conf.put( "year", event.getYear() );
				conf.put( "notation", event.getEventGroup().getNotation() + event.getYear() );

				venueList.add( conf );
			}
			responseMap.put( "conference", venueList );

		}
		else
		{
			responseMap.put( "count", 0 );
		}

		return responseMap;
	}

	@Override
	public Map<String, Object> fetchEventData( String id, String pid, String force ) throws ParseException, IOException, InterruptedException, ExecutionException, java.text.ParseException, TimeoutException, OAuthSystemException, OAuthProblemException
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// get author
		Event event = persistenceStrategy.getEventDAO().getById( id );
		if ( event == null )
		{
			responseMap.put( "status", "error - event not found" );
			return responseMap;
		}

		// pid must exist
		if ( pid == null )
			pid = IdentifierFactory.getNextDefaultIdentifier();

		// check whether it is necessary to collect information from network
		if ( this.isFetchDatasetFromDBLP( event ) || force.equals( "true" ) )
			eventPublicationCollectionService.collectPublicationListFromVenue( responseMap, event, pid );

		return responseMap;
	}

	/**
	 * Check whether fetching to network is necessary
	 * 
	 * @param author
	 * @return
	 */
	private boolean isFetchDatasetFromDBLP( Event event )
	{
		// get current timestamp
		java.util.Date date = new java.util.Date();
		Timestamp currentTimestamp = new Timestamp( date.getTime() );
		if ( event.getCrawlDate() != null )
		{
			// check if the existing author publication is obsolete
			if ( DateTimeHelper.substractTimeStampToHours( currentTimestamp, event.getCrawlDate() ) > 24 * 7 )
			{
				// update current timestamp
				event.setCrawlDate( currentTimestamp );
				persistenceStrategy.getEventDAO().persist( event );
				return true;
			}
		}
		else
		{
			// update current timestamp
			event.setCrawlDate( currentTimestamp );
			persistenceStrategy.getEventDAO().persist( event );
			return true;
		}

		// return false;
		return false;
	}

}

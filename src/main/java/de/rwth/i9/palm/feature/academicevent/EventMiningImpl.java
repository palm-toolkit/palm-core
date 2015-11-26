package de.rwth.i9.palm.feature.academicevent;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.http.ParseException;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.datasetcollect.service.DblpEventCollection;
import de.rwth.i9.palm.datasetcollect.service.EventPublicationCollectionService;
import de.rwth.i9.palm.helper.DateTimeHelper;
import de.rwth.i9.palm.helper.comparator.EventByYearComparator;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.util.IdentifierFactory;

@Component
public class EventMiningImpl implements EventMining
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private EventPublicationCollectionService eventPublicationCollectionService;

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

	@Override
	public Map<String, Object> fetchEventGroupData( String id, String pid, List<EventGroup> sessionEventGroups )
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		// check eventGroup on session
		EventGroup eventGroup = null;

		// get eventGroup from session
		if ( sessionEventGroups != null && !sessionEventGroups.isEmpty() )
		{
			if ( id != null )
			{
				for ( EventGroup sessionEventGroup : sessionEventGroups )
				{
					if ( sessionEventGroup.getId().equals( id ) )
					{
						eventGroup = sessionEventGroup;
						break;
					}
				}
			}
		}

		// get eventGroup from database
		if ( eventGroup == null )
			eventGroup = persistenceStrategy.getEventGroupDAO().getById( id );

		if ( eventGroup == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "error-msg", "eventGroup not found in system" );
			return responseMap;
		}

		// pid must exist
		if ( pid == null )
			pid = IdentifierFactory.getNextDefaultIdentifier();

		responseMap.put( "status", "ok" );

		// TODO check for expiration date
		// add date to event group
		java.util.Date date = new java.util.Date();
		Timestamp currentTimestamp = new Timestamp( date.getTime() );
		eventGroup.setRequestDate( currentTimestamp );

		// persist
		persistenceStrategy.getEventGroupDAO().persist( eventGroup );

		String year = "";
		List<Event> events = null;

		// get list of events
		if ( eventGroup.getEvents() == null || eventGroup.getEvents().isEmpty() )
		{
			events = new ArrayList<Event>();

			Map<String, Object> venueDetailMap = DblpEventCollection.getEventListFromDBLP( eventGroup.getDblpUrl(), null );

			for ( Map.Entry<String, Object> entry : venueDetailMap.entrySet() )
			{
				// only event information needed
				if ( entry.getKey().equals( "events" ) )
				{
					for ( Map<String, Object> eachEventYearMap : (List<Map<String, Object>>) entry.getValue() )
					{

						for ( Entry<String, Object> eachEventYearEntry : eachEventYearMap.entrySet() )
						{
							if ( eachEventYearEntry.getKey().equals( "volume" ) )
							{
								int position = 0;
								for ( Entry<String, String> eachEventVolumeEntry : ( (Map<String, String>) eachEventYearEntry.getValue() ).entrySet() )
								{
									Event newEvent = new Event();
									newEvent.setYear( year );
									newEvent.setPosition( position );
									newEvent.setName( eachEventVolumeEntry.getKey() );
									newEvent.setDblpUrl( eachEventVolumeEntry.getValue() );
									newEvent.setEventGroup( eventGroup );
									eventGroup.addEvent( newEvent );
									persistenceStrategy.getEventDAO().persist( newEvent );

									position++;
								}
							}
							else if ( eachEventYearEntry.getKey().equals( "year" ) )
							{
								if ( eachEventYearMap.get( "year" ) != null && !eachEventYearMap.get( "year" ).equals( "" ) )
								year = (String) eachEventYearMap.get( "year" );
							}

						}
					}
				}
			}
			// persist
			persistenceStrategy.getEventGroupDAO().persist( eventGroup );
		}
		else
		{
			events = eventGroup.getEvents();
		}
		// sort collections based on year
		Collections.sort( events, new EventByYearComparator() );

		// print event in json

		List<Map<String, String>> eventList = new ArrayList<Map<String, String>>();

		for ( Event event : events )
		{
			Map<String, String> eventMap = new LinkedHashMap<String, String>();
			eventMap.put( "id", event.getId() );
			eventMap.put( "name", WordUtils.capitalize( event.getName() ) );
			eventMap.put( "year", event.getYear() );
			eventMap.put( "url", event.getDblpUrl() );

			eventList.add( eventMap );
		}

		responseMap.put( "events", eventList );

		return responseMap;
	}
}

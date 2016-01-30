package de.rwth.i9.palm.feature.academicevent;

import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.PublicationType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class EventBasicStatisticImpl implements EventBasicStatistic
{

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Override
	public Map<String, Object> getEventBasicStatisticById( String eventId )
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// get event
		Event event = persistenceStrategy.getEventDAO().getById( eventId );
		if ( event == null )
		{
			responseMap.put( "status", "Error - event not found" );
			return responseMap;
		}
		EventGroup eventGroup = event.getEventGroup();

		if ( eventGroup == null )
		{
			responseMap.put( "status", "Error - event group not found" );
			return responseMap;
		}

		responseMap.put( "status", "ok" );

		// put eventgroup detail
		Map<String, Object> eventMap = new LinkedHashMap<String, Object>();

		if ( eventGroup.getPublicationType().equals( PublicationType.CONFERENCE ) )
		{

			if ( event.getDate() != null )
			{
				SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd" );
				eventMap.put( "Conference Date", sdf.format( event.getDate() ) );

				// Check for Editorship

			}
		}
		else if ( eventGroup.getPublicationType().equals( PublicationType.JOURNAL ) )
		{
			if ( event.getDate() != null )
			{
				SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM" );
				eventMap.put( "Date Published", sdf.format( event.getDate() ) );
			}
		}
		responseMap.put( "publication", eventMap );

		return responseMap;
	}

}

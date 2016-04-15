package de.rwth.i9.palm.feature.academicevent;

import java.util.Map;

public interface EventPublication
{
	public Map<String, Object> getPublicationListByEventId( String eventId, String query, String publicationId );
}

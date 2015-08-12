package de.rwth.i9.palm.feature.publication;

import java.util.Map;

public interface PublicationSearch
{
	public Map<String, Object> getPublicationListByQueryAndEvent( String query, String eventName, String eventId, Integer page, Integer maxresult );
}

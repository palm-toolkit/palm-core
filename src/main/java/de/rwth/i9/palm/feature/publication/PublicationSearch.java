package de.rwth.i9.palm.feature.publication;

import java.util.Map;

public interface PublicationSearch
{
	public Map<String, Object> getPublicationListByQueryAndConference( String query, String conferenceName, String conferenceId, Integer page, Integer maxresult );
}

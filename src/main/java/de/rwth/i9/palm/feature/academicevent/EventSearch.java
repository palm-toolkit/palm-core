package de.rwth.i9.palm.feature.academicevent;

import java.util.List;
import java.util.Map;

import de.rwth.i9.palm.model.EventGroup;

public interface EventSearch
{
	public List<EventGroup> getEventGroupListByQuery( String query, Integer startPage, Integer maxresult, String source, boolean persistResult );

	public Map<String, Object> printJsonOutput( Map<String, Object> responseMap, List<EventGroup> eventGroups );
}

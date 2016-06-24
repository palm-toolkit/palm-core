package de.rwth.i9.palm.feature.academicevent;

import java.util.Map;

import de.rwth.i9.palm.model.Event;

public interface EventTopicModeling
{
	public Map<String, Object> getTopicModeling( String eventId, boolean isReplaceExistingResult );

	public Map<String, Object> getStaticTopicModelingNgrams( String eventId, boolean isReplaceExistingResult );

	public Map<String, Object> getTopicModelUniCloud( Event event, boolean isReplaceExistingResult );

	public Map<String, Object> getTopicModelNCloud( Event event, boolean isReplaceExistingResult );

	public Map<String, Object> getSimilarEventsMap( Event event, int startPage, int maxresult );
}

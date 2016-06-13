package de.rwth.i9.palm.feature.academicevent;

import java.util.Map;

public interface EventTopicModeling
{
	public Map<String, Object> getTopicModeling( String eventId, boolean isReplaceExistingResult );

	public Map<String, Object> getStaticTopicModelingNgrams( String eventId, boolean isReplaceExistingResult );
}

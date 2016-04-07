package de.rwth.i9.palm.feature.circle;

import java.util.Map;

public interface CircleTopicModeling
{
	public Map<String, Object> getTopicModeling( String circleId, boolean isReplaceExistingResult );

	public Map<String, Object> getStaticTopicModelingNgrams( String circleId, boolean isReplaceExistingResult );
}

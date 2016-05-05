package de.rwth.i9.palm.feature.circle;

import java.util.Map;

public interface CircleTopicModeling
{
	public Map<String, Object> getLdaBasicExample( String circleId, boolean isReplaceExistingResult );
}

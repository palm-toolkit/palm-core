package de.rwth.i9.palm.feature.researcher;

import java.util.Map;

public interface ResearcherTopicModeling
{
	public Map<String, Object> getLdaBasicExample( String authorId, boolean isReplaceExistingResult );

}

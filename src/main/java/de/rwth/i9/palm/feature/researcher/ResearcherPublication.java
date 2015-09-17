package de.rwth.i9.palm.feature.researcher;

import java.util.Map;

public interface ResearcherPublication
{
	public Map<String, Object> getPublicationListByAuthorId( String authorId );
}

package de.rwth.i9.palm.feature.researcher;

import java.util.Map;

import de.rwth.i9.palm.model.Author;

public interface ResearcherRecommededauthor
{
	public Map<String, Object> getResearcherRecommendedAuthorMap( Author author, int startPage, int maxresult );
}

package de.rwth.i9.palm.feature.researcher;

import java.util.Map;

import de.rwth.i9.palm.model.Author;

public interface ResearcherSimilarauthor
{
	public Map<String, Object> getResearcherSimilarAuthorMap( Author author, int startPage, int maxresult );
}

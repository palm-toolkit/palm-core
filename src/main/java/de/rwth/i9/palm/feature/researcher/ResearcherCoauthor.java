package de.rwth.i9.palm.feature.researcher;

import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;

public interface ResearcherCoauthor
{
	public Map<String, Object> getResearcherCoAuthorMap( Author author, int startPage, int maxresult );

	public Map<String, Object> getResearcherCoAuthorMapByPublication( List<Author> authorList, Set<Publication> publications, String type, List<String> idsList, String startYear, String endYear );

}

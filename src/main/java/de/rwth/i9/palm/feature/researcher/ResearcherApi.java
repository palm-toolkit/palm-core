package de.rwth.i9.palm.feature.researcher;

import java.util.Map;

public interface ResearcherApi
{
	public Map<String, Object> getAuthorAutoComplete( String namePrefix );
}

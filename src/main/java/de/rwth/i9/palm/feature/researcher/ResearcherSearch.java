package de.rwth.i9.palm.feature.researcher;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface ResearcherSearch
{
	public Map<String, Object> getResearcherListByQuery( String query, Integer page, Integer maxresult ) throws IOException, InterruptedException, ExecutionException;

	public Map<String, Object> fetchResearcherData( String id, String name, String uri, String affiliation, String force ) throws IOException, InterruptedException, ExecutionException, ParseException;
}

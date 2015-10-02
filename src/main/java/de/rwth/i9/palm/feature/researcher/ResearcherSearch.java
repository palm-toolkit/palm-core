package de.rwth.i9.palm.feature.researcher;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

public interface ResearcherSearch
{
	public Map<String, Object> getResearcherListByQuery( String query, Integer page, Integer maxresult ) throws IOException, InterruptedException, ExecutionException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException;

	public Map<String, Object> fetchResearcherData( String id, String name, String uri, String affiliation, String pid, String force ) throws IOException, InterruptedException, ExecutionException, ParseException, TimeoutException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException;
}

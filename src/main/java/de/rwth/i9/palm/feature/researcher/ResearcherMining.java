package de.rwth.i9.palm.feature.researcher;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import de.rwth.i9.palm.model.Author;

public interface ResearcherMining
{
	public Map<String, Object> fetchResearcherData( String id, String name, String uri, String affiliation, String pid, String force, List<Author> sessionAuthors ) throws IOException, InterruptedException, ExecutionException, ParseException, TimeoutException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException;

}

package de.rwth.i9.palm.feature.researcher;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import de.rwth.i9.palm.feature.AcademicFeature;

public interface ResearcherInterest extends AcademicFeature
{
	public Map<String, Object> getAuthorInterestById( String authorId, String extractionServiceType, String startDate, String endDate ) throws InterruptedException, UnsupportedEncodingException, URISyntaxException, ParseException, ExecutionException;

	public Map<String, Object> getAuthorInterestByName( String authorName, String extractionServiceType, String startDate, String endDate );
}

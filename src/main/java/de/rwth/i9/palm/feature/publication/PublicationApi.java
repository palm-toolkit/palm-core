package de.rwth.i9.palm.feature.publication;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface PublicationApi
{
	public Map<String, Object> extractPfdFile( String url ) throws IOException, InterruptedException, ExecutionException;

	public Map<String, String> extractHtmlFile( String url ) throws IOException;
}

package de.rwth.i9.palm.topicextraction.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * modified from http://www.opencalais.com/opencalais-api/
 * 
 * @author sigit
 *
 */
public class OpenCalaisAPITopicExtraction
{


	public static Map<String, Object> getTopicsFromText( String text ) throws UnsupportedEncodingException
	{
		Map<String, Object> mapResults = new LinkedHashMap<String, Object>();

		HttpClient client = new HttpClient();
		client.getParams().setParameter( "http.useragent", "Calais Rest Client" );

		PostMethod method = new PostMethod( "https://api.thomsonreuters.com/permid/calais" );

		// === Set mandatory parameters
		// Set token
		method.setRequestHeader( "x-ag-access-token", "TPolW4hYpmQqjYSZFn7YGv2ek0crk7dV" );
		// Set input content type
		method.setRequestHeader( "Content-Type", "text/raw" );
		// Set response/output format
		method.setRequestHeader( "outputformat", "application/json" );

		// === Set optional parameters
		// prevent original text to be sent back
		method.setRequestHeader( "omitOutputtingOriginalText", "true" );
		// Only request socialtag
		method.setRequestHeader( "x-calais-selectiveTags", "socialtags" );
		// Lets you specify the genre of the input files, to optimize
		// extraction.
		method.setRequestHeader( "x-calais-contentClass", "research" );

		// Set content
		method.setRequestEntity( new StringRequestEntity( text, "text/plain", "UTF-8" ) );

		try
		{
			int returnCode = client.executeMethod( method );
			if ( returnCode == HttpStatus.SC_NOT_IMPLEMENTED )
			{
				System.err.println( "The Post method is not implemented by this URI" );
				// still consume the response body
				method.getResponseBodyAsString();
			}
			else if ( returnCode == HttpStatus.SC_OK )
			{
				BufferedReader reader = new BufferedReader( new InputStreamReader( method.getResponseBodyAsStream(), "UTF-8" ) );

				StringBuilder jsonString = new StringBuilder();
				String line;
				while ( ( line = reader.readLine() ) != null )
				{
					jsonString.append( line );
				}

				return extractOpencalaisJson( mapResults, jsonString.toString() );
			}
			else
			{
				System.err.println( "Got code: " + returnCode );
				System.err.println( "response: " + method.getResponseBodyAsString() );
			}
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		finally
		{
			method.releaseConnection();
		}

		return mapResults;
	}

	private static Map<String, Object> extractOpencalaisJson( Map<String, Object> mapResults, String jsonString )
	{

		// map the results into jsonMap
		ObjectMapper mapper = new ObjectMapper();

		JsonNode resultsNodes = null;
		try
		{
			resultsNodes = mapper.readTree( jsonString );
		}
		catch ( Exception e )
		{
		}

		if ( resultsNodes == null )
			return Collections.emptyMap();

		// get language
		if ( !resultsNodes.path( "doc" ).path( "meta" ).path( "language" ).isMissingNode() )
			mapResults.put( "language", resultsNodes.path( "doc" ).path( "meta" ).path( "language" ).asText().toLowerCase() );

		// get term values
		Map<String, Double> termsMapResults = new LinkedHashMap<String, Double>();
		for ( JsonNode jsonNode : resultsNodes )
		{
			if ( !jsonNode.path( "_typeGroup" ).isMissingNode() && jsonNode.path( "_typeGroup" ).asText().equals( "socialTag" ) )
			{
				double termValue = 3.0;
				if ( jsonNode.path( "importance" ).asText().equals( "2" ) )
					termValue = 2.0;
				else if ( jsonNode.path( "importance" ).asText().equals( "2" ) )
					termValue = 1.0;

				termsMapResults.put( jsonNode.path( "name" ).asText().toLowerCase(), termValue );
			}
		}

		if ( termsMapResults.isEmpty() )
			return Collections.emptyMap();
		// put term values
		mapResults.put( "termvalue", termsMapResults );

		return mapResults;
	}
}

package de.rwth.i9.palm.topicextraction.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.web.client.RestTemplate;

public class AlchemyAPITopicExtraction
{
	public static Map<String, Object> getTextRankedKeywords( String text )
	{
		Map<String, Object> mapResults = new LinkedHashMap<String, Object>();

		Map<String, Double> termsMapResults = new LinkedHashMap<String, Double>();
		
		RestTemplate restTemplate = new RestTemplate();

		String endpoint = "http://access.alchemyapi.com/calls/text/TextGetRankedKeywords";
		String apikey = "apikey=dc59f0b1685b54093b73725f900335f9684343d8";
		String outputMode = "outputMode=json";
		String keywordExtractMode = "keywordExtractMode=strict";

		// ResponseEntity<Object[]> responseEntity = restTemplate.getForEntity(
		// endpoint + "?" + apikey + "&" + outputMode + "&" + keywordExtractMode
		// + "&text=" + text, Object[].class );
		// Object[] responseObjects = responseEntity.getBody();
		// MediaType contentType = responseEntity.getHeaders().getContentType();
		// HttpStatus statusCode = responseEntity.getStatusCode();

		@SuppressWarnings( "unchecked" )
		Map<String, Object> resultsMap = restTemplate.getForObject( endpoint + "?" + apikey + "&" + outputMode + "&" + keywordExtractMode + "&text=" + text, Map.class );


		if ( resultsMap.get( "status" ).equals( "OK" ) )
		{
			@SuppressWarnings( "unchecked" )
			List<Map<String, String>> termsList = (List<Map<String, String>>) resultsMap.get( "keywords" );

			if ( termsList == null || termsList.isEmpty() )
				return Collections.emptyMap();

			for ( Map<String, String> termsMap : termsList )
				termsMapResults.put( termsMap.get( "text" ), Double.parseDouble( termsMap.get( "relevance" ) ) );

			mapResults.put( "language", resultsMap.get( "language" ) );
			mapResults.put( "termvalue", termsMapResults );
		}
		else
			return Collections.emptyMap();

		return mapResults;
	}
	
	public static Map<String, Double> getUrlRankedKeywords( String url )
	{
		Map<String, Double> termsMapResults = new LinkedHashMap<String, Double>();

		RestTemplate restTemplate = new RestTemplate();

		String endpoint = "http://access.alchemyapi.com/calls/url/URLGetRankedKeywords";
		String apikey = "apikey=dc59f0b1685b54093b73725f900335f9684343d8";
		String outputMode = "outputMode=json";
		String keywordExtractMode = "keywordExtractMode=strict";

		@SuppressWarnings( "unchecked" )
		Map<String, Object> resultsMap = restTemplate.getForObject( endpoint + "?" + apikey + "&" + outputMode + "&" + keywordExtractMode + "&text=" + url, Map.class );

		// check print
		for ( Entry<String, Object> results : resultsMap.entrySet() )
		{
			if ( results.getKey().equals( "keywords" ) )
			{
				// System.out.println( "Keyword:" );

				@SuppressWarnings( "unchecked" )
				List<Map<String, String>> termsList = (List<Map<String, String>>) results.getValue();

				if ( termsList == null || termsList.isEmpty() )
					return Collections.emptyMap();

				for ( Map<String, String> termsMap : termsList )
					termsMapResults.put( termsMap.get( "text" ), Double.parseDouble( termsMap.get( "relevance" ) ) );

			}
			else
				// System.out.println( result.getKey() + " : " +
				// result.getValue().toString() );
				if ( results.getKey().equals( "text" ) )
				if ( !results.getValue().toString().equals( "Ok" ) )
					return Collections.emptyMap();
		}
		return termsMapResults;
	}

}

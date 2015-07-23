package de.rwth.i9.palm.topicextraction.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.web.client.RestTemplate;

public class YahooContentAnalysisAPITopicExtraction
{
	/**
	 * 
https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20contentanalysis.analyze%20where%20text%3D%22The%20main%20aim%20of%20Knowledge%20Management%20(KM)%20is%20to%20connect%20people%20to%20quality%20knowledge%20as%20well%20as%20people%20to%20people%20in%20order%20to%20peak%20performance.%20This%20is%20also%20the%20primary%20goal%20of%20Learning%20Management%20(LM).%20In%20fact%2C%20in%20the%20world%20of%20e-learning%2C%20it%20is%20more%20widely%20recognised%20that%20how%20learning%20content%20is%20used%20and%20distributed%20by%20learners%20might%20be%20more%20important%20than%20how%20it%20is%20designed.%20In%20the%20last%20few%20years%2C%20there%20has%20been%20an%20increasing%20focus%20on%20social%20software%20applications%20and%20services%20as%20a%20result%20of%20the%20rapid%20development%20of%20Web%202.0%20concepts.%20In%20this%20paper%2C%20we%20argue%20that%20LM%20and%20KM%20can%20be%20viewed%20as%20two%20sides%20of%20the%20same%20coin%2C%20and%20explore%20how%20Web%202.0%20technologies%20can%20leverage%20knowledge%20sharing%20and%20learning%20and%20enhance%20individual%20performance%20whereas%20previous%20models%20of%20LM%20and%20KM%20have%20failed%2C%20and%20present%20a%20social%20software%20driven%20approach%20to%20LM%20and%20KM%22&format=json&diagnostics=true&callback=

{"query":{"count":1,"created":"2015-07-22T14:55:51Z","lang":"en-US","diagnostics":{"publiclyCallable":"true","url":{"execution-start-time":"2","execution-stop-time":"59","execution-time":"57","content":"http://analyze.yahooapis.com/ydn/cap.annotate"},"javascript":{"execution-start-time":"0","execution-stop-time":"60","execution-time":"60","instructions-used":"1161","table-name":"contentanalysis.analyze"},"user-time":"61","service-time":"57","build-version":"0.2.154"},"results":{"entities":{"entity":[{"score":"0.873","text":{"end":"193","endchar":"193","start":"175","startchar":"175","content":"Learning Management"},"types":{"type":{"region":"us","content":"/organization"}}},{"score":"0.844","text":{"end":"35","endchar":"35","start":"16","startchar":"16","content":"Knowledge Management"},"wiki_url":"http://en.wikipedia.com/wiki/Knowledge_management","types":{"type":{"region":"us","content":"/organization"}}},{"score":"0.758","text":{"end":"39","endchar":"39","start":"38","startchar":"38","content":"KM"},"wiki_url":"http://en.wikipedia.com/wiki/Ferhat_%c3%87%c3%b6km%c3%bc%c5%9f","types":{"type":{"region":"us","content":"/organization"}}}]}}}}
	 */

	public static Map<String, Double> getTextRankedKeywords( String text )
	{
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

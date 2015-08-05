package de.rwth.i9.palm.topicextraction.service;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.client.RestTemplate;

public class FiveFiltersAPITopicExtraction
{
	/**
	 * 
	 * @throws URISyntaxException
	 * @throws UnsupportedEncodingException
	 */

	public static Map<String, Object> getTextTermExtract( String text ) throws UnsupportedEncodingException
	{
		Map<String, Object> mapResults = new LinkedHashMap<String, Object>();

		Map<String, Double> termsMapResults = new LinkedHashMap<String, Double>();

		RestTemplate restTemplate = new RestTemplate();

		String endpoint = "http://termextract.fivefilters.org/extract.php?text=";
		String endQuery = "&output=json";

		@SuppressWarnings( "unchecked" )
		List<List<Object>> resultsList = (List<List<Object>>) restTemplate.getForObject( endpoint + text + endQuery, List.class );

		for ( List<Object> termObject : resultsList )
		{
			termsMapResults.put( termObject.get( 0 ).toString(), ( Double.parseDouble( termObject.get( 1 ).toString() ) + Double.parseDouble( termObject.get( 2 ).toString() ) ) );
		}

		mapResults.put( "termvalue", termsMapResults );

		return mapResults;
	}
}

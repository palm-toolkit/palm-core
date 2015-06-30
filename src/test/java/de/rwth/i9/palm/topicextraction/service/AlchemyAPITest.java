package de.rwth.i9.palm.topicextraction.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( loader = AnnotationConfigContextLoader.class )
public class AlchemyAPITest
{
	@Test
	@Ignore
	public void getListOfAuthorsTest() throws RestClientException, UnsupportedEncodingException
	{
		String endpoint = "http://access.alchemyapi.com/calls/text/TextGetRankedKeywords";
		String apikey = "apikey=dc59f0b1685b54093b73725f900335f9684343d8";
		String outputMode = "outputMode=json";
		String keywordExtractMode = "keywordExtractMode=strict";
		String text = "";

		String encodedText = cutToLength( URLEncoder.encode( text, "UTF-8" ), 6000 );

		RestTemplate restTemplate = new RestTemplate();
		String result = restTemplate.getForObject( endpoint + "?" + apikey + "&" + outputMode + "&" + keywordExtractMode + "&text=" + encodedText, String.class );

		System.out.println( result );
	}

	@Test
	public void extractWebTest() throws RestClientException, UnsupportedEncodingException
	{
		String endpoint = "http://access.alchemyapi.com/calls/url/URLGetRankedKeywords";
		String apikey = "apikey=dc59f0b1685b54093b73725f900335f9684343d8";
		String outputMode = "outputMode=json";
		String keywordExtractMode = "keywordExtractMode=strict";
		String url = "url=http://edition.cnn.com/2015/06/24/europe/france-wikileaks-nsa-spying-claims/index.html";

		// String encodedText = cutToLength( URLEncoder.encode( text, "UTF-8" ),
		// 6000 );

		RestTemplate restTemplate = new RestTemplate();
		String result = restTemplate.getForObject( endpoint + "?" + apikey + "&" + outputMode + "&" + keywordExtractMode + "&" + url, String.class );

		System.out.println( result );
	}

	public String cutToLength( String text, int maxLength )
	{
		if ( text.length() > maxLength )
			return text.substring( 0, maxLength );
		return text;
	}
}

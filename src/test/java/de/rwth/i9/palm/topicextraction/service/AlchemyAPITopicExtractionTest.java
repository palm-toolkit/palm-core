package de.rwth.i9.palm.topicextraction.service;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Map.Entry;

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
public class AlchemyAPITopicExtractionTest
{
	@Test
	@Ignore
	public void getListOfAuthorsTest() throws RestClientException, UnsupportedEncodingException
	{
		String endpoint = "http://access.alchemyapi.com/calls/text/TextGetRankedKeywords";
		String apikey = "apikey=dc59f0b1685b54093b73725f900335f9684343d8";
		String outputMode = "outputMode=json";
		String keywordExtractMode = "keywordExtractMode=strict";
		String text = "The main aim of Knowledge Management (KM) is to connect people to quality knowledge as well as people to people in order to peak performance. This is also the primary goal of Learning Management (LM). In fact, in the world of e-learning, it is more widely recognised that how learning content is used and distributed by learners might be more important than how it is designed. In the last few years, there has been an increasing focus on social software applications and services as a result of the rapid development of Web 2.0 concepts. In this paper, we argue that LM and KM can be viewed as two sides of the same coin, and explore how Web 2.0 technologies can leverage knowledge sharing and learning and enhance individual performance whereas previous models of LM and KM have failed, and present a social software driven approach to LM and KM";
		String text2 = "Kompetenzentwicklung in Lernnetzwerken für das lebenslange Lernen Lebenslanges Lernen ist eines der Schlüsselthemen für die Wissensgesellschaft. Abseits der formal organisierten Bildungsangebote hat sich mit der Verbreitung und Nutzung von Social Software eine neue und sehr heterogene Organisationsform des technologiegestützten Lernens entwickelt, die große Potenziale für die lebenslange Kompetenzentwicklung bietet. Dieser Beitrag beschreibt diese neue Organisationsform, stellt das Konzept der Social Software sowie einige beispielhafte Applikationen vor und";

		String encodedText = text2;// URLEncoder.encode( text, "UTF-8"
									// ).replace( "\\+", "%20" );

		RestTemplate restTemplate = new RestTemplate();
		String result = restTemplate.getForObject( endpoint + "?" + apikey + "&" + outputMode + "&" + keywordExtractMode + "&text=" + encodedText, String.class );

		System.out.println( result );
	}

	@SuppressWarnings( "unchecked" )
	@Test
	@Ignore
	public void getTextRankedKeywordsTest()
	{
		String text = "The main aim of Knowledge Management (KM) is to connect people to quality knowledge as well as people to people in order to peak performance. This is also the primary goal of Learning Management (LM). In fact, in the world of e-learning, it is more widely recognised that how learning content is used and distributed by learners might be more important than how it is designed. In the last few years, there has been an increasing focus on social software applications and services as a result of the rapid development of Web 2.0 concepts. In this paper, we argue that LM and KM can be viewed as two sides of the same coin, and explore how Web 2.0 technologies can leverage knowledge sharing and learning and enhance individual performance whereas previous models of LM and KM have failed, and present a social software driven approach to LM and KM";
		Map<String, Object> resultsMap = AlchemyAPITopicExtraction.getTextRankedKeywords( text );

		System.out.println( "language: " + resultsMap.get( "language" ).toString() );

		if ( resultsMap.get( "termvalue" ) != null )
			for ( Entry<String, Object> termValue : ( (Map<String, Object>) resultsMap.get( "termvalue" ) ).entrySet() )
				System.out.println( termValue.getKey() + " : " + termValue.getValue() );
	}

	@Test
	@Ignore
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

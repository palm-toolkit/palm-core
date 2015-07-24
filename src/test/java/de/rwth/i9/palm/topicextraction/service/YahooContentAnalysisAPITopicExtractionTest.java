package de.rwth.i9.palm.topicextraction.service;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( loader = AnnotationConfigContextLoader.class )
public class YahooContentAnalysisAPITopicExtractionTest
{

	@Test
	public void getTextRankedKeywordsTest()
	{
		String text = "The main aim of Knowledge Management (KM) is to connect people to quality knowledge as well as people to people in order to peak performance. This is also the primary goal of Learning Management (LM). In fact, in the world of e-learning, it is more widely recognised that how learning content is used and distributed by learners might be more important than how it is designed. In the last few years, there has been an increasing focus on social software applications and services as a result of the rapid development of Web 2.0 concepts. In this paper, we argue that LM and KM can be viewed as two sides of the same coin, and explore how Web 2.0 technologies can leverage knowledge sharing and learning and enhance individual performance whereas previous models of LM and KM have failed, and present a social software driven approach to LM and KM";
		Map<String, Double> rankedKeyword = AlchemyAPITopicExtraction.getTextRankedKeywords( text );
		for ( Entry<String, Double> termValue : rankedKeyword.entrySet() )
			System.out.println( termValue.getKey() + " : " + termValue.getValue() );
	}


	public String cutToLength( String text, int maxLength )
	{
		if ( text.length() > maxLength )
			return text.substring( 0, maxLength );
		return text;
	}
}

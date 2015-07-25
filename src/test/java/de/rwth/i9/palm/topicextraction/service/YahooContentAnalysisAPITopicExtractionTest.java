package de.rwth.i9.palm.topicextraction.service;

import org.junit.Ignore;
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
	@Ignore
	public void getTextRankedKeywordsTest()
	{
	}


	public String cutToLength( String text, int maxLength )
	{
		if ( text.length() > maxLength )
			return text.substring( 0, maxLength );
		return text;
	}
}

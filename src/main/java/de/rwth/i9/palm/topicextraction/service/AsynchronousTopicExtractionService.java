package de.rwth.i9.palm.topicextraction.service;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;

import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationTopic;

@Service
public class AsynchronousTopicExtractionService
{
	private final static Logger log = LoggerFactory.getLogger( AsynchronousTopicExtractionService.class );
	private final String TOPIC_EXTRACTION_SERVOCE_TYPE = "ALCHEMYAPI";

	@Async
	public Future<PublicationTopic> getTopicsByAlchemyApi( Publication publication, PublicationTopic publicationTopic, int maxTextLength )
	{
		Stopwatch stopwatch = Stopwatch.createStarted();

		log.info( "AlchemyAPI extract publication " + publication.getTitle() + " starting" );

		String text = publication.getTitle();
		if ( publication.getAbstractText() != null )
			text += " " + publication.getAbstractText();
		if ( publication.getContentText() != null )
			text += " " + publication.getContentText();

		text = TopicExtractionUtils.cutTextToLength( text, maxTextLength );

		Map<String, Double> topicsWithValue = AlchemyAPITopicExtraction.getTextRankedKeywords( text );

		if ( topicsWithValue != null )
		{
			publicationTopic.setTermValues( topicsWithValue );
			publicationTopic.setValid( true );
		}
		else
			publicationTopic.setValid( false );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );

		log.info( "AlchemyAPI extract publication " + publication.getTitle() + " complete in " + stopwatch );
		return new AsyncResult<PublicationTopic>( publicationTopic );
	}
}

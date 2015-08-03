package de.rwth.i9.palm.topicextraction.service;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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
	private final String TOPIC_EXTRACTION_SERVICE_TYPE = "ALCHEMYAPI";

	@SuppressWarnings( "unchecked" )
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

		// free alchemy has certain text limitation in length
		text = TopicExtractionUtils.cutTextToLength( text, maxTextLength );

		Map<String, Object> alchemyResultsMap = AlchemyAPITopicExtraction.getTextRankedKeywords( text );

		if ( alchemyResultsMap != null )
		{
			publication.setLanguage( alchemyResultsMap.get( "language" ).toString() );

			publicationTopic.setTermValues( (Map<String, Double>) alchemyResultsMap.get( "termvalue" ) );
			// filter duplicated keys, caused by accented character
			filterAlchemyResult( publicationTopic.getTermValues() );

			publicationTopic.setValid( true );
		}
		else
			publicationTopic.setValid( false );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );

		log.info( "AlchemyAPI extract publication " + publication.getTitle() + " complete in " + stopwatch );
		return new AsyncResult<PublicationTopic>( publicationTopic );
	}

	private void filterAlchemyResult( Map<String, Double> alchemyResultsMap )
	{
		Set<String> mapKeys = new HashSet<String>();

		for ( Iterator<Map.Entry<String, Double>> it = alchemyResultsMap.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<String, Double> entry = it.next();
			// filter the accented character
			String mapKey = deAccent( entry.getKey() );
			// if there is similar key, remove resultmap
			if ( mapKeys.contains( mapKey ) )
				it.remove();
			else
				mapKeys.add( mapKey );
		}

	}

	private String deAccent( String str )
	{
		String nfdNormalizedString = Normalizer.normalize( str, Normalizer.Form.NFD );
		Pattern pattern = Pattern.compile( "\\p{InCombiningDiacriticalMarks}+" );
		return pattern.matcher( nfdNormalizedString ).replaceAll( "" );
	}
}

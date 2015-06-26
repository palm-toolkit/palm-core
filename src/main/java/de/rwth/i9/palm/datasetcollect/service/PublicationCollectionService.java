package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;

@Service
public class PublicationCollectionService
{
	private final static Logger log = LoggerFactory.getLogger( PublicationCollectionService.class );

	@Async
	public Future<Long> callAsync( int taskCall ) throws InterruptedException
	{

		Stopwatch stopwatch = Stopwatch.createStarted();

		log.info( "task " + taskCall + " starting" );

		Thread.sleep( 500 );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );

		log.info( "task " + taskCall + "completed in " + stopwatch );

		return new AsyncResult<Long>( stopwatch.elapsed( TimeUnit.MILLISECONDS ) );
	}

	@Async
	public Future<Map<String, Map<String, String>>> getListOfAuthorsGoogleScholar( String authorName ) throws IOException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();

		log.info( "get author from google scholar with query " + authorName + " starting" );

		Map<String, Map<String, String>> authorMap = GoogleScholarPublicationCollection.getListOfAuthors( authorName );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );

		log.info( "get author from google scholar with query " + authorName + " complete in " + stopwatch );
		return new AsyncResult<Map<String, Map<String, String>>>( authorMap );
	}

	@Async
	public Future<Map<String, Map<String, String>>> getListOfAuthorsCiteseerX( String authorName ) throws IOException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();

		log.info( "get author from citeseerX with query " + authorName + " starting" );

		Map<String, Map<String, String>> authorMap = CiteseerXPublicationCollection.getListOfAuthors( authorName );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );

		log.info( "get author from citeSeerX with query " + authorName + " complete in " + stopwatch );
		return new AsyncResult<Map<String, Map<String, String>>>( authorMap );
	}
}

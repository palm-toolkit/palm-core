package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.google.common.base.Stopwatch;

import de.rwth.i9.palm.config.WebAppConfigTest;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( classes = WebAppConfigTest.class, loader = AnnotationConfigContextLoader.class )
public class PublicationCollectionServiceTest
{
	private final static Logger log = LoggerFactory.getLogger( PublicationCollectionServiceTest.class );

	@Autowired
	private PublicationCollectionService publicationCollectionService;

	@Test
	@Ignore
	public void test() throws IOException, InterruptedException, ExecutionException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();

		Future<Long> asyncResult1 = publicationCollectionService.callAsync( 1 );
		Future<Long> asyncResult2 = publicationCollectionService.callAsync( 2 );
		Future<Long> asyncResult3 = publicationCollectionService.callAsync( 3 );
		Future<Long> asyncResult4 = publicationCollectionService.callAsync( 4 );

		log.info( "result 1 took: " + asyncResult1.get() );
		log.info( "result 2 took: " + asyncResult2.get() );
		log.info( "result 3 took: " + asyncResult3.get() );
		log.info( "result 4 took: " + asyncResult4.get() );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );
		log.info( "time it took to perform work " + stopwatch );

	}

	@Test
	public void test2() throws IOException, InterruptedException, ExecutionException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();

		Future<List<Map<String, String>>> authorGoogleScholar = publicationCollectionService.getListOfAuthorsGoogleScholar( "chatti" );
		Future<List<Map<String, String>>> authorCiteseerX = publicationCollectionService.getListOfAuthorsCiteseerX( "chatti" );

		for ( Map<String, String> eachAuthor : authorGoogleScholar.get() )
		{
			for ( Entry<String, String> eachAuthorDetail : eachAuthor.entrySet() )
				System.out.println( eachAuthorDetail.getKey() + " : " + eachAuthorDetail.getValue() );
			System.out.println();
		}

		for ( Map<String, String> eachAuthor : authorCiteseerX.get() )
		{
			for ( Entry<String, String> eachAuthorDetail : eachAuthor.entrySet() )
				System.out.println( eachAuthorDetail.getKey() + " : " + eachAuthorDetail.getValue() );
			System.out.println();
		}

		stopwatch.elapsed( TimeUnit.MILLISECONDS );
		log.info( "time it took to perform work " + stopwatch );
	}
}

package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.ArrayList;
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
	private AsynchronousCollectionService asynchronousCollectionService;

	@Test
	@Ignore
	public void test() throws IOException, InterruptedException, ExecutionException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();

		Future<Long> asyncResult1 = asynchronousCollectionService.callAsync( 1 );
		Future<Long> asyncResult2 = asynchronousCollectionService.callAsync( 2 );
		Future<Long> asyncResult3 = asynchronousCollectionService.callAsync( 3 );
		Future<Long> asyncResult4 = asynchronousCollectionService.callAsync( 4 );
		Future<Long> asyncResult5 = asynchronousCollectionService.callAsync( 5 );
		Future<Long> asyncResult6 = asynchronousCollectionService.callAsync( 6 );
		Future<Long> asyncResult7 = asynchronousCollectionService.callAsync( 7 );
		Future<Long> asyncResult8 = asynchronousCollectionService.callAsync( 8 );
		Future<Long> asyncResult9 = asynchronousCollectionService.callAsync( 9 );
		Future<Long> asyncResult10 = asynchronousCollectionService.callAsync( 10 );
		Future<Long> asyncResult11 = asynchronousCollectionService.callAsync( 11 );
		Future<Long> asyncResult12 = asynchronousCollectionService.callAsync( 12 );


		log.info( "result 1 took: " + asyncResult1.get() );
		log.info( "result 2 took: " + asyncResult2.get() );
		log.info( "result 3 took: " + asyncResult3.get() );
		log.info( "result 4 took: " + asyncResult4.get() );
		log.info( "result 5 took: " + asyncResult5.get() );
		log.info( "result 6 took: " + asyncResult6.get() );
		log.info( "result 7 took: " + asyncResult7.get() );
		log.info( "result 8 took: " + asyncResult8.get() );
		log.info( "result 9 took: " + asyncResult9.get() );
		log.info( "result 10 took: " + asyncResult10.get() );
		log.info( "result 11 took: " + asyncResult11.get() );
		log.info( "result 12 took: " + asyncResult12.get() );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );
		log.info( "time it took to perform work " + stopwatch );

	}

	@Test
	@Ignore
	public void testList() throws IOException, InterruptedException, ExecutionException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();

		List<Future<Long>> asyncResult = new ArrayList<Future<Long>>();
		
		for( int i=0;i<20;i++){
			asyncResult.add( asynchronousCollectionService.callAsync( i ) );
		}
//
		// asyncResult.add( asynchronousCollectionService.callAsync( 1 ) );
		// asyncResult.add( asynchronousCollectionService.callAsync( 2 ) );
		// asyncResult.add( asynchronousCollectionService.callAsync( 3 ) );
		// asyncResult.add( asynchronousCollectionService.callAsync( 4 ) );
		// asyncResult.add( asynchronousCollectionService.callAsync( 5 ) );
		// asyncResult.add( asynchronousCollectionService.callAsync( 6 ) );
		// asyncResult.add( asynchronousCollectionService.callAsync( 7 ) );
		// asyncResult.add( asynchronousCollectionService.callAsync( 8 ) );
		// asyncResult.add( asynchronousCollectionService.callAsync( 9 ) );
		// asyncResult.add( asynchronousCollectionService.callAsync( 10 ) );
		// asyncResult.add( asynchronousCollectionService.callAsync( 11 ) );
		// asyncResult.add( asynchronousCollectionService.callAsync( 12 ) );
		
		boolean processIsDone = true;
		do
		{
			processIsDone = true;
			for ( Future<Long> futureList :  asyncResult  )
			{
				if ( !futureList.isDone() )
				{
					processIsDone = false;
					break;
				}
			}
			// 10-millisecond pause between each check
			Thread.sleep( 10 );
		} while ( !processIsDone );
		
		for( int i=0;i<20;i++){
			log.info( "result 1 took: " + asyncResult.get( i ).get() );
		}
//		log.info( "result 1 took: " + asyncResult.get( 0 ).get() );
//		log.info( "result 2 took: " + asyncResult.get( 0 ).get() );
//		log.info( "result 3 took: " + asyncResult.get( 0 ).get() );
//		log.info( "result 4 took: " + asyncResult.get( 0 ).get() );
//		log.info( "result 5 took: " + asyncResult.get( 0 ).get() );
//		log.info( "result 6 took: " + asyncResult.get( 0 ).get() );
//		log.info( "result 7 took: " + asyncResult.get( 0 ).get() );
//		log.info( "result 8 took: " + asyncResult.get( 0 ).get() );
//		log.info( "result 9 took: " + asyncResult.get( 0 ).get() );
//		log.info( "result 10 took: " + asyncResult.get( 0 ).get() );
//		log.info( "result 11 took: " + asyncResult.get( 0 ).get() );
//		log.info( "result 12 took: " + asyncResult.get( 0 ).get() );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );
		log.info( "time it took to perform work " + stopwatch );

	}

	@Test
	@Ignore
	public void test2() throws IOException, InterruptedException, ExecutionException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();

		Future<List<Map<String, String>>> authorGoogleScholar = asynchronousCollectionService.getListOfAuthorsGoogleScholar( "chatti" );
		Future<List<Map<String, String>>> authorCiteseerX = asynchronousCollectionService.getListOfAuthorsCiteseerX( "chatti" );

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

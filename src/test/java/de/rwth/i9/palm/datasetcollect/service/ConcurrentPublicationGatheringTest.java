package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
public class ConcurrentPublicationGatheringTest
{
	private final static Logger log = LoggerFactory.getLogger( ConcurrentPublicationGatheringTest.class );

	@Autowired
	private AsynchronousCollectionService asynchronousCollectionService;

	@Test
	@Ignore
	public void testList() throws IOException, InterruptedException, ExecutionException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();

		List<Future<Long>> asyncResult = new ArrayList<Future<Long>>();

		for ( int i = 0; i < 20; i++ )
		{
			asyncResult.add( asynchronousCollectionService.callAsync( i ) );
		}

		boolean processIsDone = true;
		do
		{
			processIsDone = true;
			for ( Future<Long> futureList : asyncResult )
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

		for ( int i = 0; i < 20; i++ )
		{
			log.info( "result 1 took: " + asyncResult.get( i ).get() );
		}

		stopwatch.elapsed( TimeUnit.MILLISECONDS );
		log.info( "time it took to perform work " + stopwatch );

	}

}

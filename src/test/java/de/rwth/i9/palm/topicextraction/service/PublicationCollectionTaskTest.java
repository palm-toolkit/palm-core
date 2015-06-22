package de.rwth.i9.palm.topicextraction.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import de.rwth.i9.palm.concurrent.ObjectPool;
import de.rwth.i9.palm.config.DatabaseConfigCoreTest;
import de.rwth.i9.palm.datasetcollect.service.PublicationCollection;
//import de.rwth.i9.palm.model.PublicationOld;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( classes = DatabaseConfigCoreTest.class, loader = AnnotationConfigContextLoader.class )
public class PublicationCollectionTaskTest extends AbstractTransactionalJUnit4SpringContextTests
{
	
	private ObjectPool<PublicationCollection> pool;
	
	private AtomicLong processNo = new AtomicLong(0);

	final Logger logger = Logger.getLogger( PublicationCollectionTaskTest.class );

	@Before
	public void setUp()
	{
		 // Create a pool of objects of type PublicationCollection. Parameters:
        // 1) Minimum number of special PublicationCollection instances residing in the pool = 4
        // 2) Maximum number of special PublicationCollection instances residing in the pool = 10
        // 3) Time in seconds for periodical checking of minIdle / maxIdle conditions in a separate thread = 5.
        //    When the number of PublicationCollection instances is less than minIdle, missing instances will be created.
        //    When the number of PublicationCollection instances is greater than maxIdle, too many instances will be removed.
        //    If the validation interval is negative, no periodical checking of minIdle / maxIdle conditions
        //    in a separate thread take place. These boundaries are ignored then.
        pool = new ObjectPool<PublicationCollection>(4, 10, 5)
        {
            protected PublicationCollection createObject() {
                // create a test object which takes some time for creation
                return new PublicationCollection("/home/temp/", processNo.incrementAndGet());
            }
        };
	}

	@After
    public void tearDown() {
        pool.shutdown();
    }

    @Test
	@Ignore
    public void testObjectPool() {
        ExecutorService executor = Executors.newFixedThreadPool(8);

        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}

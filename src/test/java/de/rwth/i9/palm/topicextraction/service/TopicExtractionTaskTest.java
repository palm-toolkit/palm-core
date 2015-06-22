package de.rwth.i9.palm.topicextraction.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import de.rwth.i9.palm.config.DatabaseConfigCoreTest;
//import de.rwth.i9.palm.model.PublicationOld;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( classes = DatabaseConfigCoreTest.class, loader = AnnotationConfigContextLoader.class )
@TransactionConfiguration
@Transactional
public class TopicExtractionTaskTest extends AbstractTransactionalJUnit4SpringContextTests
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	final Logger logger = Logger.getLogger( TopicExtractionTaskTest.class );

	@BeforeTransaction
	public void beforeTransaction()
	{
	}

	@Test
	public void testObjectPool()
	{
		ExecutorService executor = Executors.newFixedThreadPool( 8 );

		// execute
	}

}

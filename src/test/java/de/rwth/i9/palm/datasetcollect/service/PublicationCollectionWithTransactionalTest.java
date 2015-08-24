package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import de.rwth.i9.palm.config.DatabaseConfigCoreTest;
import de.rwth.i9.palm.config.WebAppConfigTest;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;
//import de.rwth.i9.palm.model.PublicationOld;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( classes = { WebAppConfigTest.class, DatabaseConfigCoreTest.class }, loader = AnnotationConfigContextLoader.class )
@TransactionConfiguration
@Transactional
public class PublicationCollectionWithTransactionalTest extends AbstractTransactionalJUnit4SpringContextTests
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private PublicationCollectionService publicationCollectionService;

	@Autowired
	private AsynchronousPublicationDetailCollectionService asynchronousPublicationDetailCollectionService;

	final Logger logger = Logger.getLogger( PublicationCollectionWithTransactionalTest.class );

	@Test

	public void testPublicationInformationEnrichment() throws IOException, InterruptedException, ExecutionException, TimeoutException
	{
		List<Author> authors = persistenceStrategy.getAuthorDAO().getByName( "mohamed amine chatti" );
		if ( authors != null && !authors.isEmpty() )
			publicationCollectionService.enrichPublicationByExtractOriginalSources( new ArrayList<Publication>( authors.get( 0 ).getPublications() ), authors.get( 0 ), false );
	}

	@Test
	@Ignore
	public void testPublicationEnrichmentPerPublication() throws IOException, InterruptedException, ExecutionException, TimeoutException
	{

		Publication publication = persistenceStrategy.getPublicationDAO().getById( "8af08983-9b1e-4d6e-b771-5fc092cd444e" );
		Future<Publication> publicationFuture = asynchronousPublicationDetailCollectionService.asyncEnrichPublicationInformationFromOriginalSource( publication );

		publicationFuture.get();
		
//		boolean enrichmentProcessIsDone = true;
//		do
//		{
//
//			if ( publicationFuture.isDone() )
//			{
//				enrichmentProcessIsDone = false;
//			}
//			// 10-millisecond pause between each check
//			Thread.sleep( 5000 );
//		} while ( !enrichmentProcessIsDone );
	}
}

package de.rwth.i9.palm.test;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
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
public class TestGetDataAndMahout extends AbstractTransactionalJUnit4SpringContextTests
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private PalmAnalytics palmAnalytics;

	final Logger logger = Logger.getLogger( TestGetDataAndMahout.class );

	@Test
	public void testGetDataFromDatabase()
	{
		System.out.println( "\n========== TEST 1 - Fetch data from database ==========" );

		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();
		int count = 0;
		if ( authors != null )
			for ( Author author : authors )
			{
				System.out.println( "author id : " + author.getId() + " > author name : " + author.getName() );
				// print one of the publication

				if ( author.getPublications() != null )
				{
					for ( Publication publication : author.getPublications() )
					{
						System.out.println( "publication id : " + publication.getId() + " > pub title: " + publication.getTitle() );
					}
				}
				System.out.println();

				if ( count > 200 )
					break;

				count++;
			}
		System.out.println( "\n\n" );
	}

	@Test
	public void testGetMethodFromOtherModule()
	{
		System.out.println( "\n========== TEST 2 - Use some function from other module  ==========" );
		String text1 = "Technology Enhanced Professional Learning";
		String text2 = "Technology Enhanced Professional Learner";

		System.out.println( "text1 : " + text1 );
		System.out.println( "text2 : " + text2 );
		System.out.println( "Levenshtein Distance : " + palmAnalytics.getTextCompare().getDistanceByLuceneLevenshteinDistance( text1, text2 ) );
		System.out.println( "\n\n" );
	}
}

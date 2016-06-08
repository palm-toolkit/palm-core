package de.rwth.i9.palm.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( classes = { WebAppConfigTest.class, DatabaseConfigCoreTest.class }, loader = AnnotationConfigContextLoader.class )
@TransactionConfiguration
@Transactional

public class TestGetDataPublications extends AbstractTransactionalJUnit4SpringContextTests
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	private final static Logger log = LoggerFactory.getLogger( TestGetDataPublications.class );


	@Test
	// @Ignore
	public void testGetDatabaseFromDatabase() throws FileNotFoundException, UnsupportedEncodingException
	{
		System.out.println( "\n========== TEST 1 - Fetch publications from database ==========" );
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();
		if ( !authors.isEmpty() )
			for ( Author author : authors )
			{
				for ( Publication publication : author.getPublications() )
				{
					if ( publication.getAbstractText() != "null" )
					{
						PrintWriter writer = new PrintWriter( "C:/Users/Piro/Desktop/Publications/Publications/" + publication.getId() + ".txt", "UTF-8" );
						writer.print( publication.getTitle() + " " );
						writer.print( publication.getAbstractText() );
						writer.println();
						writer.close();
					}
					else
					{
						continue;
					}
				}
			}
	}

	@Test
	@Ignore
	public void testGetDatabaseFromDatabaseOnSpecificYear2() throws IOException
	{
		System.out.println( "\n========== TEST 3 - Fetch publications per author Yearly from database ==========" );
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();// getByName(
																			// "mohamed
																			// amine
																			// chatti"
																			// );//getById(
																			// "e14fd198-1e54-449f-96aa-7e19d0eec488"
																			// );
		if ( !authors.isEmpty() )
			for ( Author author : authors )
			{
				for ( int year = 1980; year < 2017; year++ )
				{
					if ( !author.getPublicationsByYear( year ).isEmpty() )
					{
						PrintWriter writer = new PrintWriter( "C:/Users/Piro/Desktop/Year-Test/" + author.getId().toString() + "/" + year + ".txt" );
						for ( Publication publication : author.getPublicationsByYear( year ) )
						{
							writer.print( publication.getTitle() + " " );
							writer.println( publication.getAbstractText() + " " );
						}
						writer.close();
					}
				}
			}
	}

	@Test
	@Ignore
	public void testcreateEntityDirectories() throws IOException
	{
		System.out.println( "\n========== TEST 2 - Create Architecture for the Data Collection ==========" );
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();
			for ( Author author : authors )
			{
			// to be updated
				File theDir = new File( "C:/Users/Piro/Desktop/Year-Test/" + author.getId().toString() );

				// if the directory does not exist, create it
				if ( !theDir.exists() )
				{
					boolean result = false;

					try
					{
						theDir.mkdir();
						result = true;
					}
					catch ( SecurityException se )
					{
						// handle it
					}
					if ( result )
					{
						System.out.println( "DIR created" );
					}
				}
			}
	}


}
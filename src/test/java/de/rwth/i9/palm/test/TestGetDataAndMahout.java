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

public class TestGetDataAndMahout extends AbstractTransactionalJUnit4SpringContextTests
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Test
	@Ignore
	public void testGetDataFromDatabase() throws FileNotFoundException, UnsupportedEncodingException
	{
		System.out.println( "\n========== TEST 0 - Fetch data from database ==========" );

		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();

		int count = 0;
		if ( authors != null )
			for ( Author author : authors )
			{
				// System.out.println( "author id : " + author.getId() + " >
				// author name : " + author.getName() );
				// print one of the publication
				PrintWriter writer = new PrintWriter( "C:/Users/Piro/Desktop/Authors/Authors/" + author.getId() + ".txt", "UTF-8" );

				if ( author.getPublications() != null )
				{
					for ( Publication publication : author.getPublications() )
					{
						// System.out.println( "publication id : " +
						// publication.getId() + " > pub title: " +
						// publication.getTitle() );
						writer.print( publication.getTitle() + " " );
						writer.print( publication.getAbstractText() );
						writer.println();
					}
					writer.close();
				}
			}
	}
	
	@Test
	@Ignore
	public void testGetCoauthorsofPublication()
	{
		System.out.println( "\n========== TEST 4 - Fetch authors of publication from database ==========" );

		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();

		if ( authors != null )
			for ( Author author : authors )
			{
				for ( Publication publication : author.getPublications() )
				{
					List<Author> p = publication.getAuthors();
				}
			}
		System.out.println( "\n\n" );
	}

	@Test
	public void testGetDatabaseFromDatabase() throws FileNotFoundException, UnsupportedEncodingException
	{
		System.out.println( "\n========== TEST 1 - Fetch publications per author from database ==========" );
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();//getByName( "mohamed amine chatti" );//getById( "e14fd198-1e54-449f-96aa-7e19d0eec488" );
	
		if( !authors.isEmpty())
			for (Author author:authors)
			{	
				for(Publication publication : author.getPublications()){
					if ( publication.getAbstractText() != "null" )
					{
						PrintWriter writer = new PrintWriter( "C:/Users/Piro/Desktop/Author-Test/" + author.getId() + "/" + publication.getId() + ".txt", "UTF-8" );
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
	public void testcreateEntityDirectories() throws IOException
	{
		System.out.println( "\n========== TEST 2 - Create Architecture for the Data Collection ==========" );
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

				File theDir = new File( "C:/Users/Piro/Desktop/Author-Test/" + author.getId().toString() );

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
						PrintWriter writer = new PrintWriter( "C:/Users/Piro/Desktop/Author-Year-Test/" + author.getId().toString() + "/" + year + ".txt" );
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
	public void testGetDatabaseFromDatabase2() throws IOException
	{

		System.out.println( "\n========== TEST 4 - Fetch publications from database ==========" );
		
		Author authors = persistenceStrategy.getAuthorDAO().getById( "c442983a-0099-4d6d-89b1-6cfc57fa6138" );// getAll();//getByName(
																																	// "mohamed
																																	// amine
																																	// chatti"
																																	// );//getById(
																																	// ""
																																	// );
		for ( Publication publication : authors.getPublications() )
		{
						PrintWriter pub = new PrintWriter( "C:/Users/Piro/Desktop/Authors/Publications_Chatti/" + publication.getId() + ".txt", "UTF-8" );
						pub.print(publication.getTitle() + " " + publication.getAbstractText());
						pub.println();
						pub.close();
					}	
			}
}

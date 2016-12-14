package de.rwth.i9.palm.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.config.DatabaseConfigCoreTest;
import de.rwth.i9.palm.config.WebAppConfigTest;
import de.rwth.i9.palm.feature.researcher.ResearcherFeature;
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
	
	String path =  "C:/Users/Albi/Desktop/";

	@Autowired // actually this one is for the API, so I guess you don't need to
				// use this
	private ResearcherFeature researcherFeature;

	@Autowired
	private PalmAnalytics palmAnalytics;

	private final static Logger log = LoggerFactory.getLogger( TestGetDataAndMahout.class );

	@Test
	@Ignore
	public void getResearcherPublication()
	{
		String authorId = "07397ed7-3deb-442f-a297-bdb5b476d3e6";

		Author author = persistenceStrategy.getAuthorDAO().getById( authorId );

		// now get by year, basically, you can get all of publications from this
		// author and just filter it based on year
		List<Publication> publications = new ArrayList<Publication>();

		for ( int i = 2005; i < 2016; i++ )
		{
			System.out.println( i );
			int count = 0;
			for ( Publication publication : author.getPublications() )
			{

				if ( !publication.getYear().equals( i + "" ) )
					continue;
				
				publications.add( publication );
				System.out.println( publication.getTitle() );
				System.out.println( publication.getAbstractText() );
				count++;
			}
			System.out.println( count );
		}


	}

	@Test
	@Ignore
	public void testGetDataFromDatabase() throws FileNotFoundException, UnsupportedEncodingException
	{
<<<<<<< HEAD
		System.out.println( "\n========== TEST 1 - Fetch data from database ==========" );

		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();
		int count = 0;
=======
		System.out.println( "\n========== TEST 0 - Fetch All Authors from database ==========" );

		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();
		//Author authors = persistenceStrategy.getAuthorDAO().getById("");
		
>>>>>>> feature-topic-modelling
		if ( authors != null )
			for ( Author author : authors )
			{
				PrintWriter writer = new PrintWriter(path + "Authors/Authors/" + author.getId() + ".txt", "UTF-8" );

				if ( author.getPublications() != null )
				{
					for ( Publication publication : author.getPublications() )
					{
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
<<<<<<< HEAD
	public void testGetDatabaseFromDatabase() throws FileNotFoundException, UnsupportedEncodingException
	{
		int count = 0;
		System.out.println( "\n========== TEST 1 - Fetch publications per author from database ==========" );
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();//getByName( "mohamed amine chatti" );//getById( "e14fd198-1e54-449f-96aa-7e19d0eec488" );
	
=======
	public void testcreateAuthorDirectories() throws IOException
	{
		System.out.println( "\n========== TEST 1 - Create Architecture for the Author-Test Collection ==========" );
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();
		if ( !authors.isEmpty() )
			for ( Author author : authors )
			{

				File theDir = new File( path + "Author-Test/" + author.getId().toString() );

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
	public void testGetDatabaseFromDatabase() throws FileNotFoundException, UnsupportedEncodingException
	{
		System.out.println( "\n========== TEST 2 - Fetch publications for each Author-Test from database ==========" );
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();
		// Author authors = persistenceStrategy.getAuthorDAO().getById("");
		
>>>>>>> feature-topic-modelling
		if( !authors.isEmpty())
			for (Author author:authors)
			{	
				PrintWriter writer = new PrintWriter( "C:/Users/Piro/Desktop/Authors/Authors/" + author.getId() + ".txt", "UTF-8" );
				// writer.println( "Author Name : " + author.getName());
				for(Publication publication : author.getPublications()){
<<<<<<< HEAD
					if (publication.getAbstractText() != null){
						writer.println( publication.getTitle());
						writer.println(publication.getAbstractText());
=======
					if ( publication.getAbstractText() != "null" )
					{
						PrintWriter writer = new PrintWriter( path + "Author-Test/" + author.getId() + "/" + publication.getId() + ".txt", "UTF-8" );
						writer.print( publication.getTitle() + " " );
						writer.print( publication.getAbstractText() );
>>>>>>> feature-topic-modelling
						writer.println();
						count ++;
					}
				}
				writer.println();
				writer.println( count );
				count = 0;
				writer.close();
			}
	}
	
<<<<<<< HEAD
=======
	@Test
	@Ignore
	public void testcreateAuthorDirectoriesYearly() throws IOException
	{
		System.out.println( "\n========== TEST 3 - Create Architecture for the Author-Year-Test Collection Yearly ==========" );
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();
		
		if ( !authors.isEmpty() )
			for ( Author author : authors )
			{

				File theDir = new File( path + "Author-Year-Test/" + author.getId().toString() );

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
>>>>>>> feature-topic-modelling
	
	
	
	@Test
	public void testGetDatabaseFromDatabaseOnSpecificYear() throws IOException
	{
<<<<<<< HEAD
		System.out.println( "\n========== TEST 3 - Fetch publications per author Yearly from database ==========" );
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();//getByName( "mohamed amine chatti" );//getById( "e14fd198-1e54-449f-96aa-7e19d0eec488" );
		if( !authors.isEmpty())
			for (Author author:authors)
			{	
				for ( int year = 1900; year < 2017; year++ )
=======
		System.out.println( "\n========== TEST 4 - Fetch publications per Author-Year-Test from database ==========" );
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();
		
		 if ( !authors.isEmpty() )
			for ( Author author : authors )
			{
				for ( int year = 1980; year < 2017; year++ )
>>>>>>> feature-topic-modelling
				{
					System.out.println( year );
					for ( Publication publication : author.getPublicationsByYear( year ) )
					{
<<<<<<< HEAD

						System.out.println( publication.getTitle() );
						System.out.println( publication.getAbstractText() );
						System.out.println();
					}
				}
			}
			}

	
	@Test
	@Ignore
	public void testGetDatabaseFromDatabase2() throws IOException
	{

		System.out.println( "\n========== TEST 2 - Fetch publications from database ==========" );
		int count =0;
		//new FileWriter(log, true)
		
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();//getByName( "mohamed amine chatti" );//getById( "e14fd198-1e54-449f-96aa-7e19d0eec488" );
		if( !authors.isEmpty())
			for (Author author:authors)
			{	
				for(Publication publication : author.getPublications()){
	
					if (publication.getKeywords()!= null){
						PrintWriter pub = new PrintWriter( "C:/Users/Piro/Desktop/Publications/Publications/" + publication.getId() + ".txt", "UTF-8" );
						// pub.print(count + "\t");
						pub.print( publication.getKeywordText() + "\t" );
						pub.print(publication.getTitle() + " " + publication.getAbstractText());
						pub.println();
						// count++;
						// (System.out.println(count);
						pub.close();
					}	
					
				}

			}
		
	}
	
=======
						PrintWriter writer = new PrintWriter( path + "Author-Year-Test/" + author.getId().toString() + "/" + year + ".txt" );
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
>>>>>>> feature-topic-modelling
}

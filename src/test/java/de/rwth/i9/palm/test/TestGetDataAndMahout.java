package de.rwth.i9.palm.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

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

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.config.DatabaseConfigCoreTest;
import de.rwth.i9.palm.config.WebAppConfigTest;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorInterest;
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
	@Ignore
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

	public void testGetDatabaseFromDatabase() throws FileNotFoundException, UnsupportedEncodingException
	{
		int count = 0;
		System.out.println( "\n========== TEST 1 - Fetch publications per author from database ==========" );
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();//getByName( "mohamed amine chatti" );//getById( "e14fd198-1e54-449f-96aa-7e19d0eec488" );
	
		if( !authors.isEmpty())
			for (Author author:authors)
			{	
				PrintWriter writer = new PrintWriter("C:/Users/Piro/Desktop/Authors/Authors/" + author.getId() +".txt", "UTF-8");
				writer.println( "Author Name : " + author.getName());
				for(Publication publication : author.getPublications()){
					writer.println( publication.getTitle());
					writer.println(publication.getAbstractText());
					writer.println();
					count ++;
				}
				writer.println();
				writer.println( count );
				count =0;
				writer.close();
			}
	}
	
	@Test
	@Ignore
	public void testGetDatabaseFromDatabaseOnSpecificYear() throws IOException
	{
		System.out.println( "\n========== TEST 3 - Fetch publications per author Yearly from database ==========" );
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();//getByName( "mohamed amine chatti" );//getById( "e14fd198-1e54-449f-96aa-7e19d0eec488" );
		if( !authors.isEmpty())
			for (Author author:authors)
			{	
			for( int year = 1900 ; year < 2016 ; year ++)
				for(Publication publication : author.getPublicationsByYear(year )){
					System.out.println(publication.getTitle());
					System.out.println(publication.getAbstractText());
					System.out.println();
//					PrintWriter writer = new PrintWriter(new BufferedWriter( new FileWriter("C:/Users/Piro/Desktop/Years/Years/" +year +".txt", true)));
//					writer.println(publication.getTitle());
//					writer.println(publication.getAbstractText());
//					writer.println();
//					writer.close();
					}
				}
			}

	
	@Test
	@Ignore
	public void testGetDatabaseFromDatabase2() throws FileNotFoundException, UnsupportedEncodingException
	{

		System.out.println( "\n========== TEST 2 - Fetch publications from database ==========" );
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();//getByName( "mohamed amine chatti" );//getById( "e14fd198-1e54-449f-96aa-7e19d0eec488" );
		if( !authors.isEmpty())
			for (Author author:authors)
			{	
				for(Publication publication : author.getPublications()){
					if (publication.getAbstractText()!= null){
					PrintWriter pub = new PrintWriter("C:/Users/Piro/Desktop/Publications/Publications" + publication.getId() +".txt", "UTF-8");
					pub.println( publication.getTitle());
					pub.println( publication.getAbstractText());
					pub.println();
					pub.close();
					}
				}

			}
		
	}
	
}

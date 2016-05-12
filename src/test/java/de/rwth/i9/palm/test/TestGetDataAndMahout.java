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
	@Ignore
	public void testGetDatabaseFromDatabase() throws FileNotFoundException, UnsupportedEncodingException
	{
		int count = 0;
		System.out.println( "\n========== TEST 1 - Fetch publications per author from database ==========" );
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();//getByName( "mohamed amine chatti" );//getById( "e14fd198-1e54-449f-96aa-7e19d0eec488" );
	
		if( !authors.isEmpty())
			for (Author author:authors)
			{	
				PrintWriter writer = new PrintWriter( "C:/Users/Piro/Desktop/Authors/Authors/" + author.getId() + ".txt", "UTF-8" );
				// writer.println( "Author Name : " + author.getName());
				for(Publication publication : author.getPublications()){
					if (publication.getAbstractText() != null){
						writer.println( publication.getTitle());
						writer.println(publication.getAbstractText());
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
	
	
	@Test
	@Ignore
	public void testGetDatabaseFromDatabaseOnSpecificYear() throws IOException
	{
		System.out.println( "\n========== TEST 3 - Fetch publications per author Yearly from database ==========" );
		List<Author> authors = persistenceStrategy.getAuthorDAO().getAll();//getByName( "mohamed amine chatti" );//getById( "e14fd198-1e54-449f-96aa-7e19d0eec488" );
		if( !authors.isEmpty())
			for (Author author:authors)
			{	
				for ( int year = 1900; year < 2017; year++ )
				for(Publication publication : author.getPublicationsByYear(year )){
					System.out.println(publication.getTitle());
					System.out.println(publication.getAbstractText());
					System.out.println();
					}
				}
			}

	
	@Test

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
	
}

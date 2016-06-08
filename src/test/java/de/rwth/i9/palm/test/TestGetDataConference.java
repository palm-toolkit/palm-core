package de.rwth.i9.palm.test;

import java.io.FileNotFoundException;
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
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( classes = { WebAppConfigTest.class, DatabaseConfigCoreTest.class }, loader = AnnotationConfigContextLoader.class )
@TransactionConfiguration
@Transactional

public class TestGetDataConference extends AbstractTransactionalJUnit4SpringContextTests
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Test
	public void testGetEventPublicationsFromDatabase() throws FileNotFoundException, UnsupportedEncodingException
	{
		// int count = 0;
		System.out.println( "\n========== TEST 1 - Get Event publication ==========" );
		// Event event = persistenceStrategy.getEventDAO().getById(
		// "e61d08f9-afd4-4600-9c16-78a62cdfbee0" );

		List<Event> events = persistenceStrategy.getEventDAO().getAll();

		if ( !events.isEmpty() )
			for ( Event event : events )

		{
				PrintWriter writer = new PrintWriter( "C:/Users/Piro/Desktop/Conferences/Conferences/" + event.getId() + ".txt", "UTF-8" );
				// writer.println( "Event Name : " + event.getName() );
				for ( Publication publication : event.getPublications() )
			{
					if ( publication.getAbstractText() != null )
					{
						writer.println( publication.getTitle() );
						writer.println( publication.getAbstractText() );
						writer.println();
						// count++;
					}
			}
				writer.close();
		}
	}

	@Test
	@Ignore
	public void testGetCirclePublicationsFromDatabaseYearly() throws FileNotFoundException, UnsupportedEncodingException
	{
		int count = 0;
		System.out.println( "\n========== TEST 1 - Get Event publication ==========" );
		// Event event = persistenceStrategy.getEventDAO().getById(
		// "e61d08f9-afd4-4600-9c16-78a62cdfbee0" );

		List<Event> events = persistenceStrategy.getEventDAO().getAll();

		if ( !events.isEmpty() )
			for ( Event event : events )

			{
				for ( int year = 1980; year < 2017; year++ )
				{
					PrintWriter writer = new PrintWriter( "C:/Users/Piro/Desktop/Circles/Circles/" + event.getId() + ".txt", "UTF-8" );
					writer.println( "Event Name : " + event.getName() );
					for ( Publication publication : event.getPublications() )
					{
						if ( publication.getAbstractText() != null )
						{
							writer.println( publication.getTitle() );
							writer.println( publication.getAbstractText() );
							writer.println();
							count++;
						}
					}

				writer.println();
				writer.println( count );
				count = 0;
				writer.close();
				}
			}
	}
}
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
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
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
	@Ignore
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
	public void testcreateEntityDirectories() throws IOException
	{
		System.out.println( "\n========== TEST 2 - Create Architecture for the Data Collection ==========" );
		List<EventGroup> eventgroups = persistenceStrategy.getEventGroupDAO().getAll();// getByName(
																			// "mohamed
																			// amine
																			// chatti"
																			// );//getById(
																			// "e14fd198-1e54-449f-96aa-7e19d0eec488"
																			// );
		if ( !eventgroups.isEmpty() )
			for ( EventGroup event : eventgroups )
			{
				for ( Event eventof : event.getEvents() )
				{

					File theDir = new File( "C:/Users/Piro/Desktop/TEST/" + event.getId().toString() + "/" + eventof.getId().toString() );

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

	@Test
	@Ignore
	public void testGetDatabaseFromDatabase() throws FileNotFoundException, UnsupportedEncodingException
	{
		System.out.println( "\n========== TEST 1 - Fetch publications per Event from database ==========" );
		List<Event> events = persistenceStrategy.getEventDAO().getAll();// getByName(
																			// "mohamed
																			// amine
																			// chatti"
																			// );//getById(
																			// "e14fd198-1e54-449f-96aa-7e19d0eec488"
																			// );

		if ( !events.isEmpty() )
			for ( Event event : events )
			{
				System.out.println( event.getName() );
				for ( Publication publication : event.getPublications() )
				{
					if ( publication.getAbstractText() != "null" )
					{
						PrintWriter writer = new PrintWriter( "C:/Users/Piro/Desktop/Event-Test/" + event.getId() + "/" + publication.getId() + ".txt", "UTF-8" );
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
	public void testGetDatabaseFromDatabaseGroupEvents() throws FileNotFoundException, UnsupportedEncodingException
	{
		System.out.println( "\n========== TEST 1 - Fetch publications per Event from database ==========" );
		List<EventGroup> events = persistenceStrategy.getEventGroupDAO().getAll();// getByName(
																		// "mohamed
																		// amine
																		// chatti"
																		// );//getById(
																		// "e14fd198-1e54-449f-96aa-7e19d0eec488"
																		// );
		if ( !events.isEmpty() ){
			for ( EventGroup group : events )
			{
				System.out.println( group.getName() );
				for ( Event event : group.getEvents() )
				{

					System.out.println( event.getName() );
					for ( Publication publication : event.getPublications() )
					{
						if ( publication.getAbstractText() != "null" )
						{
							PrintWriter writer = new PrintWriter( "C:/Users/Piro/Desktop/TEST/" + group.getId() + "/" + event.getId().toString() + "/" + publication.getId() + ".txt", "UTF-8" );
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
	}
	}

}
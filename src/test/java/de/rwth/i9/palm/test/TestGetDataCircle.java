package de.rwth.i9.palm.test;

import java.io.FileNotFoundException;
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

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.config.DatabaseConfigCoreTest;
import de.rwth.i9.palm.config.WebAppConfigTest;
import de.rwth.i9.palm.feature.researcher.ResearcherFeature;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( classes = { WebAppConfigTest.class, DatabaseConfigCoreTest.class }, loader = AnnotationConfigContextLoader.class )
@TransactionConfiguration
@Transactional

public class TestGetDataCircle extends AbstractTransactionalJUnit4SpringContextTests
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired // actually this one is for the API, so I guess you don't need to
				// use this
	private ResearcherFeature researcherFeature;

	@Autowired
	private PalmAnalytics palmAnalytics;

	private final static Logger log = LoggerFactory.getLogger( TestGetDataCircle.class );


	@Test
	@Ignore
	public void testGetCirclePublicationsFromDatabase() throws FileNotFoundException, UnsupportedEncodingException
	{
		int count = 0;
		System.out.println( "\n========== TEST 1 - Get Circle publication ==========" );
		// Circle circle = persistenceStrategy.getCircleDAO().getById(
		// "e61d08f9-afd4-4600-9c16-78a62cdfbee0" );

		List<Circle> circles = persistenceStrategy.getCircleDAO().getAll();

		if ( !circles.isEmpty() )
			for ( Circle circle : circles )

		{
				PrintWriter writer = new PrintWriter( "C:/Users/Piro/Desktop/Circles/Circles/" + circle.getId() + ".txt", "UTF-8" );
				writer.println( "Circle Name : " + circle.getName() );
				for ( Publication publication : circle.getPublications() )
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

	@Test
	public void testGetCirclePublicationsFromDatabaseYearly() throws FileNotFoundException, UnsupportedEncodingException
	{
		int count = 0;
		System.out.println( "\n========== TEST 1 - Get Circle publication ==========" );
		// Circle circle = persistenceStrategy.getCircleDAO().getById(
		// "e61d08f9-afd4-4600-9c16-78a62cdfbee0" );

		List<Circle> circles = persistenceStrategy.getCircleDAO().getAll();

		if ( !circles.isEmpty() )
			for ( Circle circle : circles )

			{
				for ( int year = 1980; year < 2017; year++ )
				{
					PrintWriter writer = new PrintWriter( "C:/Users/Piro/Desktop/Circles/Circles/" + circle.getId() + ".txt", "UTF-8" );
					writer.println( "Circle Name : " + circle.getName() );
					for ( Publication publication : circle.getPublications() )
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
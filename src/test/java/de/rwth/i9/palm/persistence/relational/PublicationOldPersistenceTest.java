package de.rwth.i9.palm.persistence.relational;

import static org.junit.Assert.assertNotNull;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

import de.rwth.i9.palm.config.DatabaseConfigTest;
import de.rwth.i9.palm.model.PublicationOld;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.persistence.PublicationOldDAO;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( classes = DatabaseConfigTest.class, loader = AnnotationConfigContextLoader.class )
@Transactional
public class PublicationOldPersistenceTest
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	private PublicationOldDAO publicationOldDAO;

	@Before
	public void init()
	{
		publicationOldDAO = persistenceStrategy.getPublicationOldDAO();
		assertNotNull( publicationOldDAO );
	}

	@Test
	public void test()
	{
		List<PublicationOld> pubOlds = persistenceStrategy.getPublicationOldDAO().getAll();

		for ( PublicationOld pubOld : pubOlds )
		{
			String s = pubOld.getAuthors();
			try
			{
				byte[] bytes = s.getBytes( "UTF-8" );
				System.out.println( new String( bytes, "UTF-8" ) );
			}
			catch ( UnsupportedEncodingException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}

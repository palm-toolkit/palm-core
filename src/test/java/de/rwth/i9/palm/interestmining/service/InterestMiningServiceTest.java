package de.rwth.i9.palm.interestmining.service;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

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
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( classes = { WebAppConfigTest.class, DatabaseConfigCoreTest.class }, loader = AnnotationConfigContextLoader.class )
@TransactionConfiguration
@Transactional
public class InterestMiningServiceTest extends AbstractTransactionalJUnit4SpringContextTests
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private InterestMiningService interestMiningService;

	@Autowired
	private PalmAnalytics palmAnalytics;

	final Logger logger = Logger.getLogger( InterestMiningServiceTest.class );

	@Test
	@Ignore
	public void testInterestMining() throws ParseException
	{

		List<Author> authors = persistenceStrategy.getAuthorDAO().getByName( "mohamed amine chatti" );

		if ( authors != null )
		{
			Author author = authors.get( 0 );
			Map<String, Object> result = interestMiningService.getInterestFromAuthor( author, true, null );
		}
	}
}

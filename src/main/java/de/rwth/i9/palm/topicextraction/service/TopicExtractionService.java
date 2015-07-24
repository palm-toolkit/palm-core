package de.rwth.i9.palm.topicextraction.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.ExtractionService;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationTopic;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Service
public class TopicExtractionService
{
	private final static Logger log = LoggerFactory.getLogger( TopicExtractionService.class );

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private AsynchronousTopicExtractionService asynchronousTopicExtractionService;

	public void extractTopicFromPublicationByAuthor( Author author ) throws InterruptedException
	{
		List<Future<PublicationTopic>> publicationTopicFutureList = new ArrayList<Future<PublicationTopic>>();
		List<ExtractionService> extractionServices = persistenceStrategy.getExtractionServiceDAO().getAllActiveExtractionService();
		
		// get current date
		Calendar calendar = Calendar.getInstance();

		// loop through available extraction services
		for ( ExtractionService extractionService : extractionServices )
		{
			// check extraction service
			if ( extractionService.getLastQueryDate() != null )
			{
				if ( extractionService.getLastQueryDate().equals( calendar.getTime() ) )
				{
					extractionService.setCountQueryThisDay( extractionService.getCountQueryThisDay() + author.getPublications().size() );
					persistenceStrategy.getExtractionServiceDAO().persist( extractionService );
				}
				else
				{
					extractionService.setLastQueryDate( calendar.getTime() );
					extractionService.setCountQueryThisDay( author.getPublications().size() );
					persistenceStrategy.getExtractionServiceDAO().persist( extractionService );
				}
			}
			else
			{
				extractionService.setLastQueryDate( calendar.getTime() );
				extractionService.setCountQueryThisDay( author.getPublications().size() );
				persistenceStrategy.getExtractionServiceDAO().persist( extractionService );
			}

			// if beyond limitation query perday
			if ( extractionService.getCountQueryThisDay() > extractionService.getMaxQueryPerDay() )
				continue;
			
			// publications on specific user
			for ( Publication publication : author.getPublications() )
			{
				// at least have an abstract
				if ( publication.isContentUpdated() && publication.getAbstractText() != null )
				{
					// // remove old extracted source
					// if ( publication.getPublicationTopics() != null )
					// publication.removeAllPublicationTopic();

					// create new publication topic
					PublicationTopic publicationTopic = new PublicationTopic();
					publicationTopic.setExtractionServiceType( extractionService.getExtractionServiceType() );
					publicationTopic.setExtractionDate( calendar.getTime() );
					publicationTopic.setPublication( publication );

					publication.addPublicationTopic( publicationTopic );

					publicationTopicFutureList.add( asynchronousTopicExtractionService.getTopicsByAlchemyApi( publication, publicationTopic, extractionService.getMaxTextLength() ) );
				}
				
			}

		}
		// check whether thread worker is done
		// Wait until they are all done
		boolean processIsDone = true;
		do
		{
			processIsDone = true;
			for ( Future<PublicationTopic> futureList : publicationTopicFutureList )
			{
				if ( !futureList.isDone() )
				{
					processIsDone = false;
					break;
				}
			}
			// 10-millisecond pause between each check
			Thread.sleep( 10 );
		} while ( !processIsDone );

		// save publications, set flag, prevent re-extract publication topic
		for ( Publication publication : author.getPublications() )
		{
			publication.setContentUpdated( false );
			persistenceStrategy.getPublicationDAO().persist( publication );
		}
	}
}

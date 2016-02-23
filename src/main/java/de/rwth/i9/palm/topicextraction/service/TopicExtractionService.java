package de.rwth.i9.palm.topicextraction.service;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.ExtractionService;
import de.rwth.i9.palm.model.ExtractionServiceType;
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

	public void extractTopicFromPublicationByAuthor( Author author ) throws InterruptedException, UnsupportedEncodingException, URISyntaxException, ExecutionException
	{
		List<Future<PublicationTopic>> publicationTopicFutureList = new ArrayList<Future<PublicationTopic>>();
		List<ExtractionService> extractionServices = persistenceStrategy.getExtractionServiceDAO().getAllActiveExtractionService();

		// get current date
		Calendar calendar = Calendar.getInstance();

		boolean topicExtractionUpdated = false;

		// publications on specific user
		for ( Publication publication : author.getPublications() )
		{
			if ( publication.getAbstractText() == null )
				continue;

			// at least have an abstract
			if ( publication.isContentUpdated() )
			{
				// loop through available extraction services
				for ( ExtractionService extractionService : extractionServices )
				{
					if ( !extractionService.isActive() )
						continue;

					// count number of service being used
					countExtractionServiceUsages( extractionService, author, calendar );

					// if beyond limitation query perday
					if ( extractionService.getCountQueryThisDay() > extractionService.getMaxQueryPerDay() )
						continue;
					// // remove old extracted source
					if ( publication.getPublicationTopics() != null && !publication.getPublicationTopics().isEmpty() )
						publication.removeAllPublicationTopic();

					// create new publication topic
					PublicationTopic publicationTopic = new PublicationTopic();
					publicationTopic.setExtractionServiceType( extractionService.getExtractionServiceType() );
					publicationTopic.setExtractionDate( calendar.getTime() );
					publicationTopic.setPublication( publication );

					publication.addPublicationTopic( publicationTopic );

					if ( extractionService.getExtractionServiceType().equals( ExtractionServiceType.ALCHEMY ) )
						publicationTopicFutureList.add( asynchronousTopicExtractionService.getTopicsByAlchemyApi( publication, publicationTopic, extractionService.getMaxTextLength() ) );
					else if ( extractionService.getExtractionServiceType().equals( ExtractionServiceType.YAHOOCONTENTANALYSIS ) )
						publicationTopicFutureList.add( asynchronousTopicExtractionService.getTopicsByYahooContentAnalysis( publication, publicationTopic, extractionService.getMaxTextLength() ) );
					else if ( extractionService.getExtractionServiceType().equals( ExtractionServiceType.FIVEFILTERS ) )
						publicationTopicFutureList.add( asynchronousTopicExtractionService.getTopicsByFiveFilters( publication, publicationTopic, extractionService.getMaxTextLength() ) );
				}
			}
			else
			{
				// loop through available extraction services
				for ( ExtractionService extractionService : extractionServices )
				{
					if ( !extractionService.isActive() )
						continue;

					// count number of service being used
					countExtractionServiceUsages( extractionService, author, calendar );

					// if beyond limitation query perday
					if ( extractionService.getCountQueryThisDay() > extractionService.getMaxQueryPerDay() )
						continue;

					PublicationTopic publicationTopic = null;

					// check if ever extracted before
					for ( PublicationTopic publicationTopicEach : publication.getPublicationTopics() )
					{
						if ( publicationTopicEach.getExtractionServiceType().equals( extractionService.getExtractionServiceType() ) )
						{
							publicationTopic = publicationTopicEach;
						}
					}

					if ( publicationTopic == null )
					{
						publicationTopic = new PublicationTopic();
						publicationTopic.setExtractionServiceType( extractionService.getExtractionServiceType() );
						publicationTopic.setExtractionDate( calendar.getTime() );
						publicationTopic.setPublication( publication );

						publication.addPublicationTopic( publicationTopic );
					}
					if ( publicationTopic.getTermValues() == null || publicationTopic.getTermValues().isEmpty() )
					{
						if ( extractionService.getExtractionServiceType().equals( ExtractionServiceType.ALCHEMY ) )
							publicationTopicFutureList.add( asynchronousTopicExtractionService.getTopicsByAlchemyApi( publication, publicationTopic, extractionService.getMaxTextLength() ) );
						else if ( extractionService.getExtractionServiceType().equals( ExtractionServiceType.YAHOOCONTENTANALYSIS ) )
							publicationTopicFutureList.add( asynchronousTopicExtractionService.getTopicsByYahooContentAnalysis( publication, publicationTopic, extractionService.getMaxTextLength() ) );
						else if ( extractionService.getExtractionServiceType().equals( ExtractionServiceType.FIVEFILTERS ) )
							publicationTopicFutureList.add( asynchronousTopicExtractionService.getTopicsByFiveFilters( publication, publicationTopic, extractionService.getMaxTextLength() ) );
					}
				}

			}

		}

		// Wait until they are all done
		for ( Future<PublicationTopic> futureList : publicationTopicFutureList )
		{
			futureList.get();
		}
		// save publications, set flag, prevent re-extract publication topic
		for ( Publication publication : author.getPublications() )
		{
			publication.setContentUpdated( false );
//			System.out.println( publication.getId() + " > " + publication.getTitle() );
//			Set<PublicationTopic> topics = publication.getPublicationTopics();
//
//			for ( PublicationTopic topic : topics )
//			{
//				if ( topic.getExtractionServiceType().equals( ExtractionServiceType.ALCHEMYAPI ) )
//					if ( topic.getTermValues() != null )
//					{
//					System.out.println( topic.getExtractionServiceType().toString() + " : " );
//						System.out.print( topic.getId() + " > " );
//						for ( Entry<String, Double> termValue : topic.getTermValues().entrySet() )
//							System.out.print( termValue.getKey() + " : " + termValue.getValue() + " | " );
//					System.out.println();
//					}
//			}
//			System.out.println();

			persistenceStrategy.getPublicationDAO().persist( publication );
		}
	}
	
	
	public void extractTopicFromPublicationByCircle( Circle circle ) throws InterruptedException, UnsupportedEncodingException, URISyntaxException
	{
		List<Future<PublicationTopic>> publicationTopicFutureList = new ArrayList<Future<PublicationTopic>>();
		List<ExtractionService> extractionServices = persistenceStrategy.getExtractionServiceDAO().getAllActiveExtractionService();
		
		// get current date
		Calendar calendar = Calendar.getInstance();

		boolean topicExtractionUpdated = false;

		// loop through available extraction services
		for ( ExtractionService extractionService : extractionServices )
		{
			if ( !extractionService.isActive() )
				continue;

			// check extraction service limitation (number of queries per day)
			// TODO this is still not correct
			if ( extractionService.getLastQueryDate() != null )
			{
				if ( extractionService.getLastQueryDate().equals( calendar.getTime() ) )
				{
					extractionService.setCountQueryThisDay( extractionService.getCountQueryThisDay() + circle.getPublications().size() );
					persistenceStrategy.getExtractionServiceDAO().persist( extractionService );
				}
				else
				{
					extractionService.setLastQueryDate( calendar.getTime() );
					extractionService.setCountQueryThisDay( circle.getPublications().size() );
					persistenceStrategy.getExtractionServiceDAO().persist( extractionService );
				}
			}
			else
			{
				extractionService.setLastQueryDate( calendar.getTime() );
				extractionService.setCountQueryThisDay( circle.getPublications().size() );
				persistenceStrategy.getExtractionServiceDAO().persist( extractionService );
			}

			// if beyond limitation query perday
			if ( extractionService.getCountQueryThisDay() > extractionService.getMaxQueryPerDay() )
				continue;
			
			// publications on specific user
			for ( Publication publication : circle.getPublications() )
			{
				if ( publication.getAbstractText() == null )
					continue;

				// at least have an abstract
				if ( publication.isContentUpdated() )
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

					if ( extractionService.getExtractionServiceType().equals( ExtractionServiceType.ALCHEMY ) )
						publicationTopicFutureList.add( asynchronousTopicExtractionService.getTopicsByAlchemyApi( publication, publicationTopic, extractionService.getMaxTextLength() ) );
					else if ( extractionService.getExtractionServiceType().equals( ExtractionServiceType.YAHOOCONTENTANALYSIS ) )
						publicationTopicFutureList.add( asynchronousTopicExtractionService.getTopicsByYahooContentAnalysis( publication, publicationTopic, extractionService.getMaxTextLength() ) );
					else if ( extractionService.getExtractionServiceType().equals( ExtractionServiceType.FIVEFILTERS ) )
						publicationTopicFutureList.add( asynchronousTopicExtractionService.getTopicsByFiveFilters( publication, publicationTopic, extractionService.getMaxTextLength() ) );
				}
				else
				{
					// if something fails on last run
					PublicationTopic publicationTopic = null;
					for ( PublicationTopic publicationTopicEach : publication.getPublicationTopics() )
					{
						if ( publicationTopicEach.getExtractionServiceType().equals( extractionService.getExtractionServiceType() ) )
						{
							publicationTopic = publicationTopicEach;
						}
					}

					if ( publicationTopic == null )
					{
						publicationTopic = new PublicationTopic();
						publicationTopic.setExtractionServiceType( extractionService.getExtractionServiceType() );
						publicationTopic.setExtractionDate( calendar.getTime() );
						publicationTopic.setPublication( publication );

						publication.addPublicationTopic( publicationTopic );
					}
					if ( publicationTopic.getTermValues() == null || publicationTopic.getTermValues().isEmpty() )
					{
						if ( extractionService.getExtractionServiceType().equals( ExtractionServiceType.ALCHEMY ) )
							publicationTopicFutureList.add( asynchronousTopicExtractionService.getTopicsByAlchemyApi( publication, publicationTopic, extractionService.getMaxTextLength() ) );
						else if ( extractionService.getExtractionServiceType().equals( ExtractionServiceType.YAHOOCONTENTANALYSIS ) )
							publicationTopicFutureList.add( asynchronousTopicExtractionService.getTopicsByYahooContentAnalysis( publication, publicationTopic, extractionService.getMaxTextLength() ) );
						else if ( extractionService.getExtractionServiceType().equals( ExtractionServiceType.FIVEFILTERS ) )
							publicationTopicFutureList.add( asynchronousTopicExtractionService.getTopicsByFiveFilters( publication, publicationTopic, extractionService.getMaxTextLength() ) );
					}

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
		for ( Publication publication : circle.getPublications() )
		{
			publication.setContentUpdated( false );
//			System.out.println( publication.getId() + " > " + publication.getTitle() );
//			Set<PublicationTopic> topics = publication.getPublicationTopics();
//
//			for ( PublicationTopic topic : topics )
//			{
//				if ( topic.getExtractionServiceType().equals( ExtractionServiceType.ALCHEMYAPI ) )
//					if ( topic.getTermValues() != null )
//					{
//					System.out.println( topic.getExtractionServiceType().toString() + " : " );
//						System.out.print( topic.getId() + " > " );
//						for ( Entry<String, Double> termValue : topic.getTermValues().entrySet() )
//							System.out.print( termValue.getKey() + " : " + termValue.getValue() + " | " );
//					System.out.println();
//					}
//			}
//			System.out.println();

			persistenceStrategy.getPublicationDAO().persist( publication );
		}
	}

	private void countExtractionServiceUsages( ExtractionService extractionService, Author author, Calendar calendar )
	{

		// check extraction service limitation (number of queries per day)
		// TODO this is still not correct
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
	}

}

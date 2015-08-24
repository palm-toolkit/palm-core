package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.model.FileType;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationFile;
import de.rwth.i9.palm.model.PublicationSource;
import de.rwth.i9.palm.model.PublicationType;
import de.rwth.i9.palm.model.SourceMethod;
import de.rwth.i9.palm.model.SourceType;

/**
 * 
 * @author sigit
 *
 */
@Service
public class AsynchronousPublicationDetailCollectionService
{
	private final static Logger log = LoggerFactory.getLogger( AsynchronousPublicationDetailCollectionService.class );

	@Autowired
	private AsynchronousCollectionService asynchronousCollectionService;

	@Autowired
	private PalmAnalytics palmAnalitics;

	/**
	 * Collect publication detail from a publication from multiple source
	 * 
	 * @param publication
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Async
	public Future<Publication> asyncWalkOverSelectedPublication( Publication publication ) throws IOException, InterruptedException
	{
		// multithread publication source
		List<Future<PublicationSource>> publicationSourceFutureList = new ArrayList<Future<PublicationSource>>();

		for ( PublicationSource publicationSource : publication.getPublicationSources() )
		{
			// handling publication source
			if ( publicationSource.getSourceMethod().equals( SourceMethod.PARSEPAGE ) )
			{
				if ( publicationSource.getSourceType().equals( SourceType.GOOGLESCHOLAR ) )
					publicationSourceFutureList.add( asynchronousCollectionService.getPublicationInformationFromGoogleScholar( publicationSource ) );
				else if ( publicationSource.getSourceType().equals( SourceType.CITESEERX ) )
					publicationSourceFutureList.add( asynchronousCollectionService.getPublicationInformationFromCiteseerX( publicationSource ) );
			}
		}

		boolean walkingProcessIsDone = true;
		do
		{
			walkingProcessIsDone = true;
			for ( Future<PublicationSource> publicationSourceFuture : publicationSourceFutureList )
			{
				if ( publicationSourceFuture.isDone() )
				{
					walkingProcessIsDone = false;
					break;
				}
			}
			// 10-millisecond pause between each check
			Thread.sleep( 10 );
		} while ( !walkingProcessIsDone );

		return new AsyncResult<Publication>( publication );
	}
	
	/**
	 * Collect publication detail from a publication from multiple source
	 * 
	 * @param publication
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	@Async
	public Future<Publication> asyncEnrichPublicationInformationFromOriginalSource( Publication publication ) throws IOException, InterruptedException, ExecutionException, TimeoutException
	{
		log.info( "Start enrichment process for publication " + publication.getTitle() );
		// multithread publication source
		List<Future<PublicationSource>> publicationSourceFutureList = new ArrayList<Future<PublicationSource>>();

		if ( publication.getPublicationFiles() != null )
		{
			for ( PublicationFile publicationFile : publication.getPublicationFiles() )
			{
				if ( !publicationFile.isChecked() && publication.getPublicationType() != null )
				{

					if ( publicationFile.getFileType().equals( FileType.PDF ) && publication.getPublicationType().equals( PublicationType.CONFERENCE ) )
					{
						PublicationSource publicationSource = new PublicationSource();
						publicationSource.setSourceUrl( publicationFile.getUrl() );
						publicationSource.setSourceMethod( SourceMethod.EXTRACTPAGE );
						publicationSource.setSourceType( SourceType.PDF );

						publicationSourceFutureList.add( asynchronousCollectionService.getPublicationInformationFromPdf( publicationSource ) );
					}

					if ( publicationFile.getFileType().equals( FileType.HTML ) )
					{
						PublicationSource publicationSource = new PublicationSource();
						publicationSource.setSourceUrl( publicationFile.getUrl() );
						publicationSource.setSourceMethod( SourceMethod.EXTRACTPAGE );
						publicationSource.setSourceType( SourceType.HTML );

						publicationSourceFutureList.add( asynchronousCollectionService.getPublicationInfromationFromHtml( publicationSource ) );
					}
				}
			}
			
			// waiting until thread finished
			for ( Future<PublicationSource> publicationSourceFuture : publicationSourceFutureList )
			{
				try
				{
					PublicationSource publicationSource = publicationSourceFuture.get( 15, TimeUnit.SECONDS );
					if ( publicationSource.getAbstractText() != null )
					{
						if ( publicationSource.getSourceType().equals( SourceType.PDF ) )
						{
							if ( palmAnalitics.getTextCompare().getDistanceByLuceneLevenshteinDistance( publicationSource.getTitle().toLowerCase(), publication.getTitle().toLowerCase() ) > .9f )
							{
								publicationSource.setPublication( publication );
								publication.addPublicationSource( publicationSource );
							}
						}
						else
						{
							publicationSource.setPublication( publication );
							publication.addPublicationSource( publicationSource );
						}
					}
				}
				catch ( TimeoutException e )
				{
					log.error( e.getMessage() );
				}

			}

//			boolean enrichmentProcessIsDone = true;
//			do
//			{
//				enrichmentProcessIsDone = true;
//				for ( Future<PublicationSource> publicationSourceFuture : publicationSourceFutureList )
//				{
//					if ( publicationSourceFuture.isDone() )
//					{
//						enrichmentProcessIsDone = false;
//					}
//					//System.out.println( "ep st : " + publicationSourceFuture.isDone() + " -> " + publicationSourceFuture.get().getSourceType().toString() + " -> " + publicationSourceFuture.get().getSourceUrl() );
//				}
//				// 10-millisecond pause between each check
//				Thread.sleep( 10 );
//			} while ( !enrichmentProcessIsDone );
		}

//		log.info( "Done enrichment process for publication " + publication.getTitle() );

		return new AsyncResult<Publication>( publication );
	}

}

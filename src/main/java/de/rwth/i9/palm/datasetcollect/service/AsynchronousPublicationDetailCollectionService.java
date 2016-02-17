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
import de.rwth.i9.palm.model.CompletionStatus;
import de.rwth.i9.palm.model.FileType;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationFile;
import de.rwth.i9.palm.model.PublicationSource;
import de.rwth.i9.palm.model.PublicationType;
import de.rwth.i9.palm.model.SourceMethod;
import de.rwth.i9.palm.model.SourceType;
import de.rwth.i9.palm.service.ApplicationService;

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

	@Autowired
	private ApplicationService applicationService;

	/**
	 * Collect publication detail from a publication from multiple source
	 * 
	 * @param publication
	 * @param isPdfParsingEnable
	 * @param isHtmlParsingEnable
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	@Async
	public Future<Publication> asyncEnrichPublicationInformationFromOriginalSource( Publication publication, boolean isHtmlParsingEnable, boolean isPdfParsingEnable ) throws IOException, InterruptedException, ExecutionException, TimeoutException
	{
		// to make this process more efficient
		// prioritize the HTML extraction first over pdf
		log.info( "Start enrichment process for publication " + publication.getTitle() );
		// multithread publication source
		List<Future<PublicationSource>> publicationSourceFutureList = new ArrayList<Future<PublicationSource>>();
		// only extract one of the page
		boolean isOneSourceAlreadExtracted = false;

		if ( publication.getPublicationFiles() != null )
		{
			for ( PublicationFile publicationFile : publication.getPublicationFiles() )
			{
				if ( !publicationFile.isChecked() && publication.getPublicationType() != null )
				{

					if ( isOneSourceAlreadExtracted )
						break;

					if ( isPdfParsingEnable && publicationFile.getFileType().equals( FileType.PDF ) && publication.getPublicationType().equals( PublicationType.CONFERENCE ) )
					{
						PublicationSource publicationSource = new PublicationSource();
						publicationSource.setSourceUrl( publicationFile.getUrl() );
						publicationSource.setSourceMethod( SourceMethod.EXTRACTPAGE );
						publicationSource.setSourceType( SourceType.PDF );
						publicationSource.setPublicationType( publication.getPublicationType().toString() );

						isOneSourceAlreadExtracted = true;

						publicationFile.setChecked( true );

						publicationSourceFutureList.add( asynchronousCollectionService.getPublicationInformationFromPdf( publicationSource, publicationFile ) );
					}

					if ( isHtmlParsingEnable && publicationFile.getFileType().equals( FileType.HTML ) )
					{
						PublicationSource publicationSource = new PublicationSource();
						publicationSource.setSourceUrl( publicationFile.getUrl() );
						publicationSource.setSourceMethod( SourceMethod.EXTRACTPAGE );
						publicationSource.setSourceType( SourceType.HTML );
						publicationSource.setPublicationType( publication.getPublicationType().toString() );

						publicationFile.setChecked( true );

						isOneSourceAlreadExtracted = true;

						publicationSourceFutureList.add( asynchronousCollectionService.getPublicationInfromationFromHtml( publicationSource, publicationFile ) );
					}
				}
			}
			
			// waiting until thread finished
			for ( Future<PublicationSource> publicationSourceFuture : publicationSourceFutureList )
			{
				try
				{
					PublicationSource publicationSource = publicationSourceFuture.get( 35, TimeUnit.SECONDS );

					if ( publicationSource.getAbstractText() != null )
					{
						if ( publicationSource.getSourceType().equals( SourceType.PDF ) )
						{
							if ( publicationSource.getTitle() == null )
								continue;

							if ( palmAnalitics.getTextCompare().getDistanceByLuceneLevenshteinDistance( publicationSource.getTitle().toLowerCase(), publication.getTitle().toLowerCase() ) > .9f )
							{
								if ( !publication.getKeywordStatus().equals( CompletionStatus.COMPLETE ) && publicationSource.getKeyword() != null )
								{
									publication.setKeywordText( publicationSource.getKeyword() );
									publication.setKeywordStatus( CompletionStatus.PARTIALLY_COMPLETE );
								}
								if ( !publication.getAbstractStatus().equals( CompletionStatus.COMPLETE ) && publicationSource.getAbstractText() != null )
								{
									publication.setAbstractText( publicationSource.getAbstractText() );
									publication.setAbstractStatus( CompletionStatus.PARTIALLY_COMPLETE );
								}

								publicationSource.setPublication( publication );
								publication.addPublicationSource( publicationSource );
							}
						}
						else
						{
							if ( !publication.getKeywordStatus().equals( CompletionStatus.COMPLETE ) && publicationSource.getKeyword() != null )
							{
								publication.setKeywordText( publicationSource.getKeyword() );
								publication.setKeywordStatus( CompletionStatus.COMPLETE );
							}
							if ( !publication.getAbstractStatus().equals( CompletionStatus.COMPLETE ) && publicationSource.getAbstractText() != null )
							{
								publication.setAbstractText( publicationSource.getAbstractText() );
								publication.setAbstractStatus( CompletionStatus.COMPLETE );
							}
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

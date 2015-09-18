package de.rwth.i9.palm.pdfextraction.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Service
public class PdfExtractionService
{
	private final static Logger log = LoggerFactory.getLogger( PdfExtractionService.class );

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private AsynchronousPdfExtractionService asynchronousPdfExtractionService;

	@Autowired
	private PalmAnalytics palmAnalitics;

	/**
	 * Batch extraction of publications from pdf files on specific author
	 * 
	 * @param author
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void extractPublicationPdfFromSpecificAuthor( Author author ) throws IOException, InterruptedException, ExecutionException
	{
		// get publication that has pdf source
		List<Publication> publications = new ArrayList<Publication>();
		for ( Publication publication : author.getPublications() )
		{
			if ( publication.getPublicationFiles() != null )
				publications.add( publication );
		}

		Map<Publication, Future<List<TextSection>>> extractedPfdFutureMap = new LinkedHashMap<Publication, Future<List<TextSection>>>();

		for ( Publication publication : publications )
		{
			extractedPfdFutureMap.put( publication, this.asynchronousPdfExtractionService.extractPublicationPdfIntoTextSections( publication ) );
		}

		// check whether thread worker is done
		// Wait until they are all done
		// give 100-millisecond pause ( for downloading the pdf)
		Thread.sleep( 100 );
		boolean processIsDone = true;
		do
		{
			processIsDone = true;
			for ( Entry<Publication, Future<List<TextSection>>> futureMap : extractedPfdFutureMap.entrySet() )
			{
				if ( !futureMap.getValue().isDone() )
				{
					processIsDone = false;
					break;
				}
			}
			// 10-millisecond pause between each check
			Thread.sleep( 10 );
		} while ( !processIsDone );

		// process the extracted text sections further
		this.processExtractedPublication( extractedPfdFutureMap, null );
	}

	public void extractPdfFromSpecificPublication( Publication publication, Map<String, Object> responseMap ) throws IOException, InterruptedException, ExecutionException
	{

		Map<Publication, Future<List<TextSection>>> extractedPfdFutureMap = new LinkedHashMap<Publication, Future<List<TextSection>>>();
		extractedPfdFutureMap.put( publication, this.asynchronousPdfExtractionService.extractPublicationPdfIntoTextSections( publication ) );

		// check whether thread worker is done
		// Wait until they are all done
		// give 100-millisecond pause ( for downloading the pdf)
		Thread.sleep( 100 );
		boolean processIsDone = true;
		do
		{
			processIsDone = true;
			for ( Entry<Publication, Future<List<TextSection>>> futureMap : extractedPfdFutureMap.entrySet() )
			{
				if ( !futureMap.getValue().isDone() )
				{
					processIsDone = false;
					break;
				}
			}
			// 10-millisecond pause between each check
			Thread.sleep( 10 );
		} while ( !processIsDone );

		// process the extracted text sections further
		this.processExtractedPublication( extractedPfdFutureMap, responseMap );

	}

	/**
	 * Here the extracted text section will be 
	 * @param extractedPfdFutureMap
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void processExtractedPublication( Map<Publication, Future<List<TextSection>>> extractedPfdFutureMap, Map<String, Object> responseMap ) throws InterruptedException, ExecutionException
	{

		for ( Entry<Publication, Future<List<TextSection>>> extractedPfdFuture : extractedPfdFutureMap.entrySet() )
		{
			Publication publication = extractedPfdFuture.getKey();
			List<TextSection> textSections = extractedPfdFuture.getValue().get();
			StringBuilder publicationAbstract = new StringBuilder();
			StringBuilder publicationContent = new StringBuilder();
			StringBuilder publicationKeyword = new StringBuilder();
			StringBuilder contentSection = new StringBuilder();
			StringBuilder contentSectionWithName = new StringBuilder();

			for( TextSection textSection : textSections ){
				if ( textSection.getName() != null )
				{
					if ( textSection.getName().equals( "content" ) )
					{
						publicationContent.append( textSection.getContent() + "\n" );
						contentSectionWithName.append( textSection.getContent() + "\n" );
					}
					else if ( textSection.getName().equals( "content-header" ) )
					{
						publicationContent.append( "\t\n" + textSection.getContent() + "\n\t" );
						contentSection.setLength( 0 );
						contentSectionWithName.setLength( 0 );
						contentSectionWithName.append( "\t\n" + textSection.getContent() + "\n\t" );
					}
					else if ( textSection.getName().equals( "content-cont" ) )
					{
						publicationContent.setLength( publicationContent.length() - 1 );

						if ( publicationContent.toString().endsWith( "-" ) )
							publicationContent.setLength( publicationContent.length() - 1 );

						publicationContent.append( textSection.getContent() + "\n" );
						contentSectionWithName.append( textSection.getContent() + "\n" );
					}
					else if ( textSection.getName().equals( "keyword" ) )
					{
						publicationKeyword.append( textSection.getContent() + "\n" );
					}
					else if ( textSection.getName().equals( "abstract" ) )
					{
						publicationAbstract.append( textSection.getContent() + "\n" );
					}
					else if ( textSection.getName().equals( "abstract-header" ) )
					{
						if ( textSection.getContent().length() > 100 )
							publicationAbstract.append( textSection.getContent() + "\n" );
					}
					else if ( textSection.getName().equals( "author" ) )
					{
						// TODO process author
					}
					else if ( textSection.getName().equals( "title" ) )
					{
						if ( palmAnalitics.getTextCompare().getDistanceByLuceneLevenshteinDistance( textSection.getContent().toLowerCase(), publication.getTitle().toLowerCase() ) > 0.8f )
						{
							publication.setTitle( textSection.getContent() );
							responseMap.put( "status", "Ok" );
						}
						else
						{
							responseMap.put( "status", "Error - publication not found" );
							break;
						}
					}
				}
				else
				{
					if ( textSection.getContent().length() > 5 )
						contentSection.append( textSection.getContent() );
				}
			}
			publicationContent.setLength( publicationContent.length() - contentSectionWithName.length() );
			publication.setContentText( Jsoup.parse( publicationContent.toString() ).text() );
			publication.setAbstractText( Jsoup.parse( publicationAbstract.toString() ).text() );
			publication.setReferenceText( Jsoup.parse( contentSection.toString() ).text() );
			if ( publicationKeyword.length() > 0 )
				publication.setKeywordText( publicationKeyword.toString() );

		}
	}

	public void processExtractedPublicationAuthor( Publication publication, List<TextSection> textAuthorSections )
	{

	}

	public void processExtractedPublicationReferences( Publication publication, List<TextSection> textReferenceSections )
	{

	}
}

package de.rwth.i9.palm.pdfextraction.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
			if ( publication.getPdfSourceUrl() != null )
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

	public Map<String, Object> extractPdfFromSpecificPublication( Publication publication ) throws IOException, InterruptedException, ExecutionException
	{
		Map<String, Object> resultMap = new LinkedHashMap<String, Object>();

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
		this.processExtractedPublication( extractedPfdFutureMap, resultMap );

		return resultMap;
	}

	/**
	 * Here the extracted text section will be 
	 * @param extractedPfdFutureMap
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void processExtractedPublication( Map<Publication, Future<List<TextSection>>> extractedPfdFutureMap, Map<String, Object> resultMap ) throws InterruptedException, ExecutionException
	{

		for ( Entry<Publication, Future<List<TextSection>>> extractedPfdFuture : extractedPfdFutureMap.entrySet() )
		{
			Publication publication = extractedPfdFuture.getKey();
			List<TextSection> textSections = extractedPfdFuture.getValue().get();
			// ==list of variables==
			float titleFontSize = 0f;
			float abstractHeaderFontSize = 0f;
			float sectionHeaderFontSize = 0f;
			float contentFontSize = 0f;
			boolean titleFound = false;
			
			// column size will be calculated after introduction section found
			float columnSize = 0f;
			
			// loop, searching 0817svnugraha
			for( TextSection textSection : textSections ){
				// if( textSection)
			}
		}
	}

	public void processExtractedPublicationAuthor( Publication publication, List<TextSection> textAuthorSections )
	{

	}

	public void processExtractedPublicationReferences( Publication publication, List<TextSection> textReferenceSections )
	{

	}
}

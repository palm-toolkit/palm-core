package de.rwth.i9.palm.pdfextraction.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

@Service
public class AsynchronousPdfExtractionService
{
	private final static Logger log = LoggerFactory.getLogger( AsynchronousPdfExtractionService.class );

	@Async
	public Future<List<TextSection>> extractPdfIntoTextSections( String pdfPath ) throws IOException
	{

		Stopwatch stopwatch = Stopwatch.createStarted();
		log.info( "Download and Extract pdf " + pdfPath + " starting" );

		PdfReader reader = new PdfReader( pdfPath );

		Rectangle pdfPageSize = reader.getPageSize( 1 );
		PalmPdfExtractionStrategy palmPdfExtractionStrategy = new PalmPdfExtractionStrategy();

		// set margin and page size
		palmPdfExtractionStrategy.setPageMargin( 50f );
		palmPdfExtractionStrategy.setPageSize( pdfPageSize );

		for ( int i = 1; i <= reader.getNumberOfPages(); i++ )
		{
			// update the current page size
			palmPdfExtractionStrategy.setPageNumber( i );
			// read perpage
			PdfTextExtractor.getTextFromPage( reader, i, palmPdfExtractionStrategy );
		}

		List<TextSection> textSections = palmPdfExtractionStrategy.getTextSection();

		stopwatch.elapsed( TimeUnit.MILLISECONDS );

		log.info( "Download and Extract pdf " + pdfPath + " complete in " + stopwatch );

		return new AsyncResult<List<TextSection>>( textSections );
	}
}

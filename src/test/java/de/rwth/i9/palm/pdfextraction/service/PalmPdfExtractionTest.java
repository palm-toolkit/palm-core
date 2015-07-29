package de.rwth.i9.palm.pdfextraction.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

public class PalmPdfExtractionTest
{

	private final static Logger log = LoggerFactory.getLogger( PalmPdfExtractionTest.class );

	@Test
	public void palmPdfExtract() throws IOException
	{
		String src = "http://biblio.uabcs.mx/html/libros/pdf/18/2.pdf";
		src = "C:\\Users\\nifry\\Google Drive\\Thesis\\Papers\\02 Deconstruct and Reconstruct - Using Topic Modeling on an Analytics Corpus - 2014.pdf";
		src = "C:\\Users\\nifry\\Google Drive\\Thesis\\Papers\\01 Fostering Analytics on Learning Analytics Research - the LAK Dataset - 2014.pdf";
		src = "http://ifets.info/journals/15_3/ets_15_3_contents.pdf";
		src = "http://dspace.learningnetworks.org/bitstream/1820/3180/1/Chatti_ETS.pdf";
		src = "http://www.irrodl.org/index.php/irrodl/article/download/2032/3322";
		src = "http://honne.learning-context.de/upload/files/publications/DeLFI14_HT.pdf";

		PdfReader reader = new PdfReader( src );

		Stopwatch stopwatch = Stopwatch.createStarted();
		log.info( "start extracting pdf" );

		Rectangle pdfPageSize = reader.getPageSize( 1 );
		PalmPdfExtractionStrategy palmPdfExtractionStrategy = new PalmPdfExtractionStrategy();

		// set margin and page size
		palmPdfExtractionStrategy.setPageMargin( 50f );
		palmPdfExtractionStrategy.setPageSize( pdfPageSize );

		System.out.println( "===================== TEST PALM EXTRACTION ======================" );

		for ( int i = 1; i <= reader.getNumberOfPages(); i++ )
		{
			// update the current page size
			palmPdfExtractionStrategy.setPageNumber( i );
			PdfTextExtractor.getTextFromPage( reader, i, palmPdfExtractionStrategy );
		}

		stopwatch.elapsed( TimeUnit.MILLISECONDS );
		log.info( "Complete extracting pdf " + stopwatch );

		List<TextSection> textSections = palmPdfExtractionStrategy.getTextSections();

		int index = 0;
		for ( TextSection textSection : textSections )
		{
			String name = ( textSection.getName() != null ) ? textSection.getName() : "\t";
			System.out.println( index + " >pg:" + textSection.getPageNumber() + 
					//" >tx" + textSection.getTopLeftBoundary().get( 0 ) + 
					//" >ty" + textSection.getTopLeftBoundary().get( 1 ) +
					//" >bx" + textSection.getBottomRightBoundary().get( 0 ) + 
					//" >by" + textSection.getBottomRightBoundary().get( 1 ) +
					" >nm:" + name + " >fh:" + textSection.getFontHeight() + " >tx:" + textSection.getContent() );
			index++;
		}

	}
}

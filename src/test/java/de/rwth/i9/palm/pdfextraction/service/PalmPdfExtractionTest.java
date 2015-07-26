package de.rwth.i9.palm.pdfextraction.service;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

public class PalmPdfExtractionTest
{

	@Test
	public void palmPdfExtract() throws IOException
	{
		String src = "C:\\Users\\nifry\\Desktop\\Docear's_PDF_Inspector_--_Title_Extraction_from_PDF_files--preprint.pdf";
		src = "http://biblio.uabcs.mx/html/libros/pdf/18/2.pdf";

		PdfReader reader = new PdfReader( src );

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
			System.out.println( "============= End Of Page Number " + i + " ===========" );
		}

		List<TextSection> textSections = palmPdfExtractionStrategy.getTextSection();

		int index = 0;
		for ( TextSection textSection : textSections )
		{
			System.out.println( index + " >pg:" + textSection.getPageNumber() + " >fh:" + textSection.getFontHeight() + " >tx:" + textSection.getContent() );
			index++;
		}

	}
}

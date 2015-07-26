package de.rwth.i9.palm.pdfextraction.service;

import java.io.IOException;
import java.util.List;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

public class ItextPdfExtraction
{
	public static List<TextSection> extractPdf( String pdfPath ) throws IOException
	{
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

		return palmPdfExtractionStrategy.getTextSection();
	}
}

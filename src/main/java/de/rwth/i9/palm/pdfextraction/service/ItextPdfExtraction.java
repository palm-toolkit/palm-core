package de.rwth.i9.palm.pdfextraction.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

public class ItextPdfExtraction
{
	public static List<TextSection> extractPdf( String pdfPath ) throws IOException
	{
		PdfReader reader = null;
		try
		{
			reader = new PdfReader( pdfPath );
		}
		catch ( Exception e )
		{
			return Collections.emptyList();
		}

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

		return palmPdfExtractionStrategy.getTextSections();
	}
}

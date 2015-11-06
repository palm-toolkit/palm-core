package de.rwth.i9.palm.pdfextraction.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

public class ItextPdfExtraction
{
	public static List<TextSection> extractPdf( String pdfPath ) throws IOException
	{
		return extractPdf( pdfPath, 0 );
	}

	/**
	 * Extract pdf with specific PALM extraction strategy
	 * 
	 * @param pdfPath
	 * @param untilPage
	 *            = 0 means extract all pages
	 * @return
	 * @throws IOException
	 */
	public static List<TextSection> extractPdf( String pdfPath, int untilPage ) throws IOException
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
			// break when untilPage are specified
			if ( untilPage != 0 && i >= untilPage )
				break;
		}

		return palmPdfExtractionStrategy.getTextSections();
	}

	/**
	 * Extract pdf with specific PALM extraction strategy
	 * 
	 * @param pdfPath
	 * @param untilPage
	 *            = 0 means extract all pages
	 * @return
	 * @throws IOException
	 */
	public static List<TextSection> extractPdfFromInputStream( InputStream pdfInputStream, int untilPage ) throws IOException
	{
		PdfReader reader = null;
		try
		{
			reader = new PdfReader( pdfInputStream );
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
			// break when untilPage are specified
			if ( untilPage != 0 && i >= untilPage )
				break;
		}

		return palmPdfExtractionStrategy.getTextSections();
	}

}

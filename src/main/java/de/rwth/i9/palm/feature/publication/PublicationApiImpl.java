package de.rwth.i9.palm.feature.publication;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.datasetcollect.service.HtmlPublicationCollection;
import de.rwth.i9.palm.pdfextraction.service.PdfExtractionService;

@Component
public class PublicationApiImpl implements PublicationApi
{
	@Autowired
	private PdfExtractionService pdfExtractionService;

	@Override
	public Map<String, Object> extractPfdFile( String url ) throws IOException, InterruptedException, ExecutionException
	{
		return pdfExtractionService.extractPdfFromSpecificUrl( url );
	}

	@Override
	public Map<String, String> extractHtmlFile( String url ) throws IOException
	{
		return HtmlPublicationCollection.getPublicationInformationFromHtmlPage( url );
	}

}

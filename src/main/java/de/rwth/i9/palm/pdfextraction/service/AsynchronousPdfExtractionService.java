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

import de.rwth.i9.palm.model.Publication;

@Service
public class AsynchronousPdfExtractionService
{
	private final static Logger log = LoggerFactory.getLogger( AsynchronousPdfExtractionService.class );

	@Async
	public Future<List<TextSection>> extractPublicationPdfIntoTextSections( Publication publication ) throws IOException
	{

		Stopwatch stopwatch = Stopwatch.createStarted();
		log.info( "Download and Extract pdf " + publication.getTitle() + " starting" );

		List<TextSection> textSections = ItextPdfExtraction.extractPdf( publication.getPdfSourceUrl() );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );

		log.info( "Download and Extract pdf " + publication.getTitle() + " complete in " + stopwatch );

		return new AsyncResult<List<TextSection>>( textSections );
	}
}

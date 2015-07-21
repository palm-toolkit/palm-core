package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;

import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationSource;
import de.rwth.i9.palm.model.SourceMethod;
import de.rwth.i9.palm.model.SourceType;

@Service
public class AsynchronousCollectionService
{
	private final static Logger log = LoggerFactory.getLogger( AsynchronousCollectionService.class );

	@Async
	public Future<Long> callAsync( int taskCall ) throws InterruptedException
	{

		Stopwatch stopwatch = Stopwatch.createStarted();

		log.info( "task " + taskCall + " starting" );

		Thread.sleep( 5000 );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );

		log.info( "task " + taskCall + "completed in " + stopwatch );

		return new AsyncResult<Long>( stopwatch.elapsed( TimeUnit.MILLISECONDS ) );
	}

	@Async
	public Future<List<Map<String, String>>> getListOfAuthorsGoogleScholar( String authorName ) throws IOException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();

		log.info( "get author from google scholar with query " + authorName + " starting" );

		List<Map<String, String>> authorMap = GoogleScholarPublicationCollection.getListOfAuthors( authorName );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );

		log.info( "get author from google scholar with query " + authorName + " complete in " + stopwatch );
		return new AsyncResult<List<Map<String, String>>>( authorMap );
	}

	@Async
	public Future<List<Map<String, String>>> getListOfAuthorsCiteseerX( String authorName ) throws IOException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();

		log.info( "get author from citeseerX with query " + authorName + " starting" );

		List<Map<String, String>> authorMap = CiteseerXPublicationCollection.getListOfAuthors( authorName );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );

		log.info( "get author from citeSeerX with query " + authorName + " complete in " + stopwatch );
		return new AsyncResult<List<Map<String, String>>>( authorMap );
	}

	@Async
	public Future<List<Map<String, String>>> getListOfPublicationsGoogleScholar( String url ) throws IOException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();

		log.info( "get publication list from google scholar with url " + url + " starting" );

		List<Map<String, String>> publicationMapList = GoogleScholarPublicationCollection.getPublicationListByAuthorUrl( url );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );

		log.info( "get publication list from google scholar with url " + url + " complete in " + stopwatch );
		return new AsyncResult<List<Map<String, String>>>( publicationMapList );
	}

	@Async
	public Future<List<Map<String, String>>> getListOfPublicationCiteseerX( String url ) throws IOException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();

		log.info( "get publication list from citeseerX with query " + url + " starting" );

		List<Map<String, String>> publicationMapList = CiteseerXPublicationCollection.getPublicationListByAuthorUrl( url );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );

		log.info( "get publication list from citeSeerX with url " + url + " complete in " + stopwatch );
		return new AsyncResult<List<Map<String, String>>>( publicationMapList );
	}

	@Async
	public Future<Long> getListOfPublicationsDetailGoogleScholarForTesting( String sourceUrl ) throws IOException, InterruptedException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();

		log.info( "get publication detail from google scholar with url " + sourceUrl + " starting" );

		// scrap the webpage
		Map<String, String> publicationDetailMap = GoogleScholarPublicationCollection.getPublicationDetailByPublicationUrl( sourceUrl );

		Thread.sleep( 5000 );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );

		log.info( "get publication detail from google scholar with url " + sourceUrl + " complete in " + stopwatch );
		return new AsyncResult<Long>( stopwatch.elapsed( TimeUnit.MILLISECONDS ) );
	}

	@Async
	public Future<Long> getListOfPublicationsDetailCiteseerForTesting( String sourceUrl ) throws IOException, InterruptedException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();

		log.info( "get publication detail from google scholar with url " + sourceUrl + " starting" );

		// scrap the webpage
		Map<String, String> publicationDetailMap = CiteseerXPublicationCollection.getPublicationDetailByPublicationUrl( sourceUrl );

		Thread.sleep( 5000 );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );

		log.info( "get publication detail from google scholar with url " + sourceUrl + " complete in " + stopwatch );
		return new AsyncResult<Long>( stopwatch.elapsed( TimeUnit.MILLISECONDS ) );
	}

	@Async
	public Future<PublicationSource> getListOfPublicationsDetailGoogleScholar( PublicationSource publicationSource ) throws IOException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();

		log.info( "get publication detail from google scholar with url " + publicationSource.getSourceUrl() + " starting" );

		// scrap the webpage
		Map<String, String> publicationDetailMap = GoogleScholarPublicationCollection.getPublicationDetailByPublicationUrl( publicationSource.getSourceUrl() );

		// assign the information gathered into publicationSource object
		// this.assignInformationFromGoogleScholar( publicationSource,
		// publicationDetailMap );

		if ( publicationDetailMap.get( "doc" ) != null )
			publicationSource.setPdfSource( publicationDetailMap.get( "doc" ) );

		if ( publicationDetailMap.get( "doc_url" ) != null )
			publicationSource.setPdfSourceUrl( publicationDetailMap.get( "doc_url" ) );

		if ( publicationDetailMap.get( "Authors" ) != null )
			publicationSource.setAuthorString( publicationDetailMap.get( "Authors" ) );

		if ( publicationDetailMap.get( "Publication date" ) != null )
			publicationSource.setDate( publicationDetailMap.get( "Publication date" ) );

		if ( publicationDetailMap.get( "Journal" ) != null )
		{
			publicationSource.setPublicationType( "JOURNAL" );
			publicationSource.setPublicationEvent( publicationDetailMap.get( "Journal" ) );
		}

		if ( publicationDetailMap.get( "Book" ) != null )
		{
			publicationSource.setPublicationType( "BOOK" );
			publicationSource.setPublicationEvent( publicationDetailMap.get( "Book" ) );
		}

		if ( publicationDetailMap.get( "Conference" ) != null )
		{
			publicationSource.setPublicationType( "CONFERENCE" );
			publicationSource.setPublicationEvent( publicationDetailMap.get( "Conference" ) );
		}

		if ( publicationDetailMap.get( "Pages" ) != null )
			publicationSource.setPages( publicationDetailMap.get( "Pages" ) );

		if ( publicationDetailMap.get( "Publisher " ) != null )
			publicationSource.setPublisher( publicationDetailMap.get( "Publisher " ) );

		if ( publicationDetailMap.get( "Volume" ) != null )
			publicationSource.setVolume( publicationDetailMap.get( "Volume" ) );

		if ( publicationDetailMap.get( "Issue" ) != null )
			publicationSource.setIssue( publicationDetailMap.get( "Issue" ) );

		if ( publicationDetailMap.get( "Description" ) != null )
			publicationSource.setAbstractText( publicationDetailMap.get( "Description" ) );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );

		log.info( "get publication detail from google scholar with url " + publicationSource.getSourceUrl() + " complete in " + stopwatch );
		return new AsyncResult<PublicationSource>( publicationSource );
	}

	@Async
	public Future<PublicationSource> getListOfPublicationDetailCiteseerX( PublicationSource publicationSource ) throws IOException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();

		log.info( "get publication detail from citeseerX with query " + publicationSource.getSourceUrl() + " starting" );

		// scrap the webpage
		Map<String, String> publicationDetailMap = CiteseerXPublicationCollection.getPublicationDetailByPublicationUrl( publicationSource.getSourceUrl() );

		// assign the information gathered into publicationSource object
		// this.assignInformationFromCiteseerx( publicationSource,
		// publicationDetailMap );

		if ( publicationDetailMap.get( "doc" ) != null )
			publicationSource.setPdfSource( publicationDetailMap.get( "doc" ) );

		if ( publicationDetailMap.get( "doc_url" ) != null )
			publicationSource.setPdfSourceUrl( publicationDetailMap.get( "doc_url" ) );

		if ( publicationDetailMap.get( "coauthor" ) != null )
			publicationSource.setAuthorString( publicationDetailMap.get( "coauthor" ) );

		if ( publicationDetailMap.get( "venue" ) != null )
			publicationSource.setPublicationEvent( publicationDetailMap.get( "venue" ) );

		if ( publicationDetailMap.get( "abstract" ) != null )
			publicationSource.setAbstractText( publicationDetailMap.get( "abstract" ) );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );

		log.info( "get publication detail from citeSeerX with url " + publicationSource.getSourceUrl() + " complete in " + stopwatch );
		return new AsyncResult<PublicationSource>( publicationSource );
	}

	@Async
	public Future<Publication> asyncWalkOverSelectedPublication( Publication publication ) throws IOException, InterruptedException
	{
		// multithread publication source
		List<Future<PublicationSource>> publicationSourceFutureList = new ArrayList<Future<PublicationSource>>();

		for ( PublicationSource publicationSource : publication.getPublicationSources() )
		{
			// handling publication source
			if ( publicationSource.getSourceMethod() == SourceMethod.PARSEPAGE )
			{
				if ( publicationSource.getSourceType() == SourceType.GOOGLESCHOLAR )
					publicationSourceFutureList.add( this.getListOfPublicationsDetailGoogleScholar( publicationSource ) );
				else if ( publicationSource.getSourceType() == SourceType.CITESEERX )
					publicationSourceFutureList.add( this.getListOfPublicationDetailCiteseerX( publicationSource ) );
			}
		}

		return new AsyncResult<Publication>( publication );
	}
}

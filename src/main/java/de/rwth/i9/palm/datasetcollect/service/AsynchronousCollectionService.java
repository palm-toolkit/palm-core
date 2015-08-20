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
import de.rwth.i9.palm.utils.TextUtils;

/**
 * 
 * @author sigit
 *
 */
@Service
public class AsynchronousCollectionService
{
	private final static Logger log = LoggerFactory.getLogger( AsynchronousCollectionService.class );

	/**
	 * Only for testing purpose. This method has no functionality purpose over
	 * PALM-project
	 * 
	 * @param taskCall
	 * @return
	 * @throws InterruptedException
	 */
	@Async
	public Future<Long> callAsync( int taskCall ) throws InterruptedException
	{

		Stopwatch stopwatch = Stopwatch.createStarted();
		log.info( "task " + taskCall + " starting" );

		// simulation of some process (IO/crawling)
		Thread.sleep( 5000 );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );
		log.info( "task " + taskCall + "completed in " + stopwatch );

		return new AsyncResult<Long>( stopwatch.elapsed( TimeUnit.MILLISECONDS ) );
	}

	/**
	 * Asynchronously gather author list from google scholar
	 * 
	 * @param authorName
	 * @return
	 * @throws IOException
	 */
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

	/**
	 * Asynchronously gather author list from citeseerx
	 * 
	 * @param authorName
	 * @return
	 * @throws IOException
	 */
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

	/**
	 * Asynchronously gather author list from citeseerx
	 * 
	 * @param authorName
	 * @return
	 * @throws IOException
	 */
	@Async
	public Future<List<Map<String, String>>> getListOfAuthorsDblp( String authorName ) throws IOException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();
		log.info( "get author from DBLP with query " + authorName + " starting" );

		List<Map<String, String>> authorMap = DblpPublicationCollection.getListOfAuthors( authorName );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );
		log.info( "get author from DBLP with query " + authorName + " complete in " + stopwatch );

		return new AsyncResult<List<Map<String, String>>>( authorMap );
	}

	/**
	 * Asynchronously gather publication list from google scholar
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
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

	/**
	 * Asynchronously gather publication list from citeseerx
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
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

	/**
	 * Asynchronously gather publication list from DBLP
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	@Async
	public Future<List<Map<String, String>>> getListOfPublicationDBLP( String url ) throws IOException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();
		log.info( "get publication list from DBLP with query " + url + " starting" );

		List<Map<String, String>> publicationMapList = DblpPublicationCollection.getPublicationListByAuthorUrl( url );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );
		log.info( "get publication list from DBLP with url " + url + " complete in " + stopwatch );

		return new AsyncResult<List<Map<String, String>>>( publicationMapList );
	}

	/**
	 * Asynchronously gather publication information (keywords and abstract)
	 * from HtmlPage
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	@Async
	public Future<Map<String, String>> getPublicationInfromationFromWebPage( String url ) throws IOException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();
		log.info( "get publication list from Htmlpage " + url + " starting" );

		Map<String, String> publicationInformationMap = HtmlPublicationCollection.getPublicationInformationFromHtmlPage( url );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );
		log.info( "get publication list from Htmlpagel " + url + " complete in " + stopwatch );

		return new AsyncResult<Map<String, String>>( publicationInformationMap );
	}

	/**
	 * Asynchronously gather publication detail from google scholar
	 * 
	 * @param publicationSource
	 * @return
	 * @throws IOException
	 */
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
			publicationSource.setMainSource( publicationDetailMap.get( "doc" ) );

		if ( publicationDetailMap.get( "doc_url" ) != null )
			publicationSource.setMainSourceUrl( publicationDetailMap.get( "doc_url" ) );

		if ( publicationDetailMap.get( "Authors" ) != null )
			publicationSource.setCoAuthors( publicationDetailMap.get( "Authors" ) );

		if ( publicationDetailMap.get( "Publication date" ) != null )
			publicationSource.setDate( publicationDetailMap.get( "Publication date" ) );

		if ( publicationDetailMap.get( "Journal" ) != null )
		{
			publicationSource.setPublicationType( "JOURNAL" );
			publicationSource.setVenue( TextUtils.cutTextToLength( publicationDetailMap.get( "Journal" ), 200 ) );
		}

		if ( publicationDetailMap.get( "Book" ) != null )
		{
			publicationSource.setPublicationType( "BOOK" );
			publicationSource.setVenue( TextUtils.cutTextToLength( publicationDetailMap.get( "Book" ), 200 ) );
		}

		if ( publicationDetailMap.get( "Conference" ) != null )
		{
			publicationSource.setPublicationType( "CONFERENCE" );
			publicationSource.setVenue( TextUtils.cutTextToLength( publicationDetailMap.get( "Conference" ), 200 ) );
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
		{
			String abstractText = publicationDetailMap.get( "Description" );
			if ( abstractText.length() > 200 )
			{
				if ( abstractText.substring( 0, 8 ).equalsIgnoreCase( "abstract" ) )
					abstractText = abstractText.substring( 9 );
				if ( abstractText.endsWith( "..." ) )
					abstractText = abstractText.substring( 0, abstractText.length() - 4 );
				publicationSource.setAbstractText( abstractText );
			}
		}

		stopwatch.elapsed( TimeUnit.MILLISECONDS );
		log.info( "get publication detail from google scholar with url " + publicationSource.getSourceUrl() + " complete in " + stopwatch );

		return new AsyncResult<PublicationSource>( publicationSource );
	}

	/**
	 * Asynchronously gather publication detail from citeseerx
	 * 
	 * @param publicationSource
	 * @return
	 * @throws IOException
	 */
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
			publicationSource.setMainSource( publicationDetailMap.get( "doc" ) );

		if ( publicationDetailMap.get( "doc_url" ) != null )
			publicationSource.setMainSourceUrl( publicationDetailMap.get( "doc_url" ) );

		if ( publicationDetailMap.get( "coauthor" ) != null )
			publicationSource.setCoAuthors( publicationDetailMap.get( "coauthor" ) );

		if ( publicationDetailMap.get( "venue" ) != null )
			publicationSource.setVenue( TextUtils.cutTextToLength( publicationDetailMap.get( "venue" ), 200 ) );

		if ( publicationDetailMap.get( "abstract" ) != null )
		{

			String abstractText = publicationDetailMap.get( "abstract" );
			if ( abstractText.length() > 200 )
			{
				if ( abstractText.substring( 0, 8 ).equalsIgnoreCase( "abstract" ) )
					abstractText = abstractText.substring( 9 );
				publicationSource.setAbstractText( abstractText );
			}
		}

		stopwatch.elapsed( TimeUnit.MILLISECONDS );
		log.info( "get publication detail from citeSeerX with url " + publicationSource.getSourceUrl() + " complete in " + stopwatch );

		return new AsyncResult<PublicationSource>( publicationSource );
	}

	/**
	 * Asynchronously gather publication detail from Dblp
	 * 
	 * @param publicationSource
	 * @return
	 * @throws IOException
	 */
	@Async
	public Future<PublicationSource> getListOfPublicationDetailDblp( PublicationSource publicationSource ) throws IOException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();
		log.info( "get publication detail from Dblp with title " + publicationSource.getTitle() + " starting" );


		stopwatch.elapsed( TimeUnit.MILLISECONDS );
		log.info( "get publication detail from DBLP with title " + publicationSource.getTitle() + " complete in " + stopwatch );

		return new AsyncResult<PublicationSource>( publicationSource );
	}

	/**
	 * Main method for collect publication detail from a publication from
	 * multiple source
	 * 
	 * @param publication
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
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

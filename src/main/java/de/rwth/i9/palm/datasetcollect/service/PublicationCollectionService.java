package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Stopwatch;

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorSource;
import de.rwth.i9.palm.model.Institution;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationSource;
import de.rwth.i9.palm.model.SourceMethod;
import de.rwth.i9.palm.model.SourceType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Service
public class PublicationCollectionService
{
	private final static Logger log = LoggerFactory.getLogger( PublicationCollectionService.class );

	@Autowired
	private PersistenceStrategy persistenceStrategy;
	
	@Autowired
	private PalmAnalytics palmAnalitics;

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
	public Future<PublicationSource> getListOfPublicationsDetailGoogleScholar( PublicationSource publicationSource ) throws IOException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();

		log.info( "get publication detail from google scholar with url " + publicationSource.getSourceUrl() + " starting" );

		// scrap the webpage
		Map<String, String> publicationDetailMap = GoogleScholarPublicationCollection.getPublicationDetailByPublicationUrl( publicationSource.getSourceUrl() );
		
		// assign the information gathered into publicationSource object
		//this.assignInformationFromGoogleScholar(publicationSource, publicationDetailMap);

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
		//this.assignInformationFromCiteseerx(publicationSource, publicationDetailMap);
				
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
		
		// Wait until they are all done
//				boolean walkingPublicationSourceIsDone = true;
//				do
//				{
//					walkingPublicationSourceIsDone = true;
//					for ( Future<PublicationSource> publicationSourceFuture : publicationSourceFutureList )
//					{
//						if ( !publicationSourceFuture.isDone() ){
//							walkingPublicationSourceIsDone = false;
//							break;
//						}
//					}
//					// 10-millisecond pause between each check
//					Thread.sleep( 10 );
//				} while ( !walkingPublicationSourceIsDone );
		
		return new AsyncResult<Publication>( publication );
	}
	
	/**
	 * Assign publicationSource value to specific academic network
	 * @param publicationSource
	 * @param publicationMapList
	 */
	private void assignInformationFromGoogleScholar(PublicationSource publicationSource, Map<String, String> publicationDetailMap){
		System.out.println( "GS" );
		for ( Entry<String, String> eachPublicationDetail : publicationDetailMap.entrySet() )
			System.out.println( eachPublicationDetail.getKey() + " : " + eachPublicationDetail.getValue() );
		System.out.println(  );
	}
	
	/**
	 * Assign publicationSource value to specific academic network
	 * @param publicationSource
	 * @param publicationMapList
	 */
	private void assignInformationFromCiteseerx(PublicationSource publicationSource, Map<String, String> publicationDetailMap){
		
		System.out.println( "CX" );
		for ( Entry<String, String> eachPublicationDetail : publicationDetailMap.entrySet() )
			System.out.println( eachPublicationDetail.getKey() + " : " + eachPublicationDetail.getValue() );
		System.out.println(  );
	}
	
	
	

	/**
	 * Merging author information from multiple resources
	 * @param authorFutureLists
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@Transactional
	public void mergeAuthorInformation( List<Future<List<Map<String, String>>>> authorFutureLists ) throws InterruptedException, ExecutionException
	{
		if ( authorFutureLists.size() > 0 )
		{
			List<Map<String, String>> mergedAuthorList = new ArrayList<Map<String, String>>();
			Map<String, Integer> indexHelper = new HashMap<String, Integer>();
			for ( Future<List<Map<String, String>>> authorFutureList : authorFutureLists )
			{
				if ( authorFutureList.isDone() )
				{
					List<Map<String, String>> authorListMap = authorFutureList.get();
					for ( Map<String, String> authorMap : authorListMap )
					{
						String authorName = authorMap.get( "name" ).replaceAll( "[^a-zA-Z ]", "" ).toLowerCase().trim();
						// check if author already on array list
						Integer authorIndex = indexHelper.get( authorName );
						if ( authorIndex == null )
						{
							// if not exist on map
							mergedAuthorList.add( authorMap );
							indexHelper.put( authorName, mergedAuthorList.size() - 1 );
						}
						else
						{
							Map<String, String> mapFromMergerList = mergedAuthorList.get( authorIndex );
							for ( Map.Entry<String, String> entry : authorMap.entrySet() )
							{// merge everything else 
								if ( mapFromMergerList.get( entry.getKey() ) == null )
								{
									mapFromMergerList.put( entry.getKey(), entry.getValue() );
								}
								else
								{
									if ( entry.getKey().equals( "source" ) || entry.getKey().equals( "url" ) )
										mapFromMergerList.put( entry.getKey(), mapFromMergerList.get( entry.getKey() ) + " " + entry.getValue() );
								}
							}
						}
					}
				}
			}

			// test print
//			for ( Map<String, String> mergedAuthor : mergedAuthorList )
//			{
//				for ( Map.Entry<String, String> entry : mergedAuthor.entrySet() )
//				{
//					System.out.println( entry.getKey() + " -> " + entry.getValue() );
//				}
//				System.out.println();
//			}
			
			// update database
			for ( Map<String, String> mergedAuthor : mergedAuthorList )
			{
				String name = mergedAuthor.get( "name" );
				String institution = "";
				String otherDetail = "";

				String affliliation = mergedAuthor.get( "affiliation" );
				// looking for university
				if ( affliliation != null )
				{
					String[] authorDetails = affliliation.split( "," );
					for ( int i = 0; i < authorDetails.length; i++ )
					{
						// from word U"nivers"ity
						if ( authorDetails[i].contains( "nivers" ) || authorDetails[i].contains( "nstit" ) )
							institution = authorDetails[i].trim().toLowerCase();
						else
						{
							if ( !otherDetail.equals( "" ) )
								otherDetail += ", ";
							otherDetail += authorDetails[i];
						}
					}
				}
				List<Author> authors = persistenceStrategy.getAuthorDAO().getAuthorByNameAndInstitution( name, institution );
				Author author = null;

				if ( authors.isEmpty() )
				{
					author = new Author();
					author.setName( name );
					
					String[] splitName = name.split( " " );
					String lastName = splitName[ splitName.length - 1];
					author.setLastName( lastName );
					String firstName = name.substring( 0, name.length() - lastName.length()).trim();
					if( !firstName.equals( "" ))
						author.setFirstName( firstName );
					
					if ( !institution.equals( "" ) )
					{
						// find institution on database
						Institution institutionObject = persistenceStrategy.getInstitutionDAO().getByName( institution );
						if ( institutionObject == null )
						{
							institutionObject = new Institution();
							institutionObject.setName( institution );
							institutionObject.setURI( institution.replace( " ", "-" ) );
						}

						author.setInstitution( institutionObject );
					}

				}
				else
				{
					author = authors.get( 0 );
				}

				String photo = mergedAuthor.get( "photo" );
				if ( photo != null )
					author.setPhotoUrl( photo );

				if ( !otherDetail.equals( "" ) )
					author.setOtherDetail( otherDetail );
				
				if( mergedAuthor.get( "citedby" ) != null )
					author.setCitedBy( Integer.parseInt( mergedAuthor.get( "citedby" ) ) );

				// insert source
				List<AuthorSource> authorSources = new ArrayList<AuthorSource>();
				String[] sources = mergedAuthor.get( "source" ).split( " " );
				String[] sourceUrls = mergedAuthor.get( "url" ).split( " " );
				for ( int i = 0; i < sources.length; i++ )
				{
					if ( !sources[i].equals( "" ) )
					{
						AuthorSource as = new AuthorSource();
						as.setName( sources[i] );
						as.setSourceUrl( sourceUrls[i] );
						as.setSourceType( SourceType.valueOf( sources[i].toUpperCase() ) );
						as.setAuthor( author );

						authorSources.add( as );
					}
				}
				author.setAuthorSources( authorSources );

				persistenceStrategy.getAuthorDAO().persist( author );
			}
			
		}
		
	}

	/**
	 * THis function collect publication information and combine it into publication object
	 * @param publicationFutureLists
	 * @param author
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException 
	 */
	public void mergePublicationInformation( List<Future<List<Map<String, String>>>> publicationFutureLists , Author author ) throws InterruptedException, ExecutionException, IOException
	{
		if ( publicationFutureLists.size() > 0 )
		{
			// list/set of selected publication, either from database or completely new 
			List<Publication> selectedPublications = new ArrayList<Publication>();
			
			// first construct the publication
			// get it from database or create new if still doesn't exist
			constructPublicationWithSources( selectedPublications, publicationFutureLists , author );
			
			// extract and combine information from multiple sources
			getPublicationInformationFromSources( selectedPublications );
		}
		
	}
	
	/**
	 * Cunstrucnt the publication and publicationSources
	 * @param selectedPublications
	 * @param publicationFutureLists
	 * @param author
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void constructPublicationWithSources( List<Publication> selectedPublications,  List<Future<List<Map<String, String>>>> publicationFutureLists , Author author ) throws InterruptedException, ExecutionException{
		for ( Future<List<Map<String, String>>> publicationFutureList : publicationFutureLists )
		{
			if ( publicationFutureList.isDone() )
			{
				List<Map<String, String>> publicationMapLists = publicationFutureList.get();
				for ( Map<String, String> publicationMap : publicationMapLists )
				{
					Publication publication = null;
					String publicationTitle = publicationMap.get( "title" );
					
					if( publicationTitle == null )
						continue;
					
					// get the publication object
					List<Publication> fromDbPublication = persistenceStrategy.getPublicationDAO().getPublicationViaFuzzyQuery( publicationTitle, .8f, 1 );
					// check publication from database
					if( !fromDbPublication.isEmpty()){
						if( fromDbPublication.size()>1 ){
							// check with year
							for(Publication pub : fromDbPublication){
								Calendar cal = Calendar.getInstance();
								cal.setTime( pub.getPublicationDate() );
								if( Integer.toString(cal.get(Calendar.YEAR)).equals( publicationMap.get( "year" ) )){
									publication = pub;
									publication.addCoAuthor( author );
									break;
								}
							}
						}
					}
					
					// check publication with the current selected list.
					if( !selectedPublications.isEmpty()){
						for( Publication pub : selectedPublications){
							if( palmAnalitics.getTextCompare().getDistanceByLuceneLevenshteinDistance( pub.getTitle(), publicationTitle ) > .9f){
								publication = pub;
								publication.addCoAuthor( author );
								break;
							}
						}
					}
					
					// check if null ( really new publication )
					if( publication == null ){
						publication = new Publication();
						publication.setTitle( publicationTitle );
						publication.addCoAuthor( author );
						selectedPublications.add( publication );
					}
					
					// create publication sources and assign it to publication
					PublicationSource publicationSource = new PublicationSource();
					publicationSource.setTitle( publicationTitle );
					publicationSource.setSourceUrl( publicationMap.get( "url" ) );
					publicationSource.setSourceMethod( SourceMethod.PARSEPAGE );
					publicationSource.setSourceType( SourceType.valueOf(publicationMap.get( "source" ).toUpperCase() ) );
					publication.addPublicationSource( publicationSource );
								
					// combine information from multiple sources;
					
					// check  print 
//					for ( Entry<String, String> eachPublicationDetail : publicationMap.entrySet() )
//						System.out.println( eachPublicationDetail.getKey() + " : " + eachPublicationDetail.getValue() );
//					System.out.println();
				}
			}
		}
	}
	
	/**
	 * combine publication information from multiple publication sources
	 * @param selectedPublications
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public void getPublicationInformationFromSources( List<Publication> selectedPublications ) throws IOException, InterruptedException{
		//multi thread future publication detail
		List<Future<Publication>> selectedPublicationFutureList = new ArrayList<Future<Publication>>();
		for( Publication publication : selectedPublications){
			selectedPublicationFutureList.add( this.asyncWalkOverSelectedPublication( publication ) );
			System.out.println( "walking" + publication.getTitle() );
		}
		
		// Wait until they are all done
		Thread.sleep( 1000 );
		boolean walkingPublicationIsDone = true;
		do
		{
			walkingPublicationIsDone = true;
			for ( Future<Publication> selectedPublicationFuture : selectedPublicationFutureList )
			{
				if ( !selectedPublicationFuture.isDone() )
					walkingPublicationIsDone = false;
				else
				{
					// combine from sources to publication

				}

			}
			// 10-millisecond pause between each check
			Thread.sleep( 10 );
		} while ( !walkingPublicationIsDone );
		
		int i=0;
	}

}

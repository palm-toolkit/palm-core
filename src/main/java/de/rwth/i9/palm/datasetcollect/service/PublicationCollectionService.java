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

		log.info( "get publication from google scholar with url " + url + " starting" );

		List<Map<String, String>> publicationMapList = GoogleScholarPublicationCollection.getPublicationListByAuthorUrl( url );

		stopwatch.elapsed( TimeUnit.MILLISECONDS );

		log.info( "get author from google scholar with url " + url + " complete in " + stopwatch );
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

	public void mergePublicationInformation( List<Future<List<Map<String, String>>>> publicationFutureLists , Author author) throws InterruptedException, ExecutionException
	{
		if ( publicationFutureLists.size() > 0 )
		{
			// list/set of selected publication, either from database or completely new 
			List<Publication> selectedPublications = new ArrayList<Publication>();
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
						publication.addPublicationSource( publicationSource );
						
						
						
						// check  print 
						for ( Entry<String, String> eachPublicationDetail : publicationMap.entrySet() )
							System.out.println( eachPublicationDetail.getKey() + " : " + eachPublicationDetail.getValue() );
						System.out.println();
					}
				}
			}
			int i = 0;
		}
		
	}

}

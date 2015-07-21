package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
import de.rwth.i9.palm.model.AuthorAlias;
import de.rwth.i9.palm.model.AuthorSource;
import de.rwth.i9.palm.model.Conference;
import de.rwth.i9.palm.model.ConferenceGroup;
import de.rwth.i9.palm.model.Institution;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationSource;
import de.rwth.i9.palm.model.PublicationType;
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
		this.assignInformationFromGoogleScholar( publicationSource, publicationDetailMap );

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
		this.assignInformationFromCiteseerx( publicationSource, publicationDetailMap );
				
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
		// System.out.println( "GS" );
		// for ( Entry<String, String> eachPublicationDetail :
		// publicationDetailMap.entrySet() )
		// System.out.println( eachPublicationDetail.getKey() + " : " +
		// eachPublicationDetail.getValue() );
		// System.out.println( );
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

	}
	
	/**
	 * Assign publicationSource value to specific academic network
	 * @param publicationSource
	 * @param publicationMapList
	 */
	private void assignInformationFromCiteseerx(PublicationSource publicationSource, Map<String, String> publicationDetailMap){
		
//		System.out.println( "CX" );
//		for ( Entry<String, String> eachPublicationDetail : publicationDetailMap.entrySet() )
//			System.out.println( eachPublicationDetail.getKey() + " : " + eachPublicationDetail.getValue() );
//		System.out.println(  );
		
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
						String authorName = authorMap.get( "name" ).toLowerCase().replace( ".", "" ).trim();
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
				String name = mergedAuthor.get( "name" ).toLowerCase().replace( ".", "" ).trim();
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
					String firstName = name.substring( 0, name.length() - lastName.length() ).replace( ".", "" ).trim();
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

				// author alias if exist
				if ( mergedAuthor.get( "aliases" ) != null )
				{
					String authorAliasesString = mergedAuthor.get( "aliases" );
					// remove '[...]' sign at start and end.
					authorAliasesString = authorAliasesString.substring( 1, authorAliasesString.length() - 1 );
					for ( String authorAliasString : authorAliasesString.split( "," ) )
					{
						authorAliasString = authorAliasString.toLowerCase().replace( ".", "" ).trim();
						if ( !name.equals( authorAliasString ) )
						{
							AuthorAlias authorAlias = new AuthorAlias();
							authorAlias.setCompleteName( authorAliasString );
							authorAlias.setAuthor( author );
							author.addAlias( authorAlias );
						}
					}
				}
				String photo = mergedAuthor.get( "photo" );
				if ( photo != null )
					author.setPhotoUrl( photo );

				if ( !otherDetail.equals( "" ) )
					author.setOtherDetail( otherDetail );
				
				if( mergedAuthor.get( "citedby" ) != null )
					author.setCitedBy( Integer.parseInt( mergedAuthor.get( "citedby" ) ) );

				// insert source
				Set<AuthorSource> authorSources = new LinkedHashSet<AuthorSource>();
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
	 * THis function collect publication information and combine it into
	 * publication object
	 * 
	 * @param publicationFutureLists
	 * @param author
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException
	 * @throws ParseException
	 */
	public void mergePublicationInformation( List<Future<List<Map<String, String>>>> publicationFutureLists, Author author ) throws InterruptedException, ExecutionException, IOException, ParseException
	{
		if ( publicationFutureLists.size() > 0 )
		{
			// list/set of selected publication, either from database or completely new 
			List<Publication> selectedPublications = new ArrayList<Publication>();
			
			// first construct the publication
			// get it from database or create new if still doesn't exist
			constructPublicationWithSources( selectedPublications, publicationFutureLists , author );
			
			// extract and combine information from multiple sources
			getPublicationInformationFromSources( selectedPublications, author );
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
					List<Publication> fromDbPublications = persistenceStrategy.getPublicationDAO().getPublicationViaPhraseSlopQuery( publicationTitle.toLowerCase(), 2 );
					// check publication from database
					if ( !fromDbPublications.isEmpty() )
					{
						if ( fromDbPublications.size() > 1 )
						{
							// check with year
							for ( Publication pub : fromDbPublications )
							{
								if ( pub.getPublicationDate() == null )
									continue;

								Calendar cal = Calendar.getInstance();
								cal.setTime( pub.getPublicationDate() );
								if( Integer.toString(cal.get(Calendar.YEAR)).equals( publicationMap.get( "year" ) )){
									publication = pub;
									publication.addCoAuthor( author );
									break;
								}
							}
							// if publication still null, due to publication
							// date is null
							if ( publication == null )
								publication = fromDbPublications.get( 0 );

						}
					}
					
					// check publication with the current selected list.
					if( !selectedPublications.isEmpty()){
						for( Publication pub : selectedPublications){
							if ( palmAnalitics.getTextCompare().getDistanceByLuceneLevenshteinDistance( pub.getTitle().toLowerCase(), publicationTitle.toLowerCase() ) > .9f )
							{
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

					if ( publicationMap.get( "nocitation" ) != null )
						publicationSource.setCitedBy( Integer.parseInt( publicationMap.get( "nocitation" ) ) );

					if ( publicationMap.get( "year" ) != null )
						publicationSource.setYear( publicationMap.get( "year" ) );

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
	 * 
	 * @param selectedPublications
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws ParseException
	 */
	public void getPublicationInformationFromSources( List<Publication> selectedPublications, Author pivotAuthor ) throws IOException, InterruptedException, ExecutionException, ParseException
	{
		//multi thread future publication detail
		List<Future<Publication>> selectedPublicationFutureList = new ArrayList<Future<Publication>>();
		for( Publication publication : selectedPublications){
			selectedPublicationFutureList.add( this.asyncWalkOverSelectedPublication( publication ) );
			System.out.println( "walking" + publication.getTitle() );
		}
		
		// Wait until they are all done
		Thread.sleep( 1000 );
		boolean walkingPublicationIsDone = true;
		// list coauthor of pivotauthor
		/*List<Author> coAuthors = new ArrayList<Author>();*/
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
					mergingPublicationInformation( selectedPublicationFuture.get(), pivotAuthor/*, coAuthors*/ );
				}

			}
			// 10-millisecond pause between each check
			Thread.sleep( 10 );
		} while ( !walkingPublicationIsDone );
		
	}

	/**
	 * 
	 * @param selectedPublications
	 * @throws ParseException
	 */
	public void mergingPublicationInformation( Publication pub, Author pivotAuthor/*, List<Author> coAuthors*/ ) throws ParseException
	{
		DateFormat dateFormat = new SimpleDateFormat( "yyyy/M/u", Locale.ENGLISH );
		Calendar calendar = Calendar.getInstance();

		for ( PublicationSource pubSource : pub.getPublicationSources() )
		{
			Date publicationDate = null;
			PublicationType publicationType = null;
			// Get unique characteristic on each of the source
			if ( pubSource.getSourceType() == SourceType.GOOGLESCHOLAR )
			{
				// publication date
				if ( pubSource.getDate() != null )
				{
					String pubSourceDate = pubSource.getDate();
					if ( pubSourceDate.length() == 4 )
						pubSourceDate += "/1/1";
					else if ( pubSourceDate.length() < 8 )
						pubSourceDate += "/1";
					publicationDate = dateFormat.parse( pubSourceDate );
					pub.setPublicationDate( publicationDate );
				}

				if ( pubSource.getPages() != null )
					pub.setPages( pubSource.getPages() );

				if ( pubSource.getPublisher() != null )
					pub.setPublisher( pubSource.getPublisher() );

				if ( pubSource.getIssue() != null )
					pub.setIssue( pubSource.getIssue() );

				if ( pubSource.getVolume() != null )
					pub.setVolume( pubSource.getVolume() );

			}
			else if ( pubSource.getSourceType() == SourceType.CITESEERX )
			{

			}
			// for general information
			// author
			if ( pubSource.getAuthorString() != null )
			{
				String[] authorsArray = pubSource.getAuthorString().split( "," );
				if ( authorsArray.length > pub.getCoAuthors().size() )
				{
					for ( String authorString : authorsArray )
					{
						authorString = authorString.toLowerCase().replace( ".", "" ).trim();
						String[] splitName = authorString.split( " " );
						String lastName = splitName[splitName.length - 1];
						String firstName = authorString.substring( 0, authorString.length() - lastName.length() ).trim();
						
						Author author = null;
						if ( pivotAuthor.getName().toLowerCase().equals( authorString.toLowerCase() ) )
							author = pivotAuthor;
						else
						{
							// first check from database by full name
							List<Author> coAuthorsDb = persistenceStrategy.getAuthorDAO().getByName( authorString );
							if( !coAuthorsDb.isEmpty() ){
								// TODO: check other properties
								// for now just get the first element
								// later check whether there is already a connection with pivotAuthor
								// if not check institution
								author = coAuthorsDb.get( 0 );
							}
							
							// if there is no exact name, check for ambiguity,
							// start from lastname
							// and then check whether there is abbreviation name on first name
							if( author == null ){
								coAuthorsDb = persistenceStrategy.getAuthorDAO().getByLastName( lastName );
								if( !coAuthorsDb.isEmpty() ){
									String[] firstNameSplit = firstName.split( " " );
									for ( Author coAuthorDb : coAuthorsDb )
									{
										if ( coAuthorDb.isAliasNameFromFirstName( firstNameSplit ) )
										{
											if ( coAuthorDb.getFirstName().length() > firstName.length() )
											{
												AuthorAlias authorAlias = new AuthorAlias();
												authorAlias.setCompleteName( authorString );
												authorAlias.setAuthor( coAuthorDb );
												coAuthorDb.addAlias( authorAlias );
												persistenceStrategy.getAuthorDAO().persist( coAuthorDb );
											}
											else
											{
												// change name with longer name
												String tempName = coAuthorDb.getName();
												coAuthorDb.setName( authorString );
												coAuthorDb.setFirstName( firstName );

												AuthorAlias authorAlias = new AuthorAlias();
												authorAlias.setCompleteName( tempName );
												authorAlias.setAuthor( coAuthorDb );
												coAuthorDb.addAlias( authorAlias );
												persistenceStrategy.getAuthorDAO().persist( coAuthorDb );
											}

											author = coAuthorDb;
											break;
										}
									}
								}
							}
							// first check from coauthorList
//							for ( Author coAuthor : coAuthors )
//							{
								// check from the last name first
								// get coauthor with same last name
								
								
//								
//								if ( palmAnalitics.getTextCompare().getDistanceByLuceneLevenshteinDistance( coAuthor.getName().toLowerCase(), authorString.toLowerCase() ) > .9f )
//								{
//									author = coAuthor;
//									break;
//								}
//								if ( coAuthor.getAliases() != null )
//									for ( AuthorAlias authorAlias : coAuthor.getAliases() )
//									{
//										if ( palmAnalitics.getTextCompare().getDistanceByLuceneLevenshteinDistance( authorAlias.getName().toLowerCase(), authorString.toLowerCase() ) > .8f )
//										{
//											author = coAuthor;
//											break;
//										}
//								}

//							}
							// TODO : this probably not correct, since author
							// name are ambigous, the best way is to check their
							// relation and their affiliation
							if( author == null ){
								// create new author
									author = new Author();
								// set for all possible name
								author.setPossibleNames( authorString );
	
									// save new author
									persistenceStrategy.getAuthorDAO().persist( author );
	
									author.addPublication( pub );
									pub.addCoAuthor( author );
//								}
//								if ( !coAuthors.contains( author ) )
//									coAuthors.add( author );
							}
						}
					}
				}
			}

			// abstract ( searching the longest)
			if ( pubSource.getAbstractText() != null )
				if ( pub.getAbstractText() == null || pub.getAbstractText().length() < pubSource.getAbstractText().length() )
					pub.setAbstractText( pubSource.getAbstractText() );

			if ( pub.getPublicationDate() == null && publicationDate == null && pubSource.getYear() != null )
			{
				publicationDate = dateFormat.parse( pubSource.getYear() + "/1/1" );
				pub.setPublicationDate( publicationDate );
			}

			// pdf source ( probably need list of it)
			if ( pubSource.getPdfSource() != null && pub.getPdfSource() == null )
				pub.setPdfSource( pubSource.getPdfSource() );

			// pdf sourceUrl ( probably need list of it)
			if ( pubSource.getPdfSourceUrl() != null && pub.getPdfSourceUrl() == null )
				pub.setPdfSourceUrl( pubSource.getPdfSourceUrl() );

			if ( pubSource.getCitedBy() > 0 && pubSource.getCitedBy() > pub.getCitedBy() )
				pub.setCitedBy( pubSource.getCitedBy() );

			// venuetype
			if ( pubSource.getPublicationType() != null )
			{
				publicationType = PublicationType.valueOf( pubSource.getPublicationType() );
				pub.setPublicationType( publicationType );
			}

			if ( pubSource.getPublicationEvent() != null && pub.getConference() == null )
			{
				String eventName = pubSource.getPublicationEvent();
				ConferenceGroup conferenceGroup = null;
				Conference conference = null;
				List<ConferenceGroup> conferenceGroups = persistenceStrategy.getConferenceDAO().getConferenceViaFuzzyQuery( eventName, .8f, 1 );
				if ( conferenceGroups.isEmpty() )
				{
					if ( publicationType != null )
					{
						// create conference group
						conferenceGroup = new ConferenceGroup();
						conferenceGroup.setName( eventName );
						String notationName = null;
						String[] eventNameSplit = eventName.split( " " );
						for ( String eachEventName : eventNameSplit )
							if ( !eachEventName.equals( "" ) && Character.isUpperCase( eachEventName.charAt( 0 ) ) )
								notationName += eachEventName.substring( 0, 1 );
						conferenceGroup.setNotation( notationName );
						conferenceGroup.setConferenceType( publicationType );
						// create conference
						if ( publicationDate != null )
						{
							// save conference group
							persistenceStrategy.getConferenceGroupDAO().persist( conferenceGroup );

							calendar.setTime( publicationDate );
							conference = new Conference();
							conference.setDate( publicationDate );
							conference.setYear( Integer.toString( calendar.get( Calendar.YEAR ) ) );
							conference.setConferenceGroup( conferenceGroup );
							pub.setConference( conference );
						}
					}
				}
			}

		}
		// last save publication, this will save allrelated objects
		persistenceStrategy.getPublicationDAO().persist( pub );
	}

}

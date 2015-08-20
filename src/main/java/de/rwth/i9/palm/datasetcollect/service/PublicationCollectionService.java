package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorAlias;
import de.rwth.i9.palm.model.AuthorSource;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationFile;
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
	private AsynchronousCollectionService asynchronousCollectionService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;
	
	@Autowired
	private PalmAnalytics palmAnalitics;

	/**
	 * Fetch author' publication list from academic networks
	 * 
	 * @param responseMap
	 * @param author
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws ParseException
	 */
	public void collectPublicationListFromNetwork( Map<String, Object> responseMap, Author author ) throws IOException, InterruptedException, ExecutionException, ParseException
	{
		// get author sources
		Set<AuthorSource> authorSources = author.getAuthorSources();
		if ( authorSources == null )
		{
			// TODO update author sources
			responseMap.put( "result", "error" );
			responseMap.put( "reason", "no author sources found" );
		}

		// get source and its flag "active/inactive" as Map
		Map<SourceType, Boolean> activeSourceMap = persistenceStrategy.getSourceDAO().getActiveSourceMap();

		// future list for publication list
		// extract dataset from academic network concurrently
		// Stopwatch stopwatch = Stopwatch.createStarted();

		List<Future<List<Map<String, String>>>> publicationFutureLists = new ArrayList<Future<List<Map<String, String>>>>();
		for ( AuthorSource authorSource : authorSources )
		{
			if ( authorSource.getSourceType() == SourceType.GOOGLESCHOLAR && activeSourceMap.get( SourceType.GOOGLESCHOLAR ) )
				publicationFutureLists.add( asynchronousCollectionService.getListOfPublicationsGoogleScholar( authorSource.getSourceUrl() ) );
			else if ( authorSource.getSourceType() == SourceType.CITESEERX && activeSourceMap.get( SourceType.CITESEERX ) )
				publicationFutureLists.add( asynchronousCollectionService.getListOfPublicationCiteseerX( authorSource.getSourceUrl() ) );
			else if ( authorSource.getSourceType() == SourceType.DBLP && activeSourceMap.get( SourceType.DBLP ) )
				publicationFutureLists.add( asynchronousCollectionService.getListOfPublicationDBLP( authorSource.getSourceUrl() ) );
		}

		// Wait until they are all done
		boolean processIsDone = true;
		do
		{
			processIsDone = true;
			for ( Future<List<Map<String, String>>> futureList : publicationFutureLists )
			{
				if ( !futureList.isDone() )
				{
					processIsDone = false;
					break;
				}
			}
			// 10-millisecond pause between each check
			Thread.sleep( 10 );
		} while ( !processIsDone );

		// merge the result
		this.mergePublicationInformation( publicationFutureLists, author );
	}
	
	/**
	 * Collect publication information and combine it into publication object
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
			this.constructPublicationWithSources( selectedPublications, publicationFutureLists , author );
			
			// extract and combine information from multiple sources
			this.getPublicationInformationFromSources( selectedPublications, author );
		}
		
	}
	
	/**
	 * Construct the publication and publicationSources, with the data gathered
	 * from academic networks
	 * 
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
					
					// check publication with the current selected list.
					if( !selectedPublications.isEmpty()){
						for( Publication pub : selectedPublications){
							if ( palmAnalitics.getTextCompare().getDistanceByLuceneLevenshteinDistance( pub.getTitle().toLowerCase(), publicationTitle.toLowerCase() ) > .9f )
							{
								publication = pub;
								break;
							}
						}
					}
					
					// check with publication from database
					if ( publication == null )
					{
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
									if ( Integer.toString( cal.get( Calendar.YEAR ) ).equals( publicationMap.get( "year" ) ) )
									{
										publication = pub;
										break;
									}
								}
								// if publication still null, due to publication
								// date is null
								if ( publication == null )
									publication = fromDbPublications.get( 0 );
							}
							else
								publication = fromDbPublications.get( 0 );
							// added to selected list
							selectedPublications.add( publication );
						}
						// remove old publicationSource
						if ( publication != null )
							publication.removeNonUserInputPublicationSource();
					}

					// if not exist any where create new publication
					if( publication == null ){
						publication = new Publication();
						publication.setTitle( publicationTitle );
						selectedPublications.add( publication );
					}
					// add coauthor
					publication.addCoAuthor( author );
					
					// create publication sources and assign it to publication
					PublicationSource publicationSource = new PublicationSource();
					publicationSource.setTitle( publicationTitle );
					publicationSource.setSourceUrl( publicationMap.get( "url" ) );
					publicationSource.setSourceMethod( SourceMethod.PARSEPAGE );
					publicationSource.setSourceType( SourceType.valueOf(publicationMap.get( "source" ).toUpperCase() ) );
					publicationSource.setPublication( publication );

					if ( publicationMap.get( "nocitation" ) != null )
						publicationSource.setCitedBy( Integer.parseInt( publicationMap.get( "nocitation" ) ) );

					if ( publicationMap.get( "coauthorUrl" ) != null )
						publicationSource.setCoAuthorsUrl( publicationMap.get( "coauthorUrl" ) );

					if ( publicationMap.get( "year" ) != null )
						publicationSource.setDate( publicationMap.get( "year" ) );

					if ( publicationMap.get( "doc" ) != null )
						publicationSource.setMainSource( publicationMap.get( "doc" ) );

					if ( publicationMap.get( "doc_url" ) != null )
						publicationSource.setMainSourceUrl( publicationMap.get( "doc_url" ) );

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
			selectedPublicationFutureList.add( asynchronousCollectionService.asyncWalkOverSelectedPublication( publication ) );
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
		DateFormat dateFormat = new SimpleDateFormat( "yyyy/M/d", Locale.ENGLISH );
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
					String publicationDateFormat = "yyyy/M/d";
					if ( pubSourceDate.length() == 4 )
					{
						pubSourceDate += "/1/1";
						publicationDateFormat = "yyyy";
					}
					else if ( pubSourceDate.length() < 8 )
					{
						pubSourceDate += "/1";
						publicationDateFormat = "yyyy/M";
					}
					publicationDate = dateFormat.parse( pubSourceDate );
					pub.setPublicationDate( publicationDate );
					pub.setPublicationDateFormat( publicationDateFormat );
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
			if ( pubSource.getCoAuthors() != null )
			{
				String[] authorsArray = pubSource.getCoAuthors().split( "," );
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

							}
						}
					}
				}
			}

			// abstract ( searching the longest)
			if ( pubSource.getAbstractText() != null )
				if ( pub.getAbstractText() == null || pub.getAbstractText().length() < pubSource.getAbstractText().length() )
					pub.setAbstractText( pubSource.getAbstractText() );

			if ( pub.getPublicationDate() == null && publicationDate == null && pubSource.getDate() != null )
			{
				publicationDate = dateFormat.parse( pubSource.getDate() + "/1/1" );
				pub.setPublicationDate( publicationDate );
				pub.setPublicationDateFormat( "yyyy" );
			}

			// publication file (the pdf)
			if ( pubSource.getMainSourceUrl() != null )
			{
				PublicationFile publicationFile = new PublicationFile();
				publicationFile.setUrl( pubSource.getMainSourceUrl() );
				if ( pubSource.getMainSource() != null )
					publicationFile.setSource( pubSource.getMainSource() );
				else
					publicationFile.setSource( pubSource.getSourceType().toString().toLowerCase() );
				publicationFile.setSourceType( pubSource.getSourceType() );
				publicationFile.setPublication( pub );

				boolean duplicated = false;
				if ( pub.getPublicationFiles() != null )
					for ( PublicationFile pubFile : pub.getPublicationFiles() )
					{
						if ( pubFile.getUrl().equals( pubSource.getMainSourceUrl() ) )
							duplicated = true;
					}

				if ( !duplicated )
					pub.addPublicationFile( publicationFile );
			}

			if ( pubSource.getCitedBy() > 0 && pubSource.getCitedBy() > pub.getCitedBy() )
				pub.setCitedBy( pubSource.getCitedBy() );

			// venuetype
			if ( pubSource.getPublicationType() != null )
			{
				publicationType = PublicationType.valueOf( pubSource.getPublicationType() );
				pub.setPublicationType( publicationType );


				if ( ( publicationType.equals( "CONFERENCE" ) || publicationType.equals( "JOURNAL" ) ) && pubSource.getVenue() != null && pub.getEvent() == null )
				{
					String eventName = pubSource.getVenue();
					EventGroup eventGroup = null;
					Event event = null;
					List<EventGroup> eventGroups = persistenceStrategy.getEventDAO().getEventViaFuzzyQuery( eventName, .8f, 1 );
					if ( eventGroups.isEmpty() )
					{
						if ( publicationType != null )
						{
							// create event group
							eventGroup = new EventGroup();
							eventGroup.setName( eventName );
							String notationName = null;
							String[] eventNameSplit = eventName.split( " " );
							for ( String eachEventName : eventNameSplit )
								if ( !eachEventName.equals( "" ) && Character.isUpperCase( eachEventName.charAt( 0 ) ) )
									notationName += eachEventName.substring( 0, 1 );
							eventGroup.setNotation( notationName );
							eventGroup.setPublicationType( publicationType );
							// create event
							if ( publicationDate != null )
							{
								// save event group
								persistenceStrategy.getEventGroupDAO().persist( eventGroup );

								calendar.setTime( publicationDate );
								event = new Event();
								event.setDate( publicationDate );
								event.setYear( Integer.toString( calendar.get( Calendar.YEAR ) ) );
								event.setEventGroup( eventGroup );
								pub.setEvent( event );
							}
						}
					}
				}
			}

		}
		// set is updated true
		pub.setContentUpdated( true );
		// last save publication, this will save all related objects
		persistenceStrategy.getPublicationDAO().persist( pub );
	}

}

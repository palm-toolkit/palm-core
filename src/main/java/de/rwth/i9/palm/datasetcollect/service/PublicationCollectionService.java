package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorSource;
import de.rwth.i9.palm.model.CompletionStatus;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.FileType;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationAuthor;
import de.rwth.i9.palm.model.PublicationFile;
import de.rwth.i9.palm.model.PublicationSource;
import de.rwth.i9.palm.model.PublicationType;
import de.rwth.i9.palm.model.Source;
import de.rwth.i9.palm.model.SourceMethod;
import de.rwth.i9.palm.model.SourceType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.ApplicationService;

@Service
public class PublicationCollectionService
{
	private final static Logger log = LoggerFactory.getLogger( PublicationCollectionService.class );

	@Autowired
	private AsynchronousPublicationDetailCollectionService asynchronousPublicationDetailCollectionService;

	@Autowired
	private AsynchronousCollectionService asynchronousCollectionService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;
	
	@Autowired
	private PalmAnalytics palmAnalitics;

	@Autowired
	private ApplicationService applicationService;

	@Autowired
	private MendeleyOauth2Helper mendeleyOauth2Helper;

	/**
	 * Fetch author' publication list from academic networks
	 * 
	 * @param responseMap
	 * @param author
	 * @param pid
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws ParseException
	 * @throws TimeoutException
	 * @throws org.apache.http.ParseException
	 * @throws OAuthProblemException
	 * @throws OAuthSystemException
	 */
	public void collectPublicationListFromNetwork( Map<String, Object> responseMap, Author author, String pid ) throws IOException, InterruptedException, ExecutionException, ParseException, TimeoutException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
	{
		// process log
		applicationService.putProcessLog( pid, "Collecting publications list from Academic Networks...<br>", "replace" );

		// get author sources
		Set<AuthorSource> authorSources = author.getAuthorSources();
		if ( authorSources == null )
		{
			// TODO update author sources
			responseMap.put( "result", "error" );
			responseMap.put( "reason", "no author sources found" );
		}

		// getSourceMap
		Map<String, Source> sourceMap = applicationService.getAcademicNetworkSources();

		// future list for publication list
		// extract dataset from academic network concurrently
		// Stopwatch stopwatch = Stopwatch.createStarted();

		List<Future<List<Map<String, String>>>> publicationFutureLists = new ArrayList<Future<List<Map<String, String>>>>();

		for ( AuthorSource authorSource : authorSources )
		{
			if ( authorSource.getSourceType() == SourceType.GOOGLESCHOLAR && sourceMap.get( SourceType.GOOGLESCHOLAR.toString() ).isActive() )
				publicationFutureLists.add( asynchronousCollectionService.getListOfPublicationsGoogleScholar( authorSource.getSourceUrl(), sourceMap.get( SourceType.GOOGLESCHOLAR.toString() ) ) );
			else if ( authorSource.getSourceType() == SourceType.CITESEERX && sourceMap.get( SourceType.CITESEERX.toString() ).isActive() )
				publicationFutureLists.add( asynchronousCollectionService.getListOfPublicationCiteseerX( authorSource.getSourceUrl(), sourceMap.get( SourceType.CITESEERX.toString() ) ) );
			else if ( authorSource.getSourceType() == SourceType.DBLP && sourceMap.get( SourceType.DBLP.toString() ).isActive() )
				publicationFutureLists.add( asynchronousCollectionService.getListOfPublicationDBLP( authorSource.getSourceUrl(), sourceMap.get( SourceType.DBLP.toString() ) ) );
		}
		if ( sourceMap.get( SourceType.MENDELEY.toString() ).isActive() )
		{
			// check for token validity
			mendeleyOauth2Helper.checkAndUpdateMendeleyToken( sourceMap.get( SourceType.MENDELEY.toString() ) );
			publicationFutureLists.add( asynchronousCollectionService.getListOfPublicationDetailMendeley( author, sourceMap.get( SourceType.MENDELEY.toString() ) ) );
		}
		// for MAS since not included on author search
		if ( sourceMap.get( SourceType.MAS.toString() ).isActive() )
			publicationFutureLists.add( asynchronousCollectionService.getListOfPublicationDetailMicrosoftAcademicSearch( author, sourceMap.get( SourceType.MAS.toString() ) ) );

		// wait till everything complete
		for ( Future<List<Map<String, String>>> publicationFuture : publicationFutureLists )
		{
			publicationFuture.get();
		}

		// process log
		applicationService.putProcessLog( pid, "Done collecting publications list from Academic Networks<br><br>", "append" );

		// process log
		applicationService.putProcessLog( pid, "Merging publication list...<br>", "append" );

		// merge the result
		this.mergePublicationInformation( publicationFutureLists, author, sourceMap, pid );
	}
	
	/**
	 * Collect publication information and combine it into publication object
	 * 
	 * @param publicationFutureLists
	 * @param author
	 * @param sourceMap
	 * @param pid
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException
	 * @throws ParseException
	 * @throws TimeoutException
	 */
	public void mergePublicationInformation( List<Future<List<Map<String, String>>>> publicationFutureLists, Author author, Map<String, Source> sourceMap, String pid ) throws InterruptedException, ExecutionException, IOException, ParseException, TimeoutException
	{
		if ( publicationFutureLists.size() > 0 )
		{
			// list/set of selected publication, either from database or completely new 
			List<Publication> selectedPublications = new ArrayList<Publication>();

			// first, construct the publication
			// get it from database or create new if still doesn't exist
			this.constructPublicationWithSources( selectedPublications, publicationFutureLists , author );
			
			// process log
			applicationService.putProcessLog( pid, "Done in merging publication list<br><br>", "append" );

			// process log
			applicationService.putProcessLog( pid, "Removing incorrect publications...<br>", "append" );

			// second, remove incorrect publication based on investigation
			this.removeIncorrectPublicationFromPublicationList( selectedPublications );

			// process log
			applicationService.putProcessLog( pid, "Done removing incorrect publications<br><br>", "append" );

			// process log
			applicationService.putProcessLog( pid, "Extracting publications details...<br>", "append" );

			// third, extract and combine information from multiple sources
			this.extractPublicationInformationDetailFromSources( selectedPublications, author, sourceMap );

			// process log
			applicationService.putProcessLog( pid, "Done extracting publications details<br><br>", "append" );

			// fourth, second checking, after the information has been merged
			this.removeIncorrectPublicationPhase2FromPublicationList( selectedPublications );

			// process log
			applicationService.putProcessLog( pid, "Extracting publication information from PDF and Html...<br>", "append" );

			// enrich the publication information by extract information
			// from html or pdf source
			this.enrichPublicationByExtractOriginalSources( selectedPublications, author, false );

			// process log
			applicationService.putProcessLog( pid, "Done extracting publication information from PDF and Html<br><br>", "append" );

			// at the end save everything
			for ( Publication publication : selectedPublications )
			{
				publication.setContentUpdated( true );
				persistenceStrategy.getPublicationDAO().persist( publication );
			}
		}
		
	}
	
	/**
	 * Remove all publication that considered incorrect.
	 * 
	 * @param selectedPublications
	 */
	private void removeIncorrectPublicationFromPublicationList( List<Publication> selectedPublications )
	{
		// get current year
		int currentYear = Calendar.getInstance().get( Calendar.YEAR );

		for ( Iterator<Publication> iteratorPublication = selectedPublications.iterator(); iteratorPublication.hasNext(); )
		{
			Publication publication = iteratorPublication.next();

			// The pattern of incorrect publication
			if ( publication.getPublicationSources().size() == 1 )
			{
				List<PublicationSource> publicationSource = new ArrayList<>( publication.getPublicationSources() );

				// For google scholar :
				// 1. the publications don't have publication date.
				// 2. No other publications cited the incorrect publications for
				// years (more then 3 years)
				// 3. The title of publication contains "special issue article"
				if ( publicationSource.get( 0 ).getSourceType().equals( SourceType.GOOGLESCHOLAR ) )
				{
					// removing condition
					if ( publicationSource.get( 0 ).getDate() == null )
					{
						iteratorPublication.remove();
						continue;
					}
					else
					{
						if ( publicationSource.get( 0 ).getCitedBy() == 0 && currentYear - Integer.parseInt( publicationSource.get( 0 ).getDate() ) > 2 )
						{
							iteratorPublication.remove();
							continue;
						}
					}
				}
				// The pattern of incorrect publication
				// For MAS no author name
				else if ( publicationSource.get( 0 ).getSourceType().equals( SourceType.MAS ) )
				{
					if ( publicationSource.get( 0 ).getCoAuthors() == null || publicationSource.get( 0 ).getCoAuthors().equals( "" ) )
					{
						iteratorPublication.remove();
						continue;
					}
				}

				// The pattern of incorrect publication
				// For Mendeley is master thesis also recorded
				else if ( publicationSource.get( 0 ).getSourceType().equals( SourceType.MENDELEY ) )
				{
					if ( publicationSource.get( 0 ).getAbstractText() != null && publicationSource.get( 0 ).getAbstractText().contains( "master thesis" ) )
					{
						iteratorPublication.remove();
						continue;
					}
				}
			}

			// The pattern of incorrect publication
			// For google scholar :
			// 3. The title of publication contains "special issue article"
			if ( publication.getTitle().toLowerCase().contains( "special issue article" ) )
			{
				iteratorPublication.remove();
				continue;
			}

		}
	}

	/**
	 * Remove duplicated publication (have partial title)
	 * 
	 * @param selectedPublications
	 */
	private void removeIncorrectPublicationPhase2FromPublicationList( List<Publication> selectedPublications )
	{
		for ( Iterator<Publication> iteratorPublication = selectedPublications.iterator(); iteratorPublication.hasNext(); )
		{
			Publication publication = iteratorPublication.next();

			// The pattern of incorrect publication
			// For google scholar :
			// 4. sometimes the publication is duplicated,
			// the title of duplicated one is substring the correct one
			// e.g. "Teaching Collaborative Software Development"
			// actual title "Teaching collaborative software development: A case
			// study."
			// only check for title that shorter than 80 characters
			if ( publication.getTitle().length() < 80 )
			{
				if ( isPublicationDuplicated( publication, selectedPublications ) )
				{
					iteratorPublication.remove();
					continue;
				}
			}

		}
	}

	/**
	 * Check duplicated publication, compare publication to each other
	 * 
	 * @param publication
	 * @param selectedPublications
	 * @param maxLenghtToCompare
	 * @return
	 */
	private boolean isPublicationDuplicated( Publication publication, List<Publication> selectedPublications )
	{
		int lengthOfComparedTitleText = 40; 
		int lengthOfComparedAbstractText = 40;
		for ( Publication eachPublication : selectedPublications )
		{
			if ( eachPublication.getTitle().length() > publication.getTitle().length() )
			{
				// check title
				if( publication.getTitle().length() < lengthOfComparedTitleText )
					lengthOfComparedTitleText = publication.getTitle().length();
				String compareTitle1 = publication.getTitle().substring( 0, lengthOfComparedTitleText );
				String compareTitle2 = eachPublication.getTitle().substring( 0, lengthOfComparedTitleText );
				if ( palmAnalitics.getTextCompare().getDistanceByLuceneLevenshteinDistance( compareTitle1.toLowerCase(), compareTitle2.toLowerCase() ) > .9f ){
					// check abstract
					if ( eachPublication.getAbstractText() == null || eachPublication.getAbstractText().length() < lengthOfComparedAbstractText )
						continue;

					if( publication.getAbstractText() == null || publication.getAbstractText().length() < 100 )
						// just delete publication without abstract or short abstract
						return true;
					else{
						String compareAbstract1 = publication.getAbstractText().substring( 0, lengthOfComparedAbstractText );
						String compareAbstract2 = eachPublication.getAbstractText().substring( 0, lengthOfComparedAbstractText );
						if ( palmAnalitics.getTextCompare().getDistanceByLuceneLevenshteinDistance( compareAbstract1.toLowerCase(), compareAbstract2.toLowerCase() ) > .9f )
							return true;
					}
				}
			}
		}
		return false;
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
				// here, if process has not been completed yet. It will wait,
				// until process complete
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
						publication.setAbstractStatus( CompletionStatus.NOT_COMPLETE );
						publication.setKeywordStatus( CompletionStatus.NOT_COMPLETE );
						selectedPublications.add( publication );
					}

					// create publication sources and assign it to publication
					PublicationSource publicationSource = new PublicationSource();
					publicationSource.setTitle( publicationTitle );
					publicationSource.setSourceUrl( publicationMap.get( "url" ) );
					
					publicationSource.setSourceType( SourceType.valueOf(publicationMap.get( "source" ).toUpperCase() ) );
					
					if( publicationSource.getSourceType().equals( SourceType.GOOGLESCHOLAR ) ||  
							publicationSource.getSourceType().equals( SourceType.CITESEERX ) ||
							publicationSource.getSourceType().equals( SourceType.DBLP ))
						publicationSource.setSourceMethod( SourceMethod.PARSEPAGE );
					else if ( publicationSource.getSourceType().equals( SourceType.MENDELEY ) ||  
							publicationSource.getSourceType().equals( SourceType.MAS ))
						publicationSource.setSourceMethod( SourceMethod.API );
					
					publicationSource.setPublication( publication );

					if ( publicationMap.get( "citedby" ) != null )
						publicationSource.setCitedBy( Integer.parseInt( publicationMap.get( "citedby" ) ) );

					if ( publicationMap.get( "coauthor" ) != null )
						publicationSource.setCoAuthors( publicationMap.get( "coauthor" ) );

					if ( publicationMap.get( "coauthorUrl" ) != null )
						publicationSource.setCoAuthorsUrl( publicationMap.get( "coauthorUrl" ) );

					if ( publicationMap.get( "year" ) != null )
						publicationSource.setDate( publicationMap.get( "year" ) );

					if ( publicationMap.get( "doc" ) != null )
						publicationSource.setMainSource( publicationMap.get( "doc" ) );

					if ( publicationMap.get( "doc_url" ) != null )
						publicationSource.setMainSourceUrl( publicationMap.get( "doc_url" ) );

					if ( publicationMap.get( "type" ) != null )
						publicationSource.setPublicationType( publicationMap.get( "type" ) );
					
					if ( publicationMap.get( "abstract" ) != null && publicationMap.get( "abstract" ).length() > 250 )
						publicationSource.setAbstractText( publicationMap.get( "abstract" ) );

					if ( publicationMap.get( "keyword" ) != null )
						publicationSource.setKeyword( publicationMap.get( "keyword" ) );

					// add venue detail for DBLP
					if ( publicationSource.getSourceType().equals( SourceType.DBLP ) )
					{
						// venue url
						if ( publicationMap.get( "event_url" ) != null )
							publicationSource.setVenueUrl( publicationMap.get( "event_url" ) );
					}

					publication.addPublicationSource( publicationSource );
								
					
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
	 * @param sourceMap
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws ParseException
	 */
	public void extractPublicationInformationDetailFromSources( List<Publication> selectedPublications, Author pivotAuthor, Map<String, Source> sourceMap ) throws IOException, InterruptedException, ExecutionException, ParseException
	{
		// multithread publication source
		List<Future<PublicationSource>> publicationSourceFutureList = new ArrayList<Future<PublicationSource>>();
		for( Publication publication : selectedPublications){
			for ( PublicationSource publicationSource : publication.getPublicationSources() )
			{
				// handling publication source ( Only for google Scholar and
				// CiteseerX)
				if ( publicationSource.getSourceMethod().equals( SourceMethod.PARSEPAGE ) )
				{
					if ( publicationSource.getSourceType().equals( SourceType.GOOGLESCHOLAR ) )
						publicationSourceFutureList.add( asynchronousCollectionService.getPublicationInformationFromGoogleScholar( publicationSource, sourceMap.get( SourceType.GOOGLESCHOLAR.toString() ) ) );
					else if ( publicationSource.getSourceType().equals( SourceType.CITESEERX ) )
						publicationSourceFutureList.add( asynchronousCollectionService.getPublicationInformationFromCiteseerX( publicationSource, sourceMap.get( SourceType.CITESEERX.toString() ) ) );
				}
			}
		}

		// make sure everything is done
		for ( Future<PublicationSource> publicationSourceFuture : publicationSourceFutureList )
		{
			publicationSourceFuture.get();
		}

		for ( Publication selectedPublication : selectedPublications )
		{
			// combine from sources to publication
			this.mergingPublicationInformation( selectedPublication, pivotAuthor/* , coAuthors */ );
		}
	}

	/**
	 * 
	 * @param selectedPublications
	 * @throws ParseException
	 */
	public void mergingPublicationInformation( Publication publication,
			Author pivotAuthor/* , List<Author> coAuthors */ ) throws ParseException
	{
		DateFormat dateFormat = new SimpleDateFormat( "yyyy/M/d", Locale.ENGLISH );
		Calendar calendar = Calendar.getInstance();
		Set<String> existingMainSourceUrl = new HashSet<String>();

		for ( PublicationSource pubSource : publication.getPublicationSources() )
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
					publication.setPublicationDate( publicationDate );
					publication.setPublicationDateFormat( publicationDateFormat );
				}

				if ( pubSource.getPages() != null )
				{
					String[] pageSplit = pubSource.getPages().split( "-" );
					if ( pageSplit.length == 2 )
					{
						try
						{
							publication.setStartPage( Integer.parseInt( pageSplit[0] ) );
							publication.setEndPage( Integer.parseInt( pageSplit[1] ) );
						}
						catch ( Exception e )
						{
						}
					}
				}

				if ( pubSource.getPublisher() != null )
					publication.setPublisher( pubSource.getPublisher() );

				if ( pubSource.getIssue() != null )
					publication.setIssue( pubSource.getIssue() );

				if ( pubSource.getVolume() != null )
					publication.setVolume( pubSource.getVolume() );

			}
			else if ( pubSource.getSourceType() == SourceType.CITESEERX )
			{
				// nothing to do
			}
			else if ( pubSource.getSourceType() == SourceType.MENDELEY )
			{
				if ( !publication.getAbstractStatus().equals( CompletionStatus.COMPLETE ) && pubSource.getAbstractText() != null && pubSource.getAbstractText().length() > 250 )
				{
					publication.setAbstractText( pubSource.getAbstractText() );
					publication.setAbstractStatus( CompletionStatus.COMPLETE );
				}
				if ( !publication.getKeywordStatus().equals( CompletionStatus.COMPLETE ) && pubSource.getKeyword() != null )
				{
					publication.setKeywordText( pubSource.getKeyword() );
					publication.setKeywordStatus( CompletionStatus.COMPLETE );
				}

			}
			else if ( pubSource.getSourceType() == SourceType.MAS )
			{
				if ( !publication.getAbstractStatus().equals( CompletionStatus.COMPLETE ) && pubSource.getAbstractText() != null && pubSource.getAbstractText().length() > 250 )
				{ // sometimes MAS abstract is also incorrect
					if ( publication.getAbstractText() != null && publication.getAbstractText().length() < pubSource.getAbstractText().length() )
					{
						publication.setAbstractText( pubSource.getAbstractText() );
						publication.setAbstractStatus( CompletionStatus.PARTIALLY_COMPLETE );
					}
				}
				if ( !publication.getKeywordStatus().equals( CompletionStatus.COMPLETE ) && pubSource.getKeyword() != null )
				{
					publication.setKeywordText( pubSource.getKeyword() );
					publication.setKeywordStatus( CompletionStatus.PARTIALLY_COMPLETE );
				}
			}
			// for general information
			// author
			if ( pubSource.getCoAuthors() != null )
			{
				String[] authorsArray = pubSource.getCoAuthors().split( "," );
				// for DBLP where the coauthor have a source link
				String[] authorsUrlArray = null;
				if ( pubSource.getCoAuthorsUrl() != null )
					authorsUrlArray = pubSource.getCoAuthorsUrl().split( " " );

				if ( authorsArray.length > publication.getCoAuthors().size() )
				{
					for ( int i = 0; i < authorsArray.length; i++ )
					{
						String authorString = authorsArray[i].toLowerCase().replace( ".", "" ).trim();

						if ( authorString.equals( "" ) )
							continue;

						String[] splitName = authorString.split( " " );
						String lastName = splitName[splitName.length - 1];
						String firstName = authorString.substring( 0, authorString.length() - lastName.length() ).trim();
						
						Author author = null;
						if ( pivotAuthor.getName().toLowerCase().equals( authorString.toLowerCase() ) )
						{
							author = pivotAuthor;

							// create the relation with publication
							PublicationAuthor publicationAuthor = new PublicationAuthor();
							publicationAuthor.setPublication( publication );
							publicationAuthor.setAuthor( author );
							publicationAuthor.setPosition( i + 1 );

							publication.addPublicationAuthor( publicationAuthor );
						}
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
									for ( Author coAuthorDb : coAuthorsDb )
									{
										if ( coAuthorDb.isAliasNameFromFirstName( firstName ) )
										{
											// TODO: check with institution for
											// higher acuracy
											persistenceStrategy.getAuthorDAO().persist( coAuthorDb );

											author = coAuthorDb;
											break;
										}
									}
								}
							}

							// if author null, create new one
							if( author == null ){
								// create new author
								author = new Author();
								// set for all possible name
								author.setPossibleNames( authorString );
	
								// save new author
								persistenceStrategy.getAuthorDAO().persist( author );
							}

							// make a relation between author and publication
							PublicationAuthor publicationAuthor = new PublicationAuthor();
							publicationAuthor.setPublication( publication );
							publicationAuthor.setAuthor( author );
							publicationAuthor.setPosition( i + 1 );

							publication.addPublicationAuthor( publicationAuthor );

							// assign with authorSource, if exist
//							if ( authorsUrlArray != null && !author.equals( pivotAuthor ) )
//							{
//								AuthorSource authorSource = new AuthorSource();
//								authorSource.setName( author.getName() );
//								authorSource.setSourceUrl( authorsUrlArray[i] );
//								authorSource.setSourceType( pubSource.getSourceType() );
//								authorSource.setAuthor( author );
//
//								author.addAuthorSource( authorSource );
//								// persist new source
//								persistenceStrategy.getAuthorDAO().persist( author );
//							}
						}
					}
				}
			}

			// abstract ( searching the longest)
			if ( !publication.getAbstractStatus().equals( CompletionStatus.COMPLETE ) && pubSource.getAbstractText() != null && pubSource.getAbstractText().length() > 250 )
			{
				if ( publication.getAbstractText() == null || publication.getAbstractText().length() < pubSource.getAbstractText().length() )
				{
					publication.setAbstractText( pubSource.getAbstractText() );
					publication.setAbstractStatus( CompletionStatus.PARTIALLY_COMPLETE );
				}
			}

			// keyword
			if ( !publication.getKeywordStatus().equals( CompletionStatus.COMPLETE ) && pubSource.getKeyword() != null )
			{
				if ( publication.getKeywordText() == null )
				{
					publication.setKeywordText( pubSource.getKeyword() );
					publication.setKeywordStatus( CompletionStatus.PARTIALLY_COMPLETE );
				}
			}

			if ( publication.getPublicationDate() == null && publicationDate == null && pubSource.getDate() != null )
			{
				publicationDate = dateFormat.parse( pubSource.getDate() + "/1/1" );
				publication.setPublicationDate( publicationDate );
				publication.setPublicationDateFormat( "yyyy" );
			}

			if ( pubSource.getCitedBy() > 0 && pubSource.getCitedBy() > publication.getCitedBy() )
				publication.setCitedBy( pubSource.getCitedBy() );

			// venuetype
			if ( pubSource.getPublicationType() != null )
			{
				publicationType = PublicationType.valueOf( pubSource.getPublicationType() );
				publication.setPublicationType( publicationType );


				if ( ( publicationType.equals( "CONFERENCE" ) || publicationType.equals( "JOURNAL" ) ) && pubSource.getVenue() != null && publication.getEvent() == null )
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
								publication.setEvent( event );
							}
						}
					}
				}
			}

			// original source
			if ( pubSource.getMainSourceUrl() != null )
			{

				String[] mainSourceUrls = pubSource.getMainSourceUrl().split( " " );
				String[] mainSources = pubSource.getMainSource().split( "," );
				for ( int i = 0; i < mainSourceUrls.length; i++ )
				{
					if ( existingMainSourceUrl.contains( mainSourceUrls[i] ) )
						continue;

					existingMainSourceUrl.add( mainSourceUrls[i] );

					// not exist create new
					PublicationFile pubFile = new PublicationFile();
					pubFile.setSourceType( pubSource.getSourceType() );
					pubFile.setUrl( mainSourceUrls[i] );
					if ( mainSources[i].equals( "null" ) )
						pubFile.setSource( pubSource.getSourceType().toString().toLowerCase() );
					else
						pubFile.setSource( mainSources[i] );

					if ( mainSourceUrls[i].toLowerCase().endsWith( ".pdf" ) || mainSourceUrls[i].toLowerCase().endsWith( "pdf.php" ) || mainSources[i].toLowerCase().contains( "pdf" ) )
						pubFile.setFileType( FileType.PDF );
					else if ( mainSourceUrls[i].toLowerCase().endsWith( ".xml" ) )
					{
						// nothing to do
					}
					else
						pubFile.setFileType( FileType.HTML );
					pubFile.setPublication( publication );

					if ( pubFile.getFileType() != null )
						publication.addPublicationFile( pubFile );

				}
			}

		}

	}

	/**
	 * Extract publication information from original source either as html or
	 * pdf with asynchronous multi threads
	 * 
	 * @param publication
	 * @param pivotAuthor
	 * @param persistResult
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	public void enrichPublicationByExtractOriginalSources( List<Publication> selectedPublications, Author pivotAuthor, boolean persistResult ) throws IOException, InterruptedException, ExecutionException, TimeoutException
	{
		log.info( "Start publications enrichment for Auhtor " + pivotAuthor.getName() );
		List<Future<Publication>> selectedPublicationFutureList = new ArrayList<Future<Publication>>();

		for ( Publication publication : selectedPublications )
		{
			// only proceed for publication with not complete abstract
			if ( !publication.getAbstractStatus().equals( CompletionStatus.COMPLETE ) )
				selectedPublicationFutureList.add( asynchronousPublicationDetailCollectionService.asyncEnrichPublicationInformationFromOriginalSource( publication ) );
		}

		// check process completion
		for ( Future<Publication> selectedPublicationFuture : selectedPublicationFutureList )
		{
			Publication publication = selectedPublicationFuture.get();

			if ( persistResult )
			{
				publication.setContentUpdated( true );
				persistenceStrategy.getPublicationDAO().persist( publication );
			}
		}
		log.info( "Done publications enrichment for Auhtor " + pivotAuthor.getName() );
	}

}

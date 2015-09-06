package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.http.ParseException;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorAlias;
import de.rwth.i9.palm.model.AuthorSource;
import de.rwth.i9.palm.model.Institution;
import de.rwth.i9.palm.model.Source;
import de.rwth.i9.palm.model.SourceType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.ApplicationService;

@Service
public class ResearcherCollectionService
{
	private final static Logger log = LoggerFactory.getLogger( ResearcherCollectionService.class );

	@Autowired
	private AsynchronousAuthorCollectionService asynchronousAuthorCollectionService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private ApplicationService applicationService;

	@Autowired
	private MendeleyOauth2Helper mendeleyOauth2Helper;

	/**
	 * Gather researcher from academic networks
	 * 
	 * @param query
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws OAuthProblemException
	 * @throws OAuthSystemException
	 * @throws ParseException
	 */
	@Transactional
	public void collectAuthorInformationFromNetwork( String query ) throws IOException, InterruptedException, ExecutionException, ParseException, OAuthSystemException, OAuthProblemException
	{
		// container
		List<Future<List<Map<String, String>>>> authorFutureLists = new ArrayList<Future<List<Map<String, String>>>>();

		// getSourceMap
		Map<String, Source> sourceMap = applicationService.getAcademicNetworkSources();

		// loop through all source which is active
		for ( Map.Entry<String, Source> sourceEntry : sourceMap.entrySet() )
		{
			Source source = sourceEntry.getValue();
			if ( source.getSourceType().equals( SourceType.GOOGLESCHOLAR ) && source.isActive() )
				authorFutureLists.add( asynchronousAuthorCollectionService.getListOfAuthorsGoogleScholar( query, source ) );
			else if ( source.getSourceType().equals( SourceType.CITESEERX ) && source.isActive() )
				authorFutureLists.add( asynchronousAuthorCollectionService.getListOfAuthorsCiteseerX( query, source ) );
			else if ( source.getSourceType().equals( SourceType.DBLP ) && source.isActive() )
				authorFutureLists.add( asynchronousAuthorCollectionService.getListOfAuthorsDblp( query, source ) );
			else if ( source.getSourceType().equals( SourceType.MENDELEY ) && source.isActive() )
			{
				// check for token validity
				mendeleyOauth2Helper.checkAndUpdateMendeleyToken( source );
				authorFutureLists.add( asynchronousAuthorCollectionService.getListOfAuthorsMendeley( query, source ) );
			}
		}

		// merge the result
		this.mergeAuthorInformation( authorFutureLists );
	}

	/**
	 * Merging author information from multiple resources
	 * 
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
				List<Map<String, String>> authorListMap = authorFutureList.get();
				for ( Map<String, String> authorMap : authorListMap )
				{
					String authorName = authorMap.get( "name" ).toLowerCase().replace( ".", "" ).trim();

					if ( authorName == null )
						continue;

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

			// remove author if its only from mendeley
			// since mendeley also put non researcher on its api result
			// the source ulr of mendeley will be "MENDELEY"
			// which are less then 10 character in length
			for ( Iterator<Map<String, String>> iteratorAuthor = mergedAuthorList.iterator(); iteratorAuthor.hasNext(); )
			{
				Map<String, String> authorMap = iteratorAuthor.next();

				if ( authorMap.get( "source" ).length() < 10 )
					iteratorAuthor.remove();
			}

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
					String lastName = splitName[splitName.length - 1];
					author.setLastName( lastName );
					String firstName = name.substring( 0, name.length() - lastName.length() ).replace( ".", "" ).trim();
					if ( !firstName.equals( "" ) )
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

						author.addInstitution( institutionObject );
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

				if ( mergedAuthor.get( "citedby" ) != null )
					author.setCitedBy( Integer.parseInt( mergedAuthor.get( "citedby" ) ) );

				// TODO, specific to mendeley add adacemic_status and
				// discipline/field

				// insert source
				Set<AuthorSource> authorSources = new LinkedHashSet<AuthorSource>();

				if ( mergedAuthor.get( "source" ) != null && mergedAuthor.get( "url" ) != null )
				{
					String[] sources = mergedAuthor.get( "source" ).split( " " );
					String[] sourceUrls = mergedAuthor.get( "url" ).split( " " );
					for ( int i = 0; i < sources.length; i++ )
					{
						if ( !sources[i].equals( "" ) )
						{
							AuthorSource as = new AuthorSource();
							as.setName( name );
							as.setSourceUrl( sourceUrls[i] );
							as.setSourceType( SourceType.valueOf( sources[i].toUpperCase() ) );
							as.setAuthor( author );

							authorSources.add( as );
						}
					}
				}
				author.setAuthorSources( authorSources );

				persistenceStrategy.getAuthorDAO().persist( author );
			}

		}

	}

}

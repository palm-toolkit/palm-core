package de.rwth.i9.palm.feature.researcher;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.datasetcollect.service.PublicationCollectionService;
import de.rwth.i9.palm.datasetcollect.service.ResearcherCollectionService;
import de.rwth.i9.palm.helper.DateTimeHelper;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorSource;
import de.rwth.i9.palm.model.Source;
import de.rwth.i9.palm.model.SourceMethod;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.ApplicationService;
import de.rwth.i9.palm.util.IdentifierFactory;

@Component
public class ResearcherMiningImpl implements ResearcherMining
{
	private final static Logger LOGGER = LoggerFactory.getLogger( ResearcherMiningImpl.class );

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private PublicationCollectionService publicationCollectionService;

	@Autowired
	private ApplicationService applicationService;

	@Autowired
	private ResearcherCollectionService researcherCollectionService;

	@Override
	public Map<String, Object> fetchResearcherData( String id, String name, String uri, String affiliation, String pid, String force, List<Author> sessionAuthors ) throws IOException, InterruptedException, ExecutionException, ParseException, TimeoutException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		// check author on session
		Author author = null;

		boolean isAuthorFromSession = false;
		// get author from session
		if ( sessionAuthors != null && !sessionAuthors.isEmpty() )
		{
			if ( id != null )
			{
				for ( Author sessionAuthor : sessionAuthors )
				{
					if ( sessionAuthor.getId().equals( id ) )
					{
						author = sessionAuthor;
						isAuthorFromSession = true;
						break;
					}
				}
			}
		}

		// get author from database
		if ( author == null )
			author = this.getTargetAuthor( responseMap, id, name, uri, affiliation );

		if ( author == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "error-msg", "author not found in system" );
			return responseMap;
		}

		// pid must exist
		if ( pid == null )
			pid = IdentifierFactory.getNextDefaultIdentifier();

		responseMap.put( "status", "ok" );

		// -- check if author source complete for active source
		boolean isSourceParsePageMissing = false;
		// first get active sources
		Map<String, Source> sourceMap = applicationService.getAcademicNetworkSources();

		// author from session means that the author just added
		// loop through all source which is active
		if ( !isAuthorFromSession )
		{
			for ( Map.Entry<String, Source> sourceEntry : sourceMap.entrySet() )
			{
				Source source = sourceEntry.getValue();
				// only check for active source and parse page method
				if ( source.isActive() && source.getSourceMethod().equals( SourceMethod.PARSEPAGE ) )
				{
					if ( !author.isContainSource( source ) )
					{
						isSourceParsePageMissing = true;
						break;
					}
				}
			}
		}


		// try to get missing source
		if ( isSourceParsePageMissing )
		{
			// set to false, with assumption that source not missing, but in
			// reality is not exist
			isSourceParsePageMissing = false;
			List<Author> researcherList = researcherCollectionService.collectAuthorInformationFromNetwork( author.getName(), false );

			if ( researcherList != null && !researcherList.isEmpty() )
			{
				// try to add author source
				for ( Author researcher : researcherList )
				{
					if ( researcher.getName().equals( author.getName() ) )
					{

						if ( researcher.getAuthorSources() == null || researcher.getAuthorSources().isEmpty() )
							continue;

						for ( AuthorSource as : researcher.getAuthorSources() )
						{
							if ( sourceMap.get( as.getSourceType().toString() ).getSourceMethod().equals( SourceMethod.PARSEPAGE ) )
							{
								author.addAuthorSource( as );
								isSourceParsePageMissing = true;
							}
						}
					}
				}
			}
		}

		// check whether it is necessary to collect information from network
		if ( this.isFetchDatasetFromNetwork( author ) || isSourceParsePageMissing || force.equals( "true" ) )
		{
			publicationCollectionService.collectPublicationListFromNetwork( responseMap, author, pid );
			responseMap.put( "fetchPerformed", "yes" );
		}
		else
			responseMap.put( "fetchPerformed", "no" );

		Author targetAuthor = persistenceStrategy.getAuthorDAO().getById( id );
		// get basic author information
		Map<String, Object> authorMap = new LinkedHashMap<String, Object>();
		authorMap.put( "id", targetAuthor.getId() );
		authorMap.put( "name", targetAuthor.getName() );
		if ( targetAuthor.getPhotoUrl() != null )
			authorMap.put( "photo", targetAuthor.getPhotoUrl() );
		if ( targetAuthor.getAcademicStatus() != null )
			authorMap.put( "status", targetAuthor.getAcademicStatus() );
		if ( targetAuthor.getInstitution() != null )
			authorMap.put( "aff", targetAuthor.getInstitution().getName() );
		if ( targetAuthor.getCitedBy() > 0 )
			authorMap.put( "citedBy", targetAuthor.getCitedBy() );

		if ( targetAuthor.getPublicationAuthors() != null )
			authorMap.put( "publicationsNumber", targetAuthor.getPublicationAuthors().size() );
		else
			authorMap.put( "publicationsNumber", 0 );

		responseMap.put( "author", authorMap );

		return responseMap;
	}

	/**
	 * Get author object based on query
	 * 
	 * @param responseMap
	 * @param id
	 * @param name
	 * @param uri
	 * @param affiliation
	 * @return
	 */
	private Author getTargetAuthor( Map<String, Object> responseMap, String id, String name, String uri, String affiliation )
	{
		Author author = null;
		if ( id == null && name == null && uri == null )
		{
			responseMap.put( "result", "error" );
			responseMap.put( "reason", "no author selected" );
		}
		else
		{

			if ( id != null )
				author = persistenceStrategy.getAuthorDAO().getById( id );
			else if ( uri != null )
				author = persistenceStrategy.getAuthorDAO().getByUri( uri );
			else if ( name != null && affiliation != null )
			{
				List<Author> authors = persistenceStrategy.getAuthorDAO().getAuthorByNameAndInstitution( name, affiliation );
				if ( !authors.isEmpty() )
					author = authors.get( 0 );
			}

			if ( author == null )
			{
				responseMap.put( "result", "error" );
				responseMap.put( "reason", "no author found" );
			}
			// add author information
			// responseMap.put( "id", author.getId() );
			// responseMap.put( "name", author.getName() );
		}
		return author;
	}

	/**
	 * Check whether fetching to network is necessary
	 * 
	 * @param author
	 * @return
	 */
	private boolean isFetchDatasetFromNetwork( Author author )
	{
		// get configuration, after how many hour collecting process of
		// researcher publication need to be repeated again
		String collectAfterHours = applicationService.getConfigValue( "researcher", "setting", "collect every" );
		// by default is around 2 weeks
		int collectAfter = 336;
		// alter default value if any
		if ( collectAfterHours != null )
			try
			{
				collectAfter = Integer.parseInt( collectAfterHours );
			}
			catch ( Exception e )
			{
			}

		// get current timestamp
		java.util.Date date = new java.util.Date();
		Timestamp currentTimestamp = new Timestamp( date.getTime() );
		if ( author.getRequestDate() != null )
		{
			// check if the existing author publication is obsolete
			if ( DateTimeHelper.substractTimeStampToHours( currentTimestamp, author.getRequestDate() ) > collectAfter )
			{
				// update current timestamp
				author.setRequestDate( currentTimestamp );
				persistenceStrategy.getAuthorDAO().persist( author );
				return true;
			}
		}
		else
		{
			// update current timestamp
			author.setRequestDate( currentTimestamp );
			persistenceStrategy.getAuthorDAO().persist( author );
			return true;
		}

		return false;
	}

}

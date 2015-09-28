package de.rwth.i9.palm.feature.researcher;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.datasetcollect.service.PublicationCollectionService;
import de.rwth.i9.palm.datasetcollect.service.ResearcherCollectionService;
import de.rwth.i9.palm.helper.DateTimeHelper;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Institution;
import de.rwth.i9.palm.model.RequestType;
import de.rwth.i9.palm.model.UserRequest;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class ResearcherSearchImpl implements ResearcherSearch
{

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private ResearcherCollectionService researcherCollectionService;

	@Autowired
	private PublicationCollectionService publicationCollectionService;

	@Override
	public Map<String, Object> getResearcherListByQuery( String query, Integer page, Integer maxresult ) throws IOException, InterruptedException, ExecutionException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
	{
		boolean collectFromNetwork = false;

		if ( query == null )
			query = "";

		if ( page == null )
			page = 0;

		if ( maxresult == null )
			maxresult = 50;

		if ( !query.equals( "" ) && page == 0 )
		{
			// check whether the author query ever executed before
			UserRequest userRequest = persistenceStrategy.getUserRequestDAO().getByTypeAndQuery( RequestType.SEARCHAUTHOR, query );

			// get current timestamp
			java.util.Date date = new java.util.Date();
			Timestamp currentTimestamp = new Timestamp( date.getTime() );

			if ( userRequest == null )
			{ // there is no kind of request before
				// perform fetching data through academic network
				collectFromNetwork = true;
				// persist current request
				userRequest = new UserRequest();
				userRequest.setQueryString( query );
				userRequest.setRequestDate( currentTimestamp );
				userRequest.setRequestType( RequestType.SEARCHAUTHOR );
				persistenceStrategy.getUserRequestDAO().persist( userRequest );
			}
			else
			{
				// check if the existing userRequest obsolete (longer than a
				// week)
				if ( DateTimeHelper.substractTimeStampToHours( currentTimestamp, userRequest.getRequestDate() ) > 24 * 7 )
				{
					// update current timestamp
					userRequest.setRequestDate( currentTimestamp );
					persistenceStrategy.getUserRequestDAO().persist( userRequest );
					collectFromNetwork = true;
				}
			}

			collectFromNetwork = true;
			// collect author from network
			if ( collectFromNetwork )
				researcherCollectionService.collectAuthorInformationFromNetwork( query );
		}

		// get the researcher
		Map<String, Object> researcherMap = null;
		if ( collectFromNetwork )
			researcherMap = persistenceStrategy.getAuthorDAO().getAuthorWithPaging( query, page, maxresult );
		else
			researcherMap = persistenceStrategy.getAuthorDAO().getAuthorByFullTextSearchWithPaging( query, page, maxresult );

		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		responseMap.put( "query", query );
		responseMap.put( "page", page );
		responseMap.put( "maxresult", maxresult );

		// create the json structure for researcher list
		if ( researcherMap != null )
		{
			responseMap.put( "count", researcherMap.get( "count" ) );

			@SuppressWarnings( "unchecked" )
			List<Author> researchers = (List<Author>) researcherMap.get( "result" );
			List<Map<String, String>> researcherList = new ArrayList<Map<String, String>>();

			for ( Author researcher : researchers )
			{
				Map<String, String> pub = new LinkedHashMap<String, String>();
				pub.put( "id", researcher.getId() );
				pub.put( "name", WordUtils.capitalize( researcher.getName() ) );
				if ( researcher.getPhotoUrl() != null )
					pub.put( "photo", researcher.getPhotoUrl() );

				String otherDetail = "";
				if ( researcher.getOtherDetail() != null )
					otherDetail += researcher.getOtherDetail();
				if ( researcher.getDepartment() != null )
					otherDetail += ", " + researcher.getDepartment();
				if ( !otherDetail.equals( "" ) )
					pub.put( "detail", otherDetail );
				if ( researcher.getInstitutions() != null )
					for ( Institution institution : researcher.getInstitutions() )
					{
						if ( pub.get( "aff" ) != null )
							pub.put( "aff", pub.get( "aff" ) + ", " + institution.getName() );
						else
							pub.put( "aff", institution.getName() );
					}
				if ( researcher.getCitedBy() > 0 )
					pub.put( "citedBy", Integer.toString( researcher.getCitedBy() ) );

				researcherList.add( pub );
			}
			responseMap.put( "researcher", researcherList );

		}
		else
		{
			responseMap.put( "count", 0 );
		}

		return responseMap;// TODO Auto-generated method stub
	}

	@Override
	public Map<String, Object> fetchResearcherData( String id, String name, String uri, String affiliation, String force ) throws IOException, InterruptedException, ExecutionException, ParseException, TimeoutException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// get author
		Author author = this.getTargetAuthor( responseMap, id, name, uri, affiliation );
		if ( author == null )
			return responseMap;

		// check whether it is necessary to collect information from network
		if ( this.isFetchDatasetFromNetwork( author ) || force.equals( "true" ) )
			publicationCollectionService.collectPublicationListFromNetwork( responseMap, author );

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
			responseMap.put( "id", author.getId() );
			responseMap.put( "name", author.getName() );
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
		// get current timestamp
		java.util.Date date = new java.util.Date();
		Timestamp currentTimestamp = new Timestamp( date.getTime() );
		if ( author.getRequestDate() != null )
		{
			// check if the existing author publication is obsolete
			if ( DateTimeHelper.substractTimeStampToHours( currentTimestamp, author.getRequestDate() ) > 24 * 7 )
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

		// return false;
		return false;
	}

}

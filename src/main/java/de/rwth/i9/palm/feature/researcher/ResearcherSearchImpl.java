package de.rwth.i9.palm.feature.researcher;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.datasetcollect.service.ResearcherCollectionService;
import de.rwth.i9.palm.helper.DateTimeHelper;
import de.rwth.i9.palm.model.Author;
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

	@Override
	public Map<String, Object> getResearcherListByQuery( String query, Integer page, Integer maxresult ) throws IOException, InterruptedException, ExecutionException
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
				pub.put( "name", researcher.getName() );
				if ( researcher.getPhotoUrl() != null )
					pub.put( "photo", researcher.getPhotoUrl() );

				String otherDetail = "";
				if ( researcher.getOtherDetail() != null )
					otherDetail += researcher.getOtherDetail();
				if ( researcher.getDepartment() != null )
					otherDetail += ", " + researcher.getDepartment();
				if ( !otherDetail.equals( "" ) )
					pub.put( "detail", otherDetail );
				if ( researcher.getInstitution() != null )
					pub.put( "aff", researcher.getInstitution().getName() );
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

}

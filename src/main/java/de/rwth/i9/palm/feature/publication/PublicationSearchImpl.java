package de.rwth.i9.palm.feature.publication;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class PublicationSearchImpl implements PublicationSearch
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Override
	public Map<String, Object> getPublicationListByQueryAndConference( String query, String conferenceName, String conferenceId, Integer page, Integer maxresult )
	{
		if ( query == null )
			query = "";

		if ( page == null )
			page = 0;

		if ( maxresult == null )
			maxresult = 50;

		// get the publication
		Map<String, Object> publicationMap = persistenceStrategy.getPublicationDAO().getPublicationByFullTextSearchWithPaging( query, page, maxresult );

		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		responseMap.put( "query", query );
		responseMap.put( "page", page );
		responseMap.put( "maxresult", maxresult );

		// create the json structure for publication list
		if ( publicationMap != null )
		{
			responseMap.put( "count", publicationMap.get( "count" ) );

			@SuppressWarnings( "unchecked" )
			List<Publication> publications = (List<Publication>) publicationMap.get( "result" );
			List<Map<String, String>> publicationList = new ArrayList<Map<String, String>>();

			for ( Publication publication : publications )
			{
				Map<String, String> pub = new LinkedHashMap<String, String>();
				pub.put( "id", publication.getId() );
				pub.put( "title", publication.getTitle() );

				publicationList.add( pub );
			}
			responseMap.put( "publication", publicationList );

		}
		else
		{
			responseMap.put( "count", 0 );
		}

		return responseMap;
	}

}

package de.rwth.i9.palm.feature.publication;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.model.Author;
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

		// preparing data format
		DateFormat dateFormat = new SimpleDateFormat( "yyyy", Locale.ENGLISH );

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
			List<Map<String, Object>> publicationList = new ArrayList<Map<String, Object>>();

			for ( Publication publication : publications )
			{
				Map<String, Object> pub = new LinkedHashMap<String, Object>();
				pub.put( "id", publication.getId() );
				pub.put( "title", publication.getTitle() );
				if ( publication.getCitedBy() > 0 )
					pub.put( "cited", Integer.toString( publication.getCitedBy() ) );

				if ( publication.getPublicationDate() != null )
					pub.put( "year", dateFormat.format( publication.getPublicationDate() ) );

				List<Object> authorObject = new ArrayList<Object>();

				for ( Author author : publication.getCoAuthors() )
				{
					Map<String, Object> authorMap = new LinkedHashMap<String, Object>();
					authorMap.put( "id", author.getId() );
					authorMap.put( "name", author.getName() );
					if ( author.getInstitution() != null )
						authorMap.put( "aff", author.getInstitution().getName() );
					if ( author.getPhotoUrl() != null )
						authorMap.put( "photo", author.getPhotoUrl() );

					authorObject.add( authorMap );
				}
				pub.put( "author", authorObject );

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

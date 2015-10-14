package de.rwth.i9.palm.feature.publication;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.text.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Institution;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class PublicationSearchImpl implements PublicationSearch
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Override
	public Map<String, Object> getPublicationListByQueryAndEvent( String query, String eventName, String eventId, Integer page, Integer maxresult )
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

				if ( publication.getPublicationType() != null )
				{
					String publicationType = publication.getPublicationType().toString();
					publicationType = publicationType.substring( 0, 1 ).toUpperCase() + publicationType.toLowerCase().substring( 1 );
					pub.put( "type", publicationType );
				}

				pub.put( "title", publication.getTitle() );
				if ( publication.getCitedBy() > 0 )
					pub.put( "cited", Integer.toString( publication.getCitedBy() ) );

				if ( publication.getPublicationDate() != null )
					pub.put( "date published", dateFormat.format( publication.getPublicationDate() ) );

				List<Object> authorObject = new ArrayList<Object>();

				for ( Author author : publication.getCoAuthors() )
				{
					Map<String, Object> authorMap = new LinkedHashMap<String, Object>();
					authorMap.put( "id", author.getId() );
					authorMap.put( "name", WordUtils.capitalize( author.getName() ) );
					if ( author.getInstitutions() != null )
						for ( Institution institution : author.getInstitutions() )
						{
							if ( authorMap.get( "aff" ) != null )
								authorMap.put( "aff", authorMap.get( "aff" ) + ", " + institution.getName() );
							else
								authorMap.put( "aff", institution.getName() );
						}
					if ( author.getPhotoUrl() != null )
						authorMap.put( "photo", author.getPhotoUrl() );

					authorObject.add( authorMap );
				}
				pub.put( "authors", authorObject );

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

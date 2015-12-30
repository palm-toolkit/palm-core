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
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.Institution;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class PublicationSearchImpl implements PublicationSearch
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Override
	public Map<String, Object> getPublicationListByQuery( String query, String publicationType, String authorId, String eventId, Integer page, Integer maxresult, String source, String fulltextSearch, String orderBy )
	{
		Map<String, Object> publicationMap;

		Author author = null;
		if ( authorId != null )
			author = persistenceStrategy.getAuthorDAO().getById( authorId );

		Event event = null;
		if ( eventId != null )
			event = persistenceStrategy.getEventDAO().getById( eventId );

		// get the publication
		if( fulltextSearch.equals( "yes" )){
			publicationMap = persistenceStrategy.getPublicationDAO().getPublicationByFullTextSearchWithPaging( query, publicationType, author, event, page, maxresult, orderBy );
		} else {
			publicationMap = persistenceStrategy.getPublicationDAO().getPublicationWithPaging( query, publicationType, author, event, page, maxresult, orderBy );
		}

		return publicationMap;
	}

	@Override
	public Map<String, Object> printJsonOutput( Map<String, Object> responseMap, List<Publication> publications )
	{
		if ( publications == null || publications.isEmpty() )
		{
			responseMap.put( "count", 0 );
			return responseMap;
		}

		List<Map<String, Object>> publicationList = new ArrayList<Map<String, Object>>();

		// preparing data format
		DateFormat dateFormat = new SimpleDateFormat( "yyyy", Locale.ENGLISH );

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

		responseMap.put( "count", publicationList.size() );
		responseMap.put( "publications", publicationList );

		return responseMap;
	}

}

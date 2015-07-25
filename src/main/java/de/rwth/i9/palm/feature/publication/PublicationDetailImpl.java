package de.rwth.i9.palm.feature.publication;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class PublicationDetailImpl implements PublicationDetail
{

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Override
	public Map<String, Object> getPublicationDetailById( String publicationId )
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// get author
		Publication publication = persistenceStrategy.getPublicationDAO().getById( publicationId );
		if ( publication == null )
		{
			responseMap.put( "status", "Error - publication not found" );
			return responseMap;
		}

		responseMap.put( "status", "OK" );

		// put publication detail
		Map<String, Object> publicationMap = new LinkedHashMap<String, Object>();
		publicationMap.put( "title", publication.getTitle() );
		if ( publication.getAbstractText() != null )
			publicationMap.put( "abstract", publication.getAbstractText() );
		// coauthor
		List<Map<String, Object>> coathorList = new ArrayList<Map<String, Object>>();
		for ( Author author : publication.getCoAuthors() )
		{
			Map<String, Object> authorMap = new LinkedHashMap<String, Object>();
			authorMap.put( "id", author.getId() );
			authorMap.put( "name", author.getName() );
			if ( author.getInstitution() != null )
				authorMap.put( "aff", author.getInstitution().getName() );
			if ( author.getPhotoUrl() != null )
				authorMap.put( "photo", author.getPhotoUrl() );

			coathorList.add( authorMap );
		}
		publicationMap.put( "coauthor", coathorList );

		responseMap.put( "publication", publicationMap );

		return responseMap;
	}

}

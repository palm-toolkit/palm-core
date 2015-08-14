package de.rwth.i9.palm.feature.publication;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Institution;
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

		// get publication
		Publication publication = persistenceStrategy.getPublicationDAO().getById( publicationId );
		if ( publication == null )
		{
			responseMap.put( "status", "Error - publication not found" );
			return responseMap;
		}

		responseMap.put( "status", "OK" );

		// put publication detail
		Map<String, Object> publicationMap = new LinkedHashMap<String, Object>();
		publicationMap.put( "id", publication.getId() );
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

			coathorList.add( authorMap );
		}
		publicationMap.put( "coauthor", coathorList );
		if ( publication.getContentText() != null )
			publicationMap.put( "content", publication.getContentText() );
		if( publication.getPdfSource() != null )
			publicationMap.put( "pdf", publication.getPdfSource() );
		if( publication.getPdfSourceUrl() != null ){
			publicationMap.put( "pdfurl", publication.getPdfSourceUrl() );
			publicationMap.put( "pdfextract", publication.isPdfExtracted() );
		}
		if ( publication.getKeywordText() != null )
			publicationMap.put( "keyword", publication.getKeywordText() );
		if ( publication.getReferenceText() != null )
			publicationMap.put( "reference", publication.getReferenceText() );

		responseMap.put( "publication", publicationMap );

		return responseMap;
	}

}

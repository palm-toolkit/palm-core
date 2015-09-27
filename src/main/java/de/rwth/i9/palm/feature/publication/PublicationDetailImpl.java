package de.rwth.i9.palm.feature.publication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.text.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.helper.comparator.PublicationSourceBySourceTypeComparator;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Institution;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationFile;
import de.rwth.i9.palm.model.PublicationSource;
import de.rwth.i9.palm.model.SourceType;
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

		responseMap.put( "status", "ok" );

		// put publication detail
		Map<String, Object> publicationMap = new LinkedHashMap<String, Object>();
		publicationMap.put( "id", publication.getId() );
		publicationMap.put( "title", publication.getTitle() );
		if ( publication.getAbstractText() != null )
			publicationMap.put( "abstract", publication.getAbstractText() );
		if ( publication.getKeywordText() != null )
			publicationMap.put( "keyword", publication.getKeywordText().replace( ",", ", " ) );
		// coauthor
		List<Map<String, Object>> coathorList = new ArrayList<Map<String, Object>>();
		
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

			coathorList.add( authorMap );
		}
		publicationMap.put( "coauthor", coathorList );
		if ( publication.getContentText() != null )
			publicationMap.put( "content", publication.getContentText() );

		List<Object> publicationSourceList = new ArrayList<Object>();

		List<PublicationSource> publicationSources = new ArrayList<PublicationSource>( publication.getPublicationSources() );
		// sort publicationSource asc
		Collections.sort( publicationSources, new PublicationSourceBySourceTypeComparator() );
		// put publicationSource to Map and add into list
		for ( PublicationSource publicationSource : publicationSources )
		{
			Map<String, Object> publicationSourceMap = new LinkedHashMap<String, Object>();

			String sourceType = publicationSource.getSourceType().toString().toLowerCase();
			publicationSourceMap.put( "source", sourceType );

			if ( publicationSource.getTitle() != null )
				publicationSourceMap.put( "title", publicationSource.getTitle() );

			if ( publicationSource.getCoAuthors() != null )
				publicationSourceMap.put( "authors", publicationSource.getCoAuthors().replace( ",", ", " ) );

			if ( publicationSource.getAbstractText() != null )
				publicationSourceMap.put( "abstract", publicationSource.getAbstractText() );

			if ( publicationSource.getKeyword() != null )
				publicationSourceMap.put( "keyword", publicationSource.getKeyword().replace( ",", ", " ) );

			if ( publicationSource.getDate() != null )
				publicationSourceMap.put( "date", publicationSource.getDate() );

			if ( publicationSource.getCitedBy() > 0 )
				publicationSourceMap.put( "cited by", publicationSource.getCitedBy() );

			if ( publicationSource.getVenue() != null )
				publicationSourceMap.put( "venue", publicationSource.getVenue() );

			if ( publicationSource.getPublisher() != null )
				publicationSourceMap.put( "publisher", publicationSource.getPublisher() );

			if ( publicationSource.getIssue() != null )
				publicationSourceMap.put( "issue", publicationSource.getIssue() );

			if ( publicationSource.getVolume() != null )
				publicationSourceMap.put( "volume", publicationSource.getVolume() );

			if ( publicationSource.getPages() != null )
				publicationSourceMap.put( "page", publicationSource.getPages() );
			// add into list
			publicationSourceList.add( publicationSourceMap );
		}
		// put publicationSource into JSON
		publicationMap.put( "sources", publicationSourceList );

		PublicationFile publicationFile = null;

		if ( publication.getPublicationFiles() != null )
		{
			for ( PublicationFile pubFile : publication.getPublicationFiles() )
			{
				if ( publicationFile == null )
					publicationFile = pubFile;
				else
				{
					if ( pubFile.getSourceType().equals( SourceType.DBLP ) )
						publicationFile = pubFile;
				}

				if ( pubFile.isCorrectlyExtracted() )
				{
					publicationFile = pubFile;
					break;
				}
			}
		}

		if ( publicationFile != null )
		{
			publicationMap.put( "pdf", publicationFile.getUrl() );
			publicationMap.put( "pdfurl", publicationFile.getSource() );
			publicationMap.put( "pdfextract", publication.isPdfExtracted() );
		}

		responseMap.put( "publication", publicationMap );

		return responseMap;
	}

}

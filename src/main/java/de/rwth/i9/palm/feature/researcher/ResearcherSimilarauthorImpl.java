package de.rwth.i9.palm.feature.researcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class ResearcherSimilarauthorImpl implements ResearcherSimilarauthor
{
	@Autowired
	private PalmAnalytics palmAnalytics;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Override
	public Map<String, Object> getResearcherSimilarAuthorMap( Author author, int startPage, int maxresult )
	{
		// researchers list container
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		List<String> similarAuthors = new ArrayList<String>();
		similarAuthors = palmAnalytics.getNGrams().similarEntities( author.getId(), maxresult );

		// Prepare set of similarAuthor HashSet;
		Set<String> similarauthorSet = new HashSet<String>();

		for ( String similar : similarAuthors )
		{
			if ( persistenceStrategy.getAuthorDAO().getById( similar.split( "->" )[0] ).equals( author ) )
				continue;

			similarauthorSet.add( similar );
		}

		// prepare list of object map containing similarAuthor details
		List<Map<String, Object>> similarAuthorList = new ArrayList<Map<String, Object>>();

		for ( String similarAuthor : similarauthorSet )
		{
			// only copy necessary attributes
			// persistenceStrategy.( similar.split( "->"
			// )[0] )
			Map<String, Object> similarAuthorMap = new LinkedHashMap<String, Object>();
			similarAuthorMap.put( "id", similarAuthor.split( "->" )[0] );
			similarAuthorMap.put( "name", persistenceStrategy.getAuthorDAO().getById( similarAuthor.split( "->" )[0] ).getName() );
			if ( persistenceStrategy.getAuthorDAO().getById( similarAuthor.split( "->" )[0] ).getInstitution() != null )
				similarAuthorMap.put( "affiliation", persistenceStrategy.getAuthorDAO().getById( similarAuthor.split( "->" )[0] ).getInstitution().getName() );
			if ( persistenceStrategy.getAuthorDAO().getById( similarAuthor.split( "->" )[0] ).getPhotoUrl() != null )
				similarAuthorMap.put( "photo", persistenceStrategy.getAuthorDAO().getById( similarAuthor.split( "->" )[0] ).getPhotoUrl() );
			similarAuthorMap.put( "isAdded", persistenceStrategy.getAuthorDAO().getById( similarAuthor.split( "->" )[0] ).isAdded() );
			similarAuthorMap.put( "similarity", similarAuthor.split( "->" )[1] );

			// add into list
			similarAuthorList.add( similarAuthorMap );
		}

		// prepare list of object map containing similarAuthor details
		List<Map<String, Object>> similarAuthorListPaging = new ArrayList<Map<String, Object>>();

		int position = 0;
		for ( Map<String, Object> similarAuthor : similarAuthorList )
		{
			if ( position >= startPage && similarAuthorListPaging.size() < maxresult )
			{
				similarAuthorListPaging.add( similarAuthor );
			}
		}

		// remove unnecessary result

		// put similarAuthor to responseMap
		responseMap.put( "countTotal", similarAuthorList.size() );
		responseMap.put( "count", similarAuthorListPaging.size() );
		responseMap.put( "similarAuthors", similarAuthorListPaging );

		return responseMap;
	}

}

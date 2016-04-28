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
public class ResearcherRecommendedauthorImpl implements ResearcherRecommededauthor
{
	@Autowired
	private PalmAnalytics palmAnalytics;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Override
	public Map<String, Object> getResearcherRecommendedAuthorMap( Author author, int startPage, int maxresult )
	{
		// researchers list container
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		List<String> recommendedAuthors = new ArrayList<String>();
		recommendedAuthors = palmAnalytics.getNGrams().recommendedEntity( author.getId(), maxresult );

		// Prepare set of recommendedAuthor HashSet;
		Set<Author> recommendedauthorSet = new HashSet<Author>();

		for ( String recommendation : recommendedAuthors )
		{
			if ( persistenceStrategy.getAuthorDAO().getById( recommendation ).equals( author ) )
				continue;

			recommendedauthorSet.add( persistenceStrategy.getAuthorDAO().getById( recommendation ) );
		}

		// prepare list of object map containing recommendedAuthor details
		List<Map<String, Object>> recommendedAuthorList = new ArrayList<Map<String, Object>>();

		for ( Author recommendedAuthor : recommendedauthorSet )
		{
			// only copy necessary attributes
			Map<String, Object> recommendedAuthorMap = new LinkedHashMap<String, Object>();
			recommendedAuthorMap.put( "id", recommendedAuthor.getId() );
			recommendedAuthorMap.put( "name", recommendedAuthor.getName() );
			if ( recommendedAuthor.getInstitution() != null )
				recommendedAuthorMap.put( "affiliation", recommendedAuthor.getInstitution().getName() );
			if ( recommendedAuthor.getPhotoUrl() != null )
				recommendedAuthorMap.put( "photo", recommendedAuthor.getPhotoUrl() );
			recommendedAuthorMap.put( "isAdded", recommendedAuthor.isAdded() );

			// add into list
			recommendedAuthorList.add( recommendedAuthorMap );
		}

		// prepare list of object map containing coAuthor details
		List<Map<String, Object>> recommendedAuthorListPaging = new ArrayList<Map<String, Object>>();

		int position = 0;
		for ( Map<String, Object> recommendedAuthor : recommendedAuthorList )
		{
			if ( position >= startPage && recommendedAuthorListPaging.size() < maxresult )
			{
				recommendedAuthorListPaging.add( recommendedAuthor );
			}
		}

		// remove unnecessary result

		// put recommendedAuthor to responseMap
		responseMap.put( "countTotal", recommendedAuthorList.size() );
		responseMap.put( "count", recommendedAuthorListPaging.size() );
		responseMap.put( "recommendedAuthors", recommendedAuthorListPaging );

		return responseMap;
	}

}

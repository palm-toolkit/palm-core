package de.rwth.i9.palm.feature.researcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.helper.comparator.CoAuthorByNumberOfCollaborationComparator;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class ResearcherCoauthorImpl implements ResearcherCoauthor
{

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Override
	public Map<String, Object> getResearcherCoAuthorMap( Author author, int startPage, int maxresult )
	{
		// researchers list container
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		if ( author.getPublications() == null || author.getPublications().isEmpty() )
		{
			responseMap.put( "count", 0 );
			return responseMap;
		}

		// prepare a list of map object containing coauthor properties and
		Map<String, Integer> coAuthorCollaborationCountMap = new HashMap<String, Integer>();
		// Prepare set of coauthor HashSet;
		Set<Author> coauthorSet = new HashSet<Author>();
		// number of collaboration
		for ( Publication publication : author.getPublications() )
		{
			for ( Author coAuthor : publication.getAuthors() )
			{
				// just skip if its himself
				if ( coAuthor.equals( author ) )
					continue;

				coauthorSet.add( coAuthor );

				if ( coAuthorCollaborationCountMap.get( coAuthor.getId() ) == null )
					coAuthorCollaborationCountMap.put( coAuthor.getId(), 1 );
				else
					coAuthorCollaborationCountMap.put( coAuthor.getId(), coAuthorCollaborationCountMap.get( coAuthor.getId() ) + 1 );
			}
		}

		// prepare list of object map containing coAuthor details
		List<Map<String, Object>> coAuthorList = new ArrayList<Map<String, Object>>();

		for ( Author coAuthor : coauthorSet )
		{
			// only copy necessary attributes
			Map<String, Object> coAuthorMap = new LinkedHashMap<String, Object>();
			coAuthorMap.put( "id", coAuthor.getId() );
			coAuthorMap.put( "name", coAuthor.getName() );
			if ( coAuthor.getInstitution() != null )
				coAuthorMap.put( "affiliation", coAuthor.getInstitution().getName() );
			if ( coAuthor.getPhotoUrl() != null )
				coAuthorMap.put( "photo", coAuthor.getPhotoUrl() );
			coAuthorMap.put( "isAdded", coAuthor.isAdded() );
			coAuthorMap.put( "coautorTimes", coAuthorCollaborationCountMap.get( coAuthor.getId() ) );

			// add into list
			coAuthorList.add( coAuthorMap );
		}

		Collections.sort( coAuthorList, new CoAuthorByNumberOfCollaborationComparator() );

		// prepare list of object map containing coAuthor details
		List<Map<String, Object>> coAuthorListPaging = new ArrayList<Map<String, Object>>();

		int position = 0;
		for ( Map<String, Object> coAuthor : coAuthorList )
		{
			if ( position >= startPage && coAuthorListPaging.size() < maxresult )
			{
				coAuthorListPaging.add( coAuthor );
			}
		}

		// remove unnecessary result

		// put coauthor to responseMap
		responseMap.put( "countTotal", coAuthorList.size() );
		responseMap.put( "count", coAuthorListPaging.size() );
		responseMap.put( "coAuthors", coAuthorListPaging );

		return responseMap;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public Map<String, Object> getResearcherCoAuthorMapByPublication( List<Author> authorList, Set<Publication> publications )
	{
		// researchers list container
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// prepare a list of map object containing coauthor properties and
		Map<String, Integer> coAuthorCollaborationCountMap = new HashMap<String, Integer>();
		// Prepare set of coauthor HashSet;
		Set<Author> coauthorSet = new HashSet<Author>();
		// number of collaboration
		for ( Publication publication : publications )
		{
			for ( Author coAuthor : publication.getAuthors() )
			{
				// just skip if its one of the authors in consideration
				if ( authorList.contains( coAuthor ) )
					continue;

				coauthorSet.add( coAuthor );

				if ( coAuthorCollaborationCountMap.get( coAuthor.getId() ) == null )
					coAuthorCollaborationCountMap.put( coAuthor.getId(), 1 );
				else
					coAuthorCollaborationCountMap.put( coAuthor.getId(), coAuthorCollaborationCountMap.get( coAuthor.getId() ) + 1 );
			}
		}

		// prepare list of object map containing coAuthor details
		List<Map<String, Object>> coAuthorList = new ArrayList<Map<String, Object>>();

		for ( Author coAuthor : coauthorSet )
		{
			// only copy necessary attributes
			Map<String, Object> coAuthorMap = new LinkedHashMap<String, Object>();
			coAuthorMap.put( "id", coAuthor.getId() );
			coAuthorMap.put( "name", coAuthor.getName() );
			if ( coAuthor.getInstitution() != null )
				coAuthorMap.put( "affiliation", coAuthor.getInstitution().getName() );
			if ( coAuthor.getPhotoUrl() != null )
				coAuthorMap.put( "photo", coAuthor.getPhotoUrl() );
			coAuthorMap.put( "isAdded", coAuthor.isAdded() );
			coAuthorMap.put( "coautorTimes", coAuthorCollaborationCountMap.get( coAuthor.getId() ) );

			// add into list
			coAuthorList.add( coAuthorMap );
		}

		Collections.sort( coAuthorList, new CoAuthorByNumberOfCollaborationComparator() );

		// prepare list of object map containing coAuthor details
		List<Map<String, Object>> coAuthorListPaging = new ArrayList<Map<String, Object>>();

		int position = 0;
		int startPage = 0;
		int maxresult = 30;
		for ( Map<String, Object> coAuthor : coAuthorList )
		{
			if ( position >= startPage && coAuthorListPaging.size() < maxresult )
			{
				coAuthorListPaging.add( coAuthor );
			}
		}

		// remove unnecessary result

		// put coauthor to responseMap
		responseMap.put( "countTotal", coAuthorList.size() );
		responseMap.put( "count", coAuthorListPaging.size() );
		responseMap.put( "coAuthors", coAuthorListPaging );

		return responseMap;
	}

}

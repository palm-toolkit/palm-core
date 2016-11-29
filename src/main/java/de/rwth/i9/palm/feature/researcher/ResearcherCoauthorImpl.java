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

import de.rwth.i9.palm.helper.VADataFetcher;
import de.rwth.i9.palm.helper.comparator.CoAuthorByNumberOfCollaborationComparator;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class ResearcherCoauthorImpl implements ResearcherCoauthor
{

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private VADataFetcher dataFetcher;

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
	public Map<String, Object> getResearcherCoAuthorMapByPublication( Set<Publication> publications, String type, String visType, List<String> idsList, String startYear, String endYear, String yearFilterPresent )
	{
		// researchers list container
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// prepare a list of map object containing coauthor properties and
		Map<String, Integer> coAuthorCollaborationCountMap = new HashMap<String, Integer>();
		// Prepare set of coauthor HashSet;
		Set<Author> coauthorSet = new HashSet<Author>();

		Map<String, Object> map = dataFetcher.fetchCommonAuthors( type, publications, idsList, yearFilterPresent );
		List<Author> commonAuthors = (List<Author>) map.get( "commonAuthors" );
		Map<String, Object> collaborationMaps = (Map<String, Object>) map.get( "collaborationMaps" );
		System.out.println( "sel authors in list:" + commonAuthors.size() );
		Map<String, Integer> totalCollaborationCount = new HashMap<String, Integer>();
		if ( collaborationMaps != null )
			totalCollaborationCount = (Map<String, Integer>) collaborationMaps.get( "totalCollaborationCount" );

		List<Author> authorList = new ArrayList<Author>();
		for ( String id : idsList )
		{
			authorList.add( persistenceStrategy.getAuthorDAO().getById( id ) );
		}

		// shortlisted researchers' publications must also have the
		// corresponding topics
		List<Author> publicationAuthors = new ArrayList<Author>();
		Set<Publication> publicationsWithInterest = new HashSet<Publication>();
		if ( type.equals( "topic" ) && visType.equals( "researchers" ) )
		{
			publicationsWithInterest = dataFetcher.fetchAllPublications( type, idsList, authorList );
			for ( Publication p : publicationsWithInterest )
				for ( Author a : p.getAuthors() )
					if ( !publicationAuthors.contains( a ) )
						publicationAuthors.add( a );
		}

		for ( Author coAuthor : commonAuthors )
		{
			// just skip if its one of the authors in consideration
			if ( authorList.contains( coAuthor ) )
				continue;

			if ( !publicationsWithInterest.isEmpty() && !publicationAuthors.contains( coAuthor ) )
				continue;

			coauthorSet.add( coAuthor );

			if ( coAuthorCollaborationCountMap.get( coAuthor.getId() ) == null )
				coAuthorCollaborationCountMap.put( coAuthor.getId(), 1 );
			else
				coAuthorCollaborationCountMap.put( coAuthor.getId(), coAuthorCollaborationCountMap.get( coAuthor.getId() ) + 1 );
		}

		// }
		// prepare list of object map containing coAuthor details
		List<Map<String, Object>> coAuthorList = new ArrayList<Map<String, Object>>();

		for ( Author coAuthor : coauthorSet )
		{
			// only copy necessary attributes
			Map<String, Object> coAuthorMap = new LinkedHashMap<String, Object>();
			coAuthorMap.put( "id", coAuthor.getId() );
			coAuthorMap.put( "name", coAuthor.getName() );
			coAuthorMap.put( "isAdded", coAuthor.isAdded() );

			if ( collaborationMaps != null )
				coAuthorMap.put( "coautorTimes", totalCollaborationCount.get( coAuthor.getId() ) );
			else
				coAuthorMap.put( "coautorTimes", coAuthorCollaborationCountMap.get( coAuthor.getId() ) );

			// add into list
			coAuthorList.add( coAuthorMap );
		}

		if ( collaborationMaps != null )
			Collections.sort( coAuthorList, new CoAuthorByNumberOfCollaborationComparator() );

		// put coauthor to responseMap
		responseMap.put( "countTotal", coAuthorList.size() );
		responseMap.put( "count", coAuthorList.size() );
		responseMap.put( "coAuthors", coAuthorList );
		responseMap.put( "collaborationMaps", collaborationMaps );

		return responseMap;
	}

}

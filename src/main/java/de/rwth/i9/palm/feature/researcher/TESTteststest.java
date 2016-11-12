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

import de.rwth.i9.palm.analytics.util.InterestParser;
import de.rwth.i9.palm.helper.comparator.CoAuthorByNumberOfCollaborationComparator;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.DataMiningAuthor;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class TESTteststest
{

	@Autowired
	private PersistenceStrategy persistenceStrategy;

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
	public Map<String, Object> getResearcherCoAuthorMapByPublication( List<Author> authorList, Set<Publication> publications, String type, List<String> idsList, String startYear, String endYear )
	{
		// researchers list container
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// prepare a list of map object containing coauthor properties and
		Map<String, Integer> coAuthorCollaborationCountMap = new HashMap<String, Integer>();
		// Prepare set of coauthor HashSet;
		Set<Author> coauthorSet = new HashSet<Author>();

		if ( type.equals( "researcher" ) )
		{
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
		}
		else
		{

			List<Author> commonAuthors = new ArrayList<Author>();
			if ( type.equals( "publication" ) )
			{
				List<Integer> count = new ArrayList<Integer>();
				for ( Publication p : publications )
				{
					for ( Author a : p.getAuthors() )
					{
						if ( !commonAuthors.contains( a ) )
						{
							commonAuthors.add( a );
							count.add( 1 );
						}
						else
						{
							int index = commonAuthors.indexOf( a );
							count.set( index, count.get( index ) + 1 );
						}
					}
				}

				for ( int i = 0; i < count.size(); i++ )
				{
					if ( count.get( i ) < publications.size() )
					{
						count.remove( i );
						commonAuthors.remove( i );
						i--;
					}
				}
			}
			if ( type.equals( "topic" ) )
			{
				List<String> interestList = new ArrayList<String>();
				for ( int i = 0; i < idsList.size(); i++ )
				{
					Interest interest = persistenceStrategy.getInterestDAO().getById( idsList.get( i ) );
					interestList.add( interest.getTerm() );
				}
				System.out.println( interestList.toString() );
				List<DataMiningAuthor> DMAuthors = persistenceStrategy.getAuthorDAO().getDataMiningObjects();
				for ( DataMiningAuthor dma : DMAuthors )
				{
					Map<String, Double> interests = new HashMap<String, Double>();
					interests = InterestParser.parseInterestString( dma.getAuthor_interest_flat().getInterests() );
					if ( interests.keySet().containsAll( interestList ) )
					{
						commonAuthors.add( persistenceStrategy.getAuthorDAO().getById( dma.getId() ) );
					}
				}
			}
			if ( type.equals( "conference" ) )
			{
				List<Integer> count = new ArrayList<Integer>();
				for ( int i = 0; i < idsList.size(); i++ )
				{
					EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( idsList.get( i ) );
					List<Author> eventAuthors = new ArrayList<Author>();

					List<Event> events = eg.getEvents();
					for ( Event e : events )
					{
						List<Publication> eventPublications = e.getPublications();
						for ( Publication p : eventPublications )
						{
							if ( Integer.parseInt( p.getYear() ) >= Integer.parseInt( startYear ) && Integer.parseInt( p.getYear() ) <= Integer.parseInt( endYear ) )
							{
								List<Author> authors = p.getAuthors();
								for ( Author a : authors )
								{
									if ( !eventAuthors.contains( a ) )
									{
										eventAuthors.add( a );
									}
								}
							}
						}
					}
					for ( Author a : eventAuthors )
					{
						if ( !commonAuthors.contains( a ) )
						{
							commonAuthors.add( a );
							count.add( 0 );
						}
						else
						{
							count.set( commonAuthors.indexOf( a ), count.get( commonAuthors.indexOf( a ) ) + 1 );
						}
					}
				}

				for ( int i = 0; i < commonAuthors.size(); i++ )
				{
					if ( count.get( i ) != idsList.size() - 1 )
					{
						count.remove( i );
						commonAuthors.remove( i );
						i--;
					}
				}
			}
			if ( type.equals( "circle" ) )
			{
				List<Integer> count = new ArrayList<Integer>();
				for ( int i = 0; i < idsList.size(); i++ )
				{
					Circle c = persistenceStrategy.getCircleDAO().getById( idsList.get( i ) );
					for ( Author a : c.getAuthors() )
					{
						if ( !commonAuthors.contains( a ) )
						{
							commonAuthors.add( a );
							count.add( 0 );
						}
						else
						{
							count.set( commonAuthors.indexOf( a ), count.get( commonAuthors.indexOf( a ) ) + 1 );
						}
					}
				}

				for ( int i = 0; i < commonAuthors.size(); i++ )
				{
					if ( count.get( i ) != idsList.size() - 1 )
					{
						count.remove( i );
						commonAuthors.remove( i );
						i--;
					}
				}
			}

			// get authors from the publications
			List<Author> authors = new ArrayList<Author>();
			for ( Publication p : publications )
			{
				for ( Author a : p.getAuthors() )
				{
					if ( !authors.contains( a ) )
					{
						authors.add( a );
					}
				}
			}

			// the common authors must be part of the publication authors
			for ( int i = 0; i < commonAuthors.size(); i++ )
			{
				if ( !authors.contains( commonAuthors.get( i ) ) )
				{
					commonAuthors.remove( i );
					i--;
				}
			}

			for ( Author coAuthor : commonAuthors )
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

		for ( Map<String, Object> coAuthor : coAuthorList )
		{
			coAuthorListPaging.add( coAuthor );
		}

		// put coauthor to responseMap
		responseMap.put( "countTotal", coAuthorList.size() );
		responseMap.put( "count", coAuthorListPaging.size() );
		responseMap.put( "coAuthors", coAuthorListPaging );

		return responseMap;
	}

}

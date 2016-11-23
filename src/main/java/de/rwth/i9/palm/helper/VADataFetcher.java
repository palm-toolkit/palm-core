package de.rwth.i9.palm.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.analytics.util.InterestParser;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorInterest;
import de.rwth.i9.palm.model.AuthorInterestProfile;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.CircleInterest;
import de.rwth.i9.palm.model.CircleInterestProfile;
import de.rwth.i9.palm.model.DataMiningAuthor;
import de.rwth.i9.palm.model.DataMiningEventGroup;
import de.rwth.i9.palm.model.DataMiningPublication;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.EventInterest;
import de.rwth.i9.palm.model.EventInterestProfile;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationTopic;
import de.rwth.i9.palm.model.PublicationTopicFlat;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class VADataFetcher
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private PalmAnalytics palmAnalytics;

	public Map<String, Object> fetchCommonAuthors( String type, Set<Publication> publications, List<String> idsList, String yearFilterPresent )
	{

		Map<String, Object> finalMap = new HashMap<String, Object>();
		List<Author> commonAuthors = new ArrayList<Author>();
		List<Integer> count = new ArrayList<Integer>();

		if ( type.equals( "publication" ) )
		{
			// if the filter criteria doesn't match any publications
			if ( yearFilterPresent.equals( "true" ) && publications.isEmpty() )
			{
				System.out.println( "no match!" );
			}
			else
			{
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
			}
		}
		if ( type.equals( "researcher" ) )
		{
			// if the filter criteria doesn't match any publications
			if ( yearFilterPresent.equals( "true" ) && publications.isEmpty() )
			{
				System.out.println( "no match!" );
			}
			else
			{
				Map<String, Object> coAuthorCollaborationMaps = new HashMap<String, Object>();
				Map<String, Integer> totalCollaborationCount = new HashMap<String, Integer>();

				for ( int i = 0; i < idsList.size(); i++ )
				{
					Map<String, Integer> coAuthorCollaborationCountMap = new HashMap<String, Integer>();

					Author researcher = persistenceStrategy.getAuthorDAO().getById( idsList.get( i ) );
					List<Author> researcherCoAuthors = new ArrayList<Author>();
					List<Publication> researcherPublications = new ArrayList<Publication>( researcher.getPublications() );
					for ( Publication p : researcherPublications )
					{
						Boolean flag = true;
						if ( !publications.isEmpty() )
						{
							if ( !publications.contains( p ) )
								flag = false;
							// else
							// flag = true;
						}
						if ( flag )
						{
							for ( Author pubAuthor : p.getAuthors() )
							{
								if ( !researcherCoAuthors.contains( pubAuthor ) )
								{
									researcherCoAuthors.add( pubAuthor );
									coAuthorCollaborationCountMap.put( pubAuthor.getId(), 1 );
								}
								else
								{
									coAuthorCollaborationCountMap.put( pubAuthor.getId(), coAuthorCollaborationCountMap.get( pubAuthor.getId() ) + 1 );
								}
								if ( totalCollaborationCount.get( pubAuthor.getId() ) == null )
									totalCollaborationCount.put( pubAuthor.getId(), 1 );
								else
									totalCollaborationCount.put( pubAuthor.getId(), totalCollaborationCount.get( pubAuthor.getId() ) + 1 );

							}
						}
					}
					coAuthorCollaborationMaps.put( researcher.getId(), coAuthorCollaborationCountMap );

					for ( Author a : researcherCoAuthors )
					{
						if ( !commonAuthors.contains( a ) )
						{
							commonAuthors.add( a );
							count.add( 1 );
						}
						else
						{
							count.set( commonAuthors.indexOf( a ), count.get( commonAuthors.indexOf( a ) ) + 1 );
						}
					}
				}
				coAuthorCollaborationMaps.put( "totalCollaborationCount", totalCollaborationCount );
				finalMap.put( "collaborationMaps", coAuthorCollaborationMaps );
			}
		}
		if ( type.equals( "topic" ) )
		{
			// if the filter criteria doesn't match any publications
			if ( yearFilterPresent.equals( "true" ) && publications.isEmpty() )
			{
				System.out.println( "no match!" );
			}
			else
			{
				List<DataMiningAuthor> DMAuthors = persistenceStrategy.getAuthorDAO().getDataMiningObjects();
				for ( int i = 0; i < idsList.size(); i++ )
				{
					Interest interest = persistenceStrategy.getInterestDAO().getById( idsList.get( i ) );

					for ( DataMiningAuthor dma : DMAuthors )
					{
						Map<String, Double> interests = new HashMap<String, Double>();
						interests = InterestParser.parseInterestString( dma.getAuthor_interest_flat().getInterests() );
						if ( interests.keySet().contains( interest.getTerm() ) )
						{
							Author a = persistenceStrategy.getAuthorDAO().getById( dma.getId() );
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
				}
			}
		}
		if ( type.equals( "conference" ) )
		{
			// if the filter criteria doesn't match any publications
			if ( yearFilterPresent.equals( "true" ) && publications.isEmpty() )
			{
				System.out.println( "no match!" );
			}
			else
			{

				Map<String, Object> authorCollaborationMaps = new HashMap<String, Object>();
				Map<String, Integer> totalCollaborationCount = new HashMap<String, Integer>();

				for ( int i = 0; i < idsList.size(); i++ )
				{
					Map<String, Integer> authorCollaborationCountMap = new HashMap<String, Integer>();

					EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( idsList.get( i ) );
					List<Author> eventAuthors = new ArrayList<Author>();

					List<Event> events = eg.getEvents();
					for ( Event e : events )
					{
						List<Publication> eventPublications = e.getPublications();
						for ( Publication p : eventPublications )
						{
							Boolean flag = true;
							if ( !publications.isEmpty() )
							{
								if ( !publications.contains( p ) )
									flag = false;
								// else
								// flag = true;
							}
							if ( flag )
							{
								List<Author> authors = p.getAuthors();
								for ( Author a : authors )
								{
									if ( !eventAuthors.contains( a ) )
									{
										eventAuthors.add( a );
										authorCollaborationCountMap.put( a.getId(), 1 );
									}
									else
									{
										authorCollaborationCountMap.put( a.getId(), authorCollaborationCountMap.get( a.getId() ) + 1 );
									}
									if ( totalCollaborationCount.get( a.getId() ) == null )
										totalCollaborationCount.put( a.getId(), 1 );
									else
										totalCollaborationCount.put( a.getId(), totalCollaborationCount.get( a.getId() ) + 1 );
								}
							}
						}
					}

					authorCollaborationMaps.put( eg.getId(), authorCollaborationCountMap );

					for ( Author a : eventAuthors )
					{
						if ( !commonAuthors.contains( a ) )
						{
							commonAuthors.add( a );
							count.add( 1 );
						}
						else
						{
							count.set( commonAuthors.indexOf( a ), count.get( commonAuthors.indexOf( a ) ) + 1 );
						}
					}
				}
				authorCollaborationMaps.put( "totalCollaborationCount", totalCollaborationCount );
				finalMap.put( "collaborationMaps", authorCollaborationMaps );
			}
		}
		if ( type.equals( "circle" ) )
		{
			// if the filter criteria doesn't match any publications
			if ( yearFilterPresent.equals( "true" ) && publications.isEmpty() )
			{
				System.out.println( "no match!" );
			}
			else
			{
				for ( int i = 0; i < idsList.size(); i++ )
				{
					Circle c = persistenceStrategy.getCircleDAO().getById( idsList.get( i ) );
					for ( Author a : c.getAuthors() )
					{
						if ( !commonAuthors.contains( a ) )
						{
							commonAuthors.add( a );
							count.add( 1 );
						}
						else
						{
							count.set( commonAuthors.indexOf( a ), count.get( commonAuthors.indexOf( a ) ) + 1 );
						}
					}
				}
			}
		}

		for ( int i = 0; i < count.size(); i++ )
		{
			if ( count.get( i ) < idsList.size() )
			{
				count.remove( i );
				commonAuthors.remove( i );
				i--;
			}
		}

		System.out.println( "COMMON AUTHORS: " + commonAuthors.size() );
		System.out.println( "Pubs in data fetch: " + publications.size() );
		if ( !publications.isEmpty() )
		{
			System.out.println( "publications not empty!" );
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

			System.out.println( "pub authors: " + authors.size() );
			// the common authors must be part of the publication authors
			for ( int i = 0; i < commonAuthors.size(); i++ )
			{
				if ( !authors.contains( commonAuthors.get( i ) ) )
				{
					commonAuthors.remove( i );
					i--;
				}
			}

			System.out.println( "commonAuthors: " + commonAuthors.size() );
		}

		finalMap.put( "commonAuthors", commonAuthors );
		return finalMap;
	}

	public Set<Publication> fetchAllPublications( String type, List<String> idsList, List<Author> authorList )
	{
		Set<Publication> publications = new HashSet<Publication>();
		if ( type.equals( "researcher" ) )
		{
			for ( Author a : authorList )
			{
				publications.addAll( a.getPublications() );
			}
		}
		if ( type.equals( "conference" ) )
		{
			List<EventGroup> eventGroupList = new ArrayList<EventGroup>();
			for ( String id : idsList )
			{
				eventGroupList.add( persistenceStrategy.getEventGroupDAO().getById( id ) );
			}
			// if there are more than one conferences in consideration
			if ( idsList.size() > 1 )
			{
				List<Publication> eventGroupPublicationsList = new ArrayList<Publication>( publications );
				for ( int i = 0; i < eventGroupList.size(); i++ )
				{
					List<Event> groupEvents = eventGroupList.get( i ).getEvents();
					for ( Event e : groupEvents )
					{
						List<Publication> eventGroupPublications = e.getPublications();
						for ( int j = 0; j < eventGroupPublications.size(); j++ )
						{
							if ( !eventGroupPublicationsList.contains( eventGroupPublications.get( j ) ) && ( eventGroupPublications.get( j ).getYear() != null || eventGroupPublications.get( j ).getPublicationDate() != null ) )
							{
								eventGroupPublicationsList.add( eventGroupPublications.get( j ) );
							}
						}
					}
				}
				publications = new HashSet<Publication>( eventGroupPublicationsList );
			}
			if ( eventGroupList.size() == 1 )
			{
				publications = new HashSet<Publication>();

				// set of conditions!!
				if ( eventGroupList.get( 0 ) != null )
					if ( eventGroupList.get( 0 ).getEvents() != null )
					{
						List<Event> groupEvents = eventGroupList.get( 0 ).getEvents();
						for ( Event e : groupEvents )
						{
							List<Publication> eventGroupPublications = e.getPublications();
							for ( int j = 0; j < eventGroupPublications.size(); j++ )
							{
								if ( !publications.contains( eventGroupPublications.get( j ) ) && ( eventGroupPublications.get( j ).getYear() != null || eventGroupPublications.get( j ).getPublicationDate() != null ) )
								{
									if ( eventGroupPublications.get( j ) != null && eventGroupPublications.get( j ).getPublicationDate() != null )
										eventGroupPublications.get( j ).setYear( eventGroupPublications.get( j ).getPublicationDate().toString().substring( 0, 4 ) );

									publications.add( eventGroupPublications.get( j ) );
								}
							}
						}
					}
			}

		}
		if ( type.equals( "topic" ) )
		{
			List<DataMiningPublication> allDMPublications = persistenceStrategy.getPublicationDAO().getDataMiningObjects();
			List<DataMiningPublication> selectedDMPublications = new ArrayList<DataMiningPublication>();
			List<String> pubIds = new ArrayList<String>();
			List<Interest> interestList = new ArrayList<Interest>();
			for ( String id : idsList )
			{
				interestList.add( persistenceStrategy.getInterestDAO().getById( id ) );
			}
			for ( Interest i : interestList )
			{
				for ( DataMiningPublication dmp : allDMPublications )
				{
					PublicationTopicFlat ptf = dmp.getPublication_topic_flat();
					if ( ptf != null )
					{
						Map<String, Double> topics = InterestParser.parseInterestString( ptf.getTopics() );

						Iterator<String> term = topics.keySet().iterator();
						Iterator<Double> termWeight = topics.values().iterator();
						while ( term.hasNext() && termWeight.hasNext() )
						{
							String topic = term.next();
							float dist = palmAnalytics.getTextCompare().getDistanceByLuceneLevenshteinDistance( topic, i.getTerm() );

							if ( dist > 0.9f )
							{
								if ( !selectedDMPublications.contains( dmp ) )
								{
									selectedDMPublications.add( dmp );
									pubIds.add( dmp.getId() );
								}
							}
						}
					}
				}
			}
			publications = new HashSet<Publication>( persistenceStrategy.getPublicationDAO().getPublicationByIds( pubIds ) );
		}
		if ( type.equals( "circle" ) )
		{
			List<Circle> circleList = new ArrayList<Circle>();
			for ( String id : idsList )
			{
				circleList.add( persistenceStrategy.getCircleDAO().getById( id ) );
			}
			// if there are more than one circles in consideration
			if ( circleList.size() > 1 )
			{
				List<Publication> circlePublicationsList = new ArrayList<Publication>( publications );
				List<Publication> tempPubList = new ArrayList<Publication>();
				for ( int i = 0; i < circleList.size(); i++ )
				{
					tempPubList = new ArrayList<Publication>( circleList.get( i ).getPublications() );
					for ( int j = 0; j < tempPubList.size(); j++ )
					{
						if ( tempPubList.get( j ).getYear() != null || tempPubList.get( j ).getPublicationDate() != null )
						{
							if ( !circlePublicationsList.contains( tempPubList.get( j ) ) )
							{
								circlePublicationsList.add( tempPubList.get( j ) );
							}
						}

					}
				}
				publications = new HashSet<Publication>( circlePublicationsList );
			}
			if ( circleList.size() == 1 )
			{
				if ( circleList.get( 0 ) != null )
				{
					List<Publication> pubs = new ArrayList<Publication>( circleList.get( 0 ).getPublications() );
					for ( int c = 0; c < pubs.size(); c++ )
					{
						// to not consider publication if year is null
						if ( pubs.get( c ).getYear() != null || pubs.get( c ).getPublicationDate() != null )
						{
							publications.add( pubs.get( c ) );
						}
					}
				}
			}
		}

		return publications;
	}

	// Object : Researcher, Visualization Type : Co-authors
	public Map<String, Object> fetchCoAuthorForAuthors( Author author, String startYear, String endYear, String yearFilterPresent )
	{
		Set<Publication> authorPublications = author.getPublications();
		List<Author> allCoAuthors = new ArrayList<Author>();
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
		for ( Publication p : authorPublications )
		{
			Boolean flag = false;
			if ( startYear.equals( "" ) || startYear.equals( "0" ) || yearFilterPresent.equals( "false" ) )
			{
				flag = true;
			}
			else
			{
				if ( p.getYear() != null )
				{
					if ( ( Integer.parseInt( p.getYear() ) >= Integer.parseInt( startYear ) && Integer.parseInt( p.getYear() ) <= Integer.parseInt( endYear ) ) )
					{
						flag = true;
					}
				}
			}
			if ( flag )
			{
				List<Author> authors = p.getAuthors();
				for ( Author a : authors )
				{
					if ( !allCoAuthors.contains( a ) && !author.equals( a ) )
					{
						allCoAuthors.add( a );
						Map<String, Object> items = new HashMap<String, Object>();
						items.put( "name", a.getName() );
						items.put( "id", a.getId() );
						items.put( "isAdded", a.isAdded() );
						listItems.add( items );
					}
				}
			}
		}

		Map<String, Object> map = new HashMap<String, Object>();
		map.put( "allCoAuthors", allCoAuthors );
		map.put( "listItems", listItems );
		return map;
	}

	// Object : Researcher, Visualization Type : Conferences
	public Map<String, Object> fetchConferencesForAuthors( Author author, String startYear, String endYear, String yearFilterPresent )
	{
		Set<Publication> authorPublications = author.getPublications();
		List<EventGroup> authorEventGroups = new ArrayList<EventGroup>();
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
		for ( Publication p : authorPublications )
		{
			Boolean flag = false;
			if ( startYear.equals( "" ) || startYear.equals( "0" ) || yearFilterPresent.equals( "false" ) )
			{
				flag = true;
			}
			else
			{
				if ( p.getYear() != null )
				{
					if ( ( Integer.parseInt( p.getYear() ) >= Integer.parseInt( startYear ) && Integer.parseInt( p.getYear() ) <= Integer.parseInt( endYear ) ) )
					{
						flag = true;
					}
				}
			}

			if ( flag )
			{
				if ( p.getEvent() != null )
				{
					if ( p.getEvent().getEventGroup() != null )
					{

						EventGroup eventGroup = p.getEvent().getEventGroup();
						if ( !authorEventGroups.contains( eventGroup ) )
						{
							authorEventGroups.add( eventGroup );
							Map<String, Object> items = new HashMap<String, Object>();
							items.put( "name", eventGroup.getName() );
							items.put( "id", eventGroup.getId() );
							items.put( "isAdded", eventGroup.isAdded() );
							listItems.add( items );
						}
					}
				}
			}
		}

		Map<String, Object> map = new HashMap<String, Object>();
		map.put( "authorEventGroups", authorEventGroups );
		map.put( "listItems", listItems );
		return map;
	}

	// Object : Researcher, Visualization Type : Publications
	public Map<String, Object> fetchPublicationsForAuthors( Author author, String startYear, String endYear, String yearFilterPresent )
	{
		Set<Publication> publications = author.getPublications();
		List<Publication> authorPublications = new ArrayList<Publication>();
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();

		for ( Publication p : publications )
		{
			Boolean flag = false;
			if ( startYear.equals( "" ) || startYear.equals( "0" ) || yearFilterPresent.equals( "false" ) )
			{
				flag = true;
			}
			else
			{
				if ( p.getYear() != null )
				{
					if ( ( Integer.parseInt( p.getYear() ) >= Integer.parseInt( startYear ) && Integer.parseInt( p.getYear() ) <= Integer.parseInt( endYear ) ) )
					{
						flag = true;
					}
				}
			}

			if ( flag )
			{
				if ( !authorPublications.contains( p ) )
				{
					authorPublications.add( p );
					Map<String, Object> items = new HashMap<String, Object>();
					items.put( "name", p.getTitle() );
					items.put( "id", p.getId() );
					listItems.add( items );
				}
			}
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put( "authorPublications", authorPublications );
		map.put( "listItems", listItems );
		return map;
	}

	// Object : Researcher, Visualization Type : Topics
	public Map<String, Object> fetchTopicsForAuthors( Author author, String startYear, String endYear, String yearFilterPresent )
	{
		System.out.println( startYear + " " + endYear );
		List<String> allTopics = new ArrayList<String>();
		List<Publication> pubs = new ArrayList<Publication>( author.getPublications() );
		for ( Publication p : pubs )
		{
			Set<PublicationTopic> publicationTopics = p.getPublicationTopics();
			for ( PublicationTopic pubTopic : publicationTopics )
			{
				List<Double> topicWeights = new ArrayList<Double>( pubTopic.getTermValues().values() );
				List<String> topics = new ArrayList<String>( pubTopic.getTermValues().keySet() );
				for ( int j = 0; j < topics.size(); j++ )
				{
					if ( !allTopics.contains( topics.get( j ) ) && topicWeights.get( j ) > 0.3 )
					{
						allTopics.add( topics.get( j ) );
					}
				}
			}
		}
		System.out.println( "COMP: " + author.getName() + " : " + allTopics.size() );

		List<String> interestTopicNames = new ArrayList<String>();
		List<String> interestTopicIds = new ArrayList<String>();
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();

		Set<AuthorInterestProfile> authorInterestProfiles = author.getAuthorInterestProfiles();
		for ( AuthorInterestProfile aip : authorInterestProfiles )
		{
			Set<AuthorInterest> ais = aip.getAuthorInterests();
			for ( AuthorInterest ai : ais )
			{
				Map<Interest, Double> interests = ai.getTermWeights();
				Iterator<Interest> interestTerm = interests.keySet().iterator();
				Iterator<Double> interestTermWeight = interests.values().iterator();
				while ( interestTerm.hasNext() && interestTermWeight.hasNext() )
				{
					Interest actualInterest = interestTerm.next();
					String interest = actualInterest.getTerm();
					Double weight = interestTermWeight.next();

					if ( weight > 0.3 )
					{
						if ( allTopics.contains( interest ) || allTopics.contains( interest + "s" ) )
						{
							Boolean validYear = true;
							Calendar calendar = Calendar.getInstance();
							calendar.setTime( ai.getYear() );
							String year = Integer.toString( calendar.get( Calendar.YEAR ) );
							if ( startYear.equals( "0" ) || startYear.equals( "" ) || yearFilterPresent.equals( "false" ) )
							{
								validYear = true;
							}
							else
							{
								if ( Integer.parseInt( year ) < Integer.parseInt( startYear ) || Integer.parseInt( year ) > Integer.parseInt( endYear ) )
								{
									System.out.println( year );
									validYear = false;
								}
							}
							if ( validYear )
							{
								if ( !interestTopicNames.contains( interest ) )
								{
									interestTopicNames.add( interest );
									interestTopicIds.add( actualInterest.getId() );

									Map<String, Object> items = new HashMap<String, Object>();
									items.put( "name", interest );
									items.put( "id", actualInterest.getId() );
									listItems.add( items );
								}

							}
						}
					}

				}
			}
		}
		System.out.println( "COMP: " + author.getName() + " : " + listItems.size() );
		Map<String, Object> map = new HashMap<String, Object>();
		map.put( "interestTopicIds", interestTopicIds );
		map.put( "interestTopicNames", interestTopicNames );
		map.put( "listItems", listItems );
		return map;
	}

	// Object : Conference, Visualization Type : Researchers
	public Map<String, Object> fetchResearchersForConferences( List<Event> events, String startYear, String endYear, String yearFilterPresent )
	{
		List<Publication> eventGroupPubs = new ArrayList<Publication>();

		for ( Event e : events )
		{
			List<Publication> eventPublications = e.getPublications();
			for ( Publication p : eventPublications )
			{
				if ( !eventGroupPubs.contains( p ) )
				{
					eventGroupPubs.add( p );
				}
			}
		}
		List<Author> publicationAuthors = new ArrayList<Author>();
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
		for ( Publication p : eventGroupPubs )
		{
			Boolean flag = false;
			if ( startYear.equals( "" ) || startYear.equals( "0" ) || yearFilterPresent.equals( "false" ) )
			{
				flag = true;
			}
			else
			{
				if ( p.getYear() != null )
				{
					if ( ( Integer.parseInt( p.getYear() ) >= Integer.parseInt( startYear ) && Integer.parseInt( p.getYear() ) <= Integer.parseInt( endYear ) ) )
					{
						flag = true;
					}
				}
			}
			if ( flag )
			{
				List<Author> authors = p.getAuthors();
				for ( Author a : authors )
				{
					if ( !publicationAuthors.contains( a ) )
					{
						publicationAuthors.add( a );
						Map<String, Object> items = new HashMap<String, Object>();
						items.put( "name", a.getName() );
						items.put( "id", a.getId() );
						items.put( "isAdded", a.isAdded() );
						listItems.add( items );
					}
				}
			}
		}

		Map<String, Object> map = new HashMap<String, Object>();
		map.put( "publicationAuthors", publicationAuthors );
		map.put( "listItems", listItems );
		return map;
	}

	// Object : Conference, Visualization Type : Topics
	public Map<String, Object> fetchTopicsForConferences( List<Event> events, String startYear, String endYear, String yearFilterPresent )
	{
		List<String> allTopics = new ArrayList<String>();
		for ( Event e : events )
		{
			List<Publication> pubs = new ArrayList<Publication>( e.getPublications() );
			for ( Publication p : pubs )
			{
				Set<PublicationTopic> publicationTopics = p.getPublicationTopics();
				for ( PublicationTopic pubTopic : publicationTopics )
				{
					List<Double> topicWeights = new ArrayList<Double>( pubTopic.getTermValues().values() );
					List<String> topics = new ArrayList<String>( pubTopic.getTermValues().keySet() );
					for ( int j = 0; j < topics.size(); j++ )
					{
						if ( !allTopics.contains( topics.get( j ) ) && topicWeights.get( j ) > 0.3 )
						{
							allTopics.add( topics.get( j ) );
						}
					}
				}
			}
		}

		List<String> interestTopicNames = new ArrayList<String>();
		List<String> interestTopicIds = new ArrayList<String>();
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();

		for ( Event e : events )
		{
			Set<EventInterestProfile> eventInterestProfiles = e.getEventInterestProfiles();
			for ( EventInterestProfile eip : eventInterestProfiles )
			{
				Set<EventInterest> eventInterests = eip.getEventInterests();
				for ( EventInterest ei : eventInterests )
				{
					Map<Interest, Double> interests = ei.getTermWeights();
					Iterator<Interest> interestTerm = interests.keySet().iterator();
					Iterator<Double> interestTermWeight = interests.values().iterator();

					while ( interestTerm.hasNext() && interestTermWeight.hasNext() )
					{
						Interest actualInterest = interestTerm.next();
						String interest = ( actualInterest.getTerm() );
						Double weight = interestTermWeight.next();
						if ( weight > 0.3 )
						{
							if ( allTopics.contains( interest ) || allTopics.contains( interest + "s" ) )
							{
								Boolean validYear = true;
								Calendar calendar = Calendar.getInstance();
								calendar.setTime( ei.getYear() );
								String year = Integer.toString( calendar.get( Calendar.YEAR ) );
								if ( startYear.equals( "0" ) || startYear.equals( "" ) || yearFilterPresent.equals( "false" ) )
								{
									validYear = true;
								}
								else
								{
									if ( Integer.parseInt( year ) < Integer.parseInt( startYear ) || Integer.parseInt( year ) > Integer.parseInt( endYear ) )
									{
										validYear = false;
									}
								}
								if ( validYear )
								{
									if ( !interestTopicNames.contains( interest ) )
									{
										interestTopicNames.add( interest );
										interestTopicIds.add( actualInterest.getId() );

										Map<String, Object> items = new HashMap<String, Object>();
										items.put( "name", interest );
										items.put( "id", actualInterest.getId() );
										listItems.add( items );
									}
								}
							}
						}
					}
				}
			}
		}
		System.out.println( "in comparison: " + interestTopicIds.size() );
		Map<String, Object> map = new HashMap<String, Object>();
		map.put( "interestTopicIds", interestTopicIds );
		map.put( "interestTopicNames", interestTopicNames );
		map.put( "listItems", listItems );
		return map;
	}

	// Object : Publication, Visualization Type : Researchers
	public Map<String, Object> fetchResearchersForPublications( Publication p, String startYear, String endYear, String yearFilterPresent )
	{
		List<Author> publicationAuthors = new ArrayList<Author>();
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
		Boolean flag = false;
		if ( startYear.equals( "" ) || startYear.equals( "0" ) || yearFilterPresent.equals( "false" ) )
		{
			flag = true;
		}
		else
		{
			if ( p.getYear() != null )
			{
				if ( ( Integer.parseInt( p.getYear() ) >= Integer.parseInt( startYear ) && Integer.parseInt( p.getYear() ) <= Integer.parseInt( endYear ) ) )
				{
					flag = true;
				}
			}
		}
		if ( flag )
		{
			List<Author> authors = p.getAuthors();
			for ( Author a : authors )
			{
				if ( !publicationAuthors.contains( a ) )
				{
					publicationAuthors.add( a );
					Map<String, Object> items = new HashMap<String, Object>();
					items.put( "name", a.getName() );
					items.put( "id", a.getId() );
					items.put( "isAdded", a.isAdded() );
					listItems.add( items );
				}
			}
		}

		Map<String, Object> map = new HashMap<String, Object>();
		map.put( "publicationAuthors", publicationAuthors );
		map.put( "listItems", listItems );
		return map;

	}

	// Object : Publication, Visualization Type : Topics
	public Map<String, Object> fetchTopicsForPublications( List<Interest> allInterestsInDB, Publication p, String startYear, String endYear, String yearFilterPresent )
	{

		List<String> allPublicationInterests = new ArrayList<String>();
		List<String> allPublicationInterestIds = new ArrayList<String>();

		for ( Interest interest : allInterestsInDB )
		{
			if ( !allPublicationInterests.contains( interest.getTerm() ) )
			{
				allPublicationInterests.add( interest.getTerm() );
				allPublicationInterestIds.add( interest.getId() );
			}
		}

		List<String> interestTopicNames = new ArrayList<String>();
		List<String> interestTopicIds = new ArrayList<String>();
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();

		Boolean flag = false;
		if ( startYear.equals( "" ) || startYear.equals( "0" ) || yearFilterPresent.equals( "false" ) )
		{
			flag = true;
		}
		else
		{
			if ( p.getYear() != null )
			{
				if ( ( Integer.parseInt( p.getYear() ) >= Integer.parseInt( startYear ) && Integer.parseInt( p.getYear() ) <= Integer.parseInt( endYear ) ) )
				{
					flag = true;
				}
			}
		}

		if ( flag )
		{
			List<PublicationTopic> topics = new ArrayList<PublicationTopic>( p.getPublicationTopics() );
			for ( PublicationTopic pt : topics )
			{
				Map<String, Double> termValues = pt.getTermValues();
				List<String> terms = new ArrayList<String>( termValues.keySet() );
				// List<Double> weights = new ArrayList<Double>(
				// termValues.values() );
				for ( int k = 0; k < terms.size(); k++ )
				{
					if ( !interestTopicNames.contains( terms.get( k ) ) )// &&
					{
						if ( allPublicationInterests.contains( terms.get( k ) ) )
						{
							interestTopicNames.add( terms.get( k ) );
							int pos = allPublicationInterests.indexOf( terms.get( k ) );
							interestTopicIds.add( allPublicationInterestIds.get( pos ) );

							Map<String, Object> items = new HashMap<String, Object>();
							items.put( "name", terms.get( k ) );
							items.put( "id", allPublicationInterestIds.get( pos ) );
							listItems.add( items );
						}
						else if ( allPublicationInterests.contains( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) ) )
						{
							interestTopicNames.add( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) );
							int pos = allPublicationInterests.indexOf( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) );
							interestTopicIds.add( allPublicationInterestIds.get( pos ) );

							Map<String, Object> items = new HashMap<String, Object>();
							items.put( "name", terms.get( k ) );
							items.put( "id", allPublicationInterestIds.get( pos ) );
							listItems.add( items );
						}
					}
				}
			}
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put( "interestTopicIds", interestTopicIds );
		map.put( "interestTopicNames", interestTopicNames );
		map.put( "listItems", listItems );
		return map;
	}

	// Object : Topic, Visualization Type : Researchers
	public Map<String, Object> fetchResearchersForTopics( Interest interest, List<DataMiningAuthor> DMAuthors, List<Author> publicationAuthors, String yearFilterPresent )
	{
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
		List<Author> interestAuthors = new ArrayList<Author>();
		for ( DataMiningAuthor dma : DMAuthors )
		{
			Map<String, Double> interests = new HashMap<String, Double>();
			interests = InterestParser.parseInterestString( dma.getAuthor_interest_flat().getInterests() );
			if ( interests.keySet().contains( interest.getTerm() ) )
			{
				Author a = persistenceStrategy.getAuthorDAO().getById( dma.getId() );

				if ( !interestAuthors.contains( a ) )
				{
					interestAuthors.add( a );
					Map<String, Object> items = new HashMap<String, Object>();
					items.put( "name", a.getName() );
					items.put( "id", a.getId() );
					items.put( "isAdded", a.isAdded() );
					listItems.add( items );
				}
			}
		}

		if ( yearFilterPresent.equals( "true" ) )
		{
			for ( int j = 0; j < interestAuthors.size(); j++ )
			{
				if ( !publicationAuthors.contains( interestAuthors.get( j ) ) )
				{
					interestAuthors.remove( j );
					listItems.remove( j );
					j--;
				}
			}
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put( "interestAuthors", interestAuthors );
		map.put( "listItems", listItems );
		return map;
	}

	// Object : Topic, Visualization Type : Conferences
	public Map<String, Object> fetchConferencesForTopics( Interest interest, List<DataMiningEventGroup> DMEventGroups, List<EventGroup> publicationEventGroups, String yearFilterPresent )
	{
		System.out.println( interest.getTerm() );
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();

		List<EventGroup> interestEventGroups = new ArrayList<EventGroup>();
		for ( DataMiningEventGroup dmeg : DMEventGroups )
		{

			Map<String, Double> interests = new HashMap<String, Double>();
			interests = InterestParser.parseInterestString( dmeg.getEventGroup_interest_flat().getInterests() );
			if ( interests.keySet().contains( interest.getTerm() ) )
			{
				EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( dmeg.getId() );
				if ( !interestEventGroups.contains( eg ) )
				{
					interestEventGroups.add( eg );
					Map<String, Object> items = new HashMap<String, Object>();
					items.put( "name", eg.getName() );
					items.put( "id", eg.getId() );
					items.put( "isAdded", eg.isAdded() );
					listItems.add( items );
				}
			}
		}

		if ( yearFilterPresent.equals( "true" ) )
		{
			for ( int j = 0; j < interestEventGroups.size(); j++ )
			{
				if ( !publicationEventGroups.contains( interestEventGroups.get( j ) ) )
				{
					interestEventGroups.remove( j );
					listItems.remove( j );
					j--;
				}
				else
					System.out.println( interestEventGroups.get( j ).getName() );
			}
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put( "interestEventGroups", interestEventGroups );
		map.put( "listItems", listItems );
		return map;
	}

	// Object : Topic, Visualization Type : Publications
	public Map<String, Object> fetchPublicationsForTopics( Interest interest, List<DataMiningPublication> allDMPublications, String startYear, String endYear, String yearFilterPresent )
	{
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
		List<Publication> interestPublications = new ArrayList<Publication>();
		List<DataMiningPublication> selectedDMPublications = new ArrayList<DataMiningPublication>();

		for ( DataMiningPublication dmp : allDMPublications )
		{
			PublicationTopicFlat ptf = dmp.getPublication_topic_flat();
			if ( ptf != null )
			{
				Map<String, Double> topics = InterestParser.parseInterestString( ptf.getTopics() );

				Iterator<String> term = topics.keySet().iterator();
				Iterator<Double> termWeight = topics.values().iterator();
				while ( term.hasNext() && termWeight.hasNext() )
				{
					String topic = term.next();
					float dist = palmAnalytics.getTextCompare().getDistanceByLuceneLevenshteinDistance( topic, interest.getTerm() );

					if ( dist > 0.9f )
					{
						if ( !selectedDMPublications.contains( dmp ) )
						{
							selectedDMPublications.add( dmp );
							Publication p = persistenceStrategy.getPublicationDAO().getById( dmp.getId() );
							Boolean flag = false;
							if ( startYear.equals( "" ) || startYear.equals( "0" ) || yearFilterPresent.equals( "false" ) )
							{
								flag = true;
							}
							else
							{
								if ( p.getYear() != null )
								{
									if ( ( Integer.parseInt( p.getYear() ) >= Integer.parseInt( startYear ) && Integer.parseInt( p.getYear() ) <= Integer.parseInt( endYear ) ) )
									{
										flag = true;
									}
								}
							}
							if ( flag )
							{
								interestPublications.add( p );
								Map<String, Object> items = new HashMap<String, Object>();
								items.put( "name", p.getTitle() );
								items.put( "id", p.getId() );
								listItems.add( items );
							}
						}
					}
				}
			}
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put( "interestPublications", interestPublications );
		map.put( "listItems", listItems );
		return map;
	}

	// Object : Circle, Visualization Type : Researchers
	public Map<String, Object> fetchResearchersForCircles( Circle circle, String startYear, String endYear, String yearFilterPresent )
	{
		Set<Publication> circlePublications = circle.getPublications();
		List<Author> circleAuthors = new ArrayList<Author>( circle.getAuthors() );
		List<Author> publicationAuthors = new ArrayList<Author>();
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
		for ( Publication p : circlePublications )
		{
			Boolean flag = false;
			if ( startYear.equals( "" ) || startYear.equals( "0" ) || yearFilterPresent.equals( "false" ) )
			{
				flag = true;
			}
			else
			{
				if ( p.getYear() != null )
				{
					if ( ( Integer.parseInt( p.getYear() ) >= Integer.parseInt( startYear ) && Integer.parseInt( p.getYear() ) <= Integer.parseInt( endYear ) ) )
					{
						flag = true;
					}
				}
			}
			if ( flag )
			{
				List<Author> authors = p.getAuthors();
				for ( Author a : authors )
				{
					if ( !publicationAuthors.contains( a ) && circleAuthors.contains( a ) )
					{
						publicationAuthors.add( a );
						Map<String, Object> items = new HashMap<String, Object>();
						items.put( "name", a.getName() );
						items.put( "id", a.getId() );
						items.put( "isAdded", a.isAdded() );
						listItems.add( items );
					}
				}
			}
		}

		Map<String, Object> map = new HashMap<String, Object>();
		map.put( "publicationAuthors", publicationAuthors );
		map.put( "listItems", listItems );
		return map;
	}

	// Object : Circle, Visualization Type : Conferences
	public Map<String, Object> fetchConferencesForCircles( Circle circle, String startYear, String endYear, String yearFilterPresent )
	{
		Set<Publication> circlePublications = circle.getPublications();
		List<EventGroup> circleEventGroups = new ArrayList<EventGroup>();
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
		for ( Publication p : circlePublications )
		{
			Boolean flag = false;
			if ( startYear.equals( "" ) || startYear.equals( "0" ) || yearFilterPresent.equals( "false" ) )
			{
				flag = true;
			}
			else
			{
				if ( p.getYear() != null )
				{
					if ( ( Integer.parseInt( p.getYear() ) >= Integer.parseInt( startYear ) && Integer.parseInt( p.getYear() ) <= Integer.parseInt( endYear ) ) )
					{
						flag = true;
					}
				}
			}
			if ( flag )
			{
				if ( p.getEvent() != null )
				{
					if ( p.getEvent().getEventGroup() != null )
					{
						EventGroup eg = p.getEvent().getEventGroup();
						if ( !circleEventGroups.contains( eg ) )
						{
							circleEventGroups.add( eg );
							Map<String, Object> items = new HashMap<String, Object>();
							items.put( "name", eg.getName() );
							items.put( "id", eg.getId() );
							items.put( "isAdded", eg.isAdded() );
							listItems.add( items );
						}
					}
				}
			}
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put( "circleEventGroups", circleEventGroups );
		map.put( "listItems", listItems );
		return map;
	}

	// Object : Circle, Visualization Type : Topics
	public Map<String, Object> fetchTopicsForCircles( Circle circle, String startYear, String endYear, String yearFilterPresent )
	{
		List<String> allTopics = new ArrayList<String>();
		List<Publication> pubs = new ArrayList<Publication>( circle.getPublications() );
		for ( Publication p : pubs )
		{
			Set<PublicationTopic> publicationTopics = p.getPublicationTopics();
			for ( PublicationTopic pubTopic : publicationTopics )
			{
				List<Double> topicWeights = new ArrayList<Double>( pubTopic.getTermValues().values() );
				List<String> topics = new ArrayList<String>( pubTopic.getTermValues().keySet() );
				for ( int j = 0; j < topics.size(); j++ )
				{
					if ( !allTopics.contains( topics.get( j ) ) && topicWeights.get( j ) > 0.3 )
					{
						allTopics.add( topics.get( j ) );
					}
				}
			}
		}
		List<String> interestTopicNames = new ArrayList<String>();
		List<String> interestTopicIds = new ArrayList<String>();
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
		Set<CircleInterestProfile> circleInterestProfiles = circle.getCircleInterestProfiles();
		for ( CircleInterestProfile cip : circleInterestProfiles )
		{
			Set<CircleInterest> cis = cip.getCircleInterests();
			for ( CircleInterest ci : cis )
			{
				Map<Interest, Double> interests = ci.getTermWeights();
				Iterator<Interest> interestTerm = interests.keySet().iterator();
				Iterator<Double> interestTermWeight = interests.values().iterator();
				while ( interestTerm.hasNext() && interestTermWeight.hasNext() )
				{
					Interest actualInterest = interestTerm.next();
					String interest = ( actualInterest.getTerm() );
					Double weight = interestTermWeight.next();

					if ( weight > 0.3 )
					{
						if ( allTopics.contains( interest ) || allTopics.contains( interest + "s" ) )
						{
							Boolean validYear = true;
							Calendar calendar = Calendar.getInstance();
							calendar.setTime( ci.getYear() );
							String year = Integer.toString( calendar.get( Calendar.YEAR ) );
							if ( startYear.equals( "0" ) || startYear.equals( "" ) || yearFilterPresent.equals( "false" ) )
							{
								validYear = true;
							}
							else
							{
								if ( Integer.parseInt( year ) < Integer.parseInt( startYear ) || Integer.parseInt( year ) > Integer.parseInt( endYear ) )
								{
									validYear = false;
								}
							}
							if ( validYear )
							{
								if ( !interestTopicNames.contains( interest ) )
								{
									interestTopicNames.add( interest );
									interestTopicIds.add( actualInterest.getId() );

									Map<String, Object> items = new HashMap<String, Object>();
									items.put( "name", interest );
									items.put( "id", actualInterest.getId() );
									listItems.add( items );
								}
							}
						}
					}

				}
			}
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put( "interestTopicIds", interestTopicIds );
		map.put( "interestTopicNames", interestTopicNames );
		map.put( "listItems", listItems );
		return map;
	}

	// Object : Circle, Visualization Type : Publications
	public Map<String, Object> fetchPublicationsForCircles( Circle circle, String startYear, String endYear, String yearFilterPresent )
	{
		Set<Publication> circlePublications = circle.getPublications();
		List<Publication> allPublications = new ArrayList<Publication>();
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();

		for ( Publication p : circlePublications )
		{
			Boolean flag = false;
			if ( startYear.equals( "" ) || startYear.equals( "0" ) )
			{
				flag = true;
			}
			else
			{
				if ( p.getYear() != null || p.getPublicationDate() != null )
				{
					if ( ( Integer.parseInt( p.getYear() ) >= Integer.parseInt( startYear ) && Integer.parseInt( p.getYear() ) <= Integer.parseInt( endYear ) ) )
					{
						flag = true;
					}
				}
			}

			if ( flag )
			{
				if ( !allPublications.contains( p ) )
				{
					allPublications.add( p );
					Map<String, Object> items = new HashMap<String, Object>();
					items.put( "name", p.getTitle() );
					items.put( "id", p.getId() );
					listItems.add( items );
				}
			}
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put( "allPublications", allPublications );
		map.put( "listItems", listItems );
		return map;
	}

}

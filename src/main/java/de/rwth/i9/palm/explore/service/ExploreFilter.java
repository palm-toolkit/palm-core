package de.rwth.i9.palm.explore.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.feature.academicevent.AcademicEventFeature;
import de.rwth.i9.palm.feature.circle.CircleFeature;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorInterest;
import de.rwth.i9.palm.model.AuthorInterestProfile;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.EventInterest;
import de.rwth.i9.palm.model.EventInterestProfile;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationTopic;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class ExploreFilter
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private CircleFeature circleFeature;

	@Autowired
	private AcademicEventFeature eventFeature;

	public List<Publication> getPublicationsForFilter( List<String> idsList, String type )
	{
		List<Publication> publications = new ArrayList<Publication>();

		List<Author> authors = new ArrayList<Author>();
		List<EventGroup> eventGroupList = new ArrayList<EventGroup>();
		authors = getAuthorsFromIds( idsList );
		eventGroupList = getConferencesFromIds( idsList );

		// if there are more than one authors in consideration
		publications = new ArrayList<Publication>( typeWisePublications( type, authors, eventGroupList ) );

		System.out.println( "publications for filter: " + publications.size() );
		return publications;

	}

	public Map<String, Object> publicationFilter( List<String> idsList, String type )
	{

		Map<String, Object> publicationsMap = new HashMap<String, Object>();

		List<Publication> publications = getPublicationsForFilter( idsList, type );

		ArrayList<Map<String, Object>> publicationDetailsList = new ArrayList<Map<String, Object>>();

		for ( int i = 0; i < publications.size(); i++ )
		{
			Map<String, Object> publicationDetail = new LinkedHashMap<String, Object>();

			publicationDetail.put( "id", publications.get( i ).getId() );
			publicationDetail.put( "title", publications.get( i ).getTitle() );

			publicationDetailsList.add( publicationDetail );
		}

		publicationsMap.put( "publicationsList", publicationDetailsList );

		return publicationsMap;
	}

	public Map<String, Object> conferenceFilter( List<String> idsList, String type )
	{

		Map<String, Object> eventsMap = new HashMap<String, Object>();

		List<Publication> publications = getPublicationsForFilter( idsList, type );

		ArrayList<Map<String, Object>> eventDetailsList = new ArrayList<Map<String, Object>>();
		List<String> tempIds = new ArrayList<String>();

		for ( int i = 0; i < publications.size(); i++ )
		{
			if ( publications.get( i ).getEvent() != null )
			{
				if ( publications.get( i ).getEvent().getEventGroup() != null )
				{
					if ( !tempIds.contains( publications.get( i ).getEvent().getEventGroup().getId() ) )
					{
						Map<String, Object> eventDetail = new LinkedHashMap<String, Object>();
						tempIds.add( publications.get( i ).getEvent().getEventGroup().getId() );
						eventDetail.put( "id", publications.get( i ).getEvent().getEventGroup().getId() );
						eventDetail.put( "title", publications.get( i ).getEvent().getEventGroup().getName() );
						eventDetailsList.add( eventDetail );
					}
				}
			}
		}

		eventsMap.put( "eventsList", eventDetailsList );

		return eventsMap;
	}

	@SuppressWarnings( "unchecked" )
	public Map<String, Object> circleFilter( List<String> idsList, String type )
	{

		String query = "";
		int page = 0;
		int maxresult = 100;
		String orderBy = "citation";

		// get Author List
		List<Author> authorList = new ArrayList<Author>();

		for ( int itemIndex = 0; itemIndex < idsList.size(); itemIndex++ )
		{
			authorList.add( persistenceStrategy.getAuthorDAO().getById( idsList.get( itemIndex ) ) );

		}

		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		responseMap.put( "query", query );
		responseMap.put( "page", page );
		responseMap.put( "maxresult", maxresult );
		responseMap.put( "orderBy", orderBy );

		Map<String, Object> circleMap = circleFeature.getCircleSearch().getCircleListByResearchers( idsList, orderBy );

		return circleFeature.getCircleSearch().printJsonOutput( responseMap, (List<Circle>) circleMap.get( "circles" ) );

		// return circleMap;
	}

	public Map<String, Object> topicFilter( List<String> idsList, String type )
	{

		Map<String, Object> topicsMap = new HashMap<String, Object>();

		if ( type.equals( "researcher" ) )
		{
			List<Publication> publications = getPublicationsForFilter( idsList, type );
			ArrayList<Map<String, Object>> topicDetailsList = new ArrayList<Map<String, Object>>();

			List<String> allAuthorInterests = new ArrayList<String>();
			List<String> allAuthorInterestIds = new ArrayList<String>();

			List<Author> authorList = new ArrayList<Author>();
			authorList = getAuthorsFromIds( idsList );
			for ( Author a : authorList )
			{
				Set<AuthorInterestProfile> authorInterestProfiles = a.getAuthorInterestProfiles();
				for ( AuthorInterestProfile aip : authorInterestProfiles )
				{
					List<AuthorInterest> authorInterests = new ArrayList<AuthorInterest>( aip.getAuthorInterests() );
					for ( AuthorInterest ai : authorInterests )
					{
						Map<Interest, Double> termWeights = ai.getTermWeights();
						List<Interest> interests = new ArrayList<Interest>( termWeights.keySet() );
						// List<Double> weights = new ArrayList<Double>(
						// termWeights.values() );
						for ( int j = 0; j < termWeights.size(); j++ )
						{
							if ( !allAuthorInterests.contains( interests.get( j ).getTerm() ) ) // &&
																								// weights.get(
																								// j
																								// )
																								// >
																								// 0.5
																								// )
							{
								allAuthorInterests.add( interests.get( j ).getTerm() );
								allAuthorInterestIds.add( interests.get( j ).getId() );
							}
						}
					}
				}
			}

			List<String> allTopics = new ArrayList<String>();
			for ( Publication pub : publications )
			{

				Set<PublicationTopic> publicationTopics = pub.getPublicationTopics();
				for ( PublicationTopic pubTopic : publicationTopics )
				{

					for ( int i = 0; i < pubTopic.getTermValues().size(); i++ )
					{

						List<String> termValues = new ArrayList<>( pubTopic.getTermValues().keySet() );
						for ( int j = 0; j < termValues.size(); j++ )
						{
							Map<String, Object> topicDetail = new LinkedHashMap<String, Object>();
							if ( !allTopics.contains( termValues.get( j ) ) && allAuthorInterests.contains( termValues.get( j ) ) )
							{
								int index = allAuthorInterests.indexOf( termValues.get( j ) );
								allTopics.add( termValues.get( j ) );
								topicDetail.put( "id", allAuthorInterestIds.get( index ) );
								topicDetail.put( "title", termValues.get( j ) );
								topicDetailsList.add( topicDetail );
							}
						}

					}

				}
			}

			topicsMap.put( "topicDetailsList", topicDetailsList );
		}
		if ( type.equals( "conference" ) )
		{
			List<Publication> publications = getPublicationsForFilter( idsList, type );
			System.out.println( "PUBLICATIONS Of CONFERENCE: " + publications.size() );
			List<String> interestStrings = new ArrayList<String>();
			ArrayList<Map<String, Object>> topicDetailsList = new ArrayList<Map<String, Object>>();
			List<String> allConferenceInterests = new ArrayList<String>();
			List<String> allConferenceInterestIds = new ArrayList<String>();

			for ( int i = 0; i < idsList.size(); i++ )
			{
				@SuppressWarnings( "unchecked" )
				List<Object> innerList = (List<Object>) eventFeature.getEventMining().fetchEventGroupData( idsList.get( i ), null, null ).get( "events" );
				for ( int j = 0; j < innerList.size(); j++ )
				{
					@SuppressWarnings( "unchecked" )
					Map<String, Object> innerListMap = (Map<String, Object>) innerList.get( j );

					Event e = persistenceStrategy.getEventDAO().getById( innerListMap.get( "id" ).toString() );
					Set<EventInterestProfile> eips = e.getEventInterestProfiles();

					for ( EventInterestProfile eip : eips )
					{
						Set<EventInterest> eventInterests = eip.getEventInterests();
						for ( EventInterest ei : eventInterests )
						{

							Map<Interest, Double> termWeights = ei.getTermWeights();
							List<Interest> interests = new ArrayList<Interest>( termWeights.keySet() );
							for ( Interest interest : interests )
							{
								Map<String, Object> topicDetail = new LinkedHashMap<String, Object>();

								if ( !interestStrings.contains( interest.getTerm() ) )
								{
									allConferenceInterests.add( interest.getTerm() );
									allConferenceInterestIds.add( interest.getId() );
									// interestStrings.add( interest.getTerm()
									// );
									// topicDetail.put( "id", interest.getId()
									// );
									// topicDetail.put( "title",
									// interest.getTerm() );
								}
								// topi/cDetailsList.add( topicDetail );
							}

						}

					}

				}
			}
			List<String> allTopics = new ArrayList<String>();
			for ( Publication pub : publications )
			{

				Set<PublicationTopic> publicationTopics = pub.getPublicationTopics();
				for ( PublicationTopic pubTopic : publicationTopics )
				{

					for ( int i = 0; i < pubTopic.getTermValues().size(); i++ )
					{

						List<String> termValues = new ArrayList<>( pubTopic.getTermValues().keySet() );
						for ( int j = 0; j < termValues.size(); j++ )
						{
							Map<String, Object> topicDetail = new LinkedHashMap<String, Object>();
							if ( termValues.get( j ).equals( "populrer open" ) )
							{
								System.out.println( "\n" + pub.getTitle() );
								System.out.println( "filet: " + termValues.get( j ) );
							}
							if ( !allTopics.contains( termValues.get( j ) ) && allConferenceInterests.contains( termValues.get( j ) ) )
							{
								int index = allConferenceInterests.indexOf( termValues.get( j ) );
								allTopics.add( termValues.get( j ) );
								topicDetail.put( "id", allConferenceInterestIds.get( index ) );
								topicDetail.put( "title", termValues.get( j ) );
								topicDetailsList.add( topicDetail );
							}

						}

					}

				}
			}
			topicsMap.put( "topicDetailsList", topicDetailsList );

		}

		return topicsMap;

	}

	public Map<String, Object> timeResPubFilter( List<String> idsList, String type )
	{
		Map<String, Object> timeMap = new HashMap<String, Object>();
		List<Publication> publications = getPublicationsForFilter( idsList, type );

		int startYear = 0;
		int endYear = 0;

		for ( int i = 0; i < publications.size(); i++ )
		{
			if ( publications.get( i ).getYear() != null )
			{
				int year = Integer.parseInt( publications.get( i ).getYear() );

				if ( startYear == 0 && endYear == 0 )
				{
					startYear = year;
					endYear = year;
				}
				if ( year > endYear )
					endYear = year;
				if ( year < startYear )
					startYear = year;
			}
		}
		// System.out.println( "start: " + startYear + " end: " + endYear );

		timeMap.put( "startYear", startYear );
		timeMap.put( "endYear", endYear );
		return timeMap;
	}

	// application of filters
	public Set<Publication> getFilteredPublications( String type, List<Author> authorList, List<EventGroup> eventGroupList, List<Publication> filteredPublication, List<EventGroup> filteredConference, List<Interest> filteredTopic, List<Circle> filteredCircle, String startYear, String endYear )
	{
		Set<Publication> authorPublications = new HashSet<Publication>();

		if ( !filteredPublication.isEmpty() )
		{
			authorPublications = new HashSet<Publication>( filteredPublication );
		}
		else
		{
			authorPublications = typeWisePublications( type, authorList, eventGroupList );
			System.out.println( "filtered publications: " + authorPublications.size() );
		}

		System.out.println( "start year: " + startYear );
		List<Publication> publicationsTemp = new ArrayList<Publication>( authorPublications );
		if ( !startYear.equals( "" ) && !startYear.equals( "0" ) && startYear != null )
		{
			for ( int i = 0; i < publicationsTemp.size(); i++ )
			{
				if ( publicationsTemp.get( i ).getYear() != null )
				{
					if ( Integer.parseInt( publicationsTemp.get( i ).getYear() ) < Integer.parseInt( startYear ) || Integer.parseInt( publicationsTemp.get( i ).getYear() ) > Integer.parseInt( endYear ) )
					{
						System.out.println( publicationsTemp.get( i ).getTitle() + " " + publicationsTemp.get( i ).getYear() );
						publicationsTemp.remove( i );
						i--;
					}
				}
				else
				{
					System.out.println( "null " + publicationsTemp.get( i ).getTitle() + " " + publicationsTemp.get( i ).getYear() );

					if ( publicationsTemp.get( i ).getPublicationDate() != null )
					{
						String year = publicationsTemp.get( i ).getPublicationDate().toString().substring( 0, 4 );
						System.out.println( year + " year" );
						if ( Integer.parseInt( year ) < Integer.parseInt( startYear ) || Integer.parseInt( year ) > Integer.parseInt( endYear ) )
						{
							System.out.println( publicationsTemp.get( i ).getTitle() + " " + publicationsTemp.get( i ).getYear() );
							publicationsTemp.remove( i );
							i--;
						}
					}
					else
					{
						publicationsTemp.remove( i );
						i--;
					}
				}
			}
		}
		authorPublications = new HashSet<Publication>( publicationsTemp );
		System.out.println( "after year filter: " + authorPublications.size() );
		// conference filter
		List<Publication> conferencePublications = new ArrayList<Publication>();
		if ( !filteredConference.isEmpty() )
		{
			for ( int i = 0; i < filteredConference.size(); i++ )
			{
				List<Event> events = filteredConference.get( i ).getEvents();
				for ( int j = 0; j < events.size(); j++ )
				{
					List<Publication> eventPublications = events.get( j ).getPublications();
					for ( int k = 0; k < eventPublications.size(); k++ )
					{
						if ( !conferencePublications.contains( eventPublications.get( k ) ) )
						{
							conferencePublications.add( eventPublications.get( k ) );
						}
					}
				}
			}
			List<Publication> tempPubList = new ArrayList<Publication>( authorPublications );
			for ( int l = 0; l < tempPubList.size(); l++ )
			{
				if ( !conferencePublications.contains( tempPubList.get( l ) ) )
				{
					tempPubList.remove( l );
					l--;
				}
			}
			authorPublications = new HashSet<Publication>( tempPubList );
		}
		System.out.println( "after conference filter: " + authorPublications.size() );
		// topic filter
		if ( !filteredTopic.isEmpty() )
		{
			System.out.println( "filtered topic: " + filteredTopic.toString() );
			Set<Publication> topicPublications = new HashSet<Publication>();
			System.out.println( "filteringggggggggg" );
			for ( Publication authorPublication : authorPublications )
			{
				// if ( authorPublication.getTitle().equals( "Social Software
				// for Professional Learning" ) )
				// System.out.println( "\n" + authorPublication.getTitle() );
				List<PublicationTopic> pubTopics = new ArrayList<PublicationTopic>( authorPublication.getPublicationTopics() );
				for ( PublicationTopic pt : pubTopics )
				{
					Map<String, Double> termValues = pt.getTermValues();
					List<String> terms = new ArrayList<String>( termValues.keySet() );
					List<String> interests = new ArrayList<String>();
					for ( Interest interest : filteredTopic )
					{
						interests.add( interest.getTerm() );
					}
					if ( terms.containsAll( interests ) )
						{
						System.out.println( "true" );
						topicPublications.add( authorPublication );
						}
				}
			}
			authorPublications = topicPublications;
		}

		// circle filter
		if ( !filteredCircle.isEmpty() )
		{
			List<Publication> circlePublications = new ArrayList<Publication>();
			for ( int i = 0; i < filteredCircle.size(); i++ )
			{
				List<Publication> publications = new ArrayList<Publication>( filteredCircle.get( i ).getPublications() );
				for ( Publication p : publications )
				{
					if ( !circlePublications.contains( p ) )
					{
						circlePublications.add( p );
					}
				}
			}
			List<Publication> tempPubList = new ArrayList<Publication>( authorPublications );
			for ( int l = 0; l < tempPubList.size(); l++ )
			{
				if ( !circlePublications.contains( tempPubList.get( l ) ) )
				{
					tempPubList.remove( l );
					l--;
				}
			}
			authorPublications = new HashSet<Publication>( tempPubList );
		}

		for ( Publication p : authorPublications )
		{
			System.out.println( "short: " + p.getTitle() );
		}
		return authorPublications;
	}

	public Set<Publication> typeWisePublications( String type, List<Author> authorList, List<EventGroup> eventGroupList )
	{
		Set<Publication> authorPublications = new HashSet<Publication>();

		if ( type.equals( "researcher" ) )
		{
			// if there are more than one authors in consideration
			if ( authorList.size() > 1 )
			{
				List<Publication> authorPublicationsList = new ArrayList<Publication>( authorPublications );
				List<Publication> tempPubList = new ArrayList<Publication>();
				List<Integer> count = new ArrayList<Integer>();
				for ( int i = 0; i < authorList.size(); i++ )
				{
					tempPubList = new ArrayList<Publication>( authorList.get( i ).getPublications() );
					for ( int j = 0; j < tempPubList.size(); j++ )
					{
						if ( tempPubList.get( j ).getYear() != null || tempPubList.get( j ).getPublicationDate() != null )
						{
							if ( !authorPublicationsList.contains( tempPubList.get( j ) ) )
							{

								authorPublicationsList.add( tempPubList.get( j ) );
								count.add( 0 );
							}
							else
							{
								int index = authorPublicationsList.indexOf( tempPubList.get( j ) );
								count.set( index, count.get( index ) + 1 );
							}
						}

					}
				}
				for ( int i = 0; i < count.size(); i++ )
				{
					if ( count.get( i ) != authorList.size() - 1 )
					{
						authorPublicationsList.remove( i );
						count.remove( i );
						i--;
					}
				}
				authorPublications = new HashSet<Publication>( authorPublicationsList );
			}
			if ( authorList.size() == 1 )
			{
				if ( authorList.get( 0 ) != null )
				{
					List<Publication> pubs = new ArrayList<Publication>( authorList.get( 0 ).getPublications() );
					for ( int c = 0; c < pubs.size(); c++ )
					{
						// to not consider publication if year is null
						if ( pubs.get( c ).getYear() != null || pubs.get( c ).getPublicationDate() != null )
						{
							authorPublications.add( pubs.get( c ) );
						}
					}

				}
			}
		}
		if ( type.equals( "conference" ) )

		{
			// if there are more than one authors in consideration
			if ( eventGroupList.size() > 1 )
			{
				List<Publication> eventGroupPublicationsList = new ArrayList<Publication>( authorPublications );
				for ( int i = 0; i < eventGroupList.size(); i++ )
				{
					List<Event> groupEvents = eventGroupList.get( i ).getEvents();
					for ( Event e : groupEvents )
					{
						List<Publication> publications = e.getPublications();
						for ( int j = 0; j < publications.size(); j++ )
						{
							if ( !eventGroupPublicationsList.contains( publications.get( j ) ) && ( publications.get( j ).getYear() != null || publications.get( j ).getPublicationDate() != null ) )
							{
								eventGroupPublicationsList.add( publications.get( j ) );
							}
						}
					}
				}
				authorPublications = new HashSet<Publication>( eventGroupPublicationsList );
			}
			if ( eventGroupList.size() == 1 )
			{
				authorPublications = new HashSet<Publication>();

				// set of conditions!!
				if ( eventGroupList.get( 0 ) != null )
					if ( eventGroupList.get( 0 ).getEvents() != null )
					{
						List<Event> groupEvents = eventGroupList.get( 0 ).getEvents();
						for ( Event e : groupEvents )
						{
							List<Publication> publications = e.getPublications();
							for ( int j = 0; j < publications.size(); j++ )
							{
								if ( !authorPublications.contains( publications.get( j ) ) && ( publications.get( j ).getYear() != null || publications.get( j ).getPublicationDate() != null ) )
								{
									if ( publications.get( j ) != null && publications.get( j ).getPublicationDate() != null )
										publications.get( j ).setYear( publications.get( j ).getPublicationDate().toString().substring( 0, 4 ) );

									authorPublications.add( publications.get( j ) );
								}
							}
						}
					}
			}

		}
		if ( type.equals( "publication" ) )
		{

		}
		if ( type.equals( "topic" ) )
		{

		}
		if ( type.equals( "circle" ) )
		{

		}
		return authorPublications;
	}

	public List<Author> getAuthorsFromIds( List<String> idsList )
	{
		// get Author List
		List<Author> authorList = new ArrayList<Author>();
		for ( int itemIndex = 0; itemIndex < idsList.size(); itemIndex++ )
		{
			authorList.add( persistenceStrategy.getAuthorDAO().getById( idsList.get( itemIndex ) ) );
		}
		return authorList;

	}

	public List<EventGroup> getConferencesFromIds( List<String> idsList )
	{
		// get Event List
		List<EventGroup> eventGroupList = new ArrayList<EventGroup>();
		for ( int itemIndex = 0; itemIndex < idsList.size(); itemIndex++ )
		{
			eventGroupList.add( persistenceStrategy.getEventGroupDAO().getById( idsList.get( itemIndex ) ) );
		}
		return eventGroupList;

	}
}

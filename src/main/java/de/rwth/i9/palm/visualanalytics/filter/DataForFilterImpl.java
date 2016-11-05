package de.rwth.i9.palm.visualanalytics.filter;

import java.util.ArrayList;
import java.util.HashMap;
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
import de.rwth.i9.palm.model.CircleInterest;
import de.rwth.i9.palm.model.CircleInterestProfile;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventInterest;
import de.rwth.i9.palm.model.EventInterestProfile;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationTopic;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class DataForFilterImpl implements DataForFilter
{
	@Autowired
	private FilterHelper filterHelper;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private CircleFeature circleFeature;

	@Autowired
	private AcademicEventFeature eventFeature;

	public Map<String, Object> publicationFilter( List<String> idsList, String type )
	{

		Map<String, Object> publicationsMap = new HashMap<String, Object>();

		List<Publication> publications = filterHelper.getPublicationsForFilter( idsList, type );

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

		List<Publication> publications = filterHelper.getPublicationsForFilter( idsList, type );

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

	}

	public Map<String, Object> topicFilter( List<String> idsList, String type )
	{
		Map<String, Object> topicsMap = new HashMap<String, Object>();
		System.out.println( "type: " + type );
		ArrayList<Map<String, Object>> topicDetailsList = new ArrayList<Map<String, Object>>();

		if ( type.equals( "researcher" ) )
		{
			List<Publication> publications = filterHelper.getPublicationsForFilter( idsList, type );

			List<String> allAuthorInterests = new ArrayList<String>();
			List<String> allAuthorInterestIds = new ArrayList<String>();

			List<Author> authorList = new ArrayList<Author>();
			authorList = filterHelper.getAuthorsFromIds( idsList );
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
						for ( int j = 0; j < termWeights.size(); j++ )
						{
							if ( !allAuthorInterests.contains( interests.get( j ).getTerm() ) )
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
							if ( !allTopics.contains( termValues.get( j ) ) )
							{
								int index = 0;
								if ( allAuthorInterests.contains( termValues.get( j ) ) )
								{
									index = allAuthorInterests.indexOf( termValues.get( j ) );
									allTopics.add( termValues.get( j ) );
									topicDetail.put( "id", allAuthorInterestIds.get( index ) );
									topicDetail.put( "title", termValues.get( j ) );
									topicDetailsList.add( topicDetail );
								}
								if ( allAuthorInterests.contains( termValues.get( j ).substring( 0, termValues.get( j ).length() - 1 ) ) )
								{
									index = allAuthorInterests.indexOf( termValues.get( j ).substring( 0, termValues.get( j ).length() - 1 ) );
									allTopics.add( termValues.get( j ) );
									topicDetail.put( "id", allAuthorInterestIds.get( index ) );
									topicDetail.put( "title", termValues.get( j ).substring( 0, termValues.get( j ).length() - 1 ) );
									topicDetailsList.add( topicDetail );
								}
							}
						}
					}
				}
			}

		}
		if ( type.equals( "conference" ) )
		{
			List<Publication> publications = filterHelper.getPublicationsForFilter( idsList, type );
			List<String> interestStrings = new ArrayList<String>();
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
								}
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
							if ( !allTopics.contains( termValues.get( j ) ) )
							{
								int index = 0;// allConferenceInterests.indexOf(
												// termValues.get( j ) );
								// allTopics.add( termValues.get( j ) );
								// topicDetail.put( "id",
								// allConferenceInterestIds.get( index ) );
								// topicDetail.put( "title", termValues.get( j )
								// );
								// topicDetailsList.add( topicDetail );

								if ( allConferenceInterests.contains( termValues.get( j ) ) )
								{
									index = allConferenceInterests.indexOf( termValues.get( j ) );
									allTopics.add( termValues.get( j ) );
									topicDetail.put( "id", allConferenceInterestIds.get( index ) );
									topicDetail.put( "title", termValues.get( j ) );
									topicDetailsList.add( topicDetail );
								}
								else if ( allConferenceInterests.contains( termValues.get( j ).substring( 0, termValues.get( j ).length() - 1 ) ) )
								{
									index = allConferenceInterests.indexOf( termValues.get( j ).substring( 0, termValues.get( j ).length() - 1 ) );
									allTopics.add( termValues.get( j ) );
									topicDetail.put( "id", allConferenceInterestIds.get( index ) );
									topicDetail.put( "title", termValues.get( j ).substring( 0, termValues.get( j ).length() - 1 ) );
									topicDetailsList.add( topicDetail );
								}
							}
						}
					}
				}
			}

		}
		if ( type.equals( "publication" ) )
		{
			System.out.println( "1" );
			List<Publication> publications = filterHelper.getPublicationsForFilter( idsList, type );

			List<Interest> allInterestsInDB = persistenceStrategy.getInterestDAO().allTerms();

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
							// System.out.println( termValues.get( j ) );
							Map<String, Object> topicDetail = new LinkedHashMap<String, Object>();
							if ( !allTopics.contains( termValues.get( j ) ) )
							{
								int index = 0; // allPublicationInterests.indexOf(
												// termValues.get( j ) );
								// allTopics.add( termValues.get( j ) );
								// topicDetail.put( "id",
								// allPublicationInterestIds.get( index ) );
								// topicDetail.put( "title", termValues.get( j )
								// );
								// topicDetailsList.add( topicDetail );
								if ( allPublicationInterests.contains( termValues.get( j ) ) )
								{
									index = allPublicationInterests.indexOf( termValues.get( j ) );
									allTopics.add( termValues.get( j ) );
									topicDetail.put( "id", allPublicationInterestIds.get( index ) );
									topicDetail.put( "title", termValues.get( j ) );
									topicDetailsList.add( topicDetail );
								}
								else if ( allPublicationInterests.contains( termValues.get( j ).substring( 0, termValues.get( j ).length() - 1 ) ) )
								{
									index = allPublicationInterests.indexOf( termValues.get( j ).substring( 0, termValues.get( j ).length() - 1 ) );
									allTopics.add( termValues.get( j ) );
									topicDetail.put( "id", allPublicationInterestIds.get( index ) );
									topicDetail.put( "title", termValues.get( j ).substring( 0, termValues.get( j ).length() - 1 ) );
									topicDetailsList.add( topicDetail );
								}
							}
						}
					}
				}
			}

		}
		if ( type.equals( "circle" ) )
		{

			List<Publication> publications = filterHelper.getPublicationsForFilter( idsList, type );

			List<String> allCircleInterests = new ArrayList<String>();
			List<String> allCircleInterestIds = new ArrayList<String>();

			List<Circle> circleList = new ArrayList<Circle>();
			circleList = filterHelper.getCirclesFromIds( idsList );
			for ( Circle c : circleList )
			{
				Set<CircleInterestProfile> circleInterestProfiles = c.getCircleInterestProfiles();
				for ( CircleInterestProfile cip : circleInterestProfiles )
				{
					List<CircleInterest> circleInterests = new ArrayList<CircleInterest>( cip.getCircleInterests() );
					for ( CircleInterest ci : circleInterests )
					{
						Map<Interest, Double> termWeights = ci.getTermWeights();
						List<Interest> interests = new ArrayList<Interest>( termWeights.keySet() );
						for ( int j = 0; j < termWeights.size(); j++ )
						{
							if ( !allCircleInterests.contains( interests.get( j ).getTerm() ) )
							{
								allCircleInterests.add( interests.get( j ).getTerm() );
								allCircleInterestIds.add( interests.get( j ).getId() );
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
							if ( !allTopics.contains( termValues.get( j ) ) )
							{
								int index = 0;
								if ( allCircleInterests.contains( termValues.get( j ) ) )
								{
									index = allCircleInterests.indexOf( termValues.get( j ) );
									allTopics.add( termValues.get( j ) );
									topicDetail.put( "id", allCircleInterestIds.get( index ) );
									topicDetail.put( "title", termValues.get( j ) );
									topicDetailsList.add( topicDetail );
								}
								if ( allCircleInterests.contains( termValues.get( j ).substring( 0, termValues.get( j ).length() - 1 ) ) )
								{
									index = allCircleInterests.indexOf( termValues.get( j ).substring( 0, termValues.get( j ).length() - 1 ) );
									allTopics.add( termValues.get( j ) );
									topicDetail.put( "id", allCircleInterestIds.get( index ) );
									topicDetail.put( "title", termValues.get( j ).substring( 0, termValues.get( j ).length() - 1 ) );
									topicDetailsList.add( topicDetail );
								}
							}
						}
					}
				}
			}

		}
		topicsMap.put( "topicDetailsList", topicDetailsList );
		return topicsMap;

	}

	public Map<String, Object> timeFilter( List<String> idsList, String type )
	{
		Map<String, Object> timeMap = new HashMap<String, Object>();
		List<Publication> publications = filterHelper.getPublicationsForFilter( idsList, type );

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
		timeMap.put( "startYear", startYear );
		timeMap.put( "endYear", endYear );
		return timeMap;
	}

}

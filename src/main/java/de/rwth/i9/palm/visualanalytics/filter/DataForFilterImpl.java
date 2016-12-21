package de.rwth.i9.palm.visualanalytics.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

//import de.rwth.i9.palm.feature.academicevent.AcademicEventFeature;
import de.rwth.i9.palm.feature.circle.CircleFeature;
import de.rwth.i9.palm.helper.VADataFetcher;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorInterest;
import de.rwth.i9.palm.model.AuthorInterestProfile;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.CircleInterest;
import de.rwth.i9.palm.model.CircleInterestProfile;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
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
	private VADataFetcher dataFetcher;

	// @Autowired
	// private AcademicEventFeature eventFeature;

	public Map<String, Object> publicationFilter( List<String> idsList, String type, String visType, HttpServletRequest request )
	{
		Map<String, Object> publicationsMap = new HashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			List<Publication> publications = filterHelper.getPublicationsForFilter( idsList, type, visType, request );

			ArrayList<Map<String, Object>> publicationDetailsList = new ArrayList<Map<String, Object>>();

			for ( int i = 0; i < publications.size(); i++ )
			{
				Map<String, Object> publicationDetail = new LinkedHashMap<String, Object>();

				publicationDetail.put( "id", publications.get( i ).getId() );
				publicationDetail.put( "name", publications.get( i ).getTitle() );

				publicationDetailsList.add( publicationDetail );
			}

			publicationsMap.put( "publicationsList", publicationDetailsList );
		}
		return publicationsMap;
	}

	public Map<String, Object> conferenceFilter( List<String> idsList, String type, String visType, HttpServletRequest request )
	{

		Map<String, Object> eventsMap = new HashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			List<Publication> publications = filterHelper.getPublicationsForFilter( idsList, type, visType, request );

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
							eventDetail.put( "name", publications.get( i ).getEvent().getEventGroup().getName() );
							eventDetailsList.add( eventDetail );
						}
					}
				}
			}

			eventsMap.put( "eventsList", eventDetailsList );
		}
		return eventsMap;
	}

	@SuppressWarnings( "unchecked" )
	public Map<String, Object> circleFilter( List<String> idsList, String type, String visType )
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

	@SuppressWarnings( "unchecked" )
	public Map<String, Object> topicFilter( List<String> idsList, String type, String visType, HttpServletRequest request )
	{
		Map<String, Object> topicsMap = new HashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			List<Map<String, Object>> topicDetailsList = new ArrayList<Map<String, Object>>();

			if ( type.equals( "researcher" ) )
			{
				List<Publication> publications = filterHelper.getPublicationsForFilter( idsList, type, visType, request );
				List<String> allAuthorInterests = new ArrayList<String>();
				List<Integer> count = new ArrayList<Integer>();

				// show interests if multiple selected researchers are directly
				// connected to each
				// other
				List<Publication> commonPublications = new ArrayList<Publication>();
				if ( idsList.size() >= 2 )
				{
					List<Integer> counter = new ArrayList<Integer>();
					for ( String id : idsList )
					{
						Author a = persistenceStrategy.getAuthorDAO().getById( id );
						List<Publication> pubs = new ArrayList<Publication>( a.getPublications() );
						for ( Publication p : pubs )
						{
							if ( !commonPublications.contains( p ) )
							{
								commonPublications.add( p );
								counter.add( 0 );
							}
							else
							{
								int index = commonPublications.indexOf( p );
								counter.set( index, counter.get( index ) + 1 );
							}
						}
					}
					for ( int i = 0; i < commonPublications.size(); i++ )
					{
						if ( counter.get( i ) < idsList.size() - 1 )
						{
							counter.remove( i );
							commonPublications.remove( i );
							i--;
						}
					}
				}
				System.out.println( commonPublications.size() + " size" );


				if ( ( commonPublications != null && !commonPublications.isEmpty() ) || idsList.size() < 2 )
				{
					for ( String id : idsList )
					{
						List<String> allTopics = new ArrayList<String>();

						Author a = persistenceStrategy.getAuthorDAO().getById( id );
						List<Publication> pubs = new ArrayList<Publication>( a.getPublications() );
						for ( Publication p : pubs )
						{
							Set<PublicationTopic> publicationTopics = p.getPublicationTopics();
							for ( PublicationTopic pubTopic : publicationTopics )
							{
								List<Double> topicWeights = new ArrayList<Double>( pubTopic.getTermValues().values() );
								List<String> topics = new ArrayList<String>( pubTopic.getTermValues().keySet() );
								for ( int i = 0; i < topics.size(); i++ )
								{
									if ( !allTopics.contains( topics.get( i ) ) && topicWeights.get( i ) > 0.3 )
									{
										allTopics.add( topics.get( i ) );
									}
								}
							}
						}

						List<String> interestTopicNames = new ArrayList<String>();
						List<String> interestTopicIds = new ArrayList<String>();

						Set<AuthorInterestProfile> authorInterestProfiles = a.getAuthorInterestProfiles();
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
									String interest = ( actualInterest.getTerm() );
									Double weight = interestTermWeight.next();

									if ( weight > 0.3 )
									{
										if ( allTopics.contains( interest ) || allTopics.contains( interest + "s" ) )
										{
											if ( !interestTopicNames.contains( interest ) )
											{
												interestTopicNames.add( interest );
												interestTopicIds.add( actualInterest.getId() );
												Map<String, Object> items = new HashMap<String, Object>();
												items.put( "name", interest );
												items.put( "id", actualInterest.getId() );
											}
										}
									}
								}
							}
						}

						for ( int k = 0; k < interestTopicNames.size(); k++ )
						{
							if ( !allAuthorInterests.contains( interestTopicNames.get( k ) ) )
							{
								allAuthorInterests.add( interestTopicNames.get( k ) );
								Map<String, Object> items = new HashMap<String, Object>();
								items.put( "name", interestTopicNames.get( k ) );
								items.put( "id", interestTopicIds.get( k ) );
								topicDetailsList.add( items );

								count.add( 0 );

							}
							else
								count.set( allAuthorInterests.indexOf( interestTopicNames.get( k ) ), count.get( allAuthorInterests.indexOf( interestTopicNames.get( k ) ) ) + 1 );
						}
					}

					for ( int i = 0; i < allAuthorInterests.size(); i++ )
					{
						if ( count.get( i ) < idsList.size() - 1 )
						{
							count.remove( i );
							allAuthorInterests.remove( i );
							topicDetailsList.remove( i );
							i--;
						}
					}
				}
			}
			if ( type.equals( "conference" ) )
			{
				List<Publication> publications = filterHelper.getPublicationsForFilter( idsList, type, visType, request );
				List<String> allConferenceInterests = new ArrayList<String>();
				List<Integer> count = new ArrayList<Integer>();

				// find topics from all the conferences
				for ( String id : idsList )
				{
					List<String> allTopics = new ArrayList<String>();

					EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( id );
					List<Event> events = eg.getEvents();
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
								for ( int i = 0; i < topics.size(); i++ )
								{
									if ( !allTopics.contains( topics.get( i ) ) && topicWeights.get( i ) > 0.3 )
									{
										allTopics.add( topics.get( i ) );
									}
								}
							}
						}
					}

					List<String> interestTopicNames = new ArrayList<String>();
					List<String> interestTopicIds = new ArrayList<String>();

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
									String interest = actualInterest.getTerm();
									Double weight = interestTermWeight.next();
									if ( weight > 0.3 )
									{
										if ( allTopics.contains( interest ) || allTopics.contains( interest + "s" ) )
										{
											if ( !interestTopicNames.contains( interest ) )
											{
												interestTopicNames.add( interest );
												interestTopicIds.add( actualInterest.getId() );
												Map<String, Object> items = new HashMap<String, Object>();
												items.put( "name", interest );
												items.put( "id", actualInterest.getId() );
											}
										}
									}
								}
							}
						}
					}
					for ( int k = 0; k < interestTopicNames.size(); k++ )
					{
						if ( !allConferenceInterests.contains( interestTopicNames.get( k ) ) )
						{
							allConferenceInterests.add( interestTopicNames.get( k ) );
							Map<String, Object> items = new HashMap<String, Object>();
							items.put( "name", interestTopicNames.get( k ) );
							items.put( "id", interestTopicIds.get( k ) );
							topicDetailsList.add( items );

							count.add( 0 );

						}
						else
							count.set( allConferenceInterests.indexOf( interestTopicNames.get( k ) ), count.get( allConferenceInterests.indexOf( interestTopicNames.get( k ) ) ) + 1 );
					}
				}
				for ( int i = 0; i < allConferenceInterests.size(); i++ )
				{
					if ( count.get( i ) < idsList.size() - 1 )
					{
						count.remove( i );
						allConferenceInterests.remove( i );
						topicDetailsList.remove( i );
						i--;
					}
				}
			}
			if ( type.equals( "publication" ) )
			{
				List<Publication> publications = filterHelper.getPublicationsForFilter( idsList, type, visType, request );

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
					// System.out.println( "pub title: " + pub.getTitle() );
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
									if ( allPublicationInterests.contains( termValues.get( j ) ) )
									{
										index = allPublicationInterests.indexOf( termValues.get( j ) );
										allTopics.add( termValues.get( j ) );
										topicDetail.put( "id", allPublicationInterestIds.get( index ) );
										topicDetail.put( "name", termValues.get( j ) );
										topicDetailsList.add( topicDetail );
										// System.out.println( " topic w s: " +
										// termValues.get( j ) );
									}
									else if ( allPublicationInterests.contains( termValues.get( j ).substring( 0, termValues.get( j ).length() - 1 ) ) )
									{
										index = allPublicationInterests.indexOf( termValues.get( j ).substring( 0, termValues.get( j ).length() - 1 ) );
										allTopics.add( termValues.get( j ) );
										topicDetail.put( "id", allPublicationInterestIds.get( index ) );
										topicDetail.put( "name", termValues.get( j ).substring( 0, termValues.get( j ).length() - 1 ) );
										topicDetailsList.add( topicDetail );
										// System.out.println( " topic w/ s: " +
										// termValues.get( j ).substring( 0,
										// termValues.get( j ).length() - 1 ) );
									}
								}
							}
						}
					}
				}

			}
			if ( type.equals( "circle" ) )
			{
				List<Publication> publications = filterHelper.getPublicationsForFilter( idsList, type, visType, request );
				List<String> allCircleInterests = new ArrayList<String>();
				// List<String> allCircleInterestIds = new ArrayList<String>();
				List<Integer> count = new ArrayList<Integer>();

				// find all the topics from the circles
				for ( String id : idsList )
				{
					List<String> allTopics = new ArrayList<String>();

					Circle circle = persistenceStrategy.getCircleDAO().getById( id );

					List<Publication> pubs = new ArrayList<Publication>( circle.getPublications() );
					for ( Publication p : pubs )
					{
						Set<PublicationTopic> publicationTopics = p.getPublicationTopics();
						for ( PublicationTopic pubTopic : publicationTopics )
						{
							List<Double> topicWeights = new ArrayList<Double>( pubTopic.getTermValues().values() );
							List<String> topics = new ArrayList<String>( pubTopic.getTermValues().keySet() );
							for ( int i = 0; i < topics.size(); i++ )
							{
								if ( !allTopics.contains( topics.get( i ) ) && topicWeights.get( i ) > 0.3 )
								{
									allTopics.add( topics.get( i ) );
								}
							}
						}
					}

					List<String> interestTopicNames = new ArrayList<String>();
					List<String> interestTopicIds = new ArrayList<String>();

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
										if ( !interestTopicNames.contains( interest ) )
										{
											interestTopicNames.add( interest );
											interestTopicIds.add( actualInterest.getId() );
											Map<String, Object> items = new HashMap<String, Object>();
											items.put( "name", interest );
											items.put( "id", actualInterest.getId() );
										}
									}
								}
							}
						}
					}
					for ( int k = 0; k < interestTopicNames.size(); k++ )
					{
						if ( !allCircleInterests.contains( interestTopicNames.get( k ) ) )
						{
							allCircleInterests.add( interestTopicNames.get( k ) );
							Map<String, Object> items = new HashMap<String, Object>();
							items.put( "name", interestTopicNames.get( k ) );
							items.put( "id", interestTopicIds.get( k ) );
							topicDetailsList.add( items );

							count.add( 0 );

						}
						else
							count.set( allCircleInterests.indexOf( interestTopicNames.get( k ) ), count.get( allCircleInterests.indexOf( interestTopicNames.get( k ) ) ) + 1 );
					}
				}
				for ( int i = 0; i < allCircleInterests.size(); i++ )
				{
					if ( count.get( i ) < idsList.size() - 1 )
					{
						count.remove( i );
						allCircleInterests.remove( i );
						topicDetailsList.remove( i );
						i--;
					}
				}
			}
			topicsMap.put( "topicDetailsList", topicDetailsList );
		}
		return topicsMap;

	}

	public Map<String, Object> timeFilter( List<String> idsList, String type, String visType, HttpServletRequest request )
	{
		Map<String, Object> timeMap = new HashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			int startYear = 0;
			int endYear = 0;

			if ( type.equals( "conference" ) && idsList.size() > 1 )
			{
				List<Integer> startYearsOfConferences = new ArrayList<Integer>();
				List<Integer> endYearsOfConferences = new ArrayList<Integer>();
				for ( String id : idsList )
				{

					startYear = 0;
					endYear = 0;
					EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( id );

					List<Event> groupEvents = eg.getEvents();
					for ( Event e : groupEvents )
					{
						List<Publication> eventGroupPublications = e.getPublications();
						for ( Publication p : eventGroupPublications )
						{
							if ( p.getYear() != null )
							{
								int year = Integer.parseInt( p.getYear() );

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
					}
					startYearsOfConferences.add( startYear );
					endYearsOfConferences.add( endYear );
				}

				for ( int i = 0; i < idsList.size(); i++ )
				{
					if ( startYearsOfConferences.get( i ) > startYear )
						startYear = startYearsOfConferences.get( i );
					if ( endYearsOfConferences.get( i ) < endYear )
						endYear = endYearsOfConferences.get( i );
				}
			}
			else
			{
				List<Publication> publications = filterHelper.getPublicationsForFilter( idsList, type, visType, request );

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
			}
			timeMap.put( "startYear", startYear );
			timeMap.put( "endYear", endYear );
		}
		return timeMap;
	}

}

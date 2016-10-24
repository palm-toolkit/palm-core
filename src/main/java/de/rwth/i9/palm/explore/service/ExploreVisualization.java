package de.rwth.i9.palm.explore.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rwth.i9.palm.cluster.service.ClusteringService;
import de.rwth.i9.palm.feature.academicevent.AcademicEventFeature;
import de.rwth.i9.palm.feature.researcher.ResearcherFeature;
import de.rwth.i9.palm.graph.feature.GraphFeature;
import de.rwth.i9.palm.helper.comparator.PublicationByDateComparator;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorInterest;
import de.rwth.i9.palm.model.AuthorInterestProfile;
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
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.similarity.service.SimilarityService;

@Component
public class ExploreVisualization
{
	@Autowired
	private ResearcherFeature researcherFeature;

	@Autowired
	private AcademicEventFeature eventFeature;

	@Autowired
	private GraphFeature graphFeature;

	@Autowired
	private ClusteringService clusteringService;

	@Autowired
	private SimilarityService similarityService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	private ObjectMapper mapper = new ObjectMapper();

	public Map<String, Object> visualizeNetwork( String type, List<Author> authorList, Set<Publication> publications, List<String> idsList, String startYear, String endYear, String authoridForCoAuthors )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();
		List<Integer> count = new ArrayList<Integer>();
		List<Author> eventGroupAuthors = new ArrayList<Author>();

		if ( type.equals( "conference" ) )
		{

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
				// System.out.println( "event authors size: " +
				// eventAuthors.size() );
				for ( Author a : eventAuthors )
				{
					if ( !eventGroupAuthors.contains( a ) )
					{
						eventGroupAuthors.add( a );
						count.add( 0 );
					}
					else
					{
						count.set( eventGroupAuthors.indexOf( a ), count.get( eventGroupAuthors.indexOf( a ) ) + 1 );
					}
				}
			}
			// System.out.println( count.size() + " " + count.toString() );
			// System.out.println( eventGroupAuthors.size() + " " +
			// eventGroupAuthors.toString() );

			for ( int i = 0; i < eventGroupAuthors.size(); i++ )
			{
				if ( count.get( i ) != idsList.size() - 1 )
				{
					count.remove( i );
					eventGroupAuthors.remove( i );
					i--;
				}
				// else
				// System.out.println( eventGroupAuthors.get( i ).getName() );
			}
		}
		Author authorForCoAuthors = new Author();
		if ( authoridForCoAuthors != null )
		{
			authorForCoAuthors = persistenceStrategy.getAuthorDAO().getById( authoridForCoAuthors );
		}
		// System.out.println( "event group size " + eventGroupAuthors.size() );
		visMap.put( "graphFile", graphFeature.getGephiGraph( type, authorList, publications, idsList, eventGroupAuthors, authorForCoAuthors ).get( "graphFile" ) );
		// System.out.println( "response map for data transfer: " +
		// visMap.toString() );
		return visMap;
	}

	public Map<String, Object> visualizeLocations( String type, Set<Publication> publications, List<String> idsList, String startYear, String endYear, List<Interest> filteredTopic )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		if ( type.equals( "researcher" ) || type.equals( "publication" ) )
			visMap.putAll( researcherFeature.getResearcherAcademicEventTree().getResearcherAllAcademicEvents( publications, true ) );

		if ( type.equals( "conference" ) )
		{
			Boolean valid = false;
			if ( startYear.equals( "" ) || startYear.equals( "0" ) )
			{
				valid = true;
			}
			List<Object> listEvents = new ArrayList<Object>();
			for ( int i = 0; i < idsList.size(); i++ )
			{
				// System.out.println( " name in vis: " +
				// eventFeature.getEventMining().fetchEventGroupData(
				// idsList.get( i ), null, null ).get( "name" ) );
				@SuppressWarnings( "unchecked" )
				List<Object> innerList = (List<Object>) eventFeature.getEventMining().fetchEventGroupData( idsList.get( i ), null, null ).get( "events" );
				for ( int j = 0; j < innerList.size(); j++ )
				{
					@SuppressWarnings( "unchecked" )
					Map<String, Object> innerListMap = (Map<String, Object>) innerList.get( j );

					if ( !startYear.equals( "" ) && !startYear.equals( "0" ) )
						if ( Integer.parseInt( startYear ) <= Integer.parseInt( innerListMap.get( "year" ).toString() ) && Integer.parseInt( endYear ) >= Integer.parseInt( innerListMap.get( "year" ).toString() ) )
							valid = true;
						else
							valid = false;

					if ( !filteredTopic.isEmpty() && valid )
					{
						Event e = persistenceStrategy.getEventDAO().getById( innerListMap.get( "id" ).toString() );
						Set<EventInterestProfile> eips = e.getEventInterestProfiles();
						List<String> interestStrings = new ArrayList<String>();

						for ( EventInterestProfile eip : eips )
						{
							Set<EventInterest> eventInterests = eip.getEventInterests();
							for ( EventInterest ei : eventInterests )
							{
								Map<Interest, Double> termWeights = ei.getTermWeights();
								List<Interest> interests = new ArrayList<Interest>( termWeights.keySet() );
								// List<Double> weights = new
								// ArrayList<Double>(termWeights.values());
								for ( Interest interest : interests )
								{
									if ( !interestStrings.contains( interest.getTerm() ) )
										interestStrings.add( interest.getTerm() );
								}
							}
						}
						// System.out.println( interestStrings );
						List<String> interests = new ArrayList<String>();
						for ( Interest interest : filteredTopic )
						{
							// interests.add( interest.getTerm() );
							if ( interestStrings.contains( interest.getTerm() ) )
								interests.add( interest.getTerm() );
							if ( interestStrings.contains( interest.getTerm() + "s" ) )
								interests.add( interest.getTerm() + "s" );
						}

						if ( interests.size() == filteredTopic.size() )
						{
							valid = true;
						}
						else
							valid = false;

						// System.out.println( "\nvalid: " + valid );
					}

					if ( valid )
						listEvents.add( innerList.get( j ) );
				}
			}
			visMap.put( "events", listEvents );
			// System.out.println( "VISMAP: " + visMap.toString() );

		}

		System.out.println( visMap.toString() );
		return visMap;
	}

	public Map<String, Object> visualizeTimeline( Set<Publication> publications )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		List<Publication> publicationsList = new ArrayList<Publication>( publications );
		// sort by date
		Collections.sort( publicationsList, new PublicationByDateComparator() );

		List<Map<String, Object>> pubDetailsList = new ArrayList<Map<String, Object>>();
		// System.out.println( "\n*************************************\n" );
		for ( Publication pub : publicationsList )
		{
			Map<String, Object> pubDetails = new LinkedHashMap<String, Object>();

			// System.out.println( pub.getPublicationDate() + " " +
			// pub.getTitle() );

			pubDetails.put( "id", pub.getId() );
			pubDetails.put( "title", pub.getTitle() );
			pubDetails.put( "year", pub.getYear() );
			pubDetails.put( "type", pub.getPublicationType() );
			pubDetails.put( "date", pub.getPublicationDate() );
			pubDetailsList.add( pubDetails );
		}

		visMap.put( "pubDetailsList", pubDetailsList );

		return visMap;
	}

	public Map<String, Object> visualizeEvolution( String type, List<String> idsList, List<Author> authorList, Set<Publication> publications, String startYear, String endYear )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();
		Map<String, String> topicIdMap = new HashMap<String, String>();

		List<String> allTopics = new ArrayList<String>();
		for ( Publication pub : publications )
		{
			Set<PublicationTopic> publicationTopics = pub.getPublicationTopics();
			for ( PublicationTopic pubTopic : publicationTopics )
			{
				List<Double> topicWeights = new ArrayList<Double>( pubTopic.getTermValues().values() );
				List<String> topics = new ArrayList<String>( pubTopic.getTermValues().keySet() );
				for ( int i = 0; i < topics.size(); i++ )
				{
					if ( !allTopics.contains( topics.get( i ) ) && topicWeights.get( i ) > 0.2 )
					{
						allTopics.add( topics.get( i ) );
					}
				}
			}
		}

		List<Map<String, Object>> mapList = new ArrayList<Map<String, Object>>();

		if ( type.equals( "researcher" ) )
		{
			// If there are no common publications!
			if ( allTopics.size() == 0 )
			{
				for ( Author a : authorList )
				{
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
								if ( !allTopics.contains( topics.get( i ) ) && topicWeights.get( i ) > 0.2 )
								{
									allTopics.add( topics.get( i ) );
								}
							}
						}
					}
				}
			}
			List<Interest> authorInterests = new ArrayList<Interest>();
			List<Double> authorInterestWeights = new ArrayList<Double>();

			System.out.println( " all topics size: " + allTopics.size() );

			for ( Author a : authorList )
			{
				Map<String, List<Interest>> yearWiseInterests = new HashMap<String, List<Interest>>();

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
							String interest = actualInterest.getTerm();
							Double weight = interestTermWeight.next();

							if ( allTopics.contains( interest ) || allTopics.contains( interest + "s" ) )
							{
								Boolean validYear = true;
								Calendar calendar = Calendar.getInstance();
								calendar.setTime( ai.getYear() );
								String year = Integer.toString( calendar.get( Calendar.YEAR ) );
								if ( startYear.equals( "0" ) || startYear.equals( "" ) )
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
									List<String> yWI = new ArrayList<String>( yearWiseInterests.keySet() );
									List<List<Interest>> yWIVal = new ArrayList<List<Interest>>( yearWiseInterests.values() );
									if ( yWI.contains( year ) )
									{
										int index = yWI.indexOf( year );
										if ( !yWIVal.get( index ).contains( actualInterest ) )
										{
											yWIVal.get( index ).add( actualInterest );
											if ( !authorInterests.contains( actualInterest ) )
											{
												authorInterests.add( actualInterest );
												authorInterestWeights.add( weight );
											}
											else
											{
												int ind = authorInterests.indexOf( actualInterest );
												authorInterestWeights.set( ind, authorInterestWeights.get( ind ) + weight );
											}
											Map<String, Object> values = new HashMap<String, Object>();
											System.out.println( actualInterest.getTerm() + " : " + actualInterest.getId() );
											values.put( "Author", a.getName() );
											values.put( "Topic", actualInterest.getTerm() );
											values.put( "TopicId", actualInterest.getId() );
											values.put( "Year", year );
											values.put( "Weight", weight );
											mapList.add( values );
											topicIdMap.put( actualInterest.getTerm(), actualInterest.getId() );
										}
										else
										{
											int ind = authorInterests.indexOf( actualInterest );
											authorInterestWeights.set( ind, authorInterestWeights.get( ind ) + weight );
										}
									}
									else
									{
										List<Interest> newInterestList = new ArrayList<Interest>();
										newInterestList.add( actualInterest );
										if ( !authorInterests.contains( actualInterest ) )
										{
											authorInterests.add( actualInterest );
											authorInterestWeights.add( weight );
										}
										else
										{
											int ind = authorInterests.indexOf( actualInterest );
											authorInterestWeights.set( ind, authorInterestWeights.get( ind ) + weight );
										}
										yearWiseInterests.put( year, newInterestList );

										Map<String, Object> values = new HashMap<String, Object>();

										values.put( "Author", a.getName() );
										values.put( "Topic", actualInterest.getTerm() );
										values.put( "TopicId", actualInterest.getId() );
										values.put( "Year", year );
										values.put( "Weight", weight );
										mapList.add( values );
										topicIdMap.put( actualInterest.getTerm(), actualInterest.getId() );
									}
								}
							}
						}
					}
				}
			}
			System.out.println( "map list size: " + mapList.size() );
			double threshold = 3.0;
			if ( authorList.size() > 1 )
			{
				threshold = 2.0;
			}
			for ( int i = 0; i < authorInterests.size(); i++ )
			{
				if ( authorInterestWeights.get( i ) < threshold )
				{
					authorInterestWeights.remove( i );
					authorInterests.remove( i );
					i--;
				}

			}
			for ( int i = 0; i < mapList.size(); i++ )
			{
				Boolean flag = false;
				for ( Interest interest : authorInterests )
				{
					if ( interest.getTerm().equals( mapList.get( i ).get( "Topic" ).toString() ) )
						flag = true;
				}
				if ( !flag )
				{
					mapList.remove( i );
					i--;
				}
			}

			System.out.println( "ma lst: " + mapList.size() );

			visMap.put( "list", mapList );
			visMap.put( "topicIdMap", topicIdMap );
		}
		if ( type.equals( "conference" ) )
		{
			// If there are no common publications!
			if ( allTopics.size() == 0 )
			{
				for ( String id : idsList )
				{
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
									if ( !allTopics.contains( topics.get( i ) ) && topicWeights.get( i ) > 0.2 )
									{
										allTopics.add( topics.get( i ) );
									}
								}
							}
						}
					}
				}
			}

			List<Interest> conferenceInterests = new ArrayList<Interest>();
			List<Double> conferenceInterestWeights = new ArrayList<Double>();

			for ( String id : idsList )
			{
				EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( id );
				Map<String, List<Interest>> yearWiseInterests = new HashMap<String, List<Interest>>();

				List<Event> events = eg.getEvents();
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

								if ( allTopics.contains( interest ) || allTopics.contains( interest + "s" ) )
								{
									Boolean validYear = true;
									Calendar calendar = Calendar.getInstance();
									calendar.setTime( ei.getYear() );
									String year = Integer.toString( calendar.get( Calendar.YEAR ) );
									if ( startYear.equals( "0" ) || startYear.equals( "" ) )
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
										List<String> yWI = new ArrayList<String>( yearWiseInterests.keySet() );
										List<List<Interest>> yWIVal = new ArrayList<List<Interest>>( yearWiseInterests.values() );
										if ( yWI.contains( year ) )
										{
											int index = yWI.indexOf( year );
											if ( !yWIVal.get( index ).contains( actualInterest ) )
											{
												yWIVal.get( index ).add( actualInterest );
												if ( !conferenceInterests.contains( actualInterest ) )
												{
													conferenceInterests.add( actualInterest );
													conferenceInterestWeights.add( weight );
												}
												else
												{
													int ind = conferenceInterests.indexOf( actualInterest );
													conferenceInterestWeights.set( ind, conferenceInterestWeights.get( ind ) + weight );
												}
												Map<String, Object> values = new HashMap<String, Object>();

												values.put( "Author", eg.getName() );
												values.put( "Topic", actualInterest.getTerm() );
												values.put( "TopicId", actualInterest.getId() );
												values.put( "Year", year );
												values.put( "Weight", weight );
												mapList.add( values );
												topicIdMap.put( actualInterest.getTerm(), actualInterest.getId() );
											}
											else
											{
												int ind = conferenceInterests.indexOf( actualInterest );
												conferenceInterestWeights.set( ind, conferenceInterestWeights.get( ind ) + weight );
											}
										}
										else
										{
											List<Interest> newInterestList = new ArrayList<Interest>();
											newInterestList.add( actualInterest );
											if ( !conferenceInterests.contains( actualInterest ) )
											{
												conferenceInterests.add( actualInterest );
												conferenceInterestWeights.add( weight );
											}
											else
											{
												int ind = conferenceInterests.indexOf( actualInterest );
												conferenceInterestWeights.set( ind, conferenceInterestWeights.get( ind ) + weight );
											}
											yearWiseInterests.put( year, newInterestList );

											Map<String, Object> values = new HashMap<String, Object>();

											values.put( "Author", eg.getName() );
											values.put( "Topic", actualInterest.getTerm() );
											values.put( "TopicId", actualInterest.getId() );
											values.put( "Year", year );
											values.put( "Weight", weight );
											mapList.add( values );
											topicIdMap.put( actualInterest.getTerm(), actualInterest.getId() );
										}
									}

								}
							}

						}
					}

				}
			}
			double threshold = 1.0;
			if ( idsList.size() > 1 )
			{
				threshold = 3.0;
			}
			for ( int i = 0; i < conferenceInterests.size(); i++ )
			{
				if ( conferenceInterestWeights.get( i ) < threshold )
				{
					conferenceInterestWeights.remove( i );
					conferenceInterests.remove( i );
					i--;
				}

			}
			for ( int i = 0; i < mapList.size(); i++ )
			{
				Boolean flag = false;
				for ( Interest interest : conferenceInterests )
				{
					if ( interest.getTerm().equals( mapList.get( i ).get( "Topic" ).toString() ) )
						flag = true;
				}
				if ( !flag )
				{
					mapList.remove( i );
					i--;
				}
			}

			visMap.put( "list", mapList );

		}
		return visMap;

	}

	public Map<String, Object> visualizeBubbles( String type, List<String> idsList, List<Author> authorList, Set<Publication> publications, String startYear, String endYear )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		List<String> allTopics = new ArrayList<String>();
		for ( Publication pub : publications )
		{
			Set<PublicationTopic> publicationTopics = pub.getPublicationTopics();
			for ( PublicationTopic pubTopic : publicationTopics )
			{
				List<Double> topicWeights = new ArrayList<Double>( pubTopic.getTermValues().values() );
				List<String> topics = new ArrayList<String>( pubTopic.getTermValues().keySet() );
				for ( int i = 0; i < topics.size(); i++ )
				{
					if ( !allTopics.contains( topics.get( i ) ) && topicWeights.get( i ) > 0.2 )
					{
						allTopics.add( topics.get( i ) );
					}
				}
			}
		}

		if ( type.equals( "researcher" ) )
		{
			// If there are no common publications!
			if ( allTopics.size() == 0 )
			{
				for ( Author a : authorList )
				{
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
								if ( !allTopics.contains( topics.get( i ) ) && topicWeights.get( i ) > 0.2 )
								{
									allTopics.add( topics.get( i ) );
								}
							}
						}
					}
				}
			}
			List<Interest> authorInterests = new ArrayList<Interest>();
			List<Double> authorInterestWeights = new ArrayList<Double>();
			List<Map<Interest, Double>> authorInterestList = new ArrayList<Map<Interest, Double>>();
			for ( Author a : authorList )
			{
				Map<String, List<Interest>> yearWiseInterests = new HashMap<String, List<Interest>>();

				Map<Interest, Double> interestWeightMap = new HashMap<Interest, Double>();
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

							if ( allTopics.contains( interest ) || allTopics.contains( interest + "s" ) )
							{
								Boolean validYear = true;
								Calendar calendar = Calendar.getInstance();
								calendar.setTime( ai.getYear() );
								String year = Integer.toString( calendar.get( Calendar.YEAR ) );
								if ( startYear.equals( "0" ) || startYear.equals( "" ) )
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

									List<String> yWI = new ArrayList<String>( yearWiseInterests.keySet() );
									List<List<Interest>> yWIVal = new ArrayList<List<Interest>>( yearWiseInterests.values() );
									if ( yWI.contains( year ) )
									{
										int index = yWI.indexOf( year );
										if ( !yWIVal.get( index ).contains( actualInterest ) )
										{
											yWIVal.get( index ).add( actualInterest );
											if ( !authorInterests.contains( actualInterest ) )
											{
												authorInterests.add( actualInterest );
												authorInterestWeights.add( weight );
											}
											else
											{
												int ind = authorInterests.indexOf( actualInterest );
												authorInterestWeights.set( ind, authorInterestWeights.get( ind ) + weight );
											}
											if ( interestWeightMap.containsKey( actualInterest ) )
											{
												double w = interestWeightMap.get( actualInterest );
												interestWeightMap.remove( actualInterest );
												interestWeightMap.put( actualInterest, w + 1.0 );
											}
											else
												interestWeightMap.put( actualInterest, 1.0 );
										}
										else
										{
											int ind = authorInterests.indexOf( actualInterest );
											authorInterestWeights.set( ind, authorInterestWeights.get( ind ) + weight );
										}
									}
									else
									{
										List<Interest> newInterestList = new ArrayList<Interest>();
										newInterestList.add( actualInterest );
										if ( !authorInterests.contains( actualInterest ) )
										{
											authorInterests.add( actualInterest );
											authorInterestWeights.add( weight );
										}
										else
										{
											int ind = authorInterests.indexOf( actualInterest );
											authorInterestWeights.set( ind, authorInterestWeights.get( ind ) + weight );
										}
										yearWiseInterests.put( year, newInterestList );
										if ( interestWeightMap.containsKey( actualInterest ) )
										{
											double w = interestWeightMap.get( actualInterest );
											interestWeightMap.remove( actualInterest );
											interestWeightMap.put( actualInterest, w + 1.0 );
										}
										else
											interestWeightMap.put( actualInterest, 1.0 );
									}

								}
							}
						}
					}
				}

				authorInterestList.add( interestWeightMap );
			}

			Map<Interest, Object> finalMap = new HashMap<Interest, Object>();

			double threshold = 0.0;
			// if ( authorList.size() > 1 )
			// {
			// threshold = 0.0;
			// }

			List<Object[]> listObjects = new ArrayList<Object[]>();
			for ( int i = 0; i < authorInterests.size(); i++ )
			{
				if ( authorInterestWeights.get( i ) > threshold )
				{
					List<Double> interestList = new ArrayList<Double>();

					for ( int j = 0; j < authorInterestList.size(); j++ )
					{
						Map<Interest, Double> inWei = authorInterestList.get( j );
						List<Interest> in = new ArrayList<Interest>( inWei.keySet() );
						List<Double> wei = new ArrayList<Double>( inWei.values() );

						if ( in.contains( authorInterests.get( i ) ) )
						{
							int index = in.indexOf( authorInterests.get( i ) );
							interestList.add( wei.get( index ) );
						}
						else
							interestList.add( 0.0 );
					}
					finalMap.put( authorInterests.get( i ), interestList );
					Object[] randArray = new Object[3];
					randArray[0] = authorInterests.get( i ).getTerm();
					randArray[1] = interestList;
					randArray[2] = authorInterests.get( i ).getId();
					System.out.println( authorInterests.get( i ).getTerm() + " : " + authorInterests.get( i ).getId() );
					listObjects.add( randArray );
				}
			}

			visMap.put( "list", listObjects );
		}
		if ( type.equals( "conference" ) )
		{
			// If there are no common publications!
			if ( allTopics.size() == 0 )
			{
				for ( String id : idsList )
				{
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
									if ( !allTopics.contains( topics.get( i ) ) && topicWeights.get( i ) > 0.2 )
									{
										allTopics.add( topics.get( i ) );
									}
								}
							}
						}
					}
				}
			}

			List<Interest> conferenceInterests = new ArrayList<Interest>();
			List<Double> conferenceInterestWeights = new ArrayList<Double>();
			List<Map<Interest, Double>> conferenceInterestList = new ArrayList<Map<Interest, Double>>();
			for ( String id : idsList )
			{
				EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( id );
				Map<String, List<Interest>> yearWiseInterests = new HashMap<String, List<Interest>>();

				Map<Interest, Double> interestWeightMap = new HashMap<Interest, Double>();
				List<Event> events = eg.getEvents();
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

								if ( allTopics.contains( interest ) || allTopics.contains( interest + "s" ) )
								{
									Boolean validYear = true;
									Calendar calendar = Calendar.getInstance();
									calendar.setTime( ei.getYear() );
									String year = Integer.toString( calendar.get( Calendar.YEAR ) );
									if ( startYear.equals( "0" ) || startYear.equals( "" ) )
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

										List<String> yWI = new ArrayList<String>( yearWiseInterests.keySet() );
										List<List<Interest>> yWIVal = new ArrayList<List<Interest>>( yearWiseInterests.values() );
										if ( yWI.contains( year ) )
										{
											int index = yWI.indexOf( year );
											if ( !yWIVal.get( index ).contains( actualInterest ) )
											{
												yWIVal.get( index ).add( actualInterest );
												if ( !conferenceInterests.contains( actualInterest ) )
												{
													conferenceInterests.add( actualInterest );
													conferenceInterestWeights.add( weight );
												}
												else
												{
													int ind = conferenceInterests.indexOf( actualInterest );
													conferenceInterestWeights.set( ind, conferenceInterestWeights.get( ind ) + weight );
												}
												if ( interestWeightMap.containsKey( actualInterest ) )
												{
													double w = interestWeightMap.get( actualInterest );
													interestWeightMap.remove( actualInterest );
													interestWeightMap.put( actualInterest, w + 1.0 );
												}
												else
													interestWeightMap.put( actualInterest, 1.0 );
											}
											else
											{
												int ind = conferenceInterests.indexOf( actualInterest );
												conferenceInterestWeights.set( ind, conferenceInterestWeights.get( ind ) + weight );
											}
										}
										else
										{
											List<Interest> newInterestList = new ArrayList<Interest>();
											newInterestList.add( actualInterest );
											if ( !conferenceInterests.contains( actualInterest ) )
											{
												conferenceInterests.add( actualInterest );
												conferenceInterestWeights.add( weight );
											}
											else
											{
												int ind = conferenceInterests.indexOf( actualInterest );
												conferenceInterestWeights.set( ind, conferenceInterestWeights.get( ind ) + weight );
											}
											yearWiseInterests.put( year, newInterestList );
											if ( interestWeightMap.containsKey( actualInterest ) )
											{
												double w = interestWeightMap.get( actualInterest );
												interestWeightMap.remove( actualInterest );
												interestWeightMap.put( actualInterest, w + 1.0 );
											}
											else
												interestWeightMap.put( actualInterest, 1.0 );
										}
									}
								}
							}
						}
					}
				}
				conferenceInterestList.add( interestWeightMap );
			}

			Map<Interest, Object> finalMap = new HashMap<Interest, Object>();

			double threshold = 0.0;
			// if ( idsList.size() > 1 )
			// {
			// threshold = 3.0;
			// }

			List<Object[]> listObjects = new ArrayList<Object[]>();
			for ( int i = 0; i < conferenceInterests.size(); i++ )
			{
				if ( conferenceInterestWeights.get( i ) > threshold )
				{
					List<Double> interestList = new ArrayList<Double>();

					for ( int j = 0; j < conferenceInterestList.size(); j++ )
					{
						Map<Interest, Double> inWei = conferenceInterestList.get( j );
						List<Interest> in = new ArrayList<Interest>( inWei.keySet() );
						List<Double> wei = new ArrayList<Double>( inWei.values() );

						if ( in.contains( conferenceInterests.get( i ) ) )
						{
							int index = in.indexOf( conferenceInterests.get( i ) );
							interestList.add( wei.get( index ) );
						}
						else
							interestList.add( 0.0 );
					}
					finalMap.put( conferenceInterests.get( i ), interestList );
					Object[] randArray = new Object[3];
					randArray[0] = conferenceInterests.get( i ).getTerm();
					randArray[1] = interestList;
					randArray[2] = conferenceInterests.get( i ).getId();
					listObjects.add( randArray );
				}
			}
			visMap.put( "list", listObjects );
		}

		if ( type.equals( "publication" ) )
		{

			List<String> publicationTopics = new ArrayList<String>();
			List<Double> publicationTopicWeights = new ArrayList<Double>();
			List<Map<String, Double>> publicationTopicList = new ArrayList<Map<String, Double>>();
			for ( String id : idsList )
			{
				Map<String, List<String>> yearWiseInterests = new HashMap<String, List<String>>();
				Publication p = persistenceStrategy.getPublicationDAO().getById( id );
				Map<String, Double> topicWeightMap = new HashMap<String, Double>();

				List<PublicationTopic> pubTopics = new ArrayList<PublicationTopic>( p.getPublicationTopics() );
				for ( PublicationTopic pt : pubTopics )
				{
					Map<String, Double> termValues = pt.getTermValues();
					List<String> terms = new ArrayList<String>( termValues.keySet() );
					List<Double> weights = new ArrayList<Double>( termValues.values() );
					for ( int i = 0; i < terms.size(); i++ )
					{
						if ( allTopics.contains( terms.get( i ) ) || allTopics.contains( terms.get( i ) + "s" ) )
						{
							Boolean validYear = true;
							String year = p.getYear();
							if ( startYear.equals( "0" ) || startYear.equals( "" ) )
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

								List<String> yWI = new ArrayList<String>( yearWiseInterests.keySet() );
								List<List<String>> yWIVal = new ArrayList<List<String>>( yearWiseInterests.values() );
								if ( yWI.contains( year ) )
								{
									int index = yWI.indexOf( year );
									if ( !yWIVal.get( index ).contains( terms.get( i ) ) )
									{
										yWIVal.get( index ).add( terms.get( i ) );
										if ( !publicationTopics.contains( terms.get( i ) ) )
										{
											publicationTopics.add( terms.get( i ) );
											publicationTopicWeights.add( weights.get( i ) );
										}
										else
										{
											int ind = publicationTopics.indexOf( terms.get( i ) );
											publicationTopicWeights.set( ind, publicationTopicWeights.get( ind ) + weights.get( i ) );
										}
										if ( topicWeightMap.containsKey( terms.get( i ) ) )
										{
											double w = topicWeightMap.get( terms.get( i ) );
											topicWeightMap.remove( terms.get( i ) );
											topicWeightMap.put( terms.get( i ), w + 1.0 );
										}
										else
											topicWeightMap.put( terms.get( i ), 1.0 );
									}
									else
									{
										int ind = publicationTopics.indexOf( terms.get( i ) );
										publicationTopicWeights.set( ind, publicationTopicWeights.get( ind ) + weights.get( i ) );
									}
								}
								else
								{
									List<String> newInterestList = new ArrayList<String>();
									newInterestList.add( terms.get( i ) );
									if ( !publicationTopics.contains( terms.get( i ) ) )
									{
										publicationTopics.add( terms.get( i ) );
										publicationTopicWeights.add( weights.get( i ) );
									}
									else
									{
										int ind = publicationTopics.indexOf( terms.get( i ) );
										publicationTopicWeights.set( ind, publicationTopicWeights.get( ind ) + weights.get( i ) );
									}
									yearWiseInterests.put( year, newInterestList );
									if ( topicWeightMap.containsKey( terms.get( i ) ) )
									{
										double w = topicWeightMap.get( terms.get( i ) );
										topicWeightMap.remove( terms.get( i ) );
										topicWeightMap.put( terms.get( i ), w + 1.0 );
									}
									else
										topicWeightMap.put( terms.get( i ), 1.0 );
								}
							}
						}
					}
				}
				publicationTopicList.add( topicWeightMap );
			}

			Map<String, Object> finalMap = new HashMap<String, Object>();

			double threshold = 0.0;
			if ( authorList.size() > 1 )
			{
				threshold = 0.0;
			}

			List<Object[]> listObjects = new ArrayList<Object[]>();
			for ( int i = 0; i < publicationTopics.size(); i++ )
			{
				if ( publicationTopicWeights.get( i ) > threshold )
				{
					List<Double> interestList = new ArrayList<Double>();

					for ( int j = 0; j < publicationTopicList.size(); j++ )
					{
						Map<String, Double> inWei = publicationTopicList.get( j );
						List<String> in = new ArrayList<String>( inWei.keySet() );
						List<Double> wei = new ArrayList<Double>( inWei.values() );

						if ( in.contains( publicationTopics.get( i ) ) )
						{
							int index = in.indexOf( publicationTopics.get( i ) );
							interestList.add( wei.get( index ) );
						}
						else
							interestList.add( 0.0 );
					}
					finalMap.put( publicationTopics.get( i ), interestList );

					System.out.println( publicationTopics.get( i ) );

					Interest interest = persistenceStrategy.getInterestDAO().getInterestByTerm( publicationTopics.get( i ) );
					if ( interest != null )
					{
						System.out.println( "1" );
						Object[] randArray = new Object[3];
						randArray[0] = interest.getTerm();
						randArray[1] = interestList;
						randArray[2] = interest.getId();
						listObjects.add( randArray );
					}
					else
					{
						interest = persistenceStrategy.getInterestDAO().getInterestByTerm( publicationTopics.get( i ).substring( 0, publicationTopics.get( i ).length() - 1 ) );
						if ( interest != null )
						{
							System.out.println( "2" );
							Object[] randArray = new Object[3];
							randArray[0] = interest.getTerm();
							randArray[1] = interestList;
							randArray[2] = interest.getId();
							listObjects.add( randArray );
						}
					}
				}

			}

			visMap.put( "list", listObjects );

		}
		return visMap;
	}

	public Map<String, Object> visualizeList( String type, String visType, List<Author> authorList, Set<Publication> publications, String startYear, String endYear, List<String> idsList )
	{
		// System.out.println( "visType: " + visType );
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();
		if ( visType.equals( "researchers" ) )
			visMap.putAll( researcherFeature.getResearcherCoauthor().getResearcherCoAuthorMapByPublication( authorList, publications, type ) );

		if ( visType.equals( "conferences" ) )
		{
			if ( type.equals( "researcher" ) || type.equals( "publication" ) )
				visMap.putAll( researcherFeature.getResearcherAcademicEventTree().getResearcherAllAcademicEvents( publications, false ) );

			if ( type.equals( "conference" ) )
				visMap.putAll( researcherFeature.getResearcherAcademicEventTree().getResearcherAllAcademicEvents( publications, false ) );
		}
		if ( visType.equals( "publications" ) )
		{
			List<Publication> publicationsList = new ArrayList<Publication>( publications );
			// sort by date
			Collections.sort( publicationsList, new PublicationByDateComparator() );

			List<Map<String, Object>> pubDetailsList = new ArrayList<Map<String, Object>>();

			for ( Publication pub : publicationsList )
			{
				Map<String, Object> pubDetails = new LinkedHashMap<String, Object>();
				// System.out.println( pub.getPublicationDate() + " " +
				// pub.getTitle() );
				pubDetails.put( "id", pub.getId() );
				pubDetails.put( "title", pub.getTitle() );
				pubDetails.put( "year", pub.getYear() );
				pubDetails.put( "type", pub.getPublicationType() );
				pubDetails.put( "date", pub.getPublicationDate() );
				pubDetailsList.add( pubDetails );
			}

			visMap.put( "pubDetailsList", pubDetailsList );

		}
		if ( visType.equals( "topics" ) )
		{
			visMap = visualizeBubbles( type, idsList, authorList, publications, startYear, endYear );
		}
		if ( visType.equals( "circles" ) )
		{
		}

		return visMap;
	}

	@SuppressWarnings( "unchecked" )
	public Map<String, Object> visualizeGroup( String type, String visType, List<Author> authorList, Set<Publication> publications )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();
		if ( visType.equals( "researchers" ) )
		{
			Map<String, Object> resultMap = clusteringService.clusterAuthors( "xmeans", authorList, publications, type );
			Map<String, List<String>> clusterTerms = (Map<String, List<String>>) resultMap.get( "clusterTerms" );
			Map<String, List<String>> nodeTerms = (Map<String, List<String>>) resultMap.get( "nodeTerms" );
			Map<String, Integer> mapClusterAuthor = (Map<String, Integer>) resultMap.get( "clusterMap" );
			if ( mapClusterAuthor != null )
			{
				Iterator<Integer> clusterIterator = mapClusterAuthor.values().iterator();
				List<Integer> clusters = new ArrayList<Integer>();
				while ( clusterIterator.hasNext() )
				{
					clusters.add( clusterIterator.next() );
				}

				// 1st Level Keys i.e information about author
				Iterator<String> objectsIterator = mapClusterAuthor.keySet().iterator();
				List<String> names = new ArrayList<String>();
				List<String> ids = new ArrayList<String>();
				Object jsonObject;
				String jsonString;
				Map<String, String> mapValues = new LinkedHashMap<String, String>();
				while ( objectsIterator.hasNext() )
				{
					String objectString = objectsIterator.next();
					try
					{
						jsonObject = mapper.readValue( objectString, Object.class );
						jsonString = mapper.writeValueAsString( jsonObject );
						mapValues = (Map<String, String>) mapper.readValue( jsonString, Object.class );
					}
					catch ( Exception e )
					{
						e.printStackTrace();
					}
					Iterator<String> iterator = mapValues.values().iterator();
					while ( iterator.hasNext() )
					{
						ids.add( iterator.next() );
						names.add( iterator.next() );
					}
				}
				Map<String, Object> responseMapTest = new LinkedHashMap<String, Object>();
				List<Map<String, Object>> authors = new ArrayList<Map<String, Object>>();
				for ( int i = 0; i < clusters.size(); i++ )
				{
					Map<String, Object> responseMapTemp = new LinkedHashMap<String, Object>();
					responseMapTemp.put( "id", ids.get( i ) );
					responseMapTemp.put( "name", names.get( i ) );
					responseMapTemp.put( "cluster", clusters.get( i ) );
					responseMapTemp.put( "nodeTerms", nodeTerms.get( ids.get( i ) ) );
					// System.out.println( i + " " + clusters.get( i ) + " " +
					// clusterTerms.get( clusters.get( i ) ) );
					responseMapTemp.put( "clusterTerms", clusterTerms.get( clusters.get( i ) ) );
					authors.add( responseMapTemp );
				}
				responseMapTest.put( "coauthors", authors );
				return responseMapTest;
			}
			else
				return (Map<String, Object>) visMap.put( "coauthors", "none" );
		}
		if ( visType.equals( "conferences" ) )
		{
			Map<String, Object> resultMap = clusteringService.clusterConferences( "xmeans", authorList, publications );
			Map<String, List<String>> clusterTerms = (Map<String, List<String>>) resultMap.get( "clusterTerms" );
			Map<String, List<String>> nodeTerms = (Map<String, List<String>>) resultMap.get( "nodeTerms" );
			Map<String, Integer> mapClusterConference = (Map<String, Integer>) resultMap.get( "clusterMap" );
			if ( mapClusterConference != null )
			{
				Iterator<Integer> clusterIterator = mapClusterConference.values().iterator();
				List<Integer> clusters = new ArrayList<Integer>();
				while ( clusterIterator.hasNext() )
				{
					clusters.add( clusterIterator.next() );
				}

				// 1st Level Keys i.e information about author
				Iterator<String> objectsIterator = mapClusterConference.keySet().iterator();
				List<String> names = new ArrayList<String>();
				List<String> ids = new ArrayList<String>();
				List<String> abrs = new ArrayList<String>();
				Object jsonObject;
				String jsonString;
				Map<String, String> mapValues = new LinkedHashMap<String, String>();
				while ( objectsIterator.hasNext() )
				{
					String objectString = objectsIterator.next();
					try
					{
						jsonObject = mapper.readValue( objectString, Object.class );
						jsonString = mapper.writeValueAsString( jsonObject );
						mapValues = (Map<String, String>) mapper.readValue( jsonString, Object.class );
					}
					catch ( Exception e )
					{
						e.printStackTrace();
					}
					Iterator<String> iterator = mapValues.values().iterator();
					while ( iterator.hasNext() )
					{
						names.add( iterator.next() );
						ids.add( iterator.next() );
						abrs.add( iterator.next() );
					}
				}
				Map<String, Object> responseMapTest = new LinkedHashMap<String, Object>();
				List<Map<String, Object>> conferences = new ArrayList<Map<String, Object>>();
				for ( int i = 0; i < clusters.size(); i++ )
				{
					Map<String, Object> responseMapTemp = new LinkedHashMap<String, Object>();
					responseMapTemp.put( "id", ids.get( i ) );
					responseMapTemp.put( "name", names.get( i ) );
					responseMapTemp.put( "abr", abrs.get( i ) );
					responseMapTemp.put( "cluster", clusters.get( i ) );
					responseMapTemp.put( "nodeTerms", nodeTerms.get( ids.get( i ) ) );
					responseMapTemp.put( "clusterTerms", clusterTerms.get( clusters.get( i ) ) );
					conferences.add( responseMapTemp );
				}
				responseMapTest.put( "conferences", conferences );
				return responseMapTest;
			}
			else
				return (Map<String, Object>) visMap.put( "conferences", "none" );
		}
		if ( visType.equals( "publications" ) )
		{
			Map<String, Object> resultMap = clusteringService.clusterPublications( "xmeans", publications );
			Map<String, List<String>> clusterTerms = (Map<String, List<String>>) resultMap.get( "clusterTerms" );

			Map<String, Integer> mapClusterPublication = (Map<String, Integer>) resultMap.get( "clusterMap" );
			if ( mapClusterPublication != null )
			{
				Iterator<Integer> clusterIterator = mapClusterPublication.values().iterator();
				List<Integer> clusters = new ArrayList<Integer>();
				while ( clusterIterator.hasNext() )
				{
					clusters.add( clusterIterator.next() );
				}

				// 1st Level Keys i.e information about author
				Iterator<String> objectsIterator = mapClusterPublication.keySet().iterator();
				List<String> names = new ArrayList<String>();
				List<String> ids = new ArrayList<String>();
				Object jsonObject;
				String jsonString;
				Map<String, String> mapValues = new LinkedHashMap<String, String>();
				while ( objectsIterator.hasNext() )
				{
					String objectString = objectsIterator.next();
					try
					{
						jsonObject = mapper.readValue( objectString, Object.class );
						jsonString = mapper.writeValueAsString( jsonObject );
						mapValues = (Map<String, String>) mapper.readValue( jsonString, Object.class );
					}
					catch ( Exception e )
					{
						e.printStackTrace();
					}
					Iterator<String> iterator = mapValues.values().iterator();
					while ( iterator.hasNext() )
					{
						names.add( iterator.next() );
						ids.add( iterator.next() );
					}
				}
				Map<String, Object> responseMapTest = new LinkedHashMap<String, Object>();
				List<Map<String, Object>> publicationsList = new ArrayList<Map<String, Object>>();
				for ( int i = 0; i < clusters.size(); i++ )
				{
					Map<String, Object> responseMapTemp = new LinkedHashMap<String, Object>();
					responseMapTemp.put( "id", ids.get( i ) );
					responseMapTemp.put( "name", names.get( i ) );
					responseMapTemp.put( "cluster", clusters.get( i ) );
					responseMapTemp.put( "clusterTerms", clusterTerms.get( clusters.get( i ) ) );
					publicationsList.add( responseMapTemp );
				}
				System.out.println( publicationsList.toString() );
				responseMapTest.put( "publications", publicationsList );
				return responseMapTest;
			}
			else
				return (Map<String, Object>) visMap.put( "publications", "none" );
		}
		if ( visType.equals( "topics" ) )
		{
			return null;
		}
		if ( visType.equals( "circles" ) )
		{
			return null;
		}

		// else
		return null;
	}

	public Map<String, Object> visualizeComparison( String type, List<String> idsList, String visType, List<Author> authorList, Set<Publication> publications, String startYear, String endYear, String yearFilterPresent )
	{

		// altLabel is alternative label for discrimination
		List<Map<String, Object>> listOfMaps = new ArrayList<Map<String, Object>>();

		System.out.println( "visType: " + visType );
		System.out.println( "object type: " + type );

		if ( visType.equals( "researchers" ) )
		{
			// object type
			if ( type.equals( "researcher" ) )
			{
				Map<Author, List<Author>> mapAuthors = new HashMap<Author, List<Author>>();

				for ( int i = 0; i < authorList.size(); i++ )
				{
					Map<String, Object> mapValues = new HashMap<String, Object>();
					List<Integer> index = new ArrayList<Integer>();
					index.add( i );

					Set<Publication> authorPublications = authorList.get( i ).getPublications();
					List<Author> publicationAuthors = new ArrayList<Author>();
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
								if ( !publicationAuthors.contains( a ) && !a.equals( authorList.get( i ) ) )
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
					mapAuthors.put( authorList.get( i ), publicationAuthors );

					// single values to venn diagram
					mapValues.put( "sets", index );
					mapValues.put( "label", authorList.get( i ).getName() );
					mapValues.put( "size", publicationAuthors.size() );
					mapValues.put( "altLabel", authorList.get( i ).getName() );
					mapValues.put( "list", listItems );
					listOfMaps.add( mapValues );

					if ( mapAuthors.size() > 1 )
					{
						// publications of authors added before current author
						for ( int k = 0; k < mapAuthors.size(); k++ )
						{
							List<Author> previousAuthors = new ArrayList<Author>( mapAuthors.keySet() );
							List<List<Author>> previousAuthorLists = new ArrayList<List<Author>>( mapAuthors.values() );
							List<Author> previousAuthorCoAuthors = previousAuthorLists.get( k );
							List<Map<String, Object>> tempListItems = new ArrayList<Map<String, Object>>();
							String label = "";

							Author previousAuthor = previousAuthors.get( k );

							if ( !previousAuthor.equals( authorList.get( i ) ) )
							{
								List<Author> temp = new ArrayList<Author>();

								// find common authors
								for ( Author a : previousAuthorCoAuthors )
								{
									if ( publicationAuthors.contains( a ) )
									{
										temp.add( a );
										Map<String, Object> items = new HashMap<String, Object>();
										items.put( "name", a.getName() );
										items.put( "id", a.getId() );
										items.put( "isAdded", a.isAdded() );
										tempListItems.add( items );
									}
								}

								Map<String, Object> mapValuesForPairs = new LinkedHashMap<String, Object>();
								List<Integer> sets = new ArrayList<Integer>();
								sets.add( i );
								sets.add( authorList.indexOf( previousAuthor ) );
								label = label + authorList.get( i ).getFirstName() + "-" + previousAuthor.getFirstName();
								mapValuesForPairs.put( "sets", sets );
								mapValuesForPairs.put( "size", temp.size() );
								mapValuesForPairs.put( "list", tempListItems );
								mapValuesForPairs.put( "altLabel", label );
								listOfMaps.add( mapValuesForPairs );
							}
						}
					}
				}
				// common to all
				if ( authorList.size() > 2 )
				{
					List<Author> allAuthors = new ArrayList<Author>();
					List<Integer> count = new ArrayList<Integer>();
					List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();

					for ( int i = 0; i < authorList.size(); i++ )
					{
						List<Author> allCoAuthors = new ArrayList<Author>();
						Set<Publication> pubs = authorList.get( i ).getPublications();
						for ( Publication p : pubs )
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
								List<Author> coAuthors = p.getAuthors();
								for ( int j = 0; j < coAuthors.size(); j++ )
								{
									if ( !authorList.contains( coAuthors.get( j ) ) )
									{
										if ( !allCoAuthors.contains( coAuthors.get( j ) ) )
										{
											allCoAuthors.add( coAuthors.get( j ) );
										}
									}
								}
							}
						}

						for ( int k = 0; k < allCoAuthors.size(); k++ )
						{
							if ( !allAuthors.contains( allCoAuthors.get( k ) ) )
							{
								allAuthors.add( allCoAuthors.get( k ) );
								Map<String, Object> items = new HashMap<String, Object>();
								items.put( "name", allCoAuthors.get( k ).getName() );
								items.put( "id", allCoAuthors.get( k ).getId() );
								items.put( "isAdded", allCoAuthors.get( k ).isAdded() );
								combinedListItems.add( items );
								count.add( 0 );
							}
							else
								count.set( allAuthors.indexOf( allCoAuthors.get( k ) ), count.get( allAuthors.indexOf( allCoAuthors.get( k ) ) ) + 1 );
						}
					}

					for ( int i = 0; i < allAuthors.size(); i++ )
					{
						if ( count.get( i ) < authorList.size() - 1 )
						{
							count.remove( i );
							allAuthors.remove( i );
							combinedListItems.remove( i );
							i--;
						}
					}

					Map<String, Object> mapValuesForAll = new LinkedHashMap<String, Object>();
					List<Integer> sets = new ArrayList<Integer>();
					for ( int i = 0; i < authorList.size(); i++ )
					{
						sets.add( i );
					}

					mapValuesForAll.put( "sets", sets );
					mapValuesForAll.put( "size", count.size() );
					mapValuesForAll.put( "list", combinedListItems );

					String label = "";
					for ( int i = 0; i < authorList.size(); i++ )
					{
						label = label + authorList.get( i ).getFirstName();
					}

					mapValuesForAll.put( "altLabel", label );
					mapValuesForAll.put( "weight", 1000 );
					listOfMaps.add( mapValuesForAll );
				}
			}
			if ( type.equals( "conference" ) )
			{
				Map<EventGroup, List<Author>> mapAuthors = new HashMap<EventGroup, List<Author>>();
				List<EventGroup> eventGroupTempList = new ArrayList<EventGroup>();

				for ( int i = 0; i < idsList.size(); i++ )
				{
					Map<String, Object> mapValues = new HashMap<String, Object>();
					List<Integer> index = new ArrayList<Integer>();
					index.add( i );

					List<Publication> eventGroupPubs = new ArrayList<Publication>();
					EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( idsList.get( i ) );
					eventGroupTempList.add( eg );
					List<Event> events = eg.getEvents();
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
					mapAuthors.put( eg, publicationAuthors );

					// single values to venn diagram
					mapValues.put( "sets", index );
					mapValues.put( "label", eg.getName() );
					mapValues.put( "size", publicationAuthors.size() );
					mapValues.put( "altLabel", idsList.get( i ) );
					mapValues.put( "list", listItems );

					listOfMaps.add( mapValues );

					if ( mapAuthors.size() > 1 )
					{
						// publications of authors added before current author
						for ( int k = 0; k < mapAuthors.size(); k++ )
						{
							List<EventGroup> previousEventGroups = new ArrayList<EventGroup>( mapAuthors.keySet() );
							List<List<Author>> previousAuthorLists = new ArrayList<List<Author>>( mapAuthors.values() );
							List<Author> previousAuthorCoAuthors = previousAuthorLists.get( k );
							EventGroup previousEventGroup = previousEventGroups.get( k );
							List<Map<String, Object>> tempListItems = new ArrayList<Map<String, Object>>();

							String label = "";
							// System.out.println( previousEventGroup.getName()
							// );

							if ( !previousEventGroup.equals( eg ) )
							{
								List<Author> temp = new ArrayList<Author>();

								// find common authors
								for ( Author a : previousAuthorCoAuthors )
								{
									if ( publicationAuthors.contains( a ) )
									{
										temp.add( a );
										Map<String, Object> items = new HashMap<String, Object>();
										items.put( "name", a.getName() );
										items.put( "id", a.getId() );
										items.put( "isAdded", a.isAdded() );
										tempListItems.add( items );
									}
								}
								Map<String, Object> mapValuesForPairs = new LinkedHashMap<String, Object>();
								List<Integer> sets = new ArrayList<Integer>();
								sets.add( i );

								label = label + idsList.get( i ) + "-" + previousEventGroup.getNotation();
								sets.add( eventGroupTempList.indexOf( previousEventGroup ) ); // to-do
								mapValuesForPairs.put( "sets", sets );
								mapValuesForPairs.put( "size", tempListItems.size() );
								mapValuesForPairs.put( "list", tempListItems );
								mapValuesForPairs.put( "altLabel", label );

								listOfMaps.add( mapValuesForPairs );
							}
						}

					}

				}

				// common to all
				if ( idsList.size() > 2 )
				{
					// System.out.println( "coming here" );
					List<Author> allAuthors = new ArrayList<Author>();
					List<Integer> count = new ArrayList<Integer>();
					List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();
					for ( int i = 0; i < idsList.size(); i++ )
					{
						List<Author> allCoAuthors = new ArrayList<Author>();

						List<Publication> eventGroupPubs = new ArrayList<Publication>();
						EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( idsList.get( i ) );
						eventGroupTempList.add( eg );
						List<Event> events = eg.getEvents();
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
								List<Author> coAuthors = p.getAuthors();
								for ( int j = 0; j < coAuthors.size(); j++ )
								{
									if ( !authorList.contains( coAuthors.get( j ) ) )
									{
										if ( !allCoAuthors.contains( coAuthors.get( j ) ) )
										{
											allCoAuthors.add( coAuthors.get( j ) );
										}
									}
								}
							}
						}

						for ( int k = 0; k < allCoAuthors.size(); k++ )
						{
							if ( !allAuthors.contains( allCoAuthors.get( k ) ) )
							{
								allAuthors.add( allCoAuthors.get( k ) );
								Map<String, Object> items = new HashMap<String, Object>();
								items.put( "name", allCoAuthors.get( k ).getName() );
								items.put( "id", allCoAuthors.get( k ).getId() );
								items.put( "isAdded", allCoAuthors.get( k ).isAdded() );
								combinedListItems.add( items );
								count.add( 0 );
							}
							else
								count.set( allAuthors.indexOf( allCoAuthors.get( k ) ), count.get( allAuthors.indexOf( allCoAuthors.get( k ) ) ) + 1 );
						}

					}

					for ( int i = 0; i < allAuthors.size(); i++ )
					{
						if ( count.get( i ) < idsList.size() - 1 )
						{
							count.remove( i );
							allAuthors.remove( i );
							combinedListItems.remove( i );
							i--;
						}
					}

					Map<String, Object> mapValuesForAll = new LinkedHashMap<String, Object>();
					List<Integer> sets = new ArrayList<Integer>();
					for ( int i = 0; i < idsList.size(); i++ )
					{
						sets.add( i );
					}

					mapValuesForAll.put( "sets", sets );
					mapValuesForAll.put( "size", count.size() );
					mapValuesForAll.put( "list", combinedListItems );

					String label = "";
					for ( int i = 0; i < idsList.size(); i++ )
					{
						label = label + idsList.get( i );
					}

					mapValuesForAll.put( "altLabel", label );
					mapValuesForAll.put( "weight", 1000 );
					listOfMaps.add( mapValuesForAll );
				}
			}
			if ( type.equals( "publication" ) )
			{
				Map<Publication, List<Author>> mapAuthors = new HashMap<Publication, List<Author>>();
				List<Publication> publicationTempList = new ArrayList<Publication>();

				for ( int i = 0; i < idsList.size(); i++ )
				{
					Map<String, Object> mapValues = new HashMap<String, Object>();
					List<Integer> index = new ArrayList<Integer>();
					index.add( i );

					Publication p = persistenceStrategy.getPublicationDAO().getById( idsList.get( i ) );
					publicationTempList.add( p );
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
					mapAuthors.put( p, publicationAuthors );

					// single values to venn diagram
					mapValues.put( "sets", index );
					mapValues.put( "label", p.getTitle() );
					mapValues.put( "size", publicationAuthors.size() );
					mapValues.put( "altLabel", idsList.get( i ) );
					mapValues.put( "list", listItems );

					listOfMaps.add( mapValues );

					if ( mapAuthors.size() > 1 )
					{
						// publications of authors added before current author
						for ( int k = 0; k < mapAuthors.size(); k++ )
						{

							List<Publication> previousPublications = new ArrayList<Publication>( mapAuthors.keySet() );
							List<List<Author>> previousAuthorLists = new ArrayList<List<Author>>( mapAuthors.values() );
							List<Author> previousAuthorCoAuthors = previousAuthorLists.get( k );
							Publication previousPublication = previousPublications.get( k );
							List<Map<String, Object>> tempListItems = new ArrayList<Map<String, Object>>();

							String label = "";

							for ( Author co : previousAuthorCoAuthors )
								System.out.println( co.getName() );
							if ( !previousPublication.equals( p ) )
							{
								List<Author> temp = new ArrayList<Author>();

								// find common authors
								for ( Author a : previousAuthorCoAuthors )
								{
									if ( publicationAuthors.contains( a ) )
									{
										temp.add( a );
										Map<String, Object> items = new HashMap<String, Object>();
										items.put( "name", a.getName() );
										items.put( "id", a.getId() );
										items.put( "isAdded", a.isAdded() );
										tempListItems.add( items );
									}
								}
								Map<String, Object> mapValuesForPairs = new LinkedHashMap<String, Object>();
								List<Integer> sets = new ArrayList<Integer>();
								sets.add( i );

								label = label + idsList.get( i ) + "-" + previousPublication.getTitle();
								sets.add( publicationTempList.indexOf( previousPublication ) ); // to-do
								mapValuesForPairs.put( "sets", sets );
								mapValuesForPairs.put( "size", tempListItems.size() );
								mapValuesForPairs.put( "list", tempListItems );
								mapValuesForPairs.put( "altLabel", label );

								listOfMaps.add( mapValuesForPairs );
							}
						}
					}
				}

				// common to all
				if ( idsList.size() > 2 )
				{
					// System.out.println( "coming here" );
					List<Author> allAuthors = new ArrayList<Author>();
					List<Integer> count = new ArrayList<Integer>();
					List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();
					for ( int i = 0; i < idsList.size(); i++ )
					{
						List<Author> allCoAuthors = new ArrayList<Author>();

						Publication p = persistenceStrategy.getPublicationDAO().getById( idsList.get( i ) );
						publicationTempList.add( p );
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
							List<Author> coAuthors = p.getAuthors();
							for ( int j = 0; j < coAuthors.size(); j++ )
							{
								if ( !authorList.contains( coAuthors.get( j ) ) )
								{
									if ( !allCoAuthors.contains( coAuthors.get( j ) ) )
									{
										allCoAuthors.add( coAuthors.get( j ) );
									}
								}
							}
						}

						for ( int k = 0; k < allCoAuthors.size(); k++ )
						{
							if ( !allAuthors.contains( allCoAuthors.get( k ) ) )
							{
								allAuthors.add( allCoAuthors.get( k ) );
								Map<String, Object> items = new HashMap<String, Object>();
								items.put( "name", allCoAuthors.get( k ).getName() );
								items.put( "id", allCoAuthors.get( k ).getId() );
								items.put( "isAdded", allCoAuthors.get( k ).isAdded() );
								combinedListItems.add( items );
								count.add( 0 );
							}
							else
								count.set( allAuthors.indexOf( allCoAuthors.get( k ) ), count.get( allAuthors.indexOf( allCoAuthors.get( k ) ) ) + 1 );
						}

					}

					for ( int i = 0; i < allAuthors.size(); i++ )
					{
						if ( count.get( i ) < idsList.size() - 1 )
						{
							count.remove( i );
							allAuthors.remove( i );
							combinedListItems.remove( i );
							i--;
						}
					}

					Map<String, Object> mapValuesForAll = new LinkedHashMap<String, Object>();
					List<Integer> sets = new ArrayList<Integer>();
					for ( int i = 0; i < idsList.size(); i++ )
					{
						sets.add( i );
					}

					mapValuesForAll.put( "sets", sets );
					mapValuesForAll.put( "size", count.size() );
					mapValuesForAll.put( "list", combinedListItems );

					String label = "";
					for ( int i = 0; i < idsList.size(); i++ )
					{
						label = label + idsList.get( i );
					}

					mapValuesForAll.put( "altLabel", label );
					mapValuesForAll.put( "weight", 1000 );
					listOfMaps.add( mapValuesForAll );
				}

			}
		}
		if ( visType.equals( "conferences" ) )
		{
			Map<Author, List<EventGroup>> mapConferences = new HashMap<Author, List<EventGroup>>();

			for ( int i = 0; i < authorList.size(); i++ )
			{
				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );

				Set<Publication> authorPublications = authorList.get( i ).getPublications();
				List<EventGroup> authorEvents = new ArrayList<EventGroup>();
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
								if ( !authorEvents.contains( eventGroup ) )
								{
									authorEvents.add( eventGroup );
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
				mapConferences.put( authorList.get( i ), authorEvents );

				// single values to venn diagram
				mapValues.put( "sets", index );
				mapValues.put( "label", authorList.get( i ).getName() );
				mapValues.put( "size", authorEvents.size() );
				mapValues.put( "altLabel", authorList.get( i ).getName() );
				mapValues.put( "list", listItems );
				listOfMaps.add( mapValues );

				if ( mapConferences.size() > 1 )
				{
					// publications of authors added before current author
					for ( int k = 0; k < mapConferences.size(); k++ )
					{
						List<Author> previousAuthors = new ArrayList<Author>( mapConferences.keySet() );
						List<List<EventGroup>> previousAuthorLists = new ArrayList<List<EventGroup>>( mapConferences.values() );
						List<EventGroup> previousAuthorEvents = previousAuthorLists.get( k );
						Author previousAuthor = previousAuthors.get( k );
						List<Map<String, Object>> tempListItems = new ArrayList<Map<String, Object>>();
						String label = "";
						if ( !previousAuthor.equals( authorList.get( i ) ) )
						{
							List<EventGroup> temp = new ArrayList<EventGroup>();
							List<String> tempNames = new ArrayList<String>();

							// find common Events
							for ( EventGroup eg : previousAuthorEvents )
							{
								if ( authorEvents.contains( eg ) )
								{
									temp.add( eg );
									Map<String, Object> items = new HashMap<String, Object>();
									items.put( "name", eg.getName() );
									items.put( "id", eg.getId() );
									items.put( "isAdded", eg.isAdded() );
									tempListItems.add( items );
								}
							}

							Map<String, Object> mapValuesForPairs = new LinkedHashMap<String, Object>();
							List<Integer> sets = new ArrayList<Integer>();
							sets.add( i );
							sets.add( authorList.indexOf( previousAuthor ) );
							label = label + authorList.get( i ).getFirstName() + "-" + previousAuthor.getFirstName();
							mapValuesForPairs.put( "sets", sets );
							mapValuesForPairs.put( "size", tempListItems.size() );
							mapValuesForPairs.put( "list", tempListItems );
							mapValuesForPairs.put( "altLabel", label );
							listOfMaps.add( mapValuesForPairs );
						}
					}
				}
			}
			// common to all
			if ( authorList.size() > 2 )
			{
				List<EventGroup> allEventGroups = new ArrayList<EventGroup>();
				List<Integer> count = new ArrayList<Integer>();
				List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();

				for ( int i = 0; i < authorList.size(); i++ )
				{
					List<EventGroup> authorEventGroups = new ArrayList<EventGroup>();
					Set<Publication> pubs = authorList.get( i ).getPublications();
					for ( Publication p : pubs )
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
									if ( !authorEventGroups.contains( eg ) )
									{
										authorEventGroups.add( eg );
									}
								}
							}
						}
					}

					for ( int k = 0; k < authorEventGroups.size(); k++ )
					{
						if ( !allEventGroups.contains( authorEventGroups.get( k ) ) )
						{
							allEventGroups.add( authorEventGroups.get( k ) );
							Map<String, Object> items = new HashMap<String, Object>();
							items.put( "name", authorEventGroups.get( k ).getName() );
							items.put( "id", authorEventGroups.get( k ).getId() );
							items.put( "isAdded", authorEventGroups.get( k ).isAdded() );
							combinedListItems.add( items );
							count.add( 0 );

						}
						else
							count.set( allEventGroups.indexOf( authorEventGroups.get( k ) ), count.get( allEventGroups.indexOf( authorEventGroups.get( k ) ) ) + 1 );
					}

				}

				for ( int i = 0; i < allEventGroups.size(); i++ )
				{
					if ( count.get( i ) < authorList.size() - 1 )
					{
						count.remove( i );
						allEventGroups.remove( i );
						combinedListItems.remove( i );
						i--;
					}
				}

				Map<String, Object> mapValuesForAll = new LinkedHashMap<String, Object>();
				List<Integer> sets = new ArrayList<Integer>();
				for ( int i = 0; i < authorList.size(); i++ )
				{
					sets.add( i );
				}

				mapValuesForAll.put( "sets", sets );
				mapValuesForAll.put( "size", count.size() );
				mapValuesForAll.put( "list", combinedListItems );

				String label = "";
				for ( int i = 0; i < authorList.size(); i++ )
				{
					label = label + authorList.get( i ).getFirstName();
				}

				mapValuesForAll.put( "altLabel", label );
				mapValuesForAll.put( "weight", 1000 );
				listOfMaps.add( mapValuesForAll );
			}

		}
		if ( visType.equals( "publications" ) )
		{
			Map<Author, List<Publication>> mapPublications = new HashMap<Author, List<Publication>>();

			for ( int i = 0; i < authorList.size(); i++ )
			{
				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );

				Set<Publication> authorPublications = authorList.get( i ).getPublications();
				List<Publication> allPublications = new ArrayList<Publication>();
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
				mapPublications.put( authorList.get( i ), allPublications );

				// single values to venn diagram
				mapValues.put( "sets", index );
				mapValues.put( "label", authorList.get( i ).getName() );
				mapValues.put( "size", allPublications.size() );
				mapValues.put( "altLabel", authorList.get( i ).getName() );
				mapValues.put( "list", listItems );
				listOfMaps.add( mapValues );

				if ( mapPublications.size() > 1 )
				{
					// publications of authors added before current author
					for ( int k = 0; k < mapPublications.size(); k++ )
					{
						List<Author> previousAuthors = new ArrayList<Author>( mapPublications.keySet() );
						List<List<Publication>> previousAuthorLists = new ArrayList<List<Publication>>( mapPublications.values() );
						List<Publication> previousAuthorPublications = previousAuthorLists.get( k );
						Author previousAuthor = previousAuthors.get( k );
						List<Map<String, Object>> tempListItems = new ArrayList<Map<String, Object>>();
						String label = "";

						if ( !previousAuthor.equals( authorList.get( i ) ) )
						{
							List<Publication> temp = new ArrayList<Publication>();

							// find common publications
							for ( Publication p : previousAuthorPublications )
							{
								if ( allPublications.contains( p ) )
								{
									temp.add( p );
									Map<String, Object> items = new HashMap<String, Object>();
									items.put( "name", p.getTitle() );
									items.put( "id", p.getId() );
									tempListItems.add( items );
								}
							}

							Map<String, Object> mapValuesForPairs = new LinkedHashMap<String, Object>();
							List<Integer> sets = new ArrayList<Integer>();
							sets.add( i );
							sets.add( authorList.indexOf( previousAuthor ) );
							label = label + authorList.get( i ).getFirstName() + "-" + previousAuthor.getFirstName();
							mapValuesForPairs.put( "sets", sets );
							mapValuesForPairs.put( "size", tempListItems.size() );
							mapValuesForPairs.put( "list", tempListItems );
							mapValuesForPairs.put( "altLabel", label );
							listOfMaps.add( mapValuesForPairs );
						}
					}
				}
			}
			// common to all
			if ( authorList.size() > 2 )
			{
				List<Publication> allPublications = new ArrayList<Publication>();
				List<Integer> count = new ArrayList<Integer>();
				List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();

				for ( int i = 0; i < authorList.size(); i++ )
				{
					List<Publication> authorPublications = new ArrayList<Publication>( authorList.get( i ).getPublications() );

					for ( int k = 0; k < authorPublications.size(); k++ )
					{
						Boolean flag = false;
						if ( startYear.equals( "" ) || startYear.equals( "0" ) || yearFilterPresent.equals( "false" ) )
						{
							flag = true;
						}
						else
						{
							if ( authorPublications.get( k ).getYear() != null )
							{
								if ( ( Integer.parseInt( authorPublications.get( k ).getYear() ) >= Integer.parseInt( startYear ) && Integer.parseInt( authorPublications.get( k ).getYear() ) <= Integer.parseInt( endYear ) ) )
								{
									flag = true;
								}
							}
						}
						if ( flag )
						{

							if ( !allPublications.contains( authorPublications.get( k ) ) )
							{
								allPublications.add( authorPublications.get( k ) );
								Map<String, Object> items = new HashMap<String, Object>();
								items.put( "name", authorPublications.get( k ).getTitle() );
								items.put( "id", authorPublications.get( k ).getId() );
								combinedListItems.add( items );
								count.add( 0 );

							}
							else
								count.set( allPublications.indexOf( authorPublications.get( k ) ), count.get( allPublications.indexOf( authorPublications.get( k ) ) ) + 1 );
						}
					}

				}

				for ( int i = 0; i < allPublications.size(); i++ )
				{
					if ( count.get( i ) < authorList.size() - 1 )
					{
						count.remove( i );
						allPublications.remove( i );
						combinedListItems.remove( i );
						i--;
					}
				}

				Map<String, Object> mapValuesForAll = new LinkedHashMap<String, Object>();
				List<Integer> sets = new ArrayList<Integer>();
				for ( int i = 0; i < authorList.size(); i++ )
				{
					sets.add( i );
				}

				mapValuesForAll.put( "sets", sets );
				mapValuesForAll.put( "size", count.size() );
				mapValuesForAll.put( "list", combinedListItems );

				String label = "";
				for ( int i = 0; i < authorList.size(); i++ )
				{
					label = label + authorList.get( i ).getFirstName();
				}

				mapValuesForAll.put( "altLabel", label );
				mapValuesForAll.put( "weight", 1000 );
				listOfMaps.add( mapValuesForAll );
			}

		}
		if ( visType.equals( "topics" ) )
		{
			if ( type.equals( "researcher" ) )
			{
				Map<Author, List<String>> mapTopics = new HashMap<Author, List<String>>();

				for ( int i = 0; i < authorList.size(); i++ )
				{
					Map<String, Object> mapValues = new HashMap<String, Object>();
					List<Integer> index = new ArrayList<Integer>();
					index.add( i );

					List<String> allAuthorInterests = new ArrayList<String>();
					List<String> allAuthorInterestIds = new ArrayList<String>();
					Set<AuthorInterestProfile> authorInterestProfiles = authorList.get( i ).getAuthorInterestProfiles();
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
								if ( !allAuthorInterests.contains( interests.get( j ).getTerm() ) )
								{
									allAuthorInterests.add( interests.get( j ).getTerm() );
									allAuthorInterestIds.add( interests.get( j ).getId() );
								}
							}
						}
					}

					System.out.println( "all interests size in VIS: " + allAuthorInterests.size() );

					Set<Publication> authorPublications = authorList.get( i ).getPublications();
					List<String> interestTopicNames = new ArrayList<String>();
					List<String> interestTopicIds = new ArrayList<String>();
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
							List<PublicationTopic> topics = new ArrayList<PublicationTopic>( p.getPublicationTopics() );
							for ( PublicationTopic pt : topics )
							{
								Map<String, Double> termValues = pt.getTermValues();
								List<String> terms = new ArrayList<String>( termValues.keySet() );
								for ( int k = 0; k < terms.size(); k++ )
								{
									if ( !interestTopicNames.contains( terms.get( k ) ) )// &&
									{
										if ( allAuthorInterests.contains( terms.get( k ) ) )
										{
											interestTopicNames.add( terms.get( k ) );
											int pos = allAuthorInterests.indexOf( terms.get( k ) );
											interestTopicIds.add( allAuthorInterestIds.get( pos ) );

											Map<String, Object> items = new HashMap<String, Object>();
											items.put( "name", terms.get( k ) );
											items.put( "id", allAuthorInterestIds.get( pos ) );
											listItems.add( items );
										}
									}
									if ( !interestTopicNames.contains( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) ) )// &&
									{
										if ( allAuthorInterests.contains( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) ) )
										{
											interestTopicNames.add( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) );
											int pos = allAuthorInterests.indexOf( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) );
											interestTopicIds.add( allAuthorInterestIds.get( pos ) );

											Map<String, Object> items = new HashMap<String, Object>();
											items.put( "name", terms.get( k ) );
											items.put( "id", allAuthorInterestIds.get( pos ) );
											listItems.add( items );
										}
									}
								}
							}

						}
					}
					mapTopics.put( authorList.get( i ), interestTopicNames );

					// single values to venn diagram
					mapValues.put( "sets", index );
					mapValues.put( "label", authorList.get( i ).getName() );
					mapValues.put( "size", listItems.size() );
					mapValues.put( "altLabel", authorList.get( i ).getName() );
					mapValues.put( "list", listItems );
					listOfMaps.add( mapValues );

					if ( mapTopics.size() > 1 )
					{
						// publications of authors added before current author
						for ( int k = 0; k < mapTopics.size(); k++ )
						{
							List<Author> previousAuthors = new ArrayList<Author>( mapTopics.keySet() );
							List<List<String>> previousAuthorLists = new ArrayList<List<String>>( mapTopics.values() );
							List<String> previousAuthorTopics = previousAuthorLists.get( k );
							Author previousAuthor = previousAuthors.get( k );
							List<Map<String, Object>> tempListItems = new ArrayList<Map<String, Object>>();
							String label = "";

							if ( !previousAuthor.equals( authorList.get( i ) ) )
							{
								List<String> tempNames = new ArrayList<String>();

								// find common topics
								for ( String pat : previousAuthorTopics )
								{
									if ( interestTopicNames.contains( pat ) )
									{
										tempNames.add( pat );
										int pos = interestTopicNames.indexOf( pat );

										Map<String, Object> items = new HashMap<String, Object>();
										items.put( "name", pat );
										items.put( "id", interestTopicIds.get( pos ) );
										tempListItems.add( items );
									}
								}

								Map<String, Object> mapValuesForPairs = new LinkedHashMap<String, Object>();
								List<Integer> sets = new ArrayList<Integer>();
								sets.add( i );
								sets.add( authorList.indexOf( previousAuthor ) );
								label = label + authorList.get( i ).getFirstName() + "-" + previousAuthor.getFirstName();
								mapValuesForPairs.put( "sets", sets );
								mapValuesForPairs.put( "size", tempListItems.size() );
								mapValuesForPairs.put( "list", tempListItems );
								mapValuesForPairs.put( "altLabel", label );
								listOfMaps.add( mapValuesForPairs );
							}
						}
					}
				}
				// common to all
				if ( authorList.size() > 2 )
				{
					List<String> allInterests = new ArrayList<String>();
					// List<String> allInterestIds = new ArrayList<String>();
					List<Integer> count = new ArrayList<Integer>();
					List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();

					for ( int i = 0; i < authorList.size(); i++ )
					{
						List<String> allAuthorInterests = new ArrayList<String>();
						List<String> allAuthorInterestIds = new ArrayList<String>();
						Set<AuthorInterestProfile> authorInterestProfiles = authorList.get( i ).getAuthorInterestProfiles();
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
									if ( !allAuthorInterests.contains( interests.get( j ).getTerm() ) )
									{
										allAuthorInterests.add( interests.get( j ).getTerm() );
										allAuthorInterestIds.add( interests.get( j ).getId() );
									}
								}
							}
						}

						Set<Publication> authorPublications = authorList.get( i ).getPublications();
						List<String> interestTopicNames = new ArrayList<String>();
						List<String> interestTopicIds = new ArrayList<String>();
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
								List<PublicationTopic> topics = new ArrayList<PublicationTopic>( p.getPublicationTopics() );
								for ( PublicationTopic pt : topics )
								{
									Map<String, Double> termValues = pt.getTermValues();
									List<String> terms = new ArrayList<String>( termValues.keySet() );
									for ( int k = 0; k < terms.size(); k++ )
									{
										if ( !interestTopicNames.contains( terms.get( k ) ) )// &&
										{
											if ( allAuthorInterests.contains( terms.get( k ) ) )
											{
												interestTopicNames.add( terms.get( k ) );
												int pos = allAuthorInterests.indexOf( terms.get( k ) );
												interestTopicIds.add( allAuthorInterestIds.get( pos ) );

												Map<String, Object> items = new HashMap<String, Object>();
												items.put( "name", terms.get( k ) );
												items.put( "id", allAuthorInterestIds.get( pos ) );
												listItems.add( items );
											}
										}
										if ( !interestTopicNames.contains( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) ) )// &&
										{
											if ( allAuthorInterests.contains( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) ) )
											{
												interestTopicNames.add( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) );
												int pos = allAuthorInterests.indexOf( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) );
												interestTopicIds.add( allAuthorInterestIds.get( pos ) );

												Map<String, Object> items = new HashMap<String, Object>();
												items.put( "name", terms.get( k ) );
												items.put( "id", allAuthorInterestIds.get( pos ) );
												listItems.add( items );
											}
										}
									}
								}

							}
						}

						for ( int k = 0; k < interestTopicNames.size(); k++ )
						{
							if ( !allInterests.contains( interestTopicNames.get( k ) ) )
							{
								allInterests.add( interestTopicNames.get( k ) );
								Map<String, Object> items = new HashMap<String, Object>();
								items.put( "name", interestTopicNames.get( k ) );
								items.put( "id", interestTopicIds.get( k ) );
								combinedListItems.add( items );
								count.add( 0 );

							}
							else
								count.set( allInterests.indexOf( interestTopicNames.get( k ) ), count.get( allInterests.indexOf( interestTopicNames.get( k ) ) ) + 1 );
						}

					}

					for ( int i = 0; i < allInterests.size(); i++ )
					{
						if ( count.get( i ) < authorList.size() - 1 )
						{
							count.remove( i );
							allInterests.remove( i );
							combinedListItems.remove( i );
							i--;
						}
					}

					Map<String, Object> mapValuesForAll = new LinkedHashMap<String, Object>();
					List<Integer> sets = new ArrayList<Integer>();
					for ( int i = 0; i < authorList.size(); i++ )
					{
						sets.add( i );
					}

					mapValuesForAll.put( "sets", sets );
					mapValuesForAll.put( "size", count.size() );
					mapValuesForAll.put( "list", combinedListItems );

					String label = "";
					for ( int i = 0; i < authorList.size(); i++ )
					{
						label = label + authorList.get( i ).getFirstName();
					}

					mapValuesForAll.put( "altLabel", label );
					mapValuesForAll.put( "weight", 1000 );
					listOfMaps.add( mapValuesForAll );
				}

			}
			if ( type.equals( "conference" ) )
			{
				Map<EventGroup, List<String>> mapTopics = new HashMap<EventGroup, List<String>>();
				List<EventGroup> eventGroupTempList = new ArrayList<EventGroup>();

				for ( int i = 0; i < idsList.size(); i++ )
				{
					Map<String, Object> mapValues = new HashMap<String, Object>();
					List<Integer> index = new ArrayList<Integer>();
					index.add( i );

					List<String> allConferenceInterests = new ArrayList<String>();
					List<String> allConferenceInterestIds = new ArrayList<String>();
					List<Publication> eventGroupPubs = new ArrayList<Publication>();
					EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( idsList.get( i ) );
					eventGroupTempList.add( eg );
					List<Event> events = eg.getEvents();
					for ( Event e : events )
					{
						Set<EventInterestProfile> eventInterestProfiles = e.getEventInterestProfiles();
						for ( EventInterestProfile eip : eventInterestProfiles )
						{
							List<EventInterest> eventInterests = new ArrayList<EventInterest>( eip.getEventInterests() );
							for ( EventInterest ei : eventInterests )
							{
								Map<Interest, Double> termWeights = ei.getTermWeights();
								List<Interest> interests = new ArrayList<Interest>( termWeights.keySet() );
								List<Double> weights = new ArrayList<Double>( termWeights.values() );
								for ( int j = 0; j < termWeights.size(); j++ )
								{
									if ( !allConferenceInterests.contains( interests.get( j ).getTerm() ) )// &&
																											// weights.get(
																											// j
																											// )
																											// >
																											// 0.5
																											// )
									{
										allConferenceInterests.add( interests.get( j ).getTerm() );
										allConferenceInterestIds.add( interests.get( j ).getId() );
									}
								}
							}
						}

						List<Publication> eventPubs = e.getPublications();
						for ( Publication pub : eventPubs )
						{
							if ( !eventGroupPubs.contains( pub ) )
							{
								eventGroupPubs.add( pub );
							}
						}
					}

					List<String> interestTopicNames = new ArrayList<String>();
					List<String> interestTopicIds = new ArrayList<String>();
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
							List<PublicationTopic> topics = new ArrayList<PublicationTopic>( p.getPublicationTopics() );
							for ( PublicationTopic pt : topics )
							{
								Map<String, Double> termValues = pt.getTermValues();
								List<String> terms = new ArrayList<String>( termValues.keySet() );
								List<Double> weights = new ArrayList<Double>( termValues.values() );
								for ( int k = 0; k < terms.size(); k++ )
								{
									if ( !interestTopicNames.contains( terms.get( k ) ) )// &&
									{
										if ( allConferenceInterests.contains( terms.get( k ) ) )
										{
											interestTopicNames.add( terms.get( k ) );
											int pos = allConferenceInterests.indexOf( terms.get( k ) );
											interestTopicIds.add( allConferenceInterestIds.get( pos ) );

											Map<String, Object> items = new HashMap<String, Object>();
											items.put( "name", terms.get( k ) );
											items.put( "id", allConferenceInterestIds.get( pos ) );
											listItems.add( items );

										}
									}
									if ( terms.get( k ).length() > 0 )
									{
										if ( !interestTopicNames.contains( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) ) )// &&
										{
											if ( allConferenceInterests.contains( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) ) )
											{
												interestTopicNames.add( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) );
												int pos = allConferenceInterests.indexOf( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) );
												interestTopicIds.add( allConferenceInterestIds.get( pos ) );

												Map<String, Object> items = new HashMap<String, Object>();
												items.put( "name", terms.get( k ) );
												items.put( "id", allConferenceInterestIds.get( pos ) );
												listItems.add( items );
											}
										}
									}

								}
							}

						}
					}
					mapTopics.put( eg, interestTopicNames );

					// single values to venn diagram
					mapValues.put( "sets", index );
					mapValues.put( "label", eg.getName() );
					mapValues.put( "size", interestTopicNames.size() );
					mapValues.put( "altLabel", idsList.get( i ) );
					mapValues.put( "list", listItems );
					listOfMaps.add( mapValues );

					if ( mapTopics.size() > 1 )
					{
						// publications of authors added before current author
						for ( int k = 0; k < mapTopics.size(); k++ )
						{
							List<EventGroup> previousEvents = new ArrayList<EventGroup>( mapTopics.keySet() );
							List<List<String>> previousAuthorLists = new ArrayList<List<String>>( mapTopics.values() );
							List<String> previousAuthorTopics = previousAuthorLists.get( k );
							EventGroup previousEvent = previousEvents.get( k );
							List<List<Map<String, Object>>> tempListItems = new ArrayList<List<Map<String, Object>>>();
							String label = "";
							if ( !previousEvent.equals( eg ) )
							{
								List<PublicationTopic> temp = new ArrayList<PublicationTopic>();
								List<String> tempNames = new ArrayList<String>();

								// find common topics
								for ( String pat : previousAuthorTopics )
								{
									if ( interestTopicNames.contains( pat ) )
									{
										tempNames.add( pat );
										int pos = interestTopicNames.indexOf( pat );

										Map<String, Object> items = new HashMap<String, Object>();
										items.put( "name", pat );
										items.put( "id", interestTopicIds.get( pos ) );
										listItems.add( items );
									}
								}

								Map<String, Object> mapValuesForPairs = new LinkedHashMap<String, Object>();
								List<Integer> sets = new ArrayList<Integer>();
								sets.add( i );
								sets.add( eventGroupTempList.indexOf( previousEvent ) );
								label = label + idsList.get( i ) + "-" + previousEvent.getName();
								mapValuesForPairs.put( "sets", sets );
								mapValuesForPairs.put( "size", tempNames.size() );
								mapValuesForPairs.put( "list", tempListItems );
								mapValuesForPairs.put( "altLabel", label );
								listOfMaps.add( mapValuesForPairs );
							}
						}
					}

				}
				if ( idsList.size() > 2 )
				{
					List<String> allInterests = new ArrayList<String>();
					// List<String> allInterestIds = new ArrayList<String>();
					List<Integer> count = new ArrayList<Integer>();
					List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();

					for ( int i = 0; i < idsList.size(); i++ )
					{
						List<String> allConferenceInterests = new ArrayList<String>();
						List<String> allConferenceInterestIds = new ArrayList<String>();
						List<Publication> eventGroupPubs = new ArrayList<Publication>();
						EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( idsList.get( i ) );
						eventGroupTempList.add( eg );
						List<Event> events = eg.getEvents();
						for ( Event e : events )
						{
							Set<EventInterestProfile> eventInterestProfiles = e.getEventInterestProfiles();
							for ( EventInterestProfile eip : eventInterestProfiles )
							{
								List<EventInterest> eventInterests = new ArrayList<EventInterest>( eip.getEventInterests() );
								for ( EventInterest ei : eventInterests )
								{
									Map<Interest, Double> termWeights = ei.getTermWeights();
									List<Interest> interests = new ArrayList<Interest>( termWeights.keySet() );
									List<Double> weights = new ArrayList<Double>( termWeights.values() );
									for ( int j = 0; j < termWeights.size(); j++ )
									{
										if ( !allConferenceInterests.contains( interests.get( j ).getTerm() ) && weights.get( j ) > 0.5 )
										{
											allConferenceInterests.add( interests.get( j ).getTerm() );
											allConferenceInterestIds.add( interests.get( j ).getId() );
										}
									}
								}
							}

							List<Publication> eventPubs = e.getPublications();
							for ( Publication pub : eventPubs )
							{
								if ( !eventGroupPubs.contains( pub ) )
								{
									eventGroupPubs.add( pub );
								}
							}
						}

						List<String> interestTopicNames = new ArrayList<String>();
						List<String> interestTopicIds = new ArrayList<String>();
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
								List<PublicationTopic> topics = new ArrayList<PublicationTopic>( p.getPublicationTopics() );
								for ( PublicationTopic pt : topics )
								{
									Map<String, Double> termValues = pt.getTermValues();
									List<String> terms = new ArrayList<String>( termValues.keySet() );
									List<Double> weights = new ArrayList<Double>( termValues.values() );
									for ( int k = 0; k < terms.size(); k++ )
									{
										if ( !interestTopicNames.contains( terms.get( k ) ) )// &&
										{
											if ( allConferenceInterests.contains( terms.get( k ) ) )
											{
												interestTopicNames.add( terms.get( k ) );
												int pos = allConferenceInterests.indexOf( terms.get( k ) );
												interestTopicIds.add( allConferenceInterestIds.get( pos ) );

												Map<String, Object> items = new HashMap<String, Object>();
												items.put( "name", terms.get( k ) );
												items.put( "id", allConferenceInterestIds.get( pos ) );
												listItems.add( items );
											}
										}
										if ( terms.get( k ).length() > 0 )
										{
											if ( !interestTopicNames.contains( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) ) )// &&
											{
												if ( allConferenceInterests.contains( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) ) )
												{
													interestTopicNames.add( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) );
													int pos = allConferenceInterests.indexOf( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) );
													interestTopicIds.add( allConferenceInterestIds.get( pos ) );

													Map<String, Object> items = new HashMap<String, Object>();
													items.put( "name", terms.get( k ) );
													items.put( "id", allConferenceInterestIds.get( pos ) );
													listItems.add( items );
												}
											}
										}
									}
								}

							}
						}

						for ( int k = 0; k < interestTopicNames.size(); k++ )
						{
							if ( !allInterests.contains( interestTopicNames.get( k ) ) )
							{
								allInterests.add( interestTopicNames.get( k ) );
								Map<String, Object> items = new HashMap<String, Object>();
								items.put( "name", interestTopicNames.get( k ) );
								items.put( "id", interestTopicIds.get( k ) );
								combinedListItems.add( items );

								count.add( 0 );

							}
							else
								count.set( allInterests.indexOf( interestTopicNames.get( k ) ), count.get( allInterests.indexOf( interestTopicNames.get( k ) ) ) + 1 );
						}

					}

					for ( int i = 0; i < allInterests.size(); i++ )
					{
						if ( count.get( i ) < authorList.size() - 1 )
						{
							count.remove( i );
							allInterests.remove( i );
							combinedListItems.remove( i );
							i--;
						}
					}

					Map<String, Object> mapValuesForAll = new LinkedHashMap<String, Object>();
					List<Integer> sets = new ArrayList<Integer>();
					for ( int i = 0; i < authorList.size(); i++ )
					{
						sets.add( i );
					}

					mapValuesForAll.put( "sets", sets );
					mapValuesForAll.put( "size", count.size() );
					mapValuesForAll.put( "list", combinedListItems );

					String label = "";
					for ( int i = 0; i < idsList.size(); i++ )
					{
						label = label + idsList.get( i );
					}

					mapValuesForAll.put( "altLabel", label );
					mapValuesForAll.put( "weight", 1000 );
					listOfMaps.add( mapValuesForAll );

				}
			}
			if ( type.equals( "publication" ) )
			{
				Map<Publication, List<String>> mapTopics = new HashMap<Publication, List<String>>();
				List<Publication> publicationTempList = new ArrayList<Publication>();
				List<Interest> allInterestsInDB = persistenceStrategy.getInterestDAO().allTerms();

				for ( int i = 0; i < idsList.size(); i++ )
				{
					Map<String, Object> mapValues = new HashMap<String, Object>();
					List<Integer> index = new ArrayList<Integer>();
					index.add( i );

					List<String> allPublicationInterests = new ArrayList<String>();
					List<String> allPublicationInterestIds = new ArrayList<String>();
					// List<Publication> eventGroupPubs = new
					// ArrayList<Publication>();
					Publication p = persistenceStrategy.getPublicationDAO().getById( idsList.get( i ) );
					publicationTempList.add( p );
					for ( Interest interest : allInterestsInDB )
					{
						if ( !allPublicationInterests.contains( interest.getTerm() ) )
						{
							allPublicationInterests.add( interest.getTerm() );
							allPublicationInterestIds.add( interest.getId() );
						}
					}

					// List<Event> events = eg.getEvents();
					// for ( Event e : events )
					// {/
					// Set<EventInterestProfile> eventInterestProfiles =
					// e.getEventInterestProfiles();
					// for ( EventInterestProfile eip : eventInterestProfiles )
					// {
					// List<EventInterest> eventInterests = new
					// ArrayList<EventInterest>( eip.getEventInterests() );
					// for ( EventInterest ei : eventInterests )
					// {
					// Map<Interest, Double> termWeights = ei.getTermWeights();
					// List<Interest> interests = new ArrayList<Interest>(
					// termWeights.keySet() );
					// List<Double> weights = new ArrayList<Double>(
					// termWeights.values() );
					// for ( int j = 0; j < termWeights.size(); j++ )
					// {
					// if ( !allConferenceInterests.contains( interests.get( j
					// ).getTerm() ) )// &&
					// // weights.get(
					// // j
					// // )
					// // >
					// // 0.5
					// // )
					// {
					// allConferenceInterests.add( interests.get( j ).getTerm()
					// );
					// allConferenceInterestIds.add( interests.get( j ).getId()
					// );
					// }
					// }
					// }
					// }
					//
					// List<Publication> eventPubs = e.getPublications();
					// for ( Publication pub : eventPubs )
					// {
					// if ( !eventGroupPubs.contains( pub ) )
					// {
					// eventGroupPubs.add( pub );
					// }
					// }
					// }

					List<String> interestTopicNames = new ArrayList<String>();
					List<String> interestTopicIds = new ArrayList<String>();
					List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();

					// for ( Publication p : eventGroupPubs )
					// {
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
							List<Double> weights = new ArrayList<Double>( termValues.values() );
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
								}
								if ( !interestTopicNames.contains( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) ) )// &&
								{
									if ( allPublicationInterests.contains( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) ) )
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

						// }
					}
					mapTopics.put( p, interestTopicNames );

					// single values to venn diagram
					mapValues.put( "sets", index );
					mapValues.put( "label", p.getTitle() );
					mapValues.put( "size", interestTopicNames.size() );
					mapValues.put( "altLabel", idsList.get( i ) );
					mapValues.put( "list", listItems );
					listOfMaps.add( mapValues );

					if ( mapTopics.size() > 1 )
					{
						// publications of authors added before current author
						for ( int k = 0; k < mapTopics.size(); k++ )
						{
							List<Publication> previousPublications = new ArrayList<Publication>( mapTopics.keySet() );
							List<List<String>> previousAuthorLists = new ArrayList<List<String>>( mapTopics.values() );
							List<String> previousAuthorTopics = previousAuthorLists.get( k );
							Publication previousPublication = previousPublications.get( k );
							List<Map<String, Object>> tempListItems = new ArrayList<Map<String, Object>>();
							String label = "";
							if ( !previousPublication.equals( p ) )
							{
								List<PublicationTopic> temp = new ArrayList<PublicationTopic>();
								List<String> tempNames = new ArrayList<String>();

								// find common topics
								for ( String pat : previousAuthorTopics )
								{
									if ( interestTopicNames.contains( pat ) )
									{
										tempNames.add( pat );
										int pos = interestTopicNames.indexOf( pat );

										Map<String, Object> items = new HashMap<String, Object>();
										items.put( "name", pat );
										items.put( "id", interestTopicIds.get( pos ) );
										listItems.add( items );
									}
								}

								Map<String, Object> mapValuesForPairs = new LinkedHashMap<String, Object>();
								List<Integer> sets = new ArrayList<Integer>();
								sets.add( i );
								sets.add( publicationTempList.indexOf( previousPublication ) );
								label = label + idsList.get( i ) + "-" + previousPublication.getTitle();
								mapValuesForPairs.put( "sets", sets );
								mapValuesForPairs.put( "size", tempNames.size() );
								mapValuesForPairs.put( "list", tempListItems );
								mapValuesForPairs.put( "altLabel", label );
								listOfMaps.add( mapValuesForPairs );
							}
						}
					}

				}
				if ( idsList.size() > 2 )
				{
					List<String> allInterests = new ArrayList<String>();
					List<Integer> count = new ArrayList<Integer>();
					List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();

					for ( int i = 0; i < idsList.size(); i++ )
					{
						List<String> allPublicationInterests = new ArrayList<String>();
						List<String> allPublicationInterestIds = new ArrayList<String>();
						Publication p = persistenceStrategy.getPublicationDAO().getById( idsList.get( i ) );
						publicationTempList.add( p );
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
								List<Double> weights = new ArrayList<Double>( termValues.values() );
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
									}
									if ( !interestTopicNames.contains( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) ) )// &&
									{
										if ( allPublicationInterests.contains( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) ) )
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

						for ( int k = 0; k < interestTopicNames.size(); k++ )
						{
							if ( !allInterests.contains( interestTopicNames.get( k ) ) )
							{
								allInterests.add( interestTopicNames.get( k ) );
								Map<String, Object> items = new HashMap<String, Object>();
								items.put( "name", interestTopicNames.get( k ) );
								items.put( "id", interestTopicIds.get( k ) );
								combinedListItems.add( items );

								count.add( 0 );

							}
							else
								count.set( allInterests.indexOf( interestTopicNames.get( k ) ), count.get( allInterests.indexOf( interestTopicNames.get( k ) ) ) + 1 );
						}

					}

					for ( int i = 0; i < allInterests.size(); i++ )
					{
						if ( count.get( i ) < idsList.size() - 1 )
						{
							count.remove( i );
							allInterests.remove( i );
							combinedListItems.remove( i );
							i--;
						}
					}

					Map<String, Object> mapValuesForAll = new LinkedHashMap<String, Object>();
					List<Integer> sets = new ArrayList<Integer>();
					for ( int i = 0; i < idsList.size(); i++ )
					{
						sets.add( i );
					}

					mapValuesForAll.put( "sets", sets );
					mapValuesForAll.put( "size", count.size() );
					mapValuesForAll.put( "list", combinedListItems );

					String label = "";
					for ( int i = 0; i < idsList.size(); i++ )
					{
						label = label + idsList.get( i );
					}

					mapValuesForAll.put( "altLabel", label );
					mapValuesForAll.put( "weight", 1000 );
					listOfMaps.add( mapValuesForAll );

				}

			}

		}

		Map<String, Object> visMap = new HashMap<String, Object>();

		visMap.put( "comparisonList", listOfMaps );
		return visMap;

	}

	public Map<String, Object> visualizeSimilar( String type, String visType, List<Author> authorList, List<String> idsList )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();
		if ( visType.equals( "researchers" ) )
		{
			Map<String, Object> map = similarityService.similarAuthors( authorList );
			if ( map != null )
			{

				@SuppressWarnings( "unchecked" )
				Map<DataMiningAuthor, Map<String, Double>> interestMap = (Map<DataMiningAuthor, Map<String, Double>>) map.get( "interestMap" );

				@SuppressWarnings( "unchecked" )
				Map<DataMiningAuthor, Double> scoreMap = (Map<DataMiningAuthor, Double>) map.get( "scoreMap" );

				List<Double> similarityValues = new ArrayList<Double>( scoreMap.values() );
				List<DataMiningAuthor> similarAuthors = new ArrayList<DataMiningAuthor>( scoreMap.keySet() );

				List<Double> truncSimilarityValues = new ArrayList<Double>();
				List<String> authorNames = new ArrayList<String>();
				List<String> authorIds = new ArrayList<String>();
				List<Map<String, Double>> truncInterests = new ArrayList<Map<String, Double>>();

				// System.out.println( map.size() + " : maz size" );

				int count = 0;
				if ( interestMap.size() > 20 )
					count = 20;
				else
					count = interestMap.size();

				for ( int i = 0; i < count; i++ )
				{
					truncSimilarityValues.add( similarityValues.get( i ) );
					authorNames.add( similarAuthors.get( i ).getName() );
					authorIds.add( similarAuthors.get( i ).getId() );
					truncInterests.add( interestMap.get( similarAuthors.get( i ) ) );
				}

				visMap.put( "authorNames", authorNames );
				visMap.put( "authorIds", authorIds );
				visMap.put( "similarity", truncSimilarityValues );
				visMap.put( "interests", truncInterests );
				return visMap;
			}
			else
				return (Map<String, Object>) visMap.put( "coauthors", "none" );
		}
		if ( visType.equals( "conferences" ) )
		{
			Map<String, Object> map = similarityService.similarConferences( idsList );
			if ( map != null )
			{
				@SuppressWarnings( "unchecked" )
				Map<DataMiningPublication, Map<String, Double>> interestMap = (Map<DataMiningPublication, Map<String, Double>>) map.get( "interestMap" );

				@SuppressWarnings( "unchecked" )
				Map<DataMiningEventGroup, Double> scoreMap = (Map<DataMiningEventGroup, Double>) map.get( "scoreMap" );

				// System.out.println( map.toString() );
				List<Double> similarityValues = new ArrayList<Double>( scoreMap.values() );
				List<DataMiningEventGroup> similarConferences = new ArrayList<DataMiningEventGroup>( scoreMap.keySet() );

				List<Double> truncSimilarityValues = new ArrayList<Double>();
				List<String> conferenceNames = new ArrayList<String>();
				List<String> conferenceIds = new ArrayList<String>();
				List<Map<String, Double>> truncInterests = new ArrayList<Map<String, Double>>();

				int count = 0;
				if ( interestMap.size() > 20 )
					count = 20;
				else
					count = interestMap.size();

				for ( int i = 0; i < count; i++ )
				{
					// System.out.println( similarityValues.get( i ) + " : " +
					// similarConferences.get( i ).getName() );
					truncSimilarityValues.add( similarityValues.get( i ) );
					conferenceNames.add( similarConferences.get( i ).getName() );
					conferenceIds.add( similarConferences.get( i ).getId() );
					truncInterests.add( interestMap.get( similarConferences.get( i ) ) );
				}

				visMap.put( "authorNames", conferenceNames );
				visMap.put( "authorIds", conferenceIds );
				visMap.put( "similarity", truncSimilarityValues );
				visMap.put( "interests", truncInterests );
				return visMap;
			}
			else
				return (Map<String, Object>) visMap.put( "coauthors", "none" );

		}
		if ( visType.equals( "publications" ) )
		{
			Map<String, Object> map = similarityService.similarPublications( idsList );
			if ( map != null )
			{
				@SuppressWarnings( "unchecked" )
				Map<DataMiningPublication, Map<String, Double>> interestMap = (Map<DataMiningPublication, Map<String, Double>>) map.get( "interestMap" );

				@SuppressWarnings( "unchecked" )
				Map<DataMiningPublication, Double> scoreMap = (Map<DataMiningPublication, Double>) map.get( "scoreMap" );

				List<Double> similarityValues = new ArrayList<Double>( scoreMap.values() );
				List<DataMiningPublication> similarPublications = new ArrayList<DataMiningPublication>( scoreMap.keySet() );

				List<Double> truncSimilarityValues = new ArrayList<Double>();
				List<String> publicationNames = new ArrayList<String>();
				List<String> publicationsIds = new ArrayList<String>();
				List<Map<String, Double>> truncInterests = new ArrayList<Map<String, Double>>();

				// System.out.println( map.size() + " : maz size" );

				int count = 0;
				if ( interestMap.size() > 20 )
					count = 20;
				else
					count = interestMap.size();

				for ( int i = 0; i < count; i++ )
				{
					truncSimilarityValues.add( similarityValues.get( i ) );
					publicationNames.add( similarPublications.get( i ).getTitle() );
					publicationsIds.add( similarPublications.get( i ).getId() );
					truncInterests.add( interestMap.get( similarPublications.get( i ) ) );
				}

				visMap.put( "authorNames", publicationNames );
				visMap.put( "authorIds", publicationsIds );
				visMap.put( "similarity", truncSimilarityValues );
				visMap.put( "interests", truncInterests );
				return visMap;
			}
			else
				return (Map<String, Object>) visMap.put( "coauthors", "none" );

		}
		if ( visType.equals( "topics" ) )
		{
			return null;
		}
		if ( visType.equals( "circles" ) )
		{
			return null;
		}

		// else
		return null;
	}

}

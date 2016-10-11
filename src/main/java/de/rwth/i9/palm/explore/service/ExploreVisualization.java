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

	public Map<String, Object> visualizeLocations( String type, Set<Publication> publications, List<String> idsList, String startYear, String endYear, List<String> filteredTopic )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		if ( type.equals( "researcher" ) )
			visMap.putAll( researcherFeature.getResearcherAcademicEventTree().getResearcherAllAcademicEvents( publications, true ) );

		if ( type.equals( "conference" ) )
		{
			visMap.putAll( researcherFeature.getResearcherAcademicEventTree().getResearcherAllAcademicEvents( publications, true ) );
			// Boolean valid = false;
			// if ( startYear.equals( "" ) || startYear.equals( "0" ) )
			// {
			// valid = true;
			// }
			// List<Object> listEvents = new ArrayList<Object>();
			// for ( int i = 0; i < idsList.size(); i++ )
			// {
			// // System.out.println( " name in vis: " +
			// // eventFeature.getEventMining().fetchEventGroupData(
			// // idsList.get( i ), null, null ).get( "name" ) );
			// @SuppressWarnings( "unchecked" )
			// List<Object> innerList = (List<Object>)
			// eventFeature.getEventMining().fetchEventGroupData( idsList.get( i
			// ), null, null ).get( "events" );
			// for ( int j = 0; j < innerList.size(); j++ )
			// {
			// @SuppressWarnings( "unchecked" )
			// Map<String, Object> innerListMap = (Map<String, Object>)
			// innerList.get( j );
			//
			// if ( !startYear.equals( "" ) && !startYear.equals( "0" ) )
			// if ( Integer.parseInt( startYear ) <= Integer.parseInt(
			// innerListMap.get( "year" ).toString() ) && Integer.parseInt(
			// endYear ) >= Integer.parseInt( innerListMap.get( "year"
			// ).toString() ) )
			// valid = true;
			// else
			// valid = false;
			//
			// if ( !filteredTopic.isEmpty() && valid )
			// {
			// Event e = persistenceStrategy.getEventDAO().getById(
			// innerListMap.get( "id" ).toString() );
			// Set<EventInterestProfile> eips = e.getEventInterestProfiles();
			// List<String> interestStrings = new ArrayList<String>();
			//
			// for ( EventInterestProfile eip : eips )
			// {
			// Set<EventInterest> eventInterests = eip.getEventInterests();
			// for ( EventInterest ei : eventInterests )
			// {
			// Map<Interest, Double> termWeights = ei.getTermWeights();
			// List<Interest> interests = new ArrayList<Interest>(
			// termWeights.keySet() );
			// // List<Double> weights = new
			// // ArrayList<Double>(termWeights.values());
			// for ( Interest interest : interests )
			// {
			// if ( !interestStrings.contains( interest.getTerm() ) )
			// interestStrings.add( interest.getTerm() );
			// }
			//
			// }
			//
			// }
			// // System.out.println( interestStrings );
			//
			// if ( interestStrings.containsAll( filteredTopic ) )
			// {
			// valid = true;
			// }
			// else
			// valid = false;
			//
			// // System.out.println( "\nvalid: " + valid );
			// }
			//
			// if ( valid )
			// listEvents.add( innerList.get( j ) );
			// }
			// }
			// visMap.put( "events", listEvents );
			// // System.out.println( "VISMAP: " + visMap.toString() );
			//
		}

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
			List<String> authorInterests = new ArrayList<String>();
			List<Double> authorInterestWeights = new ArrayList<Double>();

			for ( Author a : authorList )
			{
				Map<String, List<String>> yearWiseInterests = new HashMap<String, List<String>>();

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
							String interest = ( interestTerm.next().getTerm() );
							Double weight = interestTermWeight.next();

							if ( allTopics.contains( interest ) )
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
									List<List<String>> yWIVal = new ArrayList<List<String>>( yearWiseInterests.values() );
									if ( yWI.contains( year ) )
									{
										int index = yWI.indexOf( year );
										if ( !yWIVal.get( index ).contains( interest ) )
										{
											yWIVal.get( index ).add( interest );
											if ( !authorInterests.contains( interest ) )
											{
												authorInterests.add( interest );
												authorInterestWeights.add( weight );
											}
											else
											{
												int ind = authorInterests.indexOf( interest );
												authorInterestWeights.set( ind, authorInterestWeights.get( ind ) + weight );
											}
											Map<String, Object> values = new HashMap<String, Object>();

											values.put( "Author", a.getName() );
											values.put( "Topic", interest );
											values.put( "Year", year );
											values.put( "Weight", weight );
											mapList.add( values );
										}
										else
										{
											int ind = authorInterests.indexOf( interest );
											authorInterestWeights.set( ind, authorInterestWeights.get( ind ) + weight );
										}
									}
									else
									{
										List<String> newInterestList = new ArrayList<String>();
										newInterestList.add( interest );
										if ( !authorInterests.contains( interest ) )
										{
											authorInterests.add( interest );
											authorInterestWeights.add( weight );
										}
										else
										{
											int ind = authorInterests.indexOf( interest );
											authorInterestWeights.set( ind, authorInterestWeights.get( ind ) + weight );
										}
										yearWiseInterests.put( year, newInterestList );

										Map<String, Object> values = new HashMap<String, Object>();

										values.put( "Author", a.getName() );
										values.put( "Topic", interest );
										values.put( "Year", year );
										values.put( "Weight", weight );
										mapList.add( values );
									}
								}

							}
						}

					}
				}

			}

			double threshold = 5.0;
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
				if ( !authorInterests.contains( mapList.get( i ).get( "Topic" ).toString() ) )
				{
					mapList.remove( i );
					i--;
				}
			}

			visMap.put( "list", mapList );
		}
		if ( type.equals( "conference" ) )
		{
			List<String> conferenceInterests = new ArrayList<String>();
			List<Double> conferenceInterestWeights = new ArrayList<Double>();

			for ( String id : idsList )
			{
				EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( id );
				Map<String, List<String>> yearWiseInterests = new HashMap<String, List<String>>();

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
								String interest = ( interestTerm.next().getTerm() );
								Double weight = interestTermWeight.next();

								if ( allTopics.contains( interest ) )
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
										List<List<String>> yWIVal = new ArrayList<List<String>>( yearWiseInterests.values() );
										if ( yWI.contains( year ) )
										{
											int index = yWI.indexOf( year );
											if ( !yWIVal.get( index ).contains( interest ) )
											{
												yWIVal.get( index ).add( interest );
												if ( !conferenceInterests.contains( interest ) )
												{
													conferenceInterests.add( interest );
													conferenceInterestWeights.add( weight );
												}
												else
												{
													int ind = conferenceInterests.indexOf( interest );
													conferenceInterestWeights.set( ind, conferenceInterestWeights.get( ind ) + weight );
												}
												Map<String, Object> values = new HashMap<String, Object>();

												values.put( "Author", eg.getName() );
												values.put( "Topic", interest );
												values.put( "Year", year );
												values.put( "Weight", weight );
												mapList.add( values );
											}
											else
											{
												int ind = conferenceInterests.indexOf( interest );
												conferenceInterestWeights.set( ind, conferenceInterestWeights.get( ind ) + weight );
											}
										}
										else
										{
											List<String> newInterestList = new ArrayList<String>();
											newInterestList.add( interest );
											if ( !conferenceInterests.contains( interest ) )
											{
												conferenceInterests.add( interest );
												conferenceInterestWeights.add( weight );
											}
											else
											{
												int ind = conferenceInterests.indexOf( interest );
												conferenceInterestWeights.set( ind, conferenceInterestWeights.get( ind ) + weight );
											}
											yearWiseInterests.put( year, newInterestList );

											Map<String, Object> values = new HashMap<String, Object>();

											values.put( "Author", eg.getName() );
											values.put( "Topic", interest );
											values.put( "Year", year );
											values.put( "Weight", weight );
											mapList.add( values );
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
				if ( !conferenceInterests.contains( mapList.get( i ).get( "Topic" ).toString() ) )
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
			List<String> authorInterests = new ArrayList<String>();
			List<Double> authorInterestWeights = new ArrayList<Double>();
			List<Map<String, Double>> authorInterestList = new ArrayList<Map<String, Double>>();
			for ( Author a : authorList )
			{
				Map<String, List<String>> yearWiseInterests = new HashMap<String, List<String>>();

				Map<String, Double> interestWeightMap = new HashMap<String, Double>();
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
							String interest = ( interestTerm.next().getTerm() );
							Double weight = interestTermWeight.next();

							if ( allTopics.contains( interest ) )
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
									List<List<String>> yWIVal = new ArrayList<List<String>>( yearWiseInterests.values() );
									if ( yWI.contains( year ) )
									{
										int index = yWI.indexOf( year );
										if ( !yWIVal.get( index ).contains( interest ) )
										{
											yWIVal.get( index ).add( interest );
											if ( !authorInterests.contains( interest ) )
											{
												authorInterests.add( interest );
												authorInterestWeights.add( weight );
											}
											else
											{
												int ind = authorInterests.indexOf( interest );
												authorInterestWeights.set( ind, authorInterestWeights.get( ind ) + weight );
											}
											if ( interestWeightMap.containsKey( interest ) )
											{
												double w = interestWeightMap.get( interest );
												interestWeightMap.remove( interest );
												interestWeightMap.put( interest, w + 1.0 );
											}
											else
												interestWeightMap.put( interest, 1.0 );
										}
										else
										{
											int ind = authorInterests.indexOf( interest );
											authorInterestWeights.set( ind, authorInterestWeights.get( ind ) + weight );
										}
									}
									else
									{
										List<String> newInterestList = new ArrayList<String>();
										newInterestList.add( interest );
										if ( !authorInterests.contains( interest ) )
										{
											authorInterests.add( interest );
											authorInterestWeights.add( weight );
										}
										else
										{
											int ind = authorInterests.indexOf( interest );
											authorInterestWeights.set( ind, authorInterestWeights.get( ind ) + weight );
										}
										yearWiseInterests.put( year, newInterestList );
										if ( interestWeightMap.containsKey( interest ) )
										{
											double w = interestWeightMap.get( interest );
											interestWeightMap.remove( interest );
											interestWeightMap.put( interest, w + 1.0 );
										}
										else
											interestWeightMap.put( interest, 1.0 );
									}

								}
							}
						}
					}
				}

				authorInterestList.add( interestWeightMap );
			}

			Map<String, Object> finalMap = new HashMap<String, Object>();

			double threshold = 5.0;
			if ( authorList.size() > 1 )
			{
				threshold = 2.0;
			}

			List<Object[]> listObjects = new ArrayList<Object[]>();
			for ( int i = 0; i < authorInterests.size(); i++ )
			{
				if ( authorInterestWeights.get( i ) > threshold )
				{
					List<Double> interestList = new ArrayList<Double>();

					for ( int j = 0; j < authorInterestList.size(); j++ )
					{
						Map<String, Double> inWei = authorInterestList.get( j );
						List<String> in = new ArrayList<String>( inWei.keySet() );
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
					Object[] randArray = new Object[2];
					randArray[0] = authorInterests.get( i );
					randArray[1] = interestList;
					listObjects.add( randArray );
				}

			}

			visMap.put( "list", listObjects );
		}
		if ( type.equals( "conference" ) )
		{
			long startTime = System.currentTimeMillis();

			List<String> conferenceInterests = new ArrayList<String>();
			List<Double> conferenceInterestWeights = new ArrayList<Double>();
			List<Map<String, Double>> conferenceInterestList = new ArrayList<Map<String, Double>>();
			for ( String id : idsList )
			{
				EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( id );
				Map<String, List<String>> yearWiseInterests = new HashMap<String, List<String>>();

				Map<String, Double> interestWeightMap = new HashMap<String, Double>();
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
								String interest = ( interestTerm.next().getTerm() );
								Double weight = interestTermWeight.next();

								if ( allTopics.contains( interest ) )
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
										List<List<String>> yWIVal = new ArrayList<List<String>>( yearWiseInterests.values() );
										if ( yWI.contains( year ) )
										{
											int index = yWI.indexOf( year );
											if ( !yWIVal.get( index ).contains( interest ) )
											{
												yWIVal.get( index ).add( interest );
												if ( !conferenceInterests.contains( interest ) )
												{
													conferenceInterests.add( interest );
													conferenceInterestWeights.add( weight );
												}
												else
												{
													int ind = conferenceInterests.indexOf( interest );
													conferenceInterestWeights.set( ind, conferenceInterestWeights.get( ind ) + weight );
												}
												if ( interestWeightMap.containsKey( interest ) )
												{
													double w = interestWeightMap.get( interest );
													interestWeightMap.remove( interest );
													interestWeightMap.put( interest, w + 1.0 );
												}
												else
													interestWeightMap.put( interest, 1.0 );
											}
											else
											{
												int ind = conferenceInterests.indexOf( interest );
												conferenceInterestWeights.set( ind, conferenceInterestWeights.get( ind ) + weight );
											}
										}
										else
										{
											List<String> newInterestList = new ArrayList<String>();
											newInterestList.add( interest );
											if ( !conferenceInterests.contains( interest ) )
											{
												conferenceInterests.add( interest );
												conferenceInterestWeights.add( weight );
											}
											else
											{
												int ind = conferenceInterests.indexOf( interest );
												conferenceInterestWeights.set( ind, conferenceInterestWeights.get( ind ) + weight );
											}
											yearWiseInterests.put( year, newInterestList );
											if ( interestWeightMap.containsKey( interest ) )
											{
												double w = interestWeightMap.get( interest );
												interestWeightMap.remove( interest );
												interestWeightMap.put( interest, w + 1.0 );
											}
											else
												interestWeightMap.put( interest, 1.0 );
										}

									}
								}

							}
						}
					}
				}

				conferenceInterestList.add( interestWeightMap );
			}

			Map<String, Object> finalMap = new HashMap<String, Object>();

			double threshold = 1.0;
			if ( idsList.size() > 1 )
			{
				threshold = 3.0;
			}

			List<Object[]> listObjects = new ArrayList<Object[]>();
			for ( int i = 0; i < conferenceInterests.size(); i++ )
			{
				if ( conferenceInterestWeights.get( i ) > threshold )
				{
					List<Double> interestList = new ArrayList<Double>();

					for ( int j = 0; j < conferenceInterestList.size(); j++ )
					{
						Map<String, Double> inWei = conferenceInterestList.get( j );
						List<String> in = new ArrayList<String>( inWei.keySet() );
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
					Object[] randArray = new Object[2];
					randArray[0] = conferenceInterests.get( i );
					randArray[1] = interestList;
					listObjects.add( randArray );
				}

			}

			visMap.put( "list", listObjects );

			// System.out.println( "time lapse: " + ( System.currentTimeMillis()
			// - startTime ) / 1000 );
		}
		return visMap;
	}

	public Map<String, Object> visualizeList( String type, String visType, List<Author> authorList, Set<Publication> publications, String startYear, String endYear, List<String> idsList, List<String> filteredTopic )
	{
		// System.out.println( "visType: " + visType );
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();
		if ( visType.equals( "researchers" ) )
			visMap.putAll( researcherFeature.getResearcherCoauthor().getResearcherCoAuthorMapByPublication( authorList, publications ) );
		if ( visType.equals( "conferences" ) )
		{
			if ( type.equals( "researcher" ) )
				visMap.putAll( researcherFeature.getResearcherAcademicEventTree().getResearcherAllAcademicEvents( publications, false ) );

			if ( type.equals( "conference" ) )
			{
				visMap.putAll( researcherFeature.getResearcherAcademicEventTree().getResearcherAllAcademicEvents( publications, false ) );
				// {
				// Boolean valid = false;
				// if ( startYear.equals( "" ) || startYear.equals( "0" ) )
				// {
				// valid = true;
				// }
				// List<Object> listEvents = new ArrayList<Object>();
				// for ( int i = 0; i < idsList.size(); i++ )
				// {
				// @SuppressWarnings( "unchecked" )
				// List<Object> innerList = (List<Object>)
				// eventFeature.getEventMining().fetchEventGroupData(
				// idsList.get( i ), null, null ).get( "events" );
				// for ( int j = 0; j < innerList.size(); j++ )
				// {
				// @SuppressWarnings( "unchecked" )
				// Map<String, Object> innerListMap = (Map<String, Object>)
				// innerList.get( j );
				//
				// if ( !startYear.equals( "" ) && !startYear.equals( "0" ) )
				// if ( Integer.parseInt( startYear ) <= Integer.parseInt(
				// innerListMap.get( "year" ).toString() ) && Integer.parseInt(
				// endYear ) >= Integer.parseInt( innerListMap.get( "year"
				// ).toString() ) )
				// valid = true;
				// else
				// valid = false;
				//
				// if ( !filteredTopic.isEmpty() && valid )
				// {
				// Event e = persistenceStrategy.getEventDAO().getById(
				// innerListMap.get( "id" ).toString() );
				//
				// System.out.println( "PUBLICATIONS IN CONFERNECSE: " +
				// publications.size() );
				// Set<EventInterestProfile> eips =
				// e.getEventInterestProfiles();
				// List<String> interestStrings = new ArrayList<String>();
				//
				// for ( EventInterestProfile eip : eips )
				// {
				// Set<EventInterest> eventInterests = eip.getEventInterests();
				// for ( EventInterest ei : eventInterests )
				// {
				// Map<Interest, Double> termWeights = ei.getTermWeights();
				// List<Interest> interests = new ArrayList<Interest>(
				// termWeights.keySet() );
				// // List<Double> weights = new
				// // ArrayList<Double>(termWeights.values());
				// for ( Interest interest : interests )
				// {
				// if ( !interestStrings.contains( interest.getTerm() ) )
				// interestStrings.add( interest.getTerm() );
				// }
				//
				// }
				//
				// }
				// // System.out.println( interestStrings );
				//
				// if ( interestStrings.containsAll( filteredTopic ) )
				// {
				// valid = true;
				// }
				// else
				// valid = false;
				//
				// }
				//
				// if ( valid )
				// listEvents.add( innerList.get( j ) );
				// }
				// }
				// visMap.put( "events", listEvents );
				//
				// }
			}
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
	public Map<String, Object> visualizeGroup( String visType, List<Author> authorList, Set<Publication> publications )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();
		if ( visType.equals( "researchers" ) )
		{
			Map<String, Integer> mapClusterAuthor = clusteringService.clusterAuthors( "xmeans", authorList, publications );
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
						names.add( iterator.next() );
						ids.add( iterator.next() );
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
			Map<String, Integer> mapClusterConference = clusteringService.clusterConferences( "xmeans", authorList, publications );
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
			Map<String, Integer> mapClusterPublication = clusteringService.clusterPublications( "xmeans", publications );
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
					publicationsList.add( responseMapTemp );
				}
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
					List<List<Map<String, Object>>> listItems = new ArrayList<List<Map<String, Object>>>();
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
									List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
									Map<String, Object> name = new HashMap<String, Object>();
									Map<String, Object> id = new HashMap<String, Object>();
									Map<String, Object> isAdded = new HashMap<String, Object>();
									isAdded.put( "isAdded", a.isAdded() );
									publicationAuthors.add( a );
									name.put( "name", a.getName() );
									id.put( "id", a.getId() );
									items.add( name );
									items.add( id );
									items.add( isAdded );
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
							List<List<Map<String, Object>>> tempListItems = new ArrayList<List<Map<String, Object>>>();
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
										List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
										Map<String, Object> name = new HashMap<String, Object>();
										Map<String, Object> id = new HashMap<String, Object>();
										Map<String, Object> isAdded = new HashMap<String, Object>();
										isAdded.put( "isAdded", a.isAdded() );
										temp.add( a );
										name.put( "name", a.getName() );
										id.put( "id", a.getId() );
										items.add( name );
										items.add( id );
										items.add( isAdded );
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
					List<List<Map<String, Object>>> combinedlistItems = new ArrayList<List<Map<String, Object>>>();

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

								List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
								Map<String, Object> name = new HashMap<String, Object>();
								Map<String, Object> id = new HashMap<String, Object>();
								Map<String, Object> isAdded = new HashMap<String, Object>();
								isAdded.put( "isAdded", allCoAuthors.get( k ).isAdded() );
								allAuthors.add( allCoAuthors.get( k ) );
								name.put( "name", allCoAuthors.get( k ).getName() );
								id.put( "id", allCoAuthors.get( k ).getId() );
								items.add( name );
								items.add( id );
								items.add( isAdded );
								combinedlistItems.add( items );

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
							combinedlistItems.remove( i );
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
					mapValuesForAll.put( "list", combinedlistItems );

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
					// System.out.println( "size of pubs: " +
					// eventGroupPubs.size() );
					// Set<Publication> authorPublications = authorList.get( i
					// ).getPublications();
					List<Author> publicationAuthors = new ArrayList<Author>();
					List<List<Map<String, Object>>> listItems = new ArrayList<List<Map<String, Object>>>();
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

									List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
									Map<String, Object> name = new HashMap<String, Object>();
									Map<String, Object> id = new HashMap<String, Object>();
									Map<String, Object> isAdded = new HashMap<String, Object>();
									isAdded.put( "isAdded", a.isAdded() );
									publicationAuthors.add( a );
									name.put( "name", a.getName() );
									id.put( "id", a.getId() );
									items.add( name );
									items.add( id );
									items.add( isAdded );
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
							List<List<Map<String, Object>>> tempListItems = new ArrayList<List<Map<String, Object>>>();

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

										List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
										Map<String, Object> name = new HashMap<String, Object>();
										Map<String, Object> id = new HashMap<String, Object>();
										Map<String, Object> isAdded = new HashMap<String, Object>();
										isAdded.put( "isAdded", a.isAdded() );
										temp.add( a );
										name.put( "name", a.getName() );
										id.put( "id", a.getId() );
										items.add( name );
										items.add( id );
										items.add( isAdded );
										tempListItems.add( items );
									}
								}
								Map<String, Object> mapValuesForPairs = new LinkedHashMap<String, Object>();
								List<Integer> sets = new ArrayList<Integer>();
								sets.add( i );
								// System.out.println( "index: " +
								// eventGroupTempList.indexOf(
								// previousEventGroup ) );

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
					List<List<Map<String, Object>>> combinedlistItems = new ArrayList<List<Map<String, Object>>>();
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
								List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
								Map<String, Object> name = new HashMap<String, Object>();
								Map<String, Object> id = new HashMap<String, Object>();
								Map<String, Object> isAdded = new HashMap<String, Object>();
								isAdded.put( "isAdded", allCoAuthors.get( k ).isAdded() );
								allAuthors.add( allCoAuthors.get( k ) );
								name.put( "name", allCoAuthors.get( k ).getName() );
								id.put( "id", allCoAuthors.get( k ).getId() );
								items.add( name );
								items.add( id );
								items.add( isAdded );
								combinedlistItems.add( items );

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
							combinedlistItems.remove( i );
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
					mapValuesForAll.put( "list", combinedlistItems );

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
			// List<List<Author>> combinedListCommonAuthors = new
			// ArrayList<List<Author>>();
			// List<List<Integer>> combinedListIndexes = new
			// ArrayList<List<Integer>>();

			for ( int i = 0; i < authorList.size(); i++ )
			{
				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );

				Set<Publication> authorPublications = authorList.get( i ).getPublications();
				List<EventGroup> authorEvents = new ArrayList<EventGroup>();
				List<List<Map<String, Object>>> listItems = new ArrayList<List<Map<String, Object>>>();
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
									List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
									Map<String, Object> name = new HashMap<String, Object>();
									Map<String, Object> id = new HashMap<String, Object>();
									Map<String, Object> isAdded = new HashMap<String, Object>();
									isAdded.put( "isAdded", eventGroup.isAdded() );
									authorEvents.add( eventGroup );
									name.put( "name", eventGroup.getName() );
									id.put( "id", eventGroup.getId() );
									items.add( name );
									items.add( id );
									items.add( isAdded );
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
						List<List<Map<String, Object>>> tempListItems = new ArrayList<List<Map<String, Object>>>();
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
									List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
									Map<String, Object> name = new HashMap<String, Object>();
									Map<String, Object> id = new HashMap<String, Object>();
									Map<String, Object> isAdded = new HashMap<String, Object>();
									isAdded.put( "isAdded", eg.isAdded() );
									temp.add( eg );
									name.put( "name", eg.getName() );
									id.put( "id", eg.getId() );
									items.add( name );
									items.add( id );
									items.add( isAdded );
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
				List<List<Map<String, Object>>> combinedlistItems = new ArrayList<List<Map<String, Object>>>();

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
							List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
							Map<String, Object> name = new HashMap<String, Object>();
							Map<String, Object> id = new HashMap<String, Object>();
							Map<String, Object> isAdded = new HashMap<String, Object>();
							isAdded.put( "isAdded", authorEventGroups.get( k ).isAdded() );
							allEventGroups.add( authorEventGroups.get( k ) );
							name.put( "name", authorEventGroups.get( k ).getName() );
							id.put( "id", authorEventGroups.get( k ).getId() );
							items.add( name );
							items.add( id );
							items.add( isAdded );
							combinedlistItems.add( items );

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
						combinedlistItems.remove( i );
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
				mapValuesForAll.put( "list", combinedlistItems );

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
			// List<List<Author>> combinedListCommonAuthors = new
			// ArrayList<List<Author>>();
			// List<List<Integer>> combinedListIndexes = new
			// ArrayList<List<Integer>>();

			for ( int i = 0; i < authorList.size(); i++ )
			{
				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );

				Set<Publication> authorPublications = authorList.get( i ).getPublications();
				List<Publication> allPublications = new ArrayList<Publication>();
				List<List<Map<String, Object>>> listItems = new ArrayList<List<Map<String, Object>>>();

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
							List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
							Map<String, Object> name = new HashMap<String, Object>();
							Map<String, Object> id = new HashMap<String, Object>();

							allPublications.add( p );
							name.put( "name", p.getTitle() );
							id.put( "id", p.getId() );
							items.add( name );
							items.add( id );
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
						List<List<Map<String, Object>>> tempListItems = new ArrayList<List<Map<String, Object>>>();
						String label = "";

						if ( !previousAuthor.equals( authorList.get( i ) ) )
						{
							List<Publication> temp = new ArrayList<Publication>();

							// find common publications
							for ( Publication p : previousAuthorPublications )
							{
								if ( allPublications.contains( p ) )
								{
									List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
									Map<String, Object> name = new HashMap<String, Object>();
									Map<String, Object> id = new HashMap<String, Object>();

									temp.add( p );
									name.put( "name", p.getTitle() );
									id.put( "id", p.getId() );
									items.add( name );
									items.add( id );
									tempListItems.add( items );

									temp.add( p );
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
				List<List<Map<String, Object>>> combinedlistItems = new ArrayList<List<Map<String, Object>>>();

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
								List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
								Map<String, Object> name = new HashMap<String, Object>();
								Map<String, Object> id = new HashMap<String, Object>();
								allPublications.add( authorPublications.get( k ) );
								name.put( "name", authorPublications.get( k ).getTitle() );
								id.put( "id", authorPublications.get( k ).getId() );
								items.add( name );
								items.add( id );
								combinedlistItems.add( items );

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
						combinedlistItems.remove( i );
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
				mapValuesForAll.put( "list", combinedlistItems );

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

					System.out.println( "all interests size: " + allAuthorInterests.size() );

					Set<Publication> authorPublications = authorList.get( i ).getPublications();
					List<String> interestTopicNames = new ArrayList<String>();
					List<String> interestTopicIds = new ArrayList<String>();
					List<List<Map<String, Object>>> listItems = new ArrayList<List<Map<String, Object>>>();
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
								System.out.println( "pt term string: " + pt.getTermString() );
								System.out.println( "pt id" + pt.getId() );
								Map<String, Double> termValues = pt.getTermValues();
								List<String> terms = new ArrayList<String>( termValues.keySet() );
								// List<Double> weights = new ArrayList<Double>(
								// termValues.values() );
								for ( int k = 0; k < terms.size(); k++ )
								{
									if ( !interestTopicNames.contains( terms.get( k ) ) && allAuthorInterests.contains( terms.get( k ) ) )// &&
									{
										interestTopicNames.add( terms.get( k ) );
										int pos = allAuthorInterests.indexOf( terms.get( k ) );
										interestTopicIds.add( allAuthorInterestIds.get( pos ) );

										List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
										Map<String, Object> name = new HashMap<String, Object>();
										Map<String, Object> id = new HashMap<String, Object>();

										name.put( "name", terms.get( k ) );
										id.put( "id", allAuthorInterestIds.get( pos ) );
										items.add( name );
										items.add( id );
										listItems.add( items );

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
							List<List<Map<String, Object>>> tempListItems = new ArrayList<List<Map<String, Object>>>();
							String label = "";

							if ( !previousAuthor.equals( authorList.get( i ) ) )
							{
								// List<PublicationTopic> temp = new
								// ArrayList<PublicationTopic>();
								List<String> tempNames = new ArrayList<String>();

								// find common topics
								for ( String pat : previousAuthorTopics )
								{
									if ( interestTopicNames.contains( pat ) )
									{
										tempNames.add( pat );
										int pos = interestTopicNames.indexOf( pat );

										List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
										Map<String, Object> name = new HashMap<String, Object>();
										Map<String, Object> id = new HashMap<String, Object>();

										name.put( "name", pat );
										id.put( "id", interestTopicIds.get( pos ) );
										items.add( name );
										items.add( id );
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
					List<List<Map<String, Object>>> combinedlistItems = new ArrayList<List<Map<String, Object>>>();

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

						System.out.println( "all interests size: " + allAuthorInterests.size() );

						Set<Publication> authorPublications = authorList.get( i ).getPublications();
						List<String> interestTopicNames = new ArrayList<String>();
						List<String> interestTopicIds = new ArrayList<String>();
						List<List<Map<String, Object>>> listItems = new ArrayList<List<Map<String, Object>>>();
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
									System.out.println( "pt term string: " + pt.getTermString() );
									System.out.println( "pt id" + pt.getId() );
									Map<String, Double> termValues = pt.getTermValues();
									List<String> terms = new ArrayList<String>( termValues.keySet() );
									// List<Double> weights = new
									// ArrayList<Double>(
									// termValues.values() );
									for ( int k = 0; k < terms.size(); k++ )
									{
										if ( !interestTopicNames.contains( terms.get( k ) ) && allAuthorInterests.contains( terms.get( k ) ) )// &&
										{
											interestTopicNames.add( terms.get( k ) );
											int pos = allAuthorInterests.indexOf( terms.get( k ) );
											interestTopicIds.add( allAuthorInterestIds.get( pos ) );

											List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
											Map<String, Object> name = new HashMap<String, Object>();
											Map<String, Object> id = new HashMap<String, Object>();

											name.put( "name", terms.get( k ) );
											id.put( "id", allAuthorInterestIds.get( pos ) );
											items.add( name );
											items.add( id );
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
								List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
								Map<String, Object> name = new HashMap<String, Object>();
								Map<String, Object> id = new HashMap<String, Object>();
								allInterests.add( interestTopicNames.get( k ) );
								name.put( "name", interestTopicNames.get( k ) );
								id.put( "id", interestTopicIds.get( k ) );
								items.add( name );
								items.add( id );
								combinedlistItems.add( items );

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
							combinedlistItems.remove( i );
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
					mapValuesForAll.put( "list", combinedlistItems );

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
					List<List<Map<String, Object>>> listItems = new ArrayList<List<Map<String, Object>>>();

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
									if ( !interestTopicNames.contains( terms.get( k ) ) && allConferenceInterests.contains( terms.get( k ) ) && weights.get( k ) > 0.5 )
									{
										interestTopicNames.add( terms.get( k ) );
										int pos = allConferenceInterests.indexOf( terms.get( k ) );
										interestTopicIds.add( allConferenceInterestIds.get( pos ) );

										List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
										Map<String, Object> name = new HashMap<String, Object>();
										Map<String, Object> id = new HashMap<String, Object>();

										name.put( "name", terms.get( k ) );
										id.put( "id", allConferenceInterestIds.get( pos ) );
										items.add( name );
										items.add( id );
										listItems.add( items );
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

										List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
										Map<String, Object> name = new HashMap<String, Object>();
										Map<String, Object> id = new HashMap<String, Object>();

										name.put( "name", pat );
										id.put( "id", interestTopicIds.get( pos ) );
										items.add( name );
										items.add( id );
										tempListItems.add( items );
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
					List<List<Map<String, Object>>> combinedlistItems = new ArrayList<List<Map<String, Object>>>();

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
						List<List<Map<String, Object>>> listItems = new ArrayList<List<Map<String, Object>>>();

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
										if ( !interestTopicNames.contains( terms.get( k ) ) && allConferenceInterests.contains( terms.get( k ) ) && weights.get( k ) > 0.5 )
										{
											interestTopicNames.add( terms.get( k ) );
											int pos = allConferenceInterests.indexOf( terms.get( k ) );
											interestTopicIds.add( allConferenceInterestIds.get( pos ) );

											List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
											Map<String, Object> name = new HashMap<String, Object>();
											Map<String, Object> id = new HashMap<String, Object>();

											name.put( "name", terms.get( k ) );
											id.put( "id", allConferenceInterestIds.get( pos ) );
											items.add( name );
											items.add( id );
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
								List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
								Map<String, Object> name = new HashMap<String, Object>();
								Map<String, Object> id = new HashMap<String, Object>();
								allInterests.add( interestTopicNames.get( k ) );
								name.put( "name", interestTopicNames.get( k ) );
								id.put( "id", interestTopicIds.get( k ) );
								items.add( name );
								items.add( id );
								combinedlistItems.add( items );

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
							combinedlistItems.remove( i );
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
					mapValuesForAll.put( "list", combinedlistItems );

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

				System.out.println( map.size() + " : maz size" );

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
			Map<DataMiningEventGroup, Double> map = similarityService.similarConferences( idsList );
			if ( map != null )
			{
				// System.out.println( map.toString() );
				List<Double> similarityValues = new ArrayList<Double>( map.values() );
				List<DataMiningEventGroup> similarConferences = new ArrayList<DataMiningEventGroup>( map.keySet() );

				List<Double> truncSimilarityValues = new ArrayList<Double>();
				List<String> conferenceNames = new ArrayList<String>();
				List<String> conferenceIds = new ArrayList<String>();

				int count = 0;
				if ( map.size() > 20 )
					count = 20;
				else
					count = map.size();

				for ( int i = 0; i < count; i++ )
				{
					// System.out.println( similarityValues.get( i ) + " : " +
					// similarConferences.get( i ).getName() );
					truncSimilarityValues.add( similarityValues.get( i ) );
					conferenceNames.add( similarConferences.get( i ).getName() );
					conferenceIds.add( similarConferences.get( i ).getId() );
				}

				visMap.put( "authorNames", conferenceNames );
				visMap.put( "authorIds", conferenceIds );
				visMap.put( "similarity", truncSimilarityValues );
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

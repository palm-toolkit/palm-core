package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorInterest;
import de.rwth.i9.palm.model.AuthorInterestProfile;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.EventInterest;
import de.rwth.i9.palm.model.EventInterestProfile;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationTopic;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class EvolutionVisualizationImpl implements EvolutionVisualization
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	public Map<String, Object> visualizeEvolution( String type, List<String> idsList, List<Author> authorList, Set<Publication> publications, String startYear, String endYear )
	{
		// System.out.println( startYear + " : " + endYear );
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
					if ( !allTopics.contains( topics.get( i ) ) )// &&
																	// topicWeights.get(
																	// i ) > 0.2
																	// )
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
								if ( !allTopics.contains( topics.get( i ) ) )// &&
																				// topicWeights.get(
																				// i
																				// )
																				// >
																				// 0.2
																				// )
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

			// List to check if interest belong to all selected authors
			List<List<Author>> interestAuthors = new ArrayList<List<Author>>();
			// System.out.println( " all topics size: " + allTopics.size() );

			for ( Author a : authorList )
			{
				// System.out.println( "\n" + a.getName() );
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
								// System.out.println( "interest: " + interest
								// );
								Boolean validYear = true;
								Calendar calendar = Calendar.getInstance();
								calendar.setTime( ai.getYear() );
								String year = Integer.toString( calendar.get( Calendar.YEAR ) );
								// System.out.println( "year: " + year );
								if ( startYear.equals( "0" ) || startYear.equals( "" ) )
								{
									// System.out.println( "in -4" );
									validYear = true;
								}
								else
								{
									// System.out.println( "in -3" );
									if ( Integer.parseInt( year ) < Integer.parseInt( startYear ) || Integer.parseInt( year ) > Integer.parseInt( endYear ) )
									{
										// System.out.println( "in -2: " + year
										// );
										validYear = false;
									}
								}
								if ( validYear )
								{
									List<String> yWI = new ArrayList<String>( yearWiseInterests.keySet() );
									List<List<Interest>> yWIVal = new ArrayList<List<Interest>>( yearWiseInterests.values() );
									if ( yWI.contains( year ) )
									{
										// System.out.println( "in -1" );
										int index = yWI.indexOf( year );
										if ( !yWIVal.get( index ).contains( actualInterest ) )
										{
											// System.out.println( "in 0" );
											yWIVal.get( index ).add( actualInterest );
											if ( !authorInterests.contains( actualInterest ) )
											{
												// System.out.println( "in 1" );
												authorInterests.add( actualInterest );
												authorInterestWeights.add( weight );
												List<Author> la = new ArrayList<Author>();
												la.add( a );
												interestAuthors.add( la );
											}
											else
											{
												int ind = authorInterests.indexOf( actualInterest );
												// System.out.println( "in 2 :"
												// + authorInterestWeights.get(
												// ind ) );

												authorInterestWeights.set( ind, authorInterestWeights.get( ind ) + weight );

												List<Author> la = interestAuthors.get( ind );
												if ( !la.contains( a ) )
												{
													la.add( a );
													interestAuthors.set( ind, la );
												}
											}
											Map<String, Object> values = new HashMap<String, Object>();
											// System.out.println(
											// actualInterest.getTerm() + " : "
											// + actualInterest.getId() );
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
											// System.out.println( "in 3 : " +
											// authorInterestWeights.get( ind )
											// );

											authorInterestWeights.set( ind, authorInterestWeights.get( ind ) + weight );
											List<Author> la = interestAuthors.get( ind );
											if ( !la.contains( a ) )
											{
												la.add( a );
												interestAuthors.set( ind, la );
											}
										}
									}
									else
									{
										// System.out.println( "in 4" );
										List<Interest> newInterestList = new ArrayList<Interest>();
										newInterestList.add( actualInterest );
										if ( !authorInterests.contains( actualInterest ) )
										{
											// System.out.println( "in 5" );
											authorInterests.add( actualInterest );
											authorInterestWeights.add( weight );
											List<Author> la = new ArrayList<Author>();
											la.add( a );
											interestAuthors.add( la );
										}
										else
										{
											int ind = authorInterests.indexOf( actualInterest );
											// System.out.println( "in 6 : " +
											// authorInterestWeights.get( ind )
											// );

											authorInterestWeights.set( ind, authorInterestWeights.get( ind ) + weight );
											List<Author> la = interestAuthors.get( ind );
											if ( !la.contains( a ) )
											{
												la.add( a );
												interestAuthors.set( ind, la );
											}
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
			// System.out.println( "map list size: " + mapList.size() );
			// double threshold = 3.0;
			// if ( authorList.size() > 1 )
			// {
			// if ( mapList.size() <= 50 )
			// threshold = 0.3;
			// else if ( mapList.size() > 50 || mapList.size() < 100 )
			// threshold = 0.6;
			// else if ( mapList.size() > 100 )
			// threshold = 3.0;
			// else
			// threshold = 1.0;
			// }
			// else
			// {
			// if ( mapList.size() < 50 )
			// threshold = 0.3;
			// }

			double threshold = 0.8;
			// if ( authorList.size() > 1 )
			// {
			// threshold = 0.0;
			// }

			for ( int i = 0; i < authorInterests.size(); i++ )
			{
				// System.out.println( authorInterests.get( i ).getTerm() + "
				// :::: " + authorInterestWeights.get( i ) );
				// System.out.println( interestAuthors.get( i ).size() );
				if ( authorInterestWeights.get( i ) < threshold || interestAuthors.get( i ).size() < authorList.size() )
				{
					authorInterestWeights.remove( i );
					authorInterests.remove( i );
					interestAuthors.remove( i );
					i--;
				}
				// else
				// System.out.println( authorInterests.get( i ) + " :::: " +
				// authorInterestWeights.get( i ) );

			}

			// System.out.println( "size: " + authorInterests.size() );
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

			// System.out.println( "ma lst: " + mapList.size() );

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
									if ( !allTopics.contains( topics.get( i ) ) ) // &&
																					// topicWeights.get(
																					// i
																					// )
																					// >
																					// 0.2
																					// )
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
			// List to check if interest belong to all selected conferences
			List<List<EventGroup>> interestConferences = new ArrayList<List<EventGroup>>();

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
									// System.out.println( interest + " : " +
									// year );
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
													List<EventGroup> le = new ArrayList<EventGroup>();
													le.add( eg );
													interestConferences.add( le );
												}
												else
												{
													int ind = conferenceInterests.indexOf( actualInterest );
													conferenceInterestWeights.set( ind, conferenceInterestWeights.get( ind ) + weight );
													List<EventGroup> le = interestConferences.get( ind );
													if ( !le.contains( eg ) )
													{
														le.add( eg );
														interestConferences.set( ind, le );
													}
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
												List<EventGroup> le = interestConferences.get( ind );
												if ( !le.contains( eg ) )
												{
													le.add( eg );
													interestConferences.set( ind, le );
												}
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
												List<EventGroup> le = new ArrayList<EventGroup>();
												le.add( eg );
												interestConferences.add( le );
											}
											else
											{
												int ind = conferenceInterests.indexOf( actualInterest );
												conferenceInterestWeights.set( ind, conferenceInterestWeights.get( ind ) + weight );
												List<EventGroup> le = interestConferences.get( ind );
												if ( !le.contains( eg ) )
												{
													le.add( eg );
													interestConferences.set( ind, le );
												}
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

			// System.out.println( mapList.size() + " : mapList.size()" );
			// double threshold = 1.5;
			// if ( mapList.size() < 20 )
			// threshold = 0.5;
			// if ( mapList.size() > 100 )
			// threshold = 2.5;
			// if ( mapList.size() > 1000 )
			// threshold = 3.0;
			// if ( idsList.size() > 1 )
			// {
			// threshold = 3.0;
			// }
			double threshold = 0.8;
			// if ( authorList.size() > 1 )
			// {
			// threshold = 0.0;
			// }

			for ( int i = 0; i < conferenceInterests.size(); i++ )
			{
				if ( conferenceInterestWeights.get( i ) < threshold || interestConferences.get( i ).size() < idsList.size() )
				{
					conferenceInterestWeights.remove( i );
					conferenceInterests.remove( i );
					interestConferences.remove( i );
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
}

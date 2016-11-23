package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

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
public class EvolutionVisualizationImpl implements EvolutionVisualization
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private PalmAnalytics palmAnalytics;

	public Map<String, Object> visualizeEvolution( String type, List<String> idsList, Set<Publication> publications, String startYear, String endYear, String yearFilterPresent, HttpServletRequest request )
	{
		// System.out.println( startYear + " : " + endYear );
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();
		Map<String, String> topicIdMap = new HashMap<String, String>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{

			List<String> allTopics = new ArrayList<String>();
			if ( yearFilterPresent.equals( "true" ) )
			{
				for ( Publication pub : publications )
				{
					Set<PublicationTopic> publicationTopics = pub.getPublicationTopics();
					for ( PublicationTopic pubTopic : publicationTopics )
					{
						List<Double> topicWeights = new ArrayList<Double>( pubTopic.getTermValues().values() );
						List<String> topics = new ArrayList<String>( pubTopic.getTermValues().keySet() );
						for ( int i = 0; i < topics.size(); i++ )
						{
							if ( !allTopics.contains( topics.get( i ) ) )
							{
								allTopics.add( topics.get( i ) );
							}
						}
					}
				}
			}

			List<Map<String, Object>> mapList = new ArrayList<Map<String, Object>>();

			if ( type.equals( "researcher" ) )
			{
				List<Interest> authorInterests = new ArrayList<Interest>();
				List<Double> authorInterestWeights = new ArrayList<Double>();

				// List to check if interest belong to all selected authors
				List<List<Author>> interestAuthors = new ArrayList<List<Author>>();

				for ( String id : idsList )
				{
					Author a = persistenceStrategy.getAuthorDAO().getById( id );

					// If there are no common publications!
					if ( yearFilterPresent.equals( "false" ) )
					{
						allTopics = new ArrayList<String>();
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
					}
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
														List<Author> la = new ArrayList<Author>();
														la.add( a );
														interestAuthors.add( la );
													}
													else
													{
														int ind = authorInterests.indexOf( actualInterest );
														authorInterestWeights.set( ind, authorInterestWeights.get( ind ) + weight );

														List<Author> la = interestAuthors.get( ind );
														if ( !la.contains( a ) )
														{
															la.add( a );
															interestAuthors.set( ind, la );
														}
													}
													Map<String, Object> values = new HashMap<String, Object>();
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
												List<Interest> newInterestList = new ArrayList<Interest>();
												newInterestList.add( actualInterest );
												if ( !authorInterests.contains( actualInterest ) )
												{
													authorInterests.add( actualInterest );
													authorInterestWeights.add( weight );
													List<Author> la = new ArrayList<Author>();
													la.add( a );
													interestAuthors.add( la );
												}
												else
												{
													int ind = authorInterests.indexOf( actualInterest );

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
				}

				for ( int i = 0; i < authorInterests.size(); i++ )
				{
					if ( interestAuthors.get( i ).size() < idsList.size() )
					{
						authorInterestWeights.remove( i );
						authorInterests.remove( i );
						interestAuthors.remove( i );
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

				visMap.put( "list", mapList );
				visMap.put( "topicIdMap", topicIdMap );
			}
			if ( type.equals( "conference" ) )
			{

				List<Interest> conferenceInterests = new ArrayList<Interest>();
				List<Double> conferenceInterestWeights = new ArrayList<Double>();
				// List to check if interest belong to all selected conferences
				List<List<EventGroup>> interestConferences = new ArrayList<List<EventGroup>>();

				for ( String id : idsList )
				{
					EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( id );

					// If there are no common publications!
					if ( yearFilterPresent.equals( "false" ) )
					{
						allTopics = new ArrayList<String>();
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
					}

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
									if ( weight > 0.3 )
									{
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
				}

				for ( int i = 0; i < conferenceInterests.size(); i++ )
				{
					if ( interestConferences.get( i ).size() < idsList.size() )
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
			if ( type.equals( "topic" ) )
			{
				System.out.println( "publications after filter: " + publications.size() );
				List<String> filteredPublicationIds = new ArrayList<String>();
				for ( Publication p : publications )
					filteredPublicationIds.add( p.getId() );

				List<DataMiningPublication> allDMPublications = persistenceStrategy.getPublicationDAO().getDataMiningObjects();
				for ( String id : idsList )
				{
					List<DataMiningPublication> publicationsWithTopic = new ArrayList<DataMiningPublication>();
					Interest i = persistenceStrategy.getInterestDAO().getById( id );
					for ( DataMiningPublication dmp : allDMPublications )
					{
						if ( filteredPublicationIds.contains( dmp.getId() ) )
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
										if ( !publicationsWithTopic.contains( dmp ) )
										{
											publicationsWithTopic.add( dmp );
										}
									}
								}
							}
						}
					}

					List<String> years = new ArrayList<String>();
					List<Integer> pubCount = new ArrayList<Integer>();
					for ( DataMiningPublication dmp : publicationsWithTopic )
					{
						Boolean validYear = true;
						String year = dmp.getYear();

						if ( year != null )
						{
							if ( Integer.parseInt( year ) < Integer.parseInt( startYear ) || Integer.parseInt( year ) > Integer.parseInt( endYear ) )
								validYear = false;
							if ( validYear )
							{
								if ( !years.contains( year ) )
								{
									years.add( year );
									pubCount.add( 1 );
								}
								else
								{
									int index = years.indexOf( dmp.getYear() );
									pubCount.set( index, pubCount.get( index ) + 1 );
								}
							}
						}
					}

					for ( int j = Integer.parseInt( startYear ); j <= Integer.parseInt( endYear ); j++ )
					{
						Map<String, Object> map = new HashMap<String, Object>();
						map.put( "Year", j );
						int count = 0;
						int yearIndex = years.indexOf( String.valueOf( j ) );
						if ( yearIndex != -1 )
							count = pubCount.get( yearIndex );
						map.put( "Publication Count", count );
						map.put( "Interest", i.getTerm() );
						mapList.add( map );
					}
				}
				visMap.put( "list", mapList );
			}
			if ( type.equals( "circle" ) )
			{
				List<Interest> circleInterests = new ArrayList<Interest>();
				List<Double> circleInterestWeights = new ArrayList<Double>();

				// List to check if interest belong to all selected authors
				List<List<Circle>> interestCircles = new ArrayList<List<Circle>>();

				for ( String id : idsList )
				{
					Circle c = persistenceStrategy.getCircleDAO().getById( id );

					// If there are no common publications!
					if ( allTopics.size() == 0 && yearFilterPresent.equals( "false" ) )
					{
						allTopics = new ArrayList<String>();
						List<Publication> pubs = new ArrayList<Publication>( c.getPublications() );
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
					Map<String, List<Interest>> yearWiseInterests = new HashMap<String, List<Interest>>();

					Set<CircleInterestProfile> circleInterestProfiles = c.getCircleInterestProfiles();
					for ( CircleInterestProfile aip : circleInterestProfiles )
					{
						Set<CircleInterest> cis = aip.getCircleInterests();
						for ( CircleInterest ci : cis )
						{
							Map<Interest, Double> interests = ci.getTermWeights();
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
											List<String> yWI = new ArrayList<String>( yearWiseInterests.keySet() );
											List<List<Interest>> yWIVal = new ArrayList<List<Interest>>( yearWiseInterests.values() );
											if ( yWI.contains( year ) )
											{
												int index = yWI.indexOf( year );
												if ( !yWIVal.get( index ).contains( actualInterest ) )
												{
													yWIVal.get( index ).add( actualInterest );
													if ( !circleInterests.contains( actualInterest ) )
													{
														circleInterests.add( actualInterest );
														circleInterestWeights.add( weight );
														List<Circle> lc = new ArrayList<Circle>();
														lc.add( c );
														interestCircles.add( lc );
													}
													else
													{
														int ind = circleInterests.indexOf( actualInterest );
														circleInterestWeights.set( ind, circleInterestWeights.get( ind ) + weight );
														List<Circle> lc = interestCircles.get( ind );
														if ( !lc.contains( c ) )
														{
															lc.add( c );
															interestCircles.set( ind, lc );
														}
													}
													Map<String, Object> values = new HashMap<String, Object>();
													values.put( "Author", c.getName() );
													values.put( "Topic", actualInterest.getTerm() );
													values.put( "TopicId", actualInterest.getId() );
													values.put( "Year", year );
													values.put( "Weight", weight );
													mapList.add( values );
													topicIdMap.put( actualInterest.getTerm(), actualInterest.getId() );
												}
												else
												{
													int ind = circleInterests.indexOf( actualInterest );

													circleInterestWeights.set( ind, circleInterestWeights.get( ind ) + weight );
													List<Circle> lc = interestCircles.get( ind );
													if ( !lc.contains( c ) )
													{
														lc.add( c );
														interestCircles.set( ind, lc );
													}
												}
											}
											else
											{
												List<Interest> newInterestList = new ArrayList<Interest>();
												newInterestList.add( actualInterest );
												if ( !circleInterests.contains( actualInterest ) )
												{
													circleInterests.add( actualInterest );
													circleInterestWeights.add( weight );
													List<Circle> lc = new ArrayList<Circle>();
													lc.add( c );
													interestCircles.add( lc );
												}
												else
												{
													int ind = circleInterests.indexOf( actualInterest );

													circleInterestWeights.set( ind, circleInterestWeights.get( ind ) + weight );
													List<Circle> lc = interestCircles.get( ind );
													if ( !lc.contains( c ) )
													{
														lc.add( c );
														interestCircles.set( ind, lc );
													}
												}
												yearWiseInterests.put( year, newInterestList );

												Map<String, Object> values = new HashMap<String, Object>();

												values.put( "Author", c.getName() );
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

				for ( int i = 0; i < circleInterests.size(); i++ )
				{
					if ( interestCircles.get( i ).size() < idsList.size() )
					{
						circleInterestWeights.remove( i );
						circleInterests.remove( i );
						interestCircles.remove( i );
						i--;
					}
				}

				for ( int i = 0; i < mapList.size(); i++ )
				{
					Boolean flag = false;
					for ( Interest interest : circleInterests )
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
				visMap.put( "topicIdMap", topicIdMap );

			}
		}
		return visMap;

	}
}

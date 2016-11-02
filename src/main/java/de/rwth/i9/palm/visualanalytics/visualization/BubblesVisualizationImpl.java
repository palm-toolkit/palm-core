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
public class BubblesVisualizationImpl implements BubblesVisualization
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

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
					if ( !allTopics.contains( topics.get( i ) ) )
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
			List<List<Author>> interestAuthors = new ArrayList<List<Author>>();
			List<Map<Interest, Double>> authorInterestList = new ArrayList<Map<Interest, Double>>();
			for ( Author a : authorList )
			{
				Map<String, List<Interest>> yearWiseInterests = new HashMap<String, List<Interest>>();

				Map<Interest, Double> interestWeightMap = new HashMap<Interest, Double>();
				Set<AuthorInterestProfile> authorInterestProfiles = a.getAuthorInterestProfiles();
				for ( AuthorInterestProfile aip : authorInterestProfiles )
				{
					// System.out.println( aip.getName() );
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
											if ( interestWeightMap.containsKey( actualInterest ) )
											{
												double w = interestWeightMap.get( actualInterest );
												interestWeightMap.remove( actualInterest );
												interestWeightMap.put( actualInterest, w + weight );
											}
											else
												interestWeightMap.put( actualInterest, weight );
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
											if ( interestWeightMap.containsKey( actualInterest ) )
											{
												double w = interestWeightMap.get( actualInterest );
												interestWeightMap.remove( actualInterest );
												interestWeightMap.put( actualInterest, w + weight );
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
										if ( interestWeightMap.containsKey( actualInterest ) )
										{
											double w = interestWeightMap.get( actualInterest );
											interestWeightMap.remove( actualInterest );
											interestWeightMap.put( actualInterest, w + weight );
										}
										else
											interestWeightMap.put( actualInterest, weight );
									}

								}
							}
						}
					}
				}

				authorInterestList.add( interestWeightMap );
			}

			Map<Interest, Object> finalMap = new HashMap<Interest, Object>();

			double threshold = 0.8;
			// if ( authorList.size() > 1 )
			// {
			// threshold = 0.0;
			// }

			List<Object[]> listObjects = new ArrayList<Object[]>();
			for ( int i = 0; i < authorInterests.size(); i++ )
			{
				if ( authorInterestWeights.get( i ) > threshold && interestAuthors.get( i ).size() == authorList.size() )
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
					// System.out.println( authorInterests.get( i ).getTerm() +
					// " : " + authorInterests.get( i ).getId() );
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
			}

			List<Interest> conferenceInterests = new ArrayList<Interest>();
			List<Double> conferenceInterestWeights = new ArrayList<Double>();
			List<List<EventGroup>> interestConferences = new ArrayList<List<EventGroup>>();
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
									// System.out.println( "\n " + interest );
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
												if ( interestWeightMap.containsKey( actualInterest ) )
												{
													Double w = interestWeightMap.get( actualInterest );
													interestWeightMap.remove( actualInterest );
													interestWeightMap.put( actualInterest, w + weight );
												}
												else
												{
													interestWeightMap.put( actualInterest, weight );
												}
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
												if ( interestWeightMap.containsKey( actualInterest ) )
												{
													Double w = interestWeightMap.get( actualInterest );
													interestWeightMap.remove( actualInterest );
													interestWeightMap.put( actualInterest, w + weight );
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
											if ( interestWeightMap.containsKey( actualInterest ) )
											{
												Double w = interestWeightMap.get( actualInterest );
												interestWeightMap.remove( actualInterest );
												interestWeightMap.put( actualInterest, w + weight );
											}
											else
												interestWeightMap.put( actualInterest, weight );
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

			double threshold = 0.8;
			// if ( idsList.size() > 1 )
			// {
			// threshold = 0.0;
			// }

			List<Object[]> listObjects = new ArrayList<Object[]>();
			// System.out.println( "\n in BUBBLES" );
			for ( int i = 0; i < conferenceInterests.size(); i++ )
			{
				// System.out.println( "interest: " + conferenceInterests.get( i
				// ).getTerm() + " weight: " + conferenceInterestWeights.get( i
				// ) );
				Double count = 0.0;
				if ( conferenceInterestWeights.get( i ) > threshold && interestConferences.get( i ).size() == idsList.size() )
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
							count = count + wei.get( index );
						}
						else
							interestList.add( 0.0 );
					}
					// System.out.println( count + "\n" );
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
			List<List<Publication>> interestPublications = new ArrayList<List<Publication>>();
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
											List<Publication> lp = new ArrayList<Publication>();
											lp.add( p );
											interestPublications.add( lp );
										}
										else
										{
											int ind = publicationTopics.indexOf( terms.get( i ) );
											publicationTopicWeights.set( ind, publicationTopicWeights.get( ind ) + weights.get( i ) );
											List<Publication> lp = interestPublications.get( ind );
											if ( !lp.contains( p ) )
											{
												lp.add( p );
												interestPublications.set( ind, lp );
											}
										}
										if ( topicWeightMap.containsKey( terms.get( i ) ) )
										{
											double w = topicWeightMap.get( terms.get( i ) );
											topicWeightMap.remove( terms.get( i ) );
											topicWeightMap.put( terms.get( i ), w + weights.get( i ) );
										}
										else
											topicWeightMap.put( terms.get( i ), weights.get( i ) );
									}
									else
									{
										int ind = publicationTopics.indexOf( terms.get( i ) );
										publicationTopicWeights.set( ind, publicationTopicWeights.get( ind ) + weights.get( i ) );
										List<Publication> lp = interestPublications.get( ind );
										if ( !lp.contains( p ) )
										{
											lp.add( p );
											interestPublications.set( ind, lp );
										}
										if ( topicWeightMap.containsKey( terms.get( i ) ) )
										{
											Double w = topicWeightMap.get( terms.get( i ) );
											topicWeightMap.remove( terms.get( i ) );
											topicWeightMap.put( terms.get( i ), w + weights.get( i ) );
										}
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
										List<Publication> lp = new ArrayList<Publication>();
										lp.add( p );
										interestPublications.add( lp );
									}
									else
									{
										int ind = publicationTopics.indexOf( terms.get( i ) );
										publicationTopicWeights.set( ind, publicationTopicWeights.get( ind ) + weights.get( i ) );
										List<Publication> lp = interestPublications.get( ind );
										if ( !lp.contains( p ) )
										{
											lp.add( p );
											interestPublications.set( ind, lp );
										}
									}
									yearWiseInterests.put( year, newInterestList );
									if ( topicWeightMap.containsKey( terms.get( i ) ) )
									{
										double w = topicWeightMap.get( terms.get( i ) );
										topicWeightMap.remove( terms.get( i ) );
										topicWeightMap.put( terms.get( i ), w + weights.get( i ) );
									}
									else
										topicWeightMap.put( terms.get( i ), weights.get( i ) );
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
				if ( publicationTopicWeights.get( i ) > threshold && interestPublications.get( i ).size() == idsList.size() )
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

					// System.out.println( publicationTopics.get( i ) );

					Interest interest = persistenceStrategy.getInterestDAO().getInterestByTerm( publicationTopics.get( i ) );
					if ( interest != null )
					{
						// System.out.println( "1" );
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
							// System.out.println( "2" );
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

}
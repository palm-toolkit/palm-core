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
import de.rwth.i9.palm.visualanalytics.service.FilterFeature;

@Component
public class ComparisonVisualizationÌmpl implements ComparisonVisualization
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private PalmAnalytics palmAnalytics;

	@Autowired
	private FilterFeature filterFeature;

	@Override
	public Map<String, Object> visualizeResearchersComparison( String type, List<String> idsList, Set<Publication> publications, String startYear, String endYear, String yearFilterPresent )
	{
		List<Map<String, Object>> listOfMaps = new ArrayList<Map<String, Object>>();

		// object type
		if ( type.equals( "researcher" ) )
		{
			Map<Author, List<Author>> mapAuthors = new HashMap<Author, List<Author>>();

			List<Author> authorList = new ArrayList<Author>();
			for ( String id : idsList )
			{
				authorList.add( persistenceStrategy.getAuthorDAO().getById( id ) );
			}

			for ( int i = 0; i < idsList.size(); i++ )
			{
				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );

				Author author = persistenceStrategy.getAuthorDAO().getById( idsList.get( i ) );
				Set<Publication> authorPublications = author.getPublications();
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
							if ( !publicationAuthors.contains( a ) && !author.equals( a ) )
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
				mapAuthors.put( author, publicationAuthors );

				// single values to venn diagram
				mapValues.put( "sets", index );
				mapValues.put( "label", author.getName() );
				mapValues.put( "size", publicationAuthors.size() );
				mapValues.put( "altLabel", author.getName() );
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

						if ( !previousAuthor.equals( author ) )
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
							label = label + author.getFirstName() + "-" + previousAuthor.getFirstName();
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
			if ( idsList.size() > 2 )
			{
				List<Author> allAuthors = new ArrayList<Author>();
				List<Integer> count = new ArrayList<Integer>();
				List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();

				for ( int i = 0; i < idsList.size(); i++ )
				{
					Author author = persistenceStrategy.getAuthorDAO().getById( idsList.get( i ) );
					List<Author> allCoAuthors = new ArrayList<Author>();
					Set<Publication> pubs = author.getPublications();
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
								// if ( !authorList.contains( coAuthors.get( j )
								// ) )
								// {
								if ( !allCoAuthors.contains( coAuthors.get( j ) ) )
								{
									allCoAuthors.add( coAuthors.get( j ) );
								}
								// }
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

						// for ( Author co : previousAuthorCoAuthors )
						// System.out.println( co.getName() );
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
							// if ( !authorList.contains( coAuthors.get( j ) ) )
							// {
							if ( !allCoAuthors.contains( coAuthors.get( j ) ) )
							{
								allCoAuthors.add( coAuthors.get( j ) );
							}
							// }
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
		if ( type.equals( "topic" ) )
		{
			Map<Interest, List<Author>> mapAuthors = new HashMap<Interest, List<Author>>();

			List<DataMiningPublication> allDMPublications = persistenceStrategy.getPublicationDAO().getDataMiningObjects();

			List<DataMiningAuthor> DMAuthors = persistenceStrategy.getAuthorDAO().getDataMiningObjects();

			List<Interest> interestTempList = new ArrayList<Interest>();

			for ( int i = 0; i < idsList.size(); i++ )
			{

				Interest interest = persistenceStrategy.getInterestDAO().getById( idsList.get( i ) );
				interestTempList.add( interest );

				List<DataMiningPublication> selectedDMPublications = new ArrayList<DataMiningPublication>();
				List<Publication> selectedPublications = new ArrayList<Publication>();
				List<Author> publicationAuthors = new ArrayList<Author>();

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
										else
											System.out.println( p.getTitle() );
									}
									if ( flag )
									{
										selectedPublications.add( p );
									}
								}
							}
						}
					}
				}
				for ( Publication p : selectedPublications )
				{
					for ( Author a : p.getAuthors() )
					{
						if ( !publicationAuthors.contains( a ) )
							publicationAuthors.add( a );
					}
				}

				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );

				List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
				List<Author> interestAuthors = new ArrayList<Author>();
				for ( DataMiningAuthor dma : DMAuthors )
				{
					Map<String, Double> interests = new HashMap<String, Double>();
					interests = InterestParser.parseInterestString( dma.getAuthor_interest_flat().getInterests() );
					if ( interests.keySet().contains( interest.getTerm() ) )
					{
						Author a = persistenceStrategy.getAuthorDAO().getById( dma.getId() );
						if ( publicationAuthors.contains( a ) )
						{
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
				}

				mapAuthors.put( interest, interestAuthors );

				// single values to venn diagram
				mapValues.put( "sets", index );
				mapValues.put( "label", interest.getTerm() );
				mapValues.put( "size", interestAuthors.size() );
				mapValues.put( "altLabel", idsList.get( i ) );
				mapValues.put( "list", listItems );

				listOfMaps.add( mapValues );

				if ( mapAuthors.size() > 1 )
				{
					// publications of authors added before current author
					for ( int k = 0; k < mapAuthors.size(); k++ )
					{
						List<Interest> previousInterests = new ArrayList<Interest>( mapAuthors.keySet() );
						List<List<Author>> previousAuthorLists = new ArrayList<List<Author>>( mapAuthors.values() );
						List<Author> previousAuthorCoAuthors = previousAuthorLists.get( k );
						Interest previousInterest = previousInterests.get( k );
						List<Map<String, Object>> tempListItems = new ArrayList<Map<String, Object>>();

						String label = "";

						if ( !previousInterest.equals( interest ) )
						{
							List<Author> temp = new ArrayList<Author>();

							// find common authors
							for ( Author a : previousAuthorCoAuthors )
							{
								if ( interestAuthors.contains( a ) )
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

							label = label + idsList.get( i ) + "-" + previousInterest.getTerm();
							sets.add( interestTempList.indexOf( previousInterest ) ); // to-do
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
				List<Author> allAuthors = new ArrayList<Author>();
				List<Integer> count = new ArrayList<Integer>();
				List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();
				for ( int i = 0; i < idsList.size(); i++ )
				{
					List<Author> allCoAuthors = new ArrayList<Author>();
					List<Author> publicationAuthors = new ArrayList<Author>();

					Interest interest = persistenceStrategy.getInterestDAO().getById( idsList.get( i ) );

					List<DataMiningPublication> selectedDMPublications = new ArrayList<DataMiningPublication>();
					List<Publication> selectedPublications = new ArrayList<Publication>();

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
											else
												System.out.println( p.getTitle() );
										}
										if ( flag )
										{
											selectedPublications.add( p );
										}
									}
								}
							}
						}
					}
					for ( Publication p : selectedPublications )
					{
						for ( Author a : p.getAuthors() )
						{
							if ( !publicationAuthors.contains( a ) )
								publicationAuthors.add( a );
						}
					}

					for ( DataMiningAuthor dma : DMAuthors )
					{
						Map<String, Double> interests = new HashMap<String, Double>();
						interests = InterestParser.parseInterestString( dma.getAuthor_interest_flat().getInterests() );
						if ( interests.keySet().contains( interest.getTerm() ) )
						{
							Author a = persistenceStrategy.getAuthorDAO().getById( dma.getId() );
							if ( publicationAuthors.contains( a ) )
							{
								Set<AuthorInterestProfile> authorInterestProfiles = a.getAuthorInterestProfiles();
								for ( AuthorInterestProfile aip : authorInterestProfiles )
								{
									Set<AuthorInterest> ais = aip.getAuthorInterests();
									for ( AuthorInterest ai : ais )
									{
										Calendar calendar = Calendar.getInstance();
										calendar.setTime( ai.getYear() );
										if ( !allCoAuthors.contains( a ) )
										{
											allCoAuthors.add( a );
										}
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
							count.add( 1 );
						}
						else
							count.set( allAuthors.indexOf( allCoAuthors.get( k ) ), count.get( allAuthors.indexOf( allCoAuthors.get( k ) ) ) + 1 );
					}

				}
				System.out.println( count.toString() );
				System.out.println( allAuthors.toString() );

				for ( int i = 0; i < allAuthors.size(); i++ )
				{
					if ( count.get( i ) < idsList.size() )
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
		if ( type.equals( "circle" ) )
		{

			Map<Circle, List<Author>> mapAuthors = new HashMap<Circle, List<Author>>();

			List<Circle> circleList = new ArrayList<Circle>();
			for ( String id : idsList )
			{
				circleList.add( persistenceStrategy.getCircleDAO().getById( id ) );
			}

			for ( int i = 0; i < idsList.size(); i++ )
			{
				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );

				Circle circle = persistenceStrategy.getCircleDAO().getById( idsList.get( i ) );
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
				mapAuthors.put( circle, publicationAuthors );

				// single values to venn diagram
				mapValues.put( "sets", index );
				mapValues.put( "label", circle.getName() );
				mapValues.put( "size", publicationAuthors.size() );
				mapValues.put( "altLabel", circle.getName() );
				mapValues.put( "list", listItems );
				listOfMaps.add( mapValues );

				if ( mapAuthors.size() > 1 )
				{
					// publications of authors added before current author
					for ( int k = 0; k < mapAuthors.size(); k++ )
					{
						List<Circle> previousCircles = new ArrayList<Circle>( mapAuthors.keySet() );
						List<List<Author>> previousAuthorLists = new ArrayList<List<Author>>( mapAuthors.values() );
						List<Author> previousAuthorCoAuthors = previousAuthorLists.get( k );
						List<Map<String, Object>> tempListItems = new ArrayList<Map<String, Object>>();
						String label = "";

						Circle previousCircle = previousCircles.get( k );

						if ( !previousCircle.equals( circle ) )
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
							sets.add( circleList.indexOf( previousCircle ) );
							label = label + circle.getName() + "-" + previousCircle.getName();
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
			if ( idsList.size() > 2 )
			{
				List<Author> allAuthors = new ArrayList<Author>();
				List<Integer> count = new ArrayList<Integer>();
				List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();

				for ( int i = 0; i < idsList.size(); i++ )
				{
					Circle circle = persistenceStrategy.getCircleDAO().getById( idsList.get( i ) );
					List<Author> allCoAuthors = new ArrayList<Author>();
					List<Author> circleAuthors = new ArrayList<Author>( circle.getAuthors() );
					Set<Publication> pubs = circle.getPublications();
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
								if ( !allCoAuthors.contains( coAuthors.get( j ) ) && circleAuthors.contains( coAuthors.get( j ) ) )
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
					label = label + circleList.get( i ).getName();
				}

				mapValuesForAll.put( "altLabel", label );
				mapValuesForAll.put( "weight", 1000 );
				listOfMaps.add( mapValuesForAll );
			}

		}

		Map<String, Object> visMap = new HashMap<String, Object>();
		visMap.put( "comparisonList", listOfMaps );
		return visMap;
	}

	@Override
	public Map<String, Object> visualizeConferencesComparison( String type, List<String> idsList, Set<Publication> publications, String startYear, String endYear, String yearFilterPresent )
	{
		List<Map<String, Object>> listOfMaps = new ArrayList<Map<String, Object>>();
		if ( type.equals( "researcher" ) )
		{
			Map<Author, List<EventGroup>> mapConferences = new HashMap<Author, List<EventGroup>>();

			List<Author> authorList = new ArrayList<Author>();
			for ( String id : idsList )
			{
				authorList.add( persistenceStrategy.getAuthorDAO().getById( id ) );
			}

			for ( int i = 0; i < idsList.size(); i++ )
			{
				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );
				Author author = persistenceStrategy.getAuthorDAO().getById( idsList.get( i ) );
				Set<Publication> authorPublications = author.getPublications();
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
				mapConferences.put( author, authorEvents );

				// single values to venn diagram
				mapValues.put( "sets", index );
				mapValues.put( "label", author.getName() );
				mapValues.put( "size", authorEvents.size() );
				mapValues.put( "altLabel", author.getName() );
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
						if ( !previousAuthor.equals( author ) )
						{
							List<EventGroup> temp = new ArrayList<EventGroup>();
							// List<String> tempNames = new ArrayList<String>();

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
							label = label + author.getFirstName() + "-" + previousAuthor.getFirstName();
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
				List<EventGroup> allEventGroups = new ArrayList<EventGroup>();
				List<Integer> count = new ArrayList<Integer>();
				List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();

				for ( int i = 0; i < idsList.size(); i++ )
				{
					Author author = persistenceStrategy.getAuthorDAO().getById( idsList.get( i ) );
					List<EventGroup> authorEventGroups = new ArrayList<EventGroup>();
					Set<Publication> pubs = author.getPublications();
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
					if ( count.get( i ) < idsList.size() - 1 )
					{
						count.remove( i );
						allEventGroups.remove( i );
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
					Author author = persistenceStrategy.getAuthorDAO().getById( idsList.get( i ) );
					label = label + author.getFirstName();
				}

				mapValuesForAll.put( "altLabel", label );
				mapValuesForAll.put( "weight", 1000 );
				listOfMaps.add( mapValuesForAll );
			}
		}
		if ( type.equals( "topic" ) )
		{

			Map<Interest, List<EventGroup>> mapEventGroups = new HashMap<Interest, List<EventGroup>>();

			List<DataMiningPublication> allDMPublications = persistenceStrategy.getPublicationDAO().getDataMiningObjects();

			List<DataMiningEventGroup> DMEventGroups = persistenceStrategy.getEventGroupDAO().getDataMiningObjects();

			List<Interest> interestTempList = new ArrayList<Interest>();

			for ( int i = 0; i < idsList.size(); i++ )
			{

				Interest interest = persistenceStrategy.getInterestDAO().getById( idsList.get( i ) );
				interestTempList.add( interest );

				List<DataMiningPublication> selectedDMPublications = new ArrayList<DataMiningPublication>();
				List<Publication> selectedPublications = new ArrayList<Publication>();
				List<EventGroup> publicationEventGroups = new ArrayList<EventGroup>();

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
										else
											System.out.println( p.getTitle() );
									}
									if ( flag )
									{
										selectedPublications.add( p );
									}
								}
							}
						}
					}
				}
				for ( Publication p : selectedPublications )
				{
					if ( p.getEvent() != null )
					{
						if ( p.getEvent().getEventGroup() != null )
						{
							if ( !publicationEventGroups.contains( p.getEvent().getEventGroup() ) )
							{
								publicationEventGroups.add( p.getEvent().getEventGroup() );
							}
						}
					}
				}

				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );

				List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
				List<EventGroup> interestEventGroups = new ArrayList<EventGroup>();
				for ( DataMiningEventGroup dmeg : DMEventGroups )
				{
					Map<String, Double> interests = new HashMap<String, Double>();
					interests = InterestParser.parseInterestString( dmeg.getEventGroup_interest_flat().getInterests() );
					if ( interests.keySet().contains( interest.getTerm() ) )
					{
						EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( dmeg.getId() );
						if ( publicationEventGroups.contains( eg ) )
						{
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
				}

				mapEventGroups.put( interest, interestEventGroups );

				// single values to venn diagram
				mapValues.put( "sets", index );
				mapValues.put( "label", interest.getTerm() );
				mapValues.put( "size", interestEventGroups.size() );
				mapValues.put( "altLabel", idsList.get( i ) );
				mapValues.put( "list", listItems );

				listOfMaps.add( mapValues );

				if ( mapEventGroups.size() > 1 )
				{
					// publications of authors added before current author
					for ( int k = 0; k < mapEventGroups.size(); k++ )
					{
						List<Interest> previousInterests = new ArrayList<Interest>( mapEventGroups.keySet() );
						List<List<EventGroup>> previousEventGroupLists = new ArrayList<List<EventGroup>>( mapEventGroups.values() );
						List<EventGroup> previousInterestEventGroups = previousEventGroupLists.get( k );
						Interest previousInterest = previousInterests.get( k );
						List<Map<String, Object>> tempListItems = new ArrayList<Map<String, Object>>();

						String label = "";

						if ( !previousInterest.equals( interest ) )
						{
							List<EventGroup> temp = new ArrayList<EventGroup>();

							// find common authors
							for ( EventGroup a : previousInterestEventGroups )
							{
								if ( interestEventGroups.contains( a ) )
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

							label = label + idsList.get( i ) + "-" + previousInterest.getTerm();
							sets.add( interestTempList.indexOf( previousInterest ) ); // to-do
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
				List<EventGroup> allEventGroups = new ArrayList<EventGroup>();
				List<Integer> count = new ArrayList<Integer>();
				List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();
				for ( int i = 0; i < idsList.size(); i++ )
				{
					List<EventGroup> EventGroups = new ArrayList<EventGroup>();
					List<EventGroup> publicationEventGroups = new ArrayList<EventGroup>();

					Interest interest = persistenceStrategy.getInterestDAO().getById( idsList.get( i ) );

					List<DataMiningPublication> selectedDMPublications = new ArrayList<DataMiningPublication>();
					List<Publication> selectedPublications = new ArrayList<Publication>();

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
											else
												System.out.println( p.getTitle() );
										}
										if ( flag )
										{
											selectedPublications.add( p );
										}
									}
								}
							}
						}
					}
					for ( Publication p : selectedPublications )
					{
						if ( p.getEvent() != null )
						{
							if ( p.getEvent().getEventGroup() != null )
							{
								if ( !publicationEventGroups.contains( p.getEvent().getEventGroup() ) )
								{
									publicationEventGroups.add( p.getEvent().getEventGroup() );
								}
							}
						}
					}

					for ( DataMiningEventGroup dmeg : DMEventGroups )
					{
						Map<String, Double> interests = new HashMap<String, Double>();
						interests = InterestParser.parseInterestString( dmeg.getEventGroup_interest_flat().getInterests() );
						if ( interests.keySet().contains( interest.getTerm() ) )
						{
							EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( dmeg.getId() );
							if ( publicationEventGroups.contains( eg ) )
							{
								if ( !EventGroups.contains( eg ) )
								{
									EventGroups.add( eg );
								}
							}
						}
					}

					for ( int k = 0; k < EventGroups.size(); k++ )
					{
						if ( !allEventGroups.contains( EventGroups.get( k ) ) )
						{
							allEventGroups.add( EventGroups.get( k ) );
							Map<String, Object> items = new HashMap<String, Object>();
							items.put( "name", EventGroups.get( k ).getName() );
							items.put( "id", EventGroups.get( k ).getId() );
							items.put( "isAdded", EventGroups.get( k ).isAdded() );
							combinedListItems.add( items );
							count.add( 1 );
						}
						else
							count.set( allEventGroups.indexOf( EventGroups.get( k ) ), count.get( allEventGroups.indexOf( EventGroups.get( k ) ) ) + 1 );
					}

				}
				System.out.println( count.toString() );
				System.out.println( allEventGroups.toString() );

				for ( int i = 0; i < allEventGroups.size(); i++ )
				{
					if ( count.get( i ) < idsList.size() )
					{
						count.remove( i );
						allEventGroups.remove( i );
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
		if ( type.equals( "circle" ) )
		{

			Map<Circle, List<EventGroup>> mapEventGroups = new HashMap<Circle, List<EventGroup>>();

			List<Circle> circleList = new ArrayList<Circle>();
			for ( String id : idsList )
			{
				circleList.add( persistenceStrategy.getCircleDAO().getById( id ) );
			}

			for ( int i = 0; i < idsList.size(); i++ )
			{
				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );

				Circle circle = persistenceStrategy.getCircleDAO().getById( idsList.get( i ) );
				Set<Publication> circlePublications = circle.getPublications();
				// List<EventGroup> circleEventGroups = new ArrayList<Author>(
				// circle.getAuthors() );
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
				mapEventGroups.put( circle, circleEventGroups );

				// single values to venn diagram
				mapValues.put( "sets", index );
				mapValues.put( "label", circle.getName() );
				mapValues.put( "size", circleEventGroups.size() );
				mapValues.put( "altLabel", circle.getName() );
				mapValues.put( "list", listItems );
				listOfMaps.add( mapValues );

				if ( mapEventGroups.size() > 1 )
				{
					// publications of authors added before current author
					for ( int k = 0; k < mapEventGroups.size(); k++ )
					{
						List<Circle> previousCircles = new ArrayList<Circle>( mapEventGroups.keySet() );
						List<List<EventGroup>> previousEventGroupLists = new ArrayList<List<EventGroup>>( mapEventGroups.values() );
						List<EventGroup> previousCircleEventGroups = previousEventGroupLists.get( k );
						List<Map<String, Object>> tempListItems = new ArrayList<Map<String, Object>>();
						String label = "";

						Circle previousCircle = previousCircles.get( k );

						if ( !previousCircle.equals( circle ) )
						{
							List<EventGroup> temp = new ArrayList<EventGroup>();

							// find common authors
							for ( EventGroup eg : previousCircleEventGroups )
							{
								if ( circleEventGroups.contains( eg ) )
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
							sets.add( circleList.indexOf( previousCircle ) );
							label = label + circle.getName() + "-" + previousCircle.getName();
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
			if ( idsList.size() > 2 )
			{
				List<EventGroup> allEventGroups = new ArrayList<EventGroup>();
				List<Integer> count = new ArrayList<Integer>();
				List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();

				for ( int i = 0; i < idsList.size(); i++ )
				{
					Circle circle = persistenceStrategy.getCircleDAO().getById( idsList.get( i ) );
					List<EventGroup> eventGroups = new ArrayList<EventGroup>();
					Set<Publication> pubs = circle.getPublications();
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
									if ( !eventGroups.contains( eg ) )
									{
										eventGroups.add( eg );
									}
								}
							}
						}
					}

					for ( int k = 0; k < eventGroups.size(); k++ )
					{
						if ( !allEventGroups.contains( eventGroups.get( k ) ) )
						{
							allEventGroups.add( eventGroups.get( k ) );
							Map<String, Object> items = new HashMap<String, Object>();
							items.put( "name", eventGroups.get( k ).getName() );
							items.put( "id", eventGroups.get( k ).getId() );
							items.put( "isAdded", eventGroups.get( k ).isAdded() );
							combinedListItems.add( items );
							count.add( 0 );
						}
						else
							count.set( allEventGroups.indexOf( eventGroups.get( k ) ), count.get( allEventGroups.indexOf( eventGroups.get( k ) ) ) + 1 );
					}
				}

				for ( int i = 0; i < allEventGroups.size(); i++ )
				{
					if ( count.get( i ) < idsList.size() - 1 )
					{
						count.remove( i );
						allEventGroups.remove( i );
						combinedListItems.remove( i );
						i--;
					}
				}
				System.out.println( count.toString() );
				System.out.println( allEventGroups.toString() );
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
					label = label + circleList.get( i ).getName();
				}

				mapValuesForAll.put( "altLabel", label );
				mapValuesForAll.put( "weight", 1000 );
				listOfMaps.add( mapValuesForAll );
			}

		}

		Map<String, Object> visMap = new HashMap<String, Object>();
		visMap.put( "comparisonList", listOfMaps );
		return visMap;
	}

	@Override
	public Map<String, Object> visualizePublicationsComparison( String type, List<String> idsList, Set<Publication> publications, String startYear, String endYear, String yearFilterPresent )
	{
		List<Map<String, Object>> listOfMaps = new ArrayList<Map<String, Object>>();

		if ( type.equals( "researcher" ) )
		{
			Map<Author, List<Publication>> mapPublications = new HashMap<Author, List<Publication>>();
			List<Author> authorList = new ArrayList<Author>();
			for ( String id : idsList )
			{
				authorList.add( persistenceStrategy.getAuthorDAO().getById( id ) );
			}

			for ( int i = 0; i < idsList.size(); i++ )
			{
				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );

				Author author = persistenceStrategy.getAuthorDAO().getById( idsList.get( i ) );
				Set<Publication> authorPublications = author.getPublications();
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
				mapPublications.put( author, allPublications );

				// single values to venn diagram
				mapValues.put( "sets", index );
				mapValues.put( "label", author.getName() );
				mapValues.put( "size", allPublications.size() );
				mapValues.put( "altLabel", author.getName() );
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

						if ( !previousAuthor.equals( author ) )
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
							label = label + author.getFirstName() + "-" + previousAuthor.getFirstName();
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
				List<Publication> allPublications = new ArrayList<Publication>();
				List<Integer> count = new ArrayList<Integer>();
				List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();

				for ( int i = 0; i < idsList.size(); i++ )
				{
					Author author = persistenceStrategy.getAuthorDAO().getById( idsList.get( i ) );
					List<Publication> authorPublications = new ArrayList<Publication>( author.getPublications() );

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
					if ( count.get( i ) < idsList.size() - 1 )
					{
						count.remove( i );
						allPublications.remove( i );
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
					Author author = persistenceStrategy.getAuthorDAO().getById( idsList.get( i ) );
					label = label + author.getFirstName();
				}

				mapValuesForAll.put( "altLabel", label );
				mapValuesForAll.put( "weight", 1000 );
				listOfMaps.add( mapValuesForAll );
			}
		}
		if ( type.equals( "topic" ) )
		{
			Map<Interest, List<Publication>> mapPublications = new HashMap<Interest, List<Publication>>();

			List<DataMiningPublication> allDMPublications = persistenceStrategy.getPublicationDAO().getDataMiningObjects();

			// List<DataMiningEventGroup> DMEventGroups =
			// persistenceStrategy.getEventGroupDAO().getDataMiningObjects();

			List<Interest> interestTempList = new ArrayList<Interest>();

			for ( int i = 0; i < idsList.size(); i++ )
			{

				Interest interest = persistenceStrategy.getInterestDAO().getById( idsList.get( i ) );
				interestTempList.add( interest );

				List<DataMiningPublication> selectedDMPublications = new ArrayList<DataMiningPublication>();

				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );

				List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
				List<Publication> interestPublications = new ArrayList<Publication>();

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
										else
											System.out.println( p.getTitle() );
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

				mapPublications.put( interest, interestPublications );

				// single values to venn diagram
				mapValues.put( "sets", index );
				mapValues.put( "label", interest.getTerm() );
				mapValues.put( "size", interestPublications.size() );
				mapValues.put( "altLabel", idsList.get( i ) );
				mapValues.put( "list", listItems );

				listOfMaps.add( mapValues );

				if ( mapPublications.size() > 1 )
				{
					// publications of authors added before current author
					for ( int k = 0; k < mapPublications.size(); k++ )
					{
						List<Interest> previousInterests = new ArrayList<Interest>( mapPublications.keySet() );
						List<List<Publication>> previousPublicationLists = new ArrayList<List<Publication>>( mapPublications.values() );
						List<Publication> previousPublications = previousPublicationLists.get( k );
						Interest previousInterest = previousInterests.get( k );
						List<Map<String, Object>> tempListItems = new ArrayList<Map<String, Object>>();

						String label = "";

						if ( !previousInterest.equals( interest ) )
						{
							List<Publication> temp = new ArrayList<Publication>();

							// find common authors
							for ( Publication p : previousPublications )
							{
								if ( interestPublications.contains( p ) )
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

							label = label + idsList.get( i ) + "-" + previousInterest.getTerm();
							sets.add( interestTempList.indexOf( previousInterest ) ); // to-do
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
				List<Publication> allPublications = new ArrayList<Publication>();
				List<Integer> count = new ArrayList<Integer>();
				List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();
				for ( int i = 0; i < idsList.size(); i++ )
				{
					Interest interest = persistenceStrategy.getInterestDAO().getById( idsList.get( i ) );

					List<DataMiningPublication> selectedDMPublications = new ArrayList<DataMiningPublication>();
					List<Publication> selectedPublications = new ArrayList<Publication>();

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
											else
												System.out.println( p.getTitle() );
										}
										if ( flag )
										{
											selectedPublications.add( p );
										}
									}
								}
							}
						}
					}

					for ( int k = 0; k < selectedPublications.size(); k++ )
					{
						if ( !allPublications.contains( selectedPublications.get( k ) ) )
						{
							allPublications.add( selectedPublications.get( k ) );
							Map<String, Object> items = new HashMap<String, Object>();
							items.put( "name", selectedPublications.get( k ).getTitle() );
							items.put( "id", selectedPublications.get( k ).getId() );
							combinedListItems.add( items );
							count.add( 1 );
						}
						else
							count.set( allPublications.indexOf( selectedPublications.get( k ) ), count.get( allPublications.indexOf( selectedPublications.get( k ) ) ) + 1 );
					}

				}
				System.out.println( count.toString() );
				System.out.println( allPublications.toString() );

				for ( int i = 0; i < allPublications.size(); i++ )
				{
					if ( count.get( i ) < idsList.size() )
					{
						count.remove( i );
						allPublications.remove( i );
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
		if ( type.equals( "circle" ) )
		{

			Map<Circle, List<Publication>> mapPublications = new HashMap<Circle, List<Publication>>();
			List<Circle> circleList = new ArrayList<Circle>();
			for ( String id : idsList )
			{
				circleList.add( persistenceStrategy.getCircleDAO().getById( id ) );
			}

			for ( int i = 0; i < idsList.size(); i++ )
			{
				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );

				Circle circle = persistenceStrategy.getCircleDAO().getById( idsList.get( i ) );
				Set<Publication> authorPublications = circle.getPublications();
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
				mapPublications.put( circle, allPublications );

				// single values to venn diagram
				mapValues.put( "sets", index );
				mapValues.put( "label", circle.getName() );
				mapValues.put( "size", allPublications.size() );
				mapValues.put( "altLabel", circle.getName() );
				mapValues.put( "list", listItems );
				listOfMaps.add( mapValues );

				if ( mapPublications.size() > 1 )
				{
					// publications of authors added before current author
					for ( int k = 0; k < mapPublications.size(); k++ )
					{
						List<Circle> previousCircles = new ArrayList<Circle>( mapPublications.keySet() );
						List<List<Publication>> previousCircleLists = new ArrayList<List<Publication>>( mapPublications.values() );
						List<Publication> previousCirclePublications = previousCircleLists.get( k );
						Circle previousCircle = previousCircles.get( k );
						List<Map<String, Object>> tempListItems = new ArrayList<Map<String, Object>>();
						String label = "";

						if ( !previousCircle.equals( circle ) )
						{
							List<Publication> temp = new ArrayList<Publication>();

							// find common publications
							for ( Publication p : previousCirclePublications )
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
							sets.add( circleList.indexOf( previousCircle ) );
							label = label + circle.getName() + "-" + previousCircle.getName();
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
				List<Publication> allPublications = new ArrayList<Publication>();
				List<Integer> count = new ArrayList<Integer>();
				List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();

				for ( int i = 0; i < idsList.size(); i++ )
				{
					Circle circle = persistenceStrategy.getCircleDAO().getById( idsList.get( i ) );
					List<Publication> circlePublications = new ArrayList<Publication>( circle.getPublications() );

					for ( int k = 0; k < circlePublications.size(); k++ )
					{
						Boolean flag = false;
						if ( startYear.equals( "" ) || startYear.equals( "0" ) || yearFilterPresent.equals( "false" ) )
						{
							flag = true;
						}
						else
						{
							if ( circlePublications.get( k ).getYear() != null )
							{
								if ( ( Integer.parseInt( circlePublications.get( k ).getYear() ) >= Integer.parseInt( startYear ) && Integer.parseInt( circlePublications.get( k ).getYear() ) <= Integer.parseInt( endYear ) ) )
								{
									flag = true;
								}
							}
						}
						if ( flag )
						{

							if ( !allPublications.contains( circlePublications.get( k ) ) )
							{
								allPublications.add( circlePublications.get( k ) );
								Map<String, Object> items = new HashMap<String, Object>();
								items.put( "name", circlePublications.get( k ).getTitle() );
								items.put( "id", circlePublications.get( k ).getId() );
								combinedListItems.add( items );
								count.add( 0 );

							}
							else
								count.set( allPublications.indexOf( circlePublications.get( k ) ), count.get( allPublications.indexOf( circlePublications.get( k ) ) ) + 1 );
						}
					}

				}

				for ( int i = 0; i < allPublications.size(); i++ )
				{
					if ( count.get( i ) < idsList.size() - 1 )
					{
						count.remove( i );
						allPublications.remove( i );
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
					Author author = persistenceStrategy.getAuthorDAO().getById( idsList.get( i ) );
					label = label + author.getFirstName();
				}

				mapValuesForAll.put( "altLabel", label );
				mapValuesForAll.put( "weight", 1000 );
				listOfMaps.add( mapValuesForAll );
			}

		}
		Map<String, Object> visMap = new HashMap<String, Object>();
		visMap.put( "comparisonList", listOfMaps );
		return visMap;
	}

	@Override
	public Map<String, Object> visualizeTopicsComparison( String type, List<String> idsList, Set<Publication> publications, String startYear, String endYear, String yearFilterPresent )
	{
		List<Map<String, Object>> listOfMaps = new ArrayList<Map<String, Object>>();

		if ( type.equals( "researcher" ) )
		{
			Map<Author, List<String>> mapTopics = new HashMap<Author, List<String>>();
			List<Author> authorList = new ArrayList<Author>();
			for ( String id : idsList )
			{
				authorList.add( persistenceStrategy.getAuthorDAO().getById( id ) );
			}
			for ( int i = 0; i < idsList.size(); i++ )
			{
				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );

				Author author = persistenceStrategy.getAuthorDAO().getById( idsList.get( i ) );
				List<String> allAuthorInterests = new ArrayList<String>();
				List<String> allAuthorInterestIds = new ArrayList<String>();
				Set<AuthorInterestProfile> authorInterestProfiles = author.getAuthorInterestProfiles();
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

				// System.out.println( "all interests size in VIS: " +
				// allAuthorInterests.size() );

				Set<Publication> authorPublications = author.getPublications();
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
				mapTopics.put( author, interestTopicNames );

				// single values to venn diagram
				mapValues.put( "sets", index );
				mapValues.put( "label", author.getName() );
				mapValues.put( "size", listItems.size() );
				mapValues.put( "altLabel", author.getName() );
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

						if ( !previousAuthor.equals( author ) )
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
							label = label + author.getFirstName() + "-" + previousAuthor.getFirstName();
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
				List<String> allInterests = new ArrayList<String>();
				// List<String> allInterestIds = new ArrayList<String>();
				List<Integer> count = new ArrayList<Integer>();
				List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();

				for ( int i = 0; i < idsList.size(); i++ )
				{
					Author author = persistenceStrategy.getAuthorDAO().getById( idsList.get( i ) );
					List<String> allAuthorInterests = new ArrayList<String>();
					List<String> allAuthorInterestIds = new ArrayList<String>();
					Set<AuthorInterestProfile> authorInterestProfiles = author.getAuthorInterestProfiles();
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

					Set<Publication> authorPublications = author.getPublications();
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
					Author author = persistenceStrategy.getAuthorDAO().getById( idsList.get( i ) );
					label = label + author.getFirstName();
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
						List<Map<String, Object>> tempListItems = new ArrayList<Map<String, Object>>();
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

				System.out.println( "\n" + p.getTitle() );
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
								System.out.println( terms.get( k ) + " in 1 " );
								if ( allPublicationInterests.contains( terms.get( k ) ) )
								{
									// System.out.println( " inside 1 " );
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
									System.out.println( " inside 2 " + terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) );
									interestTopicNames.add( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) );
									int pos = allPublicationInterests.indexOf( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) );
									interestTopicIds.add( allPublicationInterestIds.get( pos ) );

									Map<String, Object> items = new HashMap<String, Object>();
									items.put( "name", terms.get( k ) );
									items.put( "id", allPublicationInterestIds.get( pos ) );
									listItems.add( items );
								}
							}
							// else if ( !interestTopicNames.contains(
							// terms.get( k ).substring( 0, terms.get( k
							// ).length() - 1 ) ) )// &&
							// {
							// System.out.println( terms.get( k ) + " in 2 "
							// );
							//
							// }

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
									tempListItems.add( items );
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
									// System.out.println( terms.get( k ) +
									// " in 1 " );
									if ( allPublicationInterests.contains( terms.get( k ) ) )
									{
										// System.out.println( " inside 1 "
										// );
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
										// System.out.println( " inside 2 "
										// );
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
		if ( type.equals( "circle" ) )
		{
			Map<Circle, List<String>> mapTopics = new HashMap<Circle, List<String>>();
			List<Circle> circleList = new ArrayList<Circle>();
			for ( String id : idsList )
			{
				circleList.add( persistenceStrategy.getCircleDAO().getById( id ) );
			}
			for ( int i = 0; i < idsList.size(); i++ )
			{
				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );

				Circle circle = persistenceStrategy.getCircleDAO().getById( idsList.get( i ) );
				List<String> allCircleInterests = new ArrayList<String>();
				List<String> allCircleInterestIds = new ArrayList<String>();
				Set<CircleInterestProfile> circleInterestProfiles = circle.getCircleInterestProfiles();
				for ( CircleInterestProfile cip : circleInterestProfiles )
				{
					List<CircleInterest> circleInterests = new ArrayList<CircleInterest>( cip.getCircleInterests() );
					for ( CircleInterest ci : circleInterests )
					{
						Map<Interest, Double> termWeights = ci.getTermWeights();
						List<Interest> interests = new ArrayList<Interest>( termWeights.keySet() );
						// List<Double> weights = new ArrayList<Double>(
						// termWeights.values() );
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

				// System.out.println( "all interests size in VIS: " +
				// allAuthorInterests.size() );

				Set<Publication> circlePublications = circle.getPublications();
				List<String> interestTopicNames = new ArrayList<String>();
				List<String> interestTopicIds = new ArrayList<String>();
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
						List<PublicationTopic> topics = new ArrayList<PublicationTopic>( p.getPublicationTopics() );
						for ( PublicationTopic pt : topics )
						{
							Map<String, Double> termValues = pt.getTermValues();
							List<String> terms = new ArrayList<String>( termValues.keySet() );
							for ( int k = 0; k < terms.size(); k++ )
							{
								if ( !interestTopicNames.contains( terms.get( k ) ) )// &&
								{
									if ( allCircleInterests.contains( terms.get( k ) ) )
									{
										interestTopicNames.add( terms.get( k ) );
										int pos = allCircleInterests.indexOf( terms.get( k ) );
										interestTopicIds.add( allCircleInterestIds.get( pos ) );

										Map<String, Object> items = new HashMap<String, Object>();
										items.put( "name", terms.get( k ) );
										items.put( "id", allCircleInterestIds.get( pos ) );
										listItems.add( items );
									}
								}
								if ( !interestTopicNames.contains( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) ) )// &&
								{
									if ( allCircleInterests.contains( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) ) )
									{
										interestTopicNames.add( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) );
										int pos = allCircleInterests.indexOf( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) );
										interestTopicIds.add( allCircleInterestIds.get( pos ) );

										Map<String, Object> items = new HashMap<String, Object>();
										items.put( "name", terms.get( k ) );
										items.put( "id", allCircleInterestIds.get( pos ) );
										listItems.add( items );
									}
								}
							}
						}

					}
				}
				mapTopics.put( circle, interestTopicNames );

				// single values to venn diagram
				mapValues.put( "sets", index );
				mapValues.put( "label", circle.getName() );
				mapValues.put( "size", listItems.size() );
				mapValues.put( "altLabel", circle.getName() );
				mapValues.put( "list", listItems );
				listOfMaps.add( mapValues );

				if ( mapTopics.size() > 1 )
				{
					// publications of authors added before current author
					for ( int k = 0; k < mapTopics.size(); k++ )
					{
						List<Circle> previousCircles = new ArrayList<Circle>( mapTopics.keySet() );
						List<List<String>> previousCircleLists = new ArrayList<List<String>>( mapTopics.values() );
						List<String> previousCircleTopics = previousCircleLists.get( k );
						Circle previousCircle = previousCircles.get( k );
						List<Map<String, Object>> tempListItems = new ArrayList<Map<String, Object>>();
						String label = "";

						if ( !previousCircle.equals( circle ) )
						{
							List<String> tempNames = new ArrayList<String>();

							// find common topics
							for ( String pat : previousCircleTopics )
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
							sets.add( circleList.indexOf( previousCircle ) );
							label = label + circle.getName() + "-" + previousCircle.getName();
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
				List<String> allInterests = new ArrayList<String>();
				// List<String> allInterestIds = new ArrayList<String>();
				List<Integer> count = new ArrayList<Integer>();
				List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();

				for ( int i = 0; i < idsList.size(); i++ )
				{
					Circle circle = persistenceStrategy.getCircleDAO().getById( idsList.get( i ) );
					List<String> allCircleInterests = new ArrayList<String>();
					List<String> allCircleInterestIds = new ArrayList<String>();
					Set<CircleInterestProfile> circleInterestProfiles = circle.getCircleInterestProfiles();
					for ( CircleInterestProfile cip : circleInterestProfiles )
					{
						List<CircleInterest> circleInterests = new ArrayList<CircleInterest>( cip.getCircleInterests() );
						for ( CircleInterest ai : circleInterests )
						{
							Map<Interest, Double> termWeights = ai.getTermWeights();
							List<Interest> interests = new ArrayList<Interest>( termWeights.keySet() );
							// List<Double> weights = new ArrayList<Double>(
							// termWeights.values() );
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

					Set<Publication> circlePublications = circle.getPublications();
					List<String> interestTopicNames = new ArrayList<String>();
					List<String> interestTopicIds = new ArrayList<String>();
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
							List<PublicationTopic> topics = new ArrayList<PublicationTopic>( p.getPublicationTopics() );
							for ( PublicationTopic pt : topics )
							{
								Map<String, Double> termValues = pt.getTermValues();
								List<String> terms = new ArrayList<String>( termValues.keySet() );
								for ( int k = 0; k < terms.size(); k++ )
								{
									if ( !interestTopicNames.contains( terms.get( k ) ) )// &&
									{
										if ( allCircleInterests.contains( terms.get( k ) ) )
										{
											interestTopicNames.add( terms.get( k ) );
											int pos = allCircleInterests.indexOf( terms.get( k ) );
											interestTopicIds.add( allCircleInterestIds.get( pos ) );

											Map<String, Object> items = new HashMap<String, Object>();
											items.put( "name", terms.get( k ) );
											items.put( "id", allCircleInterestIds.get( pos ) );
											listItems.add( items );
										}
									}
									if ( !interestTopicNames.contains( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) ) )// &&
									{
										if ( allCircleInterests.contains( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) ) )
										{
											interestTopicNames.add( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) );
											int pos = allCircleInterests.indexOf( terms.get( k ).substring( 0, terms.get( k ).length() - 1 ) );
											interestTopicIds.add( allCircleInterestIds.get( pos ) );

											Map<String, Object> items = new HashMap<String, Object>();
											items.put( "name", terms.get( k ) );
											items.put( "id", allCircleInterestIds.get( pos ) );
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

		Map<String, Object> visMap = new HashMap<String, Object>();
		visMap.put( "comparisonList", listOfMaps );
		return visMap;
	}

}

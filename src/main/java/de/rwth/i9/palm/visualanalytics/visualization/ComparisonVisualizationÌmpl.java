package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.ArrayList;
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
import de.rwth.i9.palm.helper.VADataFetcher;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.DataMiningAuthor;
import de.rwth.i9.palm.model.DataMiningEventGroup;
import de.rwth.i9.palm.model.DataMiningPublication;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
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

	@Autowired
	private VADataFetcher dataFetcher;

	@SuppressWarnings( "unchecked" )
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
				Map<String, Object> map = dataFetcher.fetchCoAuthorForAuthors( author, startYear, endYear, yearFilterPresent );
				List<Author> allCoAuthors = (List<Author>) map.get( "allCoAuthors" );
				List<Map<String, Object>> listItems = (List<Map<String, Object>>) map.get( "listItems" );
				mapAuthors.put( author, allCoAuthors );

				// single values to venn diagram
				mapValues.put( "sets", index );
				mapValues.put( "label", author.getName() );
				mapValues.put( "size", allCoAuthors.size() );
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
								if ( allCoAuthors.contains( a ) )
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
					Map<String, Object> map = dataFetcher.fetchCoAuthorForAuthors( author, startYear, endYear, yearFilterPresent );
					List<Author> allCoAuthors = (List<Author>) map.get( "allCoAuthors" );

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

				EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( idsList.get( i ) );
				eventGroupTempList.add( eg );
				List<Event> events = eg.getEvents();
				Map<String, Object> map = dataFetcher.fetchResearchersForConferences( events, startYear, endYear, yearFilterPresent );
				List<Author> publicationAuthors = (List<Author>) map.get( "publicationAuthors" );
				List<Map<String, Object>> listItems = (List<Map<String, Object>>) map.get( "listItems" );
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
				List<Author> allAuthors = new ArrayList<Author>();
				List<Integer> count = new ArrayList<Integer>();
				List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();
				for ( int i = 0; i < idsList.size(); i++ )
				{
					EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( idsList.get( i ) );
					eventGroupTempList.add( eg );
					List<Event> events = eg.getEvents();
					Map<String, Object> map = dataFetcher.fetchResearchersForConferences( events, startYear, endYear, yearFilterPresent );
					List<Author> publicationAuthors = (List<Author>) map.get( "publicationAuthors" );

					for ( int k = 0; k < publicationAuthors.size(); k++ )
					{
						if ( !allAuthors.contains( publicationAuthors.get( k ) ) )
						{
							allAuthors.add( publicationAuthors.get( k ) );
							Map<String, Object> items = new HashMap<String, Object>();
							items.put( "name", publicationAuthors.get( k ).getName() );
							items.put( "id", publicationAuthors.get( k ).getId() );
							items.put( "isAdded", publicationAuthors.get( k ).isAdded() );
							combinedListItems.add( items );
							count.add( 0 );
						}
						else
							count.set( allAuthors.indexOf( publicationAuthors.get( k ) ), count.get( allAuthors.indexOf( publicationAuthors.get( k ) ) ) + 1 );
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
				Map<String, Object> map = dataFetcher.fetchResearchersForPublications( p, startYear, endYear, yearFilterPresent );
				List<Author> publicationAuthors = (List<Author>) map.get( "publicationAuthors" );
				List<Map<String, Object>> listItems = (List<Map<String, Object>>) map.get( "listItems" );

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
				List<Author> allAuthors = new ArrayList<Author>();
				List<Integer> count = new ArrayList<Integer>();
				List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();
				for ( int i = 0; i < idsList.size(); i++ )
				{
					Publication p = persistenceStrategy.getPublicationDAO().getById( idsList.get( i ) );
					publicationTempList.add( p );
					Map<String, Object> map = dataFetcher.fetchResearchersForPublications( p, startYear, endYear, yearFilterPresent );
					List<Author> publicationAuthors = (List<Author>) map.get( "publicationAuthors" );

					for ( int k = 0; k < publicationAuthors.size(); k++ )
					{
						if ( !allAuthors.contains( publicationAuthors.get( k ) ) )
						{
							allAuthors.add( publicationAuthors.get( k ) );
							Map<String, Object> items = new HashMap<String, Object>();
							items.put( "name", publicationAuthors.get( k ).getName() );
							items.put( "id", publicationAuthors.get( k ).getId() );
							items.put( "isAdded", publicationAuthors.get( k ).isAdded() );
							combinedListItems.add( items );
							count.add( 0 );
						}
						else
							count.set( allAuthors.indexOf( publicationAuthors.get( k ) ), count.get( allAuthors.indexOf( publicationAuthors.get( k ) ) ) + 1 );
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
			List<DataMiningAuthor> DMAuthors = persistenceStrategy.getAuthorDAO().getDataMiningObjects();
			List<Interest> interestTempList = new ArrayList<Interest>();
			List<Publication> selectedPublications = new ArrayList<Publication>();
			List<Author> publicationAuthors = new ArrayList<Author>();
			if ( yearFilterPresent.equals( "true" ) )
			{
				List<Interest> iList = new ArrayList<Interest>();
				for ( int i = 0; i < idsList.size(); i++ )
				{
					Interest interest = persistenceStrategy.getInterestDAO().getById( idsList.get( i ) );
					iList.add( interest );
				}
				selectedPublications = new ArrayList<Publication>( filterFeature.getFilterHelper().typeWisePublications( type, null, null, null, iList, null ) );
				selectedPublications = new ArrayList<Publication>( filterFeature.getFilteredData().getFilteredPublications( type, null, null, null, iList, null, selectedPublications, null, null, null, startYear, endYear ) );
				System.out.println( "pubs in comparison: " + selectedPublications.size() );
				for ( Publication p : selectedPublications )
				{
					for ( Author a : p.getAuthors() )
					{
						if ( !publicationAuthors.contains( a ) )
							publicationAuthors.add( a );
					}
				}
				System.out.println( "pub authors: " + publicationAuthors.size() );
			}

			for ( int i = 0; i < idsList.size(); i++ )
			{

				Interest interest = persistenceStrategy.getInterestDAO().getById( idsList.get( i ) );
				interestTempList.add( interest );

				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );

				Map<String, Object> map = dataFetcher.fetchResearchersForTopics( interest, DMAuthors, publicationAuthors, yearFilterPresent );
				List<Author> interestAuthors = (List<Author>) map.get( "interestAuthors" );
				List<Map<String, Object>> listItems = (List<Map<String, Object>>) map.get( "listItems" );

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
					Interest interest = persistenceStrategy.getInterestDAO().getById( idsList.get( i ) );
					Map<String, Object> map = dataFetcher.fetchResearchersForTopics( interest, DMAuthors, publicationAuthors, yearFilterPresent );
					List<Author> interestAuthors = (List<Author>) map.get( "interestAuthors" );

					for ( int k = 0; k < interestAuthors.size(); k++ )
					{
						if ( !allAuthors.contains( interestAuthors.get( k ) ) )
						{
							allAuthors.add( interestAuthors.get( k ) );
							Map<String, Object> items = new HashMap<String, Object>();
							items.put( "name", interestAuthors.get( k ).getName() );
							items.put( "id", interestAuthors.get( k ).getId() );
							items.put( "isAdded", interestAuthors.get( k ).isAdded() );
							combinedListItems.add( items );
							count.add( 1 );
						}
						else
							count.set( allAuthors.indexOf( interestAuthors.get( k ) ), count.get( allAuthors.indexOf( interestAuthors.get( k ) ) ) + 1 );
					}
				}

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

	@SuppressWarnings( "unchecked" )
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
				Map<String, Object> map = dataFetcher.fetchConferencesForAuthors( author, startYear, endYear, yearFilterPresent );
				List<EventGroup> authorEventGroups = (List<EventGroup>) map.get( "authorEventGroups" );
				List<Map<String, Object>> listItems = (List<Map<String, Object>>) map.get( "listItems" );
				mapConferences.put( author, authorEventGroups );

				// single values to venn diagram
				mapValues.put( "sets", index );
				mapValues.put( "label", author.getName() );
				mapValues.put( "size", authorEventGroups.size() );
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
						List<EventGroup> previousAuthorEventGroups = previousAuthorLists.get( k );
						Author previousAuthor = previousAuthors.get( k );
						List<Map<String, Object>> tempListItems = new ArrayList<Map<String, Object>>();
						String label = "";
						if ( !previousAuthor.equals( author ) )
						{
							List<EventGroup> temp = new ArrayList<EventGroup>();
							// List<String> tempNames = new ArrayList<String>();

							// find common Events
							for ( EventGroup eg : previousAuthorEventGroups )
							{
								if ( authorEventGroups.contains( eg ) )
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
					Map<String, Object> map = dataFetcher.fetchConferencesForAuthors( author, startYear, endYear, yearFilterPresent );
					List<EventGroup> authorEventGroups = (List<EventGroup>) map.get( "authorEventGroups" );

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

			List<Publication> selectedPublications = new ArrayList<Publication>();
			List<EventGroup> publicationEventGroups = new ArrayList<EventGroup>();
			if ( yearFilterPresent.equals( "true" ) )
			{
				List<Interest> iList = new ArrayList<Interest>();
				for ( int i = 0; i < idsList.size(); i++ )
				{
					Interest interest = persistenceStrategy.getInterestDAO().getById( idsList.get( i ) );
					iList.add( interest );
				}
				selectedPublications = new ArrayList<Publication>( filterFeature.getFilterHelper().typeWisePublications( type, null, null, null, iList, null ) );
				selectedPublications = new ArrayList<Publication>( filterFeature.getFilteredData().getFilteredPublications( type, null, null, null, iList, null, selectedPublications, null, null, null, startYear, endYear ) );
				System.out.println( "pubs in comparison: " + selectedPublications.size() );
				for ( Publication p : selectedPublications )
				{
					if ( p.getEvent() != null )
					{
						if ( p.getEvent().getEventGroup() != null )
						{
							if ( !publicationEventGroups.contains( p.getEvent().getEventGroup() ) )
							{
								publicationEventGroups.add( p.getEvent().getEventGroup() );
								System.out.println( "PEG:" + p.getEvent().getEventGroup().getName() );
							}
						}
					}
				}
				System.out.println( "publicationEventGroups: " + publicationEventGroups.size() );
			}

			for ( int i = 0; i < idsList.size(); i++ )
			{

				Interest interest = persistenceStrategy.getInterestDAO().getById( idsList.get( i ) );
				interestTempList.add( interest );


				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );

				Map<String, Object> map = dataFetcher.fetchConferencesForTopics( interest, DMEventGroups, publicationEventGroups, yearFilterPresent );
				List<EventGroup> interestEventGroups = (List<EventGroup>) map.get( "interestEventGroups" );
				List<Map<String, Object>> listItems = (List<Map<String, Object>>) map.get( "listItems" );

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
					Interest interest = persistenceStrategy.getInterestDAO().getById( idsList.get( i ) );

					Map<String, Object> map = dataFetcher.fetchConferencesForTopics( interest, DMEventGroups, publicationEventGroups, yearFilterPresent );
					List<EventGroup> interestEventGroups = (List<EventGroup>) map.get( "interestEventGroups" );

					for ( int k = 0; k < interestEventGroups.size(); k++ )
					{
						if ( !allEventGroups.contains( interestEventGroups.get( k ) ) )
						{
							allEventGroups.add( interestEventGroups.get( k ) );
							Map<String, Object> items = new HashMap<String, Object>();
							items.put( "name", interestEventGroups.get( k ).getName() );
							items.put( "id", interestEventGroups.get( k ).getId() );
							items.put( "isAdded", interestEventGroups.get( k ).isAdded() );
							combinedListItems.add( items );
							count.add( 1 );
						}
						else
							count.set( allEventGroups.indexOf( interestEventGroups.get( k ) ), count.get( allEventGroups.indexOf( interestEventGroups.get( k ) ) ) + 1 );
					}

				}

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

	@SuppressWarnings( "unchecked" )
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
				Map<String, Object> map = dataFetcher.fetchPublicationsForAuthors( author, startYear, endYear, yearFilterPresent );
				List<Publication> authorPublications = (List<Publication>) map.get( "authorPublications" );
				List<Map<String, Object>> listItems = (List<Map<String, Object>>) map.get( "listItems" );

				mapPublications.put( author, authorPublications );

				// single values to venn diagram
				mapValues.put( "sets", index );
				mapValues.put( "label", author.getName() );
				mapValues.put( "size", authorPublications.size() );
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
								if ( authorPublications.contains( p ) )
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
					Map<String, Object> map = dataFetcher.fetchPublicationsForAuthors( author, startYear, endYear, yearFilterPresent );
					List<Publication> authorPublications = (List<Publication>) map.get( "authorPublications" );

					for ( int k = 0; k < authorPublications.size(); k++ )
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

				Map<String, Object> map = dataFetcher.fetchPublicationsForCircles( circle, startYear, endYear, yearFilterPresent );
				List<Publication> allPublications = (List<Publication>) map.get( "allPublications" );
				List<Map<String, Object>> listItems = (List<Map<String, Object>>) map.get( "listItems" );

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
					Map<String, Object> map = dataFetcher.fetchPublicationsForCircles( circle, startYear, endYear, yearFilterPresent );
					List<Publication> circlePublications = (List<Publication>) map.get( "allPublications" );

					for ( int k = 0; k < circlePublications.size(); k++ )
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
					Circle circle = persistenceStrategy.getCircleDAO().getById( idsList.get( i ) );
					label = label + circle.getName();
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

	@SuppressWarnings( "unchecked" )
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
				Map<String, Object> map = dataFetcher.fetchTopicsForAuthors( author, startYear, endYear, yearFilterPresent );
				List<String> interestTopicNames = (List<String>) map.get( "interestTopicNames" );
				List<String> interestTopicIds = (List<String>) map.get( "interestTopicIds" );
				List<String> listItems = (List<String>) map.get( "listItems" );

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
					Map<String, Object> map = dataFetcher.fetchTopicsForAuthors( author, startYear, endYear, yearFilterPresent );
					List<String> interestTopicNames = (List<String>) map.get( "interestTopicNames" );
					List<String> interestTopicIds = (List<String>) map.get( "interestTopicIds" );

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
			List<String> allTopics = new ArrayList<String>();

			for ( int i = 0; i < idsList.size(); i++ )
			{
				EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( idsList.get( i ) );
				eventGroupTempList.add( eg );
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
			}
			for ( int i = 0; i < idsList.size(); i++ )
			{
				// List<String> allTopics = new ArrayList<String>();

				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );

				EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( idsList.get( i ) );
				eventGroupTempList.add( eg );
				List<Event> events = eg.getEvents();

				Map<String, Object> map = dataFetcher.fetchTopicsForConferences( events, allTopics, startYear, endYear, yearFilterPresent );
				List<String> interestTopicNames = (List<String>) map.get( "interestTopicNames" );
				List<String> interestTopicIds = (List<String>) map.get( "interestTopicIds" );
				List<String> listItems = (List<String>) map.get( "listItems" );

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
					EventGroup eg = persistenceStrategy.getEventGroupDAO().getById( idsList.get( i ) );
					eventGroupTempList.add( eg );
					List<Event> events = eg.getEvents();

					Map<String, Object> map = dataFetcher.fetchTopicsForConferences( events, allTopics, startYear, endYear, yearFilterPresent );
					List<String> interestTopicNames = (List<String>) map.get( "interestTopicNames" );
					List<String> interestTopicIds = (List<String>) map.get( "interestTopicIds" );

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

				Publication p = persistenceStrategy.getPublicationDAO().getById( idsList.get( i ) );
				publicationTempList.add( p );
				Map<String, Object> map = dataFetcher.fetchTopicsForPublications( allInterestsInDB, p, startYear, endYear, yearFilterPresent );
				List<String> interestTopicNames = (List<String>) map.get( "interestTopicNames" );
				List<String> interestTopicIds = (List<String>) map.get( "interestTopicIds" );
				List<Map<String, Object>> listItems = (List<Map<String, Object>>) map.get( "listItems" );

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
					Publication p = persistenceStrategy.getPublicationDAO().getById( idsList.get( i ) );
					publicationTempList.add( p );
					Map<String, Object> map = dataFetcher.fetchTopicsForPublications( allInterestsInDB, p, startYear, endYear, yearFilterPresent );
					List<String> interestTopicNames = (List<String>) map.get( "interestTopicNames" );
					List<String> interestTopicIds = (List<String>) map.get( "interestTopicIds" );

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
			List<String> allTopics = new ArrayList<String>();
			for ( String id : idsList )
			{
				Circle circle = persistenceStrategy.getCircleDAO().getById( id );
				circleList.add( circle );
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
			}
			for ( int i = 0; i < idsList.size(); i++ )
			{
				Map<String, Object> mapValues = new HashMap<String, Object>();
				List<Integer> index = new ArrayList<Integer>();
				index.add( i );

				Circle circle = persistenceStrategy.getCircleDAO().getById( idsList.get( i ) );
				Map<String, Object> map = dataFetcher.fetchTopicsForCircles( circle, allTopics, startYear, endYear, yearFilterPresent );
				List<String> interestTopicNames = (List<String>) map.get( "interestTopicNames" );
				List<String> interestTopicIds = (List<String>) map.get( "interestTopicIds" );
				List<Map<String, Object>> listItems = (List<Map<String, Object>>) map.get( "listItems" );

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
					// publications of authors added before current
					// author
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
				List<Integer> count = new ArrayList<Integer>();
				List<Map<String, Object>> combinedListItems = new ArrayList<Map<String, Object>>();

				for ( int i = 0; i < idsList.size(); i++ )
				{
					Circle circle = persistenceStrategy.getCircleDAO().getById( idsList.get( i ) );
					Map<String, Object> map = dataFetcher.fetchTopicsForCircles( circle, allTopics, startYear, endYear, yearFilterPresent );
					List<String> interestTopicNames = (List<String>) map.get( "interestTopicNames" );
					List<String> interestTopicIds = (List<String>) map.get( "interestTopicIds" );

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

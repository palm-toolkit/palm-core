package de.rwth.i9.palm.visualanalytics.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.analytics.util.InterestParser;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.DataMiningPublication;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationTopicFlat;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class FilterHelperImpl implements FilterHelper
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private PalmAnalytics palmAnalytics;

	public List<Publication> getPublicationsForFilter( List<String> idsList, String type, String visType, HttpServletRequest request )
	{
		List<Publication> publications = new ArrayList<Publication>();

		// proceed only if it a part of the current request
		if ( idsList.equals( request.getSession().getAttribute( "idsList" ) ) && type.equals( request.getSession().getAttribute( "objectType" ) ) )
		{
			List<Author> authors = new ArrayList<Author>();
			List<EventGroup> eventGroupList = new ArrayList<EventGroup>();
			List<Publication> publicationsList = new ArrayList<Publication>();
			List<Interest> interestsList = new ArrayList<Interest>();
			List<Circle> circlesList = new ArrayList<Circle>();
			if ( type.equals( "researcher" ) )
				authors = getAuthorsFromIds( idsList, request );
			if ( type.equals( "conference" ) )
				eventGroupList = getConferencesFromIds( idsList, request );
			if ( type.equals( "publication" ) )
				publicationsList = getPublicationsFromIds( idsList, request );
			if ( type.equals( "topic" ) )
				interestsList = getInterestsFromIds( idsList, request );
			if ( type.equals( "circle" ) )
				circlesList = getCirclesFromIds( idsList, request );

			// if there are more than one authors in consideration
			publications = new ArrayList<Publication>( typeWisePublications( "forFilter", type, visType, authors, eventGroupList, publicationsList, interestsList, circlesList, request ) );
		}
		return publications;

	}

	public Set<Publication> typeWisePublications( String callingFunction, String type, String visType, List<Author> authorList, List<EventGroup> eventGroupList, List<Publication> publicationsList, List<Interest> interestList, List<Circle> circleList, HttpServletRequest request )
	{
		Set<Publication> publications = new HashSet<Publication>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) )
		{
			if ( type.equals( "researcher" ) )
			{
				// if there are more than one authors in consideration
				if ( authorList.size() > 1 )
				{
					List<Publication> authorPublicationsList = new ArrayList<Publication>( publications );
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

					// find common publications for filter
					if ( callingFunction.equals( "forFilter" ) || visType.equals( "publications" ) || visType.equals( "conferences" ) )
					{
						for ( int i = 0; i < count.size(); i++ )
						{
							if ( count.get( i ) != authorList.size() - 1 )
							{
								authorPublicationsList.remove( i );
								count.remove( i );
								i--;
							}
						}
					}
					publications = new HashSet<Publication>( authorPublicationsList );
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
								publications.add( pubs.get( c ) );
							}
						}

					}
				}
			}
			if ( type.equals( "conference" ) )
			{
				// if there are more than one conferences in consideration
				if ( eventGroupList.size() > 1 )
				{
					List<Publication> eventGroupPublicationsList = new ArrayList<Publication>( publications );
					for ( int i = 0; i < eventGroupList.size(); i++ )
					{
						List<Event> groupEvents = eventGroupList.get( i ).getEvents();
						for ( Event e : groupEvents )
						{
							List<Publication> eventGroupPublications = e.getPublications();
							for ( int j = 0; j < eventGroupPublications.size(); j++ )
							{
								if ( !eventGroupPublicationsList.contains( eventGroupPublications.get( j ) ) && ( eventGroupPublications.get( j ).getYear() != null || eventGroupPublications.get( j ).getPublicationDate() != null ) )
								{
									eventGroupPublicationsList.add( eventGroupPublications.get( j ) );
								}
							}
						}
					}
					publications = new HashSet<Publication>( eventGroupPublicationsList );
				}
				if ( eventGroupList.size() == 1 )
				{
					publications = new HashSet<Publication>();

					// set of conditions!!
					if ( eventGroupList.get( 0 ) != null )
						if ( eventGroupList.get( 0 ).getEvents() != null )
						{
							List<Event> groupEvents = eventGroupList.get( 0 ).getEvents();
							for ( Event e : groupEvents )
							{
								List<Publication> eventGroupPublications = e.getPublications();
								for ( int j = 0; j < eventGroupPublications.size(); j++ )
								{
									if ( !publications.contains( eventGroupPublications.get( j ) ) && ( eventGroupPublications.get( j ).getYear() != null || eventGroupPublications.get( j ).getPublicationDate() != null ) )
									{
										if ( eventGroupPublications.get( j ) != null && eventGroupPublications.get( j ).getPublicationDate() != null )
											eventGroupPublications.get( j ).setYear( eventGroupPublications.get( j ).getPublicationDate().toString().substring( 0, 4 ) );

										publications.add( eventGroupPublications.get( j ) );
									}
								}
							}
						}
				}

			}
			if ( type.equals( "publication" ) )
			{
				publications = new HashSet<Publication>( publicationsList );
			}
			if ( type.equals( "topic" ) )
			{
				List<DataMiningPublication> allDMPublications = persistenceStrategy.getPublicationDAO().getDataMiningObjects();
				List<DataMiningPublication> selectedDMPublications = new ArrayList<DataMiningPublication>();
				List<String> pubIds = new ArrayList<String>();
				List<Integer> count = new ArrayList<Integer>();
				for ( Interest i : interestList )
				{
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
								float dist = palmAnalytics.getTextCompare().getDistanceByLuceneLevenshteinDistance( topic, i.getTerm() );

								if ( dist > 0.9f )
								{
									if ( !selectedDMPublications.contains( dmp ) )
									{
										selectedDMPublications.add( dmp );
										pubIds.add( dmp.getId() );
										count.add( 1 );
									}
									else
									{
										int index = selectedDMPublications.indexOf( dmp );
										count.set( index, count.get( index ) + 1 );
									}
								}
							}
						}
					}
				}
				// find common publications for filter
				if ( callingFunction.equals( "forFilter" ) || visType.equals( "publications" ) )
				{
					System.out.println( "cf:" + callingFunction );
					System.out.println( "vt:" + visType );
					for ( int i = 0; i < count.size(); i++ )
					{
						if ( count.get( i ) < interestList.size() )
						{
							count.remove( i );
							pubIds.remove( i );
							i--;
						}
					}
				}
				publications = new HashSet<Publication>( persistenceStrategy.getPublicationDAO().getPublicationByIds( pubIds ) );
				System.out.println( "publications count: " + publications.size() );
			}
			if ( type.equals( "circle" ) )
			{
				// if there are more than one circles in consideration
				if ( circleList.size() > 1 )
				{
					List<Publication> circlePublicationsList = new ArrayList<Publication>( publications );
					List<Publication> tempPubList = new ArrayList<Publication>();
					List<Integer> count = new ArrayList<Integer>();
					for ( int i = 0; i < circleList.size(); i++ )
					{
						tempPubList = new ArrayList<Publication>( circleList.get( i ).getPublications() );
						for ( int j = 0; j < tempPubList.size(); j++ )
						{
							if ( tempPubList.get( j ).getYear() != null || tempPubList.get( j ).getPublicationDate() != null )
							{
								if ( !circlePublicationsList.contains( tempPubList.get( j ) ) )
								{
									circlePublicationsList.add( tempPubList.get( j ) );
									count.add( 0 );
								}
								else
								{
									int index = circlePublicationsList.indexOf( tempPubList.get( j ) );
									count.set( index, count.get( index ) + 1 );
								}
							}
						}
					}
					// find common publications for filter
					if ( callingFunction.equals( "forFilter" ) || visType.equals( "publications" ) )
					{
						for ( int i = 0; i < count.size(); i++ )
						{
							if ( count.get( i ) != circleList.size() - 1 )
							{
								circlePublicationsList.remove( i );
								count.remove( i );
								i--;
							}
						}
					}
					publications = new HashSet<Publication>( circlePublicationsList );
				}
				if ( circleList.size() == 1 )
				{
					if ( circleList.get( 0 ) != null )
					{
						List<Publication> pubs = new ArrayList<Publication>( circleList.get( 0 ).getPublications() );
						for ( int c = 0; c < pubs.size(); c++ )
						{
							// to not consider publication if year is null
							if ( pubs.get( c ).getYear() != null || pubs.get( c ).getPublicationDate() != null )
							{
								publications.add( pubs.get( c ) );
							}
						}

					}
				}
			}
		}
		return publications;
	}

	public List<Author> getAuthorsFromIds( List<String> idsList, HttpServletRequest request )
	{
		// get Author List
		List<Author> authorList = new ArrayList<Author>();

		// proceed only if it a part of the current request
		if ( idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			for ( int itemIndex = 0; itemIndex < idsList.size(); itemIndex++ )
			{
				authorList.add( persistenceStrategy.getAuthorDAO().getById( idsList.get( itemIndex ) ) );
			}
		}
		return authorList;

	}

	public List<EventGroup> getConferencesFromIds( List<String> idsList, HttpServletRequest request )
	{
		// get Event List
		List<EventGroup> eventGroupList = new ArrayList<EventGroup>();

		// proceed only if it a part of the current request
		if ( idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			for ( int itemIndex = 0; itemIndex < idsList.size(); itemIndex++ )
			{
				eventGroupList.add( persistenceStrategy.getEventGroupDAO().getById( idsList.get( itemIndex ) ) );
			}
		}
		return eventGroupList;

	}

	public List<Publication> getPublicationsFromIds( List<String> idsList, HttpServletRequest request )
	{
		// get Publication List
		List<Publication> publicationList = new ArrayList<Publication>();

		// proceed only if it a part of the current request
		if ( idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			for ( int itemIndex = 0; itemIndex < idsList.size(); itemIndex++ )
			{
				publicationList.add( persistenceStrategy.getPublicationDAO().getById( idsList.get( itemIndex ) ) );
			}
		}
		return publicationList;

	}

	public List<Interest> getInterestsFromIds( List<String> idsList, HttpServletRequest request )
	{
		// get Interest List
		List<Interest> interestList = new ArrayList<Interest>();

		// proceed only if it a part of the current request
		if ( idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			for ( int itemIndex = 0; itemIndex < idsList.size(); itemIndex++ )
			{
				interestList.add( persistenceStrategy.getInterestDAO().getById( idsList.get( itemIndex ) ) );
			}
		}
		return interestList;

	}

	public List<Circle> getCirclesFromIds( List<String> idsList, HttpServletRequest request )
	{
		// get Circle List
		List<Circle> circleList = new ArrayList<Circle>();

		// proceed only if it a part of the current request
		if ( idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			for ( int itemIndex = 0; itemIndex < idsList.size(); itemIndex++ )
			{
				circleList.add( persistenceStrategy.getCircleDAO().getById( idsList.get( itemIndex ) ) );
			}
		}
		return circleList;

	}

}

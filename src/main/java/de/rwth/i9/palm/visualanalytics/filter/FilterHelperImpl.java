package de.rwth.i9.palm.visualanalytics.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.analytics.util.InterestParser;
import de.rwth.i9.palm.model.Author;
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

	public List<Publication> getPublicationsForFilter( List<String> idsList, String type )
	{
		List<Publication> publications = new ArrayList<Publication>();

		List<Author> authors = new ArrayList<Author>();
		List<EventGroup> eventGroupList = new ArrayList<EventGroup>();
		List<Publication> publicationsList = new ArrayList<Publication>();
		List<Interest> interestsList = new ArrayList<Interest>();
		if ( type.equals( "researcher" ) )
			authors = getAuthorsFromIds( idsList );
		if ( type.equals( "conference" ) )
			eventGroupList = getConferencesFromIds( idsList );
		if ( type.equals( "publication" ) )
			publicationsList = getPublicationsFromIds( idsList );
		if ( type.equals( "topic" ) )
			interestsList = getInterestsFromIds( idsList );

		// if there are more than one authors in consideration
		publications = new ArrayList<Publication>( typeWisePublications( type, authors, eventGroupList, publicationsList, interestsList ) );

		return publications;

	}

	public Set<Publication> typeWisePublications( String type, List<Author> authorList, List<EventGroup> eventGroupList, List<Publication> publicationsList, List<Interest> interestList )
	{
		Set<Publication> authorPublications = new HashSet<Publication>();

		if ( type.equals( "researcher" ) )
		{
			// if there are more than one authors in consideration
			if ( authorList.size() > 1 )
			{
				List<Publication> authorPublicationsList = new ArrayList<Publication>( authorPublications );
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
				for ( int i = 0; i < count.size(); i++ )
				{
					if ( count.get( i ) != authorList.size() - 1 )
					{
						authorPublicationsList.remove( i );
						count.remove( i );
						i--;
					}
				}
				authorPublications = new HashSet<Publication>( authorPublicationsList );
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
							authorPublications.add( pubs.get( c ) );
						}
					}

				}
			}
		}
		if ( type.equals( "conference" ) )
		{
			// if there are more than one authors in consideration
			if ( eventGroupList.size() > 1 )
			{
				List<Publication> eventGroupPublicationsList = new ArrayList<Publication>( authorPublications );
				for ( int i = 0; i < eventGroupList.size(); i++ )
				{
					List<Event> groupEvents = eventGroupList.get( i ).getEvents();
					for ( Event e : groupEvents )
					{
						List<Publication> publications = e.getPublications();
						for ( int j = 0; j < publications.size(); j++ )
						{
							if ( !eventGroupPublicationsList.contains( publications.get( j ) ) && ( publications.get( j ).getYear() != null || publications.get( j ).getPublicationDate() != null ) )
							{
								eventGroupPublicationsList.add( publications.get( j ) );
							}
						}
					}
				}
				authorPublications = new HashSet<Publication>( eventGroupPublicationsList );
			}
			if ( eventGroupList.size() == 1 )
			{
				authorPublications = new HashSet<Publication>();

				// set of conditions!!
				if ( eventGroupList.get( 0 ) != null )
					if ( eventGroupList.get( 0 ).getEvents() != null )
					{
						List<Event> groupEvents = eventGroupList.get( 0 ).getEvents();
						for ( Event e : groupEvents )
						{
							List<Publication> publications = e.getPublications();
							for ( int j = 0; j < publications.size(); j++ )
							{
								if ( !authorPublications.contains( publications.get( j ) ) && ( publications.get( j ).getYear() != null || publications.get( j ).getPublicationDate() != null ) )
								{
									if ( publications.get( j ) != null && publications.get( j ).getPublicationDate() != null )
										publications.get( j ).setYear( publications.get( j ).getPublicationDate().toString().substring( 0, 4 ) );

									authorPublications.add( publications.get( j ) );
								}
							}
						}
					}
			}

		}
		if ( type.equals( "publication" ) )
		{
			authorPublications = new HashSet<Publication>( publicationsList );
		}
		if ( type.equals( "topic" ) )
		{
			System.out.println( "in getby type" );

			List<DataMiningPublication> allDMPublications = persistenceStrategy.getPublicationDAO().getDataMiningObjects();
			List<DataMiningPublication> selectedDMPublications = new ArrayList<DataMiningPublication>();
			List<String> titles = new ArrayList<String>();
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
									titles.add( dmp.getTitle() );
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

			for ( int i = 0; i < count.size(); i++ )
			{
				if ( count.get( i ) < interestList.size() )
				{
					count.remove( i );
					titles.remove( i );
					i--;
				}
			}
			authorPublications = new HashSet<Publication>( persistenceStrategy.getPublicationDAO().getPublicationByTitle( titles ) );
		}
		if ( type.equals( "circle" ) )
		{

		}
		return authorPublications;
	}

	public List<Author> getAuthorsFromIds( List<String> idsList )
	{
		// get Author List
		List<Author> authorList = new ArrayList<Author>();
		for ( int itemIndex = 0; itemIndex < idsList.size(); itemIndex++ )
		{
			authorList.add( persistenceStrategy.getAuthorDAO().getById( idsList.get( itemIndex ) ) );
		}
		return authorList;

	}

	public List<EventGroup> getConferencesFromIds( List<String> idsList )
	{
		// get Event List
		List<EventGroup> eventGroupList = new ArrayList<EventGroup>();
		for ( int itemIndex = 0; itemIndex < idsList.size(); itemIndex++ )
		{
			eventGroupList.add( persistenceStrategy.getEventGroupDAO().getById( idsList.get( itemIndex ) ) );
		}
		return eventGroupList;

	}

	public List<Publication> getPublicationsFromIds( List<String> idsList )
	{
		// get Publication List
		List<Publication> publicationList = new ArrayList<Publication>();
		for ( int itemIndex = 0; itemIndex < idsList.size(); itemIndex++ )
		{
			publicationList.add( persistenceStrategy.getPublicationDAO().getById( idsList.get( itemIndex ) ) );
		}
		return publicationList;

	}

	public List<Interest> getInterestsFromIds( List<String> idsList )
	{
		// get Interest List
		List<Interest> interestList = new ArrayList<Interest>();
		for ( int itemIndex = 0; itemIndex < idsList.size(); itemIndex++ )
		{
			interestList.add( persistenceStrategy.getInterestDAO().getById( idsList.get( itemIndex ) ) );
		}
		return interestList;

	}

}

package de.rwth.i9.palm.helper;

import java.util.ArrayList;
import java.util.HashMap;
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
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.DataMiningAuthor;
import de.rwth.i9.palm.model.DataMiningPublication;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationTopicFlat;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class VADataFetcher
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private PalmAnalytics palmAnalytics;

	public List<Author> fetchCommonAuthors( String type, Set<Publication> publications, List<String> idsList )
	{

		System.out.println( "!!!Pub count: " + publications.size() );
		List<Author> commonAuthors = new ArrayList<Author>();
		List<Integer> count = new ArrayList<Integer>();

		if ( type.equals( "publication" ) )
		{
			for ( Publication p : publications )
			{
				for ( Author a : p.getAuthors() )
				{
					if ( !commonAuthors.contains( a ) )
					{
						commonAuthors.add( a );
						count.add( 1 );
					}
					else
					{
						int index = commonAuthors.indexOf( a );
						count.set( index, count.get( index ) + 1 );
					}
				}
			}

			System.out.println( "!!!Common auth count: " + commonAuthors.size() );
		}
		if ( type.equals( "researcher" ) )
		{
			for ( int i = 0; i < idsList.size(); i++ )
			{
				Author researcher = persistenceStrategy.getAuthorDAO().getById( idsList.get( i ) );
				List<Author> researcherCoAuthors = new ArrayList<Author>();
				List<Publication> researcherPublications = new ArrayList<Publication>( researcher.getPublications() );
				for ( Publication p : researcherPublications )
				{
					for ( Author pubAuthor : p.getAuthors() )
					{
						if ( !researcherCoAuthors.contains( pubAuthor ) )
						{
							researcherCoAuthors.add( pubAuthor );
						}
					}
				}

				for ( Author a : researcherCoAuthors )
				{
					if ( !commonAuthors.contains( a ) )
					{
						commonAuthors.add( a );
						count.add( 1 );
					}
					else
					{
						count.set( commonAuthors.indexOf( a ), count.get( commonAuthors.indexOf( a ) ) + 1 );
					}
				}
			}
		}

		if ( type.equals( "topic" ) )
		{
			List<DataMiningAuthor> DMAuthors = persistenceStrategy.getAuthorDAO().getDataMiningObjects();
			for ( int i = 0; i < idsList.size(); i++ )
			{
				Interest interest = persistenceStrategy.getInterestDAO().getById( idsList.get( i ) );

				for ( DataMiningAuthor dma : DMAuthors )
				{
					Map<String, Double> interests = new HashMap<String, Double>();
					interests = InterestParser.parseInterestString( dma.getAuthor_interest_flat().getInterests() );
					if ( interests.keySet().contains( interest.getTerm() ) )
					{
						Author a = persistenceStrategy.getAuthorDAO().getById( dma.getId() );
						if ( !commonAuthors.contains( a ) )
						{
							commonAuthors.add( a );
							count.add( 1 );
						}
						else
						{
							int index = commonAuthors.indexOf( a );
							count.set( index, count.get( index ) + 1 );
						}
					}
				}
			}
		}
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
				for ( Author a : eventAuthors )
				{
					if ( !commonAuthors.contains( a ) )
					{
						commonAuthors.add( a );
						count.add( 1 );
					}
					else
					{
						count.set( commonAuthors.indexOf( a ), count.get( commonAuthors.indexOf( a ) ) + 1 );
					}
				}
			}
		}
		if ( type.equals( "circle" ) )
		{
			for ( int i = 0; i < idsList.size(); i++ )
			{
				Circle c = persistenceStrategy.getCircleDAO().getById( idsList.get( i ) );
				for ( Author a : c.getAuthors() )
				{
					if ( !commonAuthors.contains( a ) )
					{
						commonAuthors.add( a );
						count.add( 1 );
					}
					else
					{
						count.set( commonAuthors.indexOf( a ), count.get( commonAuthors.indexOf( a ) ) + 1 );
					}
				}
			}
		}

		for ( int i = 0; i < count.size(); i++ )
		{
			if ( count.get( i ) < idsList.size() )
			{
				count.remove( i );
				commonAuthors.remove( i );
				i--;
			}
		}
		System.out.println( "!!!new author count: " + commonAuthors.size() );
		if ( !publications.isEmpty() )
		{
			// get authors from the publications
			List<Author> authors = new ArrayList<Author>();
			for ( Publication p : publications )
			{
				for ( Author a : p.getAuthors() )
				{
					if ( !authors.contains( a ) )
					{
						authors.add( a );
					}
				}
			}

			// the common authors must be part of the publication authors
			for ( int i = 0; i < commonAuthors.size(); i++ )
			{
				if ( !authors.contains( commonAuthors.get( i ) ) )
				{
					commonAuthors.remove( i );
					i--;
				}
			}
		}
		return commonAuthors;
	}

	public Set<Publication> fetchAllPublications( String type, List<String> idsList, List<Author> authorList )
	{
		Set<Publication> publications = new HashSet<Publication>();
		if ( type.equals( "researcher" ) )
		{
			for ( Author a : authorList )
			{
				publications.addAll( a.getPublications() );
			}
		}
		if ( type.equals( "conference" ) )
		{
			List<EventGroup> eventGroupList = new ArrayList<EventGroup>();
			for ( String id : idsList )
			{
				eventGroupList.add( persistenceStrategy.getEventGroupDAO().getById( id ) );
			}
			// if there are more than one conferences in consideration
			if ( idsList.size() > 1 )
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
								if ( !eventGroupPublications.contains( eventGroupPublications.get( j ) ) && ( eventGroupPublications.get( j ).getYear() != null || eventGroupPublications.get( j ).getPublicationDate() != null ) )
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
		if ( type.equals( "topic" ) )
		{
			List<DataMiningPublication> allDMPublications = persistenceStrategy.getPublicationDAO().getDataMiningObjects();
			List<DataMiningPublication> selectedDMPublications = new ArrayList<DataMiningPublication>();
			List<String> pubIds = new ArrayList<String>();
			List<Interest> interestList = new ArrayList<Interest>();
			for ( String id : idsList )
			{
				interestList.add( persistenceStrategy.getInterestDAO().getById( id ) );
			}
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
								}
							}
						}
					}
				}
			}
			publications = new HashSet<Publication>( persistenceStrategy.getPublicationDAO().getPublicationByIds( pubIds ) );
		}
		if ( type.equals( "circle" ) )
		{
			List<Circle> circleList = new ArrayList<Circle>();
			for ( String id : idsList )
			{
				circleList.add( persistenceStrategy.getCircleDAO().getById( id ) );
			}
			// if there are more than one circles in consideration
			if ( circleList.size() > 1 )
			{
				List<Publication> circlePublicationsList = new ArrayList<Publication>( publications );
				List<Publication> tempPubList = new ArrayList<Publication>();
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
							}
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

		return publications;
	}
}

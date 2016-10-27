package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class NetworkVisualizationImpl implements NetworkVisualization
{
	@Autowired
	private GraphFeature graphFeature;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

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

			for ( int i = 0; i < eventGroupAuthors.size(); i++ )
			{
				if ( count.get( i ) != idsList.size() - 1 )
				{
					count.remove( i );
					eventGroupAuthors.remove( i );
					i--;
				}
			}
		}
		Author authorForCoAuthors = new Author();
		if ( authoridForCoAuthors != null )
		{
			authorForCoAuthors = persistenceStrategy.getAuthorDAO().getById( authoridForCoAuthors );
		}
		visMap.put( "graphFile", graphFeature.getGephiGraph( type, authorList, publications, idsList, eventGroupAuthors, authorForCoAuthors ).get( "graphFile" ) );
		return visMap;
	}
}

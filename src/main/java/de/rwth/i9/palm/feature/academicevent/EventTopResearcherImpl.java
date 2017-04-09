package de.rwth.i9.palm.feature.academicevent;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class EventTopResearcherImpl implements EventTopResearcher
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@SuppressWarnings( "unchecked" )
	@Override
	public Map<String, Object> getResearcherTopListByEventId( String eventId, String pid, Integer maxresult, String orderBy ) throws UnsupportedEncodingException, InterruptedException, URISyntaxException, ExecutionException
	{
		Map<String, Object> responseMap = new HashMap<String, Object>();

		Event event = persistenceStrategy.getEventDAO().getById( eventId );

		if ( event == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "message", "Error - venue not found" );
			return responseMap;
		}

		List<Publication> eventPublications = event.getPublications();
		if ( eventPublications == null || eventPublications.isEmpty() )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "message", "Error - venue contain no publication" );
			return responseMap;
		}

		responseMap.put( "status", "ok" );

		// get data name
		String title = event.getEventGroup().getName();
		if ( event.getEventGroup().getNotation() != null && !title.equals( event.getEventGroup().getNotation() ) )
			title = event.getEventGroup().getNotation();

		Map<String, Object> eventMapQuery = new LinkedHashMap<String, Object>();
		eventMapQuery.put( "id", event.getId() );
		eventMapQuery.put( "title", title );

		responseMap.put( "event", eventMapQuery );

		List<Map<String, Object>> eventParticipantsMap = new ArrayList<Map<String, Object>>();

		for ( Publication publication : eventPublications )
		{
			List<Author> publicationAuthors = publication.getAuthors();
			if ( publicationAuthors == null || publicationAuthors.isEmpty() )
				continue;

			for ( Author author : publicationAuthors )
			{
				Map<String, Object> authorMap = new HashMap<String, Object>();
				authorMap.put( "id", author.getId() );
				authorMap.put( "name", author.getName() );
				authorMap.put( "hindex", author.getHindex() );
				if ( author.getInstitution() != null )
				{
					Map<String, String> affiliationData = new HashMap<String, String>();

					affiliationData.put( "institution", author.getInstitution().getName() );

					if ( author.getInstitution().getLocation() != null )
					{
						affiliationData.put( "country", author.getInstitution().getLocation().getCountry().getName() );
					}

					authorMap.put( "aff", affiliationData );
				}

				if ( author.getPhotoUrl() != null )
					authorMap.put( "photo", author.getPhotoUrl() );

				authorMap.put( "isAdded", author.isAdded() );
				authorMap.put( "status", author.getAcademicStatus() );

				if ( authorMap.get( "eventNrPublications" ) == null )
					authorMap.put( "eventNrPublications", 1 );
				else
					authorMap.put( "eventNrPublications", (int) authorMap.get( "eventNrPublications" ) + 1 );

				eventParticipantsMap.add( authorMap );
			}
		}

		responseMap.put( "participants", eventParticipantsMap );

		return responseMap;
	}
}

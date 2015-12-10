package de.rwth.i9.palm.feature.publication;

import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class PublicationBasicStatisticImpl implements PublicationBasicStatistic
{

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Override
	public Map<String, Object> getPublicationBasicStatisticById( String publicationId )
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// get author
		Publication publication = persistenceStrategy.getPublicationDAO().getById( publicationId );
		if ( publication == null )
		{
			responseMap.put( "status", "Error - publication not found" );
			return responseMap;
		}

		responseMap.put( "status", "ok" );

		// put publication detail
		Map<String, Object> publicationMap = new LinkedHashMap<String, Object>();

		if ( publication.getPublicationDate() != null )
		{
			SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd" );
			publicationMap.put( "publicationDate", sdf.format( publication.getPublicationDate() ) );
		}

		if ( publication.getLanguage() != null )
			publicationMap.put( "language", publication.getLanguage() );

		if ( publication.getCitedBy() != 0 )
			publicationMap.put( "cited", publication.getCitedBy() );

		if ( publication.getPublicationType() != null )
		{
			String publicationType = publication.getPublicationType().toString();
			publicationType = publicationType.substring( 0, 1 ).toUpperCase() + publicationType.toLowerCase().substring( 1 );
			publicationMap.put( "type", publicationType );
		}

		if ( publication.getEvent() != null )
		{
			Map<String, Object> eventMap = new LinkedHashMap<String, Object>();
			eventMap.put( "id", publication.getEvent().getId() );
			eventMap.put( "name", publication.getEvent().getName() );

			if ( publication.getEvent().getVolume() != null )
				eventMap.put( "volume", publication.getEvent().getVolume() );
			if ( publication.getEvent().getYear() != null )
				eventMap.put( "year", publication.getEvent().getYear() );
			eventMap.put( "isAdded", publication.getEvent().isAdded() );

			publicationMap.put( "event", eventMap );

			if ( publication.getEvent().getEventGroup() != null )
			{
				Map<String, Object> eventGroupMap = new LinkedHashMap<String, Object>();
				eventGroupMap.put( "id", publication.getEvent().getEventGroup().getId() );
				eventGroupMap.put( "name", publication.getEvent().getEventGroup().getName() );

				if ( publication.getEvent().getEventGroup().getNotation() != null )
					eventGroupMap.put( "abbr", publication.getEvent().getEventGroup().getNotation() );
				eventGroupMap.put( "isAdded", publication.getEvent().getEventGroup().isAdded() );

				publicationMap.put( "eventGroup", eventGroupMap );
			}
		}

		if ( publication.getAdditionalInformation() != null )
			publicationMap.putAll( publication.getAdditionalInformationAsMap() );

		if ( publication.getStartPage() > 0 )
			publicationMap.put( "pages", publication.getStartPage() + " - " + publication.getEndPage() );


		responseMap.put( "publication", publicationMap );

		return responseMap;
	}

}

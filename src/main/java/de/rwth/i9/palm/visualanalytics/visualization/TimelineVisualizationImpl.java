package de.rwth.i9.palm.visualanalytics.visualization;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import de.rwth.i9.palm.helper.comparator.PublicationByDateComparator;
import de.rwth.i9.palm.model.Publication;

@Component
public class TimelineVisualizationImpl implements TimelineVisualization
{
	public Map<String, Object> visualizeTimeline( List<String> idsList, Set<Publication> publications, HttpServletRequest request )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			List<Publication> publicationsList = new ArrayList<Publication>( publications );

			// sort by date
			Collections.sort( publicationsList, new PublicationByDateComparator() );

			List<Map<String, Object>> pubDetailsList = new ArrayList<Map<String, Object>>();
			for ( Publication pub : publicationsList )
			{
				Map<String, Object> pubDetails = new LinkedHashMap<String, Object>();

				pubDetails.put( "id", pub.getId() );
				pubDetails.put( "title", pub.getTitle() );
				pubDetails.put( "year", pub.getYear() );
				pubDetails.put( "type", pub.getPublicationType() );
				pubDetails.put( "date", new SimpleDateFormat( "dd/MM/yyyy" ).format( pub.getPublicationDate() ) );
				pubDetailsList.add( pubDetails );
			}

			visMap.put( "pubDetailsList", pubDetailsList );
		}
		return visMap;
	}

}

package de.rwth.i9.palm.visualanalytics.visualization;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import de.rwth.i9.palm.helper.comparator.PublicationByDateComparator;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationFile;
import de.rwth.i9.palm.model.PublicationType;

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

			String prevYear = "0";
			Map<String, Object> yearWisePublicationCategories = new HashMap<String, Object>();
			Map<String, Integer> categoryPubs = new HashMap<String, Integer>();
			List<Map<String, Object>> pubDetailsList = new ArrayList<Map<String, Object>>();
			for ( Publication pub : publicationsList )
			{
				Map<String, Object> pubDetails = new LinkedHashMap<String, Object>();

				pubDetails.put( "id", pub.getId() );
				pubDetails.put( "title", pub.getTitle() );

				PublicationType pubType = pub.getPublicationType();
				pubDetails.put( "type", pubType );

				if ( pub.getPublicationFiles() != null && !pub.getPublicationFiles().isEmpty() )
				{
					PublicationFile pf = new ArrayList<PublicationFile>( pub.getPublicationFiles() ).get( 0 );
					pubDetails.put( "url", pf.getUrl() );
				}

				if ( pub.getYear() != null )
				{
					String year = pub.getYear();
					pubDetails.put( "year", year );
					if ( !prevYear.equals( year ) )
					{
						yearWisePublicationCategories.put( prevYear, categoryPubs );
						categoryPubs = new HashMap<String, Integer>();
					}
					if ( pubType != null )
					{
						int val = 0;
						if ( categoryPubs.get( pubType.toString() ) != null )
						{
							val = categoryPubs.get( pubType.toString() );
							categoryPubs.remove( pubType.toString() );
						}
						categoryPubs.put( pubType.toString(), val + 1 );
					}
					prevYear = year;
				}
				else
					pubDetails.put( "year", "unknown" );
				if ( pub.getPublicationDate() != null )
					pubDetails.put( "date", new SimpleDateFormat( "dd/MM/yyyy" ).format( pub.getPublicationDate() ) );
				else
					pubDetails.put( "date", "unknown" );
				pubDetailsList.add( pubDetails );
			}
			yearWisePublicationCategories.put( prevYear, categoryPubs );
			visMap.put( "yearWisePublicationCategories", yearWisePublicationCategories );
			visMap.put( "pubDetailsList", pubDetailsList );
		}
		return visMap;
	}

}

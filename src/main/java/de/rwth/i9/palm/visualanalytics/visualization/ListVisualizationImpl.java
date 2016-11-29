package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.feature.researcher.ResearcherFeature;
import de.rwth.i9.palm.helper.comparator.PublicationByDateComparator;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.visualanalytics.service.VisualizationFeature;

@Component
public class ListVisualizationImpl implements ListVisualization
{
	@Autowired
	private ResearcherFeature researcherFeature;

	@Autowired
	private VisualizationFeature visualizationFeature;

	@Override
	public Map<String, Object> visualizeResearchersList( String type, String visType, Set<Publication> publications, String startYear, String endYear, List<String> idsList, String yearFilterPresent, HttpServletRequest request )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
			visMap.putAll( researcherFeature.getResearcherCoauthor().getResearcherCoAuthorMapByPublication( publications, type, visType, idsList, startYear, endYear, yearFilterPresent ) );

		return visMap;
	}

	@Override
	public Map<String, Object> visualizeConferencesList( String type, Set<Publication> publications, String startYear, String endYear, List<String> idsList, String yearFilterPresent, HttpServletRequest request )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			// if ( type.equals( "conference" ) )
			visMap.putAll( researcherFeature.getResearcherAcademicEventTree().getResearcherAllAcademicEvents( publications, false ) );

			// if ( type.equals( "researcher" ) || type.equals( "publication" )
			// ||
			// type.equals( "topic" ) || type.equals( "circle" ) )
			// visMap.putAll(
			// researcherFeature.getResearcherAcademicEventTree().getResearcherAllAcademicEvents(
			// publications, false ) );
		}
		return visMap;
	}

	@Override
	public Map<String, Object> visualizePublicationsList( String type, Set<Publication> publications, String startYear, String endYear, List<String> idsList, String yearFilterPresent, HttpServletRequest request )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
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
				pubDetails.put( "date", pub.getPublicationDate() );
				pubDetailsList.add( pubDetails );
			}

			visMap.put( "pubDetailsList", pubDetailsList );
		}
		return visMap;
	}

	@Override
	public Map<String, Object> visualizeTopicsList( String type, Set<Publication> publications, String startYear, String endYear, List<String> idsList, String yearFilterPresent, HttpServletRequest request )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			visMap = visualizationFeature.getVisBubbles().visualizeBubbles( type, idsList, publications, startYear, endYear, yearFilterPresent, request );
		}
		return visMap;
	}

}

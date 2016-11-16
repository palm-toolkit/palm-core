package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.feature.researcher.ResearcherFeature;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;

@Component
public class LocationsVisualizationImpl implements LocationsVisualization
{
	@Autowired
	private ResearcherFeature researcherFeature;

	public Map<String, Object> visualizeLocations( String type, Set<Publication> publications, List<String> idsList, String startYear, String endYear, List<Interest> filteredTopic, HttpServletRequest request )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			visMap.putAll( researcherFeature.getResearcherAcademicEventTree().getResearcherAllAcademicEvents( publications, true ) );
		}
		return visMap;
	}
}

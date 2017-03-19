package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;

public interface LocationsVisualization
{
	/**
	 * @param type
	 * @param publications
	 * @param idsList
	 * @param startYear
	 * @param endYear
	 * @param filteredTopic
	 * @param request
	 * @return
	 */
	public Map<String, Object> visualizeLocations( String type, Set<Publication> publications, List<String> idsList, String startYear, String endYear, List<Interest> filteredTopic, HttpServletRequest request );
}

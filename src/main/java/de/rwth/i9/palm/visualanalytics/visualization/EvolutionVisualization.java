package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import de.rwth.i9.palm.model.Publication;

public interface EvolutionVisualization
{
	/**
	 * @param type
	 * @param idsList
	 * @param publications
	 * @param startYear
	 * @param endYear
	 * @param yearFilterPresent
	 * @param request
	 * @return
	 */
	public Map<String, Object> visualizeEvolution( String type, List<String> idsList, Set<Publication> publications, String startYear, String endYear, String yearFilterPresent, HttpServletRequest request );
}

package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;

public interface ListVisualization
{
	/**
	 * @param type
	 * @param visType
	 * @param publications
	 * @param startYear
	 * @param endYear
	 * @param idsList
	 * @param yearFilterPresent
	 * @param filteredTopic
	 * @param request
	 * @return
	 */
	public Map<String, Object> visualizeResearchersList( String type, String visType, Set<Publication> publications, String startYear, String endYear, List<String> idsList, String yearFilterPresent, List<Interest> filteredTopic, HttpServletRequest request );

	/**
	 * @param type
	 * @param publications
	 * @param startYear
	 * @param endYear
	 * @param idsList
	 * @param yearFilterPresent
	 * @param request
	 * @return
	 */
	public Map<String, Object> visualizeConferencesList( String type, Set<Publication> publications, String startYear, String endYear, List<String> idsList, String yearFilterPresent, HttpServletRequest request );

	/**
	 * @param type
	 * @param publications
	 * @param startYear
	 * @param endYear
	 * @param idsList
	 * @param yearFilterPresent
	 * @param request
	 * @return
	 */
	public Map<String, Object> visualizePublicationsList( String type, Set<Publication> publications, String startYear, String endYear, List<String> idsList, String yearFilterPresent, HttpServletRequest request );

	/**
	 * @param type
	 * @param publications
	 * @param startYear
	 * @param endYear
	 * @param idsList
	 * @param yearFilterPresent
	 * @param request
	 * @return
	 */
	public Map<String, Object> visualizeTopicsList( String type, Set<Publication> publications, String startYear, String endYear, List<String> idsList, String yearFilterPresent, HttpServletRequest request );
}

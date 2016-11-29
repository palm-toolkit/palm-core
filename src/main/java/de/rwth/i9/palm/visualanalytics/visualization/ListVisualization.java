package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import de.rwth.i9.palm.model.Publication;

public interface ListVisualization
{
	public Map<String, Object> visualizeResearchersList( String type, String visType, Set<Publication> publications, String startYear, String endYear, List<String> idsList, String yearFilterPresent, HttpServletRequest request );

	public Map<String, Object> visualizeConferencesList( String type, Set<Publication> publications, String startYear, String endYear, List<String> idsList, String yearFilterPresent, HttpServletRequest request );

	public Map<String, Object> visualizePublicationsList( String type, Set<Publication> publications, String startYear, String endYear, List<String> idsList, String yearFilterPresent, HttpServletRequest request );

	public Map<String, Object> visualizeTopicsList( String type, Set<Publication> publications, String startYear, String endYear, List<String> idsList, String yearFilterPresent, HttpServletRequest request );
}

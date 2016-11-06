package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rwth.i9.palm.model.Publication;

public interface ComparisonVisualization
{
	public Map<String, Object> visualizeResearchersComparison( String type, List<String> idsList, Set<Publication> publications, String startYear, String endYear, String yearFilterPresent );

	public Map<String, Object> visualizeConferencesComparison( String type, List<String> idsList, Set<Publication> publications, String startYear, String endYear, String yearFilterPresent );

	public Map<String, Object> visualizePublicationsComparison( String type, List<String> idsList, Set<Publication> publications, String startYear, String endYear, String yearFilterPresent );

	public Map<String, Object> visualizeTopicsComparison( String type, List<String> idsList, Set<Publication> publications, String startYear, String endYear, String yearFilterPresent );
}

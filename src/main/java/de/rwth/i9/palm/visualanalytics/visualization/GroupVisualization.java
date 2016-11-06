package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rwth.i9.palm.model.Publication;

public interface GroupVisualization
{
	public Map<String, Object> visualizeResearchersGroup( String type, List<String> idsList, Set<Publication> publications, String startYear, String endYear );

	public Map<String, Object> visualizePublicationsGroup( String type, Set<Publication> publications );

	public Map<String, Object> visualizeConferencesGroup( String type, Set<Publication> publications );

	public Map<String, Object> visualizeTopicsGroup( String type, Set<Publication> publications );
}

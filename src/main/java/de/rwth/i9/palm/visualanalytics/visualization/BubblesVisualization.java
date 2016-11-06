package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rwth.i9.palm.model.Publication;

public interface BubblesVisualization
{
	public Map<String, Object> visualizeBubbles( String type, List<String> idsList, Set<Publication> publications, String startYear, String endYear );
}

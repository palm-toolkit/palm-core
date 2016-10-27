package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.Map;
import java.util.Set;

import de.rwth.i9.palm.model.Publication;

public interface TimelineVisualization
{
	public Map<String, Object> visualizeTimeline( Set<Publication> publications );
}

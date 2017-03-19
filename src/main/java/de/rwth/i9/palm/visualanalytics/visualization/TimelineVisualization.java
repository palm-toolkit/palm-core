package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import de.rwth.i9.palm.model.Publication;

public interface TimelineVisualization
{
	/**
	 * @param idsList
	 * @param publications
	 * @param request
	 * @return
	 */
	public Map<String, Object> visualizeTimeline( List<String> idsList, Set<Publication> publications, HttpServletRequest request );
}

package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import de.rwth.i9.palm.model.Publication;

public interface NetworkVisualization
{
	public Map<String, Object> visualizeNetwork( String type, Set<Publication> publications, List<String> idsList, String startYear, String endYear, String authoridForCoAuthors, String yearFilterPresent, HttpServletRequest request );
}

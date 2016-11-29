package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;

public interface ClusteringService
{
	public Map<String, Object> clusterAuthors( String algorithm, List<String> idsList, Set<Publication> publications, String type, String visType, String startYear, String endYear, HttpServletRequest request, String yearFilterPresent, List<Interest> filteredTopic );

	public Map<String, Object> clusterPublications( String algorithm, Set<Publication> publications );

	public Map<String, Object> clusterConferences( String algorithm, Set<Publication> publications );
}

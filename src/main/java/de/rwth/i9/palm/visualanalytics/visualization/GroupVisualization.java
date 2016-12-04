package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;

public interface GroupVisualization
{
	public Map<String, Object> visualizeResearchersGroup( String type, String visType, List<String> idsList, Set<Publication> publications, String startYear, String endYear, String yearFilterPresent, List<Interest> filteredTopic, List<String> repeatCallList, String algo, String seedVal, String noOfClustersVal, String foldsVal, String iterationsVal, HttpServletRequest request );

	public Map<String, Object> visualizePublicationsGroup( String type, String visType, Set<Publication> publications, List<String> repeatCallList, String algo, String seedVal, String noOfClustersVal, String foldsVal, String iterationsVal, HttpServletRequest request );

	public Map<String, Object> visualizeConferencesGroup( String type, String visType, Set<Publication> publications, List<Interest> filteredTopic, List<String> idsList, List<String> repeatCallList, String algo, String seedVal, String noOfClustersVal, String foldsVal, String iterationsVal, HttpServletRequest request );

	public Map<String, Object> visualizeTopicsGroup( String type, Set<Publication> publications, HttpServletRequest request );
}

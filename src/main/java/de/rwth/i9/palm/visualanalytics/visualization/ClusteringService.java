package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;

public interface ClusteringService
{
	/**
	 * @param algorithm
	 * @param idsList
	 * @param publications
	 * @param type
	 * @param visType
	 * @param startYear
	 * @param endYear
	 * @param request
	 * @param yearFilterPresent
	 * @param filteredTopic
	 * @param repeatCallList
	 * @param seedVal
	 * @param noOfClustersVal
	 * @param foldsVal
	 * @param iterationsVal
	 * @return
	 */
	public Map<String, Object> clusterAuthors( String algorithm, List<String> idsList, Set<Publication> publications, String type, String visType, String startYear, String endYear, HttpServletRequest request, String yearFilterPresent, List<Interest> filteredTopic, List<String> repeatCallList, String seedVal, String noOfClustersVal, String foldsVal, String iterationsVal );

	/**
	 * @param algorithm
	 * @param publications
	 * @param type
	 * @param visType
	 * @param repeatCallList
	 * @param seedVal
	 * @param noOfClustersVal
	 * @param foldsVal
	 * @param iterationsVal
	 * @return
	 */
	public Map<String, Object> clusterPublications( String algorithm, Set<Publication> publications, String type, String visType, List<String> repeatCallList, String seedVal, String noOfClustersVal, String foldsVal, String iterationsVal );

	/**
	 * @param algorithm
	 * @param publications
	 * @param filteredTopic
	 * @param type
	 * @param visType
	 * @param idsList
	 * @param repeatCallList
	 * @param seedVal
	 * @param noOfClustersVal
	 * @param foldsVal
	 * @param iterationsVal
	 * @return
	 */
	public Map<String, Object> clusterConferences( String algorithm, Set<Publication> publications, List<Interest> filteredTopic, String type, String visType, List<String> idsList, List<String> repeatCallList, String seedVal, String noOfClustersVal, String foldsVal, String iterationsVal );
}

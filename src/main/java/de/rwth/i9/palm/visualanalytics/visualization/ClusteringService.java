package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;

public interface ClusteringService
{
	public Map<String, Object> clusterAuthors( String algorithm, List<Author> authorList, List<String> idsList, Set<Publication> publications, String type, String startYear, String endYear );

	public Map<String, Object> clusterPublications( String algorithm, Set<Publication> publications );

	public Map<String, Object> clusterConferences( String algorithm, List<Author> authorList, Set<Publication> publications );
}

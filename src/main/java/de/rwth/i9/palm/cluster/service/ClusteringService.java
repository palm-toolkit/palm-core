package de.rwth.i9.palm.cluster.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;

public interface ClusteringService
{
	public Map<String, Object> clusterAuthors( String algorithm, List<Author> authorList, Set<Publication> publications );

	public Map<String, Integer> clusterPublications( String algorithm, Set<Publication> publications );
	// public String clusterTopics();

	public Map<String, Integer> clusterConferences( String algorithm, List<Author> authorList, Set<Publication> publications );
}

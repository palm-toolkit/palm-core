package de.rwth.i9.palm.graph.feature;

import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;

public interface GraphFeature
{
	public Map<String, Object> getGephiGraph( String type, List<Author> authorList, Set<Publication> authorPublications, List<String> idsList, List<Author> eventGroupAuthors, Author authorForCoAuthors );
}

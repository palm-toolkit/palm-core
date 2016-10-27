package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.List;
import java.util.Map;

import de.rwth.i9.palm.model.Author;

public interface SimilarityService
{
	public Map<String, Object> similarAuthors( List<Author> authorList );

	public Map<String, Object> similarConferences( List<String> idsList );

	public Map<String, Object> similarPublications( List<String> idsList );

}

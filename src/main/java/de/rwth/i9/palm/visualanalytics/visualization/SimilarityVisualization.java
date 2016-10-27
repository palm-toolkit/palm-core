package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.List;
import java.util.Map;

import de.rwth.i9.palm.model.Author;

public interface SimilarityVisualization
{
	public Map<String, Object> visualizeSimilarResearchers( String type, List<Author> authorList, List<String> idsList );

	public Map<String, Object> visualizeSimilarConferences( String type, List<Author> authorList, List<String> idsList );

	public Map<String, Object> visualizeSimilarPublications( String type, List<Author> authorList, List<String> idsList );

	public Map<String, Object> visualizeSimilarTopics( String type, List<Author> authorList, List<String> idsList );
}

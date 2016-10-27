package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;

public interface ListVisualization
{
	public Map<String, Object> visualizeResearchersList( String type, List<Author> authorList, Set<Publication> publications, String startYear, String endYear, List<String> idsList );

	public Map<String, Object> visualizeConferencesList( String type, List<Author> authorList, Set<Publication> publications, String startYear, String endYear, List<String> idsList );

	public Map<String, Object> visualizePublicationsList( String type, List<Author> authorList, Set<Publication> publications, String startYear, String endYear, List<String> idsList );

	public Map<String, Object> visualizeTopicsList( String type, List<Author> authorList, Set<Publication> publications, String startYear, String endYear, List<String> idsList );
}

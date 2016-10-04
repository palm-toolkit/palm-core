package de.rwth.i9.palm.feature.researcher;

import java.util.Map;
import java.util.Set;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;

public interface ResearcherAcademicEventTree
{
	public Map<String, Object> getResearcherAcademicEventTree( Author author );

	public Map<String, Object> getResearcherAllAcademicEvents( Set<Publication> publications );

}

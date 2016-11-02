package de.rwth.i9.palm.visualanalytics.filter;

import java.util.List;
import java.util.Set;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;

public interface FilterHelper
{
	public List<Publication> getPublicationsForFilter( List<String> idsList, String type );

	public Set<Publication> typeWisePublications( String type, List<Author> authorList, List<EventGroup> eventGroupList, List<Publication> publicationsList, List<Interest> interestList );

	public List<Author> getAuthorsFromIds( List<String> idsList );

	public List<EventGroup> getConferencesFromIds( List<String> idsList );

	public List<Publication> getPublicationsFromIds( List<String> idsList );

	public List<Interest> getInterestsFromIds( List<String> idsList );
}

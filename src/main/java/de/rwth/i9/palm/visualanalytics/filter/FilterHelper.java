package de.rwth.i9.palm.visualanalytics.filter;

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;

public interface FilterHelper
{
	/**
	 * @param idsList
	 * @param type
	 * @param visType
	 * @param request
	 * @return
	 */
	public List<Publication> getPublicationsForFilter( List<String> idsList, String type, String visType, HttpServletRequest request );

	/**
	 * @param callingFunction
	 * @param type
	 * @param visType
	 * @param authorList
	 * @param eventGroupList
	 * @param publicationsList
	 * @param interestList
	 * @param circleList
	 * @param request
	 * @return
	 */
	public Set<Publication> typeWisePublications( String callingFunction, String type, String visType, List<Author> authorList, List<EventGroup> eventGroupList, List<Publication> publicationsList, List<Interest> interestList, List<Circle> circleList, HttpServletRequest request );

	/**
	 * @param idsList
	 * @param request
	 * @return
	 */
	public List<Author> getAuthorsFromIds( List<String> idsList, HttpServletRequest request );

	/**
	 * @param idsList
	 * @param request
	 * @return
	 */
	public List<EventGroup> getConferencesFromIds( List<String> idsList, HttpServletRequest request );

	/**
	 * @param idsList
	 * @param request
	 * @return
	 */
	public List<Publication> getPublicationsFromIds( List<String> idsList, HttpServletRequest request );

	/**
	 * @param idsList
	 * @param request
	 * @return
	 */
	public List<Interest> getInterestsFromIds( List<String> idsList, HttpServletRequest request );

	/**
	 * @param idsList
	 * @param request
	 * @return
	 */
	public List<Circle> getCirclesFromIds( List<String> idsList, HttpServletRequest request );
}

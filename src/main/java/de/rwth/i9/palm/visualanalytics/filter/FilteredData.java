package de.rwth.i9.palm.visualanalytics.filter;

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;

public interface FilteredData
{
	/**
	 * @param type
	 * @param visType
	 * @param authorList
	 * @param eventGroupList
	 * @param publicationList
	 * @param interestList
	 * @param circleList
	 * @param filteredPublication
	 * @param filteredConference
	 * @param filteredTopic
	 * @param filteredCircle
	 * @param startYear
	 * @param endYear
	 * @param yearFilterPresent
	 * @param request
	 * @return
	 */
	public Set<Publication> getFilteredPublications( String type, String visType, List<Author> authorList, List<EventGroup> eventGroupList, List<Publication> publicationList, List<Interest> interestList, List<Circle> circleList, List<Publication> filteredPublication, List<EventGroup> filteredConference, List<Interest> filteredTopic, List<Circle> filteredCircle, String startYear, String endYear, String yearFilterPresent, HttpServletRequest request );
}

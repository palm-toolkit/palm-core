package de.rwth.i9.palm.visualanalytics.filter;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public interface DataForFilter
{
	/**
	 * @param idsList
	 * @param type
	 * @param visType
	 * @param request
	 * @return
	 */
	public Map<String, Object> publicationFilter( List<String> idsList, String type, String visType, HttpServletRequest request );

	/**
	 * @param idsList
	 * @param type
	 * @param visType
	 * @param request
	 * @return
	 */
	public Map<String, Object> conferenceFilter( List<String> idsList, String type, String visType, HttpServletRequest request );

	/**
	 * @param idsList
	 * @param type
	 * @param visType
	 * @return
	 */
	public Map<String, Object> circleFilter( List<String> idsList, String type, String visType );

	/**
	 * @param idsList
	 * @param type
	 * @param visType
	 * @param request
	 * @return
	 */
	public Map<String, Object> topicFilter( List<String> idsList, String type, String visType, HttpServletRequest request );

	/**
	 * @param idsList
	 * @param type
	 * @param visType
	 * @param request
	 * @return
	 */
	public Map<String, Object> timeFilter( List<String> idsList, String type, String visType, HttpServletRequest request );

}

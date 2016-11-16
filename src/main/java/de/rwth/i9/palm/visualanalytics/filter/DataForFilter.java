package de.rwth.i9.palm.visualanalytics.filter;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public interface DataForFilter
{
	public Map<String, Object> publicationFilter( List<String> idsList, String type, HttpServletRequest request );

	public Map<String, Object> conferenceFilter( List<String> idsList, String type, HttpServletRequest request );

	public Map<String, Object> circleFilter( List<String> idsList, String type );

	public Map<String, Object> topicFilter( List<String> idsList, String type, HttpServletRequest request );

	public Map<String, Object> timeFilter( List<String> idsList, String type, HttpServletRequest request );

}

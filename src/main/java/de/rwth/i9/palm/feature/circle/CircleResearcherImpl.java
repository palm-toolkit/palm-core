package de.rwth.i9.palm.feature.circle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.text.WordUtils;

import de.rwth.i9.palm.helper.comparator.AuthorByNaturalOrderComparator;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Circle;

public class CircleResearcherImpl implements CircleResearcher
{

	@Override
	public Map<String, Object> getCircleResearcherMap( Circle circle )
	{
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		List<Map<String, Object>> researcherList = new ArrayList<Map<String, Object>>();

		if ( circle.getAuthors() == null || circle.getAuthors().isEmpty() )
		{
			responseMap.put( "count", 0 );
			return responseMap;
		}

		List<Author> authors = new ArrayList<Author>();
		authors.addAll( circle.getAuthors() );

		Collections.sort( authors, new AuthorByNaturalOrderComparator() );

		for ( Author researcher : authors )
		{
			Map<String, Object> researcherMap = new LinkedHashMap<String, Object>();
			researcherMap.put( "id", researcher.getId() );
			researcherMap.put( "name", WordUtils.capitalize( researcher.getName() ) );
			if ( researcher.getPhotoUrl() != null )
				researcherMap.put( "photo", researcher.getPhotoUrl() );
			if ( researcher.getAcademicStatus() != null )
				researcherMap.put( "status", researcher.getAcademicStatus() );
			if ( researcher.getInstitution() != null )
				researcherMap.put( "aff", researcher.getInstitution().getName() );
			if ( researcher.getCitedBy() > 0 )
				researcherMap.put( "citedBy", researcher.getCitedBy() );

			if ( researcher.getPublicationAuthors() != null )
				researcherMap.put( "publicationsNumber", researcher.getNoPublication() );
			else
				researcherMap.put( "publicationsNumber", 0 );
			String otherDetail = "";
			if ( researcher.getOtherDetail() != null )
				otherDetail += researcher.getOtherDetail();
			if ( researcher.getDepartment() != null )
				otherDetail += ", " + researcher.getDepartment();
			if ( !otherDetail.equals( "" ) )
				researcherMap.put( "detail", otherDetail );

			researcherMap.put( "isAdded", researcher.isAdded() );

			researcherList.add( researcherMap );
		}
		responseMap.put( "count", researcherList.size() );
		responseMap.put( "researchers", researcherList );

		return responseMap;
	}

}

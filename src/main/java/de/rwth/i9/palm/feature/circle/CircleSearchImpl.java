package de.rwth.i9.palm.feature.circle;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.text.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class CircleSearchImpl implements CircleSearch
{

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Override
	public Map<String, Object> getCircleListByQuery( String query, String creatorId, Integer page, Integer maxresult, String fulltextSearch, String orderBy )
	{
		Map<String, Object> circleMap;

		Author creator = null;
		if ( creatorId != null )
			creator = persistenceStrategy.getAuthorDAO().getById( creatorId );

		circleMap = persistenceStrategy.getCircleDAO().getCircleWithPaging( query, creator, page, maxresult, orderBy );

		return circleMap;
	}

	@Override
	public Map<String, Object> printJsonOutput( Map<String, Object> responseMap, List<Circle> circles )
	{
		if ( circles == null || circles.isEmpty() )
		{
			responseMap.put( "count", 0 );
			return responseMap;
		}

		List<Map<String, Object>> circleList = new ArrayList<Map<String, Object>>();

		// preparing data format
		DateFormat dateFormat = new SimpleDateFormat( "dd/mm/yyyy", Locale.ENGLISH );

		for ( Circle circle : circles )
		{
			Map<String, Object> circleMap = new LinkedHashMap<String, Object>();
			circleMap.put( "id", circle.getId() );

			circleMap.put( "name", circle.getName() );
			circleMap.put( "dateCreated", dateFormat.format( circle.getCreationDate() ) );

			if ( circle.getCreator() != null )
			{
				Map<String, Object> creatorMap = new LinkedHashMap<String, Object>();
				creatorMap.put( "id", circle.getCreator().getId() );
				creatorMap.put( "name", WordUtils.capitalize( circle.getCreator().getName() ) );
				if ( circle.getCreator().getAuthor() != null )
					creatorMap.put( "authorId", circle.getCreator().getAuthor().getId() );

				circleMap.put( "creator", creatorMap );
			}

			if ( circle.getAuthors() != null )
				circleMap.put( "numberAuthors", circle.getAuthors().size() );

			if ( circle.getPublications() != null )
				circleMap.put( "numberPublications", circle.getPublications().size() );

			circleList.add( circleMap );
		}

		responseMap.put( "count", circleList.size() );
		responseMap.put( "circles", circleList );

		return responseMap;
	}

}

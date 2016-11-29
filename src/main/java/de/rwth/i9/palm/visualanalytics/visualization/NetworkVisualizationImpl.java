package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.helper.VADataFetcher;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class NetworkVisualizationImpl implements NetworkVisualization
{
	@Autowired
	private GraphFeature graphFeature;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private VADataFetcher dataFetcher;

	public Map<String, Object> visualizeNetwork( String type, Set<Publication> publications, List<String> idsList, String startYear, String endYear, String authoridForCoAuthors, String yearFilterPresent, List<Interest> filteredTopic, HttpServletRequest request )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			Author authorForCoAuthors = new Author();
			if ( authoridForCoAuthors != null )
			{
				authorForCoAuthors = persistenceStrategy.getAuthorDAO().getById( authoridForCoAuthors );
			}

			List<Author> authorList = new ArrayList<Author>();
			for ( String id : idsList )
			{
				authorList.add( persistenceStrategy.getAuthorDAO().getById( id ) );
			}

			Map<String, Object> map = dataFetcher.fetchCommonAuthors( type, publications, idsList, yearFilterPresent );
			@SuppressWarnings( "unchecked" )
			List<Author> selectedAuthors = (List<Author>) map.get( "commonAuthors" );
			// System.out.println( "sel authors in network:" +
			// selectedAuthors.size() + "\n" );

			// verify if the researchers also have the selected interests, not
			// just there publications
			if ( !filteredTopic.isEmpty() )
				selectedAuthors = dataFetcher.getAuthorsFromInterestFilter( filteredTopic, selectedAuthors );

			if ( publications.isEmpty() )
				publications = dataFetcher.fetchAllPublications( type, idsList, authorList );

			visMap.put( "graphFile", graphFeature.getGephiGraph( type, authorList, publications, idsList, authorForCoAuthors, selectedAuthors, request ).get( "graphFile" ) );
		}
		return visMap;
	}
}

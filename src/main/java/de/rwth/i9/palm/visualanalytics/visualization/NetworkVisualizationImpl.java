package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.helper.VADataFetcher;
import de.rwth.i9.palm.model.Author;
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

	public Map<String, Object> visualizeNetwork( String type, Set<Publication> publications, List<String> idsList, String startYear, String endYear, String authoridForCoAuthors )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();


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

		Map<String, Object> map = dataFetcher.fetchCommonAuthors( type, publications, idsList );
		@SuppressWarnings( "unchecked" )
		List<Author> selectedAuthors = (List<Author>) map.get( "commonAuthors" );
		System.out.println( "SELETED AUTHORS IN NETWORK: " + selectedAuthors.size() );
		if ( publications.isEmpty() )
			publications = dataFetcher.fetchAllPublications( type, idsList, authorList );

		visMap.put( "graphFile", graphFeature.getGephiGraph( type, authorList, publications, idsList, authorForCoAuthors, selectedAuthors ).get( "graphFile" ) );
		return visMap;
	}
}

package de.rwth.i9.palm.feature.researcher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Institution;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class ResearcherApiImpl implements ResearcherApi
{

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Override
	public Map<String, Object> getAuthorAutoComplete( String namePrefix )
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		List<Author> authors = persistenceStrategy.getAuthorDAO().getAuthorWithLikeQuery( namePrefix );

		if ( authors.isEmpty() )
		{
			responseMap.put( "count", 0 );
			return responseMap;
		}

		List<Object> authorObject = new ArrayList<Object>();

		for ( Author author : authors )
		{
			Map<String, Object> authorMap = new LinkedHashMap<String, Object>();
			authorMap.put( "id", author.getId() );
			authorMap.put( "name", author.getName() );
			if ( author.getInstitutions() != null )
				for ( Institution institution : author.getInstitutions() )
				{
					if ( authorMap.get( "aff" ) != null )
						authorMap.put( "aff", authorMap.get( "aff" ) + ", " + institution.getName() );
					else
						authorMap.put( "aff", institution.getName() );
				}
			if ( author.getPhotoUrl() != null )
				authorMap.put( "photo", author.getPhotoUrl() );

			authorObject.add( authorMap );
		}
		responseMap.put( "count", authors.size() );
		responseMap.put( "author", authorObject );

		return responseMap;
	}

}

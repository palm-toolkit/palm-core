package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.DataMiningAuthor;
import de.rwth.i9.palm.model.DataMiningEventGroup;
import de.rwth.i9.palm.model.DataMiningPublication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class SimilarityVisualizationImpl implements SimilarityVisualization
{
	@Autowired
	private SimilarityService similarityService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@SuppressWarnings( "unchecked" )
	@Override
	public Map<String, Object> visualizeSimilarResearchers( String type, List<String> idsList, HttpServletRequest request )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			List<Author> authorList = new ArrayList<Author>();
			for ( String id : idsList )
			{
				authorList.add( persistenceStrategy.getAuthorDAO().getById( id ) );
			}
			Map<String, Object> map = similarityService.similarAuthors( authorList );
			if ( map != null )
			{
				Map<DataMiningAuthor, Map<String, Double>> interestMap = (Map<DataMiningAuthor, Map<String, Double>>) map.get( "interestMap" );

				Map<DataMiningAuthor, Double> scoreMap = (Map<DataMiningAuthor, Double>) map.get( "scoreMap" );

				List<Double> similarityValues = new ArrayList<Double>( scoreMap.values() );
				List<DataMiningAuthor> similarAuthors = new ArrayList<DataMiningAuthor>( scoreMap.keySet() );

				List<Double> truncSimilarityValues = new ArrayList<Double>();
				List<String> authorNames = new ArrayList<String>();
				List<String> authorIds = new ArrayList<String>();
				List<Map<String, Double>> truncInterests = new ArrayList<Map<String, Double>>();

				int count = 0;
				if ( interestMap.size() > 20 )
					count = 20;
				else
					count = interestMap.size();

				for ( int i = 0; i < count; i++ )
				{
					truncSimilarityValues.add( similarityValues.get( i ) );
					authorNames.add( similarAuthors.get( i ).getName() );
					authorIds.add( similarAuthors.get( i ).getId() );
					truncInterests.add( interestMap.get( similarAuthors.get( i ) ) );
				}

				visMap.put( "names", authorNames );
				visMap.put( "ids", authorIds );
				visMap.put( "similarity", truncSimilarityValues );
				visMap.put( "interests", truncInterests );
			}
			return visMap;
		}
		else
			return (Map<String, Object>) visMap.put( "names", null );

	}

	@SuppressWarnings( "unchecked" )
	@Override
	public Map<String, Object> visualizeSimilarConferences( String type, List<String> idsList, HttpServletRequest request )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			Map<String, Object> map = similarityService.similarConferences( idsList );
			if ( map != null )
			{
				Map<DataMiningPublication, Map<String, Double>> interestMap = (Map<DataMiningPublication, Map<String, Double>>) map.get( "interestMap" );

				Map<DataMiningEventGroup, Double> scoreMap = (Map<DataMiningEventGroup, Double>) map.get( "scoreMap" );

				List<Double> similarityValues = new ArrayList<Double>( scoreMap.values() );
				List<DataMiningEventGroup> similarConferences = new ArrayList<DataMiningEventGroup>( scoreMap.keySet() );

				List<Double> truncSimilarityValues = new ArrayList<Double>();
				List<String> conferenceNames = new ArrayList<String>();
				List<String> conferenceIds = new ArrayList<String>();
				List<Map<String, Double>> truncInterests = new ArrayList<Map<String, Double>>();

				int count = 0;
				if ( interestMap.size() > 20 )
					count = 20;
				else
					count = interestMap.size();

				for ( int i = 0; i < count; i++ )
				{
					truncSimilarityValues.add( similarityValues.get( i ) );
					conferenceNames.add( similarConferences.get( i ).getName() );
					conferenceIds.add( similarConferences.get( i ).getId() );
					truncInterests.add( interestMap.get( similarConferences.get( i ) ) );
				}

				visMap.put( "names", conferenceNames );
				visMap.put( "ids", conferenceIds );
				visMap.put( "similarity", truncSimilarityValues );
				visMap.put( "interests", truncInterests );
			}
			return visMap;
		}
		else
			return (Map<String, Object>) visMap.put( "names", null );

	}

	@SuppressWarnings( "unchecked" )
	@Override
	public Map<String, Object> visualizeSimilarPublications( String type, List<String> idsList, HttpServletRequest request )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			Map<String, Object> map = similarityService.similarPublications( idsList );
			if ( map != null )
			{
				Map<DataMiningPublication, Map<String, Double>> interestMap = (Map<DataMiningPublication, Map<String, Double>>) map.get( "interestMap" );

				Map<DataMiningPublication, Double> scoreMap = (Map<DataMiningPublication, Double>) map.get( "scoreMap" );

				List<Double> similarityValues = new ArrayList<Double>( scoreMap.values() );
				List<DataMiningPublication> similarPublications = new ArrayList<DataMiningPublication>( scoreMap.keySet() );

				List<Double> truncSimilarityValues = new ArrayList<Double>();
				List<String> publicationNames = new ArrayList<String>();
				List<String> publicationsIds = new ArrayList<String>();
				List<Map<String, Double>> truncInterests = new ArrayList<Map<String, Double>>();

				int count = 0;
				if ( interestMap.size() > 20 )
					count = 20;
				else
					count = interestMap.size();

				for ( int i = 0; i < count; i++ )
				{
					truncSimilarityValues.add( similarityValues.get( i ) );
					publicationNames.add( similarPublications.get( i ).getTitle() );
					publicationsIds.add( similarPublications.get( i ).getId() );
					truncInterests.add( interestMap.get( similarPublications.get( i ) ) );
				}

				visMap.put( "names", publicationNames );
				visMap.put( "ids", publicationsIds );
				visMap.put( "similarity", truncSimilarityValues );
				visMap.put( "interests", truncInterests );
			}
			return visMap;
		}
		else
			return (Map<String, Object>) visMap.put( "names", null );

	}

	@Override
	public Map<String, Object> visualizeSimilarTopics( String type, List<String> idsList, HttpServletRequest request )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			Map<String, Object> map = similarityService.similarTopics( idsList );

			visMap.put( "names", map.get( "names" ) );
			visMap.put( "ids", map.get( "ids" ) );
			visMap.put( "similarity", map.get( "similarity" ) );
		}
		return visMap;

	}

}

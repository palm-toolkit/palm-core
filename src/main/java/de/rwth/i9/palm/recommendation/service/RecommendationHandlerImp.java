package de.rwth.i9.palm.recommendation.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.mahout.cf.taste.common.TasteException;
import org.hibernate.SessionFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.topicextraction.service.TopicExtractionService;

@Component
public class RecommendationHandlerImp implements RecommendationHandler
{

	private GenericRecommendation recommendationAlgorithms;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private TopicExtractionService topicExtractionService;

	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private PublicationExtractionService pubService;
	
	@Override
	public JSONArray computeRecommendation( String algorithm, Author researcher, int stepNo ) throws JSONException, SQLException, IOException, TasteException
	{
		initRecommendationAlgorithm();
		
		return recommendationAlgorithms.computeRecommendation( algorithm, researcher, stepNo );
	}

	@Override
	public JSONArray computeSingleTree( String algorithm, Author researcher, int stepNo, String id )
	{
		initRecommendationAlgorithm();
		
		return recommendationAlgorithms.computeSingleTree( algorithm, researcher, stepNo, id );
	}

	@Override
	public List<Object> requesetAuthor( String query, int maxResults, String queryType )
	{
		initRecommendationAlgorithm();
		
		return recommendationAlgorithms.requesetAuthor( query, maxResults, queryType );
	}

	@Override
	public void reset()
	{
		recommendationAlgorithms = null;
	}
	
	private void initRecommendationAlgorithm() 
	{
		if ( recommendationAlgorithms == null )
		{
			this.recommendationAlgorithms = new GenericRecommendation() {
				
				@Override
				protected void recommendationStep6( Author researcher, UtilINImp util )
				{
					// TODO Auto-generated method stub
					
				}
				
				@Override
				protected void recommendationStep5( Author researcher, UtilINImp util )
				{
					// TODO Auto-generated method stub
					
				}
				
				@Override
				protected void recommendationStep4( Author researcher, UtilINImp util )
				{
					// TODO Auto-generated method stub
					
				}
				
				@Override
				protected void recommendationStep3( Author researcher, UtilINImp util )
				{
					// TODO Auto-generated method stub
					
				}
				
				@Override
				protected void recommendationStep2( Author researcher, UtilINImp util )
				{
					// TODO Auto-generated method stub
					
				}
				
				@Override
				protected void recommendationStep1( Author researcher, UtilINImp util )
				{
					// TODO Auto-generated method stub
					
				}
				
				@Override
				protected JSONArray computeSingleTree( Author researcher, int stepNo, String id )
				{
					// TODO Auto-generated method stub
					return null;
				}
			};
		
			this.recommendationAlgorithms.initObjects( persistenceStrategy, topicExtractionService, sessionFactory, pubService );
		}
	}
}

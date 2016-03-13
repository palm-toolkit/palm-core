package de.rwth.i9.palm.feature.researcher;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.interestmining.service.TopicModelingService;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class ResearcherTopicModelingImpl implements ResearcherTopicModeling
{
	private final static Logger logger = LoggerFactory.getLogger( ResearcherTopicModelingImpl.class );

	@Autowired
	private TopicModelingService topicModelingService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	/**
	 * This is the first method used to show the widget Simple LDA
	 * implementation String authorId boolean isReplaceExistingResult
	 */
	@Override
	public Map<String, Object> getLdaBasicExample( String authorId, boolean isReplaceExistingResult )
	{
		// create JSON container for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		
		Author author = persistenceStrategy.getAuthorDAO().getById( authorId );

		if( author == null ){
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "author not found" );
			return responseMap;
		}
		
		// calculate and store the result of topic modeling
		topicModelingService.calculateAuthorTopicModeling( author, isReplaceExistingResult );

		// get JSON represent AuthorTOpicModelingProfile
		List<Object> topicModelingResults = topicModelingService.getAuthorTopicModeliFromDatabase( author );
		
		if ( topicModelingResults == null || topicModelingResults.isEmpty() )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "no interest profile found" );
			return responseMap;
		}
		
		responseMap.put( "status", "ok" );
		responseMap.put( "topicModel", topicModelingResults );
		
		return responseMap;
	}

	/**
	 * This is the main method that will be used to extract the topic in the
	 * form of Ngrams The result of the method will be <String, String> Where
	 * the second one is composed of topic -_- %
	 */
	@Override
	public Map<String, Object> getTopicModelingNgrams( String authorId, boolean isReplaceExistingResult )
	{
		// create JSON container for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		Author author = persistenceStrategy.getAuthorDAO().getById( authorId );

		if ( author == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "author not found" );
			return responseMap;
		}

		// calculate and store the result of topic modeling
		topicModelingService.calculateAuthorTopicModeling( author, isReplaceExistingResult );

		// get JSON represent AuthorTOpicModelingProfile
		List<Object> topicModelingResults = topicModelingService.getAuthorTopicModeliFromDatabase( author );

		if ( topicModelingResults == null || topicModelingResults.isEmpty() )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "no topic model profile found" );
			return responseMap;
		}

		responseMap.put( "status", "ok" );
		responseMap.put( "topicModel", topicModelingResults );

		return responseMap;
	}

	/**
	 * This is the main method that will be used to extract the topic in the
	 * form of Unigrams The result of the method will be <String, String> Where
	 * the second one is composed of topic -_- %
	 */
	@Override
	public Map<String, Object> getTopicModelingUnigrams( String authorId, boolean isReplaceExistingResult )
	{
		// create JSON container for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		Author author = persistenceStrategy.getAuthorDAO().getById( authorId );

		if ( author == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "author not found" );
			return responseMap;
		}

		// calculate and store the result of topic modeling
		topicModelingService.calculateAuthorTopicModeling( author, isReplaceExistingResult );

		// get JSON represent AuthorTOpicModelingProfile
		List<Object> topicModelingResults = topicModelingService.getAuthorTopicModeliFromDatabase( author );

		if ( topicModelingResults == null || topicModelingResults.isEmpty() )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "no interest profile found" );
			return responseMap;
		}

		responseMap.put( "status", "ok" );
		responseMap.put( "topicModel", topicModelingResults );

		return responseMap;
	}
}

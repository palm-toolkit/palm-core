package de.rwth.i9.palm.feature.researcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class ResearcherSimilarauthorImpl implements ResearcherSimilarauthor
{
	@Autowired
	private PalmAnalytics palmAnalytics;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

<<<<<<< HEAD
=======
	private String path = "C:/Users/Albi/Desktop/";

>>>>>>> feature-topic-modelling
	@Override
	public Map<String, Object> getResearcherSimilarAuthorMap( Author author, int startPage, int maxresult )
	{
		// researchers list container
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		List<String> similarAuthors = new ArrayList<String>();
<<<<<<< HEAD
		similarAuthors = palmAnalytics.getNGrams().similarEntities( author.getId(), maxresult );
=======
		similarAuthors = palmAnalytics.getNGrams().runSimilarEntities( author.getId().toString(), "C:/Users/Albi/Desktop/", "Authors", 20, 10, 3, false );
		//similarEntities( author.getId(), maxresult, 3 );
>>>>>>> feature-topic-modelling

		// Prepare set of similarAuthor HashSet;
		Set<String> similarauthorSet = new HashSet<String>();

		for ( String similar : similarAuthors )
		{
			if ( persistenceStrategy.getAuthorDAO().getById( similar.split( "->" )[0] ).equals( author ) )
				continue;

			similarauthorSet.add( similar );
		}

		// prepare list of object map containing similarAuthor details
		List<Map<String, Object>> similarAuthorList = new ArrayList<Map<String, Object>>();

		for ( String similarAuthor : similarauthorSet )
		{
			// only copy necessary attributes
			// persistenceStrategy.( similar.split( "->"
			// )[0] )
			Map<String, Object> similarAuthorMap = new LinkedHashMap<String, Object>();
			similarAuthorMap.put( "id", similarAuthor.split( "->" )[0] );
			similarAuthorMap.put( "name", persistenceStrategy.getAuthorDAO().getById( similarAuthor.split( "->" )[0] ).getName() );
			if ( persistenceStrategy.getAuthorDAO().getById( similarAuthor.split( "->" )[0] ).getInstitution() != null )
				similarAuthorMap.put( "affiliation", persistenceStrategy.getAuthorDAO().getById( similarAuthor.split( "->" )[0] ).getInstitution().getName() );
			if ( persistenceStrategy.getAuthorDAO().getById( similarAuthor.split( "->" )[0] ).getPhotoUrl() != null )
				similarAuthorMap.put( "photo", persistenceStrategy.getAuthorDAO().getById( similarAuthor.split( "->" )[0] ).getPhotoUrl() );
			similarAuthorMap.put( "isAdded", persistenceStrategy.getAuthorDAO().getById( similarAuthor.split( "->" )[0] ).isAdded() );
			similarAuthorMap.put( "similarity", similarAuthor.split( "->" )[1] );

			// add into list
			similarAuthorList.add( similarAuthorMap );
		}

		// prepare list of object map containing similarAuthor details
		List<Map<String, Object>> similarAuthorListPaging = new ArrayList<Map<String, Object>>();

		int position = 0;
		for ( Map<String, Object> similarAuthor : similarAuthorList )
		{
			if ( position >= startPage && similarAuthorListPaging.size() < maxresult )
			{
				similarAuthorListPaging.add( similarAuthor );
			}
		}

		// remove unnecessary result

		// put similarAuthor to responseMap
		responseMap.put( "countTotal", similarAuthorList.size() );
		responseMap.put( "count", similarAuthorListPaging.size() );
		responseMap.put( "similarAuthors", similarAuthorListPaging );

		return responseMap;
	}

	@Override
	public Map<String, Object> getResearcherSimilarAuthorTopicLevelMap( Author author, int startPage, int maxresult )
	{
		// researchers list container
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		Map<String, List<String>> similarAuthors = new LinkedHashMap<String, List<String>>();
		similarAuthors = palmAnalytics.getNGrams().runSimilarEntitiesTopicLevel( author.getId().toString(), "C:/Users/Albi/Desktop/", "Authors", 50, 10, 3, false );

		// get the id, degree of similarity, topics proportions
		// put them into the map
		// prepare list of object map containing similarAuthor details
		List<Map<String, Object>> similarAuthorList = new ArrayList<Map<String, Object>>();

		for ( Entry<String, List<String>> similar : similarAuthors.entrySet() )
		{
			if ( !persistenceStrategy.getAuthorDAO().getById( similar.getKey().split( "->" )[0] ).equals( author ) )
			{
			Map<String, Object> similarAuthorMap = new LinkedHashMap<String, Object>();

			// insert the initial basic information
			similarAuthorMap.put( "id", similar.getKey().split( "->" )[0] );
			similarAuthorMap.put( "name", persistenceStrategy.getAuthorDAO().getById( similar.getKey().split( "->" )[0] ).getName() );
			if ( persistenceStrategy.getAuthorDAO().getById( similar.getKey().split( "->" )[0] ).getInstitution() != null )
				similarAuthorMap.put( "affiliation", persistenceStrategy.getAuthorDAO().getById( similar.getKey().split( "->" )[0] ).getInstitution().getName() );
			if ( persistenceStrategy.getAuthorDAO().getById( similar.getKey().split( "->" )[0] ).getPhotoUrl() != null )
				similarAuthorMap.put( "photo", persistenceStrategy.getAuthorDAO().getById( similar.getKey().split( "->" )[0] ).getPhotoUrl() );
			similarAuthorMap.put( "isAdded", persistenceStrategy.getAuthorDAO().getById( similar.getKey().split( "->" )[0] ).isAdded() );
			similarAuthorMap.put( "similarity", similar.getKey().split( "->" )[1] );

			// construct the map for the list of topics
			List<Object> topicleveldetail = new ArrayList<Object>();
			for ( String topic : similar.getValue() )
			{
				Map<String, Object> topicproportions = new LinkedHashMap<String, Object>();
				topicproportions.put( "name", topic.split( "_-_" )[0] );
					topicproportions.put( "value", "" );// Math.round( (
														// Double.parseDouble(
														// topic.split( "_-_"
														// )[1] ) * 100 ) / 100
														// ) );
				topicleveldetail.add( topicproportions );
			}

			similarAuthorMap.put( "topicdetail", topicleveldetail );
			// add into list
			similarAuthorList.add( similarAuthorMap );
			}
		}

		// prepare list of object map containing similarAuthor details
		List<Map<String, Object>> similarAuthorListPaging = new ArrayList<Map<String, Object>>();

		int position = 0;
		for ( Map<String, Object> similarAuthor : similarAuthorList )
		{
			if ( position >= startPage && similarAuthorListPaging.size() < maxresult )
			{
				similarAuthorListPaging.add( similarAuthor );
			}
		}

		// put similarAuthor to responseMap
		responseMap.put( "countTotal", similarAuthorList.size() );
		responseMap.put( "count", similarAuthorListPaging.size() );
		responseMap.put( "similarAuthors", similarAuthorListPaging );

		return responseMap;
	}

@Override
public Map<String, Object> getResearcherSimilarAuthorTopicLevelRevised( Author author, int startPage, int maxresult ) throws NullPointerException
{
	// researchers list container
	Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
	
	// find the list of similar authors
	List<String> similarEntities = new ArrayList<String>();
	similarEntities = palmAnalytics.getNGrams().runSimilarEntities( author.getId().toString(), "C:/Users/Albi/Desktop/", "Authors", 50, 10, 3, false );
	
	List<Map<String, Object>> similarAuthorList = new ArrayList<Map<String, Object>>();
	
	// get the list of words for the author 
	List<String> authortopicWords = new ArrayList<String>();
	for (String entity : similarEntities){
		if(entity.split("->")[0].equals(author.getId()))
			authortopicWords = new ArrayList<String>(palmAnalytics.getNGrams().runweightedTopicComposition(path,"Author-Test", entity.split("->")[0], 10, 10, 10, true, false ).keySet());
	}
	
	// run for each of the entities of the list the weightedTopic Composition
	for (String entity : similarEntities){
		if(!entity.split("->")[0].equals(author.getId())){		
			List<String> similartopicWords = new ArrayList<String>(palmAnalytics.getNGrams().runweightedTopicComposition(path,"Author-Test", entity.split("->")[0], 10, 10, 10, true, false ).keySet());
			
			Map<String, Object> similarAuthorMap = new LinkedHashMap<String, Object>();

			// insert the initial basic information
			similarAuthorMap.put( "id", entity.split("->")[0] );
			similarAuthorMap.put( "name", persistenceStrategy.getAuthorDAO().getById( entity.split("->")[0] ).getName() );
			if ( persistenceStrategy.getAuthorDAO().getById( entity.split("->")[0] ).getInstitution() != null )
				similarAuthorMap.put( "affiliation", persistenceStrategy.getAuthorDAO().getById( entity.split("->")[0] ).getInstitution().getName() );
			if ( persistenceStrategy.getAuthorDAO().getById( entity.split("->")[0] ).getPhotoUrl() != null )
				similarAuthorMap.put( "photo", persistenceStrategy.getAuthorDAO().getById( entity.split("->")[0] ).getPhotoUrl() );
			similarAuthorMap.put( "isAdded", persistenceStrategy.getAuthorDAO().getById( entity.split("->")[0] ).isAdded() );
			
			// return a HashMap with the similar words and similarity degree 
			HashMap<String, Double> similarDetail = comparePhraseTopicLevel(authortopicWords,similartopicWords );
			similarAuthorMap.put( "similarity", similarDetail.entrySet().iterator().next().getValue());
			
			// construct the map for the list of topics
			List<Object> topicleveldetail = new ArrayList<Object>();
			Map<String, Object> topicproportions = new LinkedHashMap<String, Object>();
			topicproportions.put( "name", similarDetail.keySet().toArray()[0] );
			topicproportions.put( "value", "" );
			topicleveldetail.add( topicproportions );
			
			similarAuthorMap.put( "topicdetail", topicleveldetail );
			// add into list
			// check if the similarity is significant 
			// The threshhold is decided heuristically 
			double a = similarDetail.entrySet().iterator().next().getValue();
			if ( a > 0.029){
				// add into list if the similarity 
				similarAuthorList.add( similarAuthorMap );
			}
			else
			{
				continue;
			}
				
		}
	}
	// prepare list of object map containing similarAuthor details
	List<Map<String, Object>> similarAuthorListPaging = new ArrayList<Map<String, Object>>();

	int position = 0;
	for ( Map<String, Object> similarAuthor : similarAuthorList )
	{
		if ( position >= startPage && similarAuthorListPaging.size() < maxresult )
		{
			similarAuthorListPaging.add( similarAuthor );
		}
	}

	// put similarAuthor to responseMap
	responseMap.put( "countTotal", similarAuthorList.size() );
	responseMap.put( "count", similarAuthorListPaging.size() );
	responseMap.put( "similarAuthors", similarAuthorListPaging );
	
	return responseMap;
}

// compare the two authors in word level
private HashMap<String, Double> comparePhraseTopicLevel(List<String> authortopicWords, List<String> similartopicWords) {
	HashMap<String, Double> result = new HashMap<String,Double>();
	String topic = "";
	int count = 0;
	
	for (String authorphrase : authortopicWords){
		for (String similarphrase : similartopicWords){
			if (authorphrase.contains(similarphrase)){
				topic +=  authorphrase + ",";
				count++;
			}
			else if(similarphrase.contains(authorphrase)){
					topic +=  authorphrase + ",";
					count++;
				}
			else
				continue;
		}
	}
	
	String [] topicArray = new HashSet<String>(Arrays.asList(topic.split(","))).toArray(new String[0]);
	String phrase = " ";
	
	for (String str : topicArray){
		phrase +=  str + ",";
		}
	
	if (phrase != null && phrase.length() > 0 && phrase.charAt(phrase.length()-1)==',') {
		phrase = phrase.substring(0, phrase.length()-1);
	    }
		
	
	result.put(topic, (double)topicArray.length/authortopicWords.size());
	
	return result;
}

}

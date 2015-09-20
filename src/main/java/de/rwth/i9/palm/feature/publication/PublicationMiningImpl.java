package de.rwth.i9.palm.feature.publication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.helper.comparator.PublicationTopicByExtractionServiceTypeComparator;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationTopic;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class PublicationMiningImpl implements PublicationMining
{

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Override
	public Map<String, Object> getPublicationExtractedTopicsById( String publicationId )
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// get publication
		Publication publication = persistenceStrategy.getPublicationDAO().getById( publicationId );
		if ( publication == null )
		{
			responseMap.put( "status", "Error - publication not found" );
			return responseMap;
		}

		responseMap.put( "status", "ok" );

		// put publication topic
		List<PublicationTopic> publicationTopics = new ArrayList<PublicationTopic>( publication.getPublicationTopics() );

		if ( publicationTopics.isEmpty() )
		{
			responseMap.put( "status", "Error - publication contain no extracted topics" );
			return responseMap;
		}
		// sort publicationTopic in natural order based on ExtractionServiceType
		Collections.sort( publicationTopics, new PublicationTopicByExtractionServiceTypeComparator() );

		// publicationTopic list for json response
		List<Object> publicationTopicList = new ArrayList<Object>();

		// loop to each PublicationTopic
		for ( PublicationTopic publicationTopic : publicationTopics )
		{
			Map<String, Object> publicationTopicMap = new LinkedHashMap<String, Object>();
			publicationTopicMap.put( "id", publicationTopic.getId() );
			publicationTopicMap.put( "extractionDate", publicationTopic.getExtractionDate() );
			publicationTopicMap.put( "extractor", publicationTopic.getExtractionServiceType().toString() );
			// get the term value
			publicationTopicMap.put( "termvalues", publicationTopic.getTermValues() );

			// add into list
			publicationTopicList.add( publicationTopicMap );
		}

		// put publicationTopicList into responseMap
		responseMap.put( "topics", publicationTopicList );

		return responseMap;
	}

}

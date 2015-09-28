package de.rwth.i9.palm.feature.publication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.helper.comparator.PublicationTopicByExtractionServiceTypeComparator;
import de.rwth.i9.palm.helper.comparator.ValueComparator;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationTopic;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class PublicationMiningImpl implements PublicationMining
{
	private final static Logger log = LoggerFactory.getLogger( PublicationMiningImpl.class );

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Override
	public Map<String, Object> getPublicationExtractedTopicsById( String publicationId, String maxRetrieve )
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// default term retrieved
		int maxTermRetrieve = 20;

		if ( maxRetrieve != null )
		{
			try
			{
				maxTermRetrieve = Integer.parseInt( maxRetrieve );
			}
			catch ( InputMismatchException exception )
			{
				log.error( "Invalid input" );
				maxTermRetrieve = 20;
			}
		}

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
			// sort term value and cut until maxRetrieve
			Map<String, Double> sortedMap = new TreeMap<String, Double>( new ValueComparator( publicationTopic.getTermValues() ) );
			sortedMap.putAll( publicationTopic.getTermValues() );

			List<Object> termValueList = new ArrayList<Object>();
			int mapIndex = 0;
			for ( Map.Entry<String, Double> entry : sortedMap.entrySet() )
			{
				Map<String, Object> termValue = new LinkedHashMap<String, Object>();
				termValue.put( "term", entry.getKey() );
				termValue.put( "value", entry.getValue() );
				termValueList.add( termValue );

				mapIndex++;
				if ( mapIndex >= maxTermRetrieve )
					break;
			}

			publicationTopicMap.put( "termvalues", termValueList );

			// add into list
			publicationTopicList.add( publicationTopicMap );
		}

		// put publicationTopicList into responseMap
		responseMap.put( "topics", publicationTopicList );

		return responseMap;
	}

}

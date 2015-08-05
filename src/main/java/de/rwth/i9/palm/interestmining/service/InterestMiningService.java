package de.rwth.i9.palm.interestmining.service;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationTopic;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Service
public class InterestMiningService
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	public Map<String, Object> getInterestFromAuthor( Author author )
	{

		if ( author.getAuthorInterestProfiles() != null )
		{
			// TODO get and build the author interest for visualization
		}
		else
		{
			// calculate the author interest

			// Map of the entire corpus ( publication information )
			// based on language and year
			Map<String, Map<Integer, String>> languageYearCorpusMap = new HashMap<String, Map<Integer, String>>();

			// First, Categorize publication based on language and year,
			// then get publication topic from publication
			// and get unique interest from publication topic
			// put it into a Map<Language, Map< Year, Set <Interest>>>
			Map<String, Map<Integer, Set<Interest>>> languageYearInterestMap = constructInterestMapByLanguageAndYear( author, languageYearCorpusMap );

			// Second, count occurrence between unique term and entire corpus
			// based on language and year

		}

		// check whether
		return null;
	}

	/**
	 * Categorize publication on specific author based on language and year
	 * 
	 * @param author
	 * @return
	 */
	private Map<String, Map<Integer, Set<Interest>>> constructInterestMapByLanguageAndYear( Author author, Map<String, Map<Integer, String>> languageYearCorpusMap )
	{
		Calendar calendar = Calendar.getInstance();
		Map<String, Map<Integer, Set<Interest>>> languageYearInterestMap = new HashMap<String, Map<Integer, Set<Interest>>>();

		for ( Publication publication : author.getPublications() )
		{
			if ( publication.getPublicationTopics() != null && publication.getLanguage() != null )
			{
				// check whether map with specific language already exist
				if ( languageYearInterestMap.get( publication.getLanguage() ) != null )
				{
					Map<Integer, Set<Interest>> yearInterestMap = languageYearInterestMap.get( publication.getLanguage() );
					// check if on map language already exist map based on year
					calendar.setTime( publication.getPublicationDate() );
					if ( yearInterestMap.get( calendar.get( Calendar.YEAR ) ) != null )
					{
						// map year exist, then get the set and add publication
						Set<Interest> interestSets = yearInterestMap.get( calendar.get( Calendar.YEAR ) );
						// merge 2 set
						interestSets.addAll( constructInterestFromPublication( publication ) );
					}
					else
					{
						// map year not exist, then create new one
						// added interest into set
						Set<Interest> interestSets = constructInterestFromPublication( publication );
						// put into new map
						calendar.setTime( publication.getPublicationDate() );
						yearInterestMap.put( calendar.get( Calendar.YEAR ), interestSets );
					}
				}
				else
				{
					/* For the unique terms map */
					// added interests into set
					Set<Interest> interestSets = constructInterestFromPublication( publication );
					// put publication set to map based on year
					Map<Integer, Set<Interest>> yearInterestMap = new HashMap<Integer, Set<Interest>>();
					calendar.setTime( publication.getPublicationDate() );
					yearInterestMap.put( calendar.get( Calendar.YEAR ), interestSets );
					// put publication year map into language map
					languageYearInterestMap.put( publication.getLanguage(), yearInterestMap );

					/* For the corpus map */
					// String publicationInformation
				}
			}
		}
		return languageYearInterestMap;
	}

	/**
	 * Construct set of interest based on publication topics
	 * @param publication
	 * @return
	 */
	private Set<Interest> constructInterestFromPublication( Publication publication ){
		Map<String, Integer> uniqueTermAndOccurence = new HashMap<String, Integer>();
		Set<Interest> interestSets = new HashSet<Interest>();
		
		// threshold times a term appears from different publication topic extraction
		// e.g. term "learning analytics" appears on term extraction from AlchemyAPI,
		// and YCA, but not in TextWise. The term "learning analytics" thus appears
		// 2 times and will be consider as important term, since the threshold is 2,
		// from number of topic extraction ( 3 ) - 1
		int interestOccurenceThreshold = publication.getPublicationTopics().size();
		if( interestOccurenceThreshold > 2)
			interestOccurenceThreshold--;
		
		// loop throuh all topics
		for( PublicationTopic publicationTopic : publication.getPublicationTopics()){
			Map<String, Double> termValues = publicationTopic.getTermValues();
			
			// Hash
		}
		
		return null;
	}

	/**
	 * Construct text input for publication information ( title, abstract,
	 * keyword, content)
	 * 
	 * @param publication
	 * @return
	 */
	private String getPublicationText( Publication publication )
	{
		String text = publication.getTitle() + " ";
		if ( publication.getAbstractText() != null )
			text += publication.getAbstractText() + " ";
		if ( publication.getKeywords() != null )
			text += publication.getKeywords() + " ";
		if ( publication.getContentText() != null )
			text += publication.getContentText() + " ";
		return text;
	}

	private String normalizeText( String text )
	{
		// to lower case
		// remove all number
		// remove -_()
		text = text.toLowerCase().replaceAll( "\\d", "" ).replaceAll( "[_\\-()]", " " );
		return text;
	}
}

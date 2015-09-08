package de.rwth.i9.palm.interestmining.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import de.rwth.i9.palm.model.ExtractionServiceType;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationTopic;
import de.rwth.i9.palm.utils.Inflector;

public class PublicationClusterHelper
{
	// cluster identifier
	String language;
	int year;

	List<Publication> publications;

	int numberOfWordsOnTitle;
	String concatenatedTitle;

	int numberOfWordsOnKeyword;
	String concatenatedKeyword;

	int numberOfWordsOnAbstract;
	String concatenatedAbstract;

	Map<String, TermDetail> termMap;

	public List<Publication> getPublications()
	{
		return publications;
	}

	public void setPublications( List<Publication> publications )
	{
		for ( Publication publication : publications )
		{
			this.addPublicationAndUpdate( publication );
		}
	}

	public PublicationClusterHelper addPublicationAndUpdate( Publication publication )
	{
		if ( this.publications == null )
			this.publications = new ArrayList<Publication>();
		this.publications.add( publication );

		// update other properties
		this.updateConcatenatedTitle( publication.getTitle() );
		if ( publication.getKeywordText() != null )
			this.updateConcatenatedKeyword( publication.getKeywordText() );
		if ( publication.getAbstractText() != null )
			this.updateConcatenatedAbstract( publication.getAbstractText() );

		return this;
	}

	public int getYear()
	{
		return this.year;
	}

	public void setYear( int year )
	{
		this.year = year;
	}

	public String getLanguage()
	{
		return this.language;
	}

	public void setLangauge( String language )
	{
		this.language = language;
	}

	public int getNumberOfWordsOnTitle()
	{
		return numberOfWordsOnTitle;
	}

	private int updateNumberOfWordsOnTitle( String titleText )
	{
		this.numberOfWordsOnTitle += countWords( titleText );
		return this.numberOfWordsOnTitle;
	}

	public String getConcatenatedTitle()
	{
		return concatenatedTitle;
	}

	private String updateConcatenatedTitle( String titleText )
	{
		// filter and normalize text
		titleText = normalizeText( titleText );

		// concatenated
		this.concatenatedTitle += titleText + " ";

		// update word count
		updateNumberOfWordsOnTitle( titleText );

		return this.concatenatedTitle;
	}

	public int getNumberOfWordsOnKeyword()
	{
		return numberOfWordsOnKeyword;
	}

	private int updateNumberOfWordsOnKeyword( String keywordText )
	{
		this.numberOfWordsOnKeyword += countWords( keywordText );
		return this.numberOfWordsOnKeyword;
	}

	public String getConcatenatedKeyword()
	{
		return concatenatedKeyword;
	}

	private String updateConcatenatedKeyword( String keywordText )
	{
		// filter and normalize text
		keywordText = normalizeText( keywordText );

		// concatenated
		this.concatenatedKeyword += keywordText;

		// update word count
		updateNumberOfWordsOnKeyword( keywordText );

		return this.concatenatedKeyword;
	}

	public int getNumberOfWordsOnAbstract()
	{
		return numberOfWordsOnAbstract;
	}

	private int updateNumberOfWordsOnAbstract( String abstractText )
	{
		this.numberOfWordsOnAbstract += countWords( abstractText );
		return this.numberOfWordsOnAbstract;
	}

	public String getConcatenatedAbstract()
	{
		return concatenatedAbstract;
	}

	private String updateConcatenatedAbstract( String abstractText )
	{
		// filter and normalize text
		abstractText = normalizeText( abstractText );

		// concatenated
		this.concatenatedAbstract += abstractText;

		// update word count
		updateNumberOfWordsOnAbstract( abstractText );

		return this.concatenatedAbstract;
	}

	// utility methods

	// counting the number of words on text
	private int countWords( String text )
	{
		return text.split( "\\s+" ).length;
	}

	// do normalization and filtering on text
	// changed plural form into singular form
	private String normalizeText( String text )
	{
		// to lower case
		// remove all number
		// remove -_()
		// remove s on word end
		text = text.toLowerCase().replaceAll( "\\d", "" ).replaceAll( "[_\\-()]", " " );
		if ( this.getLanguage().equals( "english" ) )
			text = singularizeString( text );
		return text;
	}

	private String singularizeString( String text )
	{
		Inflector inflector = new Inflector();
		return inflector.singularize( text );
	}

	/**
	 * Only run this method after all publication has clustered This method,
	 * calculate frequencies occurred
	 */
	public Map<String, TermDetail> calculateTermProperties()
	{
		if ( this.publications != null )
			return Collections.emptyMap();

		// init termMap
		termMap = new HashMap<String, TermDetail>();

		for ( Publication publication : this.publications )
		{
			if ( publication.getPublicationTopics() != null )
			{
				for ( PublicationTopic publicationTopic : publication.getPublicationTopics() )
				{

					if ( publicationTopic.getTermValues() == null || publicationTopic.getTermValues().isEmpty() )
						continue;

					// get properties needed
					ExtractionServiceType extractionServiceType = publicationTopic.getExtractionServiceType();
					Map<String, Double> termValues = publicationTopic.getTermValues();
					
					// prepare termDetail
					TermDetail termDetail = null;
							
					for ( Map.Entry<String, Double> termValuesEntry : termValues.entrySet() )
					{
						// check if termMap has already contain term
						// if term already exist "more than one term extractor
						// services, produced same terms"
						if ( termMap.get( termValuesEntry.getKey() ) != null )
						{
							termDetail = termMap.get( termValuesEntry.getKey() );
							// update extraction service list
							termDetail.addExtractionServiceType( extractionServiceType );
						}
						// term not exist on map
						else
						{
							// create new termDetail object
							termDetail = new TermDetail();
							String termLabel = termValuesEntry.getKey();

							// add extraction service
							termDetail.addExtractionServiceType( extractionServiceType );
							termDetail.setTermLabel( termLabel );
							termDetail.setTermLength( this.countWords( termLabel ) );

							// calculate frequencies
							termDetail.setFrequencyOnTitle( StringUtils.countMatches( this.getConcatenatedTitle(), termLabel ) );
							termDetail.setFrequencyOnAbstract( StringUtils.countMatches( this.getConcatenatedAbstract(), termLabel ) );
							termDetail.setFrequencyOnKeyword( StringUtils.countMatches( this.getConcatenatedKeyword(), termLabel ) );

							// put into map
							termMap.put( termLabel, termDetail );
						}
					}

				}
			}
		}

		return termMap;
	}

	/**
	 * Class contains term properties
	 * 
	 * @author Sigit
	 *
	 */
	class TermDetail
	{

		private String termLabel;
		private int termLength;
		private Set<ExtractionServiceType> extractionServiceTypes;
		private int frequencyOnTitle;
		private int frequencyOnKeyword;
		private int frequencyOnAbstract;

		public String getTermLabel()
		{
			return termLabel;
		}

		public void setTermLabel( String termLabel )
		{
			this.termLabel = termLabel;
		}

		public int getTermLength()
		{
			return termLength;
		}

		public void setTermLength( int termLength )
		{
			this.termLength = termLength;
		}

		public Set<ExtractionServiceType> getExtractionServiceTypes()
		{
			return extractionServiceTypes;
		}

		public TermDetail addExtractionServiceType( ExtractionServiceType extractionServiceType )
		{
			if ( this.extractionServiceTypes == null )
				this.extractionServiceTypes = new HashSet<ExtractionServiceType>();

			this.extractionServiceTypes.add( extractionServiceType );

			return this;
		}

		public void setExtractionServiceTypes( Set<ExtractionServiceType> extractionServiceTypes )
		{
			this.extractionServiceTypes = extractionServiceTypes;
		}

		public int getFrequencyOnTitle()
		{
			return frequencyOnTitle;
		}

		public void setFrequencyOnTitle( int frequencyOnTitle )
		{
			this.frequencyOnTitle = frequencyOnTitle;
		}

		public int getFrequencyOnKeyword()
		{
			return frequencyOnKeyword;
		}

		public void setFrequencyOnKeyword( int frequencyOnKeyword )
		{
			this.frequencyOnKeyword = frequencyOnKeyword;
		}

		public int getFrequencyOnAbstract()
		{
			return frequencyOnAbstract;
		}

		public void setFrequencyOnAbstract( int frequencyOnAbstract )
		{
			this.frequencyOnAbstract = frequencyOnAbstract;
		}

	}

}

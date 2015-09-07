package de.rwth.i9.palm.interestmining.service;

import java.util.Map;
import java.util.Set;

import de.rwth.i9.palm.model.Publication;

public class PublicationClusterHelper
{
	int year;

	Set<Publication> publications;

	int numberOfPublication;
	int numberOfPublicationWithKeyword;

	int numberOfWordsOnTitle;
	String concatenatedTitle;

	int numberOfWordsOnKeyword;
	String concatenatedKeyword;

	int numberOfWordsOnAbstract;
	String concatenatedAbstract;

	Map<String, Integer> termTitleFreqMap;
	Map<String, Integer> termKeywordFreqMap;
	Map<String, Integer> termAbstractFreqMap;

	public int getYear()
	{
		return year;
	}

	public void setYear( int year )
	{
		this.year = year;
	}

	public Set<Publication> getPublications()
	{
		return publications;
	}

	public void setPublications( Set<Publication> publications )
	{
		this.publications = publications;
	}

	public int getNumberOfPublication()
	{
		return numberOfPublication;
	}

	public void setNumberOfPublication( int numberOfPublication )
	{
		this.numberOfPublication = numberOfPublication;
	}

	public int getNumberOfPublicationWithKeyword()
	{
		return numberOfPublicationWithKeyword;
	}

	public void setNumberOfPublicationWithKeyword( int numberOfPublicationWithKeyword )
	{
		this.numberOfPublicationWithKeyword = numberOfPublicationWithKeyword;
	}

	public int getNumberOfWordsOnTitle()
	{
		return numberOfWordsOnTitle;
	}

	public void setNumberOfWordsOnTitle( int numberOfWordsOnTitle )
	{
		this.numberOfWordsOnTitle = numberOfWordsOnTitle;
	}

	public String getConcatenatedTitle()
	{
		return concatenatedTitle;
	}

	public void setConcatenatedTitle( String concatenatedTitle )
	{
		this.concatenatedTitle = concatenatedTitle;
	}

	public int getNumberOfWordsOnKeyword()
	{
		return numberOfWordsOnKeyword;
	}

	public void setNumberOfWordsOnKeyword( int numberOfWordsOnKeyword )
	{
		this.numberOfWordsOnKeyword = numberOfWordsOnKeyword;
	}

	public String getConcatenatedKeyword()
	{
		return concatenatedKeyword;
	}

	public void setConcatenatedKeyword( String concatenatedKeyword )
	{
		this.concatenatedKeyword = concatenatedKeyword;
	}

	public int getNumberOfWordsOnAbstract()
	{
		return numberOfWordsOnAbstract;
	}

	public void setNumberOfWordsOnAbstract( int numberOfWordsOnAbstract )
	{
		this.numberOfWordsOnAbstract = numberOfWordsOnAbstract;
	}

	public String getConcatenatedAbstract()
	{
		return concatenatedAbstract;
	}

	public void setConcatenatedAbstract( String concatenatedAbstract )
	{
		this.concatenatedAbstract = concatenatedAbstract;
	}

	public Map<String, Integer> getTermTitleFreqMap()
	{
		return termTitleFreqMap;
	}

	public void setTermTitleFreqMap( Map<String, Integer> termTitleFreqMap )
	{
		this.termTitleFreqMap = termTitleFreqMap;
	}

	public Map<String, Integer> getTermKeywordFreqMap()
	{
		return termKeywordFreqMap;
	}

	public void setTermKeywordFreqMap( Map<String, Integer> termKeywordFreqMap )
	{
		this.termKeywordFreqMap = termKeywordFreqMap;
	}

	public Map<String, Integer> getTermAbstractFreqMap()
	{
		return termAbstractFreqMap;
	}

	public void setTermAbstractFreqMap( Map<String, Integer> termAbstractFreqMap )
	{
		this.termAbstractFreqMap = termAbstractFreqMap;
	}

	// getter and setter

}

package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rwth.i9.palm.model.PublicationType;
import de.rwth.i9.palm.model.Source;
import de.rwth.i9.palm.model.SourceProperty;
import de.rwth.i9.palm.model.SourceType;

public class DblpEventCollection extends PublicationCollection
{
	private final static Logger log = LoggerFactory.getLogger( DblpEventCollection.class );

	public DblpEventCollection()
	{
		super();
	}

	/**
	 * get publication list on a venue
	 * 
	 * @param url
	 * @param source
	 * @return
	 * @throws IOException
	 */
	public static Map<String, Object> getPublicationListByVenueUrl( String url, Source source ) throws IOException
	{
		Map<String, Object> venueInformationMap = new LinkedHashMap<String, Object>();

		// Using jsoup java html parser library
		Document document = PublicationCollectionHelper.getDocumentWithJsoup( url, 5000, getDblpCookie( source ) );

		if ( document == null )
			return Collections.emptyMap();

		Element mainContainer = document.select( "#main" ).first();

		if ( mainContainer == null )
		{
			log.info( "Main container not found " );
			return Collections.emptyMap();
		}


		List<Object> publicationList = new ArrayList<Object>();

		// information taken from header
		Map<String, String> headerInformation = null;
		String mainHeaderText = null;

		for ( Element element : mainContainer.children() )
		{
			if ( element.tagName().equals( "header" ) )
			{
				// main header
				if ( element.attr( "id" ) != null && element.attr( "id" ).equals( "headline" ) )
				{
					mainHeaderText = element.text();
				}
				// sub header
				else
				{
					Element h2Header = element.select( "h2" ).first();
					if ( h2Header != null )
					{
						if ( venueInformationMap.get( "type" ).equals( PublicationType.JOURNAL ) )
						{
							headerInformation = new LinkedHashMap<String, String>();
							// typical journal header "Volume 16, Number 4,
							// 2012"
							String[] headerSplit = h2Header.text().trim().split( "," );
							if ( headerSplit.length == 3 )
							{
								if ( headerSplit[0].length() > 9 )
									headerInformation.put( "volume", headerSplit[0].substring( 8 ) );
								if ( headerSplit[1].length() > 9 )
									headerInformation.put( "number", headerSplit[1].substring( 8 ) );

								headerInformation.put( "datePublished", standardizeDblpDate( headerSplit[2].trim() ) );
							}
						}
						else if ( venueInformationMap.get( "type" ).equals( PublicationType.CONFERENCE ) )
						{
							headerInformation.put( "conferenceTheme", h2Header.text().trim() );
						}
					}
				}
			}
			else if ( element.tagName().equals( "ul" ) )
			{
				if ( element.attr( "class" ).equals( "publ-list" ) )
				{
					for ( Element publicationElement : element.children() )
					{
						Map<String, String> publicationDetails = null;
						if ( venueInformationMap.get( "type" ).equals( PublicationType.JOURNAL ) )
						{
							// get publication list from journal
							if ( publicationElement.attr( "class" ).contains( "article" ) )
								publicationDetails = getDblpJournalPublication( publicationElement );
						}
						else if ( venueInformationMap.get( "type" ).equals( PublicationType.CONFERENCE ) )
						{
							// get publication list from conference
							if ( publicationElement.attr( "class" ).contains( "inproceedings" ) )
								publicationDetails = getDblpConferencePublication( publicationElement );
							else if ( publicationElement.attr( "class" ).contains( "editor" ) ){
								publicationDetails = getDblpEditorshipPublication( publicationElement );
								//TODO: get conference theme and date from editorship title
								headerInformation = new LinkedHashMap<String, String>();
								headerInformation.put( "datePublished", getDateFromEdithorshipTitle( publicationDetails.get( "title" ), (String) venueInformationMap.get( "year" ) ) );
							}
						}

						// add header detail into publication information
						if ( headerInformation != null )
							publicationDetails.putAll( headerInformation );

						// put into List
						publicationList.add( publicationDetails );
					}
				}
			}
			else if ( element.tagName().equals( "div" ) )
			{
				if ( element.attr( "id" ) != null && element.attr( "id" ).equals( "breadcrumbs" ) )
				{
					String breadCrumbsLabel = element.text();
					if ( breadCrumbsLabel.toLowerCase().contains( "journals" ) )
						venueInformationMap.put( "type", PublicationType.JOURNAL );
					else if ( breadCrumbsLabel.toLowerCase().contains( "conferences" ) )
						venueInformationMap.put( "type", PublicationType.CONFERENCE );

					// extract header information
					venueInformationMap.putAll( getDblpMainHeaderInformation( mainHeaderText, (PublicationType) venueInformationMap.get( "type" ) ) );
				}
			}
		}

		venueInformationMap.put( "publications", publicationList );

		return venueInformationMap;
	}

	private static Map<String, Object> getDblpMainHeaderInformation( String mainHeaderText, PublicationType publicationType )
	{
		Map<String, Object> mainHeaderInformation = new LinkedHashMap<String, Object>();
		if ( publicationType.equals( PublicationType.JOURNAL ) )
		{
			/// get journal name and volume
			/// e.g. Machine Learning, Volume 87
			String[] mainHeaderTextArray = mainHeaderText.split( "," );
			if ( mainHeaderTextArray.length > 1 )
			{
				mainHeaderInformation.put( "name", mainHeaderTextArray[0].trim() );
				if ( mainHeaderTextArray[1].length() > 7 )
					mainHeaderInformation.put( "volume", mainHeaderTextArray[1].substring( 7 ).trim() );
			}

		}
		else if ( publicationType.equals( PublicationType.CONFERENCE ) )
		{
			/// get conference name, year, city and country
			/// e.g. 7. CSEDU 2015: Lisbon, Portugal
			// first split between conference and location
			String[] mainHeaderTextArray = mainHeaderText.split( ":" );

			if ( mainHeaderTextArray.length == 2 )
			{
				/// get conference name and year
				/// e.g. 7. CSEDU 2015
				String venueNameAndYear = null;
				if ( mainHeaderTextArray[0].contains( "." ) )
				{
					// remove venue number e.g. 7. CSEDU 2015 to CSEDU 2015
					String[] venueNamePart = mainHeaderTextArray[0].split( "\\." );
					if ( venueNamePart.length == 2 )
						venueNameAndYear = venueNamePart[1].trim();
				}
				else
					venueNameAndYear = mainHeaderTextArray[0].trim();

				// get year and name
				if ( venueNameAndYear.length() > 6 )
				{
					mainHeaderInformation.put( "name", venueNameAndYear.substring( 0, venueNameAndYear.length() - 5 ) );
					mainHeaderInformation.put( "year", venueNameAndYear.substring( venueNameAndYear.length() - 4 ) );
				}

				/// get city and country
				/// e.g. Lisbon, Portugal

				// first check, if there is other unnecessary information
				// e.g. Lisbon, Portugal - Workshop
				String venueCityAndCountry = null;
				if ( mainHeaderTextArray[1].contains( "-" ) )
				{
					int dashIndex = mainHeaderTextArray[1].indexOf( " -" );
					venueCityAndCountry = mainHeaderTextArray[1].substring( 0, dashIndex ).trim();
				}
				else
					venueCityAndCountry = mainHeaderTextArray[1].trim();

				// get city and country
				String[] venueCityAndCountryArray = venueCityAndCountry.split( "," );
				if ( venueCityAndCountryArray.length > 1 )
				{
					mainHeaderInformation.put( "city", venueCityAndCountryArray[0].trim() );
					mainHeaderInformation.put( "country", venueCityAndCountryArray[1].trim() );
				}
			}

		}
		return mainHeaderInformation;
	}

	/**
	 * Get publication general information from DBLP
	 * 
	 * @param publicationElement
	 * @param publicationDetails
	 */
	private static void getDblpPublicationInformationInGeneral( Element publicationElement, Map<String, String> publicationDetails )
	{
		// get original source (PDF or a webpage)
		Element sourceElementContainer = publicationElement.select( "nav.publ" ).select( "li" ).first();
		Elements sourceElements = sourceElementContainer.select( "div.body" ).first().select( "a" );
		if ( sourceElements != null && sourceElements.size() > 0 )
		{
			String docUrl = "";
			String doc = "";
			for ( Element sourceElement : sourceElements )
			{
				docUrl += sourceElement.absUrl( "href" ) + " ";
				doc += sourceElement.text().replace( ",", "" ) + ",";
			}
			publicationDetails.put( "doc", doc.substring( 0, doc.length() - 1 ) );
			publicationDetails.put( "doc_url", docUrl.substring( 0, docUrl.length() - 1 ) );
		}

		// get container, where all of information resides
		Element dataElement = publicationElement.select( "div.data" ).first();

		// get list of author in comma separated, together with author link
		Elements authorElements = dataElement.select( "[itemprop=author]" );
		String authorNames = "";
		String authorUrl = "";
		for ( Element authorElement : authorElements )
		{
			authorNames += authorElement.text().replace( ",", " " ) + ",";
			Element authorUrlElement = authorElement.select( "a" ).first();
			if ( authorUrlElement != null )
				authorUrl += authorUrlElement.absUrl( "href" ) + " ";
			else
				authorUrl += "null ";
		}
		publicationDetails.put( "coauthor", authorNames.substring( 0, authorNames.length() - 1 ) );
		publicationDetails.put( "coauthorUrl", authorUrl );

		// other general information
		publicationDetails.put( "title", dataElement.select( "span.title" ).text() );
		publicationDetails.put( "source", SourceType.DBLP.toString() );
	}

	/**
	 * Part of code that extract Publication with type Editorship
	 * 
	 * @param publicationElement
	 * @return
	 * @throws IOException
	 */
	private static Map<String, String> getDblpEditorshipPublication( Element publicationElement ) throws IOException
	{
		Map<String, String> publicationDetails = new LinkedHashMap<String, String>();

		// first, extract general information on DBLP publication
		getDblpPublicationInformationInGeneral( publicationElement, publicationDetails );

		// second, extract specific information which is only available for
		// editorship.
		publicationDetails.put( "type", PublicationType.EDITORSHIP.toString() );
		// get container, where all of information resides
		Element dataElement = publicationElement.select( "div.data" ).first();
		
		publicationDetails.put( "book-series", dataElement.select( "a" ).select( "[itemtype=http://schema.org/BookSeries]" ).text() );
		//publicationDetails.put( "book-page", dataElement.select( "" ).text() );
		publicationDetails.put( "book-publisher", dataElement.select( "[itemprop=publisher]" ).text() );
		publicationDetails.put( "book-date-published", dataElement.select( "[itemprop=datePublished]" ).text() );
		publicationDetails.put( "book-isbn", dataElement.select( "[itemprop=isbn]" ).text() );

		return publicationDetails;
	}

	/**
	 * Part of code that extract Publication with type Conference
	 * 
	 * @param publicationElement
	 * @return
	 * @throws IOException
	 */
	private static Map<String, String> getDblpConferencePublication( Element publicationElement ) throws IOException
	{
		Map<String, String> publicationDetails = new LinkedHashMap<String, String>();

		// first, extract general information on DBLP publication
		getDblpPublicationInformationInGeneral( publicationElement, publicationDetails );

		// second, extract specific information which is only available for
		// conference.
		publicationDetails.put( "type", PublicationType.CONFERENCE.toString() );
		// get container, where all of information resides
		Element dataElement = publicationElement.select( "div.data" ).first();
		publicationDetails.put( "pages", dataElement.select( "[itemprop=pagination]" ).text() );

		return publicationDetails;
	}

	/**
	 * Part of code that extract Publication with type Journal
	 * 
	 * @param publicationElement
	 * @return
	 * @throws IOException
	 */
	private static Map<String, String> getDblpJournalPublication( Element publicationElement ) throws IOException
	{
		Map<String, String> publicationDetails = new LinkedHashMap<String, String>();

		// first, extract general information on DBLP publication
		getDblpPublicationInformationInGeneral( publicationElement, publicationDetails );

		// second, extract specific information which is only available for
		// journal.
		publicationDetails.put( "type", PublicationType.JOURNAL.toString() );

		// get container, where all of information resides
		Element dataElement = publicationElement.select( "div.data" ).first();
		publicationDetails.put( "pages", dataElement.select( "[itemprop=pagination]" ).text() );

		return publicationDetails;
	}

	/**
	 * Convert date on DBLP into standardize format yyyy/M
	 * 
	 * @param stringDate
	 * @return
	 */
	private static String standardizeDblpDate( String stringDate )
	{
		// return Null for false input
		if ( stringDate.length() < 4 )
			return null;
		// only contain year
		if ( stringDate.length() == 4 )
			return stringDate;

		stringDate = stringDate.toLowerCase();

		String month = "1";
		String year = stringDate.substring( stringDate.length() - 4, stringDate.length() );

		// months and quarters in number
		if ( stringDate.startsWith( "feb" ) )
			month = "2";
		else if ( stringDate.startsWith( "mar" ) )
			month = "3";
		else if ( stringDate.startsWith( "apr" ) || stringDate.startsWith( "sec" ) )
			month = "4";
		else if ( stringDate.startsWith( "may" ) )
			month = "5";
		else if ( stringDate.startsWith( "jun" ) )
			month = "6";
		else if ( stringDate.startsWith( "jul" ) || stringDate.startsWith( "thi" ) )
			month = "7";
		else if ( stringDate.startsWith( "aug" ) )
			month = "8";
		else if ( stringDate.startsWith( "sep" ) )
			month = "9";
		else if ( stringDate.startsWith( "oct" ) || stringDate.startsWith( "fou" ) )
			month = "10";
		else if ( stringDate.startsWith( "nov" ) )
			month = "11";
		else if ( stringDate.startsWith( "dec" ) )
			month = "12";

		return year + "/" + month;
	}

	/**
	 * Find string month on title and return in M format
	 * 
	 * @param editorshipTitle
	 * @return
	 */
	private static String getDateFromEdithorshipTitle( String editorshipTitle, String year )
	{
		// e.g. 23-25 May, 2015
		// e.g. 31. August - 3. September 2014
		// e.g. September 1, 2015
		String month = null;
		String day = null;
		editorshipTitle = editorshipTitle.toLowerCase();

		String[] months = { "january", "february", "march", "april", "may", "june", "july", "august", " september", "october", "november", "december" };

		for ( int i = 0; i < months.length; i++ )
		{
			int monthIndex = editorshipTitle.indexOf( months[i] );
			if ( monthIndex > -1 )
			{
				if ( monthIndex < 10 )
					continue;

				// get month
				month = Integer.toString( i + 1 );
				// get day
				String date = editorshipTitle.substring( monthIndex - 6, monthIndex + months[i].length() + 3 );
				// remove any non number and multiple spaces
				date = date.replaceAll( "[^0-9]", " " ).replaceAll( " +", " " );
				String[] dateArray = date.split( " " );
				if ( dateArray.length > 0 )
				{
					for ( int j = 0; j < dateArray.length; j++ )
					{
						if ( !dateArray[j].equals( "" ) && dateArray[j].length() < 3 )
						{
							day = dateArray[j];
							break;
						}
					}
				}
			}
		}

		if ( month != null && day != null )
			return year + "/" + month + "/" + day;
		else if ( month != null && day == null )
			return year + "/" + month;

		return year;
	}

	/**
	 * DBLP cache, important for select correct DBLP page before crawling
	 * 
	 * @return
	 */
	private static Map<String, String> getDblpCookie( Source source )
	{
		Map<String, String> cookies = new HashMap<String, String>();

		if ( source != null )
			for ( SourceProperty sourceProperty : source.getSourceProperties() )
			{
				if ( sourceProperty.getMainIdentifier().equals( "cookie" ) && sourceProperty.isValid() )
					cookies.put( sourceProperty.getSecondaryIdentifier(), sourceProperty.getValue() );
			}
		return cookies;
	}

	/**
	 * There is an API available, but not really useful for getting the complete
	 * information of author and venue
	 * http://www.dblp.org/search/api/?q=ulrik%20schroeder&h=1000&c=4&f=0&format
	 * =json
	 */
}

package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rwth.i9.palm.model.PublicationType;
import de.rwth.i9.palm.model.SourceType;

public class DblpPublicationCollection extends PublicationCollection
{
	private final static Logger log = LoggerFactory.getLogger( DblpPublicationCollection.class );

	public DblpPublicationCollection()
	{
		super();
	}

	/**
	 * Get possible author
	 * 
	 * @param authorName
	 * @return
	 * @throws IOException
	 */
	public static List<Map<String, String>> getListOfAuthors( String authorName ) throws IOException
	{
		List<Map<String, String>> authorList = new ArrayList<Map<String, String>>();

		String url = "http://dblp.uni-trier.de/search/author?q=" + authorName.replace( " ", "+" );
		// Using jsoup java html parser library
		Document document = PublicationCollectionHelper.getDocumentWithJsoup( url, 5000, getDblpCookie() );

		if ( document == null )
			return Collections.emptyList();

		// find out page is author page or search page
		String pageTitle = document.select( "title" ).text();
		if ( pageTitle.toLowerCase().contains( "author search" ) )
		{

			Element authorContainer = document.select( "header.nowrap" ).first();

			Elements authorListNodes = authorContainer.nextElementSibling().select( "ul" ).first().select( "li" );

			if ( authorListNodes.size() == 0 )
			{
				log.info( "No author with name '{}' with selector '{}' on CiteSeerX '{}'", authorName, HtmlSelectorConstant.CSX_AUTHOR_LIST, url );
				return Collections.emptyList();
			}

			// if the authors is present
			for ( Element authorListNode : authorListNodes )
			{
				Map<String, String> eachAuthorMap = new LinkedHashMap<String, String>();
				String name = authorListNode.select( "a" ).first().text();
				// get author name
				eachAuthorMap.put( "name", name );
				// set source
				eachAuthorMap.put( "source", SourceType.DBLP.toString() );
				// get author url
				eachAuthorMap.put( "url", authorListNode.select( "a" ).first().absUrl( "href" ) );

				authorList.add( eachAuthorMap );
			}
		}
		else
		{
			Map<String, String> eachAuthorMap = new LinkedHashMap<String, String>();

			// e.g:http://dblp.uni-trier.de/pers/hd/c/Chatti:Mohamed_Amine?q=mohamed+amin+chatti
			String[] urlAuthorQuery = document.baseUri().split( "\\?" );

			// e.g:http://dblp.uni-trier.de/pers/hd/c/Chatti:Mohamed_Amine
			String[] urlAuthor = urlAuthorQuery[0].split( "/" );

			// Chatti:Mohamed_Amine
			String[] authorSplitName = urlAuthor[urlAuthor.length - 1].split( ":" );

			String firstName = authorSplitName[1].replace( "_", " " ).toLowerCase();
			String lastName = authorSplitName[0].toLowerCase();

			// get author name
			eachAuthorMap.put( "name", firstName + " " + lastName );
			eachAuthorMap.put( "lastName", lastName );
			eachAuthorMap.put( "firstName", firstName );
			// set source
			eachAuthorMap.put( "source", SourceType.DBLP.toString() );
			// get author url
			eachAuthorMap.put( "url", urlAuthorQuery[0] );

			authorList.add( eachAuthorMap );
		}

		return authorList;
	}

	/**
	 * get author page and publication list
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static List<Map<String, String>> getPublicationListByAuthorUrl( String url ) throws IOException
	{
		List<Map<String, String>> publicationMapLists = new ArrayList<Map<String, String>>();

			// Using jsoup java html parser library
		Document document = PublicationCollectionHelper.getDocumentWithJsoup( url, 5000, getDblpCookie() );

		if ( document == null )
			return Collections.emptyList();

		Element publicationContainer = document.select( "#publ-section" ).first();

		if ( publicationContainer == null )
		{
			log.info( "No publication found " );
			return Collections.emptyList();
		}

		// get publication categories
		Elements publicationSections = publicationContainer.select( "div.hideable" );

		for ( Element publicationSection : publicationSections )
		{

			Element sectionHeader = publicationSection.select( "header" ).first();

			if ( sectionHeader.attr( "id" ).equals( "book" ) )
			{

				Elements publicationList = publicationSection.select( "ul.publ-list li.book" );

				for ( Element eachPublication : publicationList )
				{
					Map<String, String> publicationDetails = new LinkedHashMap<String, String>();
					publicationDetails.put( "type", PublicationType.BOOK.toString() );

					Element pdfElement = publicationList.select( "nav.publ" ).select( "li" ).first();
					if ( pdfElement.select( "div.head" ).select( "a" ).first() != null )
					{
						publicationDetails.put( "doc_url", pdfElement.select( "div.head" ).select( "a" ).first().absUrl( "href" ) );
						publicationDetails.put( "doc", pdfElement.select( "div.body" ).select( "a" ).text() );
					}
					Element dataElement = eachPublication.select( "div.data" ).first();

					Elements authorElements = dataElement.select( "[itemprop=author]" );
					String authorNames = "";
					for ( Element authorElement : authorElements )
						authorNames += authorElement.text() + ", ";
					publicationDetails.put( "coauthor", authorNames.substring( 0, authorNames.length() - 2 ) );

					publicationDetails.put( "title", dataElement.select( "span.title" ).text() );
					publicationDetails.put( "year", dataElement.select( "[itemprop=datePublished]" ).text() );
					if ( dataElement.select( "[itemprop=pagination]" ) != null )
						publicationDetails.put( "page", dataElement.select( "[itemprop=pagination]" ).text() );

					// other information
					String otherInformation = "";
					for ( Node child : dataElement.childNodes() )
					{
						if ( child instanceof TextNode )
						{
							otherInformation += ( (TextNode) child ).text() + " ";
						}
					}

					otherInformation = otherInformation.replaceAll( "[\\.:]*", "" ).trim();
					if ( !otherInformation.equals( "" ) )
						publicationDetails.put( "otherInformation", otherInformation );

					publicationDetails.put( "source", SourceType.DBLP.toString() );

					publicationMapLists.add( publicationDetails );
				}

			}
			else if ( sectionHeader.attr( "id" ).equals( "article" ) )
			{
				Elements publicationList = publicationSection.select( "ul.publ-list li.article" );

				for ( Element eachPublication : publicationList )
				{
					Map<String, String> publicationDetails = new LinkedHashMap<String, String>();
					publicationDetails.put( "type", PublicationType.JOURNAL.toString() );

					Element pdfElement = publicationList.select( "nav.publ" ).select( "li" ).first();
					if ( pdfElement.select( "div.head" ).select( "a" ).first() != null )
					{
						publicationDetails.put( "doc_url", pdfElement.select( "div.head" ).select( "a" ).first().absUrl( "href" ) );
						publicationDetails.put( "doc", pdfElement.select( "div.body" ).select( "a" ).text() );
					}
					Element dataElement = eachPublication.select( "div.data" ).first();

					Elements authorElements = dataElement.select( "[itemprop=author]" );
					String authorNames = "";
					for ( Element authorElement : authorElements )
						authorNames += authorElement.text() + ", ";
					publicationDetails.put( "coauthor", authorNames.substring( 0, authorNames.length() - 2 ) );

					publicationDetails.put( "title", dataElement.select( "span.title" ).text() );
					publicationDetails.put( "year", dataElement.select( "[itemprop=datePublished]" ).text() );
					publicationDetails.put( "source", SourceType.DBLP.toString() );

					publicationMapLists.add( publicationDetails );
				}

			}
			else if ( sectionHeader.attr( "id" ).equals( "inproceedings" ) )
			{

				Elements publicationList = publicationSection.select( "ul.publ-list li.inproceedings" );

				for ( Element eachPublication : publicationList )
				{
					Map<String, String> publicationDetails = new LinkedHashMap<String, String>();
					publicationDetails.put( "type", PublicationType.CONFERENCE.toString() );

					Element pdfElement = publicationList.select( "nav.publ" ).select( "li" ).first();
					if ( pdfElement.select( "div.head" ).select( "a" ).first() != null )
					{
						publicationDetails.put( "doc_url", pdfElement.select( "div.head" ).select( "a" ).first().absUrl( "href" ) );
						publicationDetails.put( "doc", pdfElement.select( "div.body" ).select( "a" ).text() );
					}
					Element dataElement = eachPublication.select( "div.data" ).first();

					Elements authorElements = dataElement.select( "[itemprop=author]" );
					String authorNames = "";
					for ( Element authorElement : authorElements )
						authorNames += authorElement.text() + ", ";
					publicationDetails.put( "coauthor", authorNames.substring( 0, authorNames.length() - 2 ) );

					publicationDetails.put( "title", dataElement.select( "span.title" ).text() );
					publicationDetails.put( "year", dataElement.select( "[itemprop=datePublished]" ).text() );
					publicationDetails.put( "source", SourceType.DBLP.toString() );

					publicationMapLists.add( publicationDetails );
				}
			}
		}
		return publicationMapLists;
	}

	public static Map<String, String> getPublicationDetailByPublicationUrl( String url ) throws IOException
	{
		Map<String, String> publicationDetailMaps = new LinkedHashMap<String, String>();

		Document document = null;

		try
		{
			// Using jsoup java html parser library
			document = Jsoup.connect( url ).userAgent( "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21" ).timeout( 5000 ).get();
		}
		catch ( Exception e )
		{
			return Collections.emptyMap();
		}
		if ( document == null )
			return Collections.emptyMap();

		Elements publicationDetailHeader = document.select( HtmlSelectorConstant.CSX_PUBLICATION_DETAIL_HEADER );

		if ( publicationDetailHeader.size() == 0 )
		{
			log.info( "No publication detail found " );
			return Collections.emptyMap();
		}

		publicationDetailMaps.put( "title", publicationDetailHeader.select( "h2" ).first().text() );
		publicationDetailMaps.put( "doc", publicationDetailHeader.select( "a" ).first().text() );
		publicationDetailMaps.put( "doc_url", publicationDetailHeader.select( "a" ).first().absUrl( "href" ) );

		String coAuthor = publicationDetailHeader.select( HtmlSelectorConstant.CSX_PUBLICATION_DETAIL_COAUTHOR ).text();
		if ( coAuthor.startsWith( "by" ) )
			coAuthor = coAuthor.substring( 2 );

		coAuthor = coAuthor.replaceAll( "[^\\x00-\\x7F]", " " ).trim();
		publicationDetailMaps.put( "coauthor", coAuthor );

		Elements venue = publicationDetailHeader.select( HtmlSelectorConstant.CSX_PUBLICATION_DETAIL_VENUE );

		if ( venue != null && venue.select( "td" ).size() > 1 )
			publicationDetailMaps.put( "venue", venue.select( "td" ).get( 1 ).text() );

		publicationDetailMaps.put( "abstract", document.select( HtmlSelectorConstant.CSX_PUBLICATION_DETAIL_ABSTRACT ).select( "p" ).text() );

		return publicationDetailMaps;
	}
	
	private static Map<String, String> getDblpCookie()
	{
		Map<String, String> cookies = new HashMap<String, String>();
		cookies.put( "dblp-search-mode", "c" );
		cookies.put( "dblp-view", "t" );
		return cookies;
	}

	/**
	 * There is an API available, but not really useful for getting the complete
	 * information of author and venue
	 * http://www.dblp.org/search/api/?q=ulrik%20schroeder&h=1000&c=4&f=0&format
	 * =json
	 */
}

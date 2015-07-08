package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CiteseerXPublicationCollection extends PublicationCollection
{

	private final static Logger log = LoggerFactory.getLogger( CiteseerXPublicationCollection.class );

	public CiteseerXPublicationCollection()
	{
		super();
	}

	public static List<Map<String, String>> getListOfAuthors( String authorName ) throws IOException
	{
		List<Map<String, String>> authorList = new ArrayList<Map<String, String>>();

		String url = "http://citeseerx.ist.psu.edu/search?q=" + authorName.replace( " ", "+" ) + "&submit=Search&uauth=1&sort=ndocs&t=auth";
		// Using jsoup java html parser library
		Document document = null;
		try
		{
			document = Jsoup.connect( url )
					.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
	                .timeout(5000)
					.get();
		}
		catch ( Exception e )
		{
			return Collections.emptyList();
		}
		if( document == null )
			return Collections.emptyList();

		Elements authorListNodes = document.select( HtmlSelectorConstant.CSX_AUTHOR_LIST );

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
			eachAuthorMap.put( "source", "citeseerx" );
			// get author url
			eachAuthorMap.put( "url", authorListNode.select( "a" ).first().absUrl( "href" ) );
			// get author photo
			eachAuthorMap.put( "aliases", authorListNode.select( HtmlSelectorConstant.CSX_AUTHOR_ROW_DETAIL ).select( "tr" ).first().select( "td" ).get( 1 ).text() );
			// get author affiliation
			eachAuthorMap.put( "affiliation", authorListNode.select( HtmlSelectorConstant.CSX_AUTHOR_ROW_DETAIL ).select( "tr" ).get( 1 ).select( "td" ).get( 1 ).text() );

			authorList.add( eachAuthorMap );
		}

		return authorList;
	}

	public static List<Map<String, String>> getPublicationListByAuthorUrl( String url ) throws IOException
	{
		List<Map<String, String>> publicationMapLists = new ArrayList<Map<String, String>>();

		Document document = null;
		
		try
		{
			// Using jsoup java html parser library
			document = Jsoup.connect( url + "&list=full" )
				.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
                .timeout(5000)
				.get();
		}
		catch ( Exception e )
		{
			return Collections.emptyList();
		}
		if( document == null )
			return Collections.emptyList();
		
		Elements publicationRowList = document.select( HtmlSelectorConstant.CSX_PUBLICATION_ROW_LIST );

		if ( publicationRowList.size() == 0 )
		{
			log.info( "No publication found " );
			return Collections.emptyList();
		}

		int index = 0;
		for ( Element eachPublicationRow : publicationRowList )
		{
			if( index  > 0 ){
				Map<String, String> publicationDetails = new LinkedHashMap<String, String>();

				String noCitation = eachPublicationRow.select( "td" ).first().text();
				if ( !noCitation.equals( "" ) )
					publicationDetails.put( "nocitation", noCitation );

				// set source
				publicationDetails.put( "source", "citeseerx" );
				
				publicationDetails.put( "url", eachPublicationRow.select( "a" ).first().absUrl( "href" ) );
				
				String title = eachPublicationRow.select( "a" ).first().text();
				publicationDetails.put( "title", title );

				String venueAndYear = eachPublicationRow.select( "td" ).get( 1 ).text().substring( title.length() );
				if ( venueAndYear.length() > 5 )
				{
					if ( venueAndYear.substring( venueAndYear.length() - 4 ).matches( "^\\d{4}" ) )
					{
						publicationDetails.put( "year", venueAndYear.substring( venueAndYear.length() - 4 ) );
						if ( venueAndYear.length() > 10 )
							publicationDetails.put( "venue", venueAndYear.substring( 0, venueAndYear.length() - 4 ).replace( "-", "" ).trim() );
					}
					else
						publicationDetails.put( "venue", venueAndYear.replace( "-", "" ).trim() );
				}
	
				publicationMapLists.add( publicationDetails );
				}
			index++;
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
			document = Jsoup.connect( url )
				.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
                .timeout(5000)
				.get();
		}
		catch ( Exception e )
		{
			return Collections.emptyMap();
		}
		if( document == null )
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

		if ( venue != null && venue.select( "td" ).size()>1)
			publicationDetailMaps.put( "venue", venue.select( "td" ).get( 1 ).text() );

		publicationDetailMaps.put( "abstract", document.select( HtmlSelectorConstant.CSX_PUBLICATION_DETAIL_ABSTRACT ).select( "p" ).text() );

		return publicationDetailMaps;
	}
}
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

public class DblpPublicationCollection extends PublicationCollection
{
	private final static Logger log = LoggerFactory.getLogger( DblpPublicationCollection.class );

	public DblpPublicationCollection()
	{
		super();
	}

	public static Map<String, Map<String, String>> getListOfAuthors( String authorName ) throws IOException
	{
		Map<String, Map<String, String>> authorMaps = new LinkedHashMap<String, Map<String, String>>();

		String url = "http://dblp.uni-trier.de/search/author?q=" + authorName.replace( " ", "+" );
		// Using jsoup java html parser library
		Document document = Jsoup.connect( url ).get();

		Elements authorListNodes = document.select( HtmlSelectorConstant.GS_AUTHOR_LIST_CONTAINER );

		if ( authorListNodes.size() == 0 )
		{
			log.info( "No author with name '{}' with selector '{}' on google scholar '{}'", authorName, HtmlSelectorConstant.GS_AUTHOR_LIST_CONTAINER, url );
			return Collections.emptyMap();
		}

		// if the authors is present
		for ( Element authorListNode : authorListNodes )
		{
			Map<String, String> eachAuthorMap = new LinkedHashMap<String, String>();
			String name = authorListNode.select( HtmlSelectorConstant.GS_AUTHOR_LIST_NAME ).text();
			// get author url
			eachAuthorMap.put( "url", authorListNode.select( "a" ).first().absUrl( "href" ) );
			// get author name
			eachAuthorMap.put( "name", name );
			// get author photo
			eachAuthorMap.put( "photo", authorListNodes.select( "img" ).first().absUrl( "src" ) );
			// get author affiliation
			eachAuthorMap.put( "affiliation", authorListNodes.select( HtmlSelectorConstant.GS_AUTHOR_LIST_AFFILIATION ).html() );

			authorMaps.put( name, eachAuthorMap );
		}

		return authorMaps;
	}

	public static List<Map<String, String>> getPublicationListByAuthorUrl( String url ) throws IOException
	{
		List<Map<String, String>> publicationMapLists = new ArrayList<Map<String, String>>();

		// Using jsoup java html parser library
		Document document = Jsoup.connect( url ).get();

		Elements publicationRowList = document.select( HtmlSelectorConstant.GS_PUBLICATION_ROW_LIST );

		if ( publicationRowList.size() == 0 )
		{
			log.info( "Np publication found " );
			return Collections.emptyList();
		}

		for ( Element eachPublicationRow : publicationRowList )
		{
			Map<String, String> publicationDetails = new LinkedHashMap<String, String>();
			publicationDetails.put( "url", eachPublicationRow.select( "a" ).first().absUrl( "href" ) );
			publicationDetails.put( "title", eachPublicationRow.select( "a" ).first().text() );
			publicationDetails.put( "coauthor", eachPublicationRow.select( HtmlSelectorConstant.GS_PUBLICATION_COAUTHOR_AND_VENUE ).first().text() );
			publicationDetails.put( "venue", eachPublicationRow.select( HtmlSelectorConstant.GS_PUBLICATION_COAUTHOR_AND_VENUE ).get( 1 ).text() );
			publicationDetails.put( "nocitation", eachPublicationRow.select( HtmlSelectorConstant.GS_PUBLICATION_NOCITATION ).text() );
			publicationDetails.put( "year", eachPublicationRow.select( HtmlSelectorConstant.GS_PUBLICATION_YEAR ).text() );

			publicationMapLists.add( publicationDetails );
		}

		return publicationMapLists;
	}
}

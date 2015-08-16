package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlPublicationCollection
{
	private final static Logger log = LoggerFactory.getLogger( HtmlPublicationCollection.class );

	/**
	 * Get keyword and abstract of a publication from any HTML page
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static Map<String, String> getPublicationInformationFromHtmlPage( String url ) throws IOException
	{
		Map<String, String> publicationDetailMaps = new LinkedHashMap<String, String>();

		// Using jsoup java html parser library
		Document document = PublicationCollectionHelper.getDocumentWithJsoup( url, 10000 );

		if ( document == null )
			return Collections.emptyMap();

		// Special case
		if ( url.contains( "http://ieeexplore.ieee.org/" ) )
		{
			Element elementOfInterest = document.select( "div.article" ).first();
			if ( elementOfInterest != null )
			{
				publicationDetailMaps.put( "abstract", elementOfInterest.text() );
			}
		}
		// General case
		else
		{
			Elements elements = document.body().select( "*" );

			// find either keyword or abstract header
			Element elementOfInterest = null;
			String elementOfInterestType = null;
			for ( Element element : elements )
			{
				String elementText = element.text().toLowerCase();
				if ( elementText.length() < 10 )
				{
					if ( elementText.contains( "keyword" ) )
					{
						elementOfInterest = element;
						elementOfInterestType = "keyword";
						break;
					}
					if ( elementText.contains( "abstract" ) || elementText.contains( "summary" ) )
					{
						elementOfInterest = element;
						elementOfInterestType = "abstract";
						break;
					}
				}
			}

			if ( elementOfInterest == null )
				return Collections.emptyMap();

			int numberOfCheckedSiblings = 8;
			int elementLevel = 0;
			boolean keywordFound = false;
			boolean abstractFound = false;

			for ( int i = 0; i < numberOfCheckedSiblings; i++ )
			{
				// change element pointer to next sibling
				if ( elementOfInterest.nextElementSibling() != null )
					elementOfInterest = elementOfInterest.nextElementSibling();
				else
				{
					// level up until next sibling not null
					while ( elementOfInterest.parent() != null && elementOfInterest.nextElementSibling() == null )
					{
						elementOfInterest = elementOfInterest.parent();
						elementLevel++;
					};
					// select next parent
					elementOfInterest = elementOfInterest.nextElementSibling();
					// level down until at the same level before level up
					while ( elementOfInterest.childNodes() != null && elementLevel > 0 )
					{
						try
						{
							elementOfInterest = elementOfInterest.child( 0 );
						}
						catch ( Exception e )
						{
							break;
						}
						elementLevel--;
					};
				}

				// get text
				String elementText = elementOfInterest.text();

				// check for keyword
				if ( elementOfInterestType.equals( "keyword" ) && !keywordFound )
				{
					// just check any text that contain text longer than 20
					// character
					if ( elementText.length() > 8 )
					{
						if ( publicationDetailMaps.get( "keyword" ) != null )
							publicationDetailMaps.put( "keyword", publicationDetailMaps.get( "keyword" ) + ", " + elementText );
						else
							publicationDetailMaps.put( "keyword", elementText );

						if ( elementOfInterest.nextElementSibling() == null || elementText.length() > 40 )
							keywordFound = true;
					}
					else
					{
						if ( elementText.toLowerCase().contains( "abstract" ) )
							elementOfInterestType = "abstract";
						// special case http://www.computer.org/, keyword null
						else if ( elementText.toLowerCase().equals( "null" ) )
							break;
					}
				}

				// check for abstract
				else if ( elementOfInterestType.equals( "abstract" ) && !abstractFound )
				{
					// just check any text that contain text longer than 100
					// character
					if ( elementText.length() > 100 )
					{
						publicationDetailMaps.put( "abstract", elementText );
						abstractFound = true;
					}
					else
					{
						if ( elementText.toLowerCase().contains( "keyword" ) )
							elementOfInterestType = "keyword";
					}

				}
				else if ( elementOfInterestType.equals( "keyword" ) && keywordFound )
				{
					if ( elementText.length() < 20 )
						if ( elementText.toLowerCase().contains( "abstract" ) )
							elementOfInterestType = "abstract";
				}
				else if ( elementOfInterestType.equals( "abstract" ) && abstractFound )
				{
					if ( elementText.length() < 20 )
						if ( elementText.toLowerCase().contains( "keyword" ) || elementText.toLowerCase().contains( "index terms" ) )
							elementOfInterestType = "keyword";
				}

				// both keyword and abstract found
				if ( keywordFound && abstractFound )
					break;

			}
		}

		return publicationDetailMaps;
	}
}

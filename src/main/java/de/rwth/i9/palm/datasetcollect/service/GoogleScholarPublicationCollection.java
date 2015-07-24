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
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleScholarPublicationCollection extends PublicationCollection
{
	private final static Logger log = LoggerFactory.getLogger( GoogleScholarPublicationCollection.class );

	public GoogleScholarPublicationCollection()
	{
		super();
	}

	public static List<Map<String, String>> getListOfAuthors( String authorName ) throws IOException
	{
		List<Map<String, String>> authorList = new ArrayList<Map<String, String>>();

		String url = "https://scholar.google.com/citations?view_op=search_authors&mauthors=" + authorName.replace( " ", "-" );
		// Using jsoup java html parser library
		Document document = null;

		Map<String, String> cookies = new HashMap<String, String>();
		//cookies.put( "APISID", "C3qs0OXHzPeJNxxT/At3gzfzcBTP87JOEM" );
		//cookies.put( "CONSENT", "YES+DE.en+20150705-15-0" );
		//cookies.put( "GMAIL_RTT", "37" );
		cookies.put( "GOOGLE_ABUSE_EXEMPTION", "ID=964f5a83b97f292a:TM=1437640196:C=c:IP=95.223.161.25-:S=APGng0vCH0HfwpnqD8P5QDwXNaSNQvK9tQ" );
		cookies.put( "GSP", "LM=1437638747:S=yGIL9JRwNlNlm4t5" );
		//cookies.put( "HSID", "AhDcQNKXGhrXqB3le" );
		cookies.put( "NID", "69=c4S-xS8vl9KJw4QfmFYZwY5NpTpaACuSa2hGMQALMhf7Xz6XH4B8JuyMs3hO5M7A1x003toBv_Syxhgwq_gsXnM3KvyXXN2xvYA-8SsiWSjqYapc4RtWTO1an7QGmsOl" );
		//cookies.put( "PREF", "ID=1111111111111111:FF=0:LD=en:TM=1432735603:LM=1435079443:S=bbCHTr3G7UUpxJzf" );
		//cookies.put( "SAPISID", "4ntbRtMKiBq1VQtx/AEl6RrM-kZjyLwUme" );
		//cookies.put( "SID", "DQAAAAcBAADNotp_odfLi0c7wz1_nkP08ZWRbmW4BOAzsqRfec9zOOPHLHP-lCK0g1V37rtooroDTYkre4rc8tNP3A_qDGYjtog0Ex7SjTd-wkEL3NNuVROsEQmfwHcztQ9bQ7mFbVReNSo-UXehZFiEwODVTrm-QTW-mnMRUjEN8Li0ceuXnS1G1r2Q2Fzbq5aPZeEX5MHHJaClgS7n5hN3QvPKQXIkA8v_ZOrrJLRxHiAWe5vBd7nTrWvNE_fw-R3eNpanCQXxXyZDJsc-69DVGPdqrFz-LaMRhbCOkAWwMZah9mpF2kSgHHzFR-S319RlAfKYfVdpQJilomIApYCidOp9fxHgKXj44Gcq98NobZ3P5gVCmg	" );
		//cookies.put( "SSID", "AmXTSbUMtFsE_VntY" );

//		try
//		{
			// Using jsoup java html parser library
			document = Jsoup
					.connect( url )
					.userAgent( "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.130 Safari/537.36" )
.header( "Accept", "Accept:text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8" ).cookies( cookies )
					.timeout( 5000 ).get();
//		}
//		catch ( Exception e )
//		{
//			return Collections.emptyList();
//		}
		if ( document == null )
			return Collections.emptyList();
		Elements authorListNodes = document.select( HtmlSelectorConstant.GS_AUTHOR_LIST_CONTAINER );

		if ( authorListNodes.size() == 0 )
		{
			log.info( "No author with name '{}' with selector '{}' on google scholar '{}'", authorName, HtmlSelectorConstant.GS_AUTHOR_LIST_CONTAINER, url );
			return Collections.emptyList();
		}

		// if the authors is present
		for ( Element authorListNode : authorListNodes )
		{
			Map<String, String> eachAuthorMap = new LinkedHashMap<String, String>();
			String name = authorListNode.select( HtmlSelectorConstant.GS_AUTHOR_LIST_NAME ).text();
			// get author name
			eachAuthorMap.put( "name", name );
			// set source
			eachAuthorMap.put( "source", "googlescholar" );
			// get author url
			eachAuthorMap.put( "url", authorListNode.select( "a" ).first().absUrl( "href" ) );
			// get author photo
			String photoUrl = authorListNode.select( "img" ).first().absUrl( "src" );
			if ( !photoUrl.contains( "avatar_scholar" ) )
				eachAuthorMap.put( "photo", photoUrl );
			// get author affiliation
			eachAuthorMap.put( "affiliation", authorListNode.select( HtmlSelectorConstant.GS_AUTHOR_LIST_AFFILIATION ).html().replace( "&#x2026;", "" ).trim() );
			
			String citedBy = authorListNode.select( HtmlSelectorConstant.GS_AUTHOR_LIST_NOCITATION ).html();
			if( citedBy != null && citedBy.length() > 10)
				eachAuthorMap.put( "citedby", citedBy.substring( "Cited by".length() ).trim() );

			authorList.add( eachAuthorMap );
		}

		return authorList;
	}

	public static List<Map<String, String>> getPublicationListByAuthorUrl( String url ) throws IOException
	{
		List<Map<String, String>> publicationMapLists = new ArrayList<Map<String, String>>();

		// Using jsoup java html parser library
		Document document = null;

		Map<String, String> cookies = new HashMap<String, String>();
		//cookies.put( "APISID", "C3qs0OXHzPeJNxxT/At3gzfzcBTP87JOEM" );
		//cookies.put( "CONSENT", "YES+DE.en+20150705-15-0" );
		//cookies.put( "GMAIL_RTT", "37" );
		cookies.put( "GOOGLE_ABUSE_EXEMPTION", "ID=964f5a83b97f292a:TM=1437640196:C=c:IP=95.223.161.25-:S=APGng0vCH0HfwpnqD8P5QDwXNaSNQvK9tQ" );
		cookies.put( "GSP", "LM=1437638747:S=yGIL9JRwNlNlm4t5" );
		//cookies.put( "HSID", "AhDcQNKXGhrXqB3le" );
		cookies.put( "NID", "69=c4S-xS8vl9KJw4QfmFYZwY5NpTpaACuSa2hGMQALMhf7Xz6XH4B8JuyMs3hO5M7A1x003toBv_Syxhgwq_gsXnM3KvyXXN2xvYA-8SsiWSjqYapc4RtWTO1an7QGmsOl" );
		//cookies.put( "PREF", "ID=1111111111111111:FF=0:LD=en:TM=1432735603:LM=1435079443:S=bbCHTr3G7UUpxJzf" );
		//cookies.put( "SAPISID", "4ntbRtMKiBq1VQtx/AEl6RrM-kZjyLwUme" );
		//cookies.put( "SID", "DQAAAAcBAADNotp_odfLi0c7wz1_nkP08ZWRbmW4BOAzsqRfec9zOOPHLHP-lCK0g1V37rtooroDTYkre4rc8tNP3A_qDGYjtog0Ex7SjTd-wkEL3NNuVROsEQmfwHcztQ9bQ7mFbVReNSo-UXehZFiEwODVTrm-QTW-mnMRUjEN8Li0ceuXnS1G1r2Q2Fzbq5aPZeEX5MHHJaClgS7n5hN3QvPKQXIkA8v_ZOrrJLRxHiAWe5vBd7nTrWvNE_fw-R3eNpanCQXxXyZDJsc-69DVGPdqrFz-LaMRhbCOkAWwMZah9mpF2kSgHHzFR-S319RlAfKYfVdpQJilomIApYCidOp9fxHgKXj44Gcq98NobZ3P5gVCmg	" );
		//cookies.put( "SSID", "AmXTSbUMtFsE_VntY" );

//				try
//				{
			// Using jsoup java html parser library
			document = Jsoup
					.connect( url + "&cstart=0&pagesize=1000")
					.userAgent( "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.130 Safari/537.36" )
.header( "Accept", "Accept:text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8" ).cookies( cookies )
					.timeout( 5000 ).get();
//				}
//				catch ( Exception e )
//				{
//					return Collections.emptyList();
//				}
		if ( document == null )
			return Collections.emptyList();

		Elements publicationRowList = document.select( HtmlSelectorConstant.GS_PUBLICATION_ROW_LIST );

		if ( publicationRowList.size() == 0 )
		{
			log.info( "Np publication found " );
			return Collections.emptyList();
		}

		for ( Element eachPublicationRow : publicationRowList )
		{
			Map<String, String> publicationDetails = new LinkedHashMap<String, String>();
			// set source
			publicationDetails.put( "source", "googlescholar" );
			publicationDetails.put( "url", eachPublicationRow.select( "a" ).first().absUrl( "href" ) );
			publicationDetails.put( "title", eachPublicationRow.select( "a" ).first().text() );
			publicationDetails.put( "coauthor", eachPublicationRow.select( HtmlSelectorConstant.GS_PUBLICATION_COAUTHOR_AND_VENUE ).first().text() );
			String venue = eachPublicationRow.select( HtmlSelectorConstant.GS_PUBLICATION_COAUTHOR_AND_VENUE ).get( 1 ).text().trim();
			if( !venue.equals( "" ))
				publicationDetails.put( "venue", venue );
			String noCitation = eachPublicationRow.select( HtmlSelectorConstant.GS_PUBLICATION_NOCITATION ).text().replaceAll( "[^\\d]", "" );
			if( !noCitation.equals( "" ))
				publicationDetails.put( "nocitation", noCitation );
			String year = eachPublicationRow.select( HtmlSelectorConstant.GS_PUBLICATION_YEAR ).text().trim();
			if( !year.equals( "" ))
				publicationDetails.put( "year", year );

			publicationMapLists.add( publicationDetails );
		}

		return publicationMapLists;
	}

	public static Map<String, String> getPublicationDetailByPublicationUrl( String url ) throws IOException
	{
		Map<String, String> publicationDetailMaps = new LinkedHashMap<String, String>();

		// Using jsoup java html parser library
		Document document = null;

		Map<String, String> cookies = new HashMap<String, String>();
		//cookies.put( "APISID", "C3qs0OXHzPeJNxxT/At3gzfzcBTP87JOEM" );
		//cookies.put( "CONSENT", "YES+DE.en+20150705-15-0" );
		//cookies.put( "GMAIL_RTT", "37" );
		cookies.put( "GOOGLE_ABUSE_EXEMPTION", "ID=964f5a83b97f292a:TM=1437640196:C=c:IP=95.223.161.25-:S=APGng0vCH0HfwpnqD8P5QDwXNaSNQvK9tQ" );
		cookies.put( "GSP", "LM=1437638747:S=yGIL9JRwNlNlm4t5" );
		//cookies.put( "HSID", "AhDcQNKXGhrXqB3le" );
		cookies.put( "NID", "69=c4S-xS8vl9KJw4QfmFYZwY5NpTpaACuSa2hGMQALMhf7Xz6XH4B8JuyMs3hO5M7A1x003toBv_Syxhgwq_gsXnM3KvyXXN2xvYA-8SsiWSjqYapc4RtWTO1an7QGmsOl" );
		//cookies.put( "PREF", "ID=1111111111111111:FF=0:LD=en:TM=1432735603:LM=1435079443:S=bbCHTr3G7UUpxJzf" );
		//cookies.put( "SAPISID", "4ntbRtMKiBq1VQtx/AEl6RrM-kZjyLwUme" );
		//cookies.put( "SID", "DQAAAAcBAADNotp_odfLi0c7wz1_nkP08ZWRbmW4BOAzsqRfec9zOOPHLHP-lCK0g1V37rtooroDTYkre4rc8tNP3A_qDGYjtog0Ex7SjTd-wkEL3NNuVROsEQmfwHcztQ9bQ7mFbVReNSo-UXehZFiEwODVTrm-QTW-mnMRUjEN8Li0ceuXnS1G1r2Q2Fzbq5aPZeEX5MHHJaClgS7n5hN3QvPKQXIkA8v_ZOrrJLRxHiAWe5vBd7nTrWvNE_fw-R3eNpanCQXxXyZDJsc-69DVGPdqrFz-LaMRhbCOkAWwMZah9mpF2kSgHHzFR-S319RlAfKYfVdpQJilomIApYCidOp9fxHgKXj44Gcq98NobZ3P5gVCmg	" );
		//cookies.put( "SSID", "AmXTSbUMtFsE_VntY" );

//				try
//				{
			// Using jsoup java html parser library
			document = Jsoup
					.connect( url )
					.userAgent( "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.130 Safari/537.36" )
.header( "Accept", "Accept:text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8" ).cookies( cookies )
					.timeout( 5000 ).get();
//				}
//				catch ( Exception e )
//				{
//					return Collections.emptyList();
//				}
		if ( document == null )
			return Collections.emptyMap();

		Elements publicationDetailContainer = document.select( HtmlSelectorConstant.GS_PUBLICATION_DETAIL_CONTAINER );

		if ( publicationDetailContainer.size() == 0 )
		{
			log.info( "Np publication detail found " );
			return Collections.emptyMap();
		}

		publicationDetailMaps.put( "title", publicationDetailContainer.get( 0 ).select( HtmlSelectorConstant.GS_PUBLICATION_DETAIL_TITLE ).text() );

		String docName = publicationDetailContainer.get( 0 ).select( HtmlSelectorConstant.GS_PUBLICATION_DETAIL_PDF ).text();
		if ( docName != null )
			publicationDetailMaps.put( "doc", publicationDetailContainer.get( 0 ).select( HtmlSelectorConstant.GS_PUBLICATION_DETAIL_PDF ).text() );

		try
		{
			Elements publicationPdfUrl = publicationDetailContainer.get( 0 ).select( HtmlSelectorConstant.GS_PUBLICATION_DETAIL_PDF );
			if ( publicationPdfUrl != null )
				publicationDetailMaps.put( "doc_url", publicationPdfUrl.select( "a" ).first().absUrl( "href" ) );
		}
		catch ( Exception e )
		{
			// TODO: handle exception
		}

		Elements publicationDetailsRows = publicationDetailContainer.get( 0 ).select( HtmlSelectorConstant.GS_PUBLICATION_DETAIL_PROP );

		for ( Element publicationDetail : publicationDetailsRows )
			publicationDetailMaps.put( publicationDetail.select( HtmlSelectorConstant.GS_PUBLICATION_DETAIL_PROP_LABEL ).text(), publicationDetail.select( HtmlSelectorConstant.GS_PUBLICATION_DETAIL_PROP_VALUE ).text() );

		return publicationDetailMaps;
	}
}

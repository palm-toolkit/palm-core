package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.ParseException;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import de.rwth.i9.palm.oauth2.Oauth2Client;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( loader = AnnotationConfigContextLoader.class )
public class MendeleyPublicationCollectionTest
{
	@Test
	public void getListOfAuthorsTest() throws IOException, ParseException, OAuthSystemException, OAuthProblemException
	{
		String tokenUrl = "https://api-oauth2.mendeley.com/oauth/token";
		String clientId = "392";
		String clientSecret = "VWheQe6qKEGeUXQcLj1NDTCqxkP29PyJ";

		String token = Oauth2Client.Oauth2ClientRequestToken( tokenUrl, clientId, clientSecret );
		List<Map<String, String>> authorList = MendeleyPublicationCollection.getListOfAuthors( "chatti", token );

		for ( Map<String, String> eachAuthor : authorList )
		{
			for ( Entry<String, String> eachAuthorDetail : eachAuthor.entrySet() )
				System.out.println( eachAuthorDetail.getKey() + " : " + eachAuthorDetail.getValue() );
			System.out.println();
		}
	}

	@Test
	@Ignore
	public void getListOfPublicationTest() throws IOException
	{
		List<Map<String, String>> publicationMapLists = CiteseerXPublicationCollection.getPublicationListByAuthorUrl( "http://citeseerx.ist.psu.edu/viewauth/summary?aid=1149221" );

		for ( Map<String, String> eachPublicationMap : publicationMapLists )
		{
			for ( Entry<String, String> eachPublicationDetail : eachPublicationMap.entrySet() )
				System.out.println( eachPublicationDetail.getKey() + " : " + eachPublicationDetail.getValue() );
			System.out.println();
		}
	}

	@Test
	@Ignore
	public void getPublicationDetailByPublicationUrlTest() throws IOException
	{
		Map<String, String> publicationDetailMaps = CiteseerXPublicationCollection.getPublicationDetailByPublicationUrl( "http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.65.2866" );

		for ( Entry<String, String> eachPublicationDetail : publicationDetailMaps.entrySet() )
			System.out.println( eachPublicationDetail.getKey() + " : " + eachPublicationDetail.getValue() );

	}
}

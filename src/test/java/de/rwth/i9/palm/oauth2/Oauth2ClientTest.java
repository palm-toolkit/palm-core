package de.rwth.i9.palm.oauth2;

import java.io.IOException;

import org.apache.http.ParseException;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class Oauth2ClientTest
{

	@Test
	public void test() throws ParseException, IOException, OAuthSystemException, OAuthProblemException
	{
		String TOKEN_URL = "https://api-oauth2.mendeley.com/oauth/token";
		String TRUSTED_CLIENT_ID = "392";
		String TRUSTED_SECRET = "VWheQe6qKEGeUXQcLj1NDTCqxkP29PyJ";
		String CATALOG_URL = "https://api.mendeley.com:443/search/catalog?author=mohamed%20amine%20chatti&limit=2";

		JsonNode jsonNode = Oauth2Client.Oauth2ClientRequestCatalog( TOKEN_URL, TRUSTED_CLIENT_ID, TRUSTED_SECRET, CATALOG_URL );

		System.out.println( jsonNode.toString() );

		// mendeley print
		if ( jsonNode.isArray() )
		{
			for ( JsonNode j : jsonNode )
			{
				JsonNode titleNode = j.path( "title" );
				if ( !titleNode.isMissingNode() )
					System.out.println( "title : " + titleNode.toString() );
			}
		}

	}

}
package de.rwth.i9.palm.oauth2;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.apache.ApacheHttpTransport;

public class Oauth2Client
{
	private final static String TOKEN_URL = "https://api-oauth2.mendeley.com/oauth/token";
	private final static String TRUSTED_CLIENT_ID = "392";
	private final static String TRUSTED_SECRET = "VWheQe6qKEGeUXQcLj1NDTCqxkP29PyJ";
	private final static String CATALOG_URL = "https://api.mendeley.com:443/search/catalog?author=mohamed%20amine%20chatti&limit=100";
	
	/**
	 * Get resource using Oauth2, in this case Mendeley API
	 * 
	 * @param tokenUrl
	 * @param clientId
	 * @param clientSecret
	 * @param CatalogUrl
	 * @return Jsoup JsonNode
	 * @throws ParseException
	 * @throws IOException
	 * @throws OAuthSystemException
	 * @throws OAuthProblemException
	 */
	public static JsonNode Oauth2CLientRequestCatalog( String tokenUrl, String clientId, String clientSecret, String CatalogUrl ) throws ParseException, IOException, OAuthSystemException, OAuthProblemException
	{
		
		DefaultHttpClient apacheHttpClient = ApacheHttpTransport.newDefaultHttpClient();
		
		// configure and get access token
		OAuthClientRequest request = OAuthClientRequest
	            .tokenLocation(TOKEN_URL)
	            .setClientId(TRUSTED_CLIENT_ID)
	            .setClientSecret(TRUSTED_SECRET)
	            .setGrantType(GrantType.CLIENT_CREDENTIALS)
	            .setScope("all")
	            .buildBodyMessage();
	    OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
	    OAuthJSONAccessTokenResponse tokenResponse = oAuthClient.accessToken(
	            request, OAuthJSONAccessTokenResponse.class);

		// get the resources ( authors or publications )
	    HttpGet httpGet = new HttpGet(CATALOG_URL);
	    httpGet.setHeader("Authorization", "Bearer " + tokenResponse.getAccessToken());
	    HttpResponse httpResponse = apacheHttpClient.execute(httpGet);

		// map the results into jsonMap
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readTree( httpResponse.getEntity().getContent() );
	}
}

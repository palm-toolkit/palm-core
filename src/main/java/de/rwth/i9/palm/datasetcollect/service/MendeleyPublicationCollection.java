package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.apache.ApacheHttpTransport;

import de.rwth.i9.palm.model.SourceType;

public class MendeleyPublicationCollection extends PublicationCollection
{

	private final static Logger log = LoggerFactory.getLogger( MendeleyPublicationCollection.class );

	public MendeleyPublicationCollection()
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
	public static List<Map<String, String>> getListOfAuthors( String authorName, String token ) throws IOException
	{
		List<Map<String, String>> authorList = new ArrayList<Map<String, String>>();

		String authorCatalog = "https://api.mendeley.com:443/search/profiles?query=" + URLEncoder.encode( authorName, "UTF-8" ).replace( "+", "%20" );
		// get the resources ( authors or publications )
		HttpGet httpGet = new HttpGet( authorCatalog );
		httpGet.setHeader( "Authorization", "Bearer " + token );
	    DefaultHttpClient apacheHttpClient = ApacheHttpTransport.newDefaultHttpClient();
	    HttpResponse httpResponse = apacheHttpClient.execute(httpGet);

		// map the results into jsonMap
		ObjectMapper mapper = new ObjectMapper();
		JsonNode resultsNode = mapper.readTree( httpResponse.getEntity().getContent() );
		if ( resultsNode.isArray() )
			for ( JsonNode authorNode : resultsNode )
				authorList.add( extractAuthorDetail( authorNode ) );
		else
			authorList.add( extractAuthorDetail( resultsNode ) );

		return authorList;
	}

	private static Map<String, String> extractAuthorDetail( JsonNode authorNode )
	{
		Map<String, String> authorDetailMap = new LinkedHashMap<String, String>();
		if ( !authorNode.path( "display_name" ).isMissingNode() )
			authorDetailMap.put( "name", authorNode.path( "display_name" ).textValue() );
		if ( !authorNode.path( "last_name" ).isMissingNode() )
			authorDetailMap.put( "lastName", authorNode.path( "last_name" ).textValue() );
		if ( !authorNode.path( "first_name" ).isMissingNode() )
			authorDetailMap.put( "firstName", authorNode.path( "first_name" ).textValue() );
		if ( !authorNode.path( "email" ).isMissingNode() )
			authorDetailMap.put( "email", authorNode.path( "email" ).textValue() );
		if ( !authorNode.path( "institution_details" ).isMissingNode() )
		{
			JsonNode institutionNode = authorNode.path( "institution_details" );
			if ( !institutionNode.path( "name" ).isMissingNode() )
				authorDetailMap.put( "institutionName", institutionNode.path( "name" ).textValue() );
			if ( !institutionNode.path( "city" ).isMissingNode() )
				authorDetailMap.put( "institutionCity", institutionNode.path( "city" ).textValue() );
			if ( !institutionNode.path( "state" ).isMissingNode() )
				authorDetailMap.put( "institutionState", institutionNode.path( "state" ).textValue() );
			if ( !institutionNode.path( "country" ).isMissingNode() )
				authorDetailMap.put( "institutionCountry", institutionNode.path( "country" ).textValue() );
		}
		if ( !authorNode.path( "research_interests" ).isMissingNode() )
			authorDetailMap.put( "researchInterests", authorNode.path( "esearch_interests" ).textValue() );
		if ( !authorNode.path( "academic_status" ).isMissingNode() )
			authorDetailMap.put( "academicStatus", authorNode.path( "academic_status" ).textValue() );
		if ( !authorNode.path( "discipline" ).isMissingNode() )
		{
			JsonNode disciplineNode = authorNode.path( "discipline" );
			if ( !disciplineNode.path( "name" ).isMissingNode() )
				authorDetailMap.put( "discipline", disciplineNode.path( "name" ).textValue() );
		}
		if ( !authorNode.path( "photo" ).isMissingNode() )
		{
			JsonNode authorPhoto = authorNode.path( "photo" );
			if ( !authorPhoto.path( "standard" ).isMissingNode() )
			{
				String photoPath = authorPhoto.path( "standard" ).textValue();
				if ( !photoPath.contains( "awaiting" ) )
					authorDetailMap.put( "photo", photoPath );
			}
		}
		if ( !authorNode.path( "location" ).isMissingNode() )
		{
			JsonNode authorLocation = authorNode.path( "location" );
			if ( !authorLocation.path( "latitude" ).isMissingNode() )
				authorDetailMap.put( "locationLatitude", authorLocation.path( "latitude" ).textValue() );
			if ( !authorLocation.path( "longitude" ).isMissingNode() )
				authorDetailMap.put( "locationLongitude", authorLocation.path( "longitude" ).textValue() );
			if ( !authorLocation.path( "city" ).isMissingNode() )
				authorDetailMap.put( "locationCity", authorLocation.path( "city" ).textValue() );
			if ( !authorLocation.path( "state" ).isMissingNode() )
				authorDetailMap.put( "locationState", authorLocation.path( "state" ).textValue() );
			if ( !authorLocation.path( "country" ).isMissingNode() )
				authorDetailMap.put( "locationCountry", authorLocation.path( "country" ).textValue() );
		}

		authorDetailMap.put( "source", SourceType.MENDELEY.toString() );

		return authorDetailMap;
	}
	/**
	 * Logged in as sigit nugraha Display name sigit nugraha Id
	 * cbebc27d-600d-35db-9e57-1ecbc24229de
	 * 
	 * oAuth info Access token
	 * MSwxNDM3NTg5NzI0Mjk3LDQ0NTI3OTA2MSwxMDI4LGFsbCwsRmd3RmhaUktBUkpOM192Uk1YeGp6Zmt0S1BJ
	 * Refresh token
	 * MSw0NDUyNzkwNjEsMTAyOCxhbGwsLCwsRjVNNU1HZ0UtbTB2d01yM3NjcHNXTGl0TUVr>
	 * 
	 * $config['mendeleyclientid'] = '392'; $config['mendeleykey'] =
	 * 'VWheQe6qKEGeUXQcLj1NDTCqxkP29PyJ';
	 */
}

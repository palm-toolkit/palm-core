package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( loader = AnnotationConfigContextLoader.class )
public class DblpEventCollectionTest
{
	@SuppressWarnings( "unchecked" )
	@Test
	public void getPublicationListByVenueUrlTest() throws IOException
	{
		String url = "http://dblp.uni-trier.de/db/journals/jkm/jkm16.html#Chatti12";
		url = "http://dblp.uni-trier.de/db/journals/tlt/tlt5.html#ChattiSJ12";
		url = "http://dblp.uni-trier.de/db/conf/mlearn/mlearn2014.html#GrevenCTS14";
		Map<String, Object> venueDetailMap = DblpEventCollection.getPublicationListByVenueUrl( url, null );

		// List<Map<String,String>> publicationMapList = (List<Map<String,
		// String>>) venueDetail.get( "publications" );

		// print map
		for ( Map.Entry<String, Object> entry : venueDetailMap.entrySet() )
		{
			if ( entry.getKey().equals( "publications" ) )
			{
				// print publication detail
				System.out.println( "\nPublications : " );
				for ( Map<String, String> eachPublicationMap : (List<Map<String, String>>) entry.getValue() )
				{
					for ( Entry<String, String> eachPublicationDetail : eachPublicationMap.entrySet() )
						System.out.println( eachPublicationDetail.getKey() + " : " + eachPublicationDetail.getValue() );
					System.out.println();
				}
			}
			else
			{
				System.out.println( entry.getKey() + " : " + entry.getValue() );
			}
		}

	}
	
	
}

package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( loader = AnnotationConfigContextLoader.class )
public class CiteseerXPublicationCollectionTest
{
	@Test
	@Ignore
	public void getListOfAuthorsTest() throws IOException
	{
		Map<String, Map<String, String>> authorMaps = CiteseerXPublicationCollection.getListOfAuthors( "chatti" );

		for ( Entry<String, Map<String, String>> eachAuthor : authorMaps.entrySet() )
		{
			System.out.println( "author name : " + eachAuthor.getKey() );
			for ( Entry<String, String> eachAuthorDetail : eachAuthor.getValue().entrySet() )
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
	public void getPublicationDetailByPublicationUrlTest() throws IOException
	{
		Map<String, String> publicationDetailMaps = CiteseerXPublicationCollection.getPublicationDetailByPublicationUrl( "http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.65.2866" );

		for ( Entry<String, String> eachPublicationDetail : publicationDetailMaps.entrySet() )
			System.out.println( eachPublicationDetail.getKey() + " : " + eachPublicationDetail.getValue() );

	}
}

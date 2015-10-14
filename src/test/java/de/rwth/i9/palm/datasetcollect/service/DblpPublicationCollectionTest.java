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
public class DblpPublicationCollectionTest
{
	@Test
	@Ignore
	public void getListOfAuthorsTest() throws IOException
	{
		List<Map<String, String>> authorList = DblpPublicationCollection.getListOfAuthors( "mohamed amine chatti" );

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
		List<Map<String, String>> publicationMapLists = DblpPublicationCollection.getPublicationListByAuthorUrl( "http://dblp.uni-trier.de/db/conf/csedu/csedu2009-1.html" );

		for ( Map<String, String> eachPublicationMap : publicationMapLists )
		{
			for ( Entry<String, String> eachPublicationDetail : eachPublicationMap.entrySet() )
				System.out.println( eachPublicationDetail.getKey() + " : " + eachPublicationDetail.getValue() );
			System.out.println();
		}
	}
	
}

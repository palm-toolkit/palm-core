package de.rwth.i9.palm.utils;

import java.io.UnsupportedEncodingException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.web.client.RestClientException;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( loader = AnnotationConfigContextLoader.class )
public class TextUtilsTest
{
	@Test
	public void getListOfAuthorsTest() throws RestClientException, UnsupportedEncodingException
	{
		String text = "In this paper we present a highly accurate method for extracting keyphrasesfrom multi-document sets or clusters, with no prior knowledge about the documents. The algorithm is called CorePhrase, and is based on finding a set ofcore phrases from a document cluster.CorePhrase works by extracting a list of candidate keyphrases by intersectingdocuments using a graph-based model of the phrases in the documents. This isfacilitated through a powerful phrase-based document indexing model [3].Features of the extracted candidate keyphrases are then calculated, andphrases are ranked based on their features. The top phrases are output asthe descriptive topic of the document cluster. Results show that the extractedkeyphrases are highly relevant to the topic of the document set. Figure 1 illustrates the different components of the keyphrase extraction system.The work presented here assumes that: (1) keyphrases exist in the text andare not automatically generated; (2) the algorithm discovers keyphrases ratherthan learns how to extract them; and (3) if used with text clustering, the algo-rithm is not concerned with how the clusters are generated; it extracts keyphrasesfrom already clustered documents.The paper is organized as follows. Section 2 discusses related work in keyphraseextraction. The CorePhrase algorithm is presented in section 3. Experimentalresults are presented and discussed in section 4. Finally we discuss some conclusions and outline future work in section 5.";
		System.out.println( TextUtils.normalizeText( text ) );

	}

}

package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DblpPublicationCollection extends PublicationCollection
{
	private final static Logger log = LoggerFactory.getLogger( DblpPublicationCollection.class );

	public DblpPublicationCollection()
	{
		super();
	}

	public static List<Map<String, String>> getListOfAuthors( String authorName ) throws IOException
	{
		List<Map<String, String>> authorList = new ArrayList<Map<String, String>>();

		String url = "http://dblp.uni-trier.de/search/author?q=" + authorName.replace( " ", "+" );
		// Using jsoup java html parser library
		Document document = Jsoup.connect( url ).get();

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
			// get author url
			eachAuthorMap.put( "url", authorListNode.select( "a" ).first().absUrl( "href" ) );
			// get author name
			eachAuthorMap.put( "name", name );
			// get author photo
			eachAuthorMap.put( "photo", authorListNodes.select( "img" ).first().absUrl( "src" ) );
			// get author affiliation
			eachAuthorMap.put( "affiliation", authorListNodes.select( HtmlSelectorConstant.GS_AUTHOR_LIST_AFFILIATION ).html() );

			authorList.add( eachAuthorMap );
		}

		return authorList;
	}

	public static List<Map<String, String>> getPublicationListByAuthorUrl( String url ) throws IOException
	{
		List<Map<String, String>> publicationMapLists = new ArrayList<Map<String, String>>();

		// Using jsoup java html parser library
		Document document = Jsoup.connect( url ).get();

		Elements publicationRowList = document.select( HtmlSelectorConstant.GS_PUBLICATION_ROW_LIST );

		if ( publicationRowList.size() == 0 )
		{
			log.info( "Np publication found " );
			return Collections.emptyList();
		}

		for ( Element eachPublicationRow : publicationRowList )
		{
			Map<String, String> publicationDetails = new LinkedHashMap<String, String>();
			publicationDetails.put( "url", eachPublicationRow.select( "a" ).first().absUrl( "href" ) );
			publicationDetails.put( "title", eachPublicationRow.select( "a" ).first().text() );
			publicationDetails.put( "coauthor", eachPublicationRow.select( HtmlSelectorConstant.GS_PUBLICATION_COAUTHOR_AND_VENUE ).first().text() );
			publicationDetails.put( "venue", eachPublicationRow.select( HtmlSelectorConstant.GS_PUBLICATION_COAUTHOR_AND_VENUE ).get( 1 ).text() );
			publicationDetails.put( "nocitation", eachPublicationRow.select( HtmlSelectorConstant.GS_PUBLICATION_NOCITATION ).text() );
			publicationDetails.put( "year", eachPublicationRow.select( HtmlSelectorConstant.GS_PUBLICATION_YEAR ).text() );

			publicationMapLists.add( publicationDetails );
		}

		return publicationMapLists;
	}
	
	
	/**
	API
	http://www.dblp.org/search/api/?q=ulrik%20schroeder&h=1000&c=4&f=0&format=json
	
	{
"result":{
"query":"ulrik schroeder",
"status":{
"@code":"200",
"text":"OK"
},
"time":{
"@unit":"msecs",
"text":"24.89"
},
"completions":{
"@total":"1",
"@computed":"1",
"@sent":"1",
"c":{
"@sc":"12200",
"@dc":"61",
"@oc":"122",
"@id":"15877790",
"text":"schroeder"
}
},
"hits":{
"@total":"61",
"@computed":"61",
"@sent":"61",
"@first":"0",
"hit":[{
"@score":"300",
"@id":"1637906",
"info":{"authors":{"author":["Maria Knobelsdorf","Johannes Magenheim","Torsten Brinda","Dieter Engbring","Ludger Humbert","Arno Pasternak","Ulrik Schroeder","Marco Thomas","Jan Vahrenhold"]},"title":{"@ee":"http://doi.acm.org/10.1145/2716313","text":"Computer Science Education in North-Rhine Westphalia, Germany - A Case Study. "},"venue":{"@url":"db/journals/jeric/toce15.html#KnobelsdorfMBEH15","@journal":"TOCE","@pages":"9","@number":"2","@volume":"15","text":"TOCE 15(2):9 (2015)"},"year":"2015","type":"article"},
"url":"http://www.dblp.org/rec/bibtex/journals/jeric/KnobelsdorfMBEH15"
},
{
"@score":"300",
"@id":"1673971",
"info":{"authors":{"author":["Ahmed Mohamed Fahmy Yousef","Mohamed Amine Chatti","Ulrik Schroeder","Marold Wosnitza","Harald Jakobs"]},"title":{"@ee":"http://dx.doi.org/10.5220/0004791400090020","text":"MOOCs - A Review of the State-of-the-Art. "},"venue":{"@url":"db/conf/csedu/csedu2014-3.html#YousefCSWJ14","@conference":"CSEDU","@pages":"9-20","text":"CSEDU 2014:9-20"},"year":"2014","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/csedu/YousefCSWJ14"
},
{
"@score":"300",
"@id":"1689225",
"info":{"authors":{"author":["Anuj Tewari","Nitesh Goyal","Matthew K. Chan","Tina Yau","John Canny","Ulrik Schroeder"]},"title":{"@ee":"http://arxiv.org/abs/1401.7735","text":"SPRING: speech and pronunciation improvement through games, for Hispanic children. "},"venue":{"@url":"db/journals/corr/corr1401.html#TewariGCYCS14","@journal":"CoRR","@volume":"abs/1401.7735","text":"CoRR abs/1401.7735 (2014)"},"year":"2014","type":"article"},
"url":"http://www.dblp.org/rec/bibtex/journals/corr/TewariGCYCS14"
},
{
"@score":"300",
"@id":"1705832",
"info":{"authors":{"author":["Hendrik Thüs","Mohamed Amine Chatti","Christoph Greven","Ulrik Schroeder"]},"title":{"@ee":"http://subs.emis.de/LNI/Proceedings/Proceedings233/article39.html","text":"Kontexterfassung, -modellierung und -auswertung in Lernumgebungen. "},"venue":{"@url":"db/conf/delfi/delfi2014.html#ThusCGS14","@conference":"DeLFI","@pages":"157-162","text":"DeLFI 2014:157-162"},"year":"2014","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/delfi/ThusCGS14"
},
{
"@score":"300",
"@id":"1705865",
"info":{"authors":{"author":["Vlatko Lukarov","Mohamed Amine Chatti","Hendrik Thüs","Fatemeh Salehian Kia","Arham Muslim","Christoph Greven","Ulrik Schroeder"]},"title":{"@ee":"http://ceur-ws.org/Vol-1227/paper22.pdf","text":"Data Models in Learning Analytics. "},"venue":{"@url":"db/conf/delfi/delfi2014w.html#LukarovCTKMGS14","@conference":"DeLFI Workshops","@pages":"88-95","text":"DeLFI Workshops 2014:88-95"},"year":"2014","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/delfi/LukarovCTKMGS14"
},
{
"@score":"300",
"@id":"1705882",
"info":{"authors":{"author":["Christoph Greven","Mohamed Amine Chatti","Hendrik Thüs","Ulrik Schroeder"]},"title":{"@ee":"http://ceur-ws.org/Vol-1227/paper40.pdf","text":"Mobiles Professionelles Lernen in PRiME. "},"venue":{"@url":"db/conf/delfi/delfi2014w.html#GrevenCTS14","@conference":"DeLFI Workshops","@pages":"221-228","text":"DeLFI Workshops 2014:221-228"},"year":"2014","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/delfi/GrevenCTS14"
},
{
"@score":"300",
"@id":"1738437",
"info":{"authors":{"author":["Ahmed Mohamed Fahmy Yousef","Mohamed Amine Chatti","Ulrik Schroeder","Marold Wosnitza"]},"title":{"@ee":"http://dx.doi.org/10.1109/ICALT.2014.23","text":"What Drives a Successful MOOC? An Empirical Examination of Criteria to Assure Design Quality of MOOCs. "},"venue":{"@url":"db/conf/icalt/icalt2014.html#YousefCSW14","@conference":"ICALT","@pages":"44-48","text":"ICALT 2014:44-48"},"year":"2014","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/icalt/YousefCSW14"
},
{
"@score":"300",
"@id":"1738456",
"info":{"authors":{"author":["Mohamed Amine Chatti","Darko Dugoija","Hendrik Thüs","Ulrik Schroeder"]},"title":{"@ee":"http://dx.doi.org/10.1109/ICALT.2014.42","text":"Learner Modeling in Academic Networks. "},"venue":{"@url":"db/conf/icalt/icalt2014.html#ChattiDTS14","@conference":"ICALT","@pages":"117-121","text":"ICALT 2014:117-121"},"year":"2014","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/icalt/ChattiDTS14"
},
{
"@score":"300",
"@id":"1876385",
"info":{"authors":{"author":["Christoph Greven","Mohamed Amine Chatti","Hendrik Thüs","Ulrik Schroeder"]},"title":{"@ee":"http://dx.doi.org/10.1007/978-3-319-13416-1_27","text":"Context-Aware Mobile Professional Learning in PRiME. "},"venue":{"@url":"db/conf/mlearn/mlearn2014.html#GrevenCTS14","@conference":"mLearn","@pages":"287-299","text":"mLearn 2014:287-299"},"year":"2014","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/mlearn/GrevenCTS14"
},
{
"@score":"300",
"@id":"1911329",
"info":{"authors":{"author":["Florian Kerber","Jan Holz","Hendrik Thüs","Ulrik Schroeder"]},"title":{"@ee":"http://dx.doi.org/10.5220/0004384002420245","text":"A HackIt Framework for Security Education in Computer Science - Raising Awareness in Secondary School and at University with a Challenge-based Learning Environment. "},"venue":{"@url":"db/conf/csedu/csedu2013.html#KerberHTS13","@conference":"CSEDU","@pages":"242-245","text":"CSEDU 2013:242-245"},"year":"2013","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/csedu/KerberHTS13"
},
{
"@score":"300",
"@id":"1932355",
"info":{"authors":{"author":["Andreas Schäfer","Jan Holz","Thiemo Leonhardt","Ulrik Schroeder","Philipp Brauner","Martina Ziefle"]},"title":{"@ee":"http://dx.doi.org/10.1080/08993408.2013.778040","text":"From boring to scoring - a collaborative serious game for learning and practicing mathematical logic for computer science education. "},"venue":{"@url":"db/journals/csedu/csedu23.html#SchaferHLSBZ13","@journal":"Computer Science Education (CSEDU)","@pages":"87-111","@number":"2","@volume":"23","text":"Computer Science Education (CSEDU) 23(2):87-111 (2013)"},"year":"2013","type":"article"},
"url":"http://www.dblp.org/rec/bibtex/journals/csedu/SchaferHLSBZ13"
},
{
"@score":"300",
"@id":"2018569",
"info":{"authors":{"author":["Nadine Bergner","Tim Schellartz","Ulrik Schroeder"]},"title":{"@ee":"http://subs.emis.de/LNI/Proceedings/Proceedings219/article10.html","text":"Informatik und Mathematik - kombiniert im SchülerlaborModul &quot;Einstieg in die Computergrafik&quot;. "},"venue":{"@url":"db/conf/schule/infos2013.html#BergnerSS13","@conference":"INFOS","@pages":"107-116","text":"INFOS 2013:107-116"},"year":"2013","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/schule/BergnerSS13"
},
{
"@score":"300",
"@id":"2052277",
"info":{"authors":{"author":["Anna Lea Dyckhoff","Vlatko Lukarov","Arham Muslim","Mohamed Amine Chatti","Ulrik Schroeder"]},"title":{"@ee":"http://doi.acm.org/10.1145/2460296.2460340","text":"Supporting action research with learning analytics. "},"venue":{"@url":"db/conf/lak/lak2013.html#DyckhoffLMCS13","@conference":"LAK","@pages":"220-229","text":"LAK 2013:220-229"},"year":"2013","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/lak/DyckhoffLMCS13"
},
{
"@score":"300",
"@id":"2094586",
"info":{"authors":{"author":["Philipp Brauner","André Calero Valdez","Ulrik Schroeder","Martina Ziefle"]},"title":{"@ee":"http://dx.doi.org/10.1007/978-3-642-39062-3_22","text":"Increase Physical Fitness and Create Health Awareness through Exergames and Gamification - The Role of Individual Factors, Motivation and Acceptance. "},"venue":{"@url":"db/conf/southchi/southchi2013.html#BraunerVSZ13","@conference":"SouthCHI","@pages":"349-362","text":"SouthCHI 2013:349-362"},"year":"2013","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/southchi/BraunerVSZ13"
},
{
"@score":"300",
"@id":"2097334",
"info":{"authors":{"author":["Mohamed Amine Chatti","Simona Dakova","Hendrik Thüs","Ulrik Schroeder"]},"title":{"@ee":"http://doi.ieeecomputersociety.org/10.1109/TLT.2013.23","text":"Tag-Based Collaborative Filtering Recommendation in Personal Learning Environments. "},"venue":{"@url":"db/journals/tlt/tlt6.html#ChattiDTS13","@journal":"TLT","@pages":"337-349","@number":"4","@volume":"6","text":"TLT 6(4):337-349 (2013)"},"year":"2013","type":"article"},
"url":"http://www.dblp.org/rec/bibtex/journals/tlt/ChattiDTS13"
},
{
"@score":"300",
"@id":"2147896",
"info":{"authors":{"author":["Nadine Bergner","Jan Holz","Ulrik Schroeder"]},"title":{"@ee":"","text":"Cryptography for Middle School Students in an Extracurricular Learning Place. "},"venue":{"@url":"db/conf/csedu/csedu2012-2.html#BergnerHS12","@conference":"CSEDU","@pages":"265-270","text":"CSEDU 2012:265-270"},"year":"2012","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/csedu/BergnerHS12"
},
{
"@score":"300",
"@id":"2147930",
"info":{"authors":{"author":["Jan Holz","Nadine Bergner","Andreas Schäfer","Ulrik Schroeder"]},"title":{"@ee":"","text":"Serious Games on Multi Touch Tables for Computer Science Students. "},"venue":{"@url":"db/conf/csedu/csedu2012-2.html#HolzBSS12","@conference":"CSEDU","@pages":"519-524","text":"CSEDU 2012:519-524"},"year":"2012","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/csedu/HolzBSS12"
},
{
"@score":"300",
"@id":"2182834",
"info":{"authors":{"author":["Anna Lea Dyckhoff","Dennis Zielke","Mareike Bültmann","Mohamed Amine Chatti","Ulrik Schroeder"]},"title":{"@ee":"http://www.ifets.info/download_pdf.php?j_id=56&amp;a_id=1257","text":"Design and Implementation of a Learning Analytics Toolkit for Teachers. "},"venue":{"@url":"db/journals/ets/ets15.html#DyckhoffZBCS12","@journal":"Educational Technology &amp; Society (ETS)","@pages":"58-76","@number":"3","@volume":"15","text":"Educational Technology &amp; Society (ETS) 15(3):58-76 (2012)"},"year":"2012","type":"article"},
"url":"http://www.dblp.org/rec/bibtex/journals/ets/DyckhoffZBCS12"
},
{
"@score":"300",
"@id":"2202933",
"info":{"authors":{"author":["Mohamed Amine Chatti","Ulrik Schroeder","Hendrik Thüs","Simona Dakova"]},"title":{"@ee":"http://doi.ieeecomputersociety.org/10.1109/ICALT.2012.97","text":"Harnessing Collective Intelligence in Personal Learning Environments. "},"venue":{"@url":"db/conf/icalt/icalt2012.html#ChattiSTD12","@conference":"ICALT","@pages":"344-348","text":"ICALT 2012:344-348"},"year":"2012","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/icalt/ChattiSTD12"
},
{
"@score":"300",
"@id":"2326985",
"info":{"authors":{"author":["Mohamed Amine Chatti","Ulrik Schroeder","Matthias Jarke"]},"title":{"@ee":"http://doi.ieeecomputersociety.org/10.1109/TLT.2011.33","text":"LaaN: Convergence of Knowledge Management and Technology-Enhanced Learning. "},"venue":{"@url":"db/journals/tlt/tlt5.html#ChattiSJ12","@journal":"TLT","@pages":"177-189","@number":"2","@volume":"5","text":"TLT 5(2):177-189 (2012)"},"year":"2012","type":"article"},
"url":"http://www.dblp.org/rec/bibtex/journals/tlt/ChattiSJ12"
},
{
"@score":"300",
"@id":"2338818",
"info":{"authors":{"author":["Nadine Bergner","Jan Holz","Ulrik Schroeder"]},"title":{"@ee":"http://doi.acm.org/10.1145/2481449.2481457","text":"InfoSphere: an extracurricular learning environment for computer science. "},"venue":{"@url":"db/conf/wipsce/wipsce2012.html#BergnerHS12","@conference":"WiPSCE","@pages":"22-29","text":"WiPSCE 2012:22-29"},"year":"2012","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/wipsce/BergnerHS12"
},
{
"@score":"300",
"@id":"2340469",
"info":{"authors":{"author":["Ulrike Lucke","Ulrik Schroeder"]},"title":{"@ee":"http://dx.doi.org/10.1524/icom.2012.0001","text":"Forschungsherausforderung des E-Learning. "},"venue":{"@url":"db/journals/icom/icom11.html#LuckeS12","@journal":"i-com (ICOM)","@pages":"1-2","@number":"1","@volume":"11","text":"i-com (ICOM) 11(1):1-2 (2012)"},"year":"2012","type":"article"},
"url":"http://www.dblp.org/rec/bibtex/journals/icom/LuckeS12"
},
{
"@score":"300",
"@id":"2340481",
"info":{"authors":{"author":["Daniel Herding","Ulrik Schroeder","Patrick Stalljohann","Mohamed Amine Chatti"]},"title":{"@ee":"http://dx.doi.org/10.1524/icom.2012.0006","text":"Formatives Assessment in offenen, informellen vernetzten Lernszenarien. "},"venue":{"@url":"db/journals/icom/icom11.html#HerdingSSC12","@journal":"i-com (ICOM)","@pages":"19-21","@number":"1","@volume":"11","text":"i-com (ICOM) 11(1):19-21 (2012)"},"year":"2012","type":"article"},
"url":"http://www.dblp.org/rec/bibtex/journals/icom/HerdingSSC12"
},
{
"@score":"300",
"@id":"2340484",
"info":{"authors":{"author":["Mohamed Amine Chatti","Anna Lea Dyckhoff","Ulrik Schroeder","Hendrik Thüs"]},"title":{"@ee":"http://dx.doi.org/10.1524/icom.2012.0007","text":"Forschungsfeld Learning Analytics. "},"venue":{"@url":"db/journals/icom/icom11.html#ChattiDST12","@journal":"i-com (ICOM)","@pages":"22-25","@number":"1","@volume":"11","text":"i-com (ICOM) 11(1):22-25 (2012)"},"year":"2012","type":"article"},
"url":"http://www.dblp.org/rec/bibtex/journals/icom/ChattiDST12"
},
{
"@score":"300",
"@id":"2395333",
"info":{"authors":{"author":["Ulrik Schroeder"]},"title":{"@ee":"http://subs.emis.de/LNI/Proceedings/Proceedings188/article6405.html","text":"Kollaborative und altersgerechte Lernanwendung zur Vermittlung fundamentaler Ideen der Informatik. "},"venue":{"@url":"db/conf/delfi/delfi2011.html#Schroeder11","@conference":"DeLFI","@pages":"185-196","text":"DeLFI 2011:185-196"},"year":"2011","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/delfi/Schroeder11"
},
{
"@score":"300",
"@id":"2395334",
"info":{"authors":{"author":["Jens Drummer","Sybille Hambach","Andrea Kienle","Ulrike Lucke","Alke Martens","Wolfgang Müller 0004","Christoph Rensing","Ulrik Schroeder","Andreas Schwill","Christian Spannagel","Stephan Trahasch"]},"title":{"@ee":"http://subs.emis.de/LNI/Proceedings/Proceedings188/article6396.html","text":"Forschungsherausforderung des E-Learnings. "},"venue":{"@url":"db/conf/delfi/delfi2011.html#DrummerHKLMMRSSST11","@conference":"DeLFI","@pages":"197-208","text":"DeLFI 2011:197-208"},"year":"2011","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/delfi/DrummerHKLMMRSSST11"
},
{
"@score":"300",
"@id":"2395375",
"info":{"authors":{"author":["Christoph Rensing","Mostafa Akbari","Claudia Bremer","Ulrike Lucke","Ulrik Schroeder"]},"title":{"@ee":"http://dl.mensch-und-computer.de/handle/123456789/2814","text":"Vorwort. "},"venue":{"@url":"db/conf/delfi/delfi2011w.html#RensingABLS11","@conference":"DeLFI Workshops","text":"DeLFI Workshops 2011"},"year":"2011","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/delfi/RensingABLS11"
},
{
"@score":"300",
"@id":"2397421",
"info":{"authors":{"author":["Torsten Kammer","Philipp Brauner","Thiemo Leonhardt","Ulrik Schroeder"]},"title":{"@ee":"http://dx.doi.org/10.1007/978-3-642-23985-4_16","text":"Simulating LEGO Mindstorms Robots to Facilitate Teaching Computer Programming to School Students. "},"venue":{"@url":"db/conf/ectel/ectel2011.html#KammerBLS11","@conference":"EC-TEL","@pages":"196-209","text":"EC-TEL 2011:196-209"},"year":"2011","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/ectel/KammerBLS11"
},
{
"@score":"300",
"@id":"2399032",
"info":{"authors":{"author":["Anna Lea Dyckhoff","Dennis Zielke","Mohamed Amine Chatti","Ulrik Schroeder"]},"title":{"@ee":"http://educationaldatamining.org/EDM2011/wp-content/uploads/proc/edm2011_poster19_Dyckhoff.pdf","text":"eLAT: An Exploratory Learning Analytics Tool for Reflection and Iterative Improvement of Technology Enhanced Learning. "},"venue":{"@url":"db/conf/edm/edm2011.html#DyckhoffZCS11","@conference":"EDM","@pages":"355-356","text":"EDM 2011:355-356"},"year":"2011","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/edm/DyckhoffZCS11"
},
{
"@score":"300",
"@id":"2473209",
"info":{"authors":{"author":["Thiemo Leonhardt","Philipp Brauner","Jochen Siebert","Ulrik Schroeder"]},"title":{"@ee":"http://subs.emis.de/LNI/Proceedings/Proceedings189/article6431.html","text":"Übertragbarkeit singulärer MINT-Interesse-initiierender außerschulischer Maßnahmen. "},"venue":{"@url":"db/conf/schule/infos2011.html#LeonhardtBSS11","@conference":"INFOS","@pages":"127-136","text":"INFOS 2011:127-136"},"year":"2011","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/schule/LeonhardtBSS11"
},
{
"@score":"300",
"@id":"2483410",
"info":{"authors":{"author":["Ulrik Schroeder"]},"title":{"@ee":"http://doi.acm.org/10.1145/1999747.1999749","text":"A bouquet of measures to promote computer science in middle &amp; high schools. "},"venue":{"@url":"db/conf/iticse/iticse2011.html#Schroeder11","@conference":"ITiCSE","@pages":"1","text":"ITiCSE 2011:1"},"year":"2011","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/iticse/Schroeder11"
},
{
"@score":"300",
"@id":"2506097",
"info":{"authors":{"author":["Jan Holz","Thiemo Leonhardt","Ulrik Schroeder"]},"title":{"@ee":"http://doi.acm.org/10.1145/2094131.2094148","text":"Using smartphones to motivate secondary school students for informatics. "},"venue":{"@url":"db/conf/kolicalling/kolicalling2011.html#HolzLS11","@conference":"Koli Calling","@pages":"89-94","text":"Koli Calling 2011:89-94"},"year":"2011","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/kolicalling/HolzLS11"
},
{
"@score":"300",
"@id":"2506107",
"info":{"authors":{"author":["Erika Ábrahám","Nadine Bergner","Philipp Brauner","Florian Corzilius","Nils Jansen","Thiemo Leonhardt","Ulrich Loup","Johanna Nellen","Ulrik Schroeder"]},"title":{"@ee":"http://doi.acm.org/10.1145/2094131.2094162","text":"On collaboratively conveying computer science to pupils. "},"venue":{"@url":"db/conf/kolicalling/kolicalling2011.html#AbrahamBBCJLLNS11","@conference":"Koli Calling","@pages":"132-137","text":"Koli Calling 2011:132-137"},"year":"2011","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/kolicalling/AbrahamBBCJLLNS11"
},
{
"@score":"300",
"@id":"2609007",
"info":{"authors":{"author":["Michael Kerres","Nadine Ojstersek","Ulrik Schroeder","Ulrich Hoppe"]},"title":{"@ee":"","text":"DeLFI 2010 - 8. Tagung der Fachgruppe E-Learning der Gesellschaft für Informatik e.V., 12.-15. September 2010, Universität Duisburg-Essen "},"venue":{"@url":"db/conf/delfi/delfi2010.html","@conference":"DeLFI","text":"DeLFI 2010"},"year":"2010","type":"proceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/delfi/2010"
},
{
"@score":"300",
"@id":"2609023",
"info":{"authors":{"author":["Daniel Herding","Marc Zimmermann","Christine Bescherer","Ulrik Schroeder"]},"title":{"@ee":"http://subs.emis.de/LNI/Proceedings/Proceedings169/article5728.html","text":"Entwicklung eines Frameworks für semi-automatisches Feedback zur Unterstützung bei Lernprozessen. "},"venue":{"@url":"db/conf/delfi/delfi2010.html#HerdingZBS10","@conference":"DeLFI","@pages":"145-156","text":"DeLFI 2010:145-156"},"year":"2010","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/delfi/HerdingZBS10"
},
{
"@score":"300",
"@id":"2609031",
"info":{"authors":{"author":["Erika Ábrahám","Philipp Brauner","Nils Jansen","Thiemo Leonhardt","Ulrich Loup","Ulrik Schroeder"]},"title":{"@ee":"http://subs.emis.de/LNI/Proceedings/Proceedings169/article5729.html","text":"Podcastproduktion als kollaborativer Zugang zur theoretischen Informatik. "},"venue":{"@url":"db/conf/delfi/delfi2010.html#AbrahamBJLLS10","@conference":"DeLFI","@pages":"239-251","text":"DeLFI 2010:239-251"},"year":"2010","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/delfi/AbrahamBJLLS10"
},
{
"@score":"300",
"@id":"2638074",
"info":{"authors":{"author":["Patrick Stalljohann","Ulrik Schroeder"]},"title":{"@ee":"http://dx.doi.org/10.1109/ICALT.2010.23","text":"A Portal-Based Gradebook - DAG-Based Definition of Assessment Criteria in Higher Education. "},"venue":{"@url":"db/conf/icalt/icalt2010.html#StalljohannS10","@conference":"ICALT","@pages":"58-60","text":"ICALT 2010:58-60"},"year":"2010","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/icalt/StalljohannS10"
},
{
"@score":"300",
"@id":"2638128",
"info":{"authors":{"author":["Mostafa Akbari","Georg Böhm","Ulrik Schroeder"]},"title":{"@ee":"http://dx.doi.org/10.1109/ICALT.2010.76","text":"Enabling Communication and Feedback in Mass Lectures. "},"venue":{"@url":"db/conf/icalt/icalt2010.html#AkbariBS10","@conference":"ICALT","@pages":"254-258","text":"ICALT 2010:254-258"},"year":"2010","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/icalt/AkbariBS10"
},
{
"@score":"300",
"@id":"2690644",
"info":{"authors":{"author":["Philipp Brauner","Thiemo Leonhardt","Martina Ziefle","Ulrik Schroeder"]},"title":{"@ee":"http://dx.doi.org/10.1007/978-3-642-11376-5_7","text":"The Effect of Tangible Artifacts, Gender and Subjective Technical Competence on Teaching Programming to Seventh Graders. "},"venue":{"@url":"db/conf/issep/issep2010.html#BraunerLZS10","@conference":"ISSEP","@pages":"61-71","text":"ISSEP 2010:61-71"},"year":"2010","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/issep/BraunerLZS10"
},
{
"@score":"300",
"@id":"2720439",
"info":{"authors":{"author":["Ulrik Schroeder"]},"title":{"@ee":"http://dl.mensch-und-computer.de/handle/123456789/822","text":"Interaktive Kulturen: Workshop-Band. Proceedings der Workshops der Mensch &amp; Computer 2010 - 10. Fachübergreifende Konferenz für Interaktive und Kooperative Medien, DeLFI 2010 - die 8. E-Learning Fachtagung Informatik der Gesellschaft für Informatik e.V. und der Entertainment Interfaces 2010, Duisburg, Germany, September 12-15, 2010 "},"venue":{"@url":"db/conf/mc/mc2010w.html","@conference":"Mensch &amp; Computer Workshopband","text":"Mensch &amp; Computer Workshopband 2010"},"year":"2010","type":"proceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/mc/2010w"
},
{
"@score":"300",
"@id":"2811789",
"info":{"authors":{"author":["Eva Altenbernd-Giani","Ulrik Schroeder","Mostafa Akbari"]},"title":{"@ee":"http://subs.emis.de/LNI/Proceedings/Proceedings153/article2522.html","text":"Programmierungslehrveranstaltung unter der Lupe. "},"venue":{"@url":"db/conf/delfi/delfi2009.html#Altenbernd-GianiSA09","@conference":"DeLFI","@pages":"55-66","text":"DeLFI 2009:55-66"},"year":"2009","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/delfi/Altenbernd-GianiSA09"
},
{
"@score":"300",
"@id":"2811808",
"info":{"authors":{"author":["Patrick Stalljohann","Eva Altenbernd-Giani","Anna Lea Dyckhoff","Philipp Rohde","Ulrik Schroeder"]},"title":{"@ee":"http://subs.emis.de/LNI/Proceedings/Proceedings153/article2541.html","text":"Feedback mit einem webbasierten Übungsbetrieb. "},"venue":{"@url":"db/conf/delfi/delfi2009.html#StalljohannADRS09","@conference":"DeLFI","@pages":"283-294","text":"DeLFI 2009:283-294"},"year":"2009","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/delfi/StalljohannADRS09"
},
{
"@score":"300",
"@id":"2811811",
"info":{"authors":{"author":["Andreas Harrer","Steffen Lohmann","Christoph Rensing","Ulrik Schroeder"]},"title":{"@ee":"http://dl.mensch-und-computer.de/handle/123456789/2729","text":"Vorwort. "},"venue":{"@url":"db/conf/delfi/delfi2009w.html#HarrerLRS09","@conference":"DeLFI Workshops","@pages":"12","text":"DeLFI Workshops 2009:12"},"year":"2009","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/delfi/HarrerLRS09"
},
{
"@score":"300",
"@id":"2811816",
"info":{"authors":{"author":["Mostafa Akbari","Georg Böhm","Ulrik Schroeder"]},"title":{"@ee":"http://dl.mensch-und-computer.de/handle/123456789/2725","text":"Unterstützung der Präsenzlehre in Blended Learning Szenarien mittels Microblogging. "},"venue":{"@url":"db/conf/delfi/delfi2009w.html#AkbariBS09","@conference":"DeLFI Workshops","@pages":"45-52","text":"DeLFI Workshops 2009:45-52"},"year":"2009","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/delfi/AkbariBS09"
},
{
"@score":"300",
"@id":"2811830",
"info":{"authors":{"author":["Anna Lea Dyckhoff","Daniel Herding","Ulrik Schroeder"]},"title":{"@ee":"http://dl.mensch-und-computer.de/handle/123456789/2743","text":"eLectures im Kontext eines Peerteaching-Kolloquiums: Ein Erfahrungsbericht. "},"venue":{"@url":"db/conf/delfi/delfi2009w.html#DyckhoffHS09","@conference":"DeLFI Workshops","@pages":"143-150","text":"DeLFI Workshops 2009:143-150"},"year":"2009","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/delfi/DyckhoffHS09"
},
{
"@score":"300",
"@id":"2863089",
"info":{"authors":{"author":["Ulrik Schroeder"]},"title":{"@ee":"http://dx.doi.org/10.1007/978-3-642-03426-8_3","text":"Web-Based Learning - Yes We Can! "},"venue":{"@url":"db/conf/icwl/icwl2009.html#Schroeder09","@conference":"ICWL","@pages":"25-33","text":"ICWL 2009:25-33"},"year":"2009","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/icwl/Schroeder09"
},
{
"@score":"300",
"@id":"2921532",
"info":{"authors":{"author":["Daniel Herding","Ulrik Schroeder","Mostafa Akbari"]},"title":{"@ee":"http://dl.mensch-und-computer.de/handle/123456789/306","text":"Protoreto: Interaktive und automatisch auswertbare Papierprototypen. "},"venue":{"@url":"db/conf/mc/mc2009.html#HerdingSA09","@conference":"Mensch &amp; Computer","@pages":"283-292","text":"Mensch &amp; Computer 2009:283-292"},"year":"2009","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/mc/HerdingSA09"
},
{
"@score":"300",
"@id":"2955973",
"info":{"authors":{"author":["André Calero Valdez","Martina Ziefle","Andreas Horstmann","Daniel Herding","Ulrik Schroeder"]},"title":{"@ee":"http://dx.doi.org/10.1007/978-3-642-10308-7_26","text":"Effects of Aging and Domain Knowledge on Usability in Small Screen Devices for Diabetes Patients. "},"venue":{"@url":"db/conf/usab/usab2009.html#ValdezZHHS09","@conference":"USAB","@pages":"366-386","text":"USAB 2009:366-386"},"year":"2009","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/usab/ValdezZHHS09"
},
{
"@score":"300",
"@id":"3008767",
"info":{"authors":{"author":["Anna Lea Dyckhoff","Philipp Rohde","Ulrik Schroeder","Patrick Stalljohann"]},"title":{"@ee":"http://subs.emis.de/LNI/Proceedings/Proceedings132/article5090.html","text":"Integriertes Übungsbetriebmodul im Rahmen eines hochschulweiten eLearning-Portals. "},"venue":{"@url":"db/conf/delfi/delfi2008.html#DyckhoffRSS08","@conference":"DeLFI","@pages":"185-196","text":"DeLFI 2008:185-196"},"year":"2008","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/delfi/DyckhoffRSS08"
},
{
"@score":"300",
"@id":"3008775",
"info":{"authors":{"author":["Christian Spannagel","Ulrik Schroeder"]},"title":{"@ee":"http://subs.emis.de/LNI/Proceedings/Proceedings132/article5098.html","text":"GUI-Adaptation in Lernkontexten. "},"venue":{"@url":"db/conf/delfi/delfi2008.html#SpannagelS08","@conference":"DeLFI","@pages":"281-292","text":"DeLFI 2008:281-292"},"year":"2008","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/delfi/SpannagelS08"
},
{
"@score":"300",
"@id":"3089675",
"info":{"authors":{"author":["Christian Spannagel","Raimund Girwidz","Herbert Löthe","Andreas Zendler","Ulrik Schroeder"]},"title":{"@ee":"http://dx.doi.org/10.1016/j.intcom.2007.08.002","text":"Animated demonstrations and training wheels interfaces in a complex learning environment. "},"venue":{"@url":"db/journals/iwc/iwc20.html#SpannagelGLZS08","@journal":"Interacting with Computers (IWC)","@pages":"97-111","@number":"1","@volume":"20","text":"Interacting with Computers (IWC) 20(1):97-111 (2008)"},"year":"2008","type":"article"},
"url":"http://www.dblp.org/rec/bibtex/journals/iwc/SpannagelGLZS08"
},
{
"@score":"300",
"@id":"3106396",
"info":{"authors":{"author":["Christoph Rensing","Ulrik Schroeder","Andreas Harrer","Steffen Lohmann"]},"title":{"@ee":"http://dl.mensch-und-computer.de/handle/123456789/2767","text":"Vorwort der Organisatoren des 2. Workshops E-Learning 2.0: &quot;Web 2.0 and Social Software in Technology enhanced Learning&quot;. "},"venue":{"@url":"db/conf/mc/mc2008w.html#RensingSHL08","@conference":"Mensch &amp; Computer Workshopband","@pages":"303-304","text":"Mensch &amp; Computer Workshopband 2008:303-304"},"year":"2008","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/mc/RensingSHL08"
},
{
"@score":"300",
"@id":"3168840",
"info":{"authors":{"author":["Martina Ziefle","Ulrik Schroeder","Judith Strenk","Thomas Michel"]},"title":{"@ee":"http://doi.acm.org/10.1145/1240624.1240676","text":"How younger and older adults master the usage of hyperlinks in small screen devices. "},"venue":{"@url":"db/conf/chi/chi2007.html#ZiefleSSM07","@conference":"CHI","@pages":"307-316","text":"CHI 2007:307-316"},"year":"2007","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/chi/ZiefleSSM07"
},
{
"@score":"300",
"@id":"3187270",
"info":{"authors":{"author":["Christoph Rensing","Ulrik Schroeder","Guido Rößling"]},"title":{"@ee":"http://dl.mensch-und-computer.de/handle/123456789/2708","text":"Vorwort der Organisatoren der Workshops. "},"venue":{"@url":"db/conf/delfi/delfi2007w.html#RensingSR07","@conference":"DeLFI Workshops","@pages":"5-6","text":"DeLFI Workshops 2007:5-6"},"year":"2007","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/delfi/RensingSR07"
},
{
"@score":"300",
"@id":"3248407",
"info":{"authors":{"author":["Ulrik Schroeder","N. J. van den Boom"]},"title":{"@ee":"http://subs.emis.de/LNI/Proceedings/Proceedings112/article1841.html","text":"ali - Aachener eLeitprogramme der Informatik. "},"venue":{"@url":"db/conf/schule/infos2007.html#SchroederB07","@conference":"INFOS","@pages":"329-330","text":"INFOS 2007:329-330"},"year":"2007","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/schule/SchroederB07"
},
{
"@score":"300",
"@id":"3625191",
"info":{"authors":{"author":["Ulrik Schroeder","Christian Spannagel"]},"title":{"@ee":"","text":"Supporting Active Learning in E-Learning Scenarios. "},"venue":{"@url":"db/conf/cate/cate2004.html#SchroederS04","@conference":"CATE","@pages":"124-129","text":"CATE 2004:124-129"},"year":"2004","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/cate/SchroederS04"
},
{
"@score":"300",
"@id":"3648990",
"info":{"authors":{"author":["Eva Giani","Ulrik Schroeder"]},"title":{"@ee":"http://subs.emis.de/LNI/Proceedings/Proceedings50/article3186.html","text":"Seminarkonzept zur aktiven Teilnahme mit BSCW-Unterstützung. "},"venue":{"@url":"db/conf/gi/gi2004-1.html#GianiS04","@conference":"GI Jahrestagung","@pages":"424-428","text":"GI Jahrestagung 2004:424-428"},"year":"2004","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/gi/GianiS04"
},
{
"@score":"300",
"@id":"3752373",
"info":{"authors":{"author":["Ulrik Schroeder","Christian Spannagel"]},"title":{"@ee":"http://subs.emis.de/LNI/Proceedings/Proceedings37/article1095.html","text":"Implementierung von eLearning-Szenarien nach der Theorie der kognitiven Lehre. "},"venue":{"@url":"db/conf/delfi/delfi2003.html#SchroederS03","@conference":"DeLFI","@pages":"195-204","text":"DeLFI 2003:195-204"},"year":"2003","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/delfi/SchroederS03"
},
{
"@score":"300",
"@id":"3855988",
"info":{"authors":{"author":["Christine Haller","Ulrik Schroeder"]},"title":{"@ee":"http://subs.emis.de/LNI/Proceedings/Proceedings19/article412.html","text":"Topic Maps zur Realisierung von Ontologien im Kontext der Vision des Semantic Web. "},"venue":{"@url":"db/conf/gi/gi2002-1.html#HallerS02","@conference":"GI Jahrestagung","@pages":"141-145","text":"GI Jahrestagung 2002:141-145"},"year":"2002","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/gi/HallerS02"
},
{
"@score":"300",
"@id":"4275061",
"info":{"authors":{"author":["Ulrik Schroeder"]},"title":{"@ee":"","text":"Inkrementelle, syntaxbasierte Revisions- und Variantenkontrolle mit interaktiver Konfigurationsunterstützung. "},"venue":{"@url":"","@publisher":"Shaker","text":"Shaker 1995"},"year":"1995","type":"book"},
"url":"http://www.dblp.org/rec/bibtex/books/daglib/0077975"
},
{
"@score":"300",
"@id":"4391157",
"info":{"authors":{"author":["Gregor Snelting","Franz-Josef Grosch","Ulrik Schroeder"]},"title":{"@ee":"http://dx.doi.org/10.1007/3540547428_60","text":"Inference-Based Support for Programming in the Large. "},"venue":{"@url":"db/conf/esec/esec91.html#SneltingGS91","@conference":"ESEC","@pages":"396-408","text":"ESEC 1991:396-408"},"year":"1991","type":"inproceedings"},
"url":"http://www.dblp.org/rec/bibtex/conf/esec/SneltingGS91"
}
]
}
}
}
	 */
}

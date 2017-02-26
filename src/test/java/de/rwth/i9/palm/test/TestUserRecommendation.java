package de.rwth.i9.palm.test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.ParameterMode;

import org.apache.mahout.cf.taste.common.TasteException;
import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionFactory;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.mysql.jdbc.CallableStatement;

import de.rwth.i9.palm.analytics.config.AppConfig;
import de.rwth.i9.palm.config.DatabaseConfigCoreTest;
import de.rwth.i9.palm.config.WebAppConfigTest;
import de.rwth.i9.palm.feature.researcher.ResearcherFeature;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Location;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationAuthor;
import de.rwth.i9.palm.model.User;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.persistence.PublicationDAO;
import de.rwth.i9.palm.persistence.UserDAO;
import de.rwth.i9.palm.recommendation.service.GenericRecommendation;
import de.rwth.i9.palm.recommendation.service.RecommendationHandler;
import de.rwth.i9.palm.recommendation.service.GenericRecommendation;
import de.rwth.i9.palm.recommendation.service.SNAC2RecommendationFeature;
import de.rwth.i9.palm.recommendation.service.SNAC3RecommendationFeature;
import de.rwth.i9.palm.recommendation.service.SNAINRecommendationFeature;
import de.rwth.i9.palm.recommendation.service.UtilINImp;
import de.rwth.i9.palm.topicextraction.service.TopicExtractionService;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( classes = { WebAppConfigTest.class, DatabaseConfigCoreTest.class, AppConfig.class }, loader = AnnotationConfigContextLoader.class )
@TransactionConfiguration
@Transactional

public class TestUserRecommendation extends AbstractTransactionalJUnit4SpringContextTests {

	@Autowired
	private PersistenceStrategy persistenceStrategy;
	
	@Autowired
	private SessionFactory sessionFactory;
	
	//@Autowired
	//private UtilIN util;
	//@Autowired // actually this one is for the API, so I guess you don't need to
	// use this
	//private ResearcherFeature researcherFeature;

	@Autowired
	private TopicExtractionService service;

	@Autowired
	private ResearcherFeature researcherFeature;
	
	@Autowired
	private RecommendationHandler genRec;
	
	//@Autowired
	//private PalmAnalytics palmAnalytics;


	@SuppressWarnings( "unchecked" )
	@Test
	public void testSNAC3RecommendationsSystem()
	{
		Author auth = persistenceStrategy.getAuthorDAO().getById( "c442983a-0099-4d6d-89b1-6cfc57fa6138" );
		System.out.println( "Name: " + auth.getName() );

		String query = "select t1.author_id, t2.author_id from publication_author t1 " +
		"left join publication_author t2 on (t1.publication_id = t2.publication_id and t1.author_id <> t2.author_id) " +
		"where t2.author_id = :authorID";
		
		long time = System.currentTimeMillis();
		//List<Object> coAuthors = sessionFactory.getCurrentSession().createSQLQuery( query )
		//.setParameter( "authorID", new String("c442983a-0099-4d6d-89b1-6cfc57fa6138") ).list();
		
		/*ArrayList<String> coAuthors = new ArrayList<>();
		Set<Publication> pubs = auth.getPublications();
		for ( Publication publication : pubs )
		{
			List<Author> authors = publication.getCoAuthors();
			for ( Author author : authors )
			{
				String id = author.getId();
				if( !id.equals( "c442983a-0099-4d6d-89b1-6cfc57fa6138" ) &&
						!coAuthors.contains( id ) )
					coAuthors.add( id );
			}
		}
		
		String calles = Arrays.asList( new String[]{"c442983a-0099-4d6d-89b1-6cfc57fa6138"} ).toString()
				.replace( "[", "" ).replace( "]", "" )
				.replaceAll( ", ", "\',\'" );
		
		long time = System.currentTimeMillis();
		List<Object[]> co1DAuthors = sessionFactory.getCurrentSession()
				.createSQLQuery( "CALL nugraha.Get1DCoAuthors(:IDs)" )
				.setParameter( "IDs", calles )
				.list();
		System.out.println( "Return List: " + co1DAuthors.size() + "  -  " + coAuthors.size() );
		*/
		
		/*UtilINImp util = new UtilINImp( persistenceStrategy, service, sessionFactory );
		List<String> coAuthors = new ArrayList<>( util.get3DCoAuthors( "c442983a-0099-4d6d-89b1-6cfc57fa6138" ) );
		
		System.out.println( "2D authors: " + coAuthors.size() );

		String authors = coAuthors.toString().replace( "[", "" ).replace( "]", "" )
				.replaceAll( ", ", "\',\'" );
		PublicationDAO pubDao = persistenceStrategy.getPublicationDAO();
		ArrayList<String> iNames = new ArrayList<>(Arrays.asList( new String[]{"intelligent tutoring system",
				"affect", "natural language processing", "information retrieval",
				"affective state", "recommender system", "educational data mining", 
				"serious game", "semantic web", "cognitive tutor"} ));
		JSONArray cfCResult = new JSONArray();
		for (int i = 0; i < iNames.size(); i++) 
		{
			JSONObject pubofIObj = new JSONObject();
			JSONArray pubOfIJsonArray = new JSONArray();
			List<String> list = sessionFactory.getCurrentSession()
					.createSQLQuery( "call GetTopResearcherIds(:authorsIds,:term)" )
					.setTimeout( 60 ).setParameter( "authorsIds", authors )
					.setParameter( "term", iNames.get( i ) ).list();
			
			StringBuilder queryString = new StringBuilder();
			queryString.append( "SELECT DISTINCT p FROM PublicationAuthor p " );
			queryString.append( "WHERE p.author in ( SELECT DISTINCT a FROM Author a WHERE a.id in (:auth) ) " );

			Query pubQuery = sessionFactory.getCurrentSession().createQuery( queryString.toString() );
			pubQuery.setParameterList( "auth", list );

			@SuppressWarnings( "unchecked" )
			List<PublicationAuthor> pubs = pubQuery.list();
			
			List<String> pubs = sessionFactory.getCurrentSession()
					//.createQuery( "FROM PublicationAuthor" )
					.createSQLQuery( "SELECT publication_id FROM publication_author WHERE author_id IN (:auth)" )
					.setParameterList( "auth", list ).list();
			
			for ( String pubID : pubs )
			{
				Publication publication = pubDao.getById( pubID.toString() );//pubID.getPublication();//
				
				JSONObject obj = new JSONObject();
				obj.put( "iName", iNames.get(i));
				obj.put( "pID", publication.getId() );
				obj.put( "pTitle", publication.getTitle() );
				obj.put( "pAuthors", publication.getAuthors().toString().replace( "[", "" ).replace( "]", "" ));
				obj.put( "pAbstract", publication.getAbstractText() );
				obj.put( "pKeywords", publication.getKeywordText() );
				pubOfIJsonArray.put( obj );
			}

			pubofIObj.put("publicationsOfI" + i, pubOfIJsonArray);
			cfCResult.put(pubofIObj);
		}
		System.out.println( "Output: " + cfCResult.toString() );
		
		System.out.println( "Total time: " + (System.currentTimeMillis() - time) );*/
		
		String coAuthorIDs = "[a879aa00-0b87-427b-8e4c-9eee6c961fdf, 51545b44-0b8f-4706-9a51-f303b9f26d6c, d4ef27a8-703c-4e5d-b004-66935a8f1140, 4195932f-a2bd-4b5e-bb2f-8921c4eabbc0, 7610c9fd-632a-41ea-8b7a-b729a9b059ce, 8b74189a-5ac3-480f-b0bd-9a7a749b865c, 33f228f1-4f57-4089-abd7-56d3af88aeaf, d9d768b1-f7d5-4c99-b728-d54f77c86a64, a63adce7-25b8-4334-8d00-32b62197afdb, e03b804a-265d-435b-842c-ab8c1298fdec, 18643edf-35cd-48ea-965d-d605f5386bf1, e89e5895-1f26-455a-9ce8-bd841cbcc002, bdc8ba62-6f8c-4b8c-91c2-85f4685103b3, a40b04d1-459f-4a6e-8601-0682ff6a512d, 970b413d-e7eb-4a60-acb7-aa9a8db624d2, 13f25ad5-a171-4977-8df5-a7d3eb40f43b, 4acc6f4e-530a-43c9-9aa4-3ebc87f1587d, 2be80369-b40f-4351-9f11-7ec0931f9522, 4a323894-1f52-4640-bb5c-8168c165ec58, b3820bb8-201c-4cf2-a965-93d89c42436c, f71fc4db-c693-426f-afeb-6cd775d91db7, ab7482d2-780f-462b-be86-553dcb96f4e7, 5b24e81a-9ce2-4a4f-9e09-bb109aa693fa, 75f61efc-92fa-4a38-b50c-e25c5d8effe7, d282a802-1280-4c7e-bc28-d9f94a11110a, 7396997c-306e-49c7-b953-ea8482a24732, e2f28735-f360-4a5a-ba5d-5e5f54017f20, 48f490d6-68d4-46ed-953d-c47293d6b9fd, cad63fc7-1d43-4606-873f-c94191a6adcc, 06da803f-af17-4baf-96fa-79ca62aa1dc6, 5101a240-edfd-4750-85b0-76697630898f, ce9a5b1c-b161-4a8a-81e7-9a004af6a791, ba016361-45a8-4813-ac6a-3afc5792e425, 99567bb7-33fb-44e2-b868-b354dfa2757d, dba842c1-79e3-44a1-978a-e506bd9243a0, d4a5e942-91e4-46e1-87ad-c5a7afbfb329, e459449a-8627-4ea5-b887-e6283358c061, 013e8bae-29de-46de-92ba-3d05d657d203, c2e5472f-57c0-4229-a27f-d06ea629819c, ae1fd8ca-ad9a-4fbe-a126-f4bacdce8478, 3c1b404d-6078-44f5-a81c-395329ee027e, a9e504a6-bdc9-4858-80c6-e0f8dcb606d1, cd4dd0c6-b81a-4e37-8e2f-b0a283066ed2, 8602e43d-53b1-4adb-a1dc-601359912e52, e6c3fb8b-c000-4c46-992e-0177adee897e, 6f8a8a1a-0048-4f74-85c0-c199fc3448a4, 7623bc71-aac7-43db-9979-0678946dc767, 7e578a6c-1988-4163-89ba-0b99e9dd6b03, 443bc950-e0e4-4ae3-bef7-da74b7605526, e55c3a95-36ca-4b66-91a1-4f5416a30d84, 4a866011-85b9-45c0-96ad-d0de0fac7154, 103d22ef-e5e2-4d7a-9400-8c8e58e8c6f0, 5832352e-dab4-44d5-ae46-5911b0b04380, a223a90d-c964-4b47-9087-d5553aa5833e, 87101493-f67e-4b7d-bfac-4830b924c121, b38cb52a-8057-4206-9743-0240b6cc2f81, 8a343b16-7d38-44a7-b8a4-ccb7bf5ac318, 5baee1e1-24b2-4c54-abad-d7f73cae96b8, ad1ae99b-f640-43fb-8890-879c947b9db4, 16d3067e-0afe-43aa-a15a-349e34805816, 0e831ad9-7bf2-4f9f-8636-139c2555cd91, 034b6818-2c56-43df-bd11-4cf2ed6231a2, 68db9ceb-5d06-4049-81c1-49647c72ba79, d74c97eb-1e53-4f12-b468-cf4b0640aea6, 77d60fe1-f16c-498f-9eaa-0ec70c274865, ec6c1812-21d0-434b-b6a5-6a2a00b38acf, 2fdab8c0-7967-43df-bee2-f1dcede549e1, 00448581-26a3-461c-bbdb-9174daaf0f2f, 579e8606-18d1-42fe-af77-532231bc4e80, b2881ab9-e68f-4872-a258-449e5653cc44, d651a8e5-24a5-49a1-9cd6-1d93fd81014c, 49f0fdce-b695-42ad-bca5-4f266dca2521, b3eb7a49-701a-42fe-b3fd-0804020acb46, 120ada39-0011-46f3-9bd0-dccba30405a2, 9e45810b-51fa-47ea-8fba-b33e373cae23, f620e911-b4aa-4c5f-8fbb-1a31c63bb6b2, d2be3415-4c11-484e-9b9e-8a4ae51ee44e, 4f90825e-a31a-43f4-b6f5-f61b65e4c09c, 09fe37a6-8568-48e4-826b-d249eb478337, 02c462fa-b65e-4c48-b43e-f5be3cfa5702, b966ae05-e777-47b5-8ded-2abc9d9ca969, 5633e767-50d6-48eb-b773-7b4236d8e930, 0bb5fcd5-cede-4038-b2b4-c1a761b8a675, d154bf7c-f10f-4332-8ebe-9479a91ebe9b, 58d77609-6a7d-4239-adc3-67eab4215746, 80074022-057f-4494-b533-5d94493d66db, da362a75-45d8-4798-af24-add9fcd8fd71, 91ed01ce-8f89-4622-82db-7347dfadc83b, a8291f82-06c9-4240-8fb3-ddb24bcaa295, 49e63518-43ce-4092-a8bf-569b99eacd16, 15b9a3c5-6d92-4cfd-a9d3-b18bae7f9394, 71efe3a4-0e45-4ad2-ac30-016e041ad5e9, cc072550-a67f-4060-8c62-ca766539628c, 8642b002-4e22-4f4f-a366-da6cee28bc53, 1359aacb-3153-402a-9327-cd2ac3949c3f, 5059f6af-f16d-4e6b-a1c9-b7ec817b30f9, c750b471-d80e-4fe0-8708-235976c4cd57, 599f6d8e-2090-45e7-8468-01af5dda5586, 1f3a56cc-57d4-4700-b25f-f3f08f7b9f47, dd3acc75-e505-4bf8-aa5c-3a9b866b2129, 99b6bb94-3042-4502-b738-bf8b1691910f, 37d3c514-7ed2-44b5-ad67-ac6bf6734a80, 41203772-7238-454f-a86f-502cea03c750, 7c20e044-6dd2-45ee-8e17-af8302f49a00, c3148168-1fd5-4b74-827f-93cf122242f4, 23fb9a6c-df41-4391-b14e-83f97df88404, b6af3c2a-56ce-4d82-b062-51b51ae327ae, 6f9920b8-067f-4b99-bf38-11ef1757a4e9, a541327a-6437-4f44-9804-b8aa56757e3d, 157a1a39-a326-4a6f-8f2d-85418f4b62fd, 78dacb8d-51a2-4d7d-893c-f61aaf43f548, 689ccbb9-96c2-44e3-a777-fd13ba46178f, bd938633-5469-404b-8c8b-1de194edd482, 3450f0a9-76dd-4dc9-b250-e1e1ef76836f, a7177728-66e4-4cb2-ba81-002e5f3abcb9, e349c9a8-d36b-431f-b65f-793eb12ebc94, 44781092-8c64-4710-8bd3-b33f9e7d2565, dc88ed04-7b44-4eda-aef5-3c8966f7dd0d, 9382db3e-c314-45b2-9ff9-78c9d90661cd, b62b592b-d98b-48da-97d1-b52e10179232, 934404a5-b658-4bc5-8600-e82f85bb256e, d4857aab-29ed-45fc-9d4e-782954c2473d, 3ed7c6ac-903b-4428-9834-0ebdf425a4f7, 1c90013c-fa25-4dbc-9dd2-d742b0b5aefa, 56c2e78b-c20b-4b4d-8b6d-0a0b18ae8d27, 9372c252-31be-470a-a219-19c57595cbae, dbece611-ebb7-4bce-a49d-1d8a4cd1a7c9, 5db19a9c-ad0a-47f3-bc8a-b05ccb3f9d99, 8029d627-eaa7-4e23-8856-d67ae0fb6fc6, 628cc1f1-abeb-48b9-b1d6-0d9a890adc31, 4b008854-2706-466e-9c74-b212954cfbc8, d448f1d8-89f8-4152-801f-99ec81ed355c, 388e65f0-f7c7-40f2-8354-fe5597986f8b, ee49d3fe-f85f-4346-9bcb-d5045f5037cd, 14aa0501-8f0d-41b7-8ab8-bd1933cc79fe, de4ff7bc-0444-4210-a93c-f8194c6f3bb2, f04deb30-7995-41d8-ab4b-9cb9250116f0, 3692256f-ac96-4389-baf8-b147b1457740, f0ac1889-e12a-41bc-8326-4b4c4a526082, 61cff149-07ba-4efd-bd46-afc6abeb04ef, 901207ed-74e0-4299-8757-1410df2405b2, ed33d9d8-b559-4484-a8ad-541c3f4397f1, 0c320ad4-896e-470e-ad63-48811f72d98f, 1b65bcc5-d543-4f9a-b563-b0a4add6f109, ac8e7013-c48a-4bcb-a4e5-ce8176620526, 02546436-5947-424e-9ece-410eb76f2a22, 6b3a8412-6d30-4486-981b-b4926be5a5d6, 06a3e876-3d23-4f20-87c6-8c43e5422554, c46957ac-1e0e-4b49-b753-9c9626b3e96c, 79f6ec33-bcd2-43a7-8824-30927d6b5047, 28eba1a2-ed2c-411a-b2ce-d899397aba35, 631908e8-ed3b-4675-93b1-04dd22b85f43, be26d351-f88e-464b-9c06-8858b023f08a, 0d8faf29-6c67-458d-9821-14952985071e, 14a386ff-9a9a-4f95-964d-5868357ac067, 6460b02d-f279-4138-8114-3287c3ca2954, 91b67ac8-8370-474f-9c79-68200b15def5, 5d101b96-0d5a-45f0-bb6d-33f81900b6cf, d84ef48b-0533-481b-890e-7c577a413042, 83f63e81-5395-4da5-9598-d521ed58016e, a9c298ad-feea-4b6a-ac3c-23aa73a16d20, 7efee178-0932-4fbb-bfce-c4a92d197d3b, e6383739-678d-4d70-9b7c-b862f643f548, cef9a518-98c8-4f6f-841c-bc6e9a370fc0, e50455ab-f280-4b2f-821f-31b402069f1f, 015c31ba-a182-4760-80d9-f996ac5cbcfa, fd33d603-403a-4477-9563-cf00fd175b17, fd1f3626-3a48-4c87-ae8f-99a2dfba4bfe, 8951060b-1624-4a67-84a7-7e895b7befa4, 47831c78-41c9-4def-9565-a516dc4d2209, feb96397-d383-4d9e-a29a-9770bdbfa9e2, bc57b69c-093d-4d81-8e39-a482f3d5a0c9, 4a56302e-3fda-446c-9b95-194a769a6241, dc64b741-3f69-45d0-8c34-26a499d452e8, 0a2131ac-c736-4fc3-ab54-a154306cb0b9, 385c8066-e5de-4a6a-80a9-fdd27afc51e0, 93b0c4a0-2e5b-419e-adb0-9a73f5629086, 668eb906-5f29-4c01-836c-f9715f5f5d8c, 0ab1dbe8-0676-4a5f-8263-a30092d97a79, 92334d42-c351-4427-9526-d749e20fb84a, f438e15c-f82c-46ea-9d4a-6c349295e1b1, ac56aedb-0379-4833-a134-a7dab0cd4a81, 0f02c7d1-c20b-419f-8ddf-e0eaf2909ea0, dbb5dd33-8290-49db-8244-b697e25e19c9, 03636ee0-cde7-459a-8f9b-ed30e56309d4, 31a77eb4-9e19-4704-9d12-452421f9df1c, bdd34ae3-68d0-4d96-bae7-6e5df9fd933d, b91822ee-6223-4cdb-8a90-cfab8484e173, 8d95e796-2da4-4649-a1ca-62bf2234207a, 7355514d-2044-4357-9d9f-3f06ab2abfdc, d099001e-449e-4f99-8d42-928c7e00e474, d012808c-b9bb-4803-93ef-113e678ab8c3, 1f3e39a8-36dd-4997-a615-c9d29e6fce5f, 115f20f6-6f73-4fdb-8044-fc48015779e8, 0dea07ae-ebe5-4bde-b17a-46c36fc93db8, e4f8f15d-e8fb-49dd-b526-0c9cc2f20318, ea65023b-cdd6-4cd6-a1de-0d9eaf3de80f, bd73c726-71f4-42b0-afd6-ebda26e7da1a, d69211fc-8012-43cc-8c48-ea6619b3b180, 866c2bfd-cbd1-457e-a1c9-3c29b03bfa86, 4f0cec3b-ab19-4b2e-aac0-923738a8b9e0, 2741ff68-9a47-4360-b3d6-55bb9eebd43f, 7e27c42c-f7f5-4345-bd78-2e9140e6e518, ab37c3db-f5f3-4301-9a68-7345e0d57fc0, 17cf7070-9673-4751-8261-5031f3838eda, 6cc7cbe0-df49-4802-b977-1dfd16aefc23, 90186e0f-c687-411c-a5e4-cc8bf2aef455, 1c94d5f2-5998-417f-8cf5-7b6595ec2cae, e4e64391-e7e0-4b75-b2ad-9e4bafb0775f, 06f6a4bd-8eb3-4803-aa9a-9bbcea805674, 0366e248-23d2-4a8a-975f-018d5b19cf72, 2ffb8cd9-fd0f-440a-b21e-5e39c50ba733, 4145fd84-421d-4ea8-98e3-a660da41148d, aa6984f3-42b1-4606-b193-0fe3952d5365, f3818fe8-b87d-48f7-8d63-a6060981f84b, 6a11d35e-34c8-48fd-a0e8-42adc80a110f, 4876a948-54af-4fe5-8f3e-a05b8c06d162, c1f771a5-38f4-43f8-bcc7-9b1013fa85da, 81bc5de3-a224-4896-a0d8-fa8ddb6e32b2, f988bbce-3708-4f9c-a89f-09533687cbdf, d3659449-6422-4bd8-9bfb-c3ff0baef2b8, c99420dc-f4a2-4f3e-b01a-db711cf01525, 2213d9a4-e614-4550-aa84-4302ed569de0, 735d6a0a-e6a0-4d29-8fdc-3cb596df5558, 66526e04-14e1-418b-9220-718842492a5d, a1e925be-4071-4481-a929-92b7121b93d2, ccb74015-8e2b-4833-b87e-1cdc3f293e63, 3d198d22-9749-4ad7-8458-f5edd617eba0, 04c1a4a3-b6d8-4e60-a83b-fdfe0668c158, 2f1ec8de-9eef-469a-92ad-03cdfbe6d8f4, ea7643e3-677d-4f6d-89c6-995ac06d3905, a3e6dd49-3202-4386-ac2c-976a367cd9dc, 6310e7bc-2cc2-4335-a79d-bd2b22c947d5, 08ea4648-a729-4cd3-b663-787d803e874c, 3613943c-9b52-403c-8b90-14357aaa2f20, 65fb084a-ffb2-45fe-90b8-051846e9fc30, 8f3bea87-d569-4ee5-bcfa-6b80f0fe5e22, 3faa7f2b-6514-4041-a842-dce8764aba08, d900ebde-717f-428f-9d27-7fa27763f09e, 84f9fcc6-80d8-4bc3-86e9-86508621bc98, a2fa09a4-2ec1-4f1c-8993-df40c81df8b3, e97f519a-f80e-41ad-9405-39147cb7b113, 650948dc-6aed-4a35-840f-d5856be6ac99, c6a648c8-5a57-4518-95b2-8631e9d73f51, df333ae0-2f95-4198-b9d4-7725d985418e, 62dafe61-9a19-44ed-9e2c-3091fb916306, d03d4050-9405-45f0-ba4d-f2bd1cb5fb62, 81928415-f301-4d0b-bae4-73f7fa613828, 42d1994a-99cb-4aec-8fc8-6f7ac9217cca, 333793b2-5614-42fb-8df3-1bda3cb340df, 1cb09a51-3e45-4f92-98cc-271080b1abba, f840bcb7-2ea8-493f-afe5-d293cf283d46, 97900616-96cc-4388-8d78-8a3c76c79178, 8342f224-80d3-4f5f-923f-8c50f1f71342, ec2b857d-4566-4542-b300-4c2be317e19b, 543b0b65-ec63-43f9-9214-fe6acc928521, 25ffb9dc-79ff-4614-9836-ebac62735f1b, 6f81fb96-dfab-4bd9-9517-2b418545aa04, 8fbef7bd-ab9b-4312-b6d8-2be752cca1d4, a375879a-d79f-4539-9547-4cd301bb47f7, 06092aa5-9bcf-42b0-bdc7-cb7fb0deb527, ae7754e1-1b05-4b62-8e10-17688b3f7a4a, 35b84b55-6ac5-4c94-b496-250ce258b9a7, 8b06b14d-41d6-4fc8-aad6-2bff3d238693, 7f2953a3-1bc2-42d3-a9a2-895ca0943772, 4f5db57d-4a69-4275-a687-e4e2412ec554, 4ddf0010-aeca-48b5-aa32-837386e71324, 9b656930-5f65-407e-a351-ecfb3edf5176, df044bc7-55f2-410d-80a2-976de1ad5cbc, a2900553-2ad8-4092-8c41-5760b9eae7dd, 836a7c0f-9c00-4d4b-a105-883eccff14f0, 0cd720ef-1c17-46ba-84c7-51f8e828ab38, 8d1b5a6d-2c2f-40b3-acb3-f1c3304728e7, 823e5a11-a666-4fa9-a198-0c7a87aaaabf, b567b0d8-3938-4099-8509-ae73771e2bcc, 17259073-d1f9-4dc4-b16c-c91602bacec0, 0172975b-d6f8-420e-a991-56d53ca620fb, 1d5ee180-75a4-4d94-ad48-e18977ebc1cf, 7fb13c72-562d-4acb-ab22-5fb8ae3d103d, f55f7bb7-b82f-45cb-894c-c62f6bc697f5, a879aa00-0b87-427b-8e4c-9eee6c961fdf]";
		
		try
		{
			/*JSONArray array = recommendations.computeSNAINRecommendation( 
					auth, 1 );
			array = recommendations.computeSNAINRecommendation( 
					auth, 2 );
			//System.out.println( "array: " + array );
			array = recommendations.computeSNAINRecommendation( 
					auth, 3 );
			array = recommendations.computeSNAINRecommendation( 
					auth, 4 );
			array = recommendations.computeSNAINRecommendation( 
					auth, 5 );
			System.out.println( "array: " + array );*/
			/*List result = sessionFactory.getCurrentSession()
					.createSQLQuery( "CALL nugraha.CoAuthorGraph( :researcher )" )
					.setParameter( "researcher", "c442983a-0099-4d6d-89b1-6cfc57fa6138" ).list();
			
			if ( result != null && !result.isEmpty() )
			{
				System.out.println( "Result : " + result );
			}*/
			
			/*Query quer = sessionFactory.getCurrentSession()
					.createSQLQuery( "CALL nugraha.InterestGraph(:authorID, :coauthors, :update);" )
					.setParameter( "authorID", "a879aa00-0b87-427b-8e4c-9eee6c961fdf" )
					.setParameter( "update", false )
					.setParameter( "coauthors", coAuthorIDs.replace( "[", "" ).replace( "]", "" )
							.replaceAll( ", ", "\',\'" ) );
			sessionFactory.getCurrentSession().createQuery( "" );
			List array = quer.list();
			for ( Object obj : array )
			System.out.println( "ArrayString: " + obj );*/
			
			/*Query quer = sessionFactory.getCurrentSession()
					.createSQLQuery( "CALL nugraha.GetCoAuthorGraph( :researcher )" )
					.setParameter( "researcher", "a879aa00-0b87-427b-8e4c-9eee6c961fdf" );*/
			//List result = quer.list();
			//System.out.println( "Value: " + result.size() + "  -  " + result );
			/*StringBuilder mainQuery = new StringBuilder();
			mainQuery.append( "SELECT DISTINCT " );
			mainQuery.append( "pub.id, pub.title, pub.authorText, pub.abstractText, pub.keywordText, " );
			mainQuery.append( "(SELECT GROUP_CONCAT(id,'') FROM nugraha.author WHERE name REGEXP REPLACE(pub.authorText, ',', '|') AND id IN (:author)) AS authorIds " );

			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append( "FROM publication pub " );

			stringBuilder.append( "LEFT JOIN publication_author pub_auth ON pub.id=pub_auth.publication_id " );
			stringBuilder.append( "WHERE (INSTR(pub.title, :term) OR INSTR(pub.abstractText, :term) OR INSTR(pub.keywordText, :term)) " );
			stringBuilder.append( "AND pub_auth.author_id IN (:author) " );
			stringBuilder.append( "ORDER BY pub_auth.position_ DESC" );
			
			List<Object[]> pubs = sessionFactory.getCurrentSession()
					//.createQuery( "FROM PublicationAuthor" )
					.createSQLQuery( mainQuery.toString() + stringBuilder.toString() )
					.setParameter( "term", "intelligent tutoring system" )
					.setParameterList( "author", Arrays.asList( coAuthorIDs.replace( "[", "" ).replace( "]", "" )
							.split(", ") ) ).list();
			JSONArray array = new JSONArray();
			List<String> authors = new LinkedList<>(Arrays.asList( coAuthorIDs.replace( "[", "" ).replace( "]", "" )
					.split(", ") ));
			for ( Object[] publication : pubs )
			{
				//Publication publication = pubDao.getById( pubID.toString() );

				String id = String.valueOf( publication[0] );
				String title = String.valueOf( publication[1] );
				String author = String.valueOf( publication[5] );
				String abstrac = String.valueOf( publication[3] );
				String keyword = String.valueOf( publication[4] );
				JSONObject obj = new JSONObject();
				obj.put( "pID", id); //publication.getId() );
				obj.put( "pTitle", title); //publication.getTitle() );
				obj.put( "pAuthors", author);//publication.getAuthors().toString().replace( "[", "" ).replace( "]", "" ));
				obj.put( "pAbstract", abstrac);//publication.getAbstractText() );
				obj.put( "pKeywords", keyword);//publication.getKeywordText() );
				array.put( obj );
			}
			System.out.println( "pubs: " + array );
			System.out.println( "Total time: " + (System.currentTimeMillis() - time) );*/
			
			genRec.requesetAuthor( "Ulrik", 10, "author" );
			/*JSONArray arr = genRec.computeRecommendation( "c3d", auth, 1 );
			if ( arr != null )
				System.out.println( "Array 1: " + arr );
			else
				System.out.println( "Array 1 is null." );
			

			arr = genRec.computeRecommendation( "c3d", auth, 2 );
			if ( arr != null )
				System.out.println( "Array 2: " + arr.length() );
			else
				System.out.println( "Array 2 is null." );
			

			arr = genRec.computeRecommendation( "c3d", auth, 3 );
			if ( arr != null )
				System.out.println( "Array 3: " + arr.length() );
			else
				System.out.println( "Array 3 is null." );			

			
			arr = genRec.computeRecommendation( "c3d", auth, 4 );
			if ( arr != null )
				System.out.println( "Array 4: " + arr );
			else
				System.out.println( "Array 4 is null." );

			
			arr = genRec.computeRecommendation( "c3d", auth, 5 );
			if ( arr != null )
				System.out.println( "Array 5: " + arr );
			else
				System.out.println( "Array 5 is null." );*/
		}
		catch (JSONException /*| SQLException | IOException | TasteException*/ e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*try
		{
			UtilIN util = new UtilINImp();
			ArrayList<String> interest = util.interestSNFileCreator( auth.getId(), persistenceStrategy, sessionFactory, 
					researcherFeature, service );
			System.out.println( "Interest List: " + interest );
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		/*try {
			Map<String, Object> vals 
			= getAuthorInterestById("c442983a-0099-4d6d-89b1-6cfc57fa6138", false);

			Set<String> keys = vals.keySet();
			System.out.println("Keys: " + keys);
			for(String name : keys) {
				System.out.println("Object value: " + vals.get(name));
			}

			Map<String, Object> autherInterests = new LinkedHashMap<String, Object>();

			if(vals.get("status").equals("Ok")){
				LinkedList<Object> arr = (LinkedList)vals.get("interest");

				if(arr != null) {
					for(int i=0; i<arr.size(); i++) {
						ArrayList<Object> items = (ArrayList<Object>) arr.get( i );
						System.out.println( items.toString() );
					}
				}
			}
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			System.out.println("Error occured: " + e1.getMessage());
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/

		//((Publication)author.getPublications().toArray()[0]).getPublicationTopics().
		/*
		AuthorInterestDAO interest = persistenceStrategy.getAuthorInterestDAO();
		for(AuthorInterest ai : interest.getAll()){
		System.out.println(ai.getId());
		}*/
		/*if(list != null) {
			System.out.println("Found users.");
			for (User user : list)
			{
				System.out.println("Found users name : " + user.getName());
				RecommendationFeatureImpl rm = new RecommendationFeatureImpl();
				try {
					JSONArray arr = rm.computeEigenvector(user);
					if(arr != null)
						System.out.println("User id: " + user.getId() + "  : " + arr.toString());
					else
						System.out.println("No user information found.");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (TasteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("Found no users.");
		}*/
	}
}

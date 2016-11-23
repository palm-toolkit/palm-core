package de.rwth.i9.palm.recommendation.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.mahout.cf.taste.common.TasteException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.feature.circle.CircleFeature;
import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.User;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.recommendation.service.SNAINRecommendationFeature;
import de.rwth.i9.palm.service.SecurityService;

@Controller
@SessionAttributes( { "sessionDataSet" } )
@RequestMapping( value = "/user" )
public class RecommendationController {

	private static final String LINK_NAME = "recommendation";

	@Autowired
	private SecurityService securityService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private SNAINRecommendationFeature recommendationFeature;
	
	@Autowired
	private SNAC3RecommendationFeature c3recommendationFeature;
	
	@Autowired
	private SNAC2RecommendationFeature c2recommendationFeature;

	private static int totalNo = 0;
	
	private String researcherId = null;
	
	@SuppressWarnings( "unchecked" )
	@Transactional
	@RequestMapping( value = "/check_recommendation_request", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> recommendationRequest( 
			@RequestParam( value = "id", required = false ) String Id,
			@RequestParam( value = "query", required = false ) String query,
			@RequestParam( value = "author", required = false ) String author,
			final HttpServletResponse response )
	{
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		List<Object> responseListRecommendation = new ArrayList<Object>();
		if ( query != null )
			researcherId = recommendationFeature.requesetAuthor( author, query );
		else
			researcherId = null;
		
		if ( researcherId != null && !researcherId.isEmpty() )
			responseMap.put( "pub_recommendation", createResearcherNode( persistenceStrategy.getAuthorDAO().getById( researcherId ) ) );
			
		
		responseMap.put( "count", responseListRecommendation.size() );
		responseMap.put( "status", "ok" );
		return responseMap;
	}
	
	@SuppressWarnings( "unchecked" )
	@Transactional
	@RequestMapping( value = "/single_tree_recommendation", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getSingleTreeRecommendation( 
			@RequestParam( value = "id", required = false ) String Id,
			@RequestParam( value = "stepNo", required = false ) Integer stepNo,
			@RequestParam( value = "query", required = false ) String query,
			@RequestParam( value = "page", required = false ) Integer page, 
			@RequestParam( value = "maxresult", required = false ) Integer maxresult, @RequestParam( value = "fulltextSearch", required = false ) String fulltextSearch, @RequestParam( value = "orderBy", required = false ) String orderBy,
			final HttpServletResponse response )
	{
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		List<Object> responseListRecommendation = new ArrayList<Object>();
		
		JSONArray arr = null;
		try 
		{
			Author author = null;
			if ( researcherId == null || researcherId.isEmpty() )
				author = securityService.getUser().getAuthor();
			else
				author = persistenceStrategy.getAuthorDAO().getById( researcherId );
		if ( recommendationFeature != null && query != null && query.equals( "interest" ) )
		{
			arr = recommendationFeature.computeSNAINSingleTree( author, stepNo, Id );
		}
		else if ( query != null && query.equals( "c3d" ) )
		{
			arr = c3recommendationFeature.computeSNAC3Recommendation( author );
		}
		else if ( query != null && query.equals( "c2d" ) )
		{
			arr = c2recommendationFeature.computeSNAC2Recommendation( author );
		}
		}
		catch ( IOException | JSONException | SQLException | TasteException e )
		{
			
		}
		
		if ( arr != null )
		{
			responseMap.put( "single_tree_recommendation", printRecommendationResults(responseListRecommendation, arr.optJSONObject(0)) );
		}
		
		responseMap.put( "count", responseListRecommendation.size() );
		responseMap.put( "status", "ok" );
		return responseMap;
	}
	
	@SuppressWarnings( "unchecked" )
	@Transactional
	@RequestMapping( value = "/recommendation", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getRecommendationList( 
			@RequestParam( value = "id", required = false ) String Id,
			@RequestParam( value = "requestStep", required = false ) Integer requestStep,
			@RequestParam( value = "creatorId", required = false ) String creatorId,
			@RequestParam( value = "query", required = false ) String query,
			@RequestParam( value = "page", required = false ) Integer page, 
			@RequestParam( value = "maxresult", required = false ) Integer maxresult, @RequestParam( value = "fulltextSearch", required = false ) String fulltextSearch, @RequestParam( value = "orderBy", required = false ) String orderBy,
			final HttpServletResponse response )
	{
		long time = System.currentTimeMillis();
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		if( securityService != null ) 
		{
			Author author = null;
			if ( researcherId == null || researcherId.isEmpty() )
				author = securityService.getUser().getAuthor();
			else
				author = persistenceStrategy.getAuthorDAO().getById( researcherId );
			try {
				List<Object> responseListRecommendation = new ArrayList<Object>();
				JSONArray arr = new JSONArray();
				
				if ( requestStep == 0 )
				{
					responseMap.put( "pub_recommendation", createResearcherNode( author ) );
				}
				else {
					
					//get recommendations spcific to algorithm
					if ( recommendationFeature != null && query != null && query.equals( "interest" ) )
					{
						arr = recommendationFeature.computeSNAINRecommendation( author, requestStep );
					}
					else if ( query != null && query.equals( "c3d" ) )
					{
						arr = c3recommendationFeature.computeSNAC3Recommendation( author );
					}
					else if ( query != null && query.equals( "c2d" ) )
					{
						arr = c2recommendationFeature.computeSNAC2Recommendation( author );
					}
				}
								
				//insert id/widget specific data
				if ( creatorId != null && creatorId.equals( "publication" ) && 
						arr != null && arr.length() > 0 )
				{
					//stepId related graph data
					if ( requestStep != 6 )
					{
						if ( requestStep == 1 )
							totalNo = 0;
						//inserting all the recommendation data
						/*for ( int i = 0; i < arr.length(); i++ )
						{*/
							//printRecommendationResults(responseListRecommendation, arr.optJSONObject(0));//responseListRecommendation.add( arr.get( i ) );
						//}
						//System.out.println( "ArraySize Before: " + arr.optJSONObject( 0 ).optInt( "count" ) + "  =  " + totalNo );
						responseMap.put( "pub_recommendation", printRecommendationResults(responseListRecommendation, arr.optJSONObject(0)) );
						//System.out.println( "ArraySize After: " + arr.optJSONObject( 0 ).optInt( "count" ) + "  =  " + totalNo );
						
						/*Map<String, Object> items = printRecommendationResults(responseListRecommendation, arr.optJSONObject(0));
						responseMap.put( "pub_recommendation", items );*/
					}
					else
					{
						responseMap.put( "pub_recommendation", printPublicationRecommener( arr ) );
					}
				}
				else if ( creatorId != null && creatorId.equals( "co_authors" ) && 
						arr != null && arr.length() > 0 )
				{
					totalNo = 0;
					responseMap.put( "pub_recommendation", printPublicationRecommener( arr ) );
					//responseMap.put( "coauthor_recommendation", printCoAuthorRecommener( arr ) );
				}
				
				responseMap.put( "count", responseListRecommendation.size() );
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
		responseMap.put( "status", "ok" );
		
		System.out.println( "Total Time for step " + requestStep + ": " + (System.currentTimeMillis() - time) );
		return responseMap;
	}

	private Map<String, Object> printRecommendationResults( List<Object> responseListAuthor, JSONObject arr ) 
	{

		/*if ( totalNo != 0  && totalNo != 4 )
		{
			if ( arr.has( "count" ) )
			{
				String json = arr.toString();
				int totalNodes = arr.optInt( "count" );
				for ( int i = 0; i < totalNodes; i++ )
				{
					String id = "\"id\":"+i+",", targetId = "\"id\":" + ( totalNo ) + ",",
							target = "\"target\":"+i+"}";
					json = json.replace( id, targetId )
							.replaceAll( target, "\"target\":" + ( totalNo ) + "}" )
							.replaceAll( "\"source\":"+i+",", "\"source\":" + ( totalNo ) + "," )

							.replaceAll( "\"target\":"+i+",", "\"target\":" + ( totalNo ) + "," )
							.replaceAll( "\"source\":"+i+"}", "\"source\":" + ( totalNo ) + "}" );
					totalNo++;
				}
				arr = new JSONObject( json );
			}
		}
		else
		{
			if ( arr.has( "count" ) )
			{
				totalNo = arr.getInt( "count" );
			}
		}*/
		
		JSONObject item = arr;

		Map<String, Object> itemList = new LinkedHashMap<>();
		if (item != null) {
			Set<String> keys = item.keySet();

			for ( String key : keys ) {
				List<Object> list = new LinkedList<>();				
				if(key != null && !key.isEmpty()) {
					JSONArray rec = item.optJSONArray(key);
					if ( rec != null )
					{
						for (int j=0; rec != null && j<rec.length(); j++) {
							JSONObject lItem = rec.optJSONObject(j);
							Map<String, Object> tempList = new LinkedHashMap<>();
							if(lItem != null && lItem.length() > 0) {
								Iterator<String> k = lItem.keys();
		
								while (k.hasNext()) {
									String cKey = k.next();
									Object temp = getItem(cKey, lItem);
									tempList.put(cKey, temp);
								}
							}
							list.add(tempList);						
						}
						itemList.put(key, list);
					}
					else 
					{
						itemList.put( key, item.opt( key ) );
					}
				}
			}
		}
		responseListAuthor.add(itemList);

		return itemList;
	}

	private Object getItem (String key, JSONObject obj) {
		if (obj.optJSONArray(key) != null) {
			List<Object> returnList = new LinkedList<>();
			JSONArray arr = obj.getJSONArray(key);
			for(int i=0; i<arr.length(); i++) {
				JSONObject temp = arr.getJSONObject(i);
				Map<String, Object> list = new LinkedHashMap<>();
				Iterator<String> keys = temp.keys();
				while (keys.hasNext()) {
					String cKey = keys.next();
					list.put(cKey, temp.get(cKey));
				}
				returnList.add(list);
			}
			return returnList;
		} else {
			return obj.get(key);
		}
	}

	private Map<String, Object> printCoAuthorRecommener( JSONArray arr )
	{
		JSONArray rItems = null;
		JSONObject listItems = new JSONObject();
		
		int ids = -1;
		
		//collecting information
		for ( int i = 0; i < arr.length(); i++ )
		{
			JSONObject temp = arr.optJSONObject(i);
			
			if( temp != null )
			{
				if(temp.has("sResearchers"))
					rItems = temp.optJSONArray("sResearchers");
				if(temp.has("iListInfo0"))
					listItems.put( "item0", temp.optJSONArray("iListInfo0") );
				if(temp.has("iListInfo1"))
					listItems.put( "item1", temp.optJSONArray("iListInfo1") );
				if(temp.has("iListInfo2"))
					listItems.put( "item2", temp.optJSONArray("iListInfo2") );
				if(temp.has("iListInfo3"))
					listItems.put( "item3", temp.optJSONArray("iListInfo3") );
				if(temp.has("iListInfo4"))
					listItems.put( "item4", temp.optJSONArray("iListInfo4") );
				if(temp.has("iListInfo5"))
					listItems.put( "item5", temp.optJSONArray("iListInfo5") );
				if(temp.has("iListInfo6"))
					listItems.put( "item6", temp.optJSONArray("iListInfo6") );
				if(temp.has("iListInfo7"))
					listItems.put( "item7", temp.optJSONArray("iListInfo7") );
				if(temp.has("iListInfo8"))
					listItems.put( "item8", temp.optJSONArray("iListInfo8") );
			}
		}
		

		List<Object> nodeList = new LinkedList<>();
		List<Object> linkList = new LinkedList<>();

		//inserting Active user first.
		Author author = securityService.getUser().getAuthor();
		int authorId = ++ids;
		Map<String, Object> nodes = new LinkedHashMap<>();
		nodes.put( "id", new Integer( authorId ) );
		nodes.put( "title", author.getName() );
		nodes.put( "group", Float.MAX_VALUE );
		nodes.put( "expert_in", author.getAffiliation() );
		nodes.put( "type", "2" );
		nodeList.add( nodes );
		
		for ( int i = 0; i < rItems.length(); i++ )
		{
			JSONObject item = rItems.optJSONObject( i );
			
			if ( item != null )
			{
				//add nodes
				nodeList.add( getAuthor( item, ++ids ) );
				
				Map<String, Object> link = new LinkedHashMap<>();
				link.put( "source", (ids-1) );
				link.put( "target", ids );
				linkList.add(link);
			}
		}
		
		Map<String, Object> recomm = new LinkedHashMap<>();
		recomm.put("nodes", nodeList);
		recomm.put("links", linkList);
		recomm.put("count", nodeList.size());

		return recomm;
	}

	private Map<String, Object> printPublicationRecommener(JSONArray arr) {

		int id=totalNo-1;
		JSONArray rItems = null;
		JSONArray researchers = null;
		JSONObject pubItems = new JSONObject();

		for(int i=0; i<arr.length(); i++) {
			JSONObject temp = arr.optJSONObject(i);

			if(temp != null) {
				if(temp.has("rItems"))
					rItems = temp.optJSONArray("rItems");
				if(temp.has("sResearchers"))
					researchers = temp.optJSONArray("sResearchers");
				else if (temp.has("publicationsOfI0"))
					pubItems.put("pub0", temp.opt("publicationsOfI0"));
				else if (temp.has("publicationsOfI1"))
					pubItems.put("pub1", temp.opt("publicationsOfI1"));
				else if (temp.has("publicationsOfI2"))
					pubItems.put("pub2", temp.opt("publicationsOfI2"));
				else if (temp.has("publicationsOfI3"))
					pubItems.put("pub3", temp.opt("publicationsOfI3"));
				else if (temp.has("publicationsOfI4"))
					pubItems.put("pub4", temp.opt("publicationsOfI4"));
				else if (temp.has("publicationsOfI5"))
					pubItems.put("pub5", temp.opt("publicationsOfI5"));
				else if (temp.has("publicationsOfI6"))
					pubItems.put("pub6", temp.opt("publicationsOfI6"));
				else if (temp.has("publicationsOfI7"))
					pubItems.put("pub7", temp.opt("publicationsOfI7"));
				else if (temp.has("publicationsOfI8"))
					pubItems.put("pub8", temp.opt("publicationsOfI8"));
				else if (temp.has("publicationsOfI9"))
					pubItems.put("pub9", temp.opt("publicationsOfI9"));
			}
		}

		Map<String, Map<String, Object>> uniquePubs = new TreeMap<>();

		List<Object> nodeList = new LinkedList<>();
		List<Object> linkList = new LinkedList<>();

		Map<String, Integer> termNodes = new LinkedHashMap<>();
		Map<String, Integer> nodesTitles = new LinkedHashMap<>();
		int termId = -1;
		for (int item=0; item < rItems.length(); item++) 
		{
			JSONObject tempItem = rItems.optJSONObject(item);
			
			if (tempItem != null) {

				JSONArray pubs = pubItems.optJSONArray("pub"+item);
				Map<String, Object> termNode = getTermNode(tempItem, ++id);
				if(termNode != null) {
					nodeList.add(termNode);
					
					termNodes.put( tempItem.optString("itemName"), new Integer( id ) );
					//linking the terms
					/*if(termId != -1) {
						Map<String, Object> link = new LinkedHashMap<>();
						link.put("source", new Integer(termId));
						link.put("target", new Integer(id));
						linkList.add(link);
					}*/
				}
				termId = id;

				for (int pub = 0; pubs != null && pub < pubs.length(); pub++) {
					JSONObject pubItem = pubs.optJSONObject(pub);

					//String name = tempItem.optString("itemName");
					double group = tempItem.optDouble("DegreeValue", 5.0);
					String title = pubItem.optString("pTitle");
					//String authors = pubItem.optString("pAuthors");
					//String nodeTitle = title.replaceAll(" ", "").replaceAll("[^a-zA-Z ]", "").toLowerCase() + "-" 
					//					+ name.replaceAll(" ", "").replaceAll("[^a-zA-Z ]", "").toLowerCase() + "-" 
					//					+ authors.replaceAll(" ", "").toLowerCase();
					String nodeTitle = pubItem.optString( "pID" );
					Integer ids = null;

					if(nodesTitles.get(nodeTitle) != null) {
						ids = nodesTitles.get(nodeTitle);
						
						Map<String, Object> publication = uniquePubs.get( pubItem.optString( "pID" ) );
						String interest = String.valueOf( publication.get( "interest" ) );
						interest += ", " + pubItem.optString( "iName" );
						publication.put( "interest", interest );
						uniquePubs.replace( pubItem.optString( "pID" ), publication );
					}
					else {
						id++;
						ids = new Integer(id);//title + "\n" + name + "\n" + authors + "\n" + group;

						Map<String, Object> nodes = new LinkedHashMap<>();
						nodes.put("id", ids);
						nodes.put("title", title);// + "\n" + name + "\n" + authors);
						nodes.put("group", (group <= 0) ? 5 : group/1000);
						nodes.put("type", "6");
						nodeList.add(nodes);

						nodesTitles.put(nodeTitle, ids);
						

						Map<String, Object> publication = new LinkedHashMap<>();
						publication.put( "id", pubItem.optString( "pID" ) );
						publication.put( "title", pubItem.optString( "pTitle" ) );
						publication.put( "interest", pubItem.optString( "iName" ) );
						
						if ( pubItem.has( "pKeywords" ) )
						{
							publication.put( "keywords", pubItem.optString( "pKeywords" ) );
						}
						if ( pubItem.has( "pAbstract" ) )
						{
							publication.put( "abstract", pubItem.optString( "pAbstract" ) );
						}
						if ( pubItem.has( "pAuthors" ) )
						{
							publication.put( "author", pubItem.optString( "pAuthors" ) );
						}
						uniquePubs.put( pubItem.optString( "pID" ), publication );
					}

					Map<String, Object> link = new LinkedHashMap<>();
					link.put("source", new Integer(termId));
					link.put("target", ids);
					linkList.add(link);
				}
			}
		}

		//inserting the authors
		for ( int i = 0; i < researchers.length(); i++ )
		{
			JSONObject item = researchers.optJSONObject( i );
			
			if ( item != null )
			{
				//add nodes
				nodeList.add( getAuthor( item, ++id ) );
				
				String interestItem = item.optString( "ExpertIn" );
				int sourceId = termNodes.get( interestItem );
				
				Map<String, Object> link = new LinkedHashMap<>();
				link.put( "source", sourceId );
				link.put( "target", id );
				linkList.add(link);
			}
		}
		
		Map<String, Object> recomm = new LinkedHashMap<>();
		recomm.put( "publications", new LinkedList<>( uniquePubs.values() ) );
		recomm.put("nodes", nodeList);
		recomm.put("links", linkList);
		recomm.put("count", nodeList.size());

		return recomm;
	}

	private Map<String, Object> getAuthor ( JSONObject mainNode, int ids )
	{
		Map<String, Object> nodes = new LinkedHashMap<>();
		nodes.put( "id", new Integer(ids) );
		nodes.put( "title", mainNode.opt( "rName" ) );
		if( mainNode.has( "BetweennessValue" ) )
		{
			nodes.put("group", mainNode.opt( "BetweennessValue" ) );
		} 
		else if ( mainNode.has( "DegreeValue" ) )
		{
			nodes.put("group", mainNode.opt( "DegreeValue" ) );
		} 
		else 
		{
			nodes.put("group", mainNode.opt( "SimInPercent" ) );
		}
		nodes.put( "expert_in", mainNode.opt( "ExpertIn" ) );
		nodes.put("type", "2");
		return nodes;
	}
	
	private Map<String, Object> getTermNode(JSONObject mainNode, int ids) {
		String name = mainNode.optString("itemName");
		double group = mainNode.optDouble("DegreeValue", 1.0);
		//String title = pubItem.optString("pTitle");
		//String authors = pubItem.optString("pAuthors");

		Map<String, Object> nodes = new LinkedHashMap<>();
		nodes.put( "id", new Integer( ids ) );
		nodes.put( "title", name );// + "\n" + name + "\n" + authors);
		nodes.put( "group", group );
		nodes.put( "type", "3" );
		return nodes;
	}
	
	private Map<String, Object> createResearcherNode( Author author ) 
	{
		List<Object> nodeList = new LinkedList<>();
		List<Object> linkList = new LinkedList<>();
		
		Map<String, Object> nodes = new LinkedHashMap<>();
		nodes.put( "id", author.getId() );
		nodes.put( "title", "Researcher" );
		nodes.put( "details", author.getName() );
		nodes.put( "group", 0 );
		nodes.put( "type", 0 );
		nodes.put( "size", 450 );
		nodes.put( "icon", author.getPhotoUrl() );
		nodeList.add( nodes );
		
		Map<String, Object> recomm = new LinkedHashMap<>();
		recomm.put("nodes", nodeList);
		recomm.put("links", linkList);
		recomm.put("count", nodeList.size());
		return recomm;
	}
}

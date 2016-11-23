package de.rwth.i9.palm.recommendation.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.mahout.cf.taste.common.TasteException;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.statistics.plugin.Degree;
import org.hibernate.SessionFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openide.util.Lookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.persistence.AuthorDAO;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.recommendation.service.SNAC2RecommendationFeature;
import de.rwth.i9.palm.recommendation.service.UtilINImp;
import de.rwth.i9.palm.recommendation.service.UtilService;
import de.rwth.i9.palm.topicextraction.service.TopicExtractionService;

@Service
public class SNAC2RecommendationFeatureImpl implements SNAC2RecommendationFeature
{
	private static String authorID;
	private static List<String> coAuthorsGraph;
	private static JSONArray DegreeRecFinalResult = null;

	private ArrayList<String> DegreeNames = new ArrayList<String>();

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private TopicExtractionService topicExtractionService;

	@Autowired
	private SessionFactory sessionFactory;

	/**
	 * 
	 * This methods computes Degree based on Gephi. After that we finds the
	 * similarity of the active researcher with all the other nodes in the
	 * graph. at the end, combination of SNA and Jaccard similarity cause the
	 * selection of most important researchers
	 * 
	 * @param researcherID
	 * @return
	 * @throws JSONException
	 * @throws SQLException
	 * @throws IOException
	 * @throws TasteException
	 */
	private JSONArray computeDegree( String researcherID )
			throws JSONException, SQLException, IOException, TasteException {
		Map<String, Integer> degreeResult = new TreeMap<String, Integer>();
		// Initiate a project - and therefore a workspace
		ProjectController pc = Lookup.getDefault().lookup(
				ProjectController.class);
		pc.newProject();
		Workspace workspace = pc.getCurrentWorkspace();
		// Get graph model and attribute model of current workspace
		GraphModel graphModel = workspace.getLookup().getDefault()
				.lookup(GraphController.class).getGraphModel();
		/*AttributeModel attributeModel = Lookup.getDefault()
				.lookup(AttributeController.class).getModel();
		ImportController importController = Lookup.getDefault().lookup(
				ImportController.class);
		// Import file
		Container container;
		try {
			File file = new File("data/SNA2dfile.csv");
			container = importController.importFile(file);
			container.getLoader().setEdgeDefault(EdgeDefault.UNDIRECTED); // Force
																			// DIRECTED
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		// Append imported data to GraphAPI
		importController.process(container, new DefaultProcessor(), workspace);*/

		UndirectedGraph graph = graphModel.getUndirectedGraph();
		insertGraph(graph, graphModel);

		// get max possible degree of the graph to normalize degree value
		int MaxDegreeofaNode = graph.getNodeCount() - 1;
		// *** Start Degree Computation
		Degree degree = new Degree();
		degree.execute(graphModel);
		degree.execute( graphModel );
		Column deg = graphModel.getNodeTable().getColumn(
				Degree.DEGREE);
		for (Node n : graph.getNodes()) {
			Integer dResult = (Integer) n.getAttribute(
					n.getTable().getColumn(deg.getIndex()));
			degreeResult.put((String)n.getId(), dResult);
		}
		Map<String, Integer> sortedMap = UtilService
				.sortByComparator3(degreeResult);
		// Recommend researchers that he has never worked with them before
		// find co-authors IDs
		UtilINImp util = new UtilINImp( persistenceStrategy, topicExtractionService, sessionFactory );

		List<String> CoAuthIds = util.getCoAuthors( researcherID ); //UtilService.getCoAuthorIDs(researcherID);
		// print sorted Map
		Map<String, Integer> rDegree = new LinkedHashMap<String, Integer>();
		for (Entry<String, Integer> entry : sortedMap.entrySet()) {
			// Do not add users that already worked on a paper with the active
			// researchers
			if (CoAuthIds.contains(entry.getKey())) {
				// do nothing
			} else if (entry.getKey() != researcherID) {
				rDegree.put(entry.getKey(), entry.getValue());
			}
		}
		// Compute importance of all nodes in compare with searched node
		Map<String, Double> SortedBaseImportanceLevel = UtilService
				.ComputeImportance2(rDegree, researcherID, MaxDegreeofaNode);
		DegreeNames.clear();
		ArrayList<String> rIDs3 = new ArrayList<>();
		Map<String, Integer> rIDDegreeFinal = new LinkedHashMap<>();
		Map<String, String> rIDName3 = new LinkedHashMap<>();
		AuthorDAO dao = persistenceStrategy.getAuthorDAO();
		int i = 0;
		for (Entry<String, Double> entry : SortedBaseImportanceLevel
				.entrySet()) {
			if (i < 10) {
				rIDs3.add(entry.getKey());
				rIDDegreeFinal.put(entry.getKey(),
						sortedMap.get(entry.getKey()));
				//add author name
				Author auth = dao.getById( entry.getKey() );
				rIDName3.put( entry.getKey(), auth.getName() );
				DegreeNames.add( entry.getKey() );
			}
			else
			{
				break;
			}
			i++;
		}

		JSONArray DegreeRecResult = util.DegreeJsonCreator(
				rIDDegreeFinal, rIDName3, researcherID, MaxDegreeofaNode);
		return DegreeRecResult;
	}

	private void insertGraph(UndirectedGraph graph, GraphModel model) {
		//File file = new File("F:/palm/palm-core/data/SNA3dfile.csv");
		if ( coAuthorsGraph != null && coAuthorsGraph.size() > 0 )
		{
			String spliter = ",";
			Map<String, Node> nodes = new LinkedHashMap<>();
			//BufferedReader br = new BufferedReader(new FileReader(file));

			for ( String line : coAuthorsGraph )
			{
				String[] ns = line.split(spliter);
				for(int i=0; i<ns.length; i++) 
				{
					Node node = model.factory().newNode(ns[i]);
					if( !nodes.containsKey(ns[i]) && !graph.contains( node ) ) 
					{
						nodes.put(ns[i], node);
						graph.addNode(node);
					}
				}
				graph.addEdge(model.factory().newEdge(nodes.get(ns[0]), nodes.get(ns[1]), false));
			}
		}
		else
		{
			File file = new File("F:/palm/palm-core/data/SNA2dfile.csv");
			try {
				String line = "";
				String spliter = ",";
				Map<String, Node> nodes = new LinkedHashMap<>();
				BufferedReader br = new BufferedReader(new FileReader(file));
				while((line = br.readLine()) != null) 
				{
					String[] ns = line.split(spliter);
					for(int i=0; i<ns.length; i++) 
					{
						Node node = model.factory().newNode(ns[i]);
						if( !nodes.containsKey(ns[i]) && !graph.contains( node ) ) 
						{
							nodes.put(ns[i], node);
							graph.addNode(node);
						}
					}
					graph.addEdge(model.factory().newEdge(nodes.get(ns[0]), nodes.get(ns[1]), false));
				}

				br.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	/**
	 * Recommend new Interests based on top rated Interests of recommended
	 * researchers
	 * 
	 * @param researcherID
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 * @throws SQLException
	 */
	private JSONArray topRatedItemsDegree( String researcherID )
			throws JSONException, IOException, SQLException 
	{
		/*String namesforQuery = UtilService.namesListForQuery(DegreeNames);
		// Find interests of the user
		ArrayList<String> iListof1User = new ArrayList<String>();
		iListof1User = UtilService.getInterestOf1User(researcherID);
		String idsforQuery = UtilService.idsListForQuery(namesforQuery);
		JSONArray topRatedItems = new JSONArray();
		Map<Integer, String> palmResearchers = ResearcherService
				.allRMapCreator();
		Map<String, Integer> interestIDTableMap = UtilService
				.interestIDTableMap();
		// 1.get the connection to db
		Connection myConn;
		try {
			myConn = DbService.ConnectToDB();
			// 2.create statement
			Statement myStmt = myConn.createStatement();
			ResultSet myRs = myStmt
					.executeQuery("select DISTINCT authorid,result,score from results WHERE authorid IN ("
							+ idsforQuery + ")ORDER BY score DESC;");
			// check not to recommend one Item 2 times.
			ArrayList<String> checkedList = new ArrayList<String>();
			int i = 0;
			while (myRs.next()) {
				if (iListof1User.contains(myRs.getString("result"))
						|| checkedList.contains(myRs.getString("result"))) {
				} else if (i < 10) {
					checkedList.add(myRs.getString("result"));
					JSONObject obj = new JSONObject();
					obj.put("AuthorID", myRs.getInt("authorid"));
					obj.put("AuthorName", ResearcherService.findOneResearcher(
							palmResearchers, myRs.getInt("authorid")));
					obj.put("itemName", myRs.getString("result"));
					obj.put("InterestID",
							UtilService.get1InterestID(interestIDTableMap,
									myRs.getString("result")));
					obj.put("Score", myRs.getString("score"));
					topRatedItems.put(obj);
					i++;
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return topRatedItems;*/
		//String namesforQuery = UtilService.namesListForQuery( BetweennessNames );
		//String idsforQuery = UtilService.idsListForQuery(namesforQuery);
		// Find interests of the user
		UtilINImp util = new UtilINImp( persistenceStrategy, topicExtractionService, sessionFactory );
		List<Object> iListOf1User = util.authorSortedInterest( researcherID );
		//ArrayList<String> iListof1User = new ArrayList<String>();
		//iListof1User = UtilService.getInterestOf1User(researcherID);
		//Map<Integer, String> palmResearchers = ResearcherService.allRMapCreator();
		//Map<String, Integer> interestIDTableMap = UtilService.interestIDTableMap();

		// inserting all authors interests.
		Map<String, JSONObject> interestItems = new TreeMap<>();
		Map<String, Double> itemList = new LinkedHashMap<>();

		AuthorDAO dao = persistenceStrategy.getAuthorDAO();
		for ( String authors : DegreeNames )
		{
			int i = 0;
			List<Object> authorInterest = util.authorSortedInterest( authors );

				for ( Object ai : authorInterest ) 
				{
					ArrayList<Object> items = ( ArrayList<Object> ) ai;

					if ( !iListOf1User.contains( items ) )
					{
						if ( !itemList.containsKey( items.get( 0 ) ) )
						{
							if ( i < 10 ) 
							{
								JSONObject obj = new JSONObject();
								obj.put( "AuthorID", authors );
								obj.put( "AuthorName", dao.getById( authors ).getName() );
								obj.put( "itemName", items.get( 1 ) );
								obj.put( "InterestID", items.get( 0 ) );
								obj.put( "DegreeValue", items.get( 2 ) );
								interestItems.put( String.valueOf( items.get( 0 ) ), obj );
								itemList.put( String.valueOf( items.get( 0 ) ), 
										Double.valueOf( String.valueOf( items.get( 2 ) ) ) );
								i++;
							}
							else 
							{
								break;
							}
						}
						else 
						{
							String id = String.valueOf( items.get( 0 ) );
							
							double value = itemList.get( id );
							double degree = Double.valueOf( String.valueOf( items.get( 2 ) ) );
							degree += value;
							
							itemList.replace( id, degree );
							
							JSONObject obj = new JSONObject();
							obj.put( "AuthorID", authors );
							obj.put( "AuthorName", dao.getById( authors ).getName() );
							obj.put( "itemName", items.get( 1 ) );
							obj.put( "InterestID", items.get( 0 ) );
							obj.put( "DegreeValue", degree );
							interestItems.replace( id, obj );
						}
					}
				}
		}
		
		JSONArray topRatedItems = new JSONArray();
		
		Map<String, Double> sortedList = UtilService.sortByIDComparator( itemList );

		int i = 0;
		for ( Map.Entry<String, Double> entry : sortedList.entrySet() )
		{
			if ( i++ < 10 )
			{
				topRatedItems.put( interestItems.get( entry.getKey() ) );
			}
			else 
			{
				break;
			}
		}
		
		return topRatedItems;
	}

	/**
	 * 
	 * Final Step - sending file to control
	 * 
	 * @param selectedRId
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 * @throws TasteException
	 * @throws SQLException
	 */
	@Override
	public JSONArray computeSNAC2Recommendation( Author researcher ) throws JSONException, SQLException, IOException, TasteException
	{
		JSONArray DegreeRecResult = new JSONArray();
		String researcherID = researcher.getId();

		if (  authorID != null && researcherID.equals( authorID ) &&
				DegreeRecFinalResult != null && DegreeRecFinalResult.length() > 0 )
		{
			return DegreeRecFinalResult;
		}

		System.out.println( "Author Id: " + researcherID );
		// Create SNA Graph
		if ( authorID == null || ( authorID != null && !researcherID.equals( authorID ) ) || 
				coAuthorsGraph == null || ( coAuthorsGraph != null && coAuthorsGraph.size() > 0 ) )
		{
			UtilINImp util = new UtilINImp( persistenceStrategy, topicExtractionService, sessionFactory );
			coAuthorsGraph = util.createCoAuthorGraph( researcherID, 2 );
			authorID = new String( researcherID );
			//System.out.println( "coAuthor String: " + coAuthorsGraph.toString() );
		}

		// Recommend collaborators based on Degree and Jaccard Similarity
		JSONArray DegreeSimillarR = computeDegree( researcherID );
		JSONObject DegreeSimillarRObj = new JSONObject();
		DegreeSimillarRObj.put("sResearchers", DegreeSimillarR);
		// Find top rated Items of recommended researchers
		JSONArray topRatedItems = topRatedItemsDegree(researcherID);
		JSONObject DegreeTopItemsObj = new JSONObject();
		DegreeTopItemsObj.put("rItems", topRatedItems);
		DegreeRecResult.put(DegreeSimillarRObj);
		DegreeRecResult.put(DegreeTopItemsObj);
		// start paper recommendation based recommended researchers
		ArrayList<String> rIDs = UtilService
				.FindRIdsInFinalJson(DegreeRecResult);
		ArrayList<String> iNames = UtilService
				.FindINamesInFinalJson(DegreeRecResult);

		UtilINImp util = new UtilINImp( persistenceStrategy, topicExtractionService, sessionFactory );
		JSONArray DegreeFinalResult = new JSONArray(); //util.addPubsOfIToJson(
				//DegreeRecResult, rIDs, iNames, null);
		// Create common interests Json file
		DegreeRecFinalResult = util
				.addCommonInterestListToJson( DegreeFinalResult, rIDs, researcherID );
		return DegreeRecFinalResult;
	}

}

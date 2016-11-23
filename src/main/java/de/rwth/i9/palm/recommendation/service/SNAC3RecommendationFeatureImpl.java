package de.rwth.i9.palm.recommendation.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
import org.gephi.statistics.plugin.GraphDistance;
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
import de.rwth.i9.palm.recommendation.service.SNAC3RecommendationFeature;
import de.rwth.i9.palm.recommendation.service.UtilINImp;
import de.rwth.i9.palm.recommendation.service.UtilService;
import de.rwth.i9.palm.topicextraction.service.TopicExtractionService;

@Service
public class SNAC3RecommendationFeatureImpl implements SNAC3RecommendationFeature
{

	private JSONArray BetweennessRecFinalResult2 = null;
	private String authorID;
	private List<String> coAuthorsGraph;

	private ArrayList<String> BetweennessNames = new ArrayList<String>();

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private TopicExtractionService topicExtractionService;

	@Autowired
	private SessionFactory sessionFactory;

	/**
	 * @param researcherID
	 * @return
	 * @throws JSONException
	 * @throws SQLException
	 * @throws IOException
	 * @throws TasteException
	 */
	private JSONArray computeBetweenness(String researcherID)
			throws JSONException, SQLException, IOException, TasteException {
		Map<String, Double> BetweennessResults = new TreeMap<String, Double>();
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
			File file = new File("data/SNA3dfile.csv");
			container = importController.importFile(file);
			container.getLoader().setEdgeDefault(EdgeDefault.UNDIRECTED); // Force
																			// undirected
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		// Append imported data to GraphAPI
		importController.process(container, new DefaultProcessor(), workspace);*/
		UndirectedGraph graph = graphModel.getUndirectedGraph();
		insertGraph(graph, graphModel);

		// **********start Betweenness
		GraphDistance distance = new GraphDistance();
		distance.setDirected(false);
		distance.setNormalized(true);
		distance.execute(graphModel);
		distance.execute( graphModel );
		Column Betw = graphModel.getNodeTable().getColumn(
				GraphDistance.BETWEENNESS);
		for (Node n : graph.getNodes()) {
			Double betweennessResults = (Double) n.getAttribute(
					n.getTable().getColumn(Betw.getIndex()));
			BetweennessResults.put((String)n.getId(), betweennessResults);
		}
		// converts the Map into a List, sorts the List by Comparator and put
		// the sorted list back to a Map.
		Map<String, Double> sortedMap = UtilService
				.sortByComparator(BetweennessResults);

		// find Co-authors IDs
		UtilINImp util = new UtilINImp( persistenceStrategy, topicExtractionService, sessionFactory );
		List<String> CoAuthIds = util.getCoAuthors( researcherID );

		// print sorted Map
		Map<String, Double> rIDBetweenness = new LinkedHashMap<String, Double>();
		for (Entry<String, Double> entry : sortedMap.entrySet()) {
			if (CoAuthIds.contains(entry.getKey())) {
				// do nothing
			} else if (entry.getKey() != researcherID) {
				rIDBetweenness.put(entry.getKey(), entry.getValue());
			}
		}
		// Compute importance of all nodes in compare with searched node
		Map<String, Double> SortedBaseImportanceLevel = UtilService
				.ComputeImportance(rIDBetweenness, researcherID);
		ArrayList<String> rIDs = new ArrayList<String>();
		Map<String, Double> rIDBetweennessFinal = new LinkedHashMap<String, Double>();
		// create MAP for ID and Name
		AuthorDAO dao = persistenceStrategy.getAuthorDAO();
		Map<String, String> rIDName = new HashMap<>();
		int i = 0;

		// add names to BetweennessNames
		BetweennessNames.clear();
		for (Entry<String, Double> entry : SortedBaseImportanceLevel
				.entrySet()) {
			if (i < 10) 
			{
				rIDs.add(entry.getKey());
				rIDBetweennessFinal.put(entry.getKey(),
						sortedMap.get(entry.getKey()));
				//add author name
				Author auth = dao.getById( entry.getKey() );
				rIDName.put( entry.getKey(), auth.getName() );
				BetweennessNames.add( entry.getKey() );
			}
			else 
			{
				break;
			}
			i++;
		}

		// create Json file for Betweenness results
		JSONArray BetweennessRecResult = UtilService.BetweennessJsonCreator(
				rIDName, rIDBetweennessFinal, researcherID, util );
		return BetweennessRecResult;
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
			File file = new File("F:/palm/palm-core/data/SNA3dfile.csv");
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
	 * 
	 * Recommend new Interests based on top rated Interests of recommended
	 * researchers
	 * 
	 * @param researcherID
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 * @throws SQLException
	 */
	private JSONArray topRatedItemsPR( String researcherID ) throws JSONException,
	IOException, SQLException {
		//String namesforQuery = UtilService.namesListForQuery( BetweennessNames );
		//String idsforQuery = UtilService.idsListForQuery(namesforQuery);
		// Find interests of the user
		UtilINImp util = new UtilINImp( persistenceStrategy, topicExtractionService, sessionFactory );
		List<Object> iListOf1User = util.authorSortedInterest( researcherID );
		//ArrayList<String> iListof1User = new ArrayList<String>();
		//iListof1User = UtilService.getInterestOf1User(researcherID);
		//Map<Integer, String> palmResearchers = ResearcherService.allRMapCreator();
		//Map<String, Integer> interestIDTableMap = UtilService.interestIDTableMap();

		Map<String, JSONObject> interestItems = new TreeMap<>();
		Map<String, Double> itemList = new LinkedHashMap<>();

		// inserting all authors interests.

		//ArrayList<String> checkedList = new ArrayList<String>();
		AuthorDAO dao = persistenceStrategy.getAuthorDAO();
		for ( String authors : BetweennessNames )
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
		
		/*Connection myConn;
		try {
			myConn = DbService.ConnectToDB();
			// 2.create statement
			Statement myStmt = myConn.createStatement();
			ResultSet myRs = myStmt
					.executeQuery("select DISTINCT authorid,result,score from results WHERE authorid IN ("
							+ idsforQuery + ")ORDER BY score DESC;");
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
		}*/
		return topRatedItems;
	}

	/**
	 * Final Step - Send the file to control
	 * 
	 * @param selectedRId
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 * @throws TasteException
	 * @throws SQLException
	 */	
	@Override
	public JSONArray computeSNAC3Recommendation( Author researcher ) 
			throws JSONException, SQLException, IOException, TasteException
	{
		String researcherID = researcher.getId();

		if( authorID != null && researcherID.equals( authorID ) &&
				BetweennessRecFinalResult2 != null && BetweennessRecFinalResult2.length() > 0 )
		{
			return BetweennessRecFinalResult2;
		}

		BetweennessRecFinalResult2 = new JSONArray();

		System.out.println( "Author Id: " + researcherID );
		UtilINImp util = new UtilINImp( persistenceStrategy, topicExtractionService, sessionFactory );

		// Create SNA Graph
		if ( authorID == null || ( authorID != null && !researcherID.equals( authorID ) ) || 
				coAuthorsGraph == null || ( coAuthorsGraph != null && coAuthorsGraph.size() > 0 ) )
		{
			coAuthorsGraph = new LinkedList<>( util.createCoAuthorGraph( researcherID, 3 ) );
			authorID = new String( researcherID );
			//UtilC3d.SNA3dfileCreator(researcherID);
		}

		// Recommend collaborators based on Betweenness and Jaccard Similarity
		JSONArray BetweennessSimillarR = computeBetweenness(researcherID);
		JSONObject BetweennessSimillarRObj = new JSONObject();
		BetweennessSimillarRObj.put("sResearchers", BetweennessSimillarR);
		// Find top rated Items of Recommended users
		JSONArray topRatedItems = topRatedItemsPR(researcherID);
		JSONObject BetweennessTopItemsObj = new JSONObject();
		JSONArray DegreeRecResult = new JSONArray();
		BetweennessTopItemsObj.put("rItems", topRatedItems);
		DegreeRecResult.put(BetweennessSimillarRObj);
		DegreeRecResult.put(BetweennessTopItemsObj);

		ArrayList<String> rIDs = UtilService
				.FindRIdsInFinalJson(DegreeRecResult);
		//System.out.println( "Find researcher ids JSON completed." );
		ArrayList<String> iNames = UtilService
				.FindINamesInFinalJson(DegreeRecResult);
		//System.out.println( "Find researcher name completed." );
		JSONArray DegreeRecFinalResult = new JSONArray(); //= util.addPubsOfIToJson( 
				//DegreeRecResult, rIDs, iNames, null );
		BetweennessRecFinalResult2 = util
				.addCommonInterestListToJson(DegreeRecFinalResult, rIDs, researcherID );
		return BetweennessRecFinalResult2;
	}

}

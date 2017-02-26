package de.rwth.i9.palm.recommendation.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.mahout.cf.taste.common.TasteException;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeIterable;
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
import de.rwth.i9.palm.recommendation.service.GenericRecommendation;
import de.rwth.i9.palm.recommendation.service.SNAC3RecommendationFeature;
import de.rwth.i9.palm.recommendation.service.UtilINImp;
import de.rwth.i9.palm.recommendation.service.UtilService;
import de.rwth.i9.palm.topicextraction.service.TopicExtractionService;

@Service
public class SNAC3RecommendationFeatureImpl extends GenericRecommendation
{

	private JSONArray BetweennessRecFinalResult2 = null;
	private String authorID;
	private List<String> coAuthorsGraph;

	//private ArrayList<String> BetweennessNames = new ArrayList<String>();


	private GenericRecommendation superClass;

	public SNAC3RecommendationFeatureImpl() 
	{
		this( null );
	}

	public SNAC3RecommendationFeatureImpl( GenericRecommendation superClass )
	{
		this.superClass = superClass;
	}

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
		/*ProjectController pc = Lookup.getDefault().lookup(
				ProjectController.class);
		pc.newProject();
		Workspace workspace = pc.getCurrentWorkspace();*/
		// Get graph model and attribute model of current workspace
		GraphModel graphModel = GraphModel.Factory.newInstance();
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
		//distance.execute( graph );
		distance.execute( graphModel );
		Column Betw = graphModel.getNodeTable().getColumn(
				GraphDistance.BETWEENNESS);
		for (Node n : graph.getNodes()) {
			Number bResults = (Number) n.getAttribute(
					n.getTable().getColumn(Betw.getIndex()));
			Double betweennessResults = (Double) bResults;
			BetweennessResults.put((String)n.getId(), betweennessResults);
		}
		// converts the Map into a List, sorts the List by Comparator and put
		// the sorted list back to a Map.
		Map<String, Double> sortedMap = UtilService
				.sortByComparator(BetweennessResults);

		// find Co-authors IDs
		List<String> CoAuthIds = superClass.getUtil().getCoAuthors( researcherID );

		// print sorted Map
		Map<String, Double> rIDBetweenness = new LinkedHashMap<String, Double>();
		for (Entry<String, Double> entry : sortedMap.entrySet()) {
			if (CoAuthIds.contains(entry.getKey())) {
				// do nothing
			} else if (entry.getKey() != researcherID) {
				rIDBetweenness.put(entry.getKey(), entry.getValue());
			}
		}
		System.out.println( "Step 11 : " + System.currentTimeMillis() );
		// Compute importance of all nodes in compare with searched node
		Map<String, Double> SortedBaseImportanceLevel = UtilService
				.ComputeImportance(rIDBetweenness, researcherID, superClass.getUtil());
		ArrayList<String> rIDs = new ArrayList<String>();
		Map<String, Number> rIDBetweennessFinal = new LinkedHashMap<String, Number>();
		// create MAP for ID and Name
		AuthorDAO dao = superClass.getPersistenceStrategy().getAuthorDAO();
		Map<String, String> rIDName = new HashMap<>();
		int i = 0;

		System.out.println( "Step 12 : " + System.currentTimeMillis() );
		// add names to BetweennessNames
		superClass.getNetworkCentrality().clear();
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
				superClass.getNetworkCentrality().put( entry.getKey(), entry.getValue() );
				//BetweennessNames.add( entry.getKey() );
			}
			else 
			{
				break;
			}
			i++;
		}

		// create Json file for Betweenness results
		JSONArray BetweennessRecResult = UtilService.BetweennessJsonCreator(
				rIDName, rIDBetweennessFinal, researcherID, superClass.getUtil() );
		return superClass.getUtil().createTopNAuthorsGraph( BetweennessRecResult );
	}

	private void insertGraph(UndirectedGraph graph, GraphModel model) {
		//File file = new File("F:/palm/palm-core/data/SNA3dfile.csv");
		if ( superClass.getStep2() != null && superClass.getStep2().length() > 0 )
		{
			String spliter = ",";
			Map<String, Node> nodes = new LinkedHashMap<>();
			//BufferedReader br = new BufferedReader(new FileReader(file));

			/*for ( String line : coAuthorsGraph )
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
			}*/

			JSONObject obj = superClass.getStep2().optJSONObject( 0 );
			if ( obj != null )
			{
				JSONArray links = obj.optJSONArray( "links" );
				for ( int i = 0; i < links.length(); i++ )
				{
					JSONObject link = links.optJSONObject( i );
					if ( link != null )
					{
						String source = link.optString( "source" );
						String target = link.optString( "target" );
						Node sNode = null, tNode = null;

						// creating source node
						if ( !nodes.containsKey( source ) )
						{
							sNode = model.factory().newNode( source );
							nodes.put( source, sNode );
							graph.addNode( sNode );
						}
						else
						{
							sNode = nodes.get( source );
						}

						//creating target node
						if ( !nodes.containsKey( target ) )
						{
							tNode = model.factory().newNode( target );
							nodes.put( target, tNode );
							graph.addNode( tNode );
						}
						else
						{
							tNode = nodes.get( target );
						}

						//creating edge in graph
						graph.addEdge( model.factory().newEdge( sNode, tNode, false ) );
					}
				}
			}
		}/*
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
		}*/
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
		//List<Object> iListOf1User = superClass.getUtil().authorSortedInterest( researcherID );
		//ArrayList<String> iListof1User = new ArrayList<String>();
		//iListof1User = UtilService.getInterestOf1User(researcherID);
		//Map<Integer, String> palmResearchers = ResearcherService.allRMapCreator();
		//Map<String, Integer> interestIDTableMap = UtilService.interestIDTableMap();

		Map<String, JSONObject> interestItems = new TreeMap<>();
		Map<String, Double> itemList = new LinkedHashMap<>();

		// inserting all authors interests.

		//ArrayList<String> checkedList = new ArrayList<String>();
		AuthorDAO dao = superClass.getPersistenceStrategy().getAuthorDAO();
		Set<String> coAuthors = superClass.getNetworkCentrality().keySet();
		for ( String authors : coAuthors )
		{
			int i = 0;
			List<Object> authorInterest = superClass.getUtil().authorSortedInterest( authors );

			for ( Object ai : authorInterest ) 
			{
				ArrayList<Object> items = ( ArrayList<Object> ) ai;

				//if ( !iListOf1User.contains( items ) )
				//{
				if ( !itemList.containsKey( items.get( 0 ) ) )
				{
					if ( i < 10 ) 
					{
						double degree = Double.valueOf( String.valueOf( items.get( 2 ) ) ) * 100.0;
						JSONObject obj = new JSONObject();
						obj.put( "AuthorID", authors );
						obj.put( "AuthorName", dao.getById( authors ).getName() );
						obj.put( "itemName", items.get( 1 ) );
						obj.put( "ID", items.get( 0 ) );
						obj.put( "DegreeValue", degree );
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
					obj.put( "ID", items.get( 0 ) );
					obj.put( "DegreeValue", degree );
					interestItems.replace( id, obj );
				}
				//}
			}
		}


		JSONArray topRatedItems = new JSONArray();

		Map<String, Double> sortedList = UtilService.sortByIDComparator( itemList );

		int i = 0;
		for ( Map.Entry<String, Double> entry : sortedList.entrySet() )
		{
			//if ( i++ < 10 )
			//{
			topRatedItems.put( interestItems.get( entry.getKey() ) );
			//}
			//else 
			//{
			//break;
			//}
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
	public JSONArray computeSNAC3Recommendation( Author researcher, int stepNo ) 
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

		// Create SNA Graph
		if ( authorID == null || ( authorID != null && !researcherID.equals( authorID ) ) || 
				coAuthorsGraph == null || ( coAuthorsGraph != null && coAuthorsGraph.size() > 0 ) )
		{
			coAuthorsGraph = new LinkedList<>( superClass.getUtil().createCoAuthorGraph( researcherID, 3 ) );
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
		BetweennessRecFinalResult2 = superClass.getUtil()
				.addCommonInterestListToJson(DegreeRecFinalResult, rIDs, researcherID );
		return BetweennessRecFinalResult2;
	}

	@Override
	protected void recommendationStep1( Author researcher, UtilINImp util )
	{
		String researcherID = researcher.getId();
		superClass.setStep1( util.get1DGraph( researcherID ) );

		JSONArray tempArray = new JSONArray();
		JSONObject tempObj = new JSONObject();
		JSONArray nodes = new JSONArray();

		GraphModel model = superClass.getTreeGraph()[0];
		nodes.put( util.getGraphNode( model.getUndirectedGraphVisible().getNode( researcherID ), model ) );
		tempObj.put( "nodes", nodes );
		tempArray.put( tempObj );

		superClass.updateGraphNode( superClass.getStep1(), tempArray, 1 );
	}

	@Override
	protected void recommendationStep2( Author researcher, UtilINImp util )
	{
		String researcherID = researcher.getId();
		superClass.setStep2( util.getNDCoAuthorsGraph( researcherID, superClass.getStep1(), 3 ) );
		superClass.updateGraphNode( superClass.getStep2(), superClass.getStep1(), 2 );
	}

	@Override
	protected void recommendationStep3( Author researcher, UtilINImp util )
	{
		String researcherID = researcher.getId();

		try
		{
			System.out.println( "Step 1 : " + System.currentTimeMillis() );
			superClass.setStep3( computeBetweenness( researcherID ) );
			superClass.updateGraphNode( superClass.getStep3(), null, 3 );

			System.out.println( "Step 5 : " + System.currentTimeMillis() );
			//inserting network graph coAuthors
			UndirectedGraph currentGraph = superClass.getTreeGraph()[3].getUndirectedGraph();
			UndirectedGraph prevGraph = superClass.getTreeGraph()[2].getUndirectedGraph();

			currentGraph.writeLock();
			for ( Node node : currentGraph.getNodes() )
			{
				//get neighbors
				NodeIterable neighbors = prevGraph.getNeighbors( prevGraph.getNode( node.getId() ) );
				for ( Node neighbor : neighbors )
				{
					//if node doesn't exist in the graph
					if ( Integer.valueOf( String.valueOf( neighbor.getAttribute( "type" ) ) ) == 1 )
					{
						if ( currentGraph.getNode( neighbor.getId() ) == null )
						{
							Node newNode = superClass.getTreeGraph()[3].factory().newNode( neighbor.getId() );
							newNode.setLabel( neighbor.getLabel() );
							newNode.setSize( 150 );
							newNode.setX( neighbor.x() );
							newNode.setY( neighbor.y() );
							String[] allColumns = { 
									new String( "stepNo" ),
									new String( "group" ),
									new String( "title" ),
									new String( "type" ),
									new String( "size" ) };
							for ( String col : allColumns )
								newNode.setAttribute( col, neighbor.getAttribute( col ) );
							currentGraph.addNode( newNode );
						}
						if ( currentGraph.getNode( neighbor.getId() ) != null && 
								currentGraph.getEdge( node, currentGraph.getNode( neighbor.getId() ) ) == null )
							currentGraph.addEdge( superClass.getTreeGraph()[3].factory().newEdge( node, currentGraph.getNode( neighbor.getId() ), false ) );
					}
				}
			}
			currentGraph.writeUnlock();
			System.out.println( "Step 6 : " + System.currentTimeMillis() );
		}
		catch (JSONException | SQLException | IOException | TasteException e)
		{
		}
	}

	@Override
	protected void recommendationStep4( Author researcher, UtilINImp util )
	{
		String researcherID = researcher.getId(); 

		try
		{
			superClass.setStep4( topRatedItemsPR( researcherID ) );
			BetweennessRecFinalResult2 = util.createTopInterestGraph( superClass.getStep4() );
			superClass.updateGraphNode( BetweennessRecFinalResult2, superClass.getStep3(), 4 );
		}
		catch (JSONException | IOException | SQLException e)
		{
		}
	}

	@Override
	protected void recommendationStep5( Author researcher, UtilINImp util )
	{
		JSONArray DegreeRecResult = new JSONArray();
		ArrayList<String> researcherIDs = new ArrayList<>();

		JSONObject authors = superClass.getStep3().optJSONObject( 0 );
		if ( authors != null )
		{
			JSONArray aList = authors.optJSONArray( "nodes" );
			for ( int i = 0; i < aList.length(); i++ )
			{
				JSONObject author = aList.optJSONObject( i );
				if ( author != null )
				{
					String id = author.optString( "id" );
					if ( id != null && !researcherIDs.contains( id ) )
					{
						researcherIDs.add( id );
					}
				}
			}
		}
		
		Map<String, Number> iValues = new HashMap<>();
		JSONArray interests = superClass.getStep4();
		for ( int i = 0; i < interests.length(); i++ )
		{
			JSONObject obj = interests.optJSONObject( i );
			if ( obj != null )
			{
				String id = obj.optString( "itemName" );
				int value = Double.valueOf( obj.optDouble( "DegreeValue" ) ).intValue();
				iValues.put( id, value );
			}
		}

		System.out.println( "Pub 3 : " + System.currentTimeMillis() );
		superClass.setStep5( util.addPubsOfIToJson( 
				DegreeRecResult, researcherIDs, iValues, superClass.getPubService() ) );
		System.out.println( "Pub 4 : " + System.currentTimeMillis() );
		JSONArray publicationGraph = util.createCoAuthorPublicationGraph( superClass.getStep5(), superClass.getStep3(), superClass.getStep4(), BetweennessRecFinalResult2 );
		System.out.println( "Pub 5 : " + System.currentTimeMillis() );
		superClass.updateGraphNode( publicationGraph, BetweennessRecFinalResult2, 5 );
	}

	@Override
	protected void recommendationStep6( Author researcher, UtilINImp util )
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected JSONArray computeSingleTree( Author researcher, int stepNo, String id )
	{
		JSONObject nodesJSON = new JSONObject();
		ArrayList<String> coAuthorsList = new ArrayList<>();
		Map<Integer, List<String>> uniqueIds = new LinkedHashMap<>();
		Map<Integer, List<String>> graphIds = new LinkedHashMap<>();
		boolean shouldCheckBothSides = false;
		if ( stepNo != 5 && stepNo != 0 )
			shouldCheckBothSides = true;

		for ( int i = 0; i < 6; i++ )
		{
			uniqueIds.put( i, new LinkedList<>() );
			graphIds.put( i, new LinkedList<>() );
		}

		graphIds.get( stepNo ).add( id );

		//List<String> firstIds = getStepNodes( stepNo, researcher.getId(), nodesJSON, uniqueIds, 0, true, false, graphIds );
		//List<String> nextUniqueIds = firstIds;
		createSingleTreeStep( stepNo, -1, graphIds, uniqueIds, nodesJSON );

		int step = stepNo;
		int increment = 1;

		ArrayList<Integer> coAuthors = new ArrayList<>( Arrays.asList( 4, 1 ) );
		int coAuthorSteps = 1;
		for ( int i = 0; i < superClass.getTreeGraph().length; i++ )
		{
			step += increment;
			//List<String> tempIds = getStepNodes( step, researcher.getId(), nodesJSON, uniqueIds, increment, true, false, graphIds );
			boolean tempIds = createSingleTreeStep( step, ( coAuthors.isEmpty() ? coAuthorSteps : coAuthors.get( 0 ) ), graphIds, uniqueIds, nodesJSON );
			if ( !tempIds )
			{
				increment = -1;
				step = stepNo;
				//nextUniqueIds = firstIds;
			}
			else
			{
				//graphIds.get( step ).addAll( tempIds );
				//nextUniqueIds = tempIds;
			}
			if ( step == 3 )
			{
				List<String> ids = graphIds.get( 0 );
				ids.remove( researcher.getId() );
				coAuthorsList.addAll( ids );
				/*if ( coAuthorSteps > 1 )
				{
					graphIds.put( 1, ids );
					coAuthorSteps--; step++; i--; 
					graphIds.put( 0, new LinkedList<>() );
				}*/
				if ( !coAuthors.isEmpty() )
				{
					graphIds.put( 2, ids );
					coAuthors.remove( 0 ); step++; i--;
					graphIds.put( 0, new LinkedList<>() );
				}
				else 
				{
					graphIds.put( 0, new LinkedList<>( Arrays.asList( researcher.getId() ) ) );
				}
			}
		}

		//inserting researcher id and its links
		UndirectedGraph graph = superClass.getTreeGraph()[ 1 ].getUndirectedGraph();
		JSONArray authorNodes = nodesJSON.optJSONArray( "nodes" );
		JSONArray authorLinks = nodesJSON.optJSONArray( "links" );
		Node node = graph.getNode( researcher.getId() );
		UtilINImp util = new UtilINImp( );

		for ( int i = 0; i < authorLinks.length(); i++ )
		{
			JSONObject link = authorLinks.optJSONObject( i );
			if ( link != null )
			{
				boolean changed = false;
				if ( link.optString( "source" ).equals( researcher.getId() + ":1" ) )
				{
					link.put( "source", researcher.getId() + ":0" );
					changed = true;
				}
				if ( link.optString( "target" ).equals( researcher.getId() + ":1" ) )
				{
					link.put( "target", researcher.getId() + ":0" );
					changed = true;
				}
				if ( changed )
				{
					authorLinks.put( i, link );
				}
			}
		}
		/*for ( String aId : coAuthorsList )
		{
			Node coNode = graph.getNode( aId );
			if ( coNode != null )
			{
				if ( graph.isAdjacent( node, coNode ) )
				{
					JSONObject link = new JSONObject();
					link.put( "source", node.getId() + ":" + 0 );
					link.put( "target", coNode.getId() + ":" + 1 );
					if ( link != null )
					{
						authorLinks.put( link );
					}
				}
			}
		}
		authorNodes.put( util.getGraphNode( treeGraph[ 0 ].getUndirectedGraph().getNode( researcher.getId() ), treeGraph[ 0 ] ) );*/
		nodesJSON.put( "nodes", authorNodes );
		nodesJSON.put( "links", authorLinks );
		
		nodesJSON.put( "count", 1 );
		JSONArray array = new JSONArray();
		array.put( nodesJSON );
		return array;
	}

	private boolean createSingleTreeStep( int currentStep, int coAuthorSteps, Map<Integer, List<String>> stepIds, Map<Integer, List<String>> uniqueIds, JSONObject json )
	{
		if ( superClass.getTreeGraph() == null || superClass.getTreeGraph().length <= 0 || currentStep >= superClass.getTreeGraph().length || currentStep < 0 )
			return false;

		List<String> nodesIds = stepIds.get( currentStep );

		//initializing graph json
		JSONArray authorNodes = new JSONArray();
		JSONArray authorLinks = new JSONArray();
		if ( json == null )
			json = new JSONObject();
		else
		{
			if ( json.has( "nodes" ) )
				authorNodes = json.optJSONArray( "nodes" );
			if ( json.has( "links" ) )
				authorLinks = json.optJSONArray( "links" );
		}

		while ( nodesIds != null && !nodesIds.isEmpty() )
		{
			String nodesId = nodesIds.remove( 0 );
			
			Node node = superClass.getTreeGraph()[ currentStep ].getUndirectedGraph().getNode( nodesId );

			if ( node != null )
			{
				//inserting node JSON
				JSONObject obj = superClass.getUtil().getGraphNode( node, superClass.getTreeGraph()[ currentStep ] );
				if ( obj != null && !uniqueIds.get( currentStep ).contains( nodesId ) /*&& !node.getId().equals( researcher )*/ )
				{
					if ( obj.opt( "id" ).equals( superClass.getCurrentAuthor() ) ) {
						obj.put( "id", obj.opt( "id" ) + ":0" );
						obj.put( "stepNo", 0 );
					}
					else
						obj.put( "id", obj.opt( "id" ) + ":" + currentStep );
					String details = obj.optString( "details" );
					obj.put( "details", details );
					authorNodes.put( obj );
					List<String> list = uniqueIds.get( currentStep );
					list.add( nodesId );
				}
				
				//finding neighbors on both sides of steps and inserting them
				int neighborAdded = 0;
				int neighborStep = currentStep + 1;
				int insertId = currentStep + 1;
				boolean checkStep = neighborStep < superClass.getTreeGraph().length && stepIds.get( neighborStep ) != null;
				for ( int i = 0; i < 2; i++ )
				{
					if ( checkStep )
					{
						Node currentNode = superClass.getTreeGraph()[neighborStep].getUndirectedGraph().getNode( node.getId() );
						if ( currentNode != null )
						{
							NodeIterable nodeNeighbor = superClass.getTreeGraph()[neighborStep].getUndirectedGraph().getNeighbors( currentNode );
							Iterator<Node> iterator = nodeNeighbor.iterator();
							while ( iterator.hasNext() )
							{
								Node neighbor = iterator.next();
								boolean coAuthorsCheck = true;
								if ( Integer.valueOf( String.valueOf( neighbor.getAttribute( "type" ) ) ) == UtilINImp._AUTHOR_NODE_CODE ) {
									if ( currentStep == 3 )
									{
										coAuthorsCheck = Integer.valueOf( String.valueOf( neighbor.getAttribute( "group" ) ) ) > 0 &
																	neighborAdded < 1;
										//coAuthorsCheck = Integer.valueOf( String.valueOf( neighbor.getAttribute( "group" ) ) ) <= coAuthorSteps &
										//					neighborAdded < 1 & Integer.valueOf( String.valueOf( neighbor.getAttribute( "group" ) ) ) > 0;
										insertId = 2;
									} 
									else if ( currentStep == 2 ) 
									{
										coAuthorsCheck = Integer.valueOf( String.valueOf( neighbor.getAttribute( "group" ) ) ) == 0;
									}
								}
								
								if ( !uniqueIds.get( neighborStep ).contains( neighbor.getId() ) && ( coAuthorsCheck ) )
								{
									neighborAdded++;
									JSONObject link = new JSONObject();
									link.put( "source", node.getId() + ":" + currentStep );
									link.put( "target", neighbor.getId() + ":" + insertId );
									if ( link != null )
									{
										authorLinks.put( link );
									}
									/*if ( neighborStep == 5 && Integer.valueOf( String.valueOf( neighbor.getAttribute( "type" ) ) ) != UtilINImp._PUBLICATION_NODE_CODE )
									{
										otherIds.add( String.valueOf( neighbor.getId() ) );
									}
									else*/ if ( Integer.valueOf( String.valueOf( neighbor.getAttribute( "type" ) ) ) == UtilINImp._PUBLICATION_NODE_CODE && 
											stepIds.get( 5 ) != null )
										stepIds.get( 5 ).add( String.valueOf( neighbor.getId() ) );
									/*else if ( Integer.valueOf( String.valueOf( neighbor.getAttribute( "type" ) ) ) == UtilINImp._INTERST_NODE_CODE )
									{	
										if ( ( ( Integer ) neighbor.getAttribute( "size" ) ) != 150 && stepIds.get( 3 ) != null || neighborStep == 3 > 2 && stepIds.get( 3 ) != null )
											stepIds.get( 3 ).add( String.valueOf( neighbor.getId() ) );
										else if ( stepIds.get( 2 ) != null )
											stepIds.get( 2 ).add( String.valueOf( neighbor.getId() ) );
											stepIds.get( 2 ) != null )
											stepIds.get( 2 ).add( String.valueOf( neighbor.getId() ) );
									}
									else if ( Integer.valueOf( String.valueOf( neighbor.getAttribute( "type" ) ) ) == UtilINImp._AUTHOR_NODE_CODE )
									{	
										if ( ( ( Integer ) neighbor.getAttribute( "size" ) ) != 150 && stepIds.get( 4 ) != null )
											stepIds.get( 4 ).add( String.valueOf( neighbor.getId() ) );
										else if ( stepIds.get( 1 ) != null )
											stepIds.get( 1 ).add( String.valueOf( neighbor.getId() ) );
										if ( neighborStep == 1 || neighborStep == 4 > 1 && stepIds.get( 4 ) != null )
											stepIds.get( neighborStep ).add( String.valueOf( neighbor.getId() ) );
										else if ( stepIds.get( 1 ) != null )
											stepIds.get( 1 ).add( String.valueOf( neighbor.getId() ) );
									}*/
									else if ( insertId == 1 && Integer.valueOf( String.valueOf( neighbor.getAttribute( "type" ) ) ) == UtilINImp._AUTHOR_NODE_CODE && 
											stepIds.get( 1 ) != null )
										stepIds.get( 1 ).add( String.valueOf( neighbor.getId() ) );
									else if ( insertId == 2 && Integer.valueOf( String.valueOf( neighbor.getAttribute( "type" ) ) ) == UtilINImp._AUTHOR_NODE_CODE && 
											stepIds.get( 2 ) != null )
										stepIds.get( 2 ).add( String.valueOf( neighbor.getId() ) );
									else if ( insertId == 4 && Integer.valueOf( String.valueOf( neighbor.getAttribute( "type" ) ) ) == UtilINImp._INTERST_NODE_CODE && 
											stepIds.get( 4 ) != null )
										stepIds.get( 4 ).add( String.valueOf( neighbor.getId() ) );
									else if ( insertId == 3 && Integer.valueOf( String.valueOf( neighbor.getAttribute( "type" ) ) ) == UtilINImp._AUTHOR_NODE_CODE && 
										stepIds.get( 3 ) != null )
										stepIds.get( 3 ).add( String.valueOf( neighbor.getId() ) );
									else if ( insertId == 0 && 
											( Integer.valueOf( String.valueOf( neighbor.getAttribute( "type" ) ) ) == UtilINImp._RESEARCHER_NODE_CODE ||
											Integer.valueOf( String.valueOf( neighbor.getAttribute( "type" ) ) ) == UtilINImp._AUTHOR_NODE_CODE
											) && 
											stepIds.get( 0 ) != null )
										stepIds.get( 0 ).add( String.valueOf( neighbor.getId() ) );
								}
							}
						}
					}
					/*if ( neighborStep == 5 && !otherIds.isEmpty() )
					{
						node = treeGraph[neighborStep].getUndirectedGraph().getNode( otherIds.get( 0 ) );
					}
					else 
					{*/
						neighborStep = currentStep;
						insertId = currentStep - 1;
						checkStep = ( insertId >= 0  && stepIds.get( insertId ) != null );
					//}
				}
			}
		}

		json.put( "nodes", authorNodes );
		json.put( "links", authorLinks );

		if ( currentStep != 1 )
		stepIds.put( currentStep, null );
		return true;
	}
}

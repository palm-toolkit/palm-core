package de.rwth.i9.palm.recommendation.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.mahout.cf.taste.common.TasteException;
import org.gephi.datalab.api.AttributeColumnsController;
import org.gephi.datalab.impl.AttributeColumnsControllerImpl;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeIterable;
import org.gephi.graph.api.Table;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.layout.plugin.openord.OpenOrdLayout;
import org.gephi.layout.plugin.openord.OpenOrdLayoutBuilder;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.statistics.plugin.Degree;
import org.gephi.utils.progress.ProgressTicket;
import org.hibernate.SessionFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openide.util.Lookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.persistence.AuthorDAO;
import de.rwth.i9.palm.persistence.InterestDAO;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.recommendation.service.GenericRecommendation;
import de.rwth.i9.palm.recommendation.service.SNAINRecommendationFeature;
import de.rwth.i9.palm.recommendation.service.UtilIN;
import de.rwth.i9.palm.recommendation.service.UtilINImp;
import de.rwth.i9.palm.recommendation.service.UtilService;
import de.rwth.i9.palm.topicextraction.service.TopicExtractionService;

@Service
public class SNAINRecommendationFeatureImpl extends GenericRecommendation {

	//store end results: step 3
	private JSONArray degreeTopNInterestResult = null;

		//store list of authors: step 5
	private JSONArray topCoAuthorsGraph = null;

	private GenericRecommendation superClass;
	
	public SNAINRecommendationFeatureImpl()
	{
		this( null );
	}
	
	public SNAINRecommendationFeatureImpl( GenericRecommendation superClass )
	{
		this.superClass = superClass;
	}
	
	/**
	 * Compute Betweenness on interest network
	 * 
	 * @param researcherID
	 * @return
	 * @throws JSONException
	 * @throws SQLException
	 * @throws ExecutionException 
	 * @throws URISyntaxException 
	 * @throws InterruptedException 
	 * @throws UnsupportedEncodingException 
	 */
	private JSONArray computeBetweenness(String researcherID)
			throws JSONException, SQLException, UnsupportedEncodingException, InterruptedException, URISyntaxException, ExecutionException {
		Map<String, Integer> DegreeResults = new TreeMap<String, Integer>();
		// Initiate a project - and therefore a workspace
		/*ProjectController pc = Lookup.getDefault().lookup(
				ProjectController.class);
		pc.newProject();
		Workspace workspace = pc.getCurrentWorkspace();*/

		// Get graph model and attribute model of current workspace
		/*GraphModel gm = workspace.getLookup().getDefault()
				.lookup(GraphController.class).getGraphModel();*/
		GraphModel gm = GraphModel.Factory.newInstance();
		//AttributeModel attributeModel = Lookup.getDefault()
		//		.lookup(AttributeController.class).getModel();
		/*ImportController importController = Lookup.getDefault().lookup(
				ImportController.class);
		// Import file
		Container container;
		try {
			File file = new File("F:/palm/palm-core/data/InterestSNfile.csv");
			if(file == null || !file.exists()) {
				System.out.println("The file does not exits.");
			}
			container = importController.importFile(file);
			container.getLoader().setEdgeDefault(EdgeDirectionDefault.UNDIRECTED); // Force
																			// UNDIRECTED
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}

		// Append imported data to GraphAPI
		importController.process(container, new DefaultProcessor(), workspace);*/
		UndirectedGraph graph = gm.getUndirectedGraph();

		insertGraph(graph, gm);

		// *********start Betweenness
		//GraphDistance distance = new GraphDistance();
		//distance.setDirected(false);
		//distance.execute(graph);
		//distance.execute(graphModel);

		Degree degree = new Degree();
		degree.execute( graph );
		degree.execute( gm );
		Column Degw = gm.getNodeTable().getColumn( Degree.DEGREE );
		for (Node n : graph.getNodes()) {
			Integer degreeResults = (Integer) n.getAttribute(
					n.getTable().getColumn(Degw.getIndex()));
			//int foo = Integer.parseInt((String)n.getId());
			DegreeResults.put((String)n.getId(), degreeResults);
		}
		Map<String, Integer> sortedMap = UtilService
				.sortByComparatorInt(DegreeResults);
		//ArrayList<String> rIDs = new ArrayList<>();
		Map<String, Integer> rIDDegree = new LinkedHashMap<String, Integer>();


		// get Interest of the researcher
		//Map<String, Integer> interestIDTableMap = UtilService.interestIDTableMap();
		ArrayList<String> iIDsof1R = new ArrayList<>();
		LinkedList<Object> interests = new LinkedList<>();
		Map<String, String> itemNames = new LinkedHashMap<>();

		Map<String, Object> result = superClass.getUtil().getAuthorInterestById( researcherID, false );
		if ( result.get( "status" ).equals( "Ok" ) ) {
			interests = ( LinkedList<Object> ) result.get( "interest" );

			for ( Object object : interests )
			{
				ArrayList<Object> items = ( ArrayList<Object> ) object;
				if ( items.size() > 2 )
				{
					String itemName = (String) items.get( 1 );
					String id = (String) items.get( 0 );
					iIDsof1R.add( id );
					//itemNames.put( id, itemName );
				}
			}
		}

		/*ArrayList<String> iNamesof1R = UtilService
				.getInterestOf1User(researcherID);
		for (int k = 0; k < iNamesof1R.size(); k++) {
			iIDsof1R.add(UtilService.get1InterestID(interestIDTableMap,
					iNamesof1R.get(k)));
		}*/
		superClass.getNetworkCentrality().clear();
		InterestDAO dao = superClass.getPersistenceStrategy().getInterestDAO();
		int i = 0;
		for (Entry<String, Integer> entry : sortedMap.entrySet()) {
			// DO not add users interests that already worked on a paper with the active
			// researcher
			if (iIDsof1R.contains(entry.getKey())) {
				// do nothing
				continue;
			} 

			String term = dao.getById( entry.getKey() ).getTerm();
			if ( i < 10 ) {
				itemNames.put( entry.getKey(), term );
				//rIDs.add(entry.getKey());
				rIDDegree.put(entry.getKey(), entry.getValue());
				superClass.getNetworkCentrality().put( term, entry.getValue() );
				i++;
			}
		}

		// create Json file for Betweenness results
		JSONArray DegreeRecResult = UtilINImp.DegreeJsonCreator( itemNames, rIDDegree );
		return DegreeRecResult;
	}

	private void insertGraph( UndirectedGraph graph, GraphModel model ) {
		//File file = new File("F:/palm/palm-core/data/InterestSNfile.csv");
		//try {
		//String line = "";
		String spliter = ",";
		Map<String, Node> nodes = new LinkedHashMap<>();
		//BufferedReader br = new BufferedReader(new FileReader(file));
		//while((line = br.readLine()) != null) 
		for ( int intID = 0; intID < superClass.getStep2().length(); intID++ )
		{
			JSONObject authorInterests = superClass.getStep2().optJSONObject( intID );
			JSONArray interests = authorInterests.optJSONArray( "interests" );

			for ( int interest = 0; interest < interests.length(); interest++ )
			{
				String line = interests.optString( interest );
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

		System.out.println( "Total interest graph nodes: " + nodes.size() );

		/*br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

	/**
	 * - Find indirect co-authors of 3depth degree co-auth network and put them
	 * in a list - Find the person who has highest rate of Item number 1 -
	 * Recommend that person and remove him/her from 3 depth co-auth network
	 * list - Do this for all 5 items
	 * 
	 * @param researcherID
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 * @throws SQLException
	 */
	private JSONArray rRecommenderOfDegree( String researcherID )
			throws JSONException, IOException, SQLException {
		JSONArray recR = new JSONArray();
		ArrayList<String> allCoIds = superClass.getUtil().getCoAuthors( researcherID, superClass.getTreeGraph()[1], true );
		ArrayList<String> removedIds = new ArrayList<>();
		//System.out.println( "BetweennessNames size: " + BetweennessNames.size() );
		//allCoIds = UtilC3d.find3dIndirectRIds(researcherID);
		List<String> interestItems = new LinkedList<>( superClass.getNetworkCentrality().keySet() );
		for (int i = 0; i < interestItems.size() && i < 10; i++) {
			String rID = superClass.getUtil().topRofIFinderString( allCoIds, interestItems.get(i) );

			if( rID == null || rID.isEmpty() || rID.equals( researcherID ) )
			{
				//rID = util.topRofIFinderString( removedIds, DegreeNames.get(i) );
				if ( rID == null || rID.isEmpty() )
				{
					continue;
				}
			}

			String rName = superClass.getPersistenceStrategy().getAuthorDAO().getById( rID ).getName();
			int NofcommonI = superClass.getUtil().getCommonInterests(researcherID, rID);
			double JaccardSim = superClass.getUtil().FindJaccardSimilarity(researcherID, rID);
			JSONObject obj = new JSONObject();
			obj.put("rID", rID);
			obj.put("rName", rName);
			obj.put("ExpertIn", superClass.getNetworkCentrality().get(i));
			obj.put("NofCommonInterest", NofcommonI);
			obj.put("SimInPercent", ( ( ( Integer ) superClass.getNetworkCentrality().get( interestItems.get( i ) ) ) + NofcommonI ) ); //JaccardSim * 100 );
			recR.put(obj);
			for ( int j = 0; j < allCoIds.size(); j++ ) 
			{
				if ( allCoIds.get( j ).equals( rID ) ) 
				{
					removedIds.add( allCoIds.remove( j ) );
				}
			}
		}
		return recR;
	}

	/**
	 * Main step, send JSON file to controller
	 * 
	 * @param selectedRId
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 * @throws TasteException
	 * @throws SQLException
	 */
	public JSONArray computeSNAINRecommendation( Author researcher, int stepNo ) 
			throws JSONException, SQLException, IOException, TasteException {

		/*if ( researcher == null || researcher == null )
		{
			return new JSONArray();
		}

		String researcherID = researcher.getId();

		UtilINImp util = new UtilINImp( persistenceStrategy, topicExtractionService, sessionFactory );
		JSONArray recItems = null;

		//util.resetCoAuthors();
		//ArrayList<String> rIDs = new ArrayList<>( util.get3DCoAuthors( researcherID ) );
		//System.out.println( "3DCoAuthors size: " + rIDs.size() );
		if ( stepNo == 1 )
		{
			if ( currentAuthor == null || !currentAuthor.equals( researcherID ) || 
					step1 == null || step1.length() < 1 )
			{
				currentAuthor = researcherID;
				step1 = util.get3DCoAuthorsGraph( researcherID );

				//creating temprary step0 graph
				insertResearcherNode( researcher );
				JSONArray tempArray = new JSONArray();
				JSONObject tempObj = new JSONObject();
				JSONArray nodes = new JSONArray();
				nodes.put( util.getGraphNode( treeGraph[0].getUndirectedGraphVisible().getNode( researcher.getId() ), treeGraph[0] ) );
				tempObj.put( "nodes", nodes );
				tempArray.put( tempObj );

				updateGraphNode( step1, tempArray, stepNo );
			}
			//return coAuthors;
			return util.getStepGraph( treeGraph[stepNo], stepNo );
		}
		else if ( stepNo == 2 )
		{
			if ( currentAuthor == null || !currentAuthor.equals( researcherID ) || 
					step2 == null || step2.length() < 1 )
			{
				currentAuthor = new String( researcherID );
				step2 = util.interestSNFileCreator( researcherID, treeGraph[1] );
				System.out.println( "SNFileCreator size: " + step2.length() );
				JSONArray coAuthorInterestResult = util.createInterestGraph( step2 );
				updateGraphNode( coAuthorInterestResult, step1, stepNo );
			}

			//return coAuthorInterestResult;
			return util.getStepGraph( treeGraph[stepNo], stepNo );
		}
		else if ( stepNo == 3 )
		{
			if ( currentAuthor == null || !currentAuthor.equals( researcherID ) || 
					step3 == null || step3.length() < 1 )
			{
				try
				{
					step3 = computeBetweenness(researcherID);
					degreeTopNInterestResult = util.createDegreeGraph( step3 );
					updateGraphNode( degreeTopNInterestResult, null, stepNo );

					//inserting coAuthors
					UndirectedGraph currentGraph = treeGraph[stepNo].getUndirectedGraph();
					UndirectedGraph prevGraph = treeGraph[stepNo-1].getUndirectedGraph();

					currentGraph.writeLock();
					for ( Node node : currentGraph.getNodes() )
					{
						//get neighbors
						NodeIterable neighbors = prevGraph.getNeighbors( prevGraph.getNode( node.getId() ) );
						for ( Node neighbor : neighbors )
						{
							//if node doesn't exist in the graph
							if ( Integer.valueOf( String.valueOf( neighbor.getAttribute( "type" ) ) ) == 2 )
							{
								if ( currentGraph.getNode( neighbor.getId() ) == null )
								{
									Node newNode = treeGraph[stepNo].factory().newNode( neighbor.getId() );
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
									currentGraph.addEdge( treeGraph[stepNo].factory().newEdge( node, currentGraph.getNode( neighbor.getId() ), false ) );
							}
						}
					}
					Column typeCol = treeGraph[stepNo-1].getNodeTable().getColumn( "type" );
					if ( typeCol == null )
						typeCol = treeGraph[stepNo-1].getNodeTable().addColumn( "type", Integer.class );
					currentGraph.writeLock();
					for ( Node node : currentGraph.getNodes() )
					{
						Node preNode = prevGraph.getNode( node.getId() );
						for ( Node neighbor : prevGraph.getNeighbors( preNode ) )
						{
							Node test = currentGraph.getNode( neighbor.getId() );
							Node newNode = null;
							if ( test == null && Integer.valueOf( String.valueOf( neighbor.getAttribute( typeCol ) ) ) == 1 )
							{
								newNode = treeGraph[stepNo].factory().newNode( neighbor.getId() );
								newNode.setLabel( neighbor.getLabel() );
								newNode.setSize( 150 );
								newNode.setX( neighbor.x() );
								newNode.setY( neighbor.y() );
								Table table = treeGraph[stepNo].getNodeTable();
								Column[] allColumns = { 
										( table.getColumn( "stepNo" ) == null ? treeGraph[stepNo].getNodeTable().addColumn( "stepNo", Integer.class ) : table.getColumn( "stepNo" ) ),
										( table.getColumn( "group" ) == null ? treeGraph[stepNo].getNodeTable().addColumn( "group", Integer.class ) : table.getColumn( "group" ) ),
										( table.getColumn( "title" ) == null ? treeGraph[stepNo].getNodeTable().addColumn( "title", String.class ) : table.getColumn( "title" ) ),
										( table.getColumn( "type" ) == null ? treeGraph[stepNo].getNodeTable().addColumn( "type", Integer.class ) : table.getColumn( "type" ) ),
										( table.getColumn( "size" ) == null ? treeGraph[stepNo].getNodeTable().addColumn( "size", Integer.class ) : table.getColumn( "size" ) ) };
								for ( Column col : allColumns )
									newNode.setAttribute( col, neighbor.getAttribute( treeGraph[stepNo-1].getNodeTable().getColumn( col.getId() ) ) );
								currentGraph.addNode( newNode );
							}
							if ( newNode != null && currentGraph.getEdge( node, newNode ) == null )
								currentGraph.addEdge( treeGraph[stepNo].factory().newEdge( node, newNode, false ) );
						}
					}
					currentGraph.writeUnlock();
					//currentGraph.writeUnlock();
					//calculating the positions
					OpenOrdLayout fourthLayout = new OpenOrdLayout( new OpenOrdLayoutBuilder() );
					fourthLayout.resetPropertiesValues();
					fourthLayout.setGraphModel( treeGraph[stepNo] );
					fourthLayout.initAlgo();
					for(int i = 0; i < 50 && fourthLayout.canAlgo(); i++) {
						fourthLayout.goAlgo();
					}
				}
				catch (InterruptedException | URISyntaxException | ExecutionException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			//return degreeTopNInterestResult;
			return util.getStepGraph( treeGraph[stepNo], stepNo );
		}
		else if ( stepNo == 4 )
		{
			if ( currentAuthor != null && currentAuthor.equals( researcherID ) &&
					finalResults != null && finalResults.length() > 0 )
			{
				return finalResults;
			}

			if ( currentAuthor == null || !currentAuthor.equals( researcherID ) ||
					degreeTopNInterests == null || degreeTopNInterests.length() <= 0)
			try
			{
				degreeTopNInterests = computeBetweenness(researcherID);
			}
			catch (InterruptedException | URISyntaxException | ExecutionException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		recItems = step3;

		

		try
		{
			recItems = computeBetweenness(researcherID);
			//System.out.println( "BetwweennessRecResult List: " + recItems.toString() );
		}
		catch (InterruptedException | URISyntaxException | ExecutionException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		JSONArray DegreeRecResult = new JSONArray();
		JSONObject DegreeRecItemsObj = new JSONObject();
		DegreeRecItemsObj.put("rItems", recItems);

		if ( stepNo == 4 )
		{
			if ( currentAuthor == null || !currentAuthor.equals( researcherID ) || 
					step5 == null || step5.length() < 1 )
			{
				step5 = rRecommenderOfDegree( researcherID );
				topCoAuthorsGraph = null;
			}

			if ( topCoAuthorsGraph == null || topCoAuthorsGraph.length() < 1 )
			{
				topCoAuthorsGraph = util.createTop10AuthorsGraph( step5, step3 );
				updateGraphNode( topCoAuthorsGraph, degreeTopNInterestResult, stepNo );
			}
			//return topCoAuthorsGraph;
			return util.getStepGraph( treeGraph[stepNo], stepNo );
		}

		ArrayList<String> researcherIDs = null;

		if ( stepNo == 5 ) 
		{
			boolean shouldSetGraph = false;
			if ( currentAuthor == null || !currentAuthor.equals( researcherID ) || 
					step6 == null || step6.length() < 1 )
			{
				JSONArray recR = step5;
				//System.out.println( "Calculated betweenness completed." );
				JSONObject recRObj = new JSONObject();
				recRObj.put( "sResearchers", recR );
				DegreeRecResult.put( recRObj );
				DegreeRecResult.put( DegreeRecItemsObj );
				// start paper recommendation based recommended researchers
				researcherIDs = UtilService
						.FindRIdsInFinalJson(DegreeRecResult);
				//System.out.println( "Find researcher ids JSON completed." );
				//ArrayList<String> iNames = UtilService
				//		.FindINamesInFinalJson(DegreeRecResult);
				//System.out.println( "Find researcher name completed." );
				step6 = util.addPubsOfIToJson( 
						DegreeRecResult, researcherIDs, networkCentrality, pubService );
				shouldSetGraph = true;
				JSONArray publicationGraph = util.createPublicationGraph( step6, topCoAuthorsGraph, step3 );
				if ( shouldSetGraph )
					updateGraphNode( publicationGraph, topCoAuthorsGraph, stepNo );
			}

			//return publicationGraph;
			return util.getStepGraph( treeGraph[stepNo], stepNo );
		}

		JSONArray DegreeRecFinalResult = step6;
		// start Common interest Json
		step4 = util
				.addCommonInterestListToJson(DegreeRecFinalResult, researcherIDs, researcherID );
		return step4; //degreeResult;
*/	
			return null;
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
									if ( currentStep == 1 )
									{
										coAuthorsCheck = Integer.valueOf( String.valueOf( neighbor.getAttribute( "group" ) ) ) <= coAuthorSteps &
															neighborAdded < 1;
										insertId = 1;
									}
									if ( currentStep == 2 )
									{
										coAuthorsCheck = Integer.valueOf( String.valueOf( neighbor.getAttribute( "group" ) ) ) > 0;
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
									else if ( insertId == 2 && Integer.valueOf( String.valueOf( neighbor.getAttribute( "type" ) ) ) == UtilINImp._INTERST_NODE_CODE && 
											stepIds.get( 2 ) != null )
										stepIds.get( 2 ).add( String.valueOf( neighbor.getId() ) );
									else if ( insertId == 3 && Integer.valueOf( String.valueOf( neighbor.getAttribute( "type" ) ) ) == UtilINImp._INTERST_NODE_CODE && 
											stepIds.get( 3 ) != null )
										stepIds.get( 3 ).add( String.valueOf( neighbor.getId() ) );
									else if ( insertId == 4 && Integer.valueOf( String.valueOf( neighbor.getAttribute( "type" ) ) ) == UtilINImp._AUTHOR_NODE_CODE && 
										stepIds.get( 4 ) != null )
										stepIds.get( 4 ).add( String.valueOf( neighbor.getId() ) );
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

	@Override
	protected void recommendationStep1( Author researcher, UtilINImp util )
	{
		String researcherID = researcher.getId();
		
		superClass.setStep1( util.get3DCoAuthorsGraph( researcherID ) );
		
		JSONArray tempArray = new JSONArray();
		JSONObject tempObj = new JSONObject();
		JSONArray nodes = new JSONArray();
		//nodes.put( util.getGraphNode( treeGraph[0].getUndirectedGraphVisible().getNode( researcher.getId() ), treeGraph[0] ) );
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
		try
		{
			superClass.setStep2( util.interestSNFileCreator( researcherID, superClass.getTreeGraph()[1] ) );
			JSONArray coAuthorInterestResult = util.createInterestGraph( superClass.getStep2() );
			superClass.updateGraphNode( coAuthorInterestResult, superClass.getStep1(), 2 );
		}
		catch (SQLException | IOException e)
		{
		}
	}

	@Override
	protected void recommendationStep3( Author researcher, UtilINImp util )
	{
		String researcherID = researcher.getId();
		
		try
		{
			superClass.setStep3( computeBetweenness( researcherID ) );
			
			degreeTopNInterestResult = util.createDegreeGraph( superClass.getStep3() );
			superClass.updateGraphNode( degreeTopNInterestResult, null, 3 );
	
			//inserting coAuthors
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
					if ( Integer.valueOf( String.valueOf( neighbor.getAttribute( "type" ) ) ) == 2 )
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
			/*Column typeCol = superClass.getTreeGraph()[2].getNodeTable().getColumn( "type" );
			if ( typeCol == null )
				typeCol = superClass.getTreeGraph()[2].getNodeTable().addColumn( "type", Integer.class );
			currentGraph.writeLock();
			for ( Node node : currentGraph.getNodes() )
			{
				Node preNode = prevGraph.getNode( node.getId() );
				for ( Node neighbor : prevGraph.getNeighbors( preNode ) )
				{
					Node test = currentGraph.getNode( neighbor.getId() );
					Node newNode = null;
					if ( test == null && Integer.valueOf( String.valueOf( neighbor.getAttribute( typeCol ) ) ) == 1 )
					{
						newNode = superClass.getTreeGraph()[3].factory().newNode( neighbor.getId() );
						newNode.setLabel( neighbor.getLabel() );
						newNode.setSize( 150 );
						newNode.setX( neighbor.x() );
						newNode.setY( neighbor.y() );
						Table table = superClass.getTreeGraph()[3].getNodeTable();
						Column[] allColumns = { 
								( table.getColumn( "stepNo" ) == null ? superClass.getTreeGraph()[3].getNodeTable().addColumn( "stepNo", Integer.class ) : table.getColumn( "stepNo" ) ),
								( table.getColumn( "group" ) == null ? superClass.getTreeGraph()[3].getNodeTable().addColumn( "group", Integer.class ) : table.getColumn( "group" ) ),
								( table.getColumn( "title" ) == null ? superClass.getTreeGraph()[3].getNodeTable().addColumn( "title", String.class ) : table.getColumn( "title" ) ),
								( table.getColumn( "type" ) == null ? superClass.getTreeGraph()[3].getNodeTable().addColumn( "type", Integer.class ) : table.getColumn( "type" ) ),
								( table.getColumn( "size" ) == null ? superClass.getTreeGraph()[3].getNodeTable().addColumn( "size", Integer.class ) : table.getColumn( "size" ) ) };
						for ( Column col : allColumns )
							newNode.setAttribute( col, neighbor.getAttribute( superClass.getTreeGraph()[2].getNodeTable().getColumn( col.getId() ) ) );
						currentGraph.addNode( newNode );
					}
					if ( newNode != null && currentGraph.getEdge( node, newNode ) == null )
						currentGraph.addEdge( superClass.getTreeGraph()[3].factory().newEdge( node, newNode, false ) );
				}
			}*/
			currentGraph.writeUnlock();
			//currentGraph.writeUnlock();
			//calculating the positions
			OpenOrdLayout fourthLayout = new OpenOrdLayout( new OpenOrdLayoutBuilder() );
			fourthLayout.resetPropertiesValues();
			fourthLayout.setGraphModel( superClass.getTreeGraph()[3] );
			fourthLayout.initAlgo();
			for(int i = 0; i < 50 && fourthLayout.canAlgo(); i++) {
				fourthLayout.goAlgo();
			}
		}
		catch (JSONException | UnsupportedEncodingException | SQLException | InterruptedException | URISyntaxException | ExecutionException e)
		{
		}
	}

	@Override
	protected void recommendationStep4( Author researcher, UtilINImp util )
	{
		String researcherID = researcher.getId(); 
		
		try
		{
			superClass.setStep4( rRecommenderOfDegree( researcherID ) );
			topCoAuthorsGraph = util.createTop10AuthorsGraph( superClass.getStep4(), superClass.getStep3() );
			superClass.updateGraphNode( topCoAuthorsGraph, degreeTopNInterestResult, 4 );
		}
		catch (JSONException | IOException | SQLException e)
		{
		}
	}

	@Override
	protected void recommendationStep5( Author researcher, UtilINImp util )
	{
		JSONArray DegreeRecResult = new JSONArray();
		JSONObject DegreeRecItemsObj = new JSONObject();
		
		DegreeRecItemsObj.put( "rItems", superClass.getStep3() );
		
		JSONArray recR = superClass.getStep4();
		//System.out.println( "Calculated betweenness completed." );
		JSONObject recRObj = new JSONObject();
		recRObj.put( "sResearchers", recR );
		DegreeRecResult.put( recRObj );
		DegreeRecResult.put( DegreeRecItemsObj );
		// start paper recommendation based recommended researchers
		ArrayList<String> researcherIDs = UtilService
				.FindRIdsInFinalJson(DegreeRecResult);
		//System.out.println( "Find researcher ids JSON completed." );
		//ArrayList<String> iNames = UtilService
		//		.FindINamesInFinalJson(DegreeRecResult);
		//System.out.println( "Find researcher name completed." );
		superClass.setStep5( util.addPubsOfIToJson( 
				DegreeRecResult, researcherIDs, superClass.getNetworkCentrality(), superClass.getPubService() ) );
		JSONArray publicationGraph = util.createPublicationGraph( superClass.getStep5(), topCoAuthorsGraph, superClass.getStep3() );
		superClass.updateGraphNode( publicationGraph, topCoAuthorsGraph, 5 );
	}

	@Override
	protected void recommendationStep6( Author researcher, UtilINImp util )
	{
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

		int coAuthorSteps = 1;
		for ( int i = 0; i < superClass.getTreeGraph().length; i++ )
		{
			step += increment;
			//List<String> tempIds = getStepNodes( step, researcher.getId(), nodesJSON, uniqueIds, increment, true, false, graphIds );
			boolean tempIds = createSingleTreeStep( step, coAuthorSteps, graphIds, uniqueIds, nodesJSON );
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
			if ( step == 1 )
			{
				List<String> ids = graphIds.get( 0 );
				ids.remove( researcher.getId() );
				coAuthorsList.addAll( ids );
				if ( coAuthorSteps > 0 )
				{
					graphIds.put( 1, ids );
					coAuthorSteps--; step++; i--; 
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
}

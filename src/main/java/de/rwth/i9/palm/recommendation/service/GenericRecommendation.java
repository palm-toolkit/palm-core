package de.rwth.i9.palm.recommendation.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.mahout.cf.taste.common.TasteException;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.Table;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.layout.plugin.openord.OpenOrdLayout;
import org.gephi.layout.plugin.openord.OpenOrdLayoutBuilder;
import org.hibernate.SessionFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.persistence.AuthorDAO;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.recommendation.service.PublicationExtractionService;
import de.rwth.i9.palm.recommendation.service.SNAC3RecommendationFeatureImpl;
import de.rwth.i9.palm.recommendation.service.SNAINRecommendationFeatureImpl;
import de.rwth.i9.palm.recommendation.service.UtilINImp;
import de.rwth.i9.palm.topicextraction.service.TopicExtractionService;

public abstract class GenericRecommendation
{

	private Map<String, Number> networkCentrality = new LinkedHashMap<>();

	private GenericRecommendation inReco = null;

	private PersistenceStrategy persistenceStrategy;

	private TopicExtractionService topicExtractionService;

	private SessionFactory sessionFactory;

	private PublicationExtractionService pubService;
	
	//store list of coAuthors: step 1
	private JSONArray step1 = null;

	//store list for graph interest: step 2
	private JSONArray step2 = null;

	//store step 3 results: step 3
	private JSONArray step3 = null;

	//store step 4 results: step 4
	private JSONArray step4 = null;	

	//store list of top N authors: step 5
	private JSONArray step5 = null;

	//store list of publications: step 6
	private JSONArray step6 = null;

	//store the author Id to persist each steps.
	private String currentAuthor = null;

	private GraphModel[] treeGraph = null;

	private UtilINImp util = null;

	protected abstract void recommendationStep1( Author researcher, UtilINImp util );

	protected abstract void recommendationStep2( Author researcher, UtilINImp util );

	protected abstract void recommendationStep3( Author researcher, UtilINImp util );

	protected abstract void recommendationStep4( Author researcher, UtilINImp util );

	protected abstract void recommendationStep5( Author researcher, UtilINImp util );

	protected abstract void recommendationStep6( Author researcher, UtilINImp util );

	protected abstract JSONArray computeSingleTree( Author researcher, int stepNo, String id );

	private void initTreeGraph() 
	{
		treeGraph = null;
		treeGraph = new GraphModel[6];
		treeGraph[0] = GraphModel.Factory.newInstance();
		treeGraph[1] = GraphModel.Factory.newInstance();
		treeGraph[2] = GraphModel.Factory.newInstance();
		treeGraph[3] = GraphModel.Factory.newInstance();
		treeGraph[4] = GraphModel.Factory.newInstance();
		treeGraph[5] = GraphModel.Factory.newInstance();

		util = new UtilINImp( persistenceStrategy, topicExtractionService, sessionFactory );
	}

	private void insertResearcherNode( Author researcher )
	{
		if ( treeGraph == null )
		{
			initTreeGraph();
		}

		Table table = treeGraph[0].getNodeTable();
		Column stepCol = table.addColumn( "stepNo", Integer.class );
		Column groupCol = table.addColumn( "group", Integer.class );
		Column titleCol = table.addColumn( "title", String.class );
		Column typeCol = table.addColumn( "type", Integer.class );
		Column sizeCol = table.addColumn( "size", Integer.class );

		Node graphNode = treeGraph[0].factory().newNode( researcher.getId() );
		graphNode.setAttribute( stepCol, 0 );
		graphNode.setAttribute( groupCol, 0 );
		graphNode.setAttribute( titleCol, "Researcher" );
		graphNode.setAttribute( typeCol, 0 );
		graphNode.setAttribute( sizeCol, 450 );
		graphNode.setLabel( researcher.getName() );
		treeGraph[0].getUndirectedGraphVisible().addNode( graphNode );
	}

	public void initObjects( PersistenceStrategy persistenceStrategy, 
			TopicExtractionService topicExtractionService,
			SessionFactory sessionFactory,
			PublicationExtractionService pubService )
	{
		this.persistenceStrategy = persistenceStrategy;
		this.topicExtractionService = topicExtractionService;
		this.sessionFactory = sessionFactory;
		this.pubService = pubService;
	}
	
	public JSONObject getGraphLink( org.gephi.graph.api.Edge edge )
	{
		JSONObject obj = new JSONObject();
		obj.put( "source", edge.getSource().getId() );
		obj.put( "target", edge.getTarget().getId() );
		return obj;
	}

	public void updateGraphNode( JSONArray graphNodes, JSONArray prevGraphNodes, int stepNo )
	{	
		if ( treeGraph == null )
		{
			treeGraph = new GraphModel[6];
			treeGraph[0] = GraphModel.Factory.newInstance();
			treeGraph[1] = GraphModel.Factory.newInstance();
			treeGraph[2] = GraphModel.Factory.newInstance();
			treeGraph[3] = GraphModel.Factory.newInstance();
			treeGraph[4] = GraphModel.Factory.newInstance();
			treeGraph[5] = GraphModel.Factory.newInstance();
		}

		UndirectedGraph graph = treeGraph[stepNo].getUndirectedGraphVisible();
		Set<String> uniqueIds = new LinkedHashSet<String>();

		//extracting previous step nodes
		if ( prevGraphNodes != null && prevGraphNodes.length() > 0 )
		{
			JSONObject obj = prevGraphNodes.optJSONObject( 0 );
			JSONArray nodes = obj.optJSONArray( "nodes" );

			//inserting nodes into graph
			if ( nodes != null && nodes.length() > 0 )
			{
				for ( int i = 0; i < nodes.length(); i++ )
				{
					JSONObject node = nodes.optJSONObject( i );
					if ( node != null )
					{
						String id = node.optString( "id" );
						String title = node.optString( "title" );
						String details = node.optString( "details" );
						int group = node.optInt( "group", 0 );
						int type = node.optInt( "type", 0 );
						int size = node.optInt( "size", 150 );

						Table table = treeGraph[stepNo].getNodeTable();
						Column stepCol = table.getColumn( "stepNo" );
						if ( stepCol == null )
							stepCol = table.addColumn( "stepNo", Integer.class );
						Column groupCol = table.getColumn( "group" );
						if ( groupCol == null )
							groupCol = table.addColumn( "group", Integer.class );
						Column titleCol = table.getColumn( "title" );
						if ( titleCol == null )
							titleCol = table.addColumn( "title", String.class );
						Column typeCol = table.getColumn( "type" );
						if ( typeCol == null )
							typeCol = table.addColumn( "type", Integer.class );
						Column sizeCol = table.getColumn( "size" );
						if ( sizeCol == null )
							sizeCol = table.addColumn( "size", Integer.class );

						Node graphNode = treeGraph[stepNo].factory().newNode( id );
						graphNode.setAttribute( stepCol, stepNo );
						graphNode.setAttribute( groupCol, group );
						graphNode.setAttribute( titleCol, title );
						graphNode.setAttribute( typeCol, type );
						graphNode.setAttribute( sizeCol, size );
						graphNode.setLabel( details );
						graphNode.setSize( size );

						try {
							graph.addNode( graphNode );
							uniqueIds.add( id );
						}
						catch ( IllegalArgumentException e )
						{}
					}
				}
			}
		}

		//extracting current step data
		if ( graphNodes != null && graphNodes.length() > 0 )
		{
			JSONObject obj = graphNodes.optJSONObject( 0 );
			JSONArray nodes = obj.optJSONArray( "nodes" );
			JSONArray links = obj.optJSONArray( "links" );

			//inserting nodes into graph
			if ( nodes != null && nodes.length() > 0 )
			{
				for ( int i = 0; i < nodes.length(); i++ )
				{
					JSONObject node = nodes.optJSONObject( i );
					if ( node != null )
					{
						String id = node.optString( "id" );
						String title = node.optString( "title" );
						String details = node.optString( "details" );
						int group = node.optInt( "group", 0 );
						int type = node.optInt( "type", 0 );
						int size = node.optInt( "size", 150 );

						Table table = treeGraph[stepNo].getNodeTable();
						Column stepCol = table.getColumn( "stepNo" );
						if ( stepCol == null )
							stepCol = table.addColumn( "stepNo", Integer.class );
						Column groupCol = table.getColumn( "group" );
						if ( groupCol == null )
							groupCol = table.addColumn( "group", Integer.class );
						Column titleCol = table.getColumn( "title" );
						if ( titleCol == null )
							titleCol = table.addColumn( "title", String.class );
						Column typeCol = table.getColumn( "type" );
						if ( typeCol == null )
							typeCol = table.addColumn( "type", Integer.class );
						Column sizeCol = table.getColumn( "size" );
						if ( sizeCol == null )
							sizeCol = table.addColumn( "size", Integer.class );

						Node graphNode = treeGraph[stepNo].factory().newNode( id );
						graphNode.setAttribute( stepCol, stepNo );
						graphNode.setAttribute( groupCol, group );
						graphNode.setAttribute( titleCol, title );
						graphNode.setAttribute( typeCol, type );
						graphNode.setAttribute( sizeCol, size );
						graphNode.setSize( size );
						graphNode.setLabel( details );

						try {
							if ( !uniqueIds.contains( id ) )
							{
								graph.addNode( graphNode );
								uniqueIds.add( id );
							}
						}
						catch ( IllegalArgumentException e )
						{}
					}
				}
			}

			//inserting links into graph
			if ( links != null && links.length() > 0 )
			{
				for ( int i = 0; i < links.length(); i++ )
				{
					JSONObject link = links.optJSONObject( i );
					if ( link != null )
					{
						String source = link.optString( "source" );
						String target = link.optString( "target" );
						Edge edge = treeGraph[stepNo].factory().newEdge( 
								graph.getNode( source ), graph.getNode( target ), false);
						if ( source == null || target == null || source.isEmpty() || target.isEmpty() )
						{}
						else if ( !graph.contains( edge ) && uniqueIds.contains( source ) && uniqueIds.contains( target ) )
						{
							try {
								graph.addEdge( edge );
							} catch ( Exception e ) {
								System.out.println( "links: " + link );
							}
						}
					}
				}
			}

			//calculating the positions
			AutoLayout autoLayout = new AutoLayout( 1, TimeUnit.MINUTES );
			YifanHuLayout firstLayout = new YifanHuLayout( null, new StepDisplacement( 1.0f ) );
			OpenOrdLayout fourthLayout = new OpenOrdLayout( new OpenOrdLayoutBuilder() );
			ForceAtlasLayout secondLayout = new ForceAtlasLayout( null );

			/*autoLayout.setGraphModel( graphModel );
			AutoLayout.DynamicProperty adjustBySizeProperty = AutoLayout.createDynamicProperty("forceAtlas.adjustSizes.name", Boolean.TRUE, 0.1f);//True after 10% of layout time
		    AutoLayout.DynamicProperty repulsionProperty = AutoLayout.createDynamicProperty("forceAtlas.repulsionStrength.name", new Double(500.), 0f);//500 for the complete period
		    autoLayout.addLayout(firstLayout, 0.5f);
		    autoLayout.addLayout(secondLayout, 0.5f, new AutoLayout.DynamicProperty[]{adjustBySizeProperty, repulsionProperty});
		    //autoLayout.addLayout( fourthLayout, 0.5f );
		    autoLayout.execute();
			autoLayout.addLayout( firstLayout, 0.5f );
			autoLayout.addLayout( fourthLayout, 0.5f );*/
			fourthLayout.resetPropertiesValues();
			//fourthLayout.setCooldownStage( 17 );
			//fourthLayout.setLiquidStage( 7 );
			//fourthLayout.setCrunchStage( 5 );
			//fourthLayout.setExpansionStage( 20 );
			//fourthLayout.setNumIterations( 300 );
			fourthLayout.setGraphModel( treeGraph[stepNo] );

			fourthLayout.initAlgo();

			/*for ( Node node : graph.getNodes() ) 
			{
				node.setX( 1.0f );
				node.setY( 1.0f );
			}*/

			for(int i = 0; i < 400 && fourthLayout.canAlgo(); i++) {
				fourthLayout.goAlgo();
			}

			//autoLayout.execute();
		}

		System.out.println( "After count: " + graph.getNodeCount() );
	}

	public JSONArray computeRecommendation( String algorithm, Author researcher, int stepNo ) throws JSONException, SQLException, IOException, TasteException
	{
		if ( researcher == null || researcher == null )
		{
			return new JSONArray();
		}

		String researcherID = researcher.getId();

		// creating algorithm specific object.
		if ( algorithm != null && algorithm.equals( "interest" ) )
		{
			if ( inReco == null || !( inReco instanceof SNAINRecommendationFeatureImpl ) )
				inReco = new SNAINRecommendationFeatureImpl( this );
		}
		else if ( algorithm != null && algorithm.equals( "c3d" ) )
		{
			if ( inReco == null || !( inReco instanceof SNAC3RecommendationFeatureImpl ) )
				inReco = new SNAC3RecommendationFeatureImpl( this );
		}
		else if ( algorithm != null && algorithm.equals( "c2d" ) )
		{
			if ( inReco == null || !( inReco instanceof SNAC2RecommendationFeatureImpl ) )
				inReco = new SNAC2RecommendationFeatureImpl( this );
		}

		// calling algorithm specific recommendation step
		if ( stepNo == 1 )
		{
			if ( currentAuthor == null || !currentAuthor.equals( researcherID ) || 
					step1 == null || step1.length() < 1 )
			{
				initTreeGraph();

				step1 = step2 = step3 = step4 = step5 = step6 = null;

				//creating temprary step0 graph
				insertResearcherNode( researcher );

				currentAuthor = new String( researcherID );
				inReco.recommendationStep1( researcher, util );
			}
		}
		else if ( stepNo == 2 )
		{
			if ( currentAuthor == null || !currentAuthor.equals( researcherID ) || 
					step2 == null || step2.length() < 1 )
			{
				inReco.recommendationStep2( researcher, util );
			}
		}
		else if ( stepNo == 3 )
		{
			if ( currentAuthor == null || !currentAuthor.equals( researcherID ) || 
					step3 == null || step3.length() < 1 )
			{
				inReco.recommendationStep3( researcher, util );
			}
		}
		else if ( stepNo == 4 )
		{
			if ( currentAuthor == null || !currentAuthor.equals( researcherID ) || 
					step4 == null || step4.length() < 1 )
			{
				inReco.recommendationStep4( researcher, util );
			}
		}
		else if ( stepNo == 5 )
		{
			if ( currentAuthor == null || !currentAuthor.equals( researcherID ) || 
					step5 == null || step5.length() < 1 )
			{
				inReco.recommendationStep5( researcher, util );
			}
		}
		else if ( stepNo == 6 )
		{
			if ( currentAuthor == null || !currentAuthor.equals( researcherID ) || 
					step6 == null || step6.length() < 1 )
			{
				inReco.recommendationStep6( researcher, util );
			}
		}

		JSONArray arr = util.getStepGraph( treeGraph[stepNo], stepNo );
		return arr;
	}

	public JSONArray computeSingleTree( String algorithm, Author researcher, int stepNo, String id )
	{
		GenericRecommendation inReco = null;

		// creating algorithm specific object.
		if ( algorithm != null && algorithm.equals( "interest" ) )
		{
			if ( inReco == null || !( inReco instanceof SNAINRecommendationFeatureImpl ) )
				inReco = new SNAINRecommendationFeatureImpl( this );
		}
		else if ( algorithm != null && algorithm.equals( "c3d" ) )
		{
			if ( inReco == null || !( inReco instanceof SNAC3RecommendationFeatureImpl ) )
				inReco = new SNAC3RecommendationFeatureImpl( this );
		}
		else if ( algorithm != null && algorithm.equals( "c2d" ) )
		{
			if ( inReco == null || !( inReco instanceof SNAC2RecommendationFeatureImpl ) )
				inReco = new SNAC2RecommendationFeatureImpl( this );
		}

		return inReco.computeSingleTree( researcher, stepNo, id );
	}

	public List<Object> requesetAuthor( String query, int maxResults, String queryType )
	{
		AuthorDAO dao = persistenceStrategy.getAuthorDAO();
		List<Author> list = dao.getAll();
		ArrayList<String> authors = new ArrayList<>();
		for ( Author auth : list )
		{
			authors.add( auth.getId() );
		}

		if ( queryType.equalsIgnoreCase( "author" ) )
		{
			Map<String, Object> authorMap = 
					persistenceStrategy.getAuthorDAO().getAuthorWithPaging( query, "No", 0, maxResults );
			List<Author> aList = (List<Author>) authorMap.get( "authors" );
			
			if ( aList != null )
			{
				List<Object> authorIdMap = new LinkedList<>();
				for ( Author author : aList )
				{
					Map<String, String> items = new HashMap<>();
					items.put( "label", author.getName() );
					items.put( "value", author.getId() );
					authorIdMap.add( items );
				}
				return authorIdMap;
			}
		}
		else 
		{
			try
			{
				List<String> res = util.topRofIFinderString( authors, query, maxResults );
				List<Object> authorIdMap = new LinkedList<>();
	
				if ( res != null && !res.isEmpty() )
				{
					for ( String string : res )
					{
						Map<String, String> items = new HashMap<>();
						Author author = dao.getById( string );
						items.put( "label", author.getName() );
						items.put( "value", string );
						authorIdMap.add( items );
					}
	
					return authorIdMap;
				}
			}
			catch (JSONException | IOException | SQLException e)
			{
			}
		}
		return null;
	}

	// Getter functions

	public UtilINImp getUtil()
	{
		return util;
	}

	public String getCurrentAuthor()
	{
		return currentAuthor;
	}

	public Map<String, Number> getNetworkCentrality()
	{
		return networkCentrality;
	}

	public PersistenceStrategy getPersistenceStrategy()
	{
		return persistenceStrategy;
	}

	public PublicationExtractionService getPubService()
	{
		return pubService;
	}

	public SessionFactory getSessionFactory()
	{
		return sessionFactory;
	}

	public JSONArray getStep1()
	{
		return step1;
	}

	public JSONArray getStep2()
	{
		return step2;
	}

	public JSONArray getStep3()
	{
		return step3;
	}

	public JSONArray getStep4()
	{
		return step4;
	}

	public JSONArray getStep5()
	{
		return step5;
	}

	public JSONArray getStep6()
	{
		return step6;
	}

	public TopicExtractionService getTopicExtractionService()
	{
		return topicExtractionService;
	}

	public GraphModel[] getTreeGraph()
	{
		return treeGraph;
	}

	public void setStep1( JSONArray step1 )
	{
		this.step1 = step1;
	}

	public void setStep2( JSONArray step2 )
	{
		this.step2 = step2;
	}

	public void setStep3( JSONArray step3 )
	{
		this.step3 = step3;
	}

	public void setStep4( JSONArray step4 )
	{
		this.step4 = step4;
	}

	public void setStep5( JSONArray step5 )
	{
		this.step5 = step5;
	}

	public void setStep6( JSONArray step6 )
	{
		this.step6 = step6;
	}

	public void setTreeGraph( GraphModel[] treeGraph )
	{
		this.treeGraph = treeGraph;
	}
}
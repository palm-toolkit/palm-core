package de.rwth.i9.palm.graph.feature;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.appearance.api.Function;
import org.gephi.appearance.plugin.RankingElementColorTransformer;
import org.gephi.appearance.plugin.RankingNodeSizeTransformer;
import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.Query;
import org.gephi.filters.api.Range;
import org.gephi.filters.plugin.graph.DegreeRangeBuilder.DegreeRangeFilter;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.exporter.spi.GraphExporter;
import org.gephi.io.generator.plugin.RandomGraph;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.fruchterman.FruchtermanReingold;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.statistics.plugin.GraphDistance;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

import de.rwth.i9.palm.helper.comparator.CoAuthorByNumberOfCollaborationComparator;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;

public class GraphFeatureImpl implements GraphFeature
{
	String filename = null;

	@Override
	public Map<String, Object> graphData( Author author )
	{
		// researchers list container
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		if ( author.getPublications() == null || author.getPublications().isEmpty() )
		{
			responseMap.put( "count", 0 );
			return responseMap;
		}

		// prepare a list of map object containing coauthor properties and
		Map<String, Integer> coAuthorCollaborationCountMap = new HashMap<String, Integer>();
		// Prepare set of coauthor HashSet;
		Set<Author> coauthorSet = new HashSet<Author>();
		// number of collaboration
		for ( Publication publication : author.getPublications() )
		{
			for ( Author coAuthor : publication.getAuthors() )
			{
				// just skip if its himself
				if ( coAuthor.equals( author ) )
					continue;

				coauthorSet.add( coAuthor );

				if ( coAuthorCollaborationCountMap.get( coAuthor.getId() ) == null )
					coAuthorCollaborationCountMap.put( coAuthor.getId(), 1 );
				else
					coAuthorCollaborationCountMap.put( coAuthor.getId(), coAuthorCollaborationCountMap.get( coAuthor.getId() ) + 1 );
			}
		}

		// prepare list of object map containing coAuthor details
		List<Map<String, Object>> coAuthorList = new ArrayList<Map<String, Object>>();

		for ( Author coAuthor : coauthorSet )
		{
			// only copy necessary attributes
			Map<String, Object> coAuthorMap = new LinkedHashMap<String, Object>();
			coAuthorMap.put( "id", coAuthor.getId() );
			coAuthorMap.put( "name", coAuthor.getName() );
			if ( coAuthor.getInstitution() != null )
				coAuthorMap.put( "affiliation", coAuthor.getInstitution().getName() );
			if ( coAuthor.getPhotoUrl() != null )
				coAuthorMap.put( "photo", coAuthor.getPhotoUrl() );
			coAuthorMap.put( "isAdded", coAuthor.isAdded() );
			coAuthorMap.put( "coautorTimes", coAuthorCollaborationCountMap.get( coAuthor.getId() ) );

			// add into list
			coAuthorList.add( coAuthorMap );
		}

		Collections.sort( coAuthorList, new CoAuthorByNumberOfCollaborationComparator() );

		// prepare list of object map containing coAuthor details
		List<Map<String, Object>> coAuthorListPaging = new ArrayList<Map<String, Object>>();

		int position = 0;
		for ( Map<String, Object> coAuthor : coAuthorList )
		{
			// if ( position >= startPage && coAuthorListPaging.size() <
			// maxresult )
			// {
			coAuthorListPaging.add( coAuthor );
			// }
		}

		// remove unnecessary result

		// put coauthor to responseMap
		responseMap.put( "countTotal", coAuthorList.size() );
		responseMap.put( "count", coAuthorListPaging.size() );
		responseMap.put( "coAuthors", coAuthorListPaging );

		return responseMap;
	}

	@Override
	public void newFunc()
	{
		// Edges edge = new Edges();
		// Nodes node1 = new Nodes();
		// Nodes node2 = new Nodes();
		//
		// node1.setLabel( "node1" );
		// node2.setLabel( "node2" );
		//
		// edge.setLabel( "edge1" );
		// edge.setSource( node1.getLabel() );
		// edge.setTarget( node2.getLabel() );
		// edge.setWeight( "1" );

		// Init a project - and therefore a workspace
		ProjectController pc = Lookup.getDefault().lookup( ProjectController.class );
		pc.newProject();
		System.out.println( "1" );
		// Workspace ws = pc.getCurrentWorkspace();
		// pc.deleteWorkspace( ws );

		// Create new workspace
		Workspace workspace = pc.newWorkspace( pc.getCurrentProject() );
		System.out.println( "2" );
		pc.openWorkspace( workspace );

		System.out.println( "workspace id:" + workspace.getId() );
		// WorkspaceListener workspaceListener = null ;
		// workspaceListener.initialize( workspace );

		// workspace.add( new GraphModel() );

		// Get a graph model - it exists because we have a workspace
		// GraphModel graphModel = workspace.getLookup().lookup(
		// GraphModel.class );
		// graphModel.createView();
		GraphModel graphModel = Lookup.getDefault().lookup( GraphController.class ).getGraphModel();// GraphModel(
																								// workspace
																								// );
		FilterController filterController = Lookup.getDefault().lookup( FilterController.class );
		AppearanceController appearanceController = Lookup.getDefault().lookup( AppearanceController.class );
		AppearanceModel appearanceModel = appearanceController.getModel();
		PreviewModel model = Lookup.getDefault().lookup( PreviewController.class ).getModel();

		// Append as a Directed Graph
		DirectedGraph directedGraph = graphModel.getDirectedGraph();

		List<String> dummyNames = new ArrayList<String>();
		dummyNames.add( "Dr. Chatti" );
		dummyNames.add( "Prof. Shroeder" );
		dummyNames.add( "Manpriya Guliani" );
		dummyNames.add( "Jan Murmann" );
		dummyNames.add( "Verena Schweiger" );

		Random rand = new Random();

		int max = 300;
		int min = 0;

		List<String> nodeNames = new ArrayList<String>();
		List<String> edgeNames = new ArrayList<String>();
		for ( int i = 0; i < dummyNames.size(); i++ )
		{
			nodeNames.add( "n" + rand.nextInt( ( max - min ) + 1 ) + min );
			edgeNames.add( "e" + rand.nextInt( ( max - min ) + 1 ) + min );
		}


		List<Node> nodes = new ArrayList<Node>();
		for ( int j = 0; j < nodeNames.size(); j++ )
		{
			nodes.add( graphModel.factory().newNode( nodeNames.get( j ) ) );
			nodes.get( j ).setLabel( dummyNames.get( j ) );
			nodes.get( j ).setPosition( rand.nextInt( ( max - min ) + 1 ) + min * j, rand.nextInt( ( max - min ) + 1 ) + min * j );
			// nodes.get( j ).setPosition( 0, 0 );
			directedGraph.addNode( nodes.get( j ) );
		}

		System.out.println( "3" );
		List<Edge> edges = new ArrayList<Edge>();
		for ( int k = 1; k < nodeNames.size(); k++ )
		{
			edges.add( graphModel.factory().newEdge( nodes.get( 0 ), nodes.get( k ), 0, (double) k * 3, true ) );
			directedGraph.addEdge( edges.get( k - 1 ) );
		}
		// // Create three edges
		// Edge e1 = graphModel.factory().newEdge( nodes.get( 0 ), nodes.get( 1
		// ), 0, 1.0, true );
		// Edge e2 = graphModel.factory().newEdge( nodes.get( 0 ), nodes.get( 2
		// ), 0, 1.0, true );
		Edge e4 = graphModel.factory().newEdge( nodes.get( 1 ), nodes.get( 2 ), 0, 1.0, true );
		// Edge e4 = graphModel.factory().newEdge( nodes.get( 0 ), nodes.get( 3
		// ), 0, 1.0, true );
		// Edge e5 = graphModel.factory().newEdge( nodes.get( 0 ), nodes.get( 4
		// ), 0, 1.0, true );
		Edge e5 = graphModel.factory().newEdge( nodes.get( 1 ), nodes.get( 4 ), 0, 1.0, true );
		//
		// directedGraph.addEdge( e1 );
		// directedGraph.addEdge( e2 );
		// directedGraph.addEdge( e3 );
		directedGraph.addEdge( e4 );
		directedGraph.addEdge( e5 );
		// directedGraph.addEdge( e6 );

		// Count nodes and edges
		System.out.println( "Nodes: " + directedGraph.getNodeCount() + " Edges: " + directedGraph.getEdgeCount() );

		// Filter
		DegreeRangeFilter degreeFilter = new DegreeRangeFilter();
		degreeFilter.init( directedGraph );
		degreeFilter.setRange( new Range( 1, Integer.MAX_VALUE ) ); // Remove
		// nodes
		// with
		// degree
		// < 30
		Query query = filterController.createQuery( degreeFilter );
		GraphView view = filterController.filter( query );
		graphModel.setVisibleView( view ); // Set the filter result as the
		// visible view

		// See visible graph stats
		UndirectedGraph graphVisible = graphModel.getUndirectedGraphVisible();
		System.out.println( "Nodes: " + graphVisible.getNodeCount() );
		System.out.println( "Edges: " + graphVisible.getEdgeCount() );

		// Layout for 1 minute
		AutoLayout autoLayout = new AutoLayout( 1, TimeUnit.SECONDS );
		autoLayout.setGraphModel( graphModel );
		YifanHuLayout firstLayout = new YifanHuLayout( null, new StepDisplacement( 1f ) );
		FruchtermanReingold thirdLayout = new FruchtermanReingold( null );
		AutoLayout.DynamicProperty area = AutoLayout.createDynamicProperty( "fruchtermanReingold.area.name", new Double( 1000. ), 1f );

		// ForceAtlasLayout secondLayout = new ForceAtlasLayout( null );
		// AutoLayout.DynamicProperty adjustBySizeProperty =
		// AutoLayout.createDynamicProperty( "forceAtlas.adjustSizes.name",
		// Boolean.TRUE, 0.1f );// True
		// // after 10% of layout time
		// AutoLayout.DynamicProperty repulsionProperty =
		// AutoLayout.createDynamicProperty(
		// "forceAtlas.repulsionStrength.name", new Double( 200. ), 0f );
		// // 500 for the complete period
		autoLayout.addLayout( firstLayout, 0.5f );
		// autoLayout.addLayout( secondLayout, 0.5f, new
		// AutoLayout.DynamicProperty[] { adjustBySizeProperty,
		// repulsionProperty } );
		autoLayout.addLayout( thirdLayout, 0.5f, new AutoLayout.DynamicProperty[] { area } );
		autoLayout.execute();

		// can be used for clustering according to the click
		// OpenOrdLayout ool = new OpenOrdLayout( null );

		// Manual Layout Setup
		// YifanHuLayout layout = new YifanHuLayout( null, new StepDisplacement(
		// 1f ) );
		// layout.setGraphModel( graphModel );
		// layout.initAlgo();
		// layout.resetPropertiesValues();
		// layout.setOptimalDistance( 100f );
		// layout.setAdaptiveCooling( true );
		// layout.setRelativeStrength( 1f );
		//
		// for ( int i = 0; i < 100 && layout.canAlgo(); i++ )
		// {
		// layout.goAlgo();
		// }
		// layout.endAlgo();

		// Get Centrality
		GraphDistance distance = new GraphDistance();
		distance.setDirected( true );
		distance.execute( graphModel );

		// Rank color by Degree
		Function degreeRanking = appearanceModel.getNodeFunction( directedGraph, AppearanceModel.GraphFunction.NODE_DEGREE, RankingElementColorTransformer.class );
		RankingElementColorTransformer degreeTransformer = (RankingElementColorTransformer) degreeRanking.getTransformer();
		degreeTransformer.setColors( new Color[] { new Color( 0x9990D9 ), new Color( 0xB30000 ) } );
		degreeTransformer.setColorPositions( new float[] { 0f, 1f } );
		appearanceController.transform( degreeRanking );

		// Rank size by centrality/Harmonic Closeness
		Column centralityColumn = graphModel.getNodeTable().getColumn( GraphDistance.HARMONIC_CLOSENESS );
		Function centralityRanking = appearanceModel.getNodeFunction( directedGraph, centralityColumn, RankingNodeSizeTransformer.class );
		RankingNodeSizeTransformer centralityTransformer = (RankingNodeSizeTransformer) centralityRanking.getTransformer();
		centralityTransformer.setMinSize( 3 );
		centralityTransformer.setMaxSize( 10 );
		appearanceController.transform( centralityRanking );

		// Preview
		model.getProperties().putValue( PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE );
		model.getProperties().putValue( PreviewProperty.EDGE_COLOR, new EdgeColor( Color.GRAY ) );
		model.getProperties().putValue( PreviewProperty.EDGE_THICKNESS, new Float( 0.1f ) );
		model.getProperties().putValue( PreviewProperty.NODE_LABEL_FONT, model.getProperties().getFontValue( PreviewProperty.NODE_LABEL_FONT ).deriveFont( 8 ) );

		// Export full graph
		ExportController ec = Lookup.getDefault().lookup( ExportController.class );
		try
		{
			System.out.println( "4" );
			File f = new File( "src/main/webapp/resources/gexf/co-authors.gexf" );
			System.out.println( "5" );
			ec.exportFile( f );

			System.out.println( f.getPath() );

			nodes.clear();
			edges.clear();
		}
		catch ( IOException ex )
		{
			System.out.println( ex );
			return;
		}

		// Export only visible graph
		GraphExporter exporter = (GraphExporter) ec.getExporter( "gexf" ); // Get
																			// GEXF
																			// exporter
		exporter.setExportVisible( true ); // Only exports the visible
											// (filtered) graph
		exporter.setWorkspace( workspace );
		try
		{
			ec.exportFile( new File( "src/main/webapp/resources/gexf/co-authors-directed.gexf" ), exporter );
		}
		catch ( IOException ex )
		{
			ex.printStackTrace();
			return;
		}

	}

	public void testFunction()
	{
		// Init a project - and therefore a workspace
		ProjectController pc = Lookup.getDefault().lookup( ProjectController.class );
		pc.newProject();
		final Workspace workspace1 = pc.getCurrentWorkspace();

		Random rand = new Random();

		// Generate a new random graph into a container
		Container container = Lookup.getDefault().lookup( Container.Factory.class ).newContainer();
		RandomGraph randomGraph = new RandomGraph();
		randomGraph.setNumberOfNodes( rand.nextInt( ( 30 - 10 ) + 1 ) + 10 );
		randomGraph.setWiringProbability( 0.005 );
		randomGraph.generate( container.getLoader() );

		// Append container to graph structure
		ImportController importController = Lookup.getDefault().lookup( ImportController.class );
		importController.process( container, new DefaultProcessor(), workspace1 );

		// Duplicate this workspace
		final Workspace workspace2 = pc.duplicateWorkspace( workspace1 );

		// Create Thread Pool for parallel layout
		ExecutorService executor = Executors.newFixedThreadPool( 2 );

		// Run Tasks and wait for termination in the current thread
		Future<?> f1 = executor.submit( createLayoutRunnable( workspace1 ) );
		Future<?> f2 = executor.submit( createLayoutRunnable( workspace2 ) );
		try
		{
			f1.get();
			f2.get();
		}
		catch ( Exception ex )
		{
			Exceptions.printStackTrace( ex );
		}
		executor.shutdown();

		// Export
		// ExportController ec = Lookup.getDefault().lookup(
		// ExportController.class );
		// try
		// {
		// pc.openWorkspace( workspace1 );
		// ec.exportFile( new File( "parallel_worspace1.pdf" ) );
		// pc.openWorkspace( workspace2 );
		// ec.exportFile( new File( "parallel_worspace2.pdf" ) );
		// }
		// catch ( IOException ex )
		// {
		// Exceptions.printStackTrace( ex );
		// return;
		// }

		// Export full graph
		ExportController ec = Lookup.getDefault().lookup( ExportController.class );
		try
		{
			Random r = new Random();

			pc.openWorkspace( workspace1 );
			File f = new File( "src/main/webapp/resources/gexf/co-authors" + r.nextInt() + ".gexf" );
			ec.exportFile( f );

			System.out.println( f.getName() );

			pc.openWorkspace( workspace2 );
			File file2 = new File( "src/main/webapp/resources/gexf/co-authors2.gexf" );
			ec.exportFile( file2 );

			// System.out.println( f.getPath() );
		}
		catch ( IOException ex )
		{
			System.out.println( ex );
			return;
		}
	}

	private Runnable createLayoutRunnable( final Workspace workspace )
	{
		return new Runnable()
		{

			public void run()
			{
				GraphModel gm = Lookup.getDefault().lookup( GraphController.class ).getGraphModel( workspace );
				AutoLayout autoLayout = new AutoLayout( 60, TimeUnit.SECONDS );
				autoLayout.setGraphModel( gm );
				autoLayout.addLayout( new YifanHuLayout( null, new StepDisplacement( 1f ) ), 1f );
				autoLayout.execute();
			}
		};
	}

	@Override
	public Map<String, Object> anotherFunction()
	{

		// Create Thread Pool for parallel layout
		ExecutorService executor = Executors.newFixedThreadPool( 1 );

		Future<?> f = executor.submit( createRunnable() );
		System.out.println( "1" );
		try
		{
			System.out.println( "2" );
			f.get();
			System.out.println( "3" );
		}
		catch ( InterruptedException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch ( ExecutionException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		executor.shutdown();

		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		responseMap.put( "filename", filename );

		return responseMap;

	}

	private Runnable createRunnable()
	{
		return new Runnable()
		{
			// System.out.println( "4" );
			@Override
			public void run()
			{
				System.out.println( "5" );
				// Init a project - and therefore a workspace
				ProjectController pc = Lookup.getDefault().lookup( ProjectController.class );
				pc.newProject();
				// pc.deleteWorkspace( pc.getCurrentWorkspace() );
				Workspace workspace = pc.newWorkspace( pc.getCurrentProject() );
				pc.openWorkspace( workspace );

				// Workspace workspace = pc.getCurrentWorkspace();

				System.out.println( "workspace id:" + workspace.getId() );
				GraphModel graphModel = Lookup.getDefault().lookup( GraphController.class ).getGraphModel();// GraphModel(
				// workspace
				// );
				FilterController filterController = Lookup.getDefault().lookup( FilterController.class );
				AppearanceController appearanceController = Lookup.getDefault().lookup( AppearanceController.class );
				AppearanceModel appearanceModel = appearanceController.getModel();
				PreviewModel model = Lookup.getDefault().lookup( PreviewController.class ).getModel();

				// Append as a Directed Graph
				DirectedGraph directedGraph = graphModel.getDirectedGraph();

				List<String> dummyNames = new ArrayList<String>();
				dummyNames.add( "Dr. Chatti" );
				dummyNames.add( "Prof. Shroeder" );
				dummyNames.add( "Manpriya Guliani" );
				dummyNames.add( "Jan Murmann" );
				dummyNames.add( "Verena Schweiger" );

				Random rand = new Random();

				int max = 300;
				int min = 0;

				List<String> nodeNames = new ArrayList<String>();
				List<String> edgeNames = new ArrayList<String>();
				for ( int i = 0; i < dummyNames.size(); i++ )
				{
					nodeNames.add( "n" + rand.nextInt( ( max - min ) + 1 ) + min );
					edgeNames.add( "e" + rand.nextInt( ( max - min ) + 1 ) + min );
				}


				List<Node> nodes = new ArrayList<Node>();
				List<Edge> edges = new ArrayList<Edge>();

				nodes.clear();
				edges.clear();

				workspace.remove( nodes );
				workspace.remove( edges );

				for ( int j = 0; j < nodeNames.size(); j++ )
				{
					nodes.add( graphModel.factory().newNode( nodeNames.get( j ) ) );
					nodes.get( j ).setLabel( dummyNames.get( j ) );
					nodes.get( j ).setPosition( rand.nextInt( ( max - min ) + 1 ) + min * j, rand.nextInt( ( max - min ) + 1 ) + min * j );
					// nodes.get( j ).setPosition( 0, 0 );
					directedGraph.addNode( nodes.get( j ) );
				}

				for ( int k = 1; k < nodeNames.size(); k++ )
				{
					edges.add( graphModel.factory().newEdge( nodes.get( 0 ), nodes.get( k ), 0, (double) k * 3, true ) );
					directedGraph.addEdge( edges.get( k - 1 ) );
				}

				Edge e4 = graphModel.factory().newEdge( nodes.get( 1 ), nodes.get( 2 ), 0, 1.0, true );
				Edge e5 = graphModel.factory().newEdge( nodes.get( 1 ), nodes.get( 4 ), 0, 1.0, true );
				directedGraph.addEdge( e4 );
				directedGraph.addEdge( e5 );

				// Count nodes and edges
				System.out.println( "Nodes: " + directedGraph.getNodeCount() + " Edges: " + directedGraph.getEdgeCount() );

				// Filter
				DegreeRangeFilter degreeFilter = new DegreeRangeFilter();
				degreeFilter.init( directedGraph );
				degreeFilter.setRange( new Range( 1, Integer.MAX_VALUE ) ); // Remove
				// nodes
				// with
				// degree
				// < 30
				Query query = filterController.createQuery( degreeFilter );
				GraphView view = filterController.filter( query );
				graphModel.setVisibleView( view ); // Set the filter result as
													// the
				// visible view

				// See visible graph stats
				UndirectedGraph graphVisible = graphModel.getUndirectedGraphVisible();
				System.out.println( "Nodes: " + graphVisible.getNodeCount() );
				System.out.println( "Edges: " + graphVisible.getEdgeCount() );

				// Layout for 1 minute
				AutoLayout autoLayout = new AutoLayout( 10, TimeUnit.SECONDS );
				autoLayout.setGraphModel( graphModel );
				YifanHuLayout firstLayout = new YifanHuLayout( null, new StepDisplacement( 1f ) );
				FruchtermanReingold thirdLayout = new FruchtermanReingold( null );
				AutoLayout.DynamicProperty area = AutoLayout.createDynamicProperty( "fruchtermanReingold.area.name", new Double( 1000. ), 1f );

				autoLayout.addLayout( firstLayout, 0.5f );

				autoLayout.addLayout( thirdLayout, 0.5f, new AutoLayout.DynamicProperty[] { area } );
				autoLayout.execute();

				// Get Centrality
				GraphDistance distance = new GraphDistance();
				distance.setDirected( true );
				distance.execute( graphModel );

				// Rank color by Degree
				Function degreeRanking = appearanceModel.getNodeFunction( directedGraph, AppearanceModel.GraphFunction.NODE_DEGREE, RankingElementColorTransformer.class );
				RankingElementColorTransformer degreeTransformer = (RankingElementColorTransformer) degreeRanking.getTransformer();
				degreeTransformer.setColors( new Color[] { new Color( 0x9990D9 ), new Color( 0xB30000 ) } );
				degreeTransformer.setColorPositions( new float[] { 0f, 1f } );
				appearanceController.transform( degreeRanking );

				// Rank size by centrality/Harmonic Closeness
				Column centralityColumn = graphModel.getNodeTable().getColumn( GraphDistance.HARMONIC_CLOSENESS );
				Function centralityRanking = appearanceModel.getNodeFunction( directedGraph, centralityColumn, RankingNodeSizeTransformer.class );
				RankingNodeSizeTransformer centralityTransformer = (RankingNodeSizeTransformer) centralityRanking.getTransformer();
				centralityTransformer.setMinSize( 3 );
				centralityTransformer.setMaxSize( 10 );
				appearanceController.transform( centralityRanking );

				// Preview
				model.getProperties().putValue( PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE );
				model.getProperties().putValue( PreviewProperty.EDGE_COLOR, new EdgeColor( Color.GRAY ) );
				model.getProperties().putValue( PreviewProperty.EDGE_THICKNESS, new Float( 0.1f ) );
				model.getProperties().putValue( PreviewProperty.NODE_LABEL_FONT, model.getProperties().getFontValue( PreviewProperty.NODE_LABEL_FONT ).deriveFont( 8 ) );

				// Export full graph
				ExportController ec = Lookup.getDefault().lookup( ExportController.class );
				try
				{

					final File f = new File( "src/main/webapp/resources/gexf/co-authors" + rand.nextInt( ( max - min ) + 1 ) + min + ".gexf" );
					ec.exportFile( f );

					filename = f.getName();

					System.out.println( filename );

					// nodes.clear();
					// edges.clear();
					//
					// workspace.remove( nodes );
					// workspace.remove( edges );

					pc.deleteWorkspace( workspace );

					new java.util.Timer().schedule( new java.util.TimerTask()
					{
						@Override
						public void run()
						{
							f.delete();
						}
					}, 60000 );
				}
				catch ( IOException ex )
				{
					System.out.println( ex );
					return;
				}
			}

		};
	}
}
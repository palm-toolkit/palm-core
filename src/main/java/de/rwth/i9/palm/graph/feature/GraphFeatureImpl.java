package de.rwth.i9.palm.graph.feature;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import org.gephi.filters.api.FilterController;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.layout.plugin.fruchterman.FruchtermanReingold;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;

public class GraphFeatureImpl implements GraphFeature
{
	String filename = null;

	@Override
	public Map<String, Object> getGephiGraph( Author author )
	{

		// Create Thread Pool for parallel layout
		ExecutorService executor = Executors.newFixedThreadPool( 1 );

		Future<?> f = executor.submit( createRunnable( author ) );
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

	private Runnable createRunnable( final Author author )
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

				directedGraph.writeLock();

				List<String> authorNames = new ArrayList<String>();

				authorNames.add( author.getName() );

				Set<Publication> authorPublications = author.getPublications();
				Iterator<Publication> publicationIterator = authorPublications.iterator();
				// List<List<Author>> publicationAuthors = new
				// ArrayList<List<Author>>();
				List<Author> coAuthors = new ArrayList<Author>();
				List<Integer> authorCount = new ArrayList<Integer>();
				Map<String, Integer> coAuthorCollaborationCountMap = new HashMap<String, Integer>();

				for ( Publication publication : authorPublications )
				{
					List<Author> temp = publication.getAuthors();
					System.out.println( publication.getTitle() );
					for ( Author a : temp )
					{
						if ( !a.getName().equals( author.getName() ) && !coAuthors.contains( a ) && a.isAdded() )
						{
							coAuthors.add( a );
							System.out.println( a.getId() + " " + a.getName() );

							authorNames.add( a.getName() );

						}
					}
				}

				System.out.println( "t1" );
				Random rand = new Random();

				int max = 40000;
				int min = 0;

				List<String> nodeNames = new ArrayList<String>();
				List<String> edgeNames = new ArrayList<String>();

				System.out.println( "t2" );
				for ( int i = 0; i < authorNames.size(); i++ )
				{
					nodeNames.add( "n" + i );// rand.nextInt( ( max - min ) + 1
												// ) + min );
					edgeNames.add( "e" + i );// rand.nextInt( ( max - min ) + 1
												// ) + min );
				}

				List<Node> nodes = new ArrayList<Node>();
				List<Edge> edges = new ArrayList<Edge>();

				nodes.clear();
				edges.clear();

				System.out.println( "t3" );

				System.out.println( nodeNames.size() );
				System.out.println( edgeNames.size() );

				for ( int j = 0; j < nodeNames.size(); j++ )
				{
					nodes.add( graphModel.factory().newNode( nodeNames.get( j ) ) );

					nodes.get( j ).setLabel( authorNames.get( j ) );
					nodes.get( j ).setSize( j / 3 );
					nodes.get( j ).setPosition( rand.nextInt( ( max - min ) + 1 ) + min * j, rand.nextInt( ( max - min ) + 1 ) + min * j );
					System.out.println( nodes.get( j ).getId().toString() );
					directedGraph.addNode( nodes.get( j ) );
				}

				System.out.println( "t3.2" );

				for ( int k = 1; k < nodeNames.size(); k++ )
				{
					edges.add( graphModel.factory().newEdge( nodes.get( 0 ), nodes.get( k ), 0, (double) k * 3, true ) );
					directedGraph.addEdge( edges.get( k - 1 ) );
					edges.get( k - 1 ).setColor( Color.BLACK );
				}

				System.out.println( "t3.3" );

				// Count nodes and edges
				System.out.println( "Nodes: " + directedGraph.getNodeCount() + " Edges: " + directedGraph.getEdgeCount() );

				// See visible graph stats
				UndirectedGraph graphVisible = graphModel.getUndirectedGraphVisible();
				System.out.println( "Nodes: " + graphVisible.getNodeCount() );
				System.out.println( "Edges: " + graphVisible.getEdgeCount() );

				// Layout for 1 minute
				AutoLayout autoLayout = new AutoLayout( 60, TimeUnit.SECONDS );
				autoLayout.setGraphModel( graphModel );
				ForceAtlasLayout layout = new ForceAtlasLayout( null );

				YifanHuLayout firstLayout = new YifanHuLayout( null, new StepDisplacement( 1f ) );

				AutoLayout.DynamicProperty repulsion = AutoLayout.createDynamicProperty( "forceAtlas.repulsionStrength.name", new Double( 200000.0 ), 1f );
				AutoLayout.DynamicProperty attraction = AutoLayout.createDynamicProperty( "forceAtlas.attractionStrength.name", new Double( 10.0 ), 1f );
				FruchtermanReingold thirdLayout = new FruchtermanReingold( null );
				AutoLayout.DynamicProperty area = AutoLayout.createDynamicProperty( "fruchtermanReingold.area.name", new Double( 1000. ), 1f );

				autoLayout.addLayout( firstLayout, 0.3f );
				autoLayout.addLayout( layout, 0.4f, new AutoLayout.DynamicProperty[] { repulsion, attraction } );
				autoLayout.addLayout( thirdLayout, 0.3f, new AutoLayout.DynamicProperty[] { area } );
				autoLayout.execute();

				System.out.println( "t4" );

				// Rank color by Degree
				Function degreeRanking = appearanceModel.getNodeFunction( directedGraph, AppearanceModel.GraphFunction.NODE_DEGREE, RankingElementColorTransformer.class );
				RankingElementColorTransformer degreeTransformer = (RankingElementColorTransformer) degreeRanking.getTransformer();
				degreeTransformer.setColors( new Color[] { new Color( 0x9990D9 ), new Color( 0xB30000 ) } );
				degreeTransformer.setColorPositions( new float[] { 0f, 1f } );
				appearanceController.transform( degreeRanking );
				System.out.println( "t4.2" );

				// nodes.get( 0 ).setColor( Color.PINK );
				nodes.get( 0 ).setSize( 10f );

				for ( int i = 0; i < edges.size(); i++ )
				{
					edges.get( i ).setColor( Color.BLACK );
				}

				System.out.println( "t4.3" );

				// Preview
				model.getProperties().putValue( PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE );
				model.getProperties().putValue( PreviewProperty.EDGE_COLOR, new EdgeColor( Color.GRAY ) );
				model.getProperties().putValue( PreviewProperty.EDGE_THICKNESS, new Float( 0.1f ) );
				model.getProperties().putValue( PreviewProperty.NODE_LABEL_FONT, model.getProperties().getFontValue( PreviewProperty.NODE_LABEL_FONT ).deriveFont( 8 ) );

				// Export full graph
				ExportController ec = Lookup.getDefault().lookup( ExportController.class );
				try
				{

					System.out.println( "t5" );
					final File f = new File( "src/main/webapp/resources/gexf/co-authors" + rand.nextInt( ( max - min ) + 1 ) + min + ".gexf" );
					ec.exportFile( f );
					filename = f.getName();
					System.out.println( filename );
					pc.deleteWorkspace( workspace );
					directedGraph.writeUnlock();
					System.out.println( "t6" );

					new java.util.Timer().schedule( new java.util.TimerTask()
					{
						@Override
						public void run()
						{
							f.delete();
						}
					}, 240000 );
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
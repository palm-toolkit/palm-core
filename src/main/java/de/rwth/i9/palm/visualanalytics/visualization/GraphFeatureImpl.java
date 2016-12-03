package de.rwth.i9.palm.visualanalytics.visualization;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.appearance.api.Function;
import org.gephi.appearance.plugin.RankingElementColorTransformer;
import org.gephi.appearance.plugin.RankingNodeSizeTransformer;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.statistics.plugin.GraphDistance;
import org.openide.util.Lookup;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;

class Network
{
	public void generateNetwork( final String type, final List<Author> authorList, final Set<Publication> authorPublications, final List<String> idsList, final Author authorForCoAuthors, final List<Author> selectedAuthors )
	{
		System.out.println( "authorLst: " + authorList.size() );
		System.out.println( "selectedAuthors: " + selectedAuthors.size() );
		Random rand = new Random();

		int max = 100;
		int min = 0;

		// Init a project - and therefore a workspace
		final ProjectController pc = Lookup.getDefault().lookup( ProjectController.class );
		pc.newProject();
		final Workspace workspace = pc.newWorkspace( pc.getCurrentProject() );
		pc.openWorkspace( workspace );

		GraphModel graphModel = Lookup.getDefault().lookup( GraphController.class ).getGraphModel();

		graphModel.getNodeTable().addColumn( "isAdded", boolean.class );
		graphModel.getNodeTable().addColumn( "authorId", String.class );
		graphModel.getEdgeTable().addColumn( "sourceAuthorId", String.class );
		graphModel.getEdgeTable().addColumn( "targetAuthorId", String.class );
		graphModel.getEdgeTable().addColumn( "sourceAuthorIsAdded", boolean.class );
		graphModel.getEdgeTable().addColumn( "targetAuthorIsAdded", boolean.class );

		// FilterController filterController =
		// Lookup.getDefault().lookup( FilterController.class );
		AppearanceController appearanceController = Lookup.getDefault().lookup( AppearanceController.class );
		AppearanceModel appearanceModel = appearanceController.getModel();
		// PreviewModel model = Lookup.getDefault().lookup(
		// PreviewController.class ).getModel();

		// Append as an undirected Graph
		UndirectedGraph undirectedGraph = graphModel.getUndirectedGraph();
		undirectedGraph.writeLock();

		System.out.println( "authorPublications: " + authorPublications.size() );

		List<Node> nodes = new ArrayList<Node>();
		List<Edge> edges = new ArrayList<Edge>();
		nodes.clear();
		edges.clear();

		// Set<Publication> pubs = new HashSet<Publication>();
		// if ( authorForCoAuthors != null )
		// {
		// pubs = authorForCoAuthors.getPublications();
		// authorPublications.addAll( pubs );
		// }

		if ( !selectedAuthors.isEmpty() )
		{
			// iterating over all publications of the authors
			for ( Publication publication : authorPublications )
			{
				List<Author> publicationAuthors = publication.getAuthors();
				List<Author> tempPubAuthors = new ArrayList<Author>();

				// iterating over authors of those publications
				for ( Author publicationAuthor : publicationAuthors )
				{
					if ( type.equals( "researcher" ) )
					{
						// 2nd check, if the authors are not co-authors
						// of
						// each other but have common co-authors
						if ( selectedAuthors.contains( publicationAuthor ) || authorList.contains( publicationAuthor ) )
						{
							Node n = graphModel.factory().newNode( publicationAuthor.getName() );

							// add the authors which are not already
							// present
							// in
							// the nodes table
							if ( !nodes.contains( n ) )
							{
								nodes.add( n );
								n.setLabel( publicationAuthor.getName() );
								// if ( pubs.contains( publication ) )
								// n.setSize( 0.5f );
								// else
								n.setSize( 0.1f );
								n.setAttribute( "isAdded", publicationAuthor.isAdded() );
								n.setAttribute( "authorId", publicationAuthor.getId() );
								n.setPosition( rand.nextInt( ( max - min ) + 1 ) + min, rand.nextInt( ( max - min ) + 1 ) + min );
								undirectedGraph.addNode( n );

							}
							for ( int i = 0; i < tempPubAuthors.size(); i++ )
							{
								Node pubAuthorNode = graphModel.factory().newNode( publicationAuthor.getName() );
								if ( !tempPubAuthors.get( i ).equals( publicationAuthor ) )
								{
									Node tempAuthorNode = graphModel.factory().newNode( tempPubAuthors.get( i ).getName() );
									int indexTempNode = nodes.indexOf( tempAuthorNode );
									int indexPubNode = nodes.indexOf( pubAuthorNode );
									Boolean flag = false;

									// check if an edge already exists
									// between the 2 nodes
									for ( Edge eTest : edges )
									{
										if ( ( eTest.getSource().equals( nodes.get( indexTempNode ) ) && eTest.getTarget().equals( nodes.get( indexPubNode ) ) ) || ( eTest.getSource().equals( nodes.get( indexPubNode ) ) && eTest.getTarget().equals( nodes.get( indexTempNode ) ) ) )
										{
											flag = true;
											eTest.setWeight( eTest.getWeight() + 0.1 );
										}
									}
									if ( !flag )
									{

										Edge e = graphModel.factory().newEdge( nodes.get( indexTempNode ), nodes.get( indexPubNode ), 0, 1, false );
										edges.add( e );
										e.setWeight( 0.1 );
										e.setAttribute( "sourceAuthorId", tempPubAuthors.get( i ).getId() );
										e.setAttribute( "targetAuthorId", publicationAuthor.getId() );
										e.setAttribute( "sourceAuthorIsAdded", tempPubAuthors.get( i ).isAdded() );
										e.setAttribute( "targetAuthorIsAdded", publicationAuthor.isAdded() );
										undirectedGraph.addEdge( e );
									}
								}
							}
							tempPubAuthors.add( publicationAuthor );
						}
					}
					if ( type.equals( "conference" ) )
					{
						if ( selectedAuthors.contains( publicationAuthor ) )
						{
							Node n = graphModel.factory().newNode( publicationAuthor.getName() );

							if ( !nodes.contains( n ) )
							{
								nodes.add( n );
								n.setAttribute( "isAdded", publicationAuthor.isAdded() );
								n.setAttribute( "authorId", publicationAuthor.getId() );
								n.setLabel( publicationAuthor.getName() );
								// if ( pubs.contains( publication ) )
								// n.setSize( 0.5f );
								// else
								n.setSize( 0.1f );
								n.setPosition( rand.nextInt( ( max - min ) + 1 ) + min, rand.nextInt( ( max - min ) + 1 ) + min );
								undirectedGraph.addNode( n );
							}
							for ( int i = 0; i < tempPubAuthors.size(); i++ )
							{
								Node pubAuthorNode = graphModel.factory().newNode( publicationAuthor.getName() );
								if ( !tempPubAuthors.get( i ).equals( publicationAuthor ) )
								{
									Node tempAuthorNode = graphModel.factory().newNode( tempPubAuthors.get( i ).getName() );
									int indexTempNode = nodes.indexOf( tempAuthorNode );
									int indexPubNode = nodes.indexOf( pubAuthorNode );
									Boolean flag = false;

									// check if an edge already exists
									// between the 2 nodes
									for ( Edge eTest : edges )
									{
										if ( ( eTest.getSource().equals( nodes.get( indexTempNode ) ) && eTest.getTarget().equals( nodes.get( indexPubNode ) ) ) || ( eTest.getSource().equals( nodes.get( indexPubNode ) ) && eTest.getTarget().equals( nodes.get( indexTempNode ) ) ) )
										{
											flag = true;
											eTest.setWeight( eTest.getWeight() + 0.1 );
										}
									}
									if ( !flag )
									{
										Edge e = graphModel.factory().newEdge( nodes.get( indexTempNode ), nodes.get( indexPubNode ), 0, 1, false );
										edges.add( e );
										e.setWeight( 0.1 );
										e.setAttribute( "sourceAuthorId", tempPubAuthors.get( i ).getId() );
										e.setAttribute( "targetAuthorId", publicationAuthor.getId() );
										e.setAttribute( "sourceAuthorIsAdded", tempPubAuthors.get( i ).isAdded() );
										e.setAttribute( "targetAuthorIsAdded", publicationAuthor.isAdded() );
										undirectedGraph.addEdge( e );
									}

								}
							}
							tempPubAuthors.add( publicationAuthor );
						}
					}
					if ( type.equals( "topic" ) || type.equals( "circle" ) )
					{

						if ( selectedAuthors.contains( publicationAuthor ) )
						{
							// System.out.println( "TA:" +
							// publicationAuthor.getName() );
							Node n = graphModel.factory().newNode( publicationAuthor.getName() );
							// add the authors which are not already
							// present
							// in
							// the nodes table
							if ( !nodes.contains( n ) )
							{
								nodes.add( n );
								n.setLabel( publicationAuthor.getName() );
								// if ( pubs.contains( publication ) )
								// n.setSize( 0.5f );
								// else
								n.setSize( 0.1f );
								n.setAttribute( "isAdded", publicationAuthor.isAdded() );
								n.setAttribute( "authorId", publicationAuthor.getId() );
								n.setPosition( rand.nextInt( ( max - min ) + 1 ) + min, rand.nextInt( ( max - min ) + 1 ) + min );
								undirectedGraph.addNode( n );

							}
							for ( int i = 0; i < tempPubAuthors.size(); i++ )
							{
								Node pubAuthorNode = graphModel.factory().newNode( publicationAuthor.getName() );
								if ( !tempPubAuthors.get( i ).equals( publicationAuthor ) )
								{
									Node tempAuthorNode = graphModel.factory().newNode( tempPubAuthors.get( i ).getName() );
									int indexTempNode = nodes.indexOf( tempAuthorNode );
									int indexPubNode = nodes.indexOf( pubAuthorNode );
									Boolean flag = false;

									// check if an edge already exists
									// between the 2 nodes
									for ( Edge eTest : edges )
									{
										if ( ( eTest.getSource().equals( nodes.get( indexTempNode ) ) && eTest.getTarget().equals( nodes.get( indexPubNode ) ) ) || ( eTest.getSource().equals( nodes.get( indexPubNode ) ) && eTest.getTarget().equals( nodes.get( indexTempNode ) ) ) )
										{
											flag = true;
											eTest.setWeight( eTest.getWeight() + 0.1 );
										}
									}
									if ( !flag )
									{
										Edge e = graphModel.factory().newEdge( nodes.get( indexTempNode ), nodes.get( indexPubNode ), 0, 1, false );
										edges.add( e );
										e.setWeight( 0.1 );
										e.setAttribute( "sourceAuthorId", tempPubAuthors.get( i ).getId() );
										e.setAttribute( "targetAuthorId", publicationAuthor.getId() );
										e.setAttribute( "sourceAuthorIsAdded", tempPubAuthors.get( i ).isAdded() );
										e.setAttribute( "targetAuthorIsAdded", publicationAuthor.isAdded() );
										undirectedGraph.addEdge( e );
									}
								}
							}
							tempPubAuthors.add( publicationAuthor );
						}
					}
				}
			}
		}
		// in case of publications, explicitly find common authors
		if ( type.equals( "publication" ) )
		{
			List<Author> publicationAuthors = selectedAuthors;
			List<Author> tempPubAuthors = new ArrayList<Author>();

			// iterating over authors of those publications
			for ( Author publicationAuthor : publicationAuthors )
			{
				Node n = graphModel.factory().newNode( publicationAuthor.getName() );

				if ( !nodes.contains( n ) )
				{
					nodes.add( n );
					n.setAttribute( "isAdded", publicationAuthor.isAdded() );
					n.setAttribute( "authorId", publicationAuthor.getId() );
					n.setLabel( publicationAuthor.getName() );
					n.setSize( 0.1f );
					n.setPosition( rand.nextInt( ( max - min ) + 1 ) + min, rand.nextInt( ( max - min ) + 1 ) + min );
					undirectedGraph.addNode( n );
				}
				for ( int i = 0; i < tempPubAuthors.size(); i++ )
				{
					Node pubAuthorNode = graphModel.factory().newNode( publicationAuthor.getName() );
					if ( !tempPubAuthors.get( i ).equals( publicationAuthor ) )
					{
						Node tempAuthorNode = graphModel.factory().newNode( tempPubAuthors.get( i ).getName() );
						int indexTempNode = nodes.indexOf( tempAuthorNode );
						int indexPubNode = nodes.indexOf( pubAuthorNode );
						Boolean flag = false;
						// check if an edge already exists
						// between the 2 nodes
						for ( Edge eTest : edges )
						{
							if ( ( eTest.getSource().equals( nodes.get( indexTempNode ) ) && eTest.getTarget().equals( nodes.get( indexPubNode ) ) ) || ( eTest.getSource().equals( nodes.get( indexPubNode ) ) && eTest.getTarget().equals( nodes.get( indexTempNode ) ) ) )
							{
								flag = true;
								eTest.setWeight( eTest.getWeight() + 0.1 );
							}
						}
						if ( !flag )
						{
							Edge e = graphModel.factory().newEdge( nodes.get( indexTempNode ), nodes.get( indexPubNode ), 0, 1, false );
							edges.add( e );
							e.setWeight( 0.1 );
							e.setAttribute( "sourceAuthorId", tempPubAuthors.get( i ).getId() );
							e.setAttribute( "targetAuthorId", publicationAuthor.getId() );
							e.setAttribute( "sourceAuthorIsAdded", tempPubAuthors.get( i ).isAdded() );
							e.setAttribute( "targetAuthorIsAdded", publicationAuthor.isAdded() );
							undirectedGraph.addEdge( e );
						}
					}
				}
				tempPubAuthors.add( publicationAuthor );
			}
		}

		if ( selectedAuthors.isEmpty() )
		{
			System.out.println( "selected authors: empty" );
			// if ( type.equals( "circle" ) && idsList.size() > 1 )
			// {
			// for ( Author a : selectedAuthors )
			// {
			// Node n = graphModel.factory().newNode( a.getName() );
			//
			// if ( !nodes.contains( n ) )
			// {
			// nodes.add( n );
			// n.setAttribute( "isAdded", a.isAdded() );
			// n.setAttribute( "authorId", a.getId() );
			// n.setLabel( a.getName() );
			// n.setSize( 0.1f );
			// n.setPosition( rand.nextInt( ( max - min ) + 1 ) + min,
			// rand.nextInt( ( max - min ) + 1 ) + min );
			// undirectedGraph.addNode( n );
			// }
			// }
			// }

			System.out.println( type );
			System.out.println( authorList.size() );
			System.out.println( idsList.size() );

			// find associations upto 2 levels, if authors are not
			// co-authors
			if ( type.equals( "researcher" ) && authorList.size() == 2 )
			{
				System.out.println( "inside correct condition" );

				// add nodes for main authors
				for ( int f = 0; f < authorList.size(); f++ )
				{
					Node n = graphModel.factory().newNode( authorList.get( f ).getName() );
					nodes.add( n );
					n.setAttribute( "isAdded", authorList.get( f ).isAdded() );
					n.setAttribute( "authorId", authorList.get( f ).getId() );
					n.setLabel( authorList.get( f ).getName() );
					n.setSize( 0.1f );
					n.setPosition( rand.nextInt( ( max - min ) + 1 ) + min, rand.nextInt( ( max - min ) + 1 ) + min );
					undirectedGraph.addNode( n );
				}

				List<List<Author>> coAuthorsList = new ArrayList<List<Author>>();
				List<Author> commonAuthors = new ArrayList<Author>();
				List<Integer> count = new ArrayList<Integer>();
				for ( Author a : authorList )
				{
					List<Author> coAuthors = new ArrayList<Author>();

					for ( Publication p : a.getPublications() )
					{
						for ( Author coA : p.getAuthors() )
						{
							if ( !coAuthors.contains( coA ) )
							{
								coAuthors.add( coA );
							}

						}
					}

					for ( int i = 0; i < coAuthors.size(); i++ )
					{
						if ( !commonAuthors.contains( coAuthors.get( i ) ) )
						{
							commonAuthors.add( coAuthors.get( i ) );
							count.add( 1 );
						}
						else
						{
							int index = coAuthors.indexOf( coAuthors.get( i ) );
							int prevVal = count.get( index );
							count.set( index, prevVal + 1 );
						}
					}
					coAuthorsList.add( coAuthors );
				}

				for ( int i = 0; i < count.size(); i++ )
				{
					if ( count.get( i ) < authorList.size() )
					{
						count.remove( i );
						commonAuthors.remove( i );
						i--;
					}
				}

				for ( int i = 0; i < commonAuthors.size(); i++ )
				{
					Node n = graphModel.factory().newNode( commonAuthors.get( i ).getName() );

					if ( !nodes.contains( n ) )
					{
						nodes.add( n );
						n.setAttribute( "isAdded", commonAuthors.get( i ).isAdded() );
						n.setAttribute( "authorId", commonAuthors.get( i ).getId() );
						n.setLabel( commonAuthors.get( i ).getName() );
						n.setSize( 0.1f );
						n.setPosition( rand.nextInt( ( max - min ) + 1 ) + min, rand.nextInt( ( max - min ) + 1 ) + min );
						undirectedGraph.addNode( n );

						for ( int j = 0; j < authorList.size(); j++ )
						{
							Node tempAuthorNode = graphModel.factory().newNode( authorList.get( j ).getName() );
							int indexTempNode = nodes.indexOf( tempAuthorNode );
							Edge e = graphModel.factory().newEdge( n, nodes.get( indexTempNode ), 0, 1, false );
							edges.add( e );
							e.setAttribute( "sourceAuthorId", commonAuthors.get( i ).getId() );
							e.setAttribute( "targetAuthorId", authorList.get( j ).getId() );
							e.setAttribute( "sourceAuthorIsAdded", commonAuthors.get( i ).isAdded() );
							e.setAttribute( "targetAuthorIsAdded", authorList.get( j ).isAdded() );
							undirectedGraph.addEdge( e );
						}
					}
				}
				if ( commonAuthors.isEmpty() )
				{
					for ( int i = 1; i < coAuthorsList.size(); i++ )
					{
						List<Author> list1 = coAuthorsList.get( i - 1 );
						List<Author> list2 = coAuthorsList.get( i );
						for ( int j = 0; j < list2.size(); j++ )
						{
							for ( Publication p : list2.get( j ).getPublications() )
							{
								for ( Author a : p.getAuthors() )
								{
									if ( list1.contains( a ) )
									{
										Node n = graphModel.factory().newNode( a.getName() );
										if ( !nodes.contains( n ) )
										{
											nodes.add( n );
											n.setAttribute( "isAdded", a.isAdded() );
											n.setAttribute( "authorId", a.getId() );
											n.setLabel( a.getName() );
											n.setSize( 0.1f );
											n.setPosition( rand.nextInt( ( max - min ) + 1 ) + min, rand.nextInt( ( max - min ) + 1 ) + min );
											undirectedGraph.addNode( n );
										}

										Node n2 = graphModel.factory().newNode( list2.get( j ).getName() );

										if ( !nodes.contains( n2 ) )
										{
											nodes.add( n2 );
											n2.setAttribute( "isAdded", list2.get( j ).isAdded() );
											n2.setAttribute( "authorId", list2.get( j ).getId() );
											n2.setLabel( list2.get( j ).getName() );
											n2.setSize( 0.1f );
											n2.setPosition( rand.nextInt( ( max - min ) + 1 ) + min, rand.nextInt( ( max - min ) + 1 ) + min );
											undirectedGraph.addNode( n2 );
										}

										int indexn = nodes.indexOf( n );
										int indexn2 = nodes.indexOf( n2 );

										Edge e = graphModel.factory().newEdge( nodes.get( indexn ), nodes.get( indexn2 ), 0, 1, false );
										edges.add( e );
										e.setWeight( 0.1 );
										e.setAttribute( "sourceAuthorId", a.getId() );
										e.setAttribute( "targetAuthorId", list2.get( j ).getId() );
										e.setAttribute( "sourceAuthorIsAdded", a.isAdded() );
										e.setAttribute( "targetAuthorIsAdded", list2.get( j ).isAdded() );
										undirectedGraph.addEdge( e );

										Node mainNode1 = graphModel.factory().newNode( authorList.get( i - 1 ).getName() );
										Node mainNode2 = graphModel.factory().newNode( authorList.get( i ).getName() );
										int index1 = nodes.indexOf( mainNode1 );
										int index2 = nodes.indexOf( mainNode2 );

										e = graphModel.factory().newEdge( nodes.get( index1 ), nodes.get( indexn ), 0, 1, false );
										edges.add( e );
										e.setWeight( 0.1 );
										e.setAttribute( "sourceAuthorId", authorList.get( i - 1 ).getId() );
										e.setAttribute( "targetAuthorId", a.getId() );
										e.setAttribute( "sourceAuthorIsAdded", authorList.get( i - 1 ).isAdded() );
										e.setAttribute( "targetAuthorIsAdded", a.isAdded() );
										undirectedGraph.addEdge( e );

										e = graphModel.factory().newEdge( nodes.get( index2 ), nodes.get( indexn2 ), 0, 1, false );
										e.setWeight( 0.1 );
										e.setAttribute( "sourceAuthorId", authorList.get( i ).getId() );
										e.setAttribute( "targetAuthorId", list2.get( j ).getId() );
										e.setAttribute( "sourceAuthorIsAdded", authorList.get( i ).isAdded() );
										e.setAttribute( "targetAuthorIsAdded", list2.get( j ).isAdded() );
										edges.add( e );
										undirectedGraph.addEdge( e );
									}
								}
							}
						}
					}
				}
			}
		}

		System.out.println( "nodes size: " + nodes.size() );

		int time = 2;
		if ( !type.equals( "researcher" ) || !type.equals( "circle" ) || nodes.size() > 20 )
			time = 20;
		if ( nodes.size() > 1000 )
			time = 40;

		System.out.println( "time: " + time );
		// Layout for 1 minute
		AutoLayout autoLayout = new AutoLayout( time, TimeUnit.SECONDS );
		autoLayout.setGraphModel( graphModel );
		ForceAtlasLayout forceAtlasLayout = new ForceAtlasLayout( null );

		AutoLayout.DynamicProperty repulsion = AutoLayout.createDynamicProperty( "forceAtlas.repulsionStrength.name", new Double( 200.0 ), 1f );
		AutoLayout.DynamicProperty attraction = AutoLayout.createDynamicProperty( "forceAtlas.attractionStrength.name", new Double( 10.0 ), 1f );
		autoLayout.addLayout( forceAtlasLayout, 1.0f, new AutoLayout.DynamicProperty[] { repulsion, attraction } );
		autoLayout.execute();

		System.out.println( "layout done" );
		// Get Centrality
		GraphDistance distance = new GraphDistance();
		distance.setDirected( false );
		distance.execute( graphModel );

		// Rank color by Degree
		Function degreeRanking = appearanceModel.getNodeFunction( undirectedGraph, AppearanceModel.GraphFunction.NODE_DEGREE, RankingElementColorTransformer.class );
		RankingElementColorTransformer degreeTransformer = (RankingElementColorTransformer) degreeRanking.getTransformer();
		degreeTransformer.setColors( new Color[] { new Color( 0xed8c30 ), new Color( 0xdb1616 ) } );
		degreeTransformer.setColorPositions( new float[] { 0f, 1f } );
		appearanceController.transform( degreeRanking );
		// purple 673888
		if ( nodes.size() > 1 )
		{
			// Rank size by centrality/Harmonic Closeness
			Column centralityColumn = graphModel.getNodeTable().getColumn( GraphDistance.BETWEENNESS );
			Function centralityRanking = appearanceModel.getNodeFunction( undirectedGraph, centralityColumn, RankingNodeSizeTransformer.class );
			if ( centralityRanking != null )
			{
				RankingNodeSizeTransformer centralityTransformer = (RankingNodeSizeTransformer) centralityRanking.getTransformer();
				centralityTransformer.setMinSize( 4 );
				centralityTransformer.setMaxSize( 10 );
				appearanceController.transform( centralityRanking );
			}
		}

		System.out.println( "centrality done" );
		// // Preview
		// model.getProperties().putValue( PreviewProperty.SHOW_NODE_LABELS,
		// Boolean.TRUE );
		// model.getProperties().putValue( PreviewProperty.EDGE_COLOR, new
		// EdgeColor( Color.GRAY ) );
		// model.getProperties().putValue( PreviewProperty.EDGE_THICKNESS, new
		// Float( 0.1f ) );
		// model.getProperties().putValue( PreviewProperty.NODE_LABEL_FONT,
		// model.getProperties().getFontValue( PreviewProperty.NODE_LABEL_FONT
		// ).deriveFont( 8 ) );

		// Export full graph
		ExportController ec = Lookup.getDefault().lookup( ExportController.class );
		try
		{

			File gexfDir = new File( "src/main/webapp/resources/gexf" );
			if ( !gexfDir.exists() )
			{
				try
				{
					gexfDir.mkdir();
				}
				catch ( SecurityException se )
				{
					se.getStackTrace();
				}
			}

			final File f = new File( "src/main/webapp/resources/gexf/co-authors.gexf" );
			ec.exportFile( f );

			System.out.println( "exported" );
			// String graphFile = f.getName();
			pc.deleteWorkspace( workspace );
			undirectedGraph.writeUnlock();
			new java.util.Timer().schedule( new java.util.TimerTask()
			{
				@Override
				public void run()
				{
					f.delete();
				}
			}, 120000 );
		}
		catch ( IOException ex )
		{
			System.out.println( ex );
			return;
		}

	}

}

@Component
public class GraphFeatureImpl implements GraphFeature
{
	Boolean completionFlag = true;
	ExportController ec;

	@Override
	public Map<String, Object> getGephiGraph( String type, List<Author> authorList, Set<Publication> authorPublications, List<String> idsList, Author authorForCoAuthors, List<Author> selectedAuthors, HttpServletRequest request )
	{
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// proceed only if it a part of the current request
		if ( type.equals( request.getSession().getAttribute( "objectType" ) ) && idsList.equals( request.getSession().getAttribute( "idsList" ) ) )
		{
			Network network = new Network();

			// Create Thread Pool for parallel layout
			ExecutorService executor = Executors.newFixedThreadPool( 1 );

			Future<?> f = executor.submit( createRunnable( network, type, authorList, authorPublications, idsList, authorForCoAuthors, selectedAuthors ) );
			try
			{
				f.get();
			}
			catch ( Exception e )
			{
				e.printStackTrace();
			}

			executor.shutdown();

			File file = new File( "src/main/webapp/resources/gexf/co-authors.gexf" );
			System.out.println( "file:" + file.getName() );
			// Random rand = new Random();
			//
			// int max = 10000;
			// int min = 0;
			long time = System.currentTimeMillis();
			final File newFile = new File( "src/main/webapp/resources/gexf/" + time + ".gexf" );
			System.out.println( newFile.getName() );
			// while ( newFile.exists() )
			// {
			// newFile = new File( "src/main/webapp/resources/gexf/co-authors" +
			// rand.nextInt( ( max - min ) + 1 ) + min + ".gexf" );
			// System.out.println( "re " + newFile.getName() );
			// }

			// final File fileToExport = newFile;
			file.renameTo( newFile );
			new java.util.Timer().schedule( new java.util.TimerTask()
			{
				@Override
				public void run()
				{
					newFile.delete();
				}
			}, 3600000 );
			responseMap.put( "graphFile", newFile.getName() );
		}
		return responseMap;

	}

	private Runnable createRunnable( final Network network, final String type, final List<Author> authorList, final Set<Publication> authorPublications, final List<String> idsList, final Author authorForCoAuthors, final List<Author> selectedAuthors )
	{
		return new Runnable()
		{
			@Override
			public void run()
			{
				synchronized (network)
				{
					network.generateNetwork( type, authorList, authorPublications, idsList, authorForCoAuthors, selectedAuthors );
				}
			}
		};
	}
}

package de.rwth.i9.palm.graph.feature;

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

import javax.transaction.Transactional;

import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.appearance.api.Function;
import org.gephi.appearance.plugin.RankingElementColorTransformer;
import org.gephi.filters.api.FilterController;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
@Transactional
public class GraphFeatureImpl implements GraphFeature
{
	String graphFile = null;
	Boolean completionFlag = true;
	ExportController ec;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Override
	public Map<String, Object> getGephiGraph( String type, List<Author> authorList, Set<Publication> authorPublications, List<String> idsList, List<Author> eventGroupAuthors )
	{

		// Create Thread Pool for parallel layout
		ExecutorService executor = Executors.newFixedThreadPool( 1 );
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// if ( authorPublications == null || authorPublications.isEmpty() )
		// {
		// responseMap.put( "count", 0 );
		// return responseMap;
		// }

		Future<?> f = executor.submit( createRunnable( type, authorList, authorPublications, idsList, eventGroupAuthors ) );
		try
		{
			f.get();
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}

		executor.shutdown();

		responseMap.put( "graphFile", graphFile );
		// System.out.println( "response map for gephi: " +
		// responseMap.toString() );
		return responseMap;

	}

	private Runnable createRunnable( final String type, final List<Author> authorList, final Set<Publication> authorPublications, final List<String> idsList, final List<Author> eventGroupAuthors )
	{
		return new Runnable()
		{
			@Override
			public void run()
			{
				Random rand = new Random();

				int max = 100;
				int min = 0;

				// Init a project - and therefore a workspace
				ProjectController pc = Lookup.getDefault().lookup( ProjectController.class );
				pc.newProject();
				Workspace workspace = pc.newWorkspace( pc.getCurrentProject() );
				pc.openWorkspace( workspace );

				GraphModel graphModel = Lookup.getDefault().lookup( GraphController.class ).getGraphModel();

				graphModel.getNodeTable().addColumn( "isAdded", boolean.class );
				graphModel.getNodeTable().addColumn( "authorId", String.class );

				FilterController filterController = Lookup.getDefault().lookup( FilterController.class );
				AppearanceController appearanceController = Lookup.getDefault().lookup( AppearanceController.class );
				AppearanceModel appearanceModel = appearanceController.getModel();
				PreviewModel model = Lookup.getDefault().lookup( PreviewController.class ).getModel();

				// Append as an undirected Graph
				UndirectedGraph undirectedGraph = graphModel.getUndirectedGraph();
				undirectedGraph.writeLock();

				List<Node> nodes = new ArrayList<Node>();
				List<Edge> edges = new ArrayList<Edge>();
				nodes.clear();
				edges.clear();

				// iterating over all publications of the authors
				for ( Publication publication : authorPublications )
				{
					System.out.println( "Pub name: " + publication.getTitle() );
					List<Author> publicationAuthors = publication.getAuthors();
					List<Author> tempPubAuthors = new ArrayList<Author>();

					// iterating over authors of those publications
					for ( Author publicationAuthor : publicationAuthors )
					{
						System.out.println( publicationAuthor.getName() );
						if ( type.equals( "researcher" ) )
						{
							Node n = graphModel.factory().newNode( publicationAuthor.getName() );

							// add the authors which are not already present in
							// the nodes table
							if ( !nodes.contains( n ) )
							{
								nodes.add( n );
								n.setLabel( publicationAuthor.getName() );
								n.setSize( 0.1f );
								n.setAttribute( "isAdded", publicationAuthor.isAdded() );
								n.setAttribute( "authorId", publicationAuthor.getId() );
								n.setPosition( rand.nextInt( ( max - min ) + 1 ) + min, rand.nextInt( ( max - min ) + 1 ) + min );
								undirectedGraph.addNode( n );

							}
							for ( int i = 0; i < tempPubAuthors.size(); i++ )
							{
								Node pubAuthorNode = graphModel.factory().newNode( publicationAuthor.getName() );
								System.out.println( "pub node: " + publicationAuthor.getName() );
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
										undirectedGraph.addEdge( e );
									}

								}
							}
							tempPubAuthors.add( publicationAuthor );
						}
						if ( type.equals( "conference" ) )
						{
							if ( eventGroupAuthors.contains( publicationAuthor ) )
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
									System.out.println( "pub node: " + publicationAuthor.getName() );
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
											undirectedGraph.addEdge( e );
										}

									}
								}
								tempPubAuthors.add( publicationAuthor );
							}
						}

					}
				}

				if ( authorPublications.isEmpty() && type.equals( "researcher" ) && authorList.size() > 1 )
				{
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

					System.out.println( "hai koi common? " + commonAuthors.size() );
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
								System.out.println( "1:" + list2.get( j ).getName() );
								for ( Publication p : list2.get( j ).getPublications() )
								{
									for ( Author a : p.getAuthors() )
									{
										if ( list1.contains( a ) )
										{
											System.out.println( "2:" + a.getName() );
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
												n.setAttribute( "authorId", list2.get( j ).getId() );

												n2.setLabel( list2.get( j ).getName() );
												n2.setSize( 0.1f );
												n2.setPosition( rand.nextInt( ( max - min ) + 1 ) + min, rand.nextInt( ( max - min ) + 1 ) + min );
												undirectedGraph.addNode( n2 );
											}

											int indexn = nodes.indexOf( n );
											int indexn2 = nodes.indexOf( n2 );

											System.out.println( "here" );
											System.out.println( n.getStoreId() );
											System.out.println( n2.getStoreId() );
											Edge e = graphModel.factory().newEdge( nodes.get( indexn ), nodes.get( indexn2 ), 0, 1, false );
											System.out.println( "this" );
											edges.add( e );
											System.out.println( "this2" );
											undirectedGraph.addEdge( e );
											System.out.println( "or here" );
											Node mainNode1 = graphModel.factory().newNode( authorList.get( i - 1 ).getName() );
											Node mainNode2 = graphModel.factory().newNode( authorList.get( i ).getName() );
											int index1 = nodes.indexOf( mainNode1 );
											int index2 = nodes.indexOf( mainNode2 );

											e = graphModel.factory().newEdge( nodes.get( index1 ), nodes.get( indexn ), 0, 1, false );
											edges.add( e );
											undirectedGraph.addEdge( e );

											e = graphModel.factory().newEdge( nodes.get( index2 ), nodes.get( indexn2 ), 0, 1, false );
											edges.add( e );
											undirectedGraph.addEdge( e );

										}
									}
								}
							}

						}
					}
				}

				// Layout for 1 minute
				AutoLayout autoLayout = new AutoLayout( 2, TimeUnit.SECONDS );
				autoLayout.setGraphModel( graphModel );
				ForceAtlasLayout layout = new ForceAtlasLayout( null );

				AutoLayout.DynamicProperty repulsion = AutoLayout.createDynamicProperty( "forceAtlas.repulsionStrength.name", new Double( 200.0 ), 1f );
				AutoLayout.DynamicProperty attraction = AutoLayout.createDynamicProperty( "forceAtlas.attractionStrength.name", new Double( 10.0 ), 1f );
				autoLayout.addLayout( layout, 1f, new AutoLayout.DynamicProperty[] { repulsion, attraction } );
				autoLayout.execute();

				// Rank color by Degree
				Function degreeRanking = appearanceModel.getNodeFunction( undirectedGraph, AppearanceModel.GraphFunction.NODE_DEGREE, RankingElementColorTransformer.class );
				RankingElementColorTransformer degreeTransformer = (RankingElementColorTransformer) degreeRanking.getTransformer();
				degreeTransformer.setColors( new Color[] { new Color( 0x44d5df ), new Color( 0xe53d56 ) } );
				degreeTransformer.setColorPositions( new float[] { 0f, 1f } );
				appearanceController.transform( degreeRanking );
				// purple 673888
				// Preview
				model.getProperties().putValue( PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE );
				model.getProperties().putValue( PreviewProperty.EDGE_COLOR, new EdgeColor( Color.GRAY ) );
				model.getProperties().putValue( PreviewProperty.EDGE_THICKNESS, new Float( 0.1f ) );
				model.getProperties().putValue( PreviewProperty.NODE_LABEL_FONT, model.getProperties().getFontValue( PreviewProperty.NODE_LABEL_FONT ).deriveFont( 8 ) );

				// System.out.println( "after layout" );

				// Export full graph
				ec = Lookup.getDefault().lookup( ExportController.class );
				try
				{
					final File f = new File( "src/main/webapp/resources/gexf/co-authors" + rand.nextInt( ( max - min ) + 1 ) + min + ".gexf" );
					ec.exportFile( f );
					graphFile = f.getName();
					pc.deleteWorkspace( workspace );
					undirectedGraph.writeUnlock();
					// System.out.println( "after exporting file" );
					new java.util.Timer().schedule( new java.util.TimerTask()
					{
						@Override
						public void run()
						{
							f.delete();
						}
					}, 3600000 );
				}
				catch ( IOException ex )
				{
					// System.out.println( ex );
					return;
				}
			}

		};
	}
}
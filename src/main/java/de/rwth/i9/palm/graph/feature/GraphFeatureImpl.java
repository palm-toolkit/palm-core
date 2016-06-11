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
import org.openide.util.Lookup;

import de.rwth.i9.palm.helper.comparator.CoAuthorByNumberOfCollaborationComparator;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;

public class GraphFeatureImpl implements GraphFeature
{
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

		System.out.println( "aithe te chalda!!" );

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
		Workspace workspace = pc.getCurrentWorkspace();

		// Get a graph model - it exists because we have a workspace
		GraphModel graphModel = Lookup.getDefault().lookup( GraphController.class ).getGraphModel( workspace );
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

		List<String> nodeNames = new ArrayList<String>();
		List<String> edgeNames = new ArrayList<String>();
		for ( int i = 0; i < dummyNames.size(); i++ )
		{
			nodeNames.add( "n" + i );
			edgeNames.add( "e" + i );
		}
		Random rand = new Random();

		int max = 300;
		int min = 0;

		List<Node> nodes = new ArrayList<Node>();
		for ( int j = 0; j < nodeNames.size(); j++ )
		{
			nodes.add( graphModel.factory().newNode( nodeNames.get( j ) ) );
			nodes.get( j ).setLabel( dummyNames.get( j ) );
			nodes.get( j ).setPosition( rand.nextInt( ( max - min ) + 1 ) + min * j, rand.nextInt( ( max - min ) + 1 ) + min * j );
			// nodes.get( j ).setPosition( 0, 0 );
			directedGraph.addNode( nodes.get( j ) );
		}

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
			File f = new File( "src/main/webapp/resources/gexf/co-authors.gexf" );

			ec.exportFile( f );

			System.out.println( f.getPath() );
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
		// Gexf gexf = new GexfImpl();
		// Calendar date = Calendar.getInstance();
		//
		// gexf.getMetadata().setLastModified( date.getTime() ).setCreator(
		// "Gephi.org" ).setDescription( "A Web network" );
		//
		// Graph graph = gexf.getGraph();
		// graph.setDefaultEdgeType( EdgeType.UNDIRECTED ).setMode( Mode.DYNAMIC
		// ).setTimeType( TimeFormat.XSDDATETIME );
		//
		// AttributeList attrList = new AttributeListImpl( AttributeClass.NODE
		// );
		// graph.getAttributeLists().add( attrList );
		//
		// Attribute attUrl = attrList.createAttribute( "0",
		// AttributeType.STRING, "url" );
		// Attribute attIndegree = attrList.createAttribute( "1",
		// AttributeType.FLOAT, "indegree" );
		// Attribute attFrog = attrList.createAttribute( "2",
		// AttributeType.BOOLEAN, "frog" ).setDefaultValue( "true" );
		//
		// /** Node Gephi */
		// Node gephi = graph.createNode( "0" );
		// gephi.setLabel( "Gephi" ).getAttributeValues().addValue( attUrl,
		// "http://gephi.org" ).addValue( attIndegree, "1" );
		//
		// Spell spellGephi = new SpellImpl();
		// date.set( 2012, 3, 28, 16, 10, 0 );
		// date.set( Calendar.MILLISECOND, 0 );
		// spellGephi.setStartValue( date.getTime() );
		// gephi.getSpells().add( spellGephi );
		//
		// /** Node Webatlas */
		// Node webatlas = graph.createNode( "1" );
		// webatlas.setLabel( "Webatlas" ).getAttributeValues().addValue(
		// attUrl, "http://webatlas.fr" ).addValue( attIndegree, "2" );
		//
		// Spell spellWebatlas1 = new SpellImpl();
		// date.set( Calendar.MINUTE, 15 );
		// spellWebatlas1.setStartValue( date.getTime() );
		// date.set( 2012, 3, 28, 18, 57, 2 );
		// spellWebatlas1.setEndValue( date.getTime() );
		// webatlas.getSpells().add( spellWebatlas1 );
		//
		// Spell spellWebatlas2 = new SpellImpl();
		// date.set( 2012, 3, 28, 20, 31, 10 );
		// spellWebatlas2.setStartValue( date.getTime() ).setStartIntervalType(
		// IntervalType.OPEN );
		// date.set( Calendar.MINUTE, 45 );
		// date.set( Calendar.SECOND, 21 );
		// spellWebatlas2.setEndValue( date.getTime() );
		// webatlas.getSpells().add( spellWebatlas2 );
		//
		// Spell spellWebatlas3 = new SpellImpl();
		// date.set( 2012, 3, 28, 21, 0, 0 );
		// spellWebatlas3.setStartValue( date.getTime() );
		// date.set( 2012, 4, 11, 10, 49, 27 );
		// spellWebatlas3.setEndValue( date.getTime() ).setEndIntervalType(
		// IntervalType.OPEN );
		// webatlas.getSpells().add( spellWebatlas3 );
		//
		// /** Node RTGI */
		// Node rtgi = graph.createNode( "2" );
		// rtgi.setLabel( "RTGI" ).getAttributeValues().addValue( attUrl,
		// "http://rtgi.fr" ).addValue( attIndegree, "1" );
		//
		// Spell spellRtgi = new SpellImpl();
		// date.set( 2012, 3, 27, 6, 0, 0 );
		// spellRtgi.setStartValue( date.getTime() );
		// date.set( 2012, 4, 19 );
		// spellRtgi.setEndValue( date.getTime() );
		// rtgi.getSpells().add( spellRtgi );
		//
		// /** Node BarabasiLab */
		// Node blab = graph.createNode( "3" );
		// blab.setLabel( "BarabasiLab" ).getAttributeValues().addValue( attUrl,
		// "http://barabasilab.com" ).addValue( attIndegree, "3" ).addValue(
		// attFrog, "false" );
		//
		// /** Node foobar */
		// Node foobar = graph.createNode( "4" );
		// foobar.setLabel( "FooBar" ).getAttributeValues().addValue( attUrl,
		// "http://foo.bar" ).addValue( attIndegree, "1" ).addValue( attFrog,
		// "false" );
		//
		// /** Edge 0 [gephi, webatlas] */
		// Edge edge0 = gephi.connectTo( "0", webatlas );
		//
		// Spell spellEdge0 = new SpellImpl();
		// date.set( 2012, 3, 28, 16, 15, 36 );
		// spellEdge0.setStartValue( date.getTime() );
		// date.set( 2012, 3, 28, 17, 41, 5 );
		// spellEdge0.setEndValue( date.getTime() );
		// edge0.getSpells().add( spellEdge0 );
		//
		// /** Edge 1 [gephi, rtgi] */
		// Edge edge1 = gephi.connectTo( "1", rtgi );
		//
		// Spell spellEdge1 = new SpellImpl();
		// date.set( 2012, 3, 30, 11, 16, 6 );
		// spellEdge1.setStartValue( date.getTime() );
		// date.set( 2012, 4, 3, 11, 52, 6 );
		// spellEdge1.setEndValue( date.getTime() );
		// edge1.getSpells().add( spellEdge1 );
		//
		// /** Edge 2 [rtgi, webatlas] */
		// Edge edge2 = rtgi.connectTo( "2", webatlas );
		// Spell spellEdge2 = new SpellImpl();
		// date.set( 2012, 4, 1, 11, 0, 0 );
		// spellEdge2.setStartValue( date.getTime() ).setStartIntervalType(
		// IntervalType.OPEN );
		// date.set( 2012, 4, 5, 11, 9, 44 );
		// spellEdge2.setEndValue( date.getTime() );
		// edge2.getSpells().add( spellEdge2 );
		//
		// /** Edge 3 [gephi, blab] */
		// Edge edge3 = gephi.connectTo( "3", blab );
		// Spell spellEdge3 = new SpellImpl();
		// date.set( 2012, 3, 30, 12, 13, 22 );
		// spellEdge3.setStartValue( date.getTime() );
		// date.set( Calendar.MINUTE, 58 );
		// date.set( Calendar.SECOND, 24 );
		// spellEdge3.setEndValue( date.getTime() );
		// edge3.getSpells().add( spellEdge3 );
		//
		// /** Edge 4 [webatlas, blab] */
		// Edge edge4 = webatlas.connectTo( "4", blab );
		// Spell spellEdge4 = new SpellImpl();
		// date.set( 2012, 3, 30, 21, 2, 37 );
		// spellEdge4.setStartValue( date.getTime() );
		// date.set( Calendar.MINUTE, 13 );
		// spellEdge4.setEndValue( date.getTime() );
		// edge4.getSpells().add( spellEdge4 );
		//
		// /** Edge 5 [foobar, blab] */
		// foobar.connectTo( "5", blab );
		//
		// StaxGraphWriter graphWriter = new StaxGraphWriter();
		// File f = new File( "dynamic_graph_sample.gexf" );
		// Writer out;
		// try
		// {
		// out = new FileWriter( f, false );
		// graphWriter.writeToStream( gexf, out, "UTF-8" );
		// System.out.println( f.getAbsolutePath() );
		// }
		// catch ( IOException e )
		// {
		// e.printStackTrace();
		// }
	}

}

package de.rwth.i9.palm.visualanalytics.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.visualanalytics.visualization.BubblesVisualization;
import de.rwth.i9.palm.visualanalytics.visualization.BubblesVisualizationImpl;
import de.rwth.i9.palm.visualanalytics.visualization.ComparisonVisualization;
import de.rwth.i9.palm.visualanalytics.visualization.ComparisonVisualizationÌmpl;
import de.rwth.i9.palm.visualanalytics.visualization.EvolutionVisualization;
import de.rwth.i9.palm.visualanalytics.visualization.EvolutionVisualizationImpl;
import de.rwth.i9.palm.visualanalytics.visualization.GroupVisualization;
import de.rwth.i9.palm.visualanalytics.visualization.GroupVisualizationImpl;
import de.rwth.i9.palm.visualanalytics.visualization.ListVisualization;
import de.rwth.i9.palm.visualanalytics.visualization.ListVisualizationImpl;
import de.rwth.i9.palm.visualanalytics.visualization.LocationsVisualization;
import de.rwth.i9.palm.visualanalytics.visualization.LocationsVisualizationImpl;
import de.rwth.i9.palm.visualanalytics.visualization.NetworkVisualization;
import de.rwth.i9.palm.visualanalytics.visualization.NetworkVisualizationImpl;
import de.rwth.i9.palm.visualanalytics.visualization.SimilarityVisualization;
import de.rwth.i9.palm.visualanalytics.visualization.SimilarityVisualizationImpl;
import de.rwth.i9.palm.visualanalytics.visualization.TimelineVisualization;
import de.rwth.i9.palm.visualanalytics.visualization.TimelineVisualizationImpl;

@Component
public class VisualizationFeatureImpl implements VisualizationFeature
{
	@Autowired( required = false )
	private NetworkVisualization networkVisualization;

	@Autowired( required = false )
	private LocationsVisualization locationsVisualization;

	@Autowired( required = false )
	private TimelineVisualization timelineVisualization;

	@Autowired( required = false )
	private EvolutionVisualization evolutionVisualization;

	@Autowired( required = false )
	private BubblesVisualization bubblesVisualization;

	@Autowired( required = false )
	private ListVisualization listVisualization;

	@Autowired( required = false )
	private GroupVisualization groupVisualization;

	@Autowired( required = false )
	private ComparisonVisualization comparisonVisualization;

	@Autowired( required = false )
	private SimilarityVisualization similarityVisualization;

	@Override
	public NetworkVisualization getVisNetwork()
	{
		if ( this.networkVisualization == null )
			this.networkVisualization = new NetworkVisualizationImpl();

		return this.networkVisualization;
	}

	@Override
	public LocationsVisualization getVisLocations()
	{
		if ( this.locationsVisualization == null )
			this.locationsVisualization = new LocationsVisualizationImpl();

		return this.locationsVisualization;
	}

	@Override
	public TimelineVisualization getVisTimeline()
	{
		if ( this.timelineVisualization == null )
			this.timelineVisualization = new TimelineVisualizationImpl();

		return this.timelineVisualization;
	}

	@Override
	public EvolutionVisualization getVisEvolution()
	{
		if ( this.evolutionVisualization == null )
			this.evolutionVisualization = new EvolutionVisualizationImpl();

		return this.evolutionVisualization;
	}

	@Override
	public BubblesVisualization getVisBubbles()
	{
		if ( this.bubblesVisualization == null )
			this.bubblesVisualization = new BubblesVisualizationImpl();

		return this.bubblesVisualization;
	}

	@Override
	public ListVisualization getVisList()
	{
		if ( this.listVisualization == null )
			this.listVisualization = new ListVisualizationImpl();

		return this.listVisualization;
	}

	@Override
	public GroupVisualization getVisGroup()
	{
		if ( this.groupVisualization == null )
			this.groupVisualization = new GroupVisualizationImpl();

		return this.groupVisualization;
	}

	@Override
	public ComparisonVisualization getVisComparison()
	{
		if ( this.comparisonVisualization == null )
			this.comparisonVisualization = new ComparisonVisualizationÌmpl();

		return this.comparisonVisualization;
	}

	@Override
	public SimilarityVisualization getVisSimilar()
	{
		if ( this.similarityVisualization == null )
			this.similarityVisualization = new SimilarityVisualizationImpl();

		return this.similarityVisualization;
	}
}

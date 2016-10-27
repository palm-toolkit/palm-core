package de.rwth.i9.palm.visualanalytics.service;

import de.rwth.i9.palm.visualanalytics.visualization.BubblesVisualization;
import de.rwth.i9.palm.visualanalytics.visualization.ComparisonVisualization;
import de.rwth.i9.palm.visualanalytics.visualization.EvolutionVisualization;
import de.rwth.i9.palm.visualanalytics.visualization.GroupVisualization;
import de.rwth.i9.palm.visualanalytics.visualization.ListVisualization;
import de.rwth.i9.palm.visualanalytics.visualization.LocationsVisualization;
import de.rwth.i9.palm.visualanalytics.visualization.NetworkVisualization;
import de.rwth.i9.palm.visualanalytics.visualization.SimilarityVisualization;
import de.rwth.i9.palm.visualanalytics.visualization.TimelineVisualization;

public interface VisualizationFeature
{
	public NetworkVisualization getVisNetwork();

	public LocationsVisualization getVisLocations();

	public TimelineVisualization getVisTimeline();

	public EvolutionVisualization getVisEvolution();

	public BubblesVisualization getVisBubbles();

	public ListVisualization getVisList();

	public GroupVisualization getVisGroup();

	public ComparisonVisualization getVisComparison();

	public SimilarityVisualization getVisSimilar();

}

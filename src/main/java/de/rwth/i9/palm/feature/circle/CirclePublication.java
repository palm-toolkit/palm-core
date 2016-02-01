package de.rwth.i9.palm.feature.circle;

import java.util.Map;

import de.rwth.i9.palm.model.Circle;

public interface CirclePublication
{
	public Map<String, Object> getCirclePublicationMap( Circle circle );
}

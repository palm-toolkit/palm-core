package de.rwth.i9.palm.feature.circle;

/**
 * Factory interfaces, containing all features on Circle object
 * 
 * @author sigit
 */
public interface CircleFeature
{
	public CircleApi getCircleApi();

	public CircleBasicInformation getCircleBasicInformation();

	public CircleDetail getCircleDetail();

	public CircleManage getCircleManage();

	public CircleSearch getCircleSearch();
}

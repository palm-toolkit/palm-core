package de.rwth.i9.palm.feature.circle;

import org.springframework.beans.factory.annotation.Autowired;

public class CircleFeatureImpl implements CircleFeature
{

	@Autowired( required = false )
	private CircleApi circleApi;

	@Autowired( required = false )
	private CircleBasicInformation circleBasicInformation;

	@Autowired( required = false )
	private CircleDetail circleDetail;

	@Autowired( required = false )
	private CircleManage circleManage;

	@Autowired( required = false )
	private CircleSearch circleSearch;

	@Override
	public CircleApi getCircleApi()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CircleBasicInformation getCircleBasicInformation()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CircleDetail getCircleDetail()
	{
		if ( this.circleDetail == null )
			this.circleDetail = new CircleDetailImpl();

		return this.circleDetail;
	}

	@Override
	public CircleManage getCircleManage()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CircleSearch getCircleSearch()
	{
		if ( this.circleSearch == null )
			this.circleSearch = new CircleSearchImpl();

		return this.circleSearch;
	}

}

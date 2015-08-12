package de.rwth.i9.palm.feature.publication;

import org.springframework.beans.factory.annotation.Autowired;

public class PublicationFeatureImpl implements PublicationFeature
{

	@Autowired( required = false )
	private PublicationBasicStatistic publicationBasicStatistic;

	@Autowired( required = false )
	private PublicationDetail publicationDetail;

	@Autowired( required = false )
	private PublicationSearch publicationSearch;

	@Autowired( required = false )
	private PublicationManage publicationManage;

	@Override
	public PublicationBasicStatistic getPublicationBasicStatistic()
	{
		if ( this.publicationBasicStatistic == null )
			this.publicationBasicStatistic = new PublicationBasicStatisticImpl();

		return this.publicationBasicStatistic;
	}

	@Override
	public PublicationDetail getPublicationDetail()
	{
		if ( this.publicationDetail == null )
			this.publicationDetail = new PublicationDetailImpl();

		return this.publicationDetail;
	}

	@Override
	public PublicationSearch getPublicationSearch()
	{
		if ( this.publicationSearch == null )
			this.publicationSearch = new PublicationSearchImpl();

		return this.publicationSearch;
	}

	@Override
	public PublicationManage getPublicationManage()
	{
		if ( this.publicationManage == null )
			this.publicationManage = new PublicationManageImpl();

		return this.publicationManage;
	}

}

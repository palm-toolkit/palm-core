package de.rwth.i9.palm.feature.academicevent;

import org.springframework.beans.factory.annotation.Autowired;

public class AcademicEventFeatureImpl implements AcademicEventFeature
{
	@Autowired( required = false )
	private EventBasicStatistic eventBasicStatistic;

	@Autowired( required = false )
	private EventMining eventMining;

	@Autowired( required = false )
	private EventPublication eventPublication;

	@Autowired( required = false )
	private EventSearch eventSearch;

	@Override
	public EventBasicStatistic getEventBasicStatistic()
	{
		if ( this.eventBasicStatistic == null )
			this.eventBasicStatistic = new EventBasicStatisticImpl();

		return this.eventBasicStatistic;
	}

	@Override
	public EventMining getEventMining()
	{
		if ( this.eventMining == null )
			this.eventMining = new EventMiningImpl();

		return this.eventMining;
	}

	@Override
	public EventPublication getEventPublication()
	{
		if ( this.eventPublication == null )
			this.eventPublication = new EventPublicationImpl();

		return this.eventPublication;
	}

	@Override
	public EventSearch getEventSearch()
	{
		if ( this.eventSearch == null )
			this.eventSearch = new EventSearchImpl();

		return this.eventSearch;
	}
}

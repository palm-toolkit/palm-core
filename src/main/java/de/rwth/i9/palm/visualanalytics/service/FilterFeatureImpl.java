package de.rwth.i9.palm.visualanalytics.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.visualanalytics.filter.DataForFilter;
import de.rwth.i9.palm.visualanalytics.filter.DataForFilterImpl;
import de.rwth.i9.palm.visualanalytics.filter.FilterHelper;
import de.rwth.i9.palm.visualanalytics.filter.FilterHelperImpl;
import de.rwth.i9.palm.visualanalytics.filter.FilteredData;
import de.rwth.i9.palm.visualanalytics.filter.FilteredDataImpl;

@Component
public class FilterFeatureImpl implements FilterFeature
{
	@Autowired( required = false )
	private DataForFilter dataForFilter;

	@Autowired( required = false )
	private FilterHelper filterHelper;

	@Autowired( required = false )
	private FilteredData filteredData;

	@Override
	public DataForFilter getDataForFilter()
	{
		if ( this.dataForFilter == null )
			this.dataForFilter = new DataForFilterImpl();

		return this.dataForFilter;
	}

	@Override
	public FilterHelper getFilterHelper()
	{
		if ( this.filterHelper == null )
			this.filterHelper = new FilterHelperImpl();

		return this.filterHelper;
	}

	@Override
	public FilteredData getFilteredData()
	{
		if ( this.filteredData == null )
			this.filteredData = new FilteredDataImpl();

		return this.filteredData;
	}

}

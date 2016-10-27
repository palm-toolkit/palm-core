package de.rwth.i9.palm.visualanalytics.service;

import de.rwth.i9.palm.visualanalytics.filter.DataForFilter;
import de.rwth.i9.palm.visualanalytics.filter.FilterHelper;
import de.rwth.i9.palm.visualanalytics.filter.FilteredData;

public interface FilterFeature
{
	public DataForFilter getDataForFilter();

	public FilterHelper getFilterHelper();

	public FilteredData getFilteredData();

}

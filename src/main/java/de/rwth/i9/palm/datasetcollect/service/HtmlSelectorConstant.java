package de.rwth.i9.palm.datasetcollect.service;

public class HtmlSelectorConstant
{
	// ================ Google Scholar ===================
	// first phase get the author list
	public static final String GS_AUTHOR_LIST_CONTAINER = ".gsc_1usr";
	public static final String GS_AUTHOR_LIST_NAME = ".gsc_1usr_name";
	public static final String GS_AUTHOR_LIST_AFFILIATION = ".gsc_1usr_aff";

	// second phase get the publication list
	public static final String GS_PUBLICATION_ROW_LIST = "tr.gsc_a_tr";
	public static final String GS_PUBLICATION_COAUTHOR_AND_VENUE = ".gs_gray";
	public static final String GS_PUBLICATION_NOCITATION = ".gsc_a_c";
	public static final String GS_PUBLICATION_YEAR = ".gsc_a_y";

	// third phase get the publication details
	public static final String GS_PUBLICATION_DETAIL_CONTAINER = "#gs_ccl";
	public static final String GS_PUBLICATION_DETAIL_TITLE = "#gsc_title";
	public static final String GS_PUBLICATION_DETAIL_PDF = ".gsc_title_ggi";
	public static final String GS_PUBLICATION_DETAIL_PROP = ".gs_scl";
	public static final String GS_PUBLICATION_DETAIL_PROP_LABEL = ".gsc_field";
	public static final String GS_PUBLICATION_DETAIL_PROP_VALUE = ".gsc_value";
}

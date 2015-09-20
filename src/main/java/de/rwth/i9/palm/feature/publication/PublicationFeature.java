package de.rwth.i9.palm.feature.publication;

/**
 * Factory interfaces, containing all features on Publication object
 * 
 * @author sigit
 */
public interface PublicationFeature
{
	public PublicationBasicStatistic getPublicationBasicStatistic();

	public PublicationDetail getPublicationDetail();

	public PublicationManage getPublicationManage();

	public PublicationMining getPublicationMining();

	public PublicationSearch getPublicationSearch();
}

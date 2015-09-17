package de.rwth.i9.palm.feature.researcher;

/**
 * Factory interface for features on Researcher
 * 
 * @author sigit
 *
 */
public interface ResearcherFeature
{
	public ResearcherApi getResearcherApi();

	public ResearcherInterest getResearcherInterest();
	
	public ResearcherInterestEvolution getResearcherInterestEvolution();

	public ResearcherPublication getResearcherPublication();

	public ResearcherSearch getResearcherSearch();
}

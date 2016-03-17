package de.rwth.i9.palm.feature.researcher;

import org.springframework.beans.factory.annotation.Autowired;

public class ResearcherFeatureImpl implements ResearcherFeature
{
	@Autowired( required = false )
	private ResearcherAcademicEventTree researcherAcademicEventTree;

	@Autowired( required = false )
	private ResearcherApi researcherApi;

	@Autowired( required = false )
	private ResearcherBasicInformation researcherBasicInformation;

	@Autowired( required = false )
	private ResearcherCoauthor researcherCoauthor;

	@Autowired( required = false )
	private ResearcherInterest researcherInterest;
	
	@Autowired( required = false )
	private ResearcherInterestEvolution researcherInterestEvolution;
	
	@Autowired( required = false )
	private ResearcherMining researcherMining;

	@Autowired( required = false )
	private ResearcherPublication researcherPublication;

	@Autowired( required = false )
	private ResearcherSearch researcherSearch;

	@Autowired( required = false )
	private ResearcherTopPublication researcherTopPublication;

	@Autowired( required = false )
	private ResearcherTopicModeling researcherTopicModeling;

	@Override
	public ResearcherAcademicEventTree getResearcherAcademicEventTree()
	{
		if ( this.researcherAcademicEventTree == null )
			this.researcherAcademicEventTree = new ResearcherAcademicEventTreeImpl();

		return this.researcherAcademicEventTree;
	}

	@Override
	public ResearcherApi getResearcherApi()
	{
		if ( this.researcherApi == null )
			this.researcherApi = new ResearcherApiImpl();

		return this.researcherApi;
	}

	@Override
	public ResearcherBasicInformation getResearcherBasicInformation()
	{
		if ( this.researcherBasicInformation == null )
			this.researcherBasicInformation = new ResearcherBasicInformationImpl();

		return this.researcherBasicInformation;
	}

	@Override
	public ResearcherCoauthor getResearcherCoauthor()
	{
		if ( this.researcherCoauthor == null )
			this.researcherCoauthor = new ResearcherCoauthorImpl();

		return this.researcherCoauthor;
	}

	@Override
	public ResearcherInterest getResearcherInterest()
	{
		if( this.researcherInterest == null )
			this.researcherInterest = new ResearcherInterestImpl();

		return this.researcherInterest;
	}

	@Override
	public ResearcherInterestEvolution getResearcherInterestEvolution()
	{
		if( this.researcherInterestEvolution == null )
			this.researcherInterestEvolution = new ResearcherInterestEvolutionImpl();

		return this.researcherInterestEvolution;
	}

	@Override
	public ResearcherMining getResearcherMining()
	{
		if ( this.researcherMining == null )
			this.researcherMining = new ResearcherMiningImpl();

		return this.researcherMining;
	}

	@Override
	public ResearcherPublication getResearcherPublication()
	{
		if ( this.researcherPublication == null )
			this.researcherPublication = new ResearcherPublicationImpl();

		return this.researcherPublication;
	}

	@Override
	public ResearcherSearch getResearcherSearch()
	{
		if ( this.researcherSearch == null )
			this.researcherSearch = new ResearcherSearchImpl();

		return this.researcherSearch;
	}

	@Override
	public ResearcherTopPublication getResearcherTopPublication()
	{
		if ( this.researcherTopPublication == null )
			this.researcherTopPublication = new ResearcherTopPublicationImpl();

		return this.researcherTopPublication;
	}

	@Override
	public ResearcherTopicModeling getResearcherTopicModeling()
	{
		if ( this.researcherTopicModeling == null )
			this.researcherTopicModeling = new ResearcherTopicModelingImpl();

		return this.researcherTopicModeling;
	}

}

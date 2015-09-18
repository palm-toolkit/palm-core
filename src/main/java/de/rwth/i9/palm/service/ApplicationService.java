package de.rwth.i9.palm.service;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import de.rwth.i9.palm.model.ExtractionService;
import de.rwth.i9.palm.model.InterestProfile;
import de.rwth.i9.palm.model.Source;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Service
public class ApplicationService
{
	private final static Logger log = LoggerFactory.getLogger( ApplicationService.class );

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired( required = false )
	private FreeMarkerConfigurer freemarkerConfiguration;

	// caching the sources
	private Map<String, Source> academicNetworkSourcesCache;

	// caching extraction services
	private Map<String, ExtractionService> extractionServiceCache;

	// caching interest profiles
	private Map<String, InterestProfile> interestProfileCache;

	@PostConstruct
	public void init()
	{
		log.info( "Initializing..." );

		// this.getAcademicNetworkSources();

		if ( freemarkerConfiguration != null )
		{
			log.info( "freemarkerConfiguration" );
		}
	}

	/* Source cache */
	@Transactional
	public Map<String, Source> getAcademicNetworkSources()
	{
		if ( academicNetworkSourcesCache == null || academicNetworkSourcesCache.isEmpty() )
			updateAcademicNetworkSourcesCache();
		return academicNetworkSourcesCache;
	}

	@Transactional
	public void updateAcademicNetworkSourcesCache()
	{
		academicNetworkSourcesCache = persistenceStrategy.getSourceDAO().getSourceMap();
	}

	/* extraction services cache */
	@Transactional
	public Map<String, ExtractionService> getExtractionServices()
	{
		if ( extractionServiceCache == null || extractionServiceCache.isEmpty() )
			updateExtractionServicesCache();
		return extractionServiceCache;
	}

	@Transactional
	public void updateExtractionServicesCache()
	{
		extractionServiceCache = persistenceStrategy.getExtractionServiceDAO().getExtractionServiceMap();
	}

	/* interest profile cache */
	@Transactional
	public Map<String, InterestProfile> getInterestProfiles()
	{
		if ( interestProfileCache == null || interestProfileCache.isEmpty() )
			updateInterestProfilesCache();
		return interestProfileCache;
	}

	@Transactional
	public void updateInterestProfilesCache()
	{
		interestProfileCache = persistenceStrategy.getInterestProfileDAO().getInterestProfileMap();
	}

}

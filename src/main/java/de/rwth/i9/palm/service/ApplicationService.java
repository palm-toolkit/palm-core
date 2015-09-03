package de.rwth.i9.palm.service;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Service
public class ApplicationService
{
	private final static Logger log = LoggerFactory.getLogger( ApplicationService.class );

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired( required = false )
	private FreeMarkerConfigurer freemarkerConfiguration;

	@PostConstruct
	public void init()
	{
		log.info( "Initializing..." );

		if ( freemarkerConfiguration != null )
		{
			log.info( "freemarkerConfiguration" );
		}
	}
}

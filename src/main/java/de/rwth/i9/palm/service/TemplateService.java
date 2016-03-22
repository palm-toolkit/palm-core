package de.rwth.i9.palm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.persistence.PersistenceStrategy;

/**
 * Service class which should provide methods for any affairs between views and
 * back end
 * 
 * @author sigit
 */
@Service
public class TemplateService
{
	private final Logger LOGGER = LoggerFactory.getLogger( TemplateService.class );

	@Autowired
	private PersistenceStrategy persistenceStrategy;
}

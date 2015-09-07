package de.rwth.i9.palm.interestmining.service;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Service
public class WordFreqProfile
{
	final Logger logger = Logger.getLogger( WordFreqProfile.class );
	// final private String PROFILENAME = "cvalue";

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private PalmAnalytics palmAnalytics;

}

package de.rwth.i9.palm.service;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.helper.RequestContextHelper;
import de.rwth.i9.palm.model.SessionDataSet;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Service
public class ApplicationContextService
{
	@Autowired
	private SecurityService securityService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	/* THIS CODE IS UNUSED AND SUBJECT TO BE DELETED */
	public SessionDataSet getCurrentSessionDataSet()
	{
		// first, get sessionDataSet from request context
		SessionDataSet sessionDataSet = RequestContextHelper.getSessionAttribute( "sessionDataSet" );

		if ( sessionDataSet != null && !StringUtils.isEmpty( sessionDataSet.getSessionName() ) )
			return sessionDataSet;

		// then, get sessionDataSet from database
		sessionDataSet = this.persistenceStrategy.getSessionDataSetDAO().getByUsername( securityService.getUsername() );

		if ( sessionDataSet != null )
			return sessionDataSet;

		sessionDataSet = new SessionDataSet();

		// set username for sessionDataSet
		if ( securityService.isUserAuthenticated() )
			sessionDataSet.setUserName( securityService.getUsername() );

		// finally, create new sessionDataSet object
		return sessionDataSet;
	}

	public void setCurrentSessionDataSet( final SessionDataSet sessionDataSet )
	{
		// set username for sessionDataSet
		if ( sessionDataSet.getUserName() == null )
			if ( securityService.isUserAuthenticated() )
				sessionDataSet.setUserName( securityService.getUsername() );

		RequestContextHelper.setSessionAttribute( "sessionDataSet", sessionDataSet );
	}
}
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

	/**
	 * Tries to obtain the sessionDataSet-object from different sources: <br>
	 * <br>
	 * 1) from the session. This assumes the sessionDataSet-object to be a
	 * session-scoped object. If there is no sessionDataSet-object saved in the
	 * session, it is tried to obtain it <br>
	 * 2) from the property files. <br>
	 * <br>
	 * If there is no sessionDataSet-object saved for the user, a new
	 * sessionDataSet-object is finally created.
	 * 
	 * @return A not null sessionDataSet-object.
	 */
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

	/**
	 * Saves the given <i>sessionDataSet</i> in the session. If the
	 * sessionDataSet has no username assigned, the username is obtained.
	 *
	 * @param sessionDataSet
	 */
	public void setCurrentSessionDataSet( final SessionDataSet sessionDataSet )
	{
		// set username for sessionDataSet
		if ( sessionDataSet.getUserName() == null )
			if ( securityService.isUserAuthenticated() )
				sessionDataSet.setUserName( securityService.getUsername() );

		RequestContextHelper.setSessionAttribute( "sessionDataSet", sessionDataSet );
	}
}

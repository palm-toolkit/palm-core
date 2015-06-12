package de.rwth.i9.palm.controller;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.transaction.annotation.Transactional;

import de.rwth.i9.palm.helper.RequestContextHelper;
import de.rwth.i9.palm.model.SessionDataSet;
import de.rwth.i9.palm.model.User;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler
{
	private static final Logger log = LoggerFactory.getLogger( LoginSuccessHandler.class );

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	public LoginSuccessHandler( final String defaultTargetUrl )
	{
		this.setDefaultTargetUrl( defaultTargetUrl );
	}

	@Override
	@Transactional
	public void onAuthenticationSuccess( final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication ) throws IOException, ServletException
	{
		String test = authentication.getName();
		User user = persistenceStrategy.getUserDAO().getByUsername( authentication.getName() );

		if ( user == null )
			log.warn( "John, I couldn't save the user's login time, because.. somehow.. he is not there" );
		else
		{
			// ERROR
//			String sessionId = RequestContextHelper.getSessionId();
//			user.setSessionId( sessionId );
//
//			log.info( "USER_LOGIN | {} | {}", user.getUsername(), user.getSessionId() );
//			log.debug( "USER_LOGIN_TIME | {} | {} | {}", user.getUsername(), user.getSessionId(), DateTime.now().toDate() );
//
//			persistenceStrategy.getUserDAO().touch( user );
		}

		initializeSessionAttributes( request );
		redirectOnSuccess( request, response, authentication, user );

	}

	/**
	 * @param request
	 */
	private void initializeSessionAttributes( final HttpServletRequest request )
	{
		// TODO in future load from database?

		RequestContextHelper.setSessionAttribute( "sessionDataSet", new SessionDataSet() );
	}

	/**
	 * Redirects the user to the originally requested url.
	 * 
	 * @param request
	 * @param response
	 * @param authentication
	 * @throws IOException
	 * @throws ServletException
	 */
	private void redirectOnSuccess( final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication, final User user ) throws IOException, ServletException
	{
		SavedRequest savedReq = (SavedRequest) RequestContextHelper.getSessionAttribute( "SPRING_SECURITY_SAVED_REQUEST" );

		if ( savedReq == null )
			super.onAuthenticationSuccess( request, response, authentication );
		else
		{
			log.info( "USER_REDIRECT | {} | to original {}", user.getSessionId(), savedReq.getRedirectUrl() );
			response.sendRedirect( savedReq.getRedirectUrl() );
		}
	}
}
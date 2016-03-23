package de.rwth.i9.palm.feature.user;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.helper.comparator.UserAuthorBookmarkByDateComparator;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.User;
import de.rwth.i9.palm.model.UserAuthorBookmark;
import de.rwth.i9.palm.model.UserCircleBookmark;
import de.rwth.i9.palm.model.UserEventGroupBookmark;
import de.rwth.i9.palm.model.UserPublicationBookmark;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class UserBookmarkImpl implements UserBookmark
{
	private final static Logger log = LoggerFactory.getLogger( UserBookmarkImpl.class );

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, Object> getUserBookmark( String bookmarkType, User user )
	{
		// set response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		// get researcher bookmark
		if ( bookmarkType.equals( "author" ) )
		{
			if ( user.getUserAuthorBookmarks() != null && !user.getUserAuthorBookmarks().isEmpty() )
			{
				List<Object> responseListAuthor = new ArrayList<Object>();

				List<UserAuthorBookmark> userAuthorBookmarks = new ArrayList<UserAuthorBookmark>();
				userAuthorBookmarks.addAll( user.getUserAuthorBookmarks() );
				Collections.sort( userAuthorBookmarks, new UserAuthorBookmarkByDateComparator() );

				for ( UserAuthorBookmark userAuthorBookmark : userAuthorBookmarks )
				{
					printJSONAuthorBookmark( responseListAuthor, userAuthorBookmark.getAuthor() );
				}
				responseMap.put( "researchers", responseListAuthor );
				responseMap.put( "count", responseListAuthor.size() );
			}
		}

		// get publication bookmark
		else if ( bookmarkType.equals( "publication" ) )
		{
			if ( user.getUserPublicationBookmarks() != null && !user.getUserPublicationBookmarks().isEmpty() )
			{
				for ( UserPublicationBookmark userPublicationBookmark : user.getUserPublicationBookmarks() )
				{
					printJSONPublicationBookmark( responseMap, userPublicationBookmark.getPublication() );
				}
			}
		}

		// get circle bookmark
		else if ( bookmarkType.equals( "circle" ) )
		{
			if ( user.getUserCircleBookmarks() != null && !user.getUserCircleBookmarks().isEmpty() )
			{
				for ( UserCircleBookmark userCircleBookmark : user.getUserCircleBookmarks() )
				{
					printJSONCircleBookmark( responseMap, userCircleBookmark.getCircle() );
				}
			}
		}

		// get eventGroup bookmark
		else if ( bookmarkType.equals( "eventGroup" ) )
		{
			if ( user.getUserEventGroupBookmarks() != null && !user.getUserEventGroupBookmarks().isEmpty() )
			{
				for ( UserEventGroupBookmark userEventGroupBookmark : user.getUserEventGroupBookmarks() )
				{
					printJSONEventGroupBookmark( responseMap, userEventGroupBookmark.getEventGroup() );
				}
			}
		}

		responseMap.put( "status", "ok" );
		return responseMap;
	}

	private void printJSONAuthorBookmark( List<Object> responseListAuthor, Author researcher )
	{
		Map<String, Object> researcherMap = new LinkedHashMap<String, Object>();
		researcherMap.put( "id", researcher.getId() );
		researcherMap.put( "name", WordUtils.capitalize( researcher.getName() ) );
		if ( researcher.getPhotoUrl() != null )
			researcherMap.put( "photo", researcher.getPhotoUrl() );
		if ( researcher.getAcademicStatus() != null )
			researcherMap.put( "status", researcher.getAcademicStatus() );
		if ( researcher.getInstitution() != null )
			researcherMap.put( "aff", researcher.getInstitution().getName() );
		if ( researcher.getCitedBy() > 0 )
			researcherMap.put( "citedBy", Integer.toString( researcher.getCitedBy() ) );

		if ( researcher.getPublicationAuthors() != null )
			researcherMap.put( "publicationsNumber", researcher.getPublicationAuthors().size() );
		else
			researcherMap.put( "publicationsNumber", 0 );
		String otherDetail = "";
		if ( researcher.getOtherDetail() != null )
			otherDetail += researcher.getOtherDetail();
		if ( researcher.getDepartment() != null )
			otherDetail += ", " + researcher.getDepartment();
		if ( !otherDetail.equals( "" ) )
			researcherMap.put( "detail", otherDetail );

		researcherMap.put( "isAdded", researcher.isAdded() );

		responseListAuthor.add( researcherMap );
	}

	private void printJSONPublicationBookmark( Map<String, Object> responseMap, Publication publication )
	{
		// TODO Auto-generated method stub

	}

	private void printJSONCircleBookmark( Map<String, Object> responseMap, Circle circle )
	{
		// TODO Auto-generated method stub

	}

	private void printJSONEventGroupBookmark( Map<String, Object> responseMap, EventGroup eventGroup )
	{
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, Object> addUserBookmark( String bookmarkType, String userId, String bookId )
	{
		// set response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		User user = persistenceStrategy.getUserDAO().getById( userId );

		if ( user == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "error - user not found" );
			return responseMap;
		}

		// saving researcher bookmark
		if ( bookmarkType.equals( "author" ) )
		{
			Author author = persistenceStrategy.getAuthorDAO().getById( bookId );

			if ( author == null )
			{
				responseMap.put( "status", "error" );
				responseMap.put( "statusMessage", "error - author not found" );
				return responseMap;
			}
			UserAuthorBookmark userAuthorBookmark = new UserAuthorBookmark();
			userAuthorBookmark.setUser( user );
			userAuthorBookmark.setAuthor( author );

			// get current timestamp
			java.util.Date date = new java.util.Date();
			Timestamp currentTimestamp = new Timestamp( date.getTime() );

			userAuthorBookmark.setBookedDate( currentTimestamp );

			user.addUserAuthorBookmark( userAuthorBookmark );
			persistenceStrategy.getUserDAO().persist( user );
		}

		// saving publication bookmark
		else if ( bookmarkType.equals( "publication" ) )
		{
			Publication publication = persistenceStrategy.getPublicationDAO().getById( bookId );

			if ( publication == null )
			{
				responseMap.put( "status", "error" );
				responseMap.put( "statusMessage", "error - publication not found" );
				return responseMap;
			}
			UserPublicationBookmark userPublicationBookmark = new UserPublicationBookmark();
			userPublicationBookmark.setUser( user );
			userPublicationBookmark.setPublication( publication );

			// get current timestamp
			java.util.Date date = new java.util.Date();
			Timestamp currentTimestamp = new Timestamp( date.getTime() );

			userPublicationBookmark.setBookedDate( currentTimestamp );

			user.addUserPublicationBookmark( userPublicationBookmark );
			persistenceStrategy.getUserDAO().persist( user );
		}

		// saving circle bookmark
		else if ( bookmarkType.equals( "circle" ) )
		{
			Circle circle = persistenceStrategy.getCircleDAO().getById( bookId );

			if ( circle == null )
			{
				responseMap.put( "status", "error" );
				responseMap.put( "statusMessage", "error - circle not found" );
				return responseMap;
			}
			UserCircleBookmark userCircleBookmark = new UserCircleBookmark();
			userCircleBookmark.setUser( user );
			userCircleBookmark.setCircle( circle );

			// get current timestamp
			java.util.Date date = new java.util.Date();
			Timestamp currentTimestamp = new Timestamp( date.getTime() );

			userCircleBookmark.setBookedDate( currentTimestamp );

			user.addUserCircleBookmark( userCircleBookmark );
			persistenceStrategy.getUserDAO().persist( user );
		}

		// saving eventGroup bookmark
		else if ( bookmarkType.equals( "eventGroup" ) )
		{
			EventGroup eventGroup = persistenceStrategy.getEventGroupDAO().getById( bookId );

			if ( eventGroup == null )
			{
				responseMap.put( "status", "error" );
				responseMap.put( "statusMessage", "error - eventGroup not found" );
				return responseMap;
			}
			UserEventGroupBookmark userEventGroupBookmark = new UserEventGroupBookmark();
			userEventGroupBookmark.setUser( user );
			userEventGroupBookmark.setEventGroup( eventGroup );

			// get current timestamp
			java.util.Date date = new java.util.Date();
			Timestamp currentTimestamp = new Timestamp( date.getTime() );

			userEventGroupBookmark.setBookedDate( currentTimestamp );

			user.addUserEventGroupBookmark( userEventGroupBookmark );
			persistenceStrategy.getUserDAO().persist( user );
		}

		responseMap.put( "status", "ok" );
		return responseMap;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, Object> removeUserBookmark( String bookmarkType, String userId, String bookId )
	{
		// set response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		User user = persistenceStrategy.getUserDAO().getById( userId );

		if ( user == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "error - user not found" );
			return responseMap;
		}

		// remove bookmark researcher
		if ( bookmarkType.equals( "author" ) )
		{
			Author author = persistenceStrategy.getAuthorDAO().getById( bookId );

			if ( author == null )
			{
				responseMap.put( "status", "error" );
				responseMap.put( "statusMessage", "error - author not found" );
				return responseMap;
			}

			// get object and remove link
			UserAuthorBookmark userAuthorBookmark = persistenceStrategy.getUserAuthorBookmarkDAO().getByUserAndAuthor( user, author );
			user.removeUserAuthorBookmark( userAuthorBookmark );
			userAuthorBookmark.setUser( null );
			userAuthorBookmark.setAuthor( null );

			// update and store objects
			persistenceStrategy.getUserAuthorBookmarkDAO().delete( userAuthorBookmark );
			persistenceStrategy.getUserDAO().persist( user );
		}

		// remove bookmark publication
		else if ( bookmarkType.equals( "publication" ) )
		{
			Publication publication = persistenceStrategy.getPublicationDAO().getById( bookId );

			if ( publication == null )
			{
				responseMap.put( "status", "error" );
				responseMap.put( "statusMessage", "error - publication not found" );
				return responseMap;
			}

			// get object and remove link
			UserPublicationBookmark userPublicationBookmark = persistenceStrategy.getUserPublicationBookmarkDAO().getByUserAndPublication( user, publication );
			user.removeUserPublicationBookmark( userPublicationBookmark );
			userPublicationBookmark.setUser( null );
			userPublicationBookmark.setPublication( null );

			// update and store objects
			persistenceStrategy.getUserPublicationBookmarkDAO().delete( userPublicationBookmark );
			persistenceStrategy.getUserDAO().persist( user );
		}

		// remove bookmark circle
		else if ( bookmarkType.equals( "circle" ) )
		{
			Circle circle = persistenceStrategy.getCircleDAO().getById( bookId );

			if ( circle == null )
			{
				responseMap.put( "status", "error" );
				responseMap.put( "statusMessage", "error - circle not found" );
				return responseMap;
			}

			// get object and remove link
			UserCircleBookmark userCircleBookmark = persistenceStrategy.getUserCircleBookmarkDAO().getByUserAndCircle( user, circle );
			user.removeUserCircleBookmark( userCircleBookmark );
			userCircleBookmark.setUser( null );
			userCircleBookmark.setCircle( null );

			// update and store objects
			persistenceStrategy.getUserCircleBookmarkDAO().delete( userCircleBookmark );
			persistenceStrategy.getUserDAO().persist( user );
		}

		// remove bookmark eventGroup
		if ( bookmarkType.equals( "eventGroup" ) )
		{
			EventGroup eventGroup = persistenceStrategy.getEventGroupDAO().getById( bookId );

			if ( eventGroup == null )
			{
				responseMap.put( "status", "error" );
				responseMap.put( "statusMessage", "error - eventGroup not found" );
				return responseMap;
			}

			// get object and remove link
			UserEventGroupBookmark userEventGroupBookmark = persistenceStrategy.getUserEventGroupBookmarkDAO().getByUserAndEventGroup( user, eventGroup );
			user.removeUserEventGroupBookmark( userEventGroupBookmark );
			userEventGroupBookmark.setUser( null );
			userEventGroupBookmark.setEventGroup( null );

			// update and store objects
			persistenceStrategy.getUserEventGroupBookmarkDAO().delete( userEventGroupBookmark );
			persistenceStrategy.getUserDAO().persist( user );
		}

		responseMap.put( "status", "ok" );
		return responseMap;
	}

}

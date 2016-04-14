package de.rwth.i9.palm.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Institution;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.SecurityService;

@Controller
@SessionAttributes( "author" )
@RequestMapping( value = "/researcher" )
public class ManageResearcherController
{
	private static final String LINK_NAME = "researcher";

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private SecurityService securityService;

	/**
	 * Load the add publication form together with publication object
	 * 
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/add", method = RequestMethod.GET )
	public ModelAndView addNewAuthor( 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "name", required = false ) final String name,
			final HttpServletResponse response) throws InterruptedException
	{
		ModelAndView model = null;

		if ( securityService.getUser() == null )
		{
			model = TemplateHelper.createViewWithLink( "401", "error" );
			return model;
		}

		model = TemplateHelper.createViewWithLink( "dialogIframeLayout", LINK_NAME );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.RESEARCHER, "add" );

		// create blank Author
		Author author = null;
		if ( id != null )
			author = persistenceStrategy.getAuthorDAO().getById( id );

		if ( author == null )
			author = new Author();

		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "author", author );

		return model;
	}

	/**
	 * Save changes from Add researcher detail, via Spring binding
	 * 
	 * @param extractionServiceListWrapper
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/add", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> saveNewAuthor( 
			@ModelAttribute( "author" ) Author author, 
			HttpServletRequest request, HttpServletResponse response)
	{

		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		if ( author == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "failed to save, expired session" );
			return responseMap;
		}

		Author newAuthor = null;

		if ( author.getTempId() != null && !author.getTempId().equals( "" ) )
		{
			// user select author that available form autocomplete

			@SuppressWarnings( "unchecked" )
			List<Author> sessionAuthors = (List<Author>) request.getSession().getAttribute( "authors" );

			// get author from session
			if ( sessionAuthors != null && !sessionAuthors.isEmpty() )
			{
				for ( Author sessionAuthor : sessionAuthors )
				{
					if ( sessionAuthor.getId().equals( author.getTempId() ) )
					{
						persistenceStrategy.getAuthorDAO().persist( sessionAuthor );

						newAuthor = persistenceStrategy.getAuthorDAO().getById( author.getTempId() );
						break;
					}
				}
			}
		}
		else
		{
			// user create new author not suggested by system
			newAuthor = new Author();
		}

		// if there is something wrong with the session
		if ( newAuthor == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "error on session - author not found on session" );

			return responseMap;
		}

		// set based on user input
		newAuthor.setPossibleNames( author.getName() );
		if ( !author.getAcademicStatus().equals( "" ) )
			newAuthor.setAcademicStatus( author.getAcademicStatus() );
		if ( author.getPhotoUrl().startsWith( "http:" ) )
			newAuthor.setPhotoUrl( author.getPhotoUrl() );
		newAuthor.setAdded( true );

		if ( author.getInstitution() == null && !author.getAffiliation().equals( "" ) )
		{
			Institution institution = null;
			List<Institution> institutions = persistenceStrategy.getInstitutionDAO().getByName( author.getAffiliation() );
			if ( !institutions.isEmpty() )
				institution = institutions.get( 0 );
			else
			{
				institution = new Institution();
				institution.setName( author.getAffiliation() );
			}
			newAuthor.setInstitution( institution );
		}
		persistenceStrategy.getAuthorDAO().persist( newAuthor );

		responseMap.put( "status", "ok" );
		responseMap.put( "statusMessage", "author saved" );

		Map<String, String> authorMap = new LinkedHashMap<String, String>();
		authorMap.put( "id", newAuthor.getId() );
		authorMap.put( "name", newAuthor.getName() );
		authorMap.put( "position", newAuthor.getAcademicStatus() );
		responseMap.put( "author", authorMap );

		return responseMap;
	}

	
	/**
	 * Load researcher edit form together with researcher/author object
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/edit", method = RequestMethod.GET )
	public ModelAndView editAuthor( 
			@RequestParam( value = "id") final String id,
			final HttpServletResponse response) throws InterruptedException
	{
		ModelAndView model = null;

		if ( securityService.getUser() == null )
		{
			model = TemplateHelper.createViewWithLink( "401", "error" );
			return model;
		}

		model = TemplateHelper.createViewWithLink( "dialogIframeLayout", LINK_NAME );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.RESEARCHER, "edit" );

		// create blank Author
		Author author = null;
		if ( id != null )
			author = persistenceStrategy.getAuthorDAO().getById( id );

		if ( author == null )
		{
			model = TemplateHelper.createViewWithLink( "404", "error" );
			return model;
		}

		// get institution, currently author always belong to 1 institution
		if ( author.getInstitution() != null )
			author.setAffiliation( author.getInstitution().getName() );

		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "author", author );

		return model;
	}
	
	/**
	 * Save changes update researcher information via Spring binding
	 * 
	 * @param extractionServiceListWrapper
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/edit", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> updateAuthor( 
			@ModelAttribute( "author" ) Author author, 
			HttpServletRequest request, HttpServletResponse response )
	{
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		if ( author == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "failed to save, expired session" );
			return responseMap;
		}
		// current institution
		String onDbInstitution = "";
		// get institution, currently author always belong to 1 institution
		if ( author.getInstitution() != null )
			onDbInstitution = author.getInstitution().getName();

		if ( author.getAffiliation() != null )
		{
			if ( !author.getAffiliation().isEmpty() )
			{
				if ( !author.getAffiliation().equals( onDbInstitution ) )
				{
					// change affiliation, save if not on database
					Institution institution = null;
					List<Institution> institutions = persistenceStrategy.getInstitutionDAO().getByName( author.getAffiliation() );
					if ( !institutions.isEmpty() )
						institution = institutions.get( 0 );
					else
					{
						institution = new Institution();
						institution.setName( author.getAffiliation() );
					}
					author.setInstitution( institution );
				}
			}
			else
			{
				author.setInstitution( null );
			}
		}
		// check for photo
		if ( author.getAcademicStatus().isEmpty() )
			author.setAcademicStatus( null );

		// check for photo
		if ( author.getPhotoUrl().isEmpty() )
			author.setPhotoUrl( null );

		persistenceStrategy.getAuthorDAO().persist( author );

		responseMap.put( "status", "ok" );
		responseMap.put( "statusMessage", "author saved" );

		Map<String, String> authorMap = new LinkedHashMap<String, String>();
		authorMap.put( "id", author.getId() );
		authorMap.put( "name", author.getName() );
		authorMap.put( "position", author.getAcademicStatus() );
		responseMap.put( "author", authorMap );

		return responseMap;
	}

	/**
	 * Make the researcher invisible, API for administrator
	 * 
	 * @param id
	 * @param request
	 * @param response
	 * @return
	 */
	@Transactional
	@RequestMapping( value = "/setInvisible", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> setAuthorInvisible( @RequestParam( value = "id" ) final String id, HttpServletRequest request, HttpServletResponse response )
	{
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		if ( !securityService.isAuthorizedForRole( "ADMIN" ) )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "error 401 - not authorized" );
			return responseMap;
		}

		if ( id == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "author id missing" );
			return responseMap;
		}

		Author author = persistenceStrategy.getAuthorDAO().getById( id );

		if ( author == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "author id missing" );
			return responseMap;
		}
		// clear bookmark
		author.getUserAuthorBookmarks().clear();
		// set is added false
		author.setAdded( false );
		author.setRequestDate( null );

		persistenceStrategy.getAuthorDAO().persist( author );

		responseMap.put( "status", "ok" );
		responseMap.put( "statusMessage", "author is now invisible" );

		Map<String, String> authorMap = new LinkedHashMap<String, String>();
		authorMap.put( "id", author.getId() );
		authorMap.put( "name", author.getName() );
		authorMap.put( "position", author.getAcademicStatus() );
		responseMap.put( "author", authorMap );

		return responseMap;
	}

	/**
	 * Remove researcher request time to null, forced to recheck publication
	 * 
	 * @param id
	 * @param request
	 * @param response
	 * @return
	 */
	@Transactional
	@RequestMapping( value = "/removeRequestTime", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> removeRequestTime( @RequestParam( value = "id" ) final String id, HttpServletRequest request, HttpServletResponse response )
	{
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		if ( !securityService.isAuthorizedForRole( "ADMIN" ) )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "error 401 - not authorized" );
			return responseMap;
		}

		if ( id == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "author id missing" );
			return responseMap;
		}

		Author author = persistenceStrategy.getAuthorDAO().getById( id );

		if ( author == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "author id missing" );
			return responseMap;
		}

		author.setRequestDate( null );

		persistenceStrategy.getAuthorDAO().persist( author );

		responseMap.put( "status", "ok" );
		responseMap.put( "statusMessage", "unset researcher request date" );

		Map<String, String> authorMap = new LinkedHashMap<String, String>();
		authorMap.put( "id", author.getId() );
		authorMap.put( "name", author.getName() );
		authorMap.put( "position", author.getAcademicStatus() );
		responseMap.put( "author", authorMap );

		return responseMap;
	}

}
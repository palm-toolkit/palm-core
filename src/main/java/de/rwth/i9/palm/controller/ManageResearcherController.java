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
import de.rwth.i9.palm.model.SessionDataSet;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.pdfextraction.service.PdfExtractionService;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.ApplicationContextService;
import de.rwth.i9.palm.service.ApplicationService;

@Controller
@SessionAttributes( "author" )
@RequestMapping( value = "/researcher" )
public class ManageResearcherController
{
	private static final String LINK_NAME = "researcher";

	@Autowired
	private ApplicationContextService appService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private ApplicationService applicationService;

	@Autowired
	private PdfExtractionService pdfExtractionService;

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
		// get current session object
		SessionDataSet sessionDataSet = this.appService.getCurrentSessionDataSet();

		// set model and view
		ModelAndView model = TemplateHelper.createViewWithSessionDataSet( "dialogIframeLayout", LINK_NAME, sessionDataSet );
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
	 * Save changes from Add publication detail, via Spring binding
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

		// set based on user input
		newAuthor.setName( author.getName() );
		if ( !author.getAcademicStatus().equals( "" ) )
			newAuthor.setAcademicStatus( author.getAcademicStatus() );
		if ( author.getPhotoUrl().startsWith( "http:" ) )
			newAuthor.setPhotoUrl( author.getPhotoUrl() );
		newAuthor.setAdded( true );

		if ( author.getInstitutions() == null && !author.getAffiliation().equals( "" ) )
		{
			Institution institution = null;
			List<Institution> institutions = persistenceStrategy.getInstitutionDAO().getWithFullTextSearch( author.getAffiliation() );
			if ( !institutions.isEmpty() )
				institution = institutions.get( 0 );
			else
			{
				institution = new Institution();
				institution.setName( author.getAffiliation() );
			}
			newAuthor.addInstitution( institution );
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
	 * Load the add publication form together with publication object
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/edit", method = RequestMethod.GET )
	public ModelAndView editAuthor( 
			@RequestParam( value = "id") final String authorId,
			final HttpServletResponse response) throws InterruptedException
	{
		// get current session object
		SessionDataSet sessionDataSet = this.appService.getCurrentSessionDataSet();

		// set model and view
		ModelAndView model = TemplateHelper.createViewWithSessionDataSet( "dialogIframeLayout", LINK_NAME, sessionDataSet );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.PUBLICATION, "edit" );

		// create blank Author
		Author author = persistenceStrategy.getAuthorDAO().getById( authorId );

		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "author", author );

		return model;
	}
}
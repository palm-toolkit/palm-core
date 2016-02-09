package de.rwth.i9.palm.controller;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletResponse;

import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.xml.sax.SAXException;

import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.CompletionStatus;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationAuthor;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.pdfextraction.service.PdfExtractionService;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.SecurityService;

@Controller
@SessionAttributes( "publication" )
@RequestMapping( value = "/publication" )
public class ManagePublicationController
{
	private static final String LINK_NAME = "administration";

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private PdfExtractionService pdfExtractionService;

	@Autowired
	private SecurityService securityService;

	/**
	 * Load the add publication form together with publication object
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/add", method = RequestMethod.GET )
	public ModelAndView addNewPublication( 
			final HttpServletResponse response) throws InterruptedException
	{
		ModelAndView model = null;

		if ( securityService.getUser() == null )
		{
			model = TemplateHelper.createViewWithLink( "401", "error" );
			return model;
		}

		model = TemplateHelper.createViewWithLink( "dialogIframeLayout", LINK_NAME );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.PUBLICATION, "add" );

		// create blank Publication
		Publication publication = new Publication();

		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "publication", publication );

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
	public @ResponseBody Map<String, Object> saveNewPublication( 
			@ModelAttribute( "publication" ) Publication publication,
			@RequestParam( value = "author-list-ids" , required= false ) String authorListIds,
			@RequestParam( value = "keyword-list" , required= false ) String keywordList,
			@RequestParam( value = "publication-date" , required= false ) String publicationDate,
			@RequestParam( value = "venue-type" , required= false ) String venueType,
			@RequestParam( value = "venue-id" , required= false ) String venueId,
			@RequestParam( value = "issue" , required= false ) String issue,
			@RequestParam( value = "pages" , required= false ) String pages,
			@RequestParam( value = "publisher" , required= false ) String publisher,
			final HttpServletResponse response) throws InterruptedException
	{
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		if ( publication == null || publication.getTitle() == null || publication.getTitle().isEmpty() || authorListIds == null || authorListIds.isEmpty() )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "Publication not found due to missing input or expired sission" );
		}

		/* Insert selected author into publication */
		// get author id split by "_#_"
		String[] authorIds = authorListIds.split( "_#_" );
		int authorPosition = 0;
		for ( String authorId : authorIds )
		{
			Author author = persistenceStrategy.getAuthorDAO().getById( authorId );
			if ( author == null )
				continue;

			PublicationAuthor publicationAuthor = new PublicationAuthor();
			publicationAuthor.setAuthor( author );
			publicationAuthor.setPublication( publication );
			publicationAuthor.setPosition( authorPosition );
			publication.addPublicationAuthor( publicationAuthor );

			authorPosition++;
		}

		if ( publication.getPublicationAuthors() == null || publication.getPublicationAuthors().isEmpty() )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "Failed to save new publication, publication contain no authors" );
		}

		/* ABstract */
		if ( publication.getAbstractText().isEmpty() )
			publication.setAbstractText( null );
		else
		{
			publication.setAbstractStatus( CompletionStatus.COMPLETE );
		}

		/* Insert Keyword if any */
		if ( keywordList != null && !keywordList.isEmpty() )
		{
			publication.setKeywordStatus( CompletionStatus.COMPLETE );
			publication.setKeywordText( keywordList.replace( "_#_", "," ) );
		}

		/* Insert publication date - expect valid publication date */
		if ( publicationDate != null && !publicationDate.isEmpty() )
		{
			// set date format
			DateFormat dateFormat = new SimpleDateFormat( "yyyy/M/d", Locale.ENGLISH );
			try
			{

			}
			catch ( Exception e )
			{
				// TODO: handle exception
			}
		}


		return responseMap;
	}

	/**
	 * Upload article via jquery ajax file upload saved to database
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * @throws TikaException
	 * @throws SAXException
	 */
	@RequestMapping( value = "/upload", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> multiUpload( 
			MultipartHttpServletRequest request, HttpServletResponse response ) throws IOException, InterruptedException, ExecutionException
	{
		return uploadAndExtractPdf( request );
	}

	private Map<String, Object> uploadAndExtractPdf( 
			MultipartHttpServletRequest request ) throws IOException, InterruptedException, ExecutionException
	{
		// build an iterator
		Iterator<String> itr = request.getFileNames();

		Map<String, Object> extractedPdfMap = null;

		// get each file
		while ( itr.hasNext() )
		{
			MultipartFile mpf = request.getFile( itr.next() );

			// extract pdf file
			extractedPdfMap = pdfExtractionService.extractPdfFromInputStream( mpf.getInputStream() );

			break;
		}
		return extractedPdfMap;
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
	public ModelAndView editPublication( 
			@RequestParam( value = "sessionid", required = false ) final String sessionId,
			@RequestParam( value = "id") final String publicationId,
			final HttpServletResponse response) throws InterruptedException
	{
		ModelAndView model = null;

		if ( securityService.getUser() == null )
		{
			model = TemplateHelper.createViewWithLink( "401", "error" );
			return model;
		}

		model = TemplateHelper.createViewWithLink( "dialogIframeLayout", LINK_NAME );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.PUBLICATION, "edit" );

		// get publication
		Publication publication = persistenceStrategy.getPublicationDAO().getById( publicationId );
		publication.setAuthors();

		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "publication", publication );

		return model;
	}
}
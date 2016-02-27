package de.rwth.i9.palm.controller.administration;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.InterestProfile;
import de.rwth.i9.palm.model.InterestProfileProperty;
import de.rwth.i9.palm.model.TopicModelingAlgorithmAuthor;
import de.rwth.i9.palm.model.TopicModelingAlgorithmCircle;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.ApplicationService;
import de.rwth.i9.palm.service.SecurityService;
import de.rwth.i9.palm.wrapper.InterestProfileListWrapper;
import de.rwth.i9.palm.wrapper.TopicModelingAlgorithmAuthorListWrapper;
import de.rwth.i9.palm.wrapper.TopicModelingAlgorithmCircleListWrapper;

@Controller
@SessionAttributes( "interestProfileListWrapper" )
@RequestMapping( value = "/admin/topicmodel" )
public class ManageTopicModelController
{
	private static final String LINK_NAME = "administration";

	@Autowired
	private SecurityService securityService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private ApplicationService applicationService;

	/**
	 * Load the interestProfile detail form
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/{type}", method = RequestMethod.GET )
	public ModelAndView getInterestProfile( @PathVariable String type, final HttpServletResponse response ) throws InterruptedException
	{
		ModelAndView model = null;

		if ( !securityService.isAuthorizedForRole( "ADMIN" ) )
		{
			model = TemplateHelper.createViewWithLink( "401", "error" );
			return model;
		}

		model = TemplateHelper.createViewWithLink( "widgetLayoutAjax", LINK_NAME );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.ADMINISTRATION, "topic-model-" + type );

		if ( type.equals( "author" ) )
		{
			// get list of topicModelAlgorithmAuthor and sort
			List<TopicModelingAlgorithmAuthor> topicModelAlgorithmAuthors = persistenceStrategy.getTopicModelingAlgorithmAuthorDAO().getAllActiveInterestProfile();

			// put it into wrapper class
			TopicModelingAlgorithmAuthorListWrapper topicModelAlgorithmAuthorListWrapper = new TopicModelingAlgorithmAuthorListWrapper();
			topicModelAlgorithmAuthorListWrapper.setTopicModelingAlgorithms( topicModelAlgorithmAuthors );
			model.addObject( "interestProfileListWrapper", topicModelAlgorithmAuthorListWrapper );
		}
		else if ( type.equals( "circle" ) )
		{
			// get list of topicModelingAlgorithmCircle and sort
			List<TopicModelingAlgorithmCircle> topicModelingAlgorithmCircles = persistenceStrategy.getTopicModelingAlgorithmCircleDAO().getAllActiveInterestProfile();
			// put it into wrapper class
			TopicModelingAlgorithmCircleListWrapper topicModelingAlgorithmCircleListWrapper = new TopicModelingAlgorithmCircleListWrapper();
			topicModelingAlgorithmCircleListWrapper.setTopicModelingAlgorithms( topicModelingAlgorithmCircles );
			model.addObject( "interestProfileListWrapper", topicModelingAlgorithmCircleListWrapper );
		}
		// assign the model
		model.addObject( "widgets", widgets );


		return model;
	}

	/**
	 * Save changes from InterestProfile detail, via Spring binding
	 * 
	 * @param interestProfileListWrapper
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> saveInterestProfile( 
			@ModelAttribute( "interestProfileListWrapper" ) InterestProfileListWrapper interestProfileListWrapper, 
			final HttpServletResponse response ) throws InterruptedException
	{
		// persist interestProfile from model attribute
		for ( InterestProfile interestProfile : interestProfileListWrapper.getInterestProfiles() )
		{
			if ( interestProfile.getInterestProfileProperties() != null || !interestProfile.getInterestProfileProperties().isEmpty() )
			{
				Iterator<InterestProfileProperty> iteratorInterestProfileProperty = interestProfile.getInterestProfileProperties().iterator();
				while ( iteratorInterestProfileProperty.hasNext() )
				{
					InterestProfileProperty interestProfileProperty = iteratorInterestProfileProperty.next();
					// check for invalid interestProfileProperty object to be
					// removed
					if ( interestProfileProperty.getMainIdentifier().equals( "" ) || interestProfileProperty.getValue().equals( "" ) )
					{
						iteratorInterestProfileProperty.remove();
						// delete interestProfileProperty on database
						persistenceStrategy.getInterestProfilePropertyDAO().delete( interestProfileProperty );
					}
					else
					{
						interestProfileProperty.setInterestProfile( interestProfile );
					}
				}

			}
			persistenceStrategy.getInterestProfileDAO().persist( interestProfile );
		}

		// set response
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		responseMap.put( "status", "ok" );
		responseMap.put( "format", "json" );

		// update application service interestProfile cache
		applicationService.updateInterestProfilesCache();

		return responseMap;
	}

}
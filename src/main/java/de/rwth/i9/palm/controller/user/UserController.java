package de.rwth.i9.palm.controller.user;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.feature.user.UserFeature;
import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Controller
@RequestMapping( value = "/user" )
public class UserController
{

	private static final String LINK_NAME = "user";
	
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private UserFeature userFeature;

	@RequestMapping( method = RequestMethod.GET )
	public ModelAndView userPage( 
			@RequestParam( value = "page", required = false ) final String page,
			final HttpServletResponse response ) throws InterruptedException
	{
		ModelAndView model = TemplateHelper.createViewWithLink( "user", LINK_NAME );

		if( page != null)
			model.addObject( "activeMenu", page );
		else
			model.addObject( "activeMenu", "profile" );

		return model;
	}

	/**
	 * Add user's bookmark for researcher, publication, conference or circle
	 * 
	 * @param bookmarkType
	 * @param userId
	 * @param bookId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/bookmark/{bookmarkType}", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> bookmarkAdd( 
			@PathVariable String bookmarkType, 
			@RequestParam( value = "userId" ) final String userId, 
			@RequestParam( value = "bookId" ) final String bookId, 
			final HttpServletResponse response)
	{
		return userFeature.getUserBookmark().addUserBookmark( bookmarkType, userId, bookId );
	}

	/**
	 * Add user's bookmark for researcher, publication, conference or circle
	 * 
	 * @param bookmarkType
	 * @param userId
	 * @param bookId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/bookmark/remove/{bookmarkType}", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> bookmarkRemove( @PathVariable String bookmarkType, @RequestParam( value = "userId" ) final String userId, @RequestParam( value = "bookId" ) final String bookId, final HttpServletResponse response)
	{
		return userFeature.getUserBookmark().removeUserBookmark( bookmarkType, userId, bookId );
	}

}
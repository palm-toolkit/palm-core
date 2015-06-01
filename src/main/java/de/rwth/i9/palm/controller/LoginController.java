package de.rwth.i9.palm.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping( value = "/login" )
public class LoginController
{

	@RequestMapping( method = RequestMethod.GET )
	public String login(
			@RequestParam(value="form", required = false) String formMode)
	{
		if( formMode != null && formMode.equals( "true" ) )
			return "loginForm";
		return "login";
	}
}

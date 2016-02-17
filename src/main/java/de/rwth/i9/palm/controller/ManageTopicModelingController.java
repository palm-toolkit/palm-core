package de.rwth.i9.palm.controller;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Controller
@RequestMapping( value = "/topicmodeling" )
public class ManageTopicModelingController
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private PalmAnalytics palmAnalytics;

	@Transactional
	@RequestMapping( method = RequestMethod.GET )
	public @ResponseBody String allReindex()
	{
		//palmAnalytics.getNGrams().getStringTopicsUnigrams( m, nwords, weight )
		return "success";
	}
}

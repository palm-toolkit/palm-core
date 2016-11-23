package de.rwth.i9.palm.recommendation.service;

import java.util.List;
import java.util.concurrent.Future;

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PublicationDAO;
import de.rwth.i9.palm.persistence.relational.GenericDAOHibernate;

public interface PublicationExtractionService
{	
	public Future<JSONArray> getPublicationData( List<String> authors, String interestItem, int termValue ) throws InterruptedException;
}

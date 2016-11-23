package de.rwth.i9.palm.recommendation.service;

import java.util.List;
import java.util.concurrent.Future;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.persistence.PublicationDAO;

@Service
public class PublicationExtractionServiceImp implements PublicationExtractionService
{

	@Autowired
	private PersistenceStrategy persistenceStrategy;
	
	@SuppressWarnings( "unchecked" )
	@Async
	@Transactional
	@Override
	public Future<JSONArray> getPublicationData( List<String> authors, String interestItem, int termValue ) throws InterruptedException
	{
		List<Object[]> publications = persistenceStrategy.getPublicationDAO().getInterestPublication( authors, interestItem );
		
		JSONArray array = new JSONArray();
		
		for ( Object[] publication : publications )
		{
			//Publication publication = pubDao.getById( pubID.toString() );

			String id = String.valueOf( publication[0] );
			String title = String.valueOf( publication[1] );
			String author = String.valueOf( publication[2] );
			String abstrac = String.valueOf( publication[3] );
			String keyword = String.valueOf( publication[4] );

			JSONObject obj = new JSONObject();
			obj.put( "iName", interestItem);
			obj.put( "pID", id); //publication.getId() );
			obj.put( "pTitle", title); //publication.getTitle() );
			obj.put( "pAuthors", author);//publication.getAuthors().toString().replace( "[", "" ).replace( "]", "" ));
			obj.put( "pAbstract", abstrac);//publication.getAbstractText() );
			obj.put( "pKeywords", keyword);//publication.getKeywordText() );
			obj.put( "iValue", termValue );
			array.put( obj );
			break;
		}
		
		return new AsyncResult<JSONArray>( array );
	}
	
}

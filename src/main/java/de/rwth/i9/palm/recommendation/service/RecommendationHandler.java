package de.rwth.i9.palm.recommendation.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.mahout.cf.taste.common.TasteException;
import org.json.JSONArray;
import org.json.JSONException;

import de.rwth.i9.palm.model.Author;

public interface RecommendationHandler
{
	/**
	 * Compute the recommendation specific to algorithm
	 * @param algorithm	The name of algorithm to be processed.
	 * @param researcher	The active researcher name. 
	 * @param stepNo	The step to process in recommendation algorithm
	 * @return	Returns the array of nodes and links for the specified stepNo.
	 * @throws JSONException
	 * @throws SQLException
	 * @throws IOException
	 * @throws TasteException
	 */
	public JSONArray computeRecommendation( String algorithm, Author researcher, int stepNo )
			throws JSONException, SQLException, IOException, TasteException;
	
	/**
	 * Compute the whole tree of linked items for a single selected item.
	 * @param algorithm	The name of the algorithm currently selected.
	 * @param researcher	The current researcher.
	 * @param stepNo	The selected node step number.
	 * @param id	The selected items unique id.
	 * @return	Returns the nodes and links in JSONArray.
	 */
	public JSONArray computeSingleTree( String algorithm, Author researcher, int stepNo, String id );
	
	/**
	 * Request the author to be searched in database.
	 * @param query	The query to search in database.
	 * @param maxResults	The max number of results required.
	 * @param queryType	Search item type (Author or Interest).
	 * @return	Return the sorted array of authors.
	 */
	public List<Object> requesetAuthor( String query, int maxResults, String queryType );
	
	/**
	 * Reset the variables for new algorithm
	 */
	public void reset();
}

package de.rwth.i9.palm.recommendation.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.mahout.cf.taste.common.TasteException;
import org.json.JSONArray;
import org.json.JSONException;

import de.rwth.i9.palm.model.Author;

public interface SNAINRecommendationFeature {

	public JSONArray computeSNAINRecommendation( Author researcher, int stepNo)
			throws JSONException, SQLException, IOException, TasteException;
	
	public JSONArray computeSNAINSingleTree( Author researcher, int stepNo, String id );
	
	public List<Object> requesetAuthor( String interest, int maxResults );
}

package de.rwth.i9.palm.recommendation.service;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.mahout.cf.taste.common.TasteException;
import org.json.JSONArray;
import org.json.JSONException;

import de.rwth.i9.palm.model.Author;

public interface SNAC2RecommendationFeature
{
	public JSONArray computeSNAC2Recommendation( Author researcher )
			throws JSONException, SQLException, IOException, TasteException;
}

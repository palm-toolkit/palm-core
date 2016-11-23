package de.rwth.i9.palm.recommendation.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.gephi.graph.api.GraphModel;
import org.json.JSONArray;

public interface UtilIN
{
	public JSONArray interestSNFileCreator ( String rID, GraphModel model ) throws SQLException, IOException;

	public Map<String, Object> getAuthorInterestById( String authorId, boolean isReplaceExistingResult ) throws 
			UnsupportedEncodingException, InterruptedException, URISyntaxException, ExecutionException;
	
	public Collection<String> get3DCoAuthors(String rID);
}

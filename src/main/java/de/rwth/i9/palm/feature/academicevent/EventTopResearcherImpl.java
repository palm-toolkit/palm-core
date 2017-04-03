package de.rwth.i9.palm.feature.academicevent;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;

import de.rwth.i9.palm.persistence.PersistenceStrategy;

public class EventTopResearcherImpl implements EventTopResearcher
{
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@SuppressWarnings( "unchecked" )
	@Override
	public Map<String, Object> getResearcherTopListByEventId( String eventId, String pid, Integer maxresult, String orderBy ) throws UnsupportedEncodingException, InterruptedException, URISyntaxException, ExecutionException
	{
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		responseMap.put( "status", "ok" );
		return responseMap;
	}
}

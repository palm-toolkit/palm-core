package de.rwth.i9.palm.feature.publication;

import java.util.Map;

public interface PublicationMining
{
	public Map<String, Object> getPublicationExtractedTopicsById( String publicationId, String maxRetrieve );
}

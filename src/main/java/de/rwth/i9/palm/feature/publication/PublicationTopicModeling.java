package de.rwth.i9.palm.feature.publication;

import java.util.Map;

public interface PublicationTopicModeling
{
	public Map<String, Object> getTopicModeling( String publicationId, boolean isReplaceExistingResult );

	public Map<String, Object> getStaticTopicModelingNgrams( String publicationId, boolean isReplaceExistingResult );

	public Map<String, Object> getTopicModelUniCloud( String publicationId, boolean isReplaceExistingResult );
}

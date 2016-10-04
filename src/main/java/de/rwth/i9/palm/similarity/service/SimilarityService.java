package de.rwth.i9.palm.similarity.service;

import java.util.List;
import java.util.Map;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.DataMiningAuthor;
import de.rwth.i9.palm.model.DataMiningEventGroup;

public interface SimilarityService
{
	public Map<DataMiningAuthor, Double> similarAuthors( List<Author> authorList );

	public Map<DataMiningEventGroup, Double> similarConferences( List<String> idsList );

}

package de.rwth.i9.palm.datasetcollect.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationSource;
import de.rwth.i9.palm.model.SourceMethod;
import de.rwth.i9.palm.model.SourceType;

/**
 * 
 * @author sigit
 *
 */
@Service
public class AsynchronousPublicationDetailCollectionService
{
	private final static Logger log = LoggerFactory.getLogger( AsynchronousPublicationDetailCollectionService.class );

	@Autowired
	private AsynchronousCollectionService asynchronousCollectionService;

	/**
	 * Main method for collect publication detail from a publication from
	 * multiple source
	 * 
	 * @param publication
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Async
	public Future<Publication> asyncWalkOverSelectedPublication( Publication publication ) throws IOException, InterruptedException
	{
		// multithread publication source
		List<Future<PublicationSource>> publicationSourceFutureList = new ArrayList<Future<PublicationSource>>();

		for ( PublicationSource publicationSource : publication.getPublicationSources() )
		{
			// handling publication source
			if ( publicationSource.getSourceMethod() == SourceMethod.PARSEPAGE )
			{
				if ( publicationSource.getSourceType() == SourceType.GOOGLESCHOLAR )
					publicationSourceFutureList.add( asynchronousCollectionService.getListOfPublicationsDetailGoogleScholar( publicationSource ) );
				else if ( publicationSource.getSourceType() == SourceType.CITESEERX )
					publicationSourceFutureList.add( asynchronousCollectionService.getListOfPublicationDetailCiteseerX( publicationSource ) );
			}
		}

		return new AsyncResult<Publication>( publication );
	}
}

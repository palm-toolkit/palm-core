package de.rwth.i9.palm.interestmining.service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.interestmining.service.PublicationClusterHelper.TermDetail;
import de.rwth.i9.palm.model.AuthorInterest;
import de.rwth.i9.palm.model.ExtractionServiceType;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Service
public class CorePhraseInterestProfile
{
	final Logger logger = Logger.getLogger( CorePhraseInterestProfile.class );

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	public void doCorePhraseCalculation( AuthorInterest authorInterest, PublicationClusterHelper publicationCluster, Double yearFactor, Double totalYearFactor, int numberOfExtractionService )
	{
		// assign authorInterest properties
		authorInterest.setLanguage( publicationCluster.getLanguage() );

		DateFormat dateFormat = new SimpleDateFormat( "yyyy", Locale.ENGLISH );
		try
		{
			authorInterest.setYear( dateFormat.parse( Integer.toString( publicationCluster.getYear() ) ) );
		}
		catch ( ParseException e )
		{
			e.printStackTrace();
		}

		
		Map<String, TermDetail> termDetailsMap = publicationCluster.getTermMap();
		
		for ( Map.Entry<String, TermDetail> termDetailEntryMap : termDetailsMap.entrySet() )
		{
			String term = termDetailEntryMap.getKey();

			// just skip term which are too long
			if ( term.length() > 50 )
				continue;

			TermDetail termDetail = termDetailEntryMap.getValue();
			Double intersectionFactor = 0.0;

			// calculate intersection factor
			intersectionFactor = (double) termDetail.getExtractionServiceTypes().size();

			// Extraction from yahoo content analysis have higher precision
			if ( termDetail.getExtractionServiceTypes().contains( ExtractionServiceType.YAHOOCONTENTANALYSIS ) )
				intersectionFactor += 0.5;
			
			// then multiple by year factor
			intersectionFactor = intersectionFactor * yearFactor;
			
			// Finally, put calculation into authorInterest object
			// only if intersectionFactor > 1.0
			if ( intersectionFactor <= 1.0 )
				continue;
			
			Interest interest = persistenceStrategy.getInterestDAO().getInterestByTerm( term );

			if ( interest == null )
			{
				interest = new Interest();
				interest.setTerm( term );

				persistenceStrategy.getInterestDAO().persist( interest );
			}
			authorInterest.addTermWeight( interest, intersectionFactor );

		}
		
	}

}

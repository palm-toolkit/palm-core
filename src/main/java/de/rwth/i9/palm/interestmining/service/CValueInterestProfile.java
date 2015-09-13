package de.rwth.i9.palm.interestmining.service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.analytics.algorithm.cvalue.CValue;
import de.rwth.i9.palm.analytics.algorithm.cvalue.TermCandidate;
import de.rwth.i9.palm.analytics.api.PalmAnalytics;
import de.rwth.i9.palm.interestmining.service.PublicationClusterHelper.TermDetail;
import de.rwth.i9.palm.model.AuthorInterest;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Service
public class CValueInterestProfile
{
	final Logger logger = Logger.getLogger( CValueInterestProfile.class );

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private PalmAnalytics palmAnalytics;

	public void doCValueCalculation( AuthorInterest authorInterest, PublicationClusterHelper publicationCluster, int numberOfExtractionService )
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
		List<String> terms = new ArrayList<>();
		
		for ( Map.Entry<String, TermDetail> termDetailEntryMap : termDetailsMap.entrySet() )
		{
			String term = termDetailEntryMap.getKey();

			// just skip term which are too long
			if ( term.length() > 50 )
				continue;

			TermDetail termDetail = termDetailEntryMap.getValue();
			
			//only proceed for term that intersect with other topic extractor
			if ( termDetail.getExtractionServiceTypes().size() >= 2 )
			{
			
				for ( int i = 0; i < termDetail.getFrequencyOnTitle(); i++ )
				{
					// if there is weighting add here
					terms.add( term );
				}

				for ( int i = 0; i < termDetail.getFrequencyOnKeyword(); i++ )
				{
					// if there is weighting add here
					terms.add( term );
				}

				for ( int i = 0; i < termDetail.getFrequencyOnAbstract(); i++ )
				{
					terms.add( term );
				}
			}
		}
		
		// calculate c-value
		CValue cValue = palmAnalytics.getCValueAlgorithm();
		cValue.setTerms( terms );
		cValue.calculateCValue();
		
		List<TermCandidate> termCandidates = cValue.getTermCandidates();
		
		// put calculated term value to container
		for ( TermCandidate termCandidate : termCandidates )
		{
			if ( termCandidate.getCValue() >= ( numberOfExtractionService - 1 ) )
			{
				Interest interest = persistenceStrategy.getInterestDAO().getInterestByTerm( termCandidate.getCandidateTerm() );

				if ( interest == null )
				{
					interest = new Interest();
					interest.setTerm( termCandidate.getCandidateTerm() );
					persistenceStrategy.getInterestDAO().persist( interest );
				}
				authorInterest.addTermWeight( interest, termCandidate.getCValue() );
			}
		}
		
	}
}

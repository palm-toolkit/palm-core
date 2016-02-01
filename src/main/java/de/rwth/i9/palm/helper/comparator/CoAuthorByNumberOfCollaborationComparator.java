package de.rwth.i9.palm.helper.comparator;

import java.util.Comparator;
import java.util.Map;

public class CoAuthorByNumberOfCollaborationComparator implements Comparator<Map<String, Object>>
{

	@Override
	public int compare( final Map<String, Object> author1, final Map<String, Object> author2 )
	{
		if ( author1 == null && author2 == null )
			return 0;

		if ( author1 == null )
			return -1;

		if ( author2 == null )
			return 1;

		if ( author1.get( "coautorTimes" ) == null && author2.get( "coautorTimes" ) == null )
			return 0;

		if ( author1.get( "coautorTimes" ) == null )
			return -1;

		if ( author2.get( "coautorTimes" ) == null )
			return 1;

		int noCitation1 = (int) author1.get( "coautorTimes" );
		int noCitation2 = (int) author2.get( "coautorTimes" );

		if ( noCitation1 < noCitation2 )
			return 1;

		if ( noCitation1 > noCitation2 )
			return -1;

		return 0;
	}

}
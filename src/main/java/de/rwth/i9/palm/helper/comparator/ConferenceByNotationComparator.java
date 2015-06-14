package de.rwth.i9.palm.helper.comparator;

import java.util.Comparator;

import de.rwth.i9.palm.model.Conference;

/**
 * @author matthaeus
 * 
 */
public class ConferenceByNotationComparator implements Comparator<Conference>
{

	@Override
	public int compare( final Conference conference1, final Conference conference2 )
	{
		String notation1 = conference1.getConferenceGroup().getNotation() + conference1.getYear();
		String notation2 = conference2.getConferenceGroup().getNotation() + conference2.getYear();

		return notation1.compareTo( notation2 );
	}

}
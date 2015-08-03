package de.rwth.i9.palm.utils;

public class TextUtils
{
	public static String cutTextToLength( String text, int maxTextLength )
	{
		if ( text.length() > maxTextLength )
			return text.substring( 0, maxTextLength );
		return text;
	}
}

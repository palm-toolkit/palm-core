package de.rwth.i9.palm.feature.user;

import java.util.Map;

public interface UserBookmark
{
	/**
	 * Add bookmark for user for researcher, publication, conference and circle
	 * 
	 * @param bookmarkType
	 * @param userId
	 * @param bookId
	 * @return
	 */
	public Map<String, Object> addUserBookmark( String bookmarkType, String userId, String bookId );

	/**
	 * Remove bookmark for user for researcher, publication, conference and
	 * circle
	 * 
	 * @param bookmarkType
	 * @param userId
	 * @param bookId
	 * @return
	 */
	public Map<String, Object> removeUserBookmark( String bookmarkType, String userId, String bookId );
}

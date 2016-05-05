package de.rwth.i9.palm.feature.user;

import org.springframework.beans.factory.annotation.Autowired;

public class UserFeatureImpl implements UserFeature
{

	@Autowired( required = false )
	private UserBookmark userBookmark;

	@Override
	public UserBookmark getUserBookmark()
	{
		if ( this.userBookmark == null )
			this.userBookmark = new UserBookmarkImpl();

		return this.userBookmark;
	}

}

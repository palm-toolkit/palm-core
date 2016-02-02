package de.rwth.i9.palm.feature.researcher;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import de.rwth.i9.palm.helper.TreeHelper;
import de.rwth.i9.palm.helper.comparator.TreeDirectChildByChildNumberComparator;
import de.rwth.i9.palm.helper.comparator.TreeDirectChildByNaturalOrderComparator;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationType;

public class ResearcherAcademicEventTreeImpl implements ResearcherAcademicEventTree
{
	@Override
	public Map<String, Object> getResearcherAcademicEventTree( Author author )
	{
		// researchers list container
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		if ( author.getPublications() == null || author.getPublications().isEmpty() )
		{
			responseMap.put( "count", 0 );
			return responseMap;
		}
		// prepare tree helper
		TreeHelper rootTreeHelper = new TreeHelper();
		rootTreeHelper.setKey( "EventTree" );
		rootTreeHelper.setTitle( "EventTree" );

		for ( Publication publication : author.getPublications() )
		{
			if ( publication.getPublicationType() == null )
				continue;
			if ( publication.getPublicationType().equals( PublicationType.BOOK ) )
				continue;

			if ( publication.getEvent() != null )
			{
				TreeHelper treeHelperLv1 = TreeHelper.findNodeByKey( rootTreeHelper, publication.getEvent().getEventGroup().getId() );
				
				if( treeHelperLv1 == null ){
					// create first level (conference group)
					treeHelperLv1 = new TreeHelper();
					treeHelperLv1.setKey( publication.getEvent().getEventGroup().getId() );
					String nodeTitle =  publication.getEvent().getEventGroup().getName();
					if( !nodeTitle.equals( publication.getEvent().getEventGroup().getNotation() ) )
						nodeTitle += " (" + publication.getEvent().getEventGroup().getNotation() + ") ";
					treeHelperLv1.setTitle( nodeTitle );
					treeHelperLv1.setType( publication.getEvent().getEventGroup().getPublicationType().toString() );
					treeHelperLv1.setHref( "venue?&id=" + publication.getEvent().getEventGroup().getId() + "&name=" + publication.getEvent().getEventGroup().getName() );
					treeHelperLv1.setFolder( true );
					
					// add first level as child of root
					rootTreeHelper.addChild( treeHelperLv1 );
					
					// create second level ( conference year )
					TreeHelper treeHelperLv2 = new TreeHelper();
					treeHelperLv2.setKey( publication.getEvent().getId() );
					if( publication.getEvent().getName() != null)
						nodeTitle = publication.getEvent().getName();
					else{
						if( publication.getEvent().getVolume() != null )
							nodeTitle = "Volume " + publication.getEvent().getVolume() + ", " + publication.getEvent().getYear();
						else
							nodeTitle = publication.getEvent().getYear();
					}
					treeHelperLv2.setTitle( nodeTitle );
					treeHelperLv2.setType( publication.getPublicationType().toString() );
					treeHelperLv2.setHref( "venue?&id=" + publication.getEvent().getEventGroup().getId() + "&eventId=" + publication.getEvent().getId() + "&name=" + publication.getEvent().getEventGroup().getName() );
					treeHelperLv2.setFolder( true );
					
					// add second level as child of first level
					treeHelperLv1.addChild( treeHelperLv2 );
					
					// add third level for publication
					// create second level ( publication )
					TreeHelper treeHelperLv3 = new TreeHelper();
					treeHelperLv3.setKey( publication.getId() );
					treeHelperLv3.setTitle( publication.getTitle() );
					treeHelperLv3.setType( publication.getPublicationType().toString() );

					// add second level as child of first level
					treeHelperLv2.addChild( treeHelperLv3 );

				} else{

					TreeHelper treeHelperLv2 = TreeHelper.findNodeByKey( treeHelperLv1, publication.getEvent().getId() );

					if ( treeHelperLv2 == null )
					{
						// create second level ( conference year )
						String nodeTitle = "";
						treeHelperLv2 = new TreeHelper();
						treeHelperLv2.setKey( publication.getEvent().getId() );
						if ( publication.getEvent().getName() != null )
							nodeTitle = publication.getEvent().getName();
						else
						{
							if ( publication.getEvent().getVolume() != null )
								nodeTitle = "Volume " + publication.getEvent().getVolume() + ", " + publication.getEvent().getYear();
							else
								nodeTitle = publication.getEvent().getYear();
						}
						treeHelperLv2.setTitle( nodeTitle );
						treeHelperLv2.setType( publication.getPublicationType().toString() );
						treeHelperLv2.setHref( "venue?&id=" + publication.getEvent().getEventGroup().getId() + "&eventId=" + publication.getEvent().getId() + "&name=" + publication.getEvent().getEventGroup().getName() );
						treeHelperLv2.setFolder( true );

						// add second level as child of first level
						treeHelperLv1.addChild( treeHelperLv2 );
					}
					
					// add third level for publication
					// create second level ( publication )
					TreeHelper treeHelperLv3 = new TreeHelper();
					treeHelperLv3.setKey( publication.getId() );
					treeHelperLv3.setTitle( publication.getTitle() );
					treeHelperLv3.setType( publication.getPublicationType().toString() );

					// add second level as child of first level
					treeHelperLv2.addChild( treeHelperLv3 );
				}
			}
			// Event from additional information is not included
			else if ( publication.getAdditionalInformation() != null )
			{
			}
		}
		// sort
		Collections.sort( rootTreeHelper.getChildren(), new TreeDirectChildByChildNumberComparator() );

		// short children
		for ( TreeHelper th : rootTreeHelper.getChildren() )
		{
			Collections.sort( th.getChildren(), new TreeDirectChildByNaturalOrderComparator() );
		}
		// put coauthor to responseMap
		responseMap.put( "evenTree", rootTreeHelper);

		return responseMap;
	}

}

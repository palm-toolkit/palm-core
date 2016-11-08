package de.rwth.i9.palm.visualanalytics.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.PublicationTopic;

@Component
public class FilteredDataImpl implements FilteredData
{
	@Autowired
	private FilterHelper filterHelper;

	public Set<Publication> getFilteredPublications( String type, List<Author> authorList, List<EventGroup> eventGroupList, List<Publication> publicationList, List<Interest> interestList, List<Circle> circleList, List<Publication> filteredPublication, List<EventGroup> filteredConference, List<Interest> filteredTopic, List<Circle> filteredCircle, String startYear, String endYear )
	{
		Set<Publication> authorPublications = new HashSet<Publication>();

		if ( !filteredPublication.isEmpty() )
		{
			authorPublications = new HashSet<Publication>( filteredPublication );
		}
		else
		{
			authorPublications = filterHelper.typeWisePublications( type, authorList, eventGroupList, publicationList, interestList, circleList );
		}

		List<Publication> publicationsTemp = new ArrayList<Publication>( authorPublications );
		if ( !startYear.equals( "" ) && !startYear.equals( "0" ) && startYear != null )
		{
			for ( int i = 0; i < publicationsTemp.size(); i++ )
			{
				if ( publicationsTemp.get( i ).getYear() != null )
				{
					if ( Integer.parseInt( publicationsTemp.get( i ).getYear() ) < Integer.parseInt( startYear ) || Integer.parseInt( publicationsTemp.get( i ).getYear() ) > Integer.parseInt( endYear ) )
					{
						publicationsTemp.remove( i );
						i--;
					}
				}
				else
				{
					if ( publicationsTemp.get( i ).getPublicationDate() != null )
					{
						String year = publicationsTemp.get( i ).getPublicationDate().toString().substring( 0, 4 );
						if ( Integer.parseInt( year ) < Integer.parseInt( startYear ) || Integer.parseInt( year ) > Integer.parseInt( endYear ) )
						{
							publicationsTemp.remove( i );
							i--;
						}
					}
					else
					{
						publicationsTemp.remove( i );
						i--;
					}
				}
			}
		}
		authorPublications = new HashSet<Publication>( publicationsTemp );
		System.out.println( "PUBLICATIONS FROM FILTERED DATA: " + publicationsTemp.size() );
		// conference filter
		List<Publication> conferencePublications = new ArrayList<Publication>();
		if ( !filteredConference.isEmpty() )
		{
			for ( int i = 0; i < filteredConference.size(); i++ )
			{
				List<Event> events = filteredConference.get( i ).getEvents();
				for ( int j = 0; j < events.size(); j++ )
				{
					List<Publication> eventPublications = events.get( j ).getPublications();
					for ( int k = 0; k < eventPublications.size(); k++ )
					{
						if ( !conferencePublications.contains( eventPublications.get( k ) ) )
						{
							conferencePublications.add( eventPublications.get( k ) );
						}
					}
				}
			}
			List<Publication> tempPubList = new ArrayList<Publication>( authorPublications );
			for ( int l = 0; l < tempPubList.size(); l++ )
			{
				if ( !conferencePublications.contains( tempPubList.get( l ) ) )
				{
					tempPubList.remove( l );
					l--;
				}
			}
			authorPublications = new HashSet<Publication>( tempPubList );
		}

		// topic filter
		if ( !filteredTopic.isEmpty() )
		{
			Set<Publication> topicPublications = new HashSet<Publication>();

			for ( Publication authorPublication : authorPublications )
			{
				List<PublicationTopic> pubTopics = new ArrayList<PublicationTopic>( authorPublication.getPublicationTopics() );

				for ( PublicationTopic pt : pubTopics )
				{
					Map<String, Double> termValues = pt.getTermValues();
					List<String> terms = new ArrayList<String>( termValues.keySet() );
					List<String> interests = new ArrayList<String>();
					for ( Interest interest : filteredTopic )
					{
						if ( terms.contains( interest.getTerm() ) )
							interests.add( interest.getTerm() );
						if ( terms.contains( interest.getTerm() + "s" ) )
						{
							interests.add( interest.getTerm() + "s" );
						}
					}
					if ( interests.size() == filteredTopic.size() )
					{
						topicPublications.add( authorPublication );
					}
				}
			}
			authorPublications = topicPublications;
		}

		// circle filter
		if ( !filteredCircle.isEmpty() )
		{
			List<Publication> circlePublications = new ArrayList<Publication>();
			for ( int i = 0; i < filteredCircle.size(); i++ )
			{
				List<Publication> publications = new ArrayList<Publication>( filteredCircle.get( i ).getPublications() );
				for ( Publication p : publications )
				{
					if ( !circlePublications.contains( p ) )
					{
						circlePublications.add( p );
					}
				}
			}
			List<Publication> tempPubList = new ArrayList<Publication>( authorPublications );
			for ( int l = 0; l < tempPubList.size(); l++ )
			{
				if ( !circlePublications.contains( tempPubList.get( l ) ) )
				{
					tempPubList.remove( l );
					l--;
				}
			}
			authorPublications = new HashSet<Publication>( tempPubList );
		}
		return authorPublications;
	}

}

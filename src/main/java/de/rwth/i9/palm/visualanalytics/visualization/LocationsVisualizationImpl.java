package de.rwth.i9.palm.visualanalytics.visualization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.rwth.i9.palm.feature.academicevent.AcademicEventFeature;
import de.rwth.i9.palm.feature.researcher.ResearcherFeature;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventInterest;
import de.rwth.i9.palm.model.EventInterestProfile;
import de.rwth.i9.palm.model.Interest;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Component
public class LocationsVisualizationImpl implements LocationsVisualization
{
	@Autowired
	private ResearcherFeature researcherFeature;

	@Autowired
	private AcademicEventFeature eventFeature;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	public Map<String, Object> visualizeLocations( String type, Set<Publication> publications, List<String> idsList, String startYear, String endYear, List<Interest> filteredTopic )
	{
		Map<String, Object> visMap = new LinkedHashMap<String, Object>();

		if ( type.equals( "researcher" ) || type.equals( "publication" ) || type.equals( "topic" ) )
			visMap.putAll( researcherFeature.getResearcherAcademicEventTree().getResearcherAllAcademicEvents( publications, true ) );

		if ( type.equals( "conference" ) )
		{
			Boolean valid = false;
			if ( startYear.equals( "" ) || startYear.equals( "0" ) )
			{
				valid = true;
			}
			List<Object> listEvents = new ArrayList<Object>();
			for ( int i = 0; i < idsList.size(); i++ )
			{
				@SuppressWarnings( "unchecked" )
				List<Object> innerList = (List<Object>) eventFeature.getEventMining().fetchEventGroupData( idsList.get( i ), null, null ).get( "events" );
				for ( int j = 0; j < innerList.size(); j++ )
				{
					@SuppressWarnings( "unchecked" )
					Map<String, Object> innerListMap = (Map<String, Object>) innerList.get( j );

					if ( !startYear.equals( "" ) && !startYear.equals( "0" ) )
						if ( Integer.parseInt( startYear ) <= Integer.parseInt( innerListMap.get( "year" ).toString() ) && Integer.parseInt( endYear ) >= Integer.parseInt( innerListMap.get( "year" ).toString() ) )
							valid = true;
						else
							valid = false;

					if ( !filteredTopic.isEmpty() && valid )
					{
						Event e = persistenceStrategy.getEventDAO().getById( innerListMap.get( "id" ).toString() );
						Set<EventInterestProfile> eips = e.getEventInterestProfiles();
						List<String> interestStrings = new ArrayList<String>();

						for ( EventInterestProfile eip : eips )
						{
							Set<EventInterest> eventInterests = eip.getEventInterests();
							for ( EventInterest ei : eventInterests )
							{
								Map<Interest, Double> termWeights = ei.getTermWeights();
								List<Interest> interests = new ArrayList<Interest>( termWeights.keySet() );
								for ( Interest interest : interests )
								{
									if ( !interestStrings.contains( interest.getTerm() ) )
										interestStrings.add( interest.getTerm() );
								}
							}
						}
						List<String> interests = new ArrayList<String>();
						for ( Interest interest : filteredTopic )
						{
							if ( interestStrings.contains( interest.getTerm() ) )
								interests.add( interest.getTerm() );
							if ( interestStrings.contains( interest.getTerm() + "s" ) )
								interests.add( interest.getTerm() + "s" );
						}

						if ( interests.size() == filteredTopic.size() )
						{
							valid = true;
						}
						else
							valid = false;

					}

					if ( valid )
						listEvents.add( innerList.get( j ) );
				}
			}
			visMap.put( "events", listEvents );

		}

		return visMap;
	}
}

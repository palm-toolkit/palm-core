package de.rwth.i9.palm.feature.academicevent;

/**
 * Factory interface for features Venue/AcademicEvent
 * 
 * @author sigit
 *
 */
public interface AcademicEventFeature
{
	public EventSearch getEventSearch();

	public EventPublication getEventPublication();
}

package de.rwth.i9.palm.feature.academicevent;

/**
 * Factory interface for features Venue/AcademicEvent
 * 
 * @author sigit
 *
 */
public interface AcademicEventFeature
{
	public EventMining getEventMining();

	public EventPublication getEventPublication();

	public EventSearch getEventSearch();
}

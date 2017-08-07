package org.projectforge.business.teamcal.event.ical;

public enum EventHandleError
{
  EVENT_TO_DELETE_NOT_FOUND,       // No event in db to delete
  CALANDER_NOT_SPECIFIED,          // no calender defined
  MAIN_RECURRING_EVENT_MISSING     // the main event of an reccurring one is missing
}

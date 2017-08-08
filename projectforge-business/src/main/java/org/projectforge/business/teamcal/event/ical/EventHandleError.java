package org.projectforge.business.teamcal.event.ical;

public enum EventHandleError
{
  CALANDER_NOT_SPECIFIED,               // no calender defined
  NO_METHOD_SELECTED,                   // no method for handling event selected

  WARN_MAIN_RECURRING_EVENT_MISSING,    // the main event of an reccurring one is missing
  WARN_EVENT_TO_DELETE_NOT_FOUND,       // No event in db to delete
  WARN_OUTDATED                         // the parsed event is older then the current DB state
}

package org.projectforge.business.teamcal.event.ical;

import java.util.ArrayList;
import java.util.List;

import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

public class EventHandle
{
  public enum HandleState
  {
    UNKNOWN, PROCESSED, ERROR
  }

  private HandleState state;
  private TeamEventDO event;
  private HandleMethod method;
  private boolean process;
  private TeamCalDO calendar;

  private List<EventHandleError> errors;

  public EventHandle(final TeamEventDO event, final TeamCalDO calendar, final HandleMethod method)
  {
    this.state = HandleState.UNKNOWN;
    this.event = event;
    this.method = method;
    this.process = false;
    this.calendar = calendar;
    this.errors = new ArrayList<>();
  }

  public HandleState getState()
  {
    return state;
  }

  public void setState(final HandleState state)
  {
    this.state = state;
  }

  public TeamEventDO getEvent()
  {
    return event;
  }

  public void setEvent(final TeamEventDO event)
  {
    this.event = event;
  }

  public boolean isProcess()
  {
    return process;
  }

  public void setProcess(final boolean process)
  {
    this.process = process;
  }

  public HandleMethod getMethod()
  {
    return method;
  }

  public void setMethod(final HandleMethod method)
  {
    this.method = method;
  }

  public TeamCalDO getCalendar()
  {
    return calendar;
  }

  public void setCalendar(final TeamCalDO calendar)
  {
    this.calendar = calendar;
  }

  public void addError(final EventHandleError error)
  {
    this.errors.add(error);
  }

  public List<EventHandleError> getErrors()
  {
    return errors;
  }

  public void setErrors(final List<EventHandleError> errors)
  {
    this.errors = errors;
  }
}

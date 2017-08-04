package org.projectforge.business.teamcal.event.ical;

import java.util.ArrayList;
import java.util.List;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

public class EventHandle
{
  public enum HandleState
  {
    UNKNOWN, PROCESSED, ERROR
  }

  private HandleState state;
  private TeamEventDO event;
  private boolean process;

  private List<String> errors;

  public EventHandle(final TeamEventDO event)
  {
    this.state = HandleState.UNKNOWN;
    this.event = event;
    this.process = false;
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

  public List<String> getErrors()
  {
    return errors;
  }

  public void setErrors(final List<String> errors)
  {
    this.errors = errors;
  }
}

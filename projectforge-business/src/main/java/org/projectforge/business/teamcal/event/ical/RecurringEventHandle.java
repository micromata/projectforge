package org.projectforge.business.teamcal.event.ical;

import java.util.ArrayList;
import java.util.List;

import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

public class RecurringEventHandle extends EventHandle
{
  private List<EventHandle> relatedEvents;

  public RecurringEventHandle(final TeamEventDO event, final TeamCalDO calendar, final HandleMethod method)
  {
    super(event, calendar, method);
    this.relatedEvents = new ArrayList<>();
  }

  public List<EventHandle> getRelatedEvents()
  {
    return this.relatedEvents;
  }
}

package org.projectforge.business.teamcal.service;

import org.springframework.stereotype.Service;

@Service
public class TeamCalServiceImpl
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamCalServiceImpl.class);

  //  public TeamEventDO createTeamEventDO(final VEvent event, boolean withUid)
  //  {
  //    return null;
  //  }
  //
  //  public static List<VEvent> getVEvents(final net.fortuna.ical4j.model.Calendar calendar)
  //  {
  //    final List<VEvent> events = new ArrayList<>();
  //    @SuppressWarnings("unchecked")
  //    final List<CalendarComponent> list = calendar.getComponents(Component.VEVENT);
  //    if (list == null || list.size() == 0) {
  //      return events;
  //    }
  //    // Temporary not used, because multiple events are not supported.
  //    for (final Component c : list) {
  //      final VEvent event = (VEvent) c;
  //
  //      if (event.getSummary() != null && StringUtils.equals(event.getSummary().getValue(), TeamCalConfig.SETUP_EVENT) == true) {
  //        // skip setup event!
  //        continue;
  //      }
  //      events.add(event);
  //    }
  //    return events;
  //  }
  //
  //  public List<TeamEventDO> getTeamEvents(final net.fortuna.ical4j.model.Calendar calendar)
  //  {
  //    final List<VEvent> list = getVEvents(calendar);
  //    return convert(list);
  //  }
  //
  //  public List<TeamEventDO> convert(final List<VEvent> list)
  //  {
  //    final List<TeamEventDO> events = new ArrayList<TeamEventDO>();
  //    if (list == null || list.size() == 0) {
  //      return events;
  //    }
  //    for (final VEvent vEvent : list) {
  //      events.add(createTeamEventDO(vEvent, true));
  //    }
  //    events.sort((o1, o2) -> {
  //      final Date startDate1 = o1.getStartDate();
  //      final Date startDate2 = o2.getStartDate();
  //      if (startDate1 == null) {
  //        if (startDate2 == null) {
  //          return 0;
  //        }
  //        return -1;
  //      }
  //      return startDate1.compareTo(startDate2);
  //    });
  //    return events;
  //  }

}

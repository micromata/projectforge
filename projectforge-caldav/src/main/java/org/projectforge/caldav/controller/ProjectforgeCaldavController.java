package org.projectforge.caldav.controller;

import java.util.Date;
import java.util.List;

import org.projectforge.caldav.config.ApplicationContextProvider;
import org.projectforge.caldav.model.Calendar;
import org.projectforge.caldav.model.CalendarsHome;
import org.projectforge.caldav.model.Meeting;
import org.projectforge.caldav.model.User;
import org.projectforge.caldav.model.UsersHome;
import org.projectforge.caldav.rest.CalendarRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.annotations.Calendars;
import io.milton.annotations.ChildrenOf;
import io.milton.annotations.Delete;
import io.milton.annotations.Get;
import io.milton.annotations.ICalData;
import io.milton.annotations.PutChild;
import io.milton.annotations.ResourceController;
import io.milton.annotations.Root;

@ResourceController
public class ProjectforgeCaldavController extends BaseDavController
{
  private static Logger log = LoggerFactory.getLogger(ProjectforgeCaldavController.class);

  private CalendarRest calendarRest;

  public ProjectforgeCaldavController()
  {
    log.info("ProjectforgeCaldavController()");
  }

  @Root
  public ProjectforgeCaldavController getRoot()
  {
    return this;
  }

  @ChildrenOf
  public UsersHome getUsersHome(ProjectforgeCaldavController root)
  {
    if (usersHome == null) {
      log.info("Create new UsersHome");
      usersHome = new UsersHome();
    }
    return usersHome;
  }

  @ChildrenOf
  public CalendarsHome getCalendarsHome(User user)
  {
    CalendarsHome calendarsHome = getUserCache().getUserCalendarHomeMap().get(user.getPk());
    if (calendarsHome != null) {
      return calendarsHome;
    }
    log.info("Creating CalendarsHome for user:" + user);
    calendarsHome = new CalendarsHome(user);
    getUserCache().getUserCalendarHomeMap().put(user.getPk(), calendarsHome);
    return calendarsHome;
  }

  @ChildrenOf
  @Calendars
  public List<Calendar> getCalendarsHome(CalendarsHome cals)
  {
    return getCalendarRest().getCalendarList(cals.getUser());
  }

  @ChildrenOf
  public List<Meeting> getCalendarEvents(Calendar cal)
  {
    return getCalendarRest().getCalendarEvents(cal);
  }

  @Get
  @ICalData
  public byte[] getMeetingData(Meeting m)
  {
    return m.getIcalData();
  }

  @PutChild
  public Meeting createMeeting(Calendar cal, byte[] ical, String newName)
  {
    Meeting requestMeeting = new Meeting(cal);
    requestMeeting.setIcalData(ical);
    Date now = new Date();
    requestMeeting.setCreateDate(now);
    requestMeeting.setModifiedDate(now);
    requestMeeting.setName(newName);
    return getCalendarRest().saveCalendarEvent(requestMeeting);
  }

  @PutChild
  public Meeting updateMeeting(Meeting m, byte[] ical)
  {
    m.setIcalData(ical);
    final Meeting meetingUpdated = getCalendarRest().updateCalendarEvent(m);

    // update modification date in event parameter, required for computing eTag!
    m.setModifiedDate(meetingUpdated.getModifiedDate());

    return meetingUpdated;
  }

  @Delete
  public void deleteMeeting(Meeting m)
  {
    getCalendarRest().deleteCalendarEvent(m);
  }

  private CalendarRest getCalendarRest()
  {
    if (calendarRest == null) {
      calendarRest = ApplicationContextProvider.getApplicationContext().getBean(CalendarRest.class);
    }
    return calendarRest;
  }
}

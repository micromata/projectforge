package org.projectforge.caldav.service;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.ent.config.HttpManagerBuilderEnt;
import io.milton.http.HttpManager;
import io.milton.http.ResourceFactory;
import io.milton.http.caldav.CalendarSearchService;
import io.milton.http.caldav.EventResource;
import io.milton.http.caldav.EventResourceImpl;
import io.milton.http.caldav.ICalFormatter;
import io.milton.http.caldav.ITip;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.mail.MailboxAddress;
import io.milton.principal.CalDavPrincipal;
import io.milton.resource.CalendarResource;
import io.milton.resource.CollectionResource;
import io.milton.resource.ICalResource;
import io.milton.resource.Resource;
import io.milton.resource.SchedulingResponseItem;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.CalendarComponent;

/**
 * Created by blumenstein on 17.05.17.
 */
public class ProjectForgeCalendarSearchService implements CalendarSearchService
{
  private static final Logger log = LoggerFactory.getLogger(ProjectForgeCalendarSearchService.class);
  private static final SimpleDateFormat LOG_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private final HttpManagerBuilderEnt builderEnt;
  private final ICalFormatter formatter;
  private final SimpleDateFormat untilFormatter;
  private ResourceFactory rFactory;
  private String schedulingColName = "cals";
  private String inboxName = "inbox";
  private String outBoxName = "outbox";
  private String usersBasePath = "/users/";

  public ProjectForgeCalendarSearchService(final HttpManagerBuilderEnt builderEnt)
  {
    if (builderEnt == null) {
      throw new NullPointerException("ResourceFactory is null");
    }
    this.builderEnt = builderEnt;
    this.formatter = new ICalFormatter();
    this.untilFormatter = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
  }

  @Override
  public List<ICalResource> findCalendarResources(CalendarResource calendar, Date start, Date end) throws NotAuthorizedException, BadRequestException
  {
    return findCalendarResources(calendar, start, end, null);
  }

  @Override
  public List<ICalResource> findCalendarResources(CalendarResource calendar, Date start, Date end,
      AbstractMap.SimpleImmutableEntry<String, String> propFilter) throws NotAuthorizedException, BadRequestException
  {
    log.info("Find calender resources of '{}'/'{}' within time window from '{}' to '{}'", calendar.getName(), calendar.getUniqueId(),
        start != null ? LOG_FORMAT.format(start) : "null",
        end != null ? LOG_FORMAT.format(end) : "null");

    // build a list of all calendar resources
    List<ICalResource> list = new ArrayList<>();
    for (Resource r : calendar.getChildren()) {
      if (r instanceof ICalResource) {
        ICalResource cr = (ICalResource) r;
        list.add(cr);
      }
    }

    int sizeBefore = list.size();

    // So now we have (or might have) start and end dates, so filter list
    Iterator<ICalResource> it = list.iterator();
    while (it.hasNext()) {
      ICalResource r = it.next();
      log.debug("Check event '{}'", r.getUniqueId());

      // create calender object
      StringReader sin = new StringReader(r.getICalData());
      CalendarBuilder builder = new CalendarBuilder();
      Calendar cal = null;

      try {
        cal = builder.build(sin);
      } catch (IOException e) {
        log.error("Exception building calendar from ics", e);
      } catch (ParserException e) {
        log.error("Unable to parse ics", e);
      }

      // check if event is inside boundaries
      try {
        if (outsideDates(r, cal, start, end)) {
          it.remove();
          continue;
        }
      } catch (IOException e) {
        log.error("Date constraints of event '{}' could not be checked, event is skipped", r.getUniqueId(), e);
        it.remove();
        continue;
      } catch (ParserException e) {
        log.error("Date constraints of event '{}' could not be checked, event is skipped", r.getUniqueId(), e);
        it.remove();
        continue;
      }

      // check if event fulfills properties
      if (propFilter != null) {
        if (cal != null && !cal.getComponent("VEVENT").getProperty(propFilter.getKey()).getValue().equals(propFilter.getValue())) {
          log.debug("Does not match properties filter: '{}'", r.getUniqueId());
          it.remove();
        }
      }
    }

    log.info("Found {} ({} total, {} removed) events for calender resources '{}'/'{}'", list.size(), sizeBefore, (sizeBefore - list.size()), calendar.getName(),
        calendar.getUniqueId());

    return list;
  }

  private Map<String, String> extractRRule(Calendar calender)
  {
    if (calender == null)
      return null;

    final CalendarComponent vevent = calender.getComponent("VEVENT");
    if (vevent == null)
      return null;

    final Property rrule = vevent.getProperty("RRULE");
    if (rrule == null)
      return null;

    final String value = rrule.getValue();
    if (value == null)
      return null;

    final String values[] = value.split(";");
    Map<String, String> keyValues = new TreeMap<>();

    for (final String v : values) {
      final String kv[] = v.split("=");

      if (kv.length != 2)
        continue;

      keyValues.put(kv[0], kv[1]);
    }

    return keyValues;
  }

  private boolean outsideDates(ICalResource r, Calendar calender, Date start, Date end) throws IOException, ParserException
  {
    Map<String, String> kv = this.extractRRule(calender);

    log.debug("Check outsideDates for event '{}' ({})", r.getUniqueId(), kv != null ? "recurring" : "normal");

    if (kv != null) {
      String until = kv.get("UNTIL");
      if (until != null) {
        if (start == null) {
          return false;
        }

        try {
          Date uDate = this.untilFormatter.parse(until);
          if (uDate.before(start)) {
            return true;
          }
        } catch (ParseException e) {
          log.warn("Until date '{}' from RRULE can't be parsed for event '{}', start/stop date can't be checked!", until,
              r.getUniqueId(), e);
          // Event is recurring, returning false prevents wrong pick out
          return false;
        }
      }
      // Recurring event without until date -> not outside
      return false;
    }

    EventResource event;
    if (r instanceof EventResource) {
      event = (EventResource) r;
    } else {
      event = new EventResourceImpl();
      formatter.parseEvent(event, r.getICalData());
    }

    if (start != null) {
      if (event.getEnd().before(start)) {
        log.debug("Event is before start: {} < {}",
            event.getEnd() != null ? LOG_FORMAT.format(event.getEnd()) : "null",
            start != null ? LOG_FORMAT.format(start) : "null");
        return true;
      }
    }

    if (end != null) {
      if (event.getStart().after(end)) {
        log.debug("Event is after end: {} < {}",
            event.getStart() != null ? LOG_FORMAT.format(event.getStart()) : "null",
            end != null ? LOG_FORMAT.format(end) : "null");
        return true;
      }
    }

    return false;
  }

  @Override
  public List<SchedulingResponseItem> queryFreeBusy(CalDavPrincipal principal, String iCalText)
  {
    log.info("Query free/busy time for principal '{}' and iCalText '{}'", principal, iCalText);
    ICalFormatter.FreeBusyRequest r = formatter.parseFreeBusyRequest(iCalText);
    log.info("queryFreeBusy: attendees=" + r.getAttendeeLines().size() + " - " + r.getAttendeeMailtos().size());
    List<SchedulingResponseItem> list = new ArrayList<SchedulingResponseItem>();
    // For each attendee locate events within the given date range and add them as busy responses
    try {
      for (String attendeeMailto : r.getAttendeeMailtos()) {
        MailboxAddress add = MailboxAddress.parse(attendeeMailto);
        CalDavPrincipal attendee = findUserFromMailto(add);
        if (attendee == null) {
          log.warn("Attendee not found: " + attendeeMailto);
          SchedulingResponseItem item = new SchedulingResponseItem(attendeeMailto, ITip.StatusResponse.RS_INVALID_37, null);
          list.add(item);
        } else {
          log.info("Found attendee: " + attendee.getName());
          // Now locate events and build an ical response
          String ical = buildFreeBusyAttendeeResponse(attendee, r, add.domain, attendeeMailto);
          SchedulingResponseItem item = new SchedulingResponseItem(attendeeMailto, ITip.StatusResponse.RS_SUCCESS_20, ical);
          list.add(item);
        }
      }
    } catch (NotAuthorizedException ex) {
      throw new RuntimeException(ex);
    } catch (BadRequestException ex) {
      throw new RuntimeException(ex);
    }
    return list;
  }

  /**
   * Attempt to iterate over the entire users collection, and for each event
   * in each user's calendar check if the given user is an attendee, and if
   * return it.
   * <p>
   * Rather inefficient
   *
   * @param user
   * @return
   * @throws NotAuthorizedException
   * @throws BadRequestException
   */
  @Override
  public List<ICalResource> findAttendeeResources(CalDavPrincipal user) throws NotAuthorizedException, BadRequestException
  {
    log.info("Find attendee resources for CalDAV principal '{}'", user.getName());

    List<ICalResource> list = new ArrayList<>();
    return list;
    //    String host = HttpManager.request().getHostHeader();
    //    Resource rUsersHome = getResourceFactory().getResource(host, usersBasePath);
    //    if (rUsersHome instanceof CollectionResource) {
    //      CollectionResource usersHome = (CollectionResource) rUsersHome;
    //      for (Resource rUser : usersHome.getChildren()) {
    //        if (rUser instanceof CalDavPrincipal) {
    //          CalDavPrincipal p = (CalDavPrincipal) rUser;
    //          for (String href : p.getCalendarHomeSet()) {
    //            Resource rCalHome = getResourceFactory().getResource(host, href);
    //            if (rCalHome instanceof CollectionResource) {
    //              CollectionResource calHome = (CollectionResource) rCalHome;
    //              for (Resource rCal : calHome.getChildren()) {
    //                if (rCal instanceof CalendarResource) {
    //                  CalendarResource cal = (CalendarResource) rCal;
    //                  for (Resource rEvent : cal.getChildren()) {
    //                    if (rEvent instanceof ICalResource) {
    //                      ICalResource event = (ICalResource) rEvent;
    //                      if (isAttendeeOf(user, event)) {
    //                        list.add(event);
    //                      }
    //                    }
    //                  }
    //                }
    //              }
    //            }
    //          }
    //        }
    //      }
    //    }
    //    return list;
  }

  @Override
  public String findAttendeeResourcesCTag(CalDavPrincipal attendee) throws NotAuthorizedException, BadRequestException
  {
    log.info("Find attendee resources CTag of caldav principal '{}'", attendee.getName());

    Date latest = null;
    for (ICalResource r : findAttendeeResources(attendee)) {
      Date d = r.getModifiedDate();
      if (latest == null || d.after(latest)) {
        latest = d;
      }
    }
    if (latest != null) {
      return "mod-" + latest.getTime();
    } else {
      return "na";
    }

  }

  @Override
  public String getSchedulingColName()
  {
    return schedulingColName;
  }

  public void setSchedulingColName(String schedulingColName)
  {
    this.schedulingColName = schedulingColName;
  }

  @Override
  public String getSchedulingInboxColName()
  {
    return inboxName;
  }

  public void setSchedulingInboxColName(String inboxName)
  {
    this.inboxName = inboxName;
  }

  @Override
  public String getSchedulingOutboxColName()
  {
    return outBoxName;
  }

  public void setSchedulingOutboxColName(String outBoxName)
  {
    this.outBoxName = outBoxName;
  }

  /**
   * Use the domain portion of the email as the host, and the initial portion
   * as the userid. This wont work in systems which require use userid's with
   *
   * @param add
   * @return
   */
  private CalDavPrincipal findUserFromMailto(MailboxAddress add) throws NotAuthorizedException, BadRequestException
  {
    String userPath = usersBasePath + add.user;
    Resource r = getResourceFactory().getResource(add.domain, userPath);
    if (r == null) {
      log.warn("Failed to find: " + userPath + " in host: " + add.domain);
      return null;
    } else {
      if (r instanceof CalDavPrincipal) {
        CalDavPrincipal p = (CalDavPrincipal) r;
        return p;
      } else {
        log.warn("findUserFromMailto: found a resource but it is not a CalDavPrincipal. Is a: " + r.getClass().getCanonicalName());
        return null;
      }
    }
  }

  public String getUsersBasePath()
  {
    return usersBasePath;
  }

  public void setUsersBasePath(String usersBasePath)
  {
    this.usersBasePath = usersBasePath;
  }

  private String buildFreeBusyAttendeeResponse(CalDavPrincipal attendee, ICalFormatter.FreeBusyRequest request, String domain, String attendeeMailto)
      throws NotAuthorizedException, BadRequestException
  {
    Map<String, String> source = request.getLines();
    StringBuilder sb = new StringBuilder();
    sb.append("BEGIN:VCALENDAR\n");
    sb.append("VERSION:2.0 PRODID:-//milton.io//CalDAV Server//EN\n");
    sb.append("METHOD:REPLY\n");
    sb.append("BEGIN:VFREEBUSY\n");
    // Copy these lines back verbatim
    sb.append(source.get("UID")).append("\n");
    sb.append(source.get("DTSTAMP")).append("\n");
    sb.append(source.get("DTSTART")).append("\n");
    sb.append(source.get("DTEND")).append("\n");
    sb.append(source.get("ORGANIZER")).append("\n");
    // Output the original attendee line
    sb.append(request.getAttendeeLines().get(attendeeMailto)).append("\n");

    Date start = request.getStart();
    Date finish = request.getFinish();
    for (String href : attendee.getCalendarHomeSet()) {
      if (log.isTraceEnabled()) {
        log.trace("Look for calendar home: " + href);
      }
      Resource rCalHome = getResourceFactory().getResource(domain, href);
      if (rCalHome instanceof CollectionResource) {
        CollectionResource calHome = (CollectionResource) rCalHome;
        log.trace("Look for calendars in home");
        for (Resource rColCal : calHome.getChildren()) {
          if (rColCal instanceof CalendarResource) {
            CalendarResource cal = (CalendarResource) rColCal;
            List<ICalResource> eventsInRange = findCalendarResources(cal, start, finish, null);
            if (log.isTraceEnabled()) {
              log.trace("Process calendar: " + cal.getName() + " events in range=" + eventsInRange.size());
              log.trace("  range= " + start + " - " + finish);
            }
            for (ICalResource event : eventsInRange) {
              log.trace("Process event: " + event.getName());
              EventResourceImpl er = new EventResourceImpl();
              try {
                formatter.parseEvent(er, event.getICalData());
              } catch (IOException ex) {
                throw new RuntimeException(ex);
              } catch (ParserException ex) {
                throw new RuntimeException(ex);
              }

              // write the freebusy statement, Eg:
              // FREEBUSY;FBTYPE=BUSY:20090602T110000Z/20090602T120000Z
              sb.append("FREEBUSY;FBTYPE=BUSY:");
              sb.append(formatter.formatDate(er.getStart()));
              sb.append("/");
              sb.append(formatter.formatDate(er.getEnd()));
              sb.append("\n");
            }
          }
        }
      } else {
        if (rCalHome == null) {
          log.warn("Didnt find calendar home: " + href + " in domain: " + domain);
        } else {
          log.warn("Found a resource at the calendar home address, but it is not a CollectionResource. Is a: " + rCalHome.getClass());
        }
      }
    }
    sb.append("END:VFREEBUSY\n");
    sb.append("END:VCALENDAR\n");
    return sb.toString();
  }

  /**
   * Check if the given user is an attendee of the given event. Just does
   * a simple check on the userId portion of the mailto address against
   * the name of the principal
   *
   * @param user
   * @param event
   * @return
   */
  private boolean isAttendeeOf(CalDavPrincipal user, ICalResource event)
  {
    for (String mailTo : formatter.parseAttendees(event.getICalData())) {
      if (mailTo == null) {
        log.debug("E-mail address for event attendee '{}' is null", event.getName());
        continue;
      }

      try {
        MailboxAddress add = MailboxAddress.parse(mailTo);
        if (add.user.equals(user.getName())) {
          return true;
        }
      } catch (IllegalArgumentException e) {
        log.warn("Could not parse E-mail address '{}' for event attendee, skip attendee", mailTo, event.getName());
      }
    }
    return false;
  }

  private ResourceFactory getResourceFactory()
  {
    if (this.rFactory == null) {
      this.rFactory = this.builderEnt.getResourceFactory();
    }

    return this.rFactory;
  }
}

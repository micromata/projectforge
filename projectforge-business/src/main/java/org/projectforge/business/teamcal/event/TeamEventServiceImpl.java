/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.business.teamcal.event;

import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.RRule;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.calendar.event.model.ICalendarEvent;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.configuration.DomainService;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.diff.TeamEventDiff;
import org.projectforge.business.teamcal.event.diff.TeamEventDiffType;
import org.projectforge.business.teamcal.event.diff.TeamEventField;
import org.projectforge.business.teamcal.event.ical.ICalGenerator;
import org.projectforge.business.teamcal.event.ical.ICalHandler;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDao;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeStatus;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.teamcal.service.CryptService;
import org.projectforge.business.teamcal.servlet.TeamCalResponseServlet;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.calendar.ICal4JUtils;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.mail.Mail;
import org.projectforge.mail.SendMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TeamEventServiceImpl implements TeamEventService
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TeamEventServiceImpl.class);

  private enum EventMailType
  {
    NEW, DELETED, UPDATED
  }

  @Autowired
  private AddressDao addressDao;

  @Autowired
  private TeamEventAttendeeDao teamEventAttendeeDao;

  @Autowired
  private TeamEventDao teamEventDao;

  @Autowired
  private SendMail sendMail;

  @Autowired
  private UserService userService;

  @Autowired
  private CryptService cryptService;

  @Autowired
  private ConfigurationService configService;

  @Autowired
  private DomainService domainService;

  // Set TeamCalEvent fields used for computing a diff in order to send notification mails
  private static final Set<TeamEventField> TEAM_EVENT_FIELD_FILTER = Stream.of(
      TeamEventField.START_DATE,
      TeamEventField.END_DATE,
      TeamEventField.ALL_DAY,
      TeamEventField.LOCATION,
      TeamEventField.NOTE,
      TeamEventField.SUBJECT,
      TeamEventField.RECURRENCE_EX_DATES,
      TeamEventField.RECURRENCE_RULE,
      TeamEventField.RECURRENCE_REFERENCE_DATE
  ).collect(Collectors.toCollection(HashSet::new));

  @Override
  public List<Integer> getAssignedAttendeeIds(TeamEventDO data)
  {
    List<Integer> assignedAttendees = new ArrayList<>();
    if (data != null && data.getAttendees() != null) {
      for (TeamEventAttendeeDO attendee : data.getAttendees()) {
        assignedAttendees.add(attendee.getId());
      }
    }
    return assignedAttendees;
  }

  @Override
  public List<TeamEventAttendeeDO> getAddressesAndUserAsAttendee()
  {
    List<TeamEventAttendeeDO> resultList = new ArrayList<>();
    List<AddressDO> allAddressList = addressDao.internalLoadAllNotDeleted();
    List<PFUserDO> allUserList = userService.getAllActiveUsers();
    Set<Integer> addedUserIds = new HashSet<>();
    for (AddressDO singleAddress : allAddressList) {
      if (StringUtils.isBlank(singleAddress.getEmail()) == false) {
        TeamEventAttendeeDO attendee = new TeamEventAttendeeDO();
        attendee.setStatus(TeamEventAttendeeStatus.IN_PROCESS);
        attendee.setAddress(singleAddress);
        PFUserDO userWithSameMail = allUserList.stream()
            .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().equals(singleAddress.getEmail().toLowerCase())).findFirst().orElse(null);
        if (userWithSameMail != null && addedUserIds.contains(userWithSameMail.getId()) == false) {
          attendee.setUser(userWithSameMail);
          addedUserIds.add(userWithSameMail.getId());
        }
        resultList.add(attendee);
      }
    }
    for (PFUserDO u : allUserList) {
      if (addedUserIds.contains(u.getId()) == false) {
        TeamEventAttendeeDO attendee = new TeamEventAttendeeDO();
        attendee.setStatus(TeamEventAttendeeStatus.IN_PROCESS);
        attendee.setUser(u);
        resultList.add(attendee);
      }
    }
    return resultList;
  }

  @Override
  public TeamEventAttendeeDO getAttendee(Integer attendeeId)
  {
    return teamEventAttendeeDao.internalGetById(attendeeId);
  }

  @Override
  public void assignAttendees(TeamEventDO data, Set<TeamEventAttendeeDO> itemsToAssign, Set<TeamEventAttendeeDO> itemsToUnassign)
  {
    for (TeamEventAttendeeDO assignAttendee : itemsToAssign) {
      if (assignAttendee.getId() == null || assignAttendee.getId() < 0) {
        assignAttendee.setId(null);
        if (assignAttendee.getStatus() == null) {
          assignAttendee.setStatus(TeamEventAttendeeStatus.NEEDS_ACTION);
        }
        data.addAttendee(assignAttendee);
        teamEventAttendeeDao.internalSave(assignAttendee);
      }
    }

    if (data.getAttendees() != null && itemsToUnassign != null && itemsToUnassign.size() > 0) {
      data.getAttendees().removeAll(itemsToUnassign);
      for (TeamEventAttendeeDO deleteAttendee : itemsToUnassign) {
        teamEventAttendeeDao.internalMarkAsDeleted(deleteAttendee);
      }
    }

    teamEventDao.update(data);
  }

  @Override
  public void updateAttendees(TeamEventDO event, Set<TeamEventAttendeeDO> attendeesOldState)
  {
    final Set<TeamEventAttendeeDO> attendeesNewState = event.getAttendees();

    // new list is empty -> delete all
    if (attendeesNewState == null || attendeesNewState.isEmpty()) {
      if (attendeesOldState != null && attendeesOldState.isEmpty() == false) {
        for (TeamEventAttendeeDO attendee : attendeesOldState) {
          teamEventAttendeeDao.internalMarkAsDeleted(attendee);
        }
      }

      return;
    }

    // old list is empty -> insert all
    if (attendeesOldState == null || attendeesOldState.isEmpty()) {
      for (TeamEventAttendeeDO attendee : attendeesNewState) {
        // save new attendee
        attendee.setId(null);
        if (attendee.getStatus() == null) {
          attendee.setStatus(TeamEventAttendeeStatus.NEEDS_ACTION);
        }

        teamEventAttendeeDao.internalSave(attendee);
      }

      return;
    }

    // compute diff
    for (TeamEventAttendeeDO attendee : attendeesNewState) {
      boolean found = false;
      String eMail = attendee.getAddress() != null ? attendee.getAddress().getEmail() : attendee.getUrl();

      if (eMail == null) {
        // should not occur
        continue;
      }

      for (TeamEventAttendeeDO attendeeOld : attendeesOldState) {
        String eMailOld = attendeeOld.getAddress() != null ? attendeeOld.getAddress().getEmail() : attendeeOld.getUrl();

        if (eMail.equals(eMailOld)) {
          found = true;

          // update values
          attendee.setPk(attendeeOld.getPk());
          attendee.setComment(attendeeOld.getComment());
          attendee.setCommentOfAttendee(attendeeOld.getCommentOfAttendee());
          attendee.setLoginToken(attendeeOld.getLoginToken());
          attendee.setNumber(attendeeOld.getNumber());
          attendee.setAddress(attendeeOld.getAddress());
          attendee.setUser(attendeeOld.getUser());

          teamEventAttendeeDao.internalSave(attendee);

          break;
        }
      }

      if (found == false) {
        // save new attendee
        attendee.setId(null);
        if (attendee.getStatus() == null) {
          attendee.setStatus(TeamEventAttendeeStatus.NEEDS_ACTION);
        }
        teamEventAttendeeDao.internalSave(attendee);
      }
    }

    for (TeamEventAttendeeDO attendee : attendeesOldState) {
      boolean found = false;
      String eMail = attendee.getAddress() != null ? attendee.getAddress().getEmail() : attendee.getUrl();

      for (TeamEventAttendeeDO attendeeNew : attendeesNewState) {
        String eMailNew = attendeeNew.getAddress() != null ? attendeeNew.getAddress().getEmail() : attendeeNew.getUrl();

        if (eMail.equals(eMailNew)) {
          found = true;
          break;
        }
      }

      if (found == false) {
        // delete attendee
        teamEventAttendeeDao.internalMarkAsDeleted(attendee);
      }
    }
  }

  @Override
  public boolean checkAndSendMail(final TeamEventDO event, final TeamEventDiffType diffType)
  {
    if (this.preCheckSendMail(event) == false) {
      return false;
    }

    final TeamEventDiff diff = TeamEventDiff.compute(event, diffType);
    return this.checkAndSendMail(diff);
  }

  @Override
  public boolean checkAndSendMail(final TeamEventDO eventNew, final TeamEventDO eventOld)
  {
    if (this.preCheckSendMail(eventNew) == false) {
      return false;
    }

    final TeamEventDiff diff = TeamEventDiff.compute(eventNew, eventOld, TEAM_EVENT_FIELD_FILTER);
    return this.checkAndSendMail(diff);
  }

  private boolean checkAndSendMail(final TeamEventDiff diff)
  {
    boolean result = true;

    switch (diff.getDiffType()) {
      case NEW:
      case RESTORED:
        result &= this.sendMail(diff.getEventNewState(), diff, diff.getEventNewState().getAttendees(), EventMailType.NEW);
        break;
      case DELETED:
        result &= this.sendMail(diff.getEventNewState(), diff, diff.getEventNewState().getAttendees(), EventMailType.DELETED);
        break;
      case UPDATED:
      case ATTENDEES:
        result &= this.sendMail(diff.getEventNewState(), diff, diff.getAttendeesNotChanged(), EventMailType.UPDATED);
        result &= this.sendMail(diff.getEventNewState(), diff, diff.getAttendeesAdded(), EventMailType.NEW);
        result &= this.sendMail(diff.getEventOldState(), diff, diff.getAttendeesRemoved(), EventMailType.DELETED);
        break;
      case NONE:
        // nothing to do
        break;
    }

    return result;
  }

  private boolean preCheckSendMail(final TeamEventDO event)
  {
    // check event ownership
    if (event.getOwnership() != null && event.getOwnership() == false) {
      return false;
    }

    // check date, send mails for future events only
    final Date now = new Date();
    if (event.getStartDate().after(now)) {
      return true;
    }

    // No recurrence so event is in the past
    if (event.getRecurrenceRule() == null) {
      return false;
    }

    // Check rrule to see if an until date exists
    try {
      final RRule rRule = new RRule(event.getRecurrenceRule());
      final net.fortuna.ical4j.model.Date until = rRule.getRecur().getUntil();
      if (until == null) {
        return true;
      }

      final Date untilDate = new Date(until.getTime());
      return untilDate.before(now) == false;
    } catch (ParseException e) {
      return false;
    }
  }

  private boolean sendMail(final TeamEventDO event, final TeamEventDiff diff, final Set<TeamEventAttendeeDO> attendees, final EventMailType mailType)
  {
    boolean result = true;

    for (TeamEventAttendeeDO attendee : attendees) {
      result &= this.sendMail(event, diff, attendee, mailType);
    }

    return result;
  }

  private boolean sendMail(final TeamEventDO event, final TeamEventDiff diff, TeamEventAttendeeDO attendee, final EventMailType mailType)
  {
    final PFUserDO sender = ThreadLocalUserContext.getUser();

    if (sender == null) {
      return false;
    }

    final Mail msg = createMail(event, mailType, sender);
    final Map<String, Object> dataMap = createData(event, diff, sender, attendee, mailType);

    // add attendee as receiver
    if (StringUtils.isNotBlank(attendee.getEMailAddress())) {
      msg.addTo(attendee.getEMailAddress());
    } else if (StringUtils.isNotBlank(attendee.getUrl())) {
      msg.addTo(attendee.getUrl());
    }

    if (msg.getTo().isEmpty()) {
      return false;
    }

    // set mail content
    final String content = sendMail.renderGroovyTemplate(msg, "mail/teamEventEmail.html", dataMap, ThreadLocalUserContext.getUser());
    msg.setContent(content);

    // create iCal
    Method method = null;
    switch (mailType) {
      case NEW:
      case UPDATED:
        method = Method.REQUEST;
        break;
      case DELETED:
        method = Method.CANCEL;
        break;
    }

    final ICalGenerator generator = ICalGenerator.forMethod(method);
    generator.addEvent(event);
    ByteArrayOutputStream icsFile = generator.getCalendarAsByteStream();

    try {
      String ics = icsFile.toString(StandardCharsets.UTF_8.name());

      // send mail & return result
      return sendMail.send(msg, ics, null);
    } catch (UnsupportedEncodingException e) {
      log.error("An error occurred while sending an event notification to attendee", e);
      return false;
    }
  }

  private Mail createMail(final TeamEventDO event, final EventMailType mailType, final PFUserDO sender)
  {
    final Mail msg = new Mail();
    msg.setFrom(sender.getEmail());
    msg.setFromRealname(sender.getFullname());

    msg.setContentType(Mail.CONTENTTYPE_HTML);
    final String subject = I18nHelper.getLocalizedMessage("plugins.teamcal.attendee.email.subject." + mailType.name().toLowerCase(),
        sender.getFullname(), event.getSubject());
    msg.setProjectForgeSubject(subject);
    return msg;
  }

  private Map<String, Object> createData(final TeamEventDO event, final TeamEventDiff diff, final PFUserDO sender,
      TeamEventAttendeeDO attendee, final EventMailType mailType)
  {
    // get local and timezone
    final Locale locale;
    final TimeZone timezone;

    if (attendee.getUser() != null) {
      locale = attendee.getUser().getLocale() != null ? attendee.getUser().getLocale() : ThreadLocalUserContext.getLocale(null);
      timezone = attendee.getUser().getTimeZoneObject();
    } else {
      locale = sender.getLocale() != null ? sender.getLocale() : ThreadLocalUserContext.getLocale(null);
      timezone = sender.getTimeZoneObject();
    }

    // TODO rework!
    // TODO add diff stuff if updated
    DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
    formatter.setTimeZone(timezone);

    final Map<String, Object> dataMap = new HashMap<>();
    Calendar startDate = Calendar.getInstance(timezone);
    startDate.setTime(event.getStartDate());
    Calendar endDate = Calendar.getInstance(timezone);
    endDate.setTime(event.getEndDate());

    String location = event.getLocation() != null ? event.getLocation() : "";
    String note = event.getNote() != null ? event.getNote() : "";
    formatter = new SimpleDateFormat("EEEE", locale);
    formatter.setTimeZone(timezone);
    String startDay = formatter.format(startDate.getTime());
    String endDay = formatter.format(endDate.getTime());

    formatter = new SimpleDateFormat("dd. MMMMM YYYY HH:mm", locale);
    formatter.setTimeZone(timezone);
    String beginDateTime = formatter.format(startDate.getTime());
    String endDateTime = formatter.format(endDate.getTime());
    String invitationText = I18nHelper.getLocalizedMessage("plugins.teamcal.attendee.email.content." + mailType.name().toLowerCase(),
        sender.getFullname(), event.getSubject());
    String beginText = startDay + ", " + beginDateTime + " " + I18nHelper.getLocalizedMessage("oclock") + ".";
    String endText = endDay + ", " + endDateTime + " " + I18nHelper.getLocalizedMessage("oclock") + ".";
    String dayOfWeek = startDay;

    String fromToHeader;
    if (startDate.get(Calendar.DATE) == endDate.get(Calendar.DATE)) //Einen Tag
    {
      formatter = new SimpleDateFormat("HH:mm", locale);
      formatter.setTimeZone(timezone);
      String endTime = formatter.format(endDate.getTime());
      fromToHeader =
          beginDateTime + " - " + endTime + " " + I18nHelper.getLocalizedMessage("oclock") + ".";
    } else    //Mehrere Tage
    {
      fromToHeader = beginDateTime;
    }
    if (event.getAllDay()) {
      formatter = new SimpleDateFormat("dd. MMMMM YYYY", locale);
      formatter.setTimeZone(timezone);
      fromToHeader = formatter.format(startDate.getTime());
      formatter = new SimpleDateFormat("EEEE, dd. MMMMM YYYY", locale);
      formatter.setTimeZone(timezone);
      beginText =
          I18nHelper.getLocalizedMessage("plugins.teamcal.event.allDay") + ", " + formatter.format(startDate.getTime());
      endText = I18nHelper.getLocalizedMessage("plugins.teamcal.event.allDay") + ", " + formatter.format(endDate.getTime());
    }
    List<String> attendeeList = new ArrayList<>();
    for (TeamEventAttendeeDO attendees : event.getAttendees()) {
      attendeeList.add(attendees.getAddress() != null ? attendees.getAddress().getEmail() : attendees.getUrl());
    }
    String repeat = "";
    RRule rRule = null;
    ArrayList<String> exDate = new ArrayList<>();
    if (event.hasRecurrence()) {
      try {
        rRule = new RRule(event.getRecurrenceRule());
      } catch (ParseException e) {
        e.printStackTrace();
      }
      repeat = getRepeatText(rRule);
      formatter = new SimpleDateFormat("dd.MM.yyyy", locale);
      if (event.getRecurrenceExDate() != null && event.getRecurrenceExDate().length() > 7) {
        String[] exDateSplit = event.getRecurrenceExDate().split(",");
        for (int i = 0; i < exDateSplit.length - 1; i++) {
          Date date = ICal4JUtils.parseICalDateString(exDateSplit[i], timezone);
          if (date != null) {
            exDate.add(formatter.format(date));
          }
        }
      }
    }

    dataMap.put("dayOfWeek", dayOfWeek);
    dataMap.put("fromToHeader", fromToHeader);
    dataMap.put("invitationText", invitationText);
    dataMap.put("beginText", beginText);
    dataMap.put("endText", endText);
    dataMap.put("attendeeList", attendeeList);
    dataMap.put("location", location);
    dataMap.put("note", note);
    dataMap.put("acceptLink", getResponseLink(event, attendee, TeamEventAttendeeStatus.ACCEPTED));
    dataMap.put("declineLink", getResponseLink(event, attendee, TeamEventAttendeeStatus.DECLINED));
    dataMap.put("deleted", mailType == EventMailType.DELETED ? "true" : "false");
    dataMap.put("hasRRule", event.hasRecurrence() ? "true" : "false");
    dataMap.put("repeat", repeat);
    dataMap.put("exDateList", exDate);

    return dataMap;
  }

  private String getResponseLink(TeamEventDO event, TeamEventAttendeeDO attendee, TeamEventAttendeeStatus status)
  {
    final String messageParamBegin = "calendar=" + event.getCalendarId() + "&uid=" + event.getUid() + "&attendee=" + attendee.getId();
    final String acceptParams = cryptService.encryptParameterMessage(messageParamBegin + "&status=" + status.name());
    return domainService.getDomain() + TeamCalResponseServlet.PFCALENDAR + "?" + acceptParams;
  }

  private String getRepeatText(RRule rRule)
  {
    String msg = "";
    StringBuilder stringBuilder = new StringBuilder();
    switch (rRule.getRecur().getFrequency()) {
      case "DAILY": {
        //JEDEN
        if (rRule.getRecur().getInterval() == -1) {
          msg = I18nHelper.getLocalizedMessage("plugins.teamcal.event.event.everyDay");
        } else //ALLE ...
        {
          msg = I18nHelper.getLocalizedMessage("plugins.teamcal.event.event.allDay", rRule.getRecur().getInterval());
        }
      }
      break;
      case "WEEKLY": {
        //JEDEN
        if (rRule.getRecur().getInterval() == -1) {
          msg = I18nHelper.getLocalizedMessage("plugins.teamcal.event.event.everyWeek");
        } else //ALLE ...
        {
          msg = I18nHelper.getLocalizedMessage("plugins.teamcal.event.event.allWeeks", rRule.getRecur().getInterval());
        }
      }
      break;
      case "MONTHLY": {
        //JEDEN
        if (rRule.getRecur().getInterval() == -1) {
          msg = I18nHelper.getLocalizedMessage("plugins.teamcal.event.event.everyMonth");
        } else //ALLE ...
        {
          msg = I18nHelper.getLocalizedMessage("plugins.teamcal.event.event.allMonth", rRule.getRecur().getInterval());
        }
      }
      break;
      case "YEARLY": {
        //JEDEN
        if (rRule.getRecur().getInterval() == -1) {
          msg = I18nHelper.getLocalizedMessage("plugins.teamcal.event.event.everyYear");
        } else //ALLE ...
        {
          msg = I18nHelper.getLocalizedMessage("plugins.teamcal.event.event.allYear", rRule.getRecur().getInterval());
        }
      }
      break;
    }

    //BIS ZUM
    if (rRule.getRecur().getUntil() != null) {
      SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.YYYY", ThreadLocalUserContext.getLocale());
      Date date = new Date(rRule.getRecur().getUntil().getTime());
      msg += stringBuilder.append(" " + I18nHelper.getLocalizedMessage("plugins.teamcal.event.event.endsAt", formatter.format(date))).toString();
    }//... MALE
    else if (rRule.getRecur().getCount() != -1) {
      msg += stringBuilder.append(", " + I18nHelper.getLocalizedMessage("plugins.teamcal.event.event.endsBy", rRule.getRecur().getCount())).toString();
    }//FÜR IMMER
    else {

    }

    return msg;
  }

  @Override
  public TeamEventDO findByUid(Integer calendarId, String reqEventUid, boolean excludeDeleted)
  {
    return teamEventDao.getByUid(calendarId, reqEventUid, excludeDeleted);
  }

  @Override
  public TeamEventAttendeeDO findByAttendeeId(Integer attendeeId, boolean checkAccess)
  {
    TeamEventAttendeeDO result = null;
    if (checkAccess) {
      result = teamEventAttendeeDao.getById(attendeeId);
    } else {
      result = teamEventAttendeeDao.internalGetById(attendeeId);
    }
    return result;
  }

  @Override
  public TeamEventAttendeeDO findByAttendeeId(Integer attendeeId)
  {
    return findByAttendeeId(attendeeId, true);
  }

  @Override
  public void update(TeamEventDO event)
  {
    update(event, true);
  }

  @Override
  public void update(TeamEventDO event, boolean checkAccess)
  {
    teamEventDao.internalUpdate(event, checkAccess);
  }

  @Override
  public List<ICalendarEvent> getEventList(TeamEventFilter filter, boolean calculateRecurrenceEvents)
  {
    return teamEventDao.getEventList(filter, calculateRecurrenceEvents);
  }

  @Override
  public List<TeamEventDO> getTeamEventDOList(TeamEventFilter filter)
  {
    return teamEventDao.getList(filter);
  }

  @Override
  public TeamEventDO getById(Integer teamEventId)
  {
    return teamEventDao.getById(teamEventId);
  }

  @Override
  public void saveOrUpdate(TeamEventDO teamEvent)
  {
    teamEventDao.saveOrUpdate(teamEvent);
  }

  @Override
  public void markAsDeleted(TeamEventDO teamEvent)
  {
    teamEventDao.markAsDeleted(teamEvent);
  }

  @Override
  public void undelete(TeamEventDO teamEvent)
  {
    teamEventDao.undelete(teamEvent);
  }

  @Override
  public void save(TeamEventDO newEvent)
  {
    teamEventDao.save(newEvent);
  }

  @Override
  public TeamEventDao getTeamEventDao()
  {
    return teamEventDao;
  }

  @Override
  public void updateAttendee(TeamEventAttendeeDO attendee, boolean accesscheck)
  {
    if (accesscheck) {
      teamEventAttendeeDao.update(attendee);
    } else {
      teamEventAttendeeDao.internalUpdate(attendee);
    }
  }

  @Override
  public List<Integer> getCalIdList(Collection<TeamCalDO> teamCals)
  {
    return teamEventDao.getCalIdList(teamCals);
  }

  @Override
  public ICalHandler getEventHandler(final TeamCalDO defaultCalendar)
  {
    return new ICalHandler(this, defaultCalendar);
  }

  @Override
  public void fixAttendees(final TeamEventDO event)
  {
    List<TeamEventAttendeeDO> attendeesFromDbList = this.getAddressesAndUserAsAttendee();

    Integer internalNewAttendeeSequence = -10000;
    boolean found;

    for (TeamEventAttendeeDO attendeeDO : event.getAttendees()) {
      found = false;

      // search for eMail in DB as possible attendee
      for (TeamEventAttendeeDO dBAttendee : attendeesFromDbList) {
        if (dBAttendee.getEMailAddress() != null && dBAttendee.getEMailAddress().equals(attendeeDO.getUrl())) {
          attendeeDO = dBAttendee;
          attendeeDO.setId(internalNewAttendeeSequence--);
          found = true;
          break;
        }
      }

      if (found == false) {
        attendeeDO.setId(internalNewAttendeeSequence--);
      }
    }
  }
}

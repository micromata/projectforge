package org.projectforge.web.teamcal.event;

import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.TeamEventService;
import org.projectforge.business.teamcal.event.TeamEventServiceImpl;
import org.projectforge.business.teamcal.event.ical.EventHandle;
import org.projectforge.business.teamcal.event.ical.EventHandleError;
import org.projectforge.business.teamcal.event.ical.HandleMethod;
import org.projectforge.business.teamcal.event.ical.ICalHandler;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDao;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeStatus;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.teamcal.service.CryptService;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.mail.SendMail;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.fortuna.ical4j.model.parameter.CuType;
import net.fortuna.ical4j.model.parameter.Role;

@PrepareForTest({ ThreadLocalUserContext.class, ConfigXml.class })
@PowerMockIgnore({ "javax.management.*", "javax.xml.parsers.*", "com.sun.org.apache.xerces.internal.jaxp.*", "ch.qos.logback.*", "org.slf4j.*" })
public class ICalHandlerTest extends PowerMockTestCase
{
  @InjectMocks
  public TeamEventService eventService = new TeamEventServiceImpl();

  @Mock
  private AddressDao addressDao;

  @Mock
  private TeamEventAttendeeDao teamEventAttendeeDao;

  @Mock
  private TeamEventDao teamEventDao;

  @Mock
  private SendMail sendMail;

  @Mock
  private UserService userService;

  @Mock
  private CryptService cryptService;

  @Mock
  private ConfigurationService configService;

  @BeforeMethod
  public void setUp()
  {
    Mockito.reset(teamEventDao);

    MockitoAnnotations.initMocks(this);
    mockStatic(ThreadLocalUserContext.class);
    mockStatic(ConfigXml.class);
    Locale locale = Locale.getDefault();
    TimeZone timeZone = TimeZone.getDefault();
    PowerMockito.when(ThreadLocalUserContext.getLocale()).thenReturn(locale);
    PowerMockito.when(ThreadLocalUserContext.getTimeZone()).thenReturn(timeZone);
  }

  @Test
  public void testICalHandlerInput() throws IOException
  {
    ArgumentCaptor<TeamEventDO> savedEvent = ArgumentCaptor.forClass(TeamEventDO.class);

    TeamCalDO calendar = new TeamCalDO();
    ICalHandler handler = eventService.getEventHandler(calendar);
    Assert.assertNotNull(handler);

    boolean result = handler.readICal(IOUtils.toString(this.getClass().getResourceAsStream("/ical/ical_test_input.ics"), "UTF-8"), HandleMethod.ADD_UPDATE);
    Assert.assertTrue(result);

    result = handler.validate();
    Assert.assertTrue(result);
    handler.persist(true);

    Mockito.verify(teamEventDao).save(savedEvent.capture());
    TeamEventDO event = savedEvent.getValue();
    Assert.assertNotNull(event);

    Assert.assertEquals(calendar, event.getCalendar());
    Assert.assertEquals("ICAL_IMPORT_UID_VALUE_UPDATE_IF_REQUIRED", event.getUid());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2017-06-22 15:07:52.000", DateHelper.UTC).getTime(), event.getDtStamp().getTime());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2020-01-01 12:00:00.000", DateHelper.EUROPE_BERLIN).getTime(), event.getStartDate().getTime());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2020-01-01 13:00:00.000", DateHelper.EUROPE_BERLIN).getTime(), event.getEndDate().getTime());
    Assert.assertEquals("Test ical import web frontend", event.getSubject());
    Assert.assertEquals(Integer.valueOf(0), event.getSequence());
    Assert.assertEquals("mailto:organizer@example.com", event.getOrganizer());
    Assert.assertEquals(3, event.getAttendees().size());

    for (TeamEventAttendeeDO attendee : event.getAttendees()) {
      if (attendee.getUrl().equals("organizer@example.com")) {
        validateAttendee(attendee, CuType.INDIVIDUAL, TeamEventAttendeeStatus.ACCEPTED, Role.CHAIR, null, "Organizer", null);
      } else if (attendee.getUrl().equals("a1@example.com")) {
        validateAttendee(attendee, CuType.INDIVIDUAL, TeamEventAttendeeStatus.NEEDS_ACTION, null, Boolean.TRUE, "Attendee1", null);
      } else if (attendee.getUrl().equals("a2@example.com")) {
        validateAttendee(attendee, CuType.INDIVIDUAL, TeamEventAttendeeStatus.NEEDS_ACTION, null, Boolean.TRUE, "Attendee2", null);
      } else {
        Assert.fail("Unknown attendee " + attendee.getUrl());
      }
    }
  }

  @Test
  public void testICalHandlerCalendarMissing() throws IOException
  {
    ICalHandler handler = eventService.getEventHandler(null);
    Assert.assertNotNull(handler);

    boolean result = handler.readICal(IOUtils.toString(this.getClass().getResourceAsStream("/ical/ical_test_input.ics"), "UTF-8"), HandleMethod.ADD_UPDATE);
    Assert.assertTrue(result);
    Assert.assertEquals(1, handler.eventCount());

    result = handler.validate();
    Assert.assertFalse(result);

    final EventHandle handle = handler.getSingleEventHandles().get(0);
    Assert.assertEquals(1, handle.getErrors().size());
    Assert.assertEquals(EventHandleError.CALANDER_NOT_SPECIFIED, handle.getErrors().get(0));
  }

  @Test
  public void testICalHandlerReadMethod1() throws IOException
  {
    ICalHandler handler = eventService.getEventHandler(null);
    Assert.assertNotNull(handler);

    boolean result = handler.readICal(IOUtils.toString(this.getClass().getResourceAsStream("/ical/ical_test_input.ics"), "UTF-8"), HandleMethod.CANCEL);
    Assert.assertTrue(result);
    Assert.assertEquals(1, handler.eventCount());

    result = handler.validate();
    Assert.assertFalse(result);

    final EventHandle handle = handler.getSingleEventHandles().get(0);
    Assert.assertEquals(HandleMethod.ADD_UPDATE, handle.getMethod());

  }

  @Test
  public void testICalHandlerReadMethod2() throws IOException
  {
    ICalHandler handler = eventService.getEventHandler(null);
    Assert.assertNotNull(handler);

    boolean result = handler.readICal(IOUtils.toString(this.getClass().getResourceAsStream("/ical/normal_delete.ics"), "UTF-8"), HandleMethod.ADD_UPDATE);
    Assert.assertTrue(result);
    Assert.assertEquals(1, handler.eventCount());

    result = handler.validate();
    Assert.assertFalse(result);

    final EventHandle handle = handler.getSingleEventHandles().get(0);
    Assert.assertEquals(HandleMethod.CANCEL, handle.getMethod());
  }

  @Test
  public void testICalHandlerOutdated() throws IOException
  {
    ArgumentCaptor<TeamEventDO> savedEvent = ArgumentCaptor.forClass(TeamEventDO.class);
    ArgumentCaptor<TeamEventDO> updatedEvent = ArgumentCaptor.forClass(TeamEventDO.class);
    TeamCalDO calendar = new TeamCalDO();
    calendar.setId(100);

    ICalHandler handler = eventService.getEventHandler(calendar);
    Assert.assertNotNull(handler);

    boolean result = handler.readICal(IOUtils.toString(this.getClass().getResourceAsStream("/ical/ical_test_input.ics"), "UTF-8"), HandleMethod.ADD_UPDATE);
    Assert.assertTrue(result);

    result = handler.validate();
    Assert.assertTrue(result);
    handler.persist(true);
    Mockito.verify(teamEventDao).save(savedEvent.capture());

    when(eventService.findByUid(100, "ICAL_IMPORT_UID_VALUE_UPDATE_IF_REQUIRED", false)).thenReturn(savedEvent.getValue());

    // outdated
    handler = eventService.getEventHandler(calendar);
    Assert.assertNotNull(handler);

    result = handler.readICal(IOUtils.toString(this.getClass().getResourceAsStream("/ical/ical_test_input-outdated.ics"), "UTF-8"), HandleMethod.ADD_UPDATE);
    Assert.assertTrue(result);
    Assert.assertEquals(1, handler.eventCount());

    result = handler.validate();
    Assert.assertTrue(result);

    final EventHandle handle = handler.getSingleEventHandles().get(0);
    Assert.assertEquals(1, handle.getWarnings().size());
    Assert.assertEquals(EventHandleError.WARN_OUTDATED, handle.getWarnings().get(0));

    handler.persist(false);
    Mockito.verify(teamEventDao, Mockito.never()).internalUpdate(updatedEvent.capture(), Mockito.eq(true));

    handler.persist(true);
    Mockito.verify(teamEventDao).internalUpdate(updatedEvent.capture(), Mockito.eq(true));
    TeamEventDO event = updatedEvent.getValue();
    Assert.assertNotNull(event);

    Assert.assertEquals(calendar, event.getCalendar());
    Assert.assertEquals("ICAL_IMPORT_UID_VALUE_UPDATE_IF_REQUIRED", event.getUid());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2017-06-21 15:07:52.000", DateHelper.UTC).getTime(), event.getDtStamp().getTime());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2020-01-01 12:00:00.000", DateHelper.EUROPE_BERLIN).getTime(), event.getStartDate().getTime());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2020-01-01 13:00:00.000", DateHelper.EUROPE_BERLIN).getTime(), event.getEndDate().getTime());
    Assert.assertEquals("Test ical import web frontend (edited)", event.getSubject());
    Assert.assertEquals(Integer.valueOf(0), event.getSequence());
    Assert.assertEquals("mailto:organizer@example.com", event.getOrganizer());
    Assert.assertEquals(3, event.getAttendees().size());

    for (TeamEventAttendeeDO attendee : event.getAttendees()) {
      if (attendee.getUrl().equals("organizer@example.com")) {
        validateAttendee(attendee, CuType.INDIVIDUAL, TeamEventAttendeeStatus.ACCEPTED, Role.CHAIR, null, "Organizer", null);
      } else if (attendee.getUrl().equals("a1@example.com")) {
        validateAttendee(attendee, CuType.INDIVIDUAL, TeamEventAttendeeStatus.NEEDS_ACTION, null, Boolean.TRUE, "Attendee1", null);
      } else if (attendee.getUrl().equals("a2@example.com")) {
        validateAttendee(attendee, CuType.INDIVIDUAL, TeamEventAttendeeStatus.NEEDS_ACTION, null, Boolean.TRUE, "Attendee2", null);
      } else {
        Assert.fail("Unknown attendee " + attendee.getUrl());
      }
    }
  }

  @Test
  public void testICalHandlerNormal() throws IOException
  {
    TeamCalDO calendar = new TeamCalDO();
    calendar.setId(100);
    ArgumentCaptor<TeamEventDO> savedEvent = ArgumentCaptor.forClass(TeamEventDO.class);
    ArgumentCaptor<TeamEventDO> updatedEvent = ArgumentCaptor.forClass(TeamEventDO.class);
    ArgumentCaptor<TeamEventDO> deletedEvent = ArgumentCaptor.forClass(TeamEventDO.class);

    // invite --------------------------------------------------------------------------------------------------------------------
    ICalHandler handler = eventService.getEventHandler(calendar);
    Assert.assertNotNull(handler);

    boolean result = handler.readICal(IOUtils.toString(this.getClass().getResourceAsStream("/ical/normal_invite.ics"), "UTF-8"), HandleMethod.ADD_UPDATE);
    Assert.assertTrue(result);

    result = handler.validate();
    Assert.assertTrue(result);
    handler.persist(true);

    Mockito.verify(teamEventDao).save(savedEvent.capture());
    TeamEventDO event = savedEvent.getValue();
    Assert.assertNotNull(event);

    Assert.assertEquals(calendar, event.getCalendar());
    Assert.assertEquals("ICAL_UID_NORMAL_EVENT_TEST", event.getUid());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2017-06-21 06:34:14.000", DateHelper.UTC).getTime(), event.getDtStamp().getTime());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2021-01-01 12:00:00.000", DateHelper.EUROPE_BERLIN).getTime(), event.getStartDate().getTime());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2021-01-01 13:00:00.000", DateHelper.EUROPE_BERLIN).getTime(), event.getEndDate().getTime());
    Assert.assertEquals("Test normaler Event", event.getSubject());
    Assert.assertEquals(Integer.valueOf(0), event.getSequence());
    Assert.assertEquals("mailto:owner@example.com", event.getOrganizer());
    Assert.assertEquals(null, event.getLocation());
    Assert.assertEquals(2, event.getAttendees().size());

    for (TeamEventAttendeeDO attendee : event.getAttendees()) {
      if (attendee.getUrl().equals("owner@example.com")) {
        validateAttendee(attendee, CuType.INDIVIDUAL, TeamEventAttendeeStatus.ACCEPTED, Role.CHAIR, null, "Owner", null);
      } else if (attendee.getUrl().equals("a1@example.com")) {
        validateAttendee(attendee, CuType.INDIVIDUAL, TeamEventAttendeeStatus.NEEDS_ACTION, null, null, "Attendee", null);
      } else {
        Assert.fail("Unknown attendee " + attendee.getUrl());
      }
    }

    when(eventService.findByUid(Mockito.eq(100), Mockito.eq("ICAL_UID_NORMAL_EVENT_TEST"), Mockito.anyBoolean())).thenReturn(event);

    // edit ----------------------------------------------------------------------------------------------------------------------
    handler = eventService.getEventHandler(calendar);
    Assert.assertNotNull(handler);

    result = handler.readICal(IOUtils.toString(this.getClass().getResourceAsStream("/ical/normal_edit.ics"), "UTF-8"), HandleMethod.ADD_UPDATE);
    Assert.assertTrue(result);

    result = handler.validate();
    Assert.assertTrue(result);
    handler.persist(true);

    Mockito.verify(teamEventDao).internalUpdate(updatedEvent.capture(), Mockito.eq(true));
    event = updatedEvent.getValue();
    Assert.assertNotNull(event);

    Assert.assertEquals(calendar, event.getCalendar());
    Assert.assertEquals("ICAL_UID_NORMAL_EVENT_TEST", event.getUid());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2017-06-21 06:49:49.000", DateHelper.UTC).getTime(), event.getDtStamp().getTime());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2021-01-01 12:00:00.000", DateHelper.EUROPE_BERLIN).getTime(), event.getStartDate().getTime());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2021-01-01 13:00:00.000", DateHelper.EUROPE_BERLIN).getTime(), event.getEndDate().getTime());
    Assert.assertEquals("Test normaler Event (angepasst)", event.getSubject());
    Assert.assertEquals(Integer.valueOf(0), event.getSequence());
    Assert.assertEquals("mailto:owner@example.com", event.getOrganizer());
    Assert.assertEquals("Hinzugefügter Ort", event.getLocation());
    Assert.assertEquals(2, event.getAttendees().size());

    for (TeamEventAttendeeDO attendee : event.getAttendees()) {
      if (attendee.getUrl().equals("owner@example.com")) {
        validateAttendee(attendee, CuType.INDIVIDUAL, TeamEventAttendeeStatus.ACCEPTED, Role.CHAIR, null, "Owner", null);
      } else if (attendee.getUrl().equals("a1@example.com")) {
        validateAttendee(attendee, CuType.INDIVIDUAL, TeamEventAttendeeStatus.NEEDS_ACTION, null, null, "Attendee", null);
      } else {
        Assert.fail("Unknown attendee " + attendee.getUrl());
      }
    }

    // delete --------------------------------------------------------------------------------------------------------------------
    handler = eventService.getEventHandler(calendar);
    Assert.assertNotNull(handler);

    result = handler.readICal(IOUtils.toString(this.getClass().getResourceAsStream("/ical/normal_delete.ics"), "UTF-8"), HandleMethod.CANCEL);
    Assert.assertTrue(result);

    result = handler.validate();
    Assert.assertTrue(result);
    handler.persist(true);

    Mockito.verify(teamEventDao).markAsDeleted(deletedEvent.capture());
  }

  @Test
  public void testICalHandlerEventToDeleteNotFound() throws IOException
  {
    TeamCalDO calendar = new TeamCalDO();
    calendar.setId(100);

    // delete --------------------------------------------------------------------------------------------------------------------
    ICalHandler handler = eventService.getEventHandler(calendar);
    Assert.assertNotNull(handler);

    boolean result = handler.readICal(IOUtils.toString(this.getClass().getResourceAsStream("/ical/normal_delete.ics"), "UTF-8"), HandleMethod.CANCEL);
    Assert.assertTrue(result);
    Assert.assertEquals(1, handler.eventCount());

    result = handler.validate();
    Assert.assertTrue(result);

    final EventHandle handle = handler.getSingleEventHandles().get(0);
    Assert.assertEquals(1, handle.getWarnings().size());
    Assert.assertEquals(EventHandleError.WARN_EVENT_TO_DELETE_NOT_FOUND, handle.getWarnings().get(0));
  }

  @Test
  public void testICalHandlerRecurrency() throws IOException
  {
    TeamCalDO calendar = new TeamCalDO();
    calendar.setId(100);
    ArgumentCaptor<TeamEventDO> savedEvent = ArgumentCaptor.forClass(TeamEventDO.class);
    ArgumentCaptor<TeamEventDO> updatedEvent = ArgumentCaptor.forClass(TeamEventDO.class);
    ArgumentCaptor<TeamEventDO> deletedEvent = ArgumentCaptor.forClass(TeamEventDO.class);

    // invite --------------------------------------------------------------------------------------------------------------------
    ICalHandler handler = eventService.getEventHandler(calendar);
    Assert.assertNotNull(handler);

    boolean result = handler.readICal(IOUtils.toString(this.getClass().getResourceAsStream("/ical/recurring_invite.ics"), "UTF-8"), HandleMethod.ADD_UPDATE);
    Assert.assertTrue(result);

    result = handler.validate();
    Assert.assertTrue(result);
    handler.persist(true);

    Mockito.verify(teamEventDao).save(savedEvent.capture());
    TeamEventDO event = savedEvent.getValue();
    Assert.assertNotNull(event);

    Assert.assertEquals(calendar, event.getCalendar());
    Assert.assertEquals("366F19E0-1602-4D58-B303-E1D58AF4D227", event.getUid());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2017-07-04 15:13:03.000", DateHelper.UTC).getTime(), event.getDtStamp().getTime());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2017-07-03 14:45:00.000", DateHelper.EUROPE_BERLIN).getTime(), event.getStartDate().getTime());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2017-07-03 15:00:00.000", DateHelper.EUROPE_BERLIN).getTime(), event.getEndDate().getTime());
    Assert.assertEquals("Daily", event.getSubject());
    Assert.assertEquals(Integer.valueOf(0), event.getSequence());
    Assert.assertEquals("mailto:organizer@example.com", event.getOrganizer());
    Assert.assertEquals(null, event.getLocation());
    Assert.assertEquals("", event.getNote());
    Assert.assertEquals(2, event.getAttendees().size());

    for (TeamEventAttendeeDO attendee : event.getAttendees()) {
      if (attendee.getUrl().equals("organizer@example.com")) {
        validateAttendee(attendee, CuType.INDIVIDUAL, TeamEventAttendeeStatus.ACCEPTED, Role.CHAIR, null, "Organizer", null);
      } else if (attendee.getUrl().equals("a1@example.com")) {
        validateAttendee(attendee, CuType.INDIVIDUAL, TeamEventAttendeeStatus.NEEDS_ACTION, null, Boolean.TRUE, "Attendee1", null);
      } else {
        Assert.fail("Unknown attendee " + attendee.getUrl());
      }
    }

    Assert.assertEquals("FREQ=DAILY", event.getRecurrenceRule());
    Assert.assertEquals(null, event.getRecurrenceUntil());
    Assert.assertEquals(null, event.getRecurrenceExDate());
    Assert.assertEquals(null, event.getRecurrenceReferenceId());

    when(eventService.findByUid(Mockito.eq(100), Mockito.eq("366F19E0-1602-4D58-B303-E1D58AF4D227"), Mockito.anyBoolean())).thenReturn(event);

    // add exception -------------------------------------------------------------------------------------------------------------
    handler = eventService.getEventHandler(calendar);
    Assert.assertNotNull(handler);

    result = handler.readICal(IOUtils.toString(this.getClass().getResourceAsStream("/ical/recurring_add_exception.ics"), "UTF-8"), HandleMethod.ADD_UPDATE);
    Assert.assertTrue(result);

    result = handler.validate();
    Assert.assertTrue(result);
    handler.persist(true);

    Mockito.verify(teamEventDao, Mockito.times(2)).save(savedEvent.capture());
    event = savedEvent.getValue();
    Assert.assertNotNull(event);

    Assert.assertEquals(calendar, event.getCalendar());
    Assert.assertEquals(null, event.getUid());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2017-07-04 15:13:44.000", DateHelper.UTC).getTime(), event.getDtStamp().getTime());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2017-07-06 16:00:00.000", DateHelper.EUROPE_BERLIN).getTime(), event.getStartDate().getTime());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2017-07-06 16:15:00.000", DateHelper.EUROPE_BERLIN).getTime(), event.getEndDate().getTime());
    Assert.assertEquals("Daily", event.getSubject());
    Assert.assertEquals(Integer.valueOf(0), event.getSequence());
    Assert.assertEquals("mailto:organizer@example.com", event.getOrganizer());
    Assert.assertEquals(null, event.getLocation());
    Assert.assertEquals("", event.getNote());
    Assert.assertEquals(2, event.getAttendees().size());

    for (TeamEventAttendeeDO attendee : event.getAttendees()) {
      if (attendee.getUrl().equals("organizer@example.com")) {
        validateAttendee(attendee, CuType.INDIVIDUAL, TeamEventAttendeeStatus.NEEDS_ACTION, Role.CHAIR, Boolean.TRUE, "Organizer", null);
      } else if (attendee.getUrl().equals("a1@example.com")) {
        validateAttendee(attendee, CuType.INDIVIDUAL, TeamEventAttendeeStatus.NEEDS_ACTION, null, Boolean.TRUE, "Attendee1", null);
      } else {
        Assert.fail("Unknown attendee " + attendee.getUrl());
      }
    }

    Assert.assertEquals(null, event.getRecurrenceRule());
    Assert.assertEquals(null, event.getRecurrenceUntil());
    Assert.assertEquals(null, event.getRecurrenceExDate());
    Assert.assertEquals("20170706T124500", event.getRecurrenceReferenceId());

    // edit ----------------------------------------------------------------------------------------------------------------------
    handler = eventService.getEventHandler(calendar);
    Assert.assertNotNull(handler);

    result = handler.readICal(IOUtils.toString(this.getClass().getResourceAsStream("/ical/recurring_update_futur_events.ics"), "UTF-8"),
        HandleMethod.ADD_UPDATE);
    Assert.assertTrue(result);

    result = handler.validate();
    Assert.assertTrue(result);
    Assert.assertEquals(1, handler.eventCount());
    handler.persist(true);

    Mockito.verify(teamEventDao, Mockito.times(3)).save(savedEvent.capture());
    Mockito.verify(teamEventDao).internalUpdate(updatedEvent.capture(), Mockito.eq(true));
    event = savedEvent.getValue();
    Assert.assertNotNull(event);

    Assert.assertEquals(calendar, event.getCalendar());
    Assert.assertEquals(null, event.getUid());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2017-07-05 05:28:46.000", DateHelper.UTC).getTime(), event.getDtStamp().getTime());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2017-07-06 16:00:00.000", DateHelper.EUROPE_BERLIN).getTime(), event.getStartDate().getTime());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2017-07-06 16:15:00.000", DateHelper.EUROPE_BERLIN).getTime(), event.getEndDate().getTime());
    Assert.assertEquals("Daily", event.getSubject());
    Assert.assertEquals(Integer.valueOf(0), event.getSequence());
    Assert.assertEquals("mailto:organizer@example.com", event.getOrganizer());
    Assert.assertEquals("this place", event.getLocation());
    Assert.assertEquals("", event.getNote());
    Assert.assertEquals(2, event.getAttendees().size());

    for (TeamEventAttendeeDO attendee : event.getAttendees()) {
      if (attendee.getUrl().equals("organizer@example.com")) {
        validateAttendee(attendee, CuType.INDIVIDUAL, TeamEventAttendeeStatus.ACCEPTED, Role.CHAIR, null, "Organizer", null);
      } else if (attendee.getUrl().equals("a1@example.com")) {
        validateAttendee(attendee, CuType.INDIVIDUAL, TeamEventAttendeeStatus.NEEDS_ACTION, null, Boolean.TRUE, "Attendee1", null);
      } else {
        Assert.fail("Unknown attendee " + attendee.getUrl());
      }
    }

    Assert.assertEquals(null, event.getRecurrenceRule());
    Assert.assertEquals(null, event.getRecurrenceUntil());
    Assert.assertEquals(null, event.getRecurrenceExDate());
    Assert.assertEquals("20170706T124500", event.getRecurrenceReferenceId());

    event = updatedEvent.getValue();
    Assert.assertNotNull(event);

    Assert.assertEquals(calendar, event.getCalendar());
    Assert.assertEquals("366F19E0-1602-4D58-B303-E1D58AF4D227", event.getUid());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2017-07-05 05:28:46.000", DateHelper.UTC).getTime(), event.getDtStamp().getTime());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2017-07-04 14:45:00.000", DateHelper.EUROPE_BERLIN).getTime(), event.getStartDate().getTime());
    Assert.assertEquals(DateHelper.parseIsoTimestamp("2017-07-04 15:00:00.000", DateHelper.EUROPE_BERLIN).getTime(), event.getEndDate().getTime());
    Assert.assertEquals("Daily", event.getSubject());
    Assert.assertEquals(Integer.valueOf(0), event.getSequence());
    Assert.assertEquals("mailto:organizer@example.com", event.getOrganizer());
    Assert.assertEquals("this place", event.getLocation());
    Assert.assertEquals("", event.getNote());
    Assert.assertEquals(2, event.getAttendees().size());

    for (TeamEventAttendeeDO attendee : event.getAttendees()) {
      if (attendee.getUrl().equals("organizer@example.com")) {
        validateAttendee(attendee, CuType.INDIVIDUAL, TeamEventAttendeeStatus.ACCEPTED, Role.CHAIR, null, "Organizer", null);
      } else if (attendee.getUrl().equals("a1@example.com")) {
        validateAttendee(attendee, CuType.INDIVIDUAL, TeamEventAttendeeStatus.NEEDS_ACTION, null, Boolean.TRUE, "Attendee1", null);
      } else {
        Assert.fail("Unknown attendee " + attendee.getUrl());
      }
    }

    Assert.assertEquals("FREQ=DAILY", event.getRecurrenceRule());
    Assert.assertEquals(null, event.getRecurrenceUntil());
    Assert.assertEquals(null, event.getRecurrenceExDate());
    Assert.assertEquals(null, event.getRecurrenceReferenceId());

    when(eventService.findByUid(Mockito.eq(100), Mockito.eq("366F19E0-1602-4D58-B303-E1D58AF4D227"), Mockito.anyBoolean())).thenReturn(event);

    // delete --------------------------------------------------------------------------------------------------------------------
    handler = eventService.getEventHandler(calendar);
    Assert.assertNotNull(handler);

    result = handler.readICal(IOUtils.toString(this.getClass().getResourceAsStream("/ical/recurring_delete.ics"), "UTF-8"), HandleMethod.CANCEL);
    Assert.assertTrue(result);

    result = handler.validate();
    Assert.assertTrue(result);
    handler.persist(true);

    Mockito.verify(teamEventDao).markAsDeleted(deletedEvent.capture());
  }

  void validateAttendee(final TeamEventAttendeeDO attendee, final CuType cuType, final TeamEventAttendeeStatus status,
      final Role role, final Boolean rsvp, final String cn, final String additionalParams)
  {
    if (cuType != null)
      Assert.assertEquals(cuType.getValue(), attendee.getCuType());
    else
      Assert.assertNull(attendee.getCuType());

    if (status != null)
      Assert.assertEquals(status, attendee.getStatus());
    else
      Assert.assertNull(attendee.getStatus());

    if (role != null)
      Assert.assertEquals(role.getValue(), attendee.getRole());
    else
      Assert.assertNull(attendee.getRole());

    if (rsvp != null)
      Assert.assertEquals(rsvp, attendee.getRsvp());
    else
      Assert.assertNull(attendee.getRsvp());

    if (cn != null)
      Assert.assertEquals(cn, attendee.getCommonName());
    else
      Assert.assertNull(attendee.getCommonName());

    if (additionalParams != null)
      Assert.assertEquals(attendee, attendee.getAdditionalParams());
    else
      Assert.assertNull(attendee.getAdditionalParams());
  }

}

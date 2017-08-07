package org.projectforge.web.teamcal.event;

import static org.powermock.api.mockito.PowerMockito.mockStatic;

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
import org.projectforge.business.teamcal.event.ical.HandleMethod;
import org.projectforge.business.teamcal.event.ical.ICalHandler;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDao;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.teamcal.service.CryptService;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.mail.SendMail;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
    MockitoAnnotations.initMocks(this);
    mockStatic(ThreadLocalUserContext.class);
    mockStatic(ConfigXml.class);
    Locale locale = Locale.getDefault();
    TimeZone timeZone = TimeZone.getDefault();
    PowerMockito.when(ThreadLocalUserContext.getLocale()).thenReturn(locale);
    PowerMockito.when(ThreadLocalUserContext.getTimeZone()).thenReturn(timeZone);
  }

  @Test
  public void testICalHandlerTestInput1() throws IOException
  {
    ArgumentCaptor<TeamEventDO> savedEvent = ArgumentCaptor.forClass(TeamEventDO.class);

    TeamCalDO calendar = new TeamCalDO();
    ICalHandler handler = eventService.getEventHandler(calendar);
    Assert.assertNotNull(handler);

    boolean result = handler.readICal(IOUtils.toString(this.getClass().getResourceAsStream("/ical/ical_test_input1.ics"), "UTF-8"), HandleMethod.ADD_UPDATE);
    Assert.assertTrue(result);

    result = handler.validate();
    Assert.assertTrue(result);
    handler.persistErrorFree();

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

    //        ATTENDEE;CUTYPE=INDIVIDUAL;PARTSTAT=ACCEPTED;ROLE=CHAIR;CN=Organizer:mailto:organizer@example.com
    //      ATTENDEE;CN=Attendee1;CUTYPE=INDIVIDUAL;PARTSTAT=NEEDS-ACTION;RSVP=TRUE:mailto:a1@example.com
    //      ATTENDEE;CN=Attendee2;CUTYPE=INDIVIDUAL;PARTSTAT=NEEDS-ACTION;RSVP=TRUE:mailto:a2@example.com
  }
}

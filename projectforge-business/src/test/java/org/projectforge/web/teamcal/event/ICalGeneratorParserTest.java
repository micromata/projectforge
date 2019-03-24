/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.web.teamcal.event;

import static org.projectforge.business.teamcal.event.ical.ICalConverterStore.*;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.projectforge.business.teamcal.event.ical.ICalGenerator;
import org.projectforge.business.teamcal.event.ical.ICalParser;
import org.projectforge.business.teamcal.event.model.ReminderActionType;
import org.projectforge.business.teamcal.event.model.ReminderDurationUnit;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeStatus;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.test.AbstractTestNGBase;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.fortuna.ical4j.model.parameter.CuType;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.Method;

public class ICalGeneratorParserTest extends AbstractTestNGBase
{

  @Override
  @BeforeClass
  public void setUp()
  {
    super.setUp();
    PFUserDO user = new PFUserDO();
    user.setUsername("UserName");
    user.setFirstname("FirstName");
    user.setLastname("LastName");
    user.setTimeZone(DateHelper.EUROPE_BERLIN);
    ThreadLocalUserContext.setUser(getUserGroupCache(), PFUserDO.createCopyWithoutSecretFields(user));
  }

  @Test
  public void testICalGenerator()
  {
    TeamEventDO event = new TeamEventDO();

    // set alarm
    event.setReminderActionType(ReminderActionType.MESSAGE);
    event.setReminderDuration(100);
    event.setReminderDurationUnit(ReminderDurationUnit.MINUTES);

    // set Attendees
    TeamEventAttendeeDO attendee = new TeamEventAttendeeDO();
    attendee.setUrl("test@test.de");
    event.addAttendee(attendee);

    // set creator
    PFUserDO user = new PFUserDO();
    user.setUsername("UserName");
    user.setFirstname("FirstName");
    user.setLastname("LastName");
    user.setTimeZone(DateHelper.EUROPE_BERLIN);
    event.setCreator(user);

    // set description
    event.setSubject("subject");

    // set dt end
    event.setEndDate(new Timestamp(DateHelper.parseIsoTimestamp("2017-07-31 00:00:00.000", DateHelper.EUROPE_BERLIN).getTime()));

    // set dt stamp
    event.setDtStamp(new Timestamp(DateHelper.parseIsoTimestamp("2017-07-30 12:00:00.000", DateHelper.EUROPE_BERLIN).getTime()));

    // set dt start
    event.setStartDate(new Timestamp(DateHelper.parseIsoTimestamp("2017-07-31 12:00:00.000", DateHelper.EUROPE_BERLIN).getTime()));

    // set location
    event.setLocation("location");

    // set organizer
    event.setOrganizer("organizer");

    // set sequence
    event.setSequence(5);

    // set summary
    event.setNote("summary");

    // set uid
    event.setUid("uid string");

    ICalGenerator generatorFull = ICalGenerator.exportAllFields();

    generatorFull.addEvent(event);

    Assert.assertFalse(generatorFull.isEmpty());

    String ical = new String(generatorFull.getCalendarAsByteStream().toByteArray());
    ical = ical.replaceAll("\r\n", ""); // remove linebreaks to enable a simple testing

    // set alarm
    Assert.assertTrue(ical.contains("BEGIN:VALARM"));
    Assert.assertTrue(ical.contains("TRIGGER:-PT100M"));
    Assert.assertTrue(ical.contains("ACTION:DISPLAY"));
    Assert.assertTrue(ical.contains("END:VALARM"));

    // set Attendees
    Assert.assertTrue(ical.contains("ATTENDEE;CN=test@test.de;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT= NEEDS-ACTION:mailto:test@test.de"));

    // set description
    Assert.assertTrue(ical.contains("DESCRIPTION:summary"));

    // set dt end
    Assert.assertTrue(ical.contains("DTEND;TZID=Europe/Berlin:20170731T000000"));

    // set dt stamp
    Assert.assertTrue(ical.contains("DTSTAMP:20170730T100000Z"));

    // set dt start
    Assert.assertTrue(ical.contains("DTSTART;TZID=Europe/Berlin:20170731T120000"));

    // set location
    Assert.assertTrue(ical.contains("LOCATION:location"));

    // set organizer
    Assert.assertTrue(ical.contains("ORGANIZER;CUTYPE=INDIVIDUAL;ROLE=CHAIR;PARTSTAT=ACCEPTED:organizer"));

    // set sequence
    Assert.assertTrue(ical.contains("SEQUENCE:5"));

    // set summary
    Assert.assertTrue(ical.contains("SUMMARY:subject"));

    // set uid
    Assert.assertTrue(ical.contains("UID:uid string"));

    ICalGenerator generatorCancel = ICalGenerator.forMethod(Method.CANCEL);

    generatorCancel.addEvent(event);

    Assert.assertFalse(generatorCancel.isEmpty());

    ical = new String(generatorCancel.getCalendarAsByteStream().toByteArray());
    ical = ical.replaceAll("\r\n", ""); // remove linebreaks to enable a simple testing

    // set alarm
    Assert.assertFalse(ical.contains("BEGIN:VALARM"));
    Assert.assertFalse(ical.contains("TRIGGER:-PT100M"));
    Assert.assertFalse(ical.contains("ACTION:DISPLAY"));
    Assert.assertFalse(ical.contains("END:VALARM"));

    // set Attendees
    Assert.assertTrue(ical.contains("ATTENDEE;CN=test@test.de;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT= NEEDS-ACTION:mailto:test@test.de"));

    // set description
    Assert.assertFalse(ical.contains("DESCRIPTION:summary"));

    // set dt end
    Assert.assertFalse(ical.contains("DTEND;TZID=Europe/Berlin:20170731T000000"));

    // set dt stamp
    Assert.assertTrue(ical.contains("DTSTAMP:20170730T100000Z"));

    // set dt start
    Assert.assertTrue(ical.contains("DTSTART;TZID=Europe/Berlin:20170731T120000"));

    // set location
    Assert.assertFalse(ical.contains("LOCATION:location"));

    // set organizer
    Assert.assertTrue(ical.contains("ORGANIZER;CUTYPE=INDIVIDUAL;ROLE=CHAIR;PARTSTAT=ACCEPTED:organizer"));

    // set sequence
    Assert.assertTrue(ical.contains("SEQUENCE:5"));

    // set summary
    Assert.assertFalse(ical.contains("SUMMARY:subject"));

    // set uid
    Assert.assertTrue(ical.contains("UID:uid string"));
  }

  @Test
  public void testICalParser() throws IOException
  {
    ICalParser parser = ICalParser.parseAllFields();

    Assert.assertTrue(parser.parse(IOUtils.toString(this.getClass().getResourceAsStream("/ical/ical_test_input.ics"), "UTF-8")));

    // check event
    Assert.assertEquals(parser.getExtractedEvents().size(), 1);
    TeamEventDO event = parser.getExtractedEvents().get(0);
    Assert.assertNotNull(event);

    // VEVENT_DTSTART
    Assert.assertEquals(event.getStartDate().getTime(), DateHelper.parseIsoTimestamp("2020-01-01 12:00:00.000", DateHelper.EUROPE_BERLIN).getTime());
    // VEVENT_DTEND
    Assert.assertEquals(event.getEndDate().getTime(), DateHelper.parseIsoTimestamp("2020-01-01 13:00:00.000", DateHelper.EUROPE_BERLIN).getTime());
    // VEVENT_SUMMARY
    Assert.assertEquals(event.getSubject(), "Test ical import web frontend");
    // VEVENT_UID
    Assert.assertEquals(event.getUid(), "ICAL_IMPORT_UID_VALUE_UPDATE_IF_REQUIRED");
    // VEVENT_CREATED
    Assert.assertNull(event.getCreated());
    // VEVENT_LOCATION
    Assert.assertNull(event.getLocation());
    // VEVENT_DTSTAMP
    Assert.assertEquals(event.getDtStamp().getTime(), DateHelper.parseIsoTimestamp("2017-06-22 15:07:52.000", DateHelper.UTC).getTime());
    // VEVENT_LAST_MODIFIED
    Assert.assertNull(event.getLastUpdate());
    // VEVENT_SEQUENCE
    Assert.assertEquals(event.getSequence(), Integer.valueOf(0));
    // VEVENT_ORGANIZER
    Assert.assertEquals(event.getOrganizer(), "mailto:organizer@example.com");
    Assert.assertEquals(event.getOrganizerAdditionalParams(), "CN=Organizer");
    // VEVENT_TRANSP
    // currently not implemented
    // VEVENT_ALARM
    Assert.assertNull(event.getReminderActionType());
    // VEVENT_DESCRIPTION
    Assert.assertNull(event.getNote());
    // VEVENT_ATTENDEES
    Assert.assertEquals(event.getAttendees().size(), 3);
    for (TeamEventAttendeeDO attendee : event.getAttendees()) {
      Assert.assertTrue(attendee.getUrl().equals("organizer@example.com")
          || attendee.getUrl().equals("a1@example.com")
          || attendee.getUrl().equals("a2@example.com"));
    }
    // VEVENT_RRULE
    Assert.assertNull(event.getRecurrenceRuleObject());
    // VEVENT_EX_DATE
    Assert.assertNull(event.getRecurrenceExDate());
  }

  @Test
  public void textGenerateParse()
  {
    List<TeamEventDO> events = this.getEvents();
    ICalGenerator generatorFull = ICalGenerator.exportAllFields();
    ICalGenerator generatorCancel = ICalGenerator.forMethod(Method.CANCEL);
    ICalParser parser = ICalParser.parseAllFields();

    for (TeamEventDO event : events) {
      TeamEventDO full = this.generateParse(generatorFull, parser, event);
      TeamEventDO cancel = this.generateParse(generatorCancel, parser, event);

      this.compareEvents(generatorFull.getExportsVEvent(), event, full);
      this.compareEvents(generatorCancel.getExportsVEvent(), event, cancel);
    }
  }

  private TeamEventDO generateParse(final ICalGenerator generator, final ICalParser parser, final TeamEventDO event)
  {
    generator.reset();
    generator.addEvent(event);
    Assert.assertFalse(generator.isEmpty());
    String ics = new String(generator.getCalendarAsByteStream().toByteArray());
    parser.reset();
    Assert.assertTrue(parser.parse(ics));
    Assert.assertEquals(parser.getExtractedEvents().size(), 1);
    return parser.getExtractedEvents().get(0);
  }

  private List<TeamEventDO> getEvents()
  {
    final List<TeamEventDO> events = new ArrayList<>();

    // simple event
    TeamEventDO event = new TeamEventDO();

    event.setReminderActionType(ReminderActionType.MESSAGE);
    event.setReminderDuration(100);
    event.setReminderDurationUnit(ReminderDurationUnit.MINUTES);
    TeamEventAttendeeDO attendee = new TeamEventAttendeeDO();
    attendee.setUrl("test@test.de");
    attendee.setStatus(TeamEventAttendeeStatus.DECLINED);
    attendee.setCommonName("test");
    attendee.setRole(Role.ROLE);
    attendee.setCuType(CuType.INDIVIDUAL.getValue());
    attendee.setRsvp(true);
    event.addAttendee(attendee);
    event.setSubject("subject");
    event.setEndDate(new Timestamp(DateHelper.parseIsoTimestamp("2017-07-31 00:00:00.000", DateHelper.EUROPE_BERLIN).getTime()));
    event.setDtStamp(new Timestamp(DateHelper.parseIsoTimestamp("2017-07-30 12:00:00.000", DateHelper.EUROPE_BERLIN).getTime()));
    event.setStartDate(new Timestamp(DateHelper.parseIsoTimestamp("2017-07-31 12:00:00.000", DateHelper.EUROPE_BERLIN).getTime()));
    event.setLocation("location");
    event.setOrganizer("organizer");
    event.setSequence(5);
    event.setNote("summary");
    event.setUid("uid string");

    events.add(event);

    // all day event
    event = new TeamEventDO();

    event.setAllDay(true);
    event.setSubject("subject");
    event.setEndDate(new Timestamp(DateHelper.parseIsoTimestamp("2017-07-31 00:00:00.000", DateHelper.UTC).getTime()));
    event.setDtStamp(new Timestamp(DateHelper.parseIsoTimestamp("2017-07-30 12:00:00.000", DateHelper.UTC).getTime()));
    event.setStartDate(new Timestamp(DateHelper.parseIsoTimestamp("2017-08-31 00:00:00.000", DateHelper.UTC).getTime()));
    event.setOrganizer("organizer");
    event.setSequence(5);
    event.setUid("uid string");

    events.add(event);

    return events;
  }

  private void compareEvents(final List<String> fields, final TeamEventDO eventSrc, final TeamEventDO eventExtracted)
  {
    for (String field : fields) {
      if (VEVENT_DTSTART.equals(field)) {
        Assert.assertEquals(eventExtracted.getStartDate(), eventSrc.getStartDate());
      } else if (VEVENT_DTEND.equals(field)) {
        Assert.assertEquals(eventExtracted.getEndDate(), eventSrc.getEndDate());
      } else if (VEVENT_SUMMARY.equals(field)) {
        Assert.assertEquals(eventExtracted.getSubject(), eventSrc.getSubject());
      } else if (VEVENT_UID.equals(field)) {
        Assert.assertEquals(eventExtracted.getUid(), eventSrc.getUid());
      } else if (VEVENT_CREATED.equals(field)) {
        // currently not implemented
      } else if (VEVENT_LOCATION.equals(field)) {
        Assert.assertEquals(eventExtracted.getLocation(), eventSrc.getLocation());
      } else if (VEVENT_DTSTAMP.equals(field)) {
        Assert.assertEquals(eventExtracted.getDtStamp(), eventSrc.getDtStamp());
      } else if (VEVENT_LAST_MODIFIED.equals(field)) {
        // currently not implemented
      } else if (VEVENT_SEQUENCE.equals(field)) {
        Assert.assertEquals(eventExtracted.getSequence(), eventSrc.getSequence());
      } else if (VEVENT_ORGANIZER.equals(field)) {
        Assert.assertEquals(eventExtracted.getOrganizer(), eventSrc.getOrganizer());
        if (eventSrc.getOrganizerAdditionalParams() != null) {
          Assert.assertEquals(eventExtracted.getOrganizerAdditionalParams(), eventSrc.getOrganizerAdditionalParams());
        } else {
          Assert.assertEquals(eventExtracted.getOrganizerAdditionalParams(), "CUTYPE=INDIVIDUAL;ROLE=CHAIR;PARTSTAT=ACCEPTED");
        }
      } else if (VEVENT_TRANSP.equals(field)) {
        // currently not implemented
      } else if (VEVENT_ALARM.equals(field)) {
        Assert.assertEquals(eventExtracted.getReminderDuration(), eventSrc.getReminderDuration());
        Assert.assertEquals(eventExtracted.getReminderActionType(), eventSrc.getReminderActionType());
        Assert.assertEquals(eventExtracted.getReminderDurationUnit(), eventSrc.getReminderDurationUnit());
      } else if (VEVENT_DESCRIPTION.equals(field)) {
        Assert.assertEquals(eventExtracted.getNote(), eventSrc.getNote());
      } else if (VEVENT_ATTENDEES.equals(field)) {
        if (eventSrc.getAttendees() == null) {
          Assert.assertNull(eventExtracted.getAttendees());
        } else {
          Assert.assertEquals(eventExtracted.getAttendees().size(), eventSrc.getAttendees().size());

          for (TeamEventAttendeeDO attendee : eventSrc.getAttendees()) {
            TeamEventAttendeeDO found = null;

            for (TeamEventAttendeeDO attendee2 : eventSrc.getAttendees()) {
              if (attendee.getUrl().equals(attendee2.getUrl())) {
                found = attendee2;
                break;
              }
            }

            Assert.assertNotNull(found);
            Assert.assertEquals(found.getCommonName(), attendee.getCommonName());
            Assert.assertEquals(found.getStatus(), attendee.getStatus());
            Assert.assertEquals(found.getCuType(), attendee.getCuType());
            Assert.assertEquals(found.getRole(), attendee.getRole());
            Assert.assertEquals(found.getRsvp(), attendee.getRsvp());
            Assert.assertEquals(found.getAdditionalParams(), attendee.getAdditionalParams());
          }
        }
      } else if (VEVENT_RRULE.equals(field)) {
        Assert.assertEquals(eventExtracted.getRecurrenceRule(), eventSrc.getRecurrenceRule());
        Assert.assertEquals(eventExtracted.getRecurrenceDate(), eventSrc.getRecurrenceDate());
      } else if (VEVENT_RECURRENCE_ID.equals(field)) {
        // currently not implemented
      } else if (VEVENT_EX_DATE.equals(field)) {
        Assert.assertEquals(eventExtracted.getRecurrenceExDate(), eventSrc.getRecurrenceExDate());
      } else {
        throw new RuntimeException("Unknown field " + field);
      }
    }

    // check blank fields
    for (String field : FULL_LIST) {
      {
        if (fields.contains(field)) {
          continue;
        }

        if (VEVENT_DTSTART.equals(field)) {
          Assert.assertNull(eventExtracted.getStartDate());
        } else if (VEVENT_DTEND.equals(field)) {
          Assert.assertNull(eventExtracted.getEndDate());
        } else if (VEVENT_SUMMARY.equals(field)) {
          Assert.assertNull(eventExtracted.getSubject());
        } else if (VEVENT_UID.equals(field)) {
          Assert.assertNull(eventExtracted.getUid());
        } else if (VEVENT_CREATED.equals(field)) {
          Assert.assertNull(eventExtracted.getCreated());
        } else if (VEVENT_LOCATION.equals(field)) {
          Assert.assertNull(eventExtracted.getLocation());
        } else if (VEVENT_DTSTAMP.equals(field)) {
          Assert.assertNull(eventExtracted.getDtStamp());
        } else if (VEVENT_LAST_MODIFIED.equals(field)) {
          Assert.assertNull(eventExtracted.getLastUpdate());
        } else if (VEVENT_SEQUENCE.equals(field)) {
          Assert.assertNull(eventExtracted.getSequence());
        } else if (VEVENT_ORGANIZER.equals(field)) {
          Assert.assertNull(eventExtracted.getOrganizer());
          Assert.assertNull(eventExtracted.getOrganizerAdditionalParams());
        } else if (VEVENT_TRANSP.equals(field)) {
          // currently not implemented
        } else if (VEVENT_ALARM.equals(field)) {
          Assert.assertNull(eventExtracted.getReminderActionType());
          Assert.assertNull(eventExtracted.getReminderDuration());
          Assert.assertNull(eventExtracted.getReminderDurationUnit());
        } else if (VEVENT_DESCRIPTION.equals(field)) {
          Assert.assertNull(eventExtracted.getNote());
        } else if (VEVENT_ATTENDEES.equals(field)) {
          Assert.assertNull(eventExtracted.getAttendees());
        } else if (VEVENT_RRULE.equals(field)) {
          Assert.assertNull(eventExtracted.getRecurrenceRule());
          Assert.assertNull(eventExtracted.getRecurrenceDate());
        } else if (VEVENT_RECURRENCE_ID.equals(field)) {
          // currently not implemented
        } else if (VEVENT_EX_DATE.equals(field)) {
          Assert.assertNull(eventExtracted.getRecurrenceExDate());
        } else {
          throw new RuntimeException("Unknown field " + field);
        }
      }
    }
  }
}

/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.teamcal.event

import net.fortuna.ical4j.model.parameter.CuType
import net.fortuna.ical4j.model.parameter.Role
import net.fortuna.ical4j.model.property.Method
import org.apache.commons.io.IOUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.business.teamcal.event.ical.ICalConverterStore
import org.projectforge.business.teamcal.event.ical.ICalGenerator
import org.projectforge.business.teamcal.event.ical.ICalParser
import org.projectforge.business.teamcal.event.model.*
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.setUser
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.PFUserDO.Companion.createCopy
import org.projectforge.framework.time.DateHelper
import org.projectforge.test.AbstractTestBase
import java.io.IOException
import java.util.*

class ICalGeneratorParserTest : AbstractTestBase() {
    @BeforeEach
    fun setUp() {
        val user = PFUserDO()
        user.username = "UserName"
        user.firstname = "FirstName"
        user.lastname = "LastName"
        user.timeZone = DateHelper.EUROPE_BERLIN
        setUser(createCopy(user))
    }

    @Test
    fun testICalGenerator() {
        val event = TeamEventDO()

        // set alarm
        event.reminderActionType = ReminderActionType.MESSAGE
        event.reminderDuration = 100L
        event.reminderDurationUnit = ReminderDurationUnit.MINUTES

        // set Attendees
        val attendee = TeamEventAttendeeDO()
        attendee.url = "test@test.de"
        event.addAttendee(attendee)

        // set creator
        val user = PFUserDO()
        user.username = "UserName"
        user.firstname = "FirstName"
        user.lastname = "LastName"
        user.timeZone = DateHelper.EUROPE_BERLIN
        event.creator = user

        // set description
        event.subject = "subject"

        // set dt end
        event.endDate = Date(DateHelper.parseIsoTimestamp("2017-07-31 00:00:00.000", DateHelper.EUROPE_BERLIN).time)

        // set dt stamp
        event.dtStamp = Date(DateHelper.parseIsoTimestamp("2017-07-30 12:00:00.000", DateHelper.EUROPE_BERLIN).time)

        // set dt start
        event.startDate = Date(DateHelper.parseIsoTimestamp("2017-07-31 12:00:00.000", DateHelper.EUROPE_BERLIN).time)

        // set location
        event.location = "location"

        // set organizer
        event.organizer = "organizer"

        // set sequence
        event.sequence = 5

        // set summary
        event.note = "summary"

        // set uid
        event.uid = "uid string"

        val generatorFull = ICalGenerator.exportAllFields()

        generatorFull.addEvent(event)

        Assertions.assertFalse(generatorFull.isEmpty)

        var ical = String(generatorFull.calendarAsByteStream.toByteArray())
        ical = ical.replace("\r\n".toRegex(), "") // remove linebreaks to enable a simple testing

        // set alarm
        Assertions.assertTrue(ical.contains("BEGIN:VALARM"))
        Assertions.assertTrue(ical.contains("TRIGGER:-PT1H40"))
        Assertions.assertTrue(ical.contains("ACTION:DISPLAY"))
        Assertions.assertTrue(ical.contains("END:VALARM"))

        // set Attendees
        Assertions.assertTrue(ical.contains("ATTENDEE;CN=test@test.de;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT= NEEDS-ACTION:mailto:test@test.de"))

        // set description
        Assertions.assertTrue(ical.contains("DESCRIPTION:summary"))

        // set dt end
        Assertions.assertTrue(ical.contains("DTEND;TZID=Europe/Berlin:20170731T000000"))

        // set dt stamp
        Assertions.assertTrue(ical.contains("DTSTAMP:20170730T100000Z"))

        // set dt start
        Assertions.assertTrue(ical.contains("DTSTART;TZID=Europe/Berlin:20170731T120000"))

        // set location
        Assertions.assertTrue(ical.contains("LOCATION:location"))

        // set organizer
        Assertions.assertTrue(ical.contains("ORGANIZER;CUTYPE=INDIVIDUAL;ROLE=CHAIR;PARTSTAT=ACCEPTED:organizer"))

        // set sequence
        Assertions.assertTrue(ical.contains("SEQUENCE:5"))

        // set summary
        Assertions.assertTrue(ical.contains("SUMMARY:subject"))

        // set uid
        Assertions.assertTrue(ical.contains("UID:uid string"))

        val generatorCancel = ICalGenerator.forMethod(Method.CANCEL)

        generatorCancel.addEvent(event)

        Assertions.assertFalse(generatorCancel.isEmpty)

        ical = String(generatorCancel.calendarAsByteStream.toByteArray())
        ical = ical.replace("\r\n".toRegex(), "") // remove linebreaks to enable a simple testing

        // set alarm
        Assertions.assertFalse(ical.contains("BEGIN:VALARM"))
        Assertions.assertFalse(ical.contains("TRIGGER:-PT100M"))
        Assertions.assertFalse(ical.contains("ACTION:DISPLAY"))
        Assertions.assertFalse(ical.contains("END:VALARM"))

        // set Attendees
        Assertions.assertTrue(ical.contains("ATTENDEE;CN=test@test.de;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT= NEEDS-ACTION:mailto:test@test.de"))

        // set description
        Assertions.assertFalse(ical.contains("DESCRIPTION:summary"))

        // set dt end
        Assertions.assertFalse(ical.contains("DTEND;TZID=Europe/Berlin:20170731T000000"))

        // set dt stamp
        Assertions.assertTrue(ical.contains("DTSTAMP:20170730T100000Z"))

        // set dt start
        Assertions.assertTrue(ical.contains("DTSTART;TZID=Europe/Berlin:20170731T120000"))

        // set location
        Assertions.assertFalse(ical.contains("LOCATION:location"))

        // set organizer
        Assertions.assertTrue(ical.contains("ORGANIZER;CUTYPE=INDIVIDUAL;ROLE=CHAIR;PARTSTAT=ACCEPTED:organizer"))

        // set sequence
        Assertions.assertTrue(ical.contains("SEQUENCE:5"))

        // set summary
        Assertions.assertFalse(ical.contains("SUMMARY:subject"))

        // set uid
        Assertions.assertTrue(ical.contains("UID:uid string"))
    }

    @Test
    @Throws(IOException::class)
    fun testICalParser() {
        val parser = ICalParser.parseAllFields()

        Assertions.assertTrue(
            parser.parse(
                IOUtils.toString(
                    javaClass.getResourceAsStream("/ical/ical_test_input.ics"), "UTF-8"
                )
            )
        )

        // check event
        Assertions.assertEquals(parser.extractedEvents.size, 1)
        val event = parser.extractedEvents[0]
        Assertions.assertNotNull(event)

        // VEVENT_DTSTART
        Assertions.assertEquals(
            event.startDate!!.time,
            DateHelper.parseIsoTimestamp("2020-01-01 12:00:00.000", DateHelper.EUROPE_BERLIN).time
        )
        // VEVENT_DTEND
        Assertions.assertEquals(
            event.endDate!!.time,
            DateHelper.parseIsoTimestamp("2020-01-01 13:00:00.000", DateHelper.EUROPE_BERLIN).time
        )
        // VEVENT_SUMMARY
        Assertions.assertEquals(event.subject, "Test ical import web frontend")
        // VEVENT_UID
        Assertions.assertEquals(event.uid, "ICAL_IMPORT_UID_VALUE_UPDATE_IF_REQUIRED")
        // VEVENT_CREATED
        Assertions.assertNull(event.created)
        // VEVENT_LOCATION
        Assertions.assertNull(event.location)
        // VEVENT_DTSTAMP
        Assertions.assertEquals(
            event.dtStamp!!.time,
            DateHelper.parseIsoTimestamp("2017-06-22 15:07:52.000", DateHelper.UTC).time
        )
        // VEVENT_LAST_MODIFIED
        Assertions.assertNull(event.lastUpdate)
        // VEVENT_SEQUENCE
        Assertions.assertEquals(event.sequence, 0)
        // VEVENT_ORGANIZER
        Assertions.assertEquals(event.organizer, "mailto:organizer@example.com")
        Assertions.assertEquals(event.organizerAdditionalParams, "CN=Organizer")
        // VEVENT_TRANSP
        // currently not implemented
        // VEVENT_ALARM
        Assertions.assertNull(event.reminderActionType)
        // VEVENT_DESCRIPTION
        Assertions.assertNull(event.note)
        // VEVENT_ATTENDEES
        Assertions.assertEquals(event.attendees!!.size, 3)
        for (attendee in event.attendees!!) {
            Assertions.assertTrue(
                attendee.url == "organizer@example.com"
                        || attendee.url == "a1@example.com"
                        || attendee.url == "a2@example.com"
            )
        }
        // VEVENT_RRULE
        Assertions.assertNull(event.getRecurrenceRuleObject())
        // VEVENT_EX_DATE
        Assertions.assertNull(event.recurrenceExDate)
    }

    @Test
    fun textGenerateParse() {
        val events = this.events
        val generatorFull = ICalGenerator.exportAllFields()
        val generatorCancel = ICalGenerator.forMethod(Method.CANCEL)
        val parser = ICalParser.parseAllFields()

        for (event in events) {
            val full = this.generateParse(generatorFull, parser, event)
            val cancel = this.generateParse(generatorCancel, parser, event)

            this.compareEvents(generatorFull.exportsVEvent, event, full)
            this.compareEvents(generatorCancel.exportsVEvent, event, cancel)
        }
    }

    private fun generateParse(generator: ICalGenerator, parser: ICalParser, event: TeamEventDO): TeamEventDO {
        generator.reset()
        generator.addEvent(event)
        Assertions.assertFalse(generator.isEmpty)
        val ics = generator.calendarAsByteStream.toString()
        parser.reset()
        Assertions.assertTrue(parser.parse(ics))
        Assertions.assertEquals(parser.extractedEvents.size, 1)
        return parser.extractedEvents[0]
    }

    private val events: List<TeamEventDO>
        get() {
            val events: MutableList<TeamEventDO> = ArrayList()

            // simple event
            var event = TeamEventDO()

            event.reminderActionType = ReminderActionType.MESSAGE
            event.reminderDuration = 100L
            event.reminderDurationUnit = ReminderDurationUnit.MINUTES
            val attendee = TeamEventAttendeeDO()
            attendee.url = "test@test.de"
            attendee.status = TeamEventAttendeeStatus.DECLINED
            attendee.commonName = "test"
            attendee.role = Role.ROLE
            attendee.cuType = CuType.INDIVIDUAL.value
            attendee.rsvp = true
            event.addAttendee(attendee)
            event.subject = "subject"
            event.endDate = Date(
                DateHelper.parseIsoTimestamp("2017-07-31 00:00:00.000", DateHelper.EUROPE_BERLIN).time
            )
            event.dtStamp = Date(
                DateHelper.parseIsoTimestamp("2017-07-30 12:00:00.000", DateHelper.EUROPE_BERLIN).time
            )
            event.startDate = Date(
                DateHelper.parseIsoTimestamp("2017-07-31 12:00:00.000", DateHelper.EUROPE_BERLIN).time
            )
            event.location = "location"
            event.organizer = "organizer"
            event.sequence = 5
            event.note = "summary"
            event.uid = "uid string"

            events.add(event)

            // all day event
            event = TeamEventDO()

            event.allDay = true
            event.subject = "subject"
            event.endDate =
                Date(DateHelper.parseIsoTimestamp("2017-07-31 00:00:00.000", DateHelper.UTC).time)
            event.dtStamp =
                Date(DateHelper.parseIsoTimestamp("2017-07-30 12:00:00.000", DateHelper.UTC).time)
            event.startDate =
                Date(DateHelper.parseIsoTimestamp("2017-08-31 00:00:00.000", DateHelper.UTC).time)
            event.organizer = "organizer"
            event.sequence = 5
            event.uid = "uid string"

            events.add(event)

            return events
        }

    private fun compareEvents(fields: List<String>, eventSrc: TeamEventDO, eventExtracted: TeamEventDO) {
        for (field in fields) {
            if (ICalConverterStore.VEVENT_DTSTART == field) {
                Assertions.assertEquals(eventExtracted.startDate, eventSrc.startDate)
            } else if (ICalConverterStore.VEVENT_DTEND == field) {
                Assertions.assertEquals(eventExtracted.endDate, eventSrc.endDate)
            } else if (ICalConverterStore.VEVENT_SUMMARY == field) {
                Assertions.assertEquals(eventExtracted.subject, eventSrc.subject)
            } else if (ICalConverterStore.VEVENT_UID == field) {
                Assertions.assertEquals(eventExtracted.uid, eventSrc.uid)
            } else if (ICalConverterStore.VEVENT_CREATED == field) {
                // currently not implemented
            } else if (ICalConverterStore.VEVENT_LOCATION == field) {
                Assertions.assertEquals(eventExtracted.location, eventSrc.location)
            } else if (ICalConverterStore.VEVENT_DTSTAMP == field) {
                Assertions.assertEquals(eventExtracted.dtStamp, eventSrc.dtStamp)
            } else if (ICalConverterStore.VEVENT_LAST_MODIFIED == field) {
                // currently not implemented
            } else if (ICalConverterStore.VEVENT_SEQUENCE == field) {
                Assertions.assertEquals(eventExtracted.sequence, eventSrc.sequence)
            } else if (ICalConverterStore.VEVENT_ORGANIZER == field) {
                Assertions.assertEquals(eventExtracted.organizer, eventSrc.organizer)
                if (eventSrc.organizerAdditionalParams != null) {
                    Assertions.assertEquals(
                        eventExtracted.organizerAdditionalParams,
                        eventSrc.organizerAdditionalParams
                    )
                } else {
                    Assertions.assertEquals(
                        eventExtracted.organizerAdditionalParams,
                        "CUTYPE=INDIVIDUAL;ROLE=CHAIR;PARTSTAT=ACCEPTED"
                    )
                }
            } else if (ICalConverterStore.VEVENT_TRANSP == field) {
                // currently not implemented
            } else if (ICalConverterStore.VEVENT_ALARM == field) {
                Assertions.assertEquals(eventExtracted.reminderDuration, eventSrc.reminderDuration)
                Assertions.assertEquals(eventExtracted.reminderActionType, eventSrc.reminderActionType)
                Assertions.assertEquals(eventExtracted.reminderDurationUnit, eventSrc.reminderDurationUnit)
            } else if (ICalConverterStore.VEVENT_DESCRIPTION == field) {
                Assertions.assertEquals(eventExtracted.note, eventSrc.note)
            } else if (ICalConverterStore.VEVENT_ATTENDEES == field) {
                if (eventSrc.attendees == null) {
                    Assertions.assertNull(eventExtracted.attendees)
                } else {
                    Assertions.assertEquals(eventExtracted.attendees!!.size, eventSrc.attendees!!.size)

                    for (attendee in eventSrc.attendees!!) {
                        var found: TeamEventAttendeeDO? = null

                        for (attendee2 in eventSrc.attendees!!) {
                            if (attendee.url == attendee2.url) {
                                found = attendee2
                                break
                            }
                        }

                        Assertions.assertNotNull(found)
                        Assertions.assertEquals(found!!.commonName, attendee.commonName)
                        Assertions.assertEquals(found.status, attendee.status)
                        Assertions.assertEquals(found.cuType, attendee.cuType)
                        Assertions.assertEquals(found.role, attendee.role)
                        Assertions.assertEquals(found.rsvp, attendee.rsvp)
                        Assertions.assertEquals(found.additionalParams, attendee.additionalParams)
                    }
                }
            } else if (ICalConverterStore.VEVENT_RRULE == field) {
                Assertions.assertEquals(eventExtracted.recurrenceRule, eventSrc.recurrenceRule)
                Assertions.assertEquals(eventExtracted.recurrenceReferenceDate, eventSrc.recurrenceReferenceDate)
            } else if (ICalConverterStore.VEVENT_RECURRENCE_ID == field) {
                // currently not implemented
            } else if (ICalConverterStore.VEVENT_EX_DATE == field) {
                Assertions.assertEquals(eventExtracted.recurrenceExDate, eventSrc.recurrenceExDate)
            } else {
                throw RuntimeException("Unknown field $field")
            }
        }

        // check blank fields
        ICalConverterStore.FULL_LIST.forEach { field ->
            if (fields.contains(field)) {
                return@forEach
            }
            if (ICalConverterStore.VEVENT_DTSTART == field) {
                Assertions.assertNull(eventExtracted.startDate)
            } else if (ICalConverterStore.VEVENT_DTEND == field) {
                Assertions.assertNull(eventExtracted.endDate)
            } else if (ICalConverterStore.VEVENT_SUMMARY == field) {
                Assertions.assertNull(eventExtracted.subject)
            } else if (ICalConverterStore.VEVENT_UID == field) {
                Assertions.assertNull(eventExtracted.uid)
            } else if (ICalConverterStore.VEVENT_CREATED == field) {
                Assertions.assertNull(eventExtracted.created)
            } else if (ICalConverterStore.VEVENT_LOCATION == field) {
                Assertions.assertNull(eventExtracted.location)
            } else if (ICalConverterStore.VEVENT_DTSTAMP == field) {
                Assertions.assertNull(eventExtracted.dtStamp)
            } else if (ICalConverterStore.VEVENT_LAST_MODIFIED == field) {
                Assertions.assertNull(eventExtracted.lastUpdate)
            } else if (ICalConverterStore.VEVENT_SEQUENCE == field) {
                Assertions.assertNull(eventExtracted.sequence)
            } else if (ICalConverterStore.VEVENT_ORGANIZER == field) {
                Assertions.assertNull(eventExtracted.organizer)
                Assertions.assertNull(eventExtracted.organizerAdditionalParams)
            } else if (ICalConverterStore.VEVENT_TRANSP == field) {
                // currently not implemented
            } else if (ICalConverterStore.VEVENT_ALARM == field) {
                Assertions.assertNull(eventExtracted.reminderActionType)
                Assertions.assertNull(eventExtracted.reminderDuration)
                Assertions.assertNull(eventExtracted.reminderDurationUnit)
            } else if (ICalConverterStore.VEVENT_DESCRIPTION == field) {
                Assertions.assertNull(eventExtracted.note)
            } else if (ICalConverterStore.VEVENT_ATTENDEES == field) {
                Assertions.assertNull(eventExtracted.attendees)
            } else if (ICalConverterStore.VEVENT_RRULE == field) {
                Assertions.assertNull(eventExtracted.recurrenceRule)
                Assertions.assertNull(eventExtracted.recurrenceReferenceDate)
            } else if (ICalConverterStore.VEVENT_RECURRENCE_ID == field) {
                // currently not implemented
            } else if (ICalConverterStore.VEVENT_EX_DATE == field) {
                Assertions.assertNull(eventExtracted.recurrenceExDate)
            } else {
                throw RuntimeException("Unknown field $field")
            }
        }
    }
}

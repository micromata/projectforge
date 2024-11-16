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

package org.projectforge.caldav.service

import io.milton.ent.config.HttpManagerBuilderEnt
import io.milton.http.ResourceFactory
import io.milton.http.caldav.*
import io.milton.http.caldav.ICalFormatter.FreeBusyRequest
import io.milton.http.exceptions.BadRequestException
import io.milton.http.exceptions.NotAuthorizedException
import io.milton.mail.MailboxAddress
import io.milton.principal.CalDavPrincipal
import io.milton.resource.CalendarResource
import io.milton.resource.CollectionResource
import io.milton.resource.ICalResource
import io.milton.resource.SchedulingResponseItem
import net.fortuna.ical4j.data.ParserException
import net.fortuna.ical4j.model.component.VEvent
import org.projectforge.business.teamcal.event.ical.VEventUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.AbstractMap.SimpleImmutableEntry
import kotlin.Throws

/**
 * Created by blumenstein on 17.05.17.
 */
class ProjectForgeCalendarSearchService(builderEnt: HttpManagerBuilderEnt?) : CalendarSearchService {
    private val builderEnt: HttpManagerBuilderEnt
    private val formatter: ICalFormatter
    private val untilFormatter: SimpleDateFormat
    private var rFactory: ResourceFactory? = null
    private var schedulingColName = "cals"
    private var inboxName = "inbox"
    private var outBoxName = "outbox"
    private var usersBasePath = "/users/"

    @Throws(NotAuthorizedException::class, BadRequestException::class)
    override fun findCalendarResources(calendar: CalendarResource, start: Date, end: Date): List<ICalResource> {
        return findCalendarResources(calendar, start, end, null)
    }

    @Throws(NotAuthorizedException::class, BadRequestException::class)
    override fun findCalendarResources(
        calendar: CalendarResource, start: Date, end: Date,
        propFilter: SimpleImmutableEntry<String, String>?
    ): List<ICalResource> {
        log.info(
            "Find calender resources of '{}'/'{}' within time window from '{}' to '{}'",
            calendar.name,
            calendar.uniqueId,
            LOG_FORMAT.format(start),
            LOG_FORMAT.format(end)
        )
        // build a list of all calendar resources
        val list: MutableList<ICalResource> = ArrayList()
        for (r in calendar.children) {
            if (r is ICalResource) {
                list.add(r)
            }
        }
        val sizeBefore = list.size
        // So now we have (or might have) start and end dates, so filter list
        val it = list.iterator()
        while (it.hasNext()) {
            val r = it.next()
            log.debug("Check event '{}'", r.uniqueId)
            // create calender object
            val event = VEventUtils.parseVEventFromIcs(r.iCalData)
            // check if event is inside boundaries
            try {
                if (outsideDates(r, event, start, end)) {
                    it.remove()
                    continue
                }
            } catch (e: IOException) {
                log.error("Date constraints of event '{}' could not be checked, event is skipped", r.uniqueId, e)
                it.remove()
                continue
            } catch (e: ParserException) {
                log.error("Date constraints of event '{}' could not be checked, event is skipped", r.uniqueId, e)
                it.remove()
                continue
            }
            // check if event fulfills properties
            if (propFilter != null) {
                if (event != null && VEventUtils.getPropertyValue(event, propFilter.key) != propFilter.value) {
                    log.debug("Does not match properties filter: '{}'", r.uniqueId)
                    it.remove()
                }
            }
        }
        log.info(
            "Found {} ({} total, {} removed) events for calender resources '{}'/'{}'",
            list.size,
            sizeBefore,
            sizeBefore - list.size,
            calendar.name,
            calendar.uniqueId
        )
        return list
    }

    private fun extractRRule(vevent: VEvent?): Map<String, String>? {
        if (vevent == null) return null
        val rrule = VEventUtils.getRRuleString(vevent)
        val values = rrule?.split(";".toRegex())?.toTypedArray()
        val keyValues: MutableMap<String, String> = TreeMap()
        values?.forEach { v ->
            val kv = v.split("=".toRegex()).toTypedArray()
            if (kv.size == 2) {
                keyValues[kv[0]] = kv[1]
            }
        }
        return keyValues
    }

    @Throws(IOException::class, ParserException::class)
    private fun outsideDates(r: ICalResource, event: VEvent?, start: Date?, end: Date?): Boolean {
        val kv = extractRRule(event)
        log.debug("Check outsideDates for event '{}' ({})", r.uniqueId, if (kv != null) "recurring" else "normal")
        if (kv != null) {
            val until = kv["UNTIL"]
            if (until != null) {
                if (start == null) {
                    return false
                }
                try {
                    val uDate = untilFormatter.parse(until)
                    if (uDate.before(start)) {
                        return true
                    }
                } catch (e: ParseException) {
                    log.warn(
                        "Until date '{}' from RRULE can't be parsed for event '{}', start/stop date can't be checked!",
                        until,
                        r.uniqueId,
                        e
                    )
                    // Event is recurring, returning false prevents wrong pick out
                    return false
                }
            }
            // Recurring event without until date -> not outside
            return false
        }
        val event: EventResource
        if (r is EventResource) {
            event = r
        } else {
            event = EventResourceImpl()
            formatter.parseEvent(event, r.iCalData)
        }
        if (start != null) {
            if (event.end.before(start)) {
                log.debug(
                    "Event is before start: {} < {}",
                    if (event.end != null) LOG_FORMAT.format(event.end) else "null",
                    LOG_FORMAT.format(start)
                )
                return true
            }
        }
        if (end != null) {
            if (event.start.after(end)) {
                log.debug(
                    "Event is after end: {} < {}",
                    if (event.start != null) LOG_FORMAT.format(event.start) else "null",
                    LOG_FORMAT.format(end)
                )
                return true
            }
        }
        return false
    }

    override fun queryFreeBusy(principal: CalDavPrincipal, iCalText: String): List<SchedulingResponseItem> {
        log.info("Query free/busy time for principal '{}' and iCalText '{}'", principal, iCalText)
        val r = formatter.parseFreeBusyRequest(iCalText)
        log.info("queryFreeBusy: attendees=" + r.attendeeLines.size + " - " + r.attendeeMailtos.size)
        val list: MutableList<SchedulingResponseItem> = ArrayList()
        // For each attendee locate events within the given date range and add them as busy responses
        try {
            for (attendeeMailto in r.attendeeMailtos) {
                val add = MailboxAddress.parse(attendeeMailto)
                val attendee = findUserFromMailto(add)
                if (attendee == null) {
                    log.warn("Attendee not found: $attendeeMailto")
                    val item = SchedulingResponseItem(attendeeMailto, ITip.StatusResponse.RS_INVALID_37, null)
                    list.add(item)
                } else {
                    log.info("Found attendee: " + attendee.name)
                    // Now locate events and build an ical response
                    val ical = buildFreeBusyAttendeeResponse(attendee, r, add.domain, attendeeMailto)
                    val item = SchedulingResponseItem(attendeeMailto, ITip.StatusResponse.RS_SUCCESS_20, ical)
                    list.add(item)
                }
            }
        } catch (ex: NotAuthorizedException) {
            throw RuntimeException(ex)
        } catch (ex: BadRequestException) {
            throw RuntimeException(ex)
        }
        return list
    }

    /**
     * Attempt to iterate over the entire users collection, and for each event
     * in each user's calendar check if the given user is an attendee, and if
     * return it.
     *
     *
     * Rather inefficient
     *
     * @param user
     * @return
     * @throws NotAuthorizedException
     * @throws BadRequestException
     */
    @Throws(NotAuthorizedException::class, BadRequestException::class)
    override fun findAttendeeResources(user: CalDavPrincipal): List<ICalResource> {
        log.info("Find attendee resources for CalDAV principal '{}'", user.name)
        return ArrayList()
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

    @Throws(NotAuthorizedException::class, BadRequestException::class)
    override fun findAttendeeResourcesCTag(attendee: CalDavPrincipal): String {
        log.info("Find attendee resources CTag of caldav principal '{}'", attendee.name)
        var latest: Date? = null
        for (r in findAttendeeResources(attendee)) {
            val d = r.modifiedDate
            if (latest == null || d.after(latest)) {
                latest = d
            }
        }
        return if (latest != null) {
            "mod-" + latest.time
        } else {
            "na"
        }
    }

    override fun getSchedulingColName(): String {
        return schedulingColName
    }

    fun setSchedulingColName(schedulingColName: String) {
        this.schedulingColName = schedulingColName
    }

    override fun getSchedulingInboxColName(): String {
        return inboxName
    }

    fun setSchedulingInboxColName(inboxName: String) {
        this.inboxName = inboxName
    }

    override fun getSchedulingOutboxColName(): String {
        return outBoxName
    }

    fun setSchedulingOutboxColName(outBoxName: String) {
        this.outBoxName = outBoxName
    }

    /**
     * Use the domain portion of the email as the host, and the initial portion
     * as the userid. This wont work in systems which require use userid's with
     *
     * @param add
     * @return
     */
    @Throws(NotAuthorizedException::class, BadRequestException::class)
    private fun findUserFromMailto(add: MailboxAddress): CalDavPrincipal? {
        val userPath = usersBasePath + add.user
        val r = resourceFactory!!.getResource(add.domain, userPath)
        return if (r == null) {
            log.warn("Failed to find: " + userPath + " in host: " + add.domain)
            null
        } else {
            if (r is CalDavPrincipal) {
                r
            } else {
                log.warn("findUserFromMailto: found a resource but it is not a CalDavPrincipal. Is a: " + r.javaClass.canonicalName)
                null
            }
        }
    }

    @Throws(NotAuthorizedException::class, BadRequestException::class)
    private fun buildFreeBusyAttendeeResponse(
        attendee: CalDavPrincipal,
        request: FreeBusyRequest,
        domain: String,
        attendeeMailto: String
    ): String {
        val source = request.lines
        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\n")
        sb.append("VERSION:2.0 PRODID:-//milton.io//CalDAV Server//EN\n")
        sb.append("METHOD:REPLY\n")
        sb.append("BEGIN:VFREEBUSY\n")
        // Copy these lines back verbatim
        sb.append(source["UID"]).append("\n")
        sb.append(source["DTSTAMP"]).append("\n")
        sb.append(source["DTSTART"]).append("\n")
        sb.append(source["DTEND"]).append("\n")
        sb.append(source["ORGANIZER"]).append("\n")
        // Output the original attendee line
        sb.append(request.attendeeLines[attendeeMailto]).append("\n")
        val start = request.start
        val finish = request.finish
        for (href in attendee.calendarHomeSet) {
            if (log.isTraceEnabled) {
                log.trace("Look for calendar home: $href")
            }
            val rCalHome = resourceFactory!!.getResource(domain, href)
            if (rCalHome is CollectionResource) {
                log.trace("Look for calendars in home")
                for (rColCal in rCalHome.children) {
                    if (rColCal is CalendarResource) {
                        val eventsInRange = findCalendarResources(rColCal, start, finish, null)
                        if (log.isTraceEnabled) {
                            log.trace("Process calendar: " + rColCal.name + " events in range=" + eventsInRange.size)
                            log.trace("  range= $start - $finish")
                        }
                        for (event in eventsInRange) {
                            log.trace("Process event: " + event.name)
                            val er = EventResourceImpl()
                            try {
                                formatter.parseEvent(er, event.iCalData)
                            } catch (ex: IOException) {
                                throw RuntimeException(ex)
                            } catch (ex: ParserException) {
                                throw RuntimeException(ex)
                            }
                            // write the freebusy statement, Eg:
// FREEBUSY;FBTYPE=BUSY:20090602T110000Z/20090602T120000Z
                            sb.append("FREEBUSY;FBTYPE=BUSY:")
                            sb.append(formatter.formatDate(er.start))
                            sb.append("/")
                            sb.append(formatter.formatDate(er.end))
                            sb.append("\n")
                        }
                    }
                }
            } else {
                if (rCalHome == null) {
                    log.warn("Didnt find calendar home: $href in domain: $domain")
                } else {
                    log.warn("Found a resource at the calendar home address, but it is not a CollectionResource. Is a: " + rCalHome.javaClass)
                }
            }
        }
        sb.append("END:VFREEBUSY\n")
        sb.append("END:VCALENDAR\n")
        return sb.toString()
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
    private fun isAttendeeOf(user: CalDavPrincipal, event: ICalResource): Boolean {
        for (mailTo in formatter.parseAttendees(event.iCalData)) {
            if (mailTo == null) {
                log.debug("E-mail address for event attendee '{}' is null", event.name)
                continue
            }
            try {
                val add = MailboxAddress.parse(mailTo)
                if (add.user == user.name) {
                    return true
                }
            } catch (e: IllegalArgumentException) {
                log.warn("Could not parse E-mail address '{}' for event attendee, skip attendee", mailTo, event.name)
            }
        }
        return false
    }

    private val resourceFactory: ResourceFactory?
        get() {
            if (rFactory == null) {
                rFactory = builderEnt.resourceFactory
            }
            return rFactory
        }

    companion object {
        private val log = LoggerFactory.getLogger(ProjectForgeCalendarSearchService::class.java)
        private val LOG_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    }

    init {
        if (builderEnt == null) {
            throw NullPointerException("ResourceFactory is null")
        }
        this.builderEnt = builderEnt
        formatter = ICalFormatter()
        untilFormatter = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'")
    }
}

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

package org.projectforge.rest.calendar.converter

import net.fortuna.ical4j.model.Parameter
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.parameter.*
import net.fortuna.ical4j.model.property.Attendee
import org.apache.commons.validator.routines.EmailValidator
import org.projectforge.business.teamcal.event.TeamEventService
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeStatus
import org.projectforge.rest.dto.CalEvent
import org.springframework.beans.factory.annotation.Autowired
import java.net.URI
import java.util.*

class AttendeeConverter : PropertyConverter() {

    @Autowired
    private val teamEventService: TeamEventService? = null

    override fun toVEvent(event: CalEvent, vEvent: VEvent): Boolean {
        if (event.attendees == null) {
            return false
        }

        // TODO add organizer user, most likely as chair
        for (a in event.attendees!!) {
            val email = "mailto:" + if (a.address != null) a.address!!.email else a.url

            val attendee = Attendee(URI.create(email))

            // set common name
            if (a.address != null) {
                attendee.parameters.add(Cn(a.address!!.fullName))
            } else if (a.commonName != null) {
                attendee.parameters.add(Cn(a.commonName))
            } else {
                attendee.parameters.add(Cn(a.url))
            }

            attendee.parameters.add(if (a.cuType != null) CuType(a.cuType) else CuType.INDIVIDUAL)
            attendee.parameters.add(if (a.role != null) Role(a.role) else Role.REQ_PARTICIPANT)
            if (a.rsvp != null) {
                attendee.parameters.add(Rsvp(a.rsvp))
            }
            attendee.parameters.add(if (a.status != null) a.status!!.partStat else PartStat.NEEDS_ACTION)
            this.parseAdditionalParameters(attendee.parameters, a.additionalParams)

            vEvent.properties.add(attendee)
        }

        return true
    }

    override fun fromVEvent(event: CalEvent, vEvent: VEvent): Boolean {
        val eventAttendees = vEvent.getProperties(Attendee.ATTENDEE)
        if (eventAttendees == null || eventAttendees.isEmpty()) {
            return false
        }

        for (eventAttendee in eventAttendees) {
            val attendee = eventAttendee as Attendee
            val attendeeUri = attendee.calAddress
            val email = attendeeUri?.schemeSpecificPart

            if (email != null && !EmailValidator.getInstance().isValid(email)) {
                continue // TODO maybe validation is not necessary, could also be en url? check rfc
            }

            val attendeeDO = TeamEventAttendeeDO()
            attendeeDO.url = email

            // set additional fields
            val cn = attendee.getParameter(Parameter.CN) as Cn
            val cuType = attendee.getParameter(Parameter.CUTYPE) as CuType
            val partStat = attendee.getParameter(Parameter.PARTSTAT) as PartStat
            val rsvp = attendee.getParameter(Parameter.RSVP) as Rsvp
            val role = attendee.getParameter(Parameter.ROLE) as Role

            attendeeDO.commonName = cn.value
            attendeeDO.status = if (partStat != null) TeamEventAttendeeStatus.getStatusForPartStat(partStat.value) else null
            attendeeDO.cuType = cuType.value
            attendeeDO.rsvp = rsvp.rsvp
            attendeeDO.role = role.value

            // further params
            val sb = StringBuilder()
            val iter = attendee.parameters.iterator()

            while (iter.hasNext()) {
                val param = iter.next()
                if (param.name == null || STEP_OVER.contains(param.name)) {
                    continue
                }

                sb.append(";")
                sb.append(param.toString())
            }

            if (sb.isNotEmpty()) {
                // remove first ';'
                attendeeDO.additionalParams = sb.substring(1)
            }

            event.attendees!!.add(attendeeDO)
        }

        return true
    }

    companion object {
        private val STEP_OVER = Arrays.asList(Parameter.CN, Parameter.CUTYPE, Parameter.PARTSTAT, Parameter.RSVP, Parameter.ROLE)
    }
}

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
import net.fortuna.ical4j.model.ParameterList
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.parameter.Cn
import net.fortuna.ical4j.model.parameter.CuType
import net.fortuna.ical4j.model.parameter.PartStat
import net.fortuna.ical4j.model.parameter.Role
import net.fortuna.ical4j.model.property.Organizer
import org.projectforge.rest.dto.CalEvent

import java.net.URISyntaxException

class OrganizerConverter(private val useBlankOrganizer: Boolean) : PropertyConverter() {

    override fun toVEvent(event: CalEvent): Property? {
        val param = ParameterList()
        val organizerMail: String?

        if (event.ownership != null && event.ownership!!) {
            // TODO improve ownership handling
            param.add(Cn(event.creator!!.getFullname()))
            param.add(CuType.INDIVIDUAL)
            param.add(Role.CHAIR)
            param.add(PartStat.ACCEPTED)

            organizerMail = if (this.useBlankOrganizer) {
                "mailto:null"
            } else {
                "mailto:" + event.creator!!.email!!
            }
        } else if (event.organizer != null) {
            // read owner from
            this.parseAdditionalParameters(param, event.organizerAdditionalParams)
            if (param.getParameter(Parameter.CUTYPE) == null) {
                param.add(CuType.INDIVIDUAL)
            }
            if (param.getParameter(Parameter.ROLE) == null) {
                param.add(Role.CHAIR)
            }
            if (param.getParameter(Parameter.PARTSTAT) == null) {
                param.add(PartStat.ACCEPTED)
            }
            organizerMail = event.organizer
        } else {
            return null
        }

        return try {
            Organizer(param, organizerMail)
        } catch (e: URISyntaxException) {
            // TODO handle exception and use better default
            try {
                Organizer(ParameterList(), "mailto:null")
            } catch (e1: URISyntaxException) {
                null
            }

        }

    }

    override fun fromVEvent(event: CalEvent, vEvent: VEvent): Boolean {
        var ownership = false

        val organizer = vEvent.organizer
        if (organizer != null) {
            val organizerCNParam = organizer.getParameter(Parameter.CN)
            val organizerMailParam = organizer.getParameter("EMAIL")

            val organizerCN = organizerCNParam?.value
            val organizerEMail = organizerMailParam?.value
            val organizerValue = organizer.value

            // determine ownership
            if ("mailto:null" == organizerValue) {
                // owner mail to is missing (apple calender tool)
                ownership = true
            } else if (organizerCN != null && event.creator != null && organizerCN == event.creator!!.username) {
                // organizer name is user name
                ownership = true
            } else if (organizerEMail != null && event.creator != null && organizerEMail == event.creator!!.email) {
                // organizer email is user email
                ownership = true
            }

            // further parameters
            val sb = StringBuilder()
            val iter = organizer.parameters.iterator()

            while (iter.hasNext()) {
                val param = iter.next()
                if (param.name == null) {
                    continue
                }

                sb.append(";")
                sb.append(param.toString())
            }

            if (sb.isNotEmpty()) {
                // remove first ';'
                event.organizerAdditionalParams = sb.substring(1)
            }

            if ("mailto:null" != organizerValue) {
                event.organizer = organizer.value
            }
        } else {
            // some clients, such as thunderbird lightning, do not send an organizer -> pf has ownership
            ownership = true
        }

        event.ownership = ownership
        return false
    }
}

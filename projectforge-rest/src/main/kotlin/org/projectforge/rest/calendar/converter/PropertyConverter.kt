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

import net.fortuna.ical4j.model.ParameterFactoryImpl
import net.fortuna.ical4j.model.ParameterList
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.VEvent
import org.projectforge.rest.dto.CalEvent

import java.net.URISyntaxException

abstract class PropertyConverter : VEventComponentConverter {
    open fun toVEvent(event: CalEvent): Property? {
        return null
    }

    override fun toVEvent(event: CalEvent, vEvent: VEvent): Boolean {
        val property = this.toVEvent(event) ?: return false

        vEvent.properties.add(property)

        return true
    }

    protected fun isAllDay(vEvent: VEvent): Boolean {
        val dtStart = vEvent.startDate
        return dtStart != null && dtStart.date !is net.fortuna.ical4j.model.DateTime
    }

    protected fun parseAdditionalParameters(list: ParameterList?, additonalParams: String?) {
        if (list == null || additonalParams == null) {
            return
        }

        val parameterFactory = ParameterFactoryImpl.getInstance()
        val sb = StringBuilder()
        var escaped = false
        val chars = additonalParams.toCharArray()
        var name: String? = null

        for (c in chars) {
            when (c) {
                ';' -> if (!escaped && name != null && sb.isNotEmpty()) {
                    try {
                        list.add(parameterFactory.createParameter(name, sb.toString().replace("\"".toRegex(), "")))
                    } catch (e: URISyntaxException) {
                        // TODO
                        e.printStackTrace()
                    }

                    name = null
                    sb.setLength(0)
                }
                '"' -> escaped = !escaped
                '=' -> if (!escaped && sb.isNotEmpty()) {
                    name = sb.toString()
                    sb.setLength(0)
                }
                else -> sb.append(c)
            }
        }

        if (!escaped && name != null && sb.isNotEmpty()) {
            try {
                list.add(parameterFactory.createParameter(name, sb.toString().replace("\"".toRegex(), "")))
            } catch (e: URISyntaxException) {
                // TODO
                e.printStackTrace()
            }

        }
    }
}

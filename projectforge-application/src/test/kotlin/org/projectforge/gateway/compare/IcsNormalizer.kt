/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.gateway.compare

import org.projectforge.business.teamcal.TeamCalConfig

/**
 * Makes the ICS answers of the main instance and the gateway comparable: unfolded lines, without DTSTAMP (ical4j
 * stamps every event with the time of the export), events sorted by UID and RECURRENCE-ID. The setup event of an
 * empty calendar (ICalGenerator.ensureNotEmpty) has a random UID and the time of the export: both are replaced.
 */
object IcsNormalizer {
    fun normalize(ics: String): String {
        val lines = unfold(ics).filter { it.isNotEmpty() && !it.startsWith("DTSTAMP") }
        val result = mutableListOf<String>()
        val events = mutableListOf<List<String>>()
        var event: MutableList<String>? = null
        for (line in lines) {
            when {
                event != null -> {
                    event.add(line)
                    if (line == "END:VEVENT") {
                        events.add(neutralizeSetupEvent(event))
                        event = null
                    }
                }
                line == "BEGIN:VEVENT" -> event = mutableListOf(line)
                line == "END:VCALENDAR" -> {
                    events.sortedBy { sortKey(it) }.forEach { result.addAll(it) }
                    events.clear()
                    result.add(line)
                }
                else -> result.add(line)
            }
        }
        events.forEach { result.addAll(it) } // Not terminated by END:VCALENDAR.
        event?.let { result.addAll(it) }
        return result.joinToString("\n")
    }

    /**
     * @return The number of events (for the report).
     */
    fun countEvents(ics: String): Int = unfold(ics).count { it == "BEGIN:VEVENT" }

    /**
     * RFC 5545 3.1: a line starting with a blank or tab continues the previous one.
     */
    internal fun unfold(text: String): List<String> {
        return text.replace("\r\n", "\n").replace(Regex("\n[ \t]"), "").split("\n")
    }

    private fun neutralizeSetupEvent(event: List<String>): List<String> {
        if (event.none { it == "SUMMARY:${TeamCalConfig.SETUP_EVENT}" }) {
            return event
        }
        return event.map { line ->
            val name = SETUP_EVENT_VOLATILE.firstOrNull { line.startsWith("$it:") || line.startsWith("$it;") }
            if (name != null) "$name:{setup}" else line
        }
    }

    private val SETUP_EVENT_VOLATILE = listOf("UID", "DTSTART", "DTEND")

    private fun sortKey(event: List<String>): String {
        fun value(name: String) = event.firstOrNull { it.startsWith("$name:") || it.startsWith("$name;") }
            ?.substringAfter(":") ?: ""
        return "${value("UID")}|${value("RECURRENCE-ID")}|${value("DTSTART")}"
    }
}

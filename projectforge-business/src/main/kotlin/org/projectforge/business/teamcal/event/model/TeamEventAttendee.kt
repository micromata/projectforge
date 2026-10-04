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


package org.projectforge.business.teamcal.event.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import mu.KotlinLogging
import org.projectforge.framework.json.JsonUtils

private val log = KotlinLogging.logger {}

/**
 * An attendee of a team event, stored as part of the JSON column [TeamEventDO.attendeesJson]. A self-contained
 * snapshot without a reference to a user or an address: attendees are only shown, never edited or invited.
 *
 * @param name The display name (the CN of an ics ATTENDEE), if known.
 * @param email The mail address (the mailto: value of an ics ATTENDEE), if known.
 * @param status The participation status (PARTSTAT), if known.
 */
data class TeamEventAttendee(
    var name: String? = null,
    var email: String? = null,
    var status: TeamEventAttendeeStatus? = null,
) {
    /** The name, else the mail address. Not stored. */
    @get:JsonIgnore
    val displayName: String?
        get() = name?.takeIf { it.isNotBlank() } ?: email

    companion object {
        private val listType = object : TypeReference<List<TeamEventAttendee>>() {}

        /** Empty for null, blank or unreadable JSON (logged, an event must still be displayable). */
        fun fromJson(json: String?): List<TeamEventAttendee> {
            if (json.isNullOrBlank()) {
                return emptyList()
            }
            return try {
                JsonUtils.fromJson(json, listType, failOnUnknownProps = false) ?: emptyList()
            } catch (ex: Exception) {
                log.error(ex) { "Can't parse attendees of team event: $json" }
                emptyList()
            }
        }

        /** The length of the column [TeamEventDO.attendeesJson]. */
        const val MAX_JSON_LENGTH = 10000

        /**
         * Null for no attendees, so an event without any keeps an empty column. A list too long for the column
         * is cut at the end (logged): a huge invitation list must not make the event unsavable.
         */
        fun toJson(attendees: List<TeamEventAttendee>?): String? {
            if (attendees.isNullOrEmpty()) {
                return null
            }
            var list: List<TeamEventAttendee> = attendees
            var json = JsonUtils.toJson(list, ignoreNullableProps = true)
            while (json.length > MAX_JSON_LENGTH && list.size > 1) {
                list = list.subList(0, (list.size * MAX_JSON_LENGTH / json.length).coerceAtLeast(1))
                json = JsonUtils.toJson(list, ignoreNullableProps = true)
            }
            if (json.length > MAX_JSON_LENGTH) {
                log.warn { "Attendees of team event dropped, they don't fit in the column: $json" }
                return null
            }
            if (list.size < attendees.size) {
                log.warn { "Only ${list.size} of ${attendees.size} attendees of team event stored (column length)." }
            }
            return json
        }
    }
}

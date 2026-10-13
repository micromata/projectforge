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

package org.projectforge.business.lanesandplanes

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.common.logging.error
import org.projectforge.framework.json.JsonUtils
import java.io.File

private val log = KotlinLogging.logger {}

/**
 * Lanes & Planes identifies users by their email only (the ident can't be used to match them). A user whose email
 * changed in ProjectForge would be deactivated in L&P and created anew (with the default role). So a changed email
 * is held back: the email of the last push is sent, until the new one was changed in L&P by hand and is listed in
 * [LanesAndPlanesSettings.confirmedEmailChanges].
 */
object LanesAndPlanesEmailHolds {
    /** A user sent with the email of the last push instead of the new one. */
    data class Hold(val ident: String, val sentEmail: String, val newEmail: String)

    class Result(val users: List<LanesAndPlanesUser>, val holds: List<Hold>)

    /**
     * @param lastEmails The emails of the last successful push by ident (employees only, see [State]).
     * @param confirmed The confirmed new emails, lower case.
     */
    fun apply(users: List<LanesAndPlanesUser>, lastEmails: Map<String, String>, confirmed: Set<String>): Result {
        val usedEmails = users.map { it.email.lowercase() }.toSet()
        val holds = mutableListOf<Hold>()
        val result = users.map { user ->
            val ident = user.ident ?: return@map user
            val last = lastEmails[ident] ?: return@map user
            if (last.equals(user.email, ignoreCase = true) || user.email.lowercase() in confirmed) {
                return@map user
            }
            if (last.lowercase() in usedEmails) {
                log.error(LanesAndPlanesLogEvents.EMAIL_CHANGED) {
                    "Lanes & Planes: the email of user #$ident changed from '$last' to '${user.email}', but '$last' " +
                            "is now used by another user: sending '${user.email}'. Check the user in Lanes & Planes."
                }
                return@map user
            }
            holds += Hold(ident, sentEmail = last, newEmail = user.email)
            user.copy(email = last)
        }
        return Result(result.sortedBy { it.email.lowercase() }, holds)
    }

    /** The state of the given push state file, null if missing or not readable. */
    fun load(file: File): State? {
        if (!file.exists()) {
            return null
        }
        return try {
            JsonUtils.fromJson(file.readText(), State::class.java, failOnUnknownProps = false)
        } catch (ex: Exception) {
            log.warn { "Lanes & Planes: push state ${file.absolutePath} not readable, ignored: ${ex.message}" }
            null
        }
    }

    /** Stores the emails of the given (pushed) users by ident and their number. */
    fun save(file: File, users: List<LanesAndPlanesUser>) {
        val emails = users.mapNotNull { user -> user.ident?.let { it to user.email } }.toMap()
        file.writeText(JsonUtils.toJson(State(emails, users.size)))
    }

    /**
     * The state of the last successful push.
     * @param users The number of users sent (additional users included), see LanesAndPlanesSyncService.refuseReason.
     */
    class State(var emailsByIdent: Map<String, String> = emptyMap(), var users: Int? = null)
}

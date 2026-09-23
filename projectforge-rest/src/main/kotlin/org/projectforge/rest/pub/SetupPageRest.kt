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

package org.projectforge.rest.pub

import mu.KotlinLogging
import org.projectforge.Constants
import org.projectforge.business.admin.SetupService
import org.projectforge.business.admin.SetupTarget
import org.projectforge.framework.persistence.database.DatabaseService
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.TimeZone

private val log = KotlinLogging.logger {}

/**
 * Public REST endpoint for the initial database setup (first run).
 * Replaces the legacy Wicket /wa/setup page.
 * No authentication required — all endpoints under /rsPublic/ are public.
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}/setup")
open class SetupPageRest {

    /** A single timezone entry for the timezone picker. */
    data class TimeZoneInfo(val id: String, val displayName: String)

    /** Response for GET /rsPublic/setup/status. */
    data class SetupState(
        /** True when the database already has data — show "already set up" message instead of the form. */
        val alreadyInitialized: Boolean,
        val defaultUsername: String = DatabaseService.DEFAULT_ADMIN_USER,
        val defaultCalendarDomain: String = "local",
        val defaultTimeZone: String = TimeZone.getDefault().id,
        /** Sorted list of all available time zone IDs + display names for the timezone picker. */
        val availableTimeZones: List<TimeZoneInfo> = emptyList(),
    )

    /** Request body for POST /rsPublic/setup. */
    data class SetupRequest(
        val setupTarget: String = "TEST_DATA",
        val username: String = "",
        val password: String = "",
        val passwordRepeat: String = "",
        val timeZone: String = "",
        val calendarDomain: String = "",
        val sysopEMail: String? = null,
        val feedbackEMail: String? = null,
    )

    /** Response for POST /rsPublic/setup. */
    data class SetupResult(
        val success: Boolean,
        /** Localised error message when [success] is false. */
        val message: String? = null,
        /** Form field the error belongs to (for per-field display), if applicable. */
        val field: String? = null,
        /** On success: the URL the client should navigate to after setup. */
        val redirectUrl: String? = null,
    )

    @Autowired
    private lateinit var databaseService: DatabaseService

    @Autowired
    private lateinit var setupService: SetupService

    /**
     * Returns the current setup state and the data needed to render the form.
     * The client should check [SetupState.alreadyInitialized] first.
     */
    @GetMapping("status")
    fun getStatus(): SetupState {
        if (databaseService.databaseTablesWithEntriesExist()) {
            return SetupState(alreadyInitialized = true)
        }
        val timeZones = TimeZone.getAvailableIDs()
            .map { id -> TimeZoneInfo(id, TimeZone.getTimeZone(id).getDisplayName(false, TimeZone.LONG)) }
            .sortedBy { it.id }
        return SetupState(
            alreadyInitialized = false,
            defaultTimeZone = TimeZone.getDefault().id,
            availableTimeZones = timeZones,
        )
    }

    /**
     * Performs the initial database setup. The setup logic lives in [SetupService] (formerly the
     * Wicket SetupPage, now removed).
     *
     * No auto-login: the freshly initialised database changes caches, menus and user state that a
     * running session would not pick up cleanly. The client is sent to the login page instead, so
     * the admin signs in with the credentials just created and the app loads from a clean session.
     */
    @PostMapping
    fun finish(@RequestBody body: SetupRequest): SetupResult {
        if (databaseService.databaseTablesWithEntriesExist()) {
            log.error("Setup POST called but the database is already initialised — rejected.")
            return SetupResult(success = false, message = "Setup has already been completed.")
        }

        val errors = setupService.validate(
            password = body.password,
            passwordRepeat = body.passwordRepeat,
            calendarDomain = body.calendarDomain.ifBlank { "local" },
        )
        if (errors.isNotEmpty()) {
            val first = errors.first()
            return SetupResult(success = false, message = first.message, field = first.field)
        }

        val target = if (body.setupTarget == "EMPTY_DATABASE") SetupTarget.EMPTY_DATABASE else SetupTarget.TEST_DATA
        val timeZone = TimeZone.getTimeZone(body.timeZone.ifBlank { TimeZone.getDefault().id })

        setupService.finish(
            target = target,
            adminUsername = body.username.ifBlank { DatabaseService.DEFAULT_ADMIN_USER },
            password = body.password,
            timeZone = timeZone,
            calendarDomain = body.calendarDomain.ifBlank { "local" },
            sysopEMail = body.sysopEMail?.takeIf { it.isNotBlank() },
            feedbackEMail = body.feedbackEMail?.takeIf { it.isNotBlank() },
        )

        return SetupResult(
            success = true,
            redirectUrl = "/${Constants.NEXT_APP_PATH}login",
        )
    }
}

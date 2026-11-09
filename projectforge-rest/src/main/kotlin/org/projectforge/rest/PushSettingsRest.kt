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


package org.projectforge.rest

import org.projectforge.business.push.PushContentPrefService
import org.projectforge.business.push.PushSettings
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The logged-in user's settings of the pushes to their apps (see [PushContentPrefService]): the same setting in "My
 * account" and in the app.
 */
@RestController
@RequestMapping("${Rest.URL}/pushSettings")
class PushSettingsRest {
    class Data(
        /** For display only: false if the server keeps the content out of every push, there is nothing to choose. */
        val available: Boolean = true,
        /** The push shows the title and the beginning of the notification. */
        val content: Boolean = true,
        /** For display only, ignored on saving: false for a restricted or demo user (a shared account). */
        val writeAccess: Boolean = true,
    )

    /** The changed setting. */
    class Request(val content: Boolean = true)

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var pushContentPrefService: PushContentPrefService

    @AccessChecked("Own user only (logged-in user's settings)")
    @GetMapping
    fun get(): Data {
        val settings = pushContentPrefService.get(ThreadLocalUserContext.loggedInUserId!!)
        return Data(
            available = pushContentPrefService.isAvailable,
            content = settings.content,
            writeAccess = !accessChecker.isRestrictedOrDemoUser,
        )
    }

    /** @return The settings as saved. */
    @AccessChecked("Own user only (logged-in user's settings)")
    @PostMapping
    fun save(@RequestBody request: Request): Data {
        accessChecker.checkRestrictedOrDemoUser()
        pushContentPrefService.save(ThreadLocalUserContext.loggedInUserId!!, PushSettings(request.content))
        return get()
    }
}

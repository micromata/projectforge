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

package org.projectforge.rest.sipgate

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.sipgate.SipgateConfiguration
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.rest.SystemRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.concurrent.thread

private val log = KotlinLogging.logger {}

/**
 * The Sipgate administration actions, offered by the Sipgate menu of the System page in next (shown if Sipgate is
 * configured, see [SystemRest.SystemAdminData.sipgateConfigured]). Admin group only; the configuration of Sipgate
 * holds the credentials, so all actions require the second factor (ProjectForge2FAInitialization).
 */
@RestController
@RequestMapping("${Rest.URL}/sipgate")
class SipgateAdminRest {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var sipgateConfiguration: SipgateConfiguration

    @Autowired
    private lateinit var sipgateContactSyncService: SipgateContactSyncService

    @Autowired
    private lateinit var sipgateSyncService: SipgateSyncService

    /** The configuration of Sipgate (users, devices, numbers etc.) as Excel file. */
    @AccessChecked("Admin group + not restricted/demo + Sipgate configured (checkAccess)")
    @GetMapping("downloadConfiguration")
    fun downloadConfiguration(): ResponseEntity<ByteArrayResource> {
        checkAccess()
        log.info { "Downloading configuration (users, devices etc.) of Sipgate." }
        return SipgateExcelExporter.download(sipgateSyncService.readStorage())
    }

    /** Synchronizes the Sipgate contacts with the local addresses and reloads the remote configuration. */
    @AccessChecked("Admin group + not restricted/demo + Sipgate configured (checkAccess)")
    @PostMapping("synchronize")
    fun synchronize(): SystemRest.MessageResponse {
        checkAccess()
        thread(start = true) {
            sipgateSyncService.readStorage(true)
        }
        return sync(resetContacts = false)
    }

    /** As [synchronize], but resets the Sipgate contacts (e.g. for contacts with duplicated numbers). */
    @AccessChecked("Admin group + not restricted/demo + Sipgate configured (checkAccess)")
    @PostMapping("resetContacts")
    fun resetContacts(): SystemRest.MessageResponse {
        checkAccess()
        return sync(resetContacts = true)
    }

    private fun sync(resetContacts: Boolean): SystemRest.MessageResponse {
        log.info { "Synchronizing addresses for Sipgate (resetContacts=$resetContacts)." }
        val ctx = sipgateContactSyncService.sync(resetContacts = resetContacts)
        log.info { "Synchronizing result: local addresses: [${ctx.localCounter}], remote contacts: [${ctx.remoteCounter}]." }
        return SystemRest.MessageResponse(
            translateMsg("system.admin.sipgate.syncResult", ctx.localCounter, ctx.remoteCounter)
        )
    }

    /**
     * Admin group only, not for restricted or demo users. Without a configuration of Sipgate the actions would fail
     * with an obscure error of the client, so they are refused with a clear message (the menu is hidden then anyway).
     */
    internal fun checkAccess() {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        accessChecker.checkRestrictedOrDemoUser()
        if (!sipgateConfiguration.isConfigured()) {
            throw UserException("system.admin.sipgate.notConfigured")
        }
    }
}

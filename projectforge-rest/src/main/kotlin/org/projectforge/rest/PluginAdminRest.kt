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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * The **Plugins** administration page (`/next/plugins`), successor of Wicket's `PluginListPage`
 * (`wa/wicket/bookmarkable/org.projectforge.web.admin.PluginListPage`).
 *
 * Like [SystemRest], [PersonalStatisticsRest] and [SendTextMessageRest] this is a non-entity, standalone action
 * page: it exposes plain JSON so the next frontend can render the list and the activate/deactivate buttons, and each
 * action is a thin wrapper around [PluginAdminService] (the same service the Wicket page used).
 *
 * **Access:** the classic page was reachable through an admin-only menu entry and behind the admin 2FA prefix
 * (`org.projectforge.web.admin`, see `ProjectForge2FAInitialization`). These REST endpoints have no DAO access
 * backstop, so every method checks the admin group itself ([checkAdminAccess]); the mutating [setActivated] also
 * checks that the user is neither restricted nor a demo user ([checkWriteAccess]).
 */
@RestController
@RequestMapping("${Rest.URL}/pluginList")
class PluginAdminRest {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var pluginAdminService: PluginAdminService

    /** One available plugin as the frontend renders it. */
    class PluginItem(
        val id: String,
        val name: String,
        val description: String,
        /** Whether the plugin is activated in the database configuration (the toggle's state). */
        val active: Boolean,
        /**
         * Whether the plugin is forced active via `projectforge.plugins.ensure-active` and therefore cannot be
         * deactivated here.
         */
        val ensureActive: Boolean,
    )

    /** The initial state of the page: the available plugins and the ids forced active via configuration. */
    class PluginListData(
        val plugins: List<PluginItem>,
        val ensureActivePluginIds: List<String>,
    )

    /** A plain result message, shown by the frontend as a success toast. */
    class MessageResponse(val message: String)

    class SetActivatedRequest(var id: String? = null, var activate: Boolean = false)

    @AccessChecked("Admin group (checkIsLoggedInUserMemberOfAdminGroup)")
    @GetMapping
    fun getData(): PluginListData {
        checkAdminAccess()
        val activated = pluginAdminService.activatedPluginsFromConfiguration
        val ensureActiveIds = pluginAdminService.ensureActivePluginIds
        val plugins = pluginAdminService.availablePlugins.map { plugin ->
            val info = plugin.info
            PluginItem(
                id = info.id,
                name = info.name,
                description = info.description,
                active = activated.contains(info.id),
                ensureActive = ensureActiveIds.contains(info.id),
            )
        }
        return PluginListData(plugins = plugins, ensureActivePluginIds = ensureActiveIds)
    }

    @AccessChecked("Admin group + not restricted/demo (checkWriteAccess)")
    @PostMapping("setActivated")
    fun setActivated(@RequestBody request: SetActivatedRequest): MessageResponse {
        checkWriteAccess()
        val id = request.id
        require(!id.isNullOrBlank()) { "Plugin id must be given." }
        // A plugin forced active via projectforge.plugins.ensure-active cannot be deactivated here (the frontend
        // disables the button, but a direct REST call must not bypass that).
        require(!(request.activate.not() && pluginAdminService.ensureActivePluginIds.contains(id))) {
            "Plugin '$id' is forced active via projectforge.plugins.ensure-active and cannot be deactivated."
        }
        log.info { "Admin user ${if (request.activate) "activates" else "deactivates"} plugin '$id'." }
        pluginAdminService.storePluginToBeActivated(id, request.activate)
        return MessageResponse(translate("system.pluginAdmin.restartRequired"))
    }

    private fun checkAdminAccess() {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
    }

    private fun checkWriteAccess() {
        checkAdminAccess()
        accessChecker.checkRestrictedOrDemoUser()
    }
}

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


package org.projectforge.rest.feature

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.NextMigration
import org.projectforge.feature.FeatureDefinition
import org.projectforge.feature.FeatureService
import org.projectforge.model.rest.RestPaths
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.rest.config.AttachmentCategoryRequests
import org.projectforge.ui.AutoCompletion
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * Which request paths belong to switched off features (see [FeatureService]), for [FeatureFilter] and the frontend.
 *
 * The owners of a path are the features of the longest of their paths (segment by segment) being a prefix of it
 * ([FeatureDefinition.paths]). The REST endpoints of a plugin which no menu entry claims belong to the plugin's
 * feature (found by the package of the controller), and so do its next pages ([org.projectforge.plugins.core.AbstractPlugin.featureNextRoutes]).
 *
 * A path is blocked, if all of its owners are switched off. Always reachable:
 * - paths without owner, and the core ([CORE_REST], [CORE_REST_PUBLIC]),
 * - the [FeatureDefinition.sharedPaths],
 * - the lookups of the pickers on other pages (`<category>/autosearch`, `<category>/autocomplete`), whose data
 *   are access checked as usual.
 */
@Component
class FeaturePathIndex {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    // Got lazily: built on the first request, when all REST controllers are there.
    private val featureService by lazy { applicationContext.getBean(FeatureService::class.java) }

    private val pluginAdminService by lazy { applicationContext.getBean(PluginAdminService::class.java) }

    private class Index(
        val definitions: List<FeatureDefinition>,
        val owners: Map<String, Set<String>>,
        val shared: Set<String>,
    )

    @Volatile
    private var index: Index? = null

    /** Plugin id -> path keys of its REST controllers and next pages. */
    private val pluginPaths: Map<String, Set<String>> by lazy {
        val plugins = pluginAdminService.availablePlugins
        val result = mutableMapOf<String, MutableSet<String>>()
        applicationContext.getBeansWithAnnotation(RestController::class.java).values.forEach { bean ->
            val clazz = AopUtils.getTargetClass(bean)
            val plugin = RestControllerPaths.pluginOf(clazz, plugins) ?: return@forEach
            val paths = result.getOrPut(plugin.id) { mutableSetOf() }
            RestControllerPaths.categoryKeys(clazz).forEach { key ->
                paths.add(key)
                if (key.startsWith(FeatureDefinition.REST_PREFIX)) {
                    val category = key.removePrefix(FeatureDefinition.REST_PREFIX)
                    paths.add("${FeatureDefinition.NEXT_PREFIX}${NextMigration.routeOrCategory(category)}")
                }
            }
        }
        plugins.forEach { plugin ->
            plugin.featureNextRoutes.forEach {
                result.getOrPut(plugin.id) { mutableSetOf() }.add("${FeatureDefinition.NEXT_PREFIX}${it.trim('/')}")
            }
        }
        result
    }

    /**
     * @param path The request path without context path, e.g. `/rs/address/list` or `/next/address/`.
     */
    fun isBlocked(path: String): Boolean {
        val segments = path.split('/').filter { it.isNotEmpty() }
        if (segments.size < 2) {
            return false
        }
        when (segments[0]) {
            RestPaths.REST -> if (segments[1] == ATTACHMENTS) {
                // upload/<category>/..., download/<category>/..., the others carry it in their body (see FeatureFilter).
                if (segments.size >= 4 && segments[2] in AttachmentCategoryRequests.PATH_CATEGORY_CALLS) {
                    return isCategoryBlocked(segments[3])
                }
                return false
            }

            RestPaths.REST_PUBLIC -> {}
            NEXT -> if (segments[1] == NEXT_ASSETS || segments.getOrNull(2) == NEXT_ASSETS && segments[1] == "public") {
                return false // The assets, also below the public pages (Constants.NEXT_PUBLIC_ASSETS_PATH).
            }
            else -> return false
        }
        if (segments[0] != NEXT && segments.size >= 3 && segments[2] in LOOKUPS) {
            return false
        }
        return isBlocked(segments)
    }

    /**
     * @param category The REST category of an attachment call, e.g. `datatransfer`.
     */
    fun isCategoryBlocked(category: String): Boolean {
        return isBlocked(listOf(RestPaths.REST, category))
    }

    /**
     * The next routes of switched off features, for the frontend (`FeatureGuard`, without `next/`, e.g.
     * `address`). It decides by the longest route being a prefix of the page's path, so the reachable routes below a
     * switched off one are given as well.
     */
    class NextRoutes(val disabled: List<String>, val enabled: List<String>)

    fun nextRoutes(): NextRoutes {
        val states = getIndex().owners.keys.filter { it.startsWith(FeatureDefinition.NEXT_PREFIX) }
            .associate { it.removePrefix(FeatureDefinition.NEXT_PREFIX) to !isBlocked(it.split('/')) }
        val disabled = states.filterValues { !it }.keys.sorted()
        val enabled = states.filterValues { it }.keys
            .filter { route -> disabled.any { route.startsWith("$it/") } }
            .sorted()
        return NextRoutes(disabled, enabled)
    }

    private fun isBlocked(segments: List<String>): Boolean {
        val index = getIndex()
        for (n in segments.size downTo 2) {
            if (n == 2 && isCore(segments[0], segments[1])) {
                return false // A sub path of the core may belong to a feature (menu/customizer), the core itself not.
            }
            val key = segments.subList(0, n).joinToString("/")
            if (key in index.shared) {
                return false
            }
            index.owners[key]?.let { owners ->
                return owners.none { featureService.isEnabled(it) }
            }
        }
        return false
    }

    private fun isCore(root: String, category: String): Boolean {
        return when (root) {
            RestPaths.REST -> category in CORE_REST
            RestPaths.REST_PUBLIC -> category in CORE_REST_PUBLIC
            else -> false
        }
    }

    private fun getIndex(): Index {
        val definitions = featureService.definitions
        index?.let { if (it.definitions === definitions) return it }
        val owners = mutableMapOf<String, MutableSet<String>>()
        val shared = mutableSetOf<String>()
        definitions.forEach { definition ->
            definition.paths.forEach { owners.getOrPut(it) { mutableSetOf() }.add(definition.id) }
            shared.addAll(definition.sharedPaths)
        }
        // Paths of plugins not claimed by a menu entry: switched off with the plugin.
        pluginPaths.forEach { (pluginId, paths) ->
            paths.forEach { path ->
                if (owners[path].isNullOrEmpty() && path.substringAfter('/') !in CORE_REST) {
                    owners.getOrPut(path) { mutableSetOf() }.add(pluginId)
                }
            }
        }
        log.debug { "Feature paths: $owners" }
        return Index(definitions, owners, shared).also { index = it }
    }

    companion object {
        private const val NEXT = "next"

        /** The assets of projectforge-next. */
        private const val NEXT_ASSETS = "_next"

        private const val ATTACHMENTS = "attachments"

        private val LOOKUPS = setOf(AutoCompletion.AUTOCOMPLETE_OBJECT, AutoCompletion.AUTOCOMPLETE_TEXT)

        /**
         * The REST categories below `/rs` of the app itself (login state, menu, settings, notifications, 2FA, history,
         * attachments): always reachable.
         */
        val CORE_REST = setOf(
            "2FA",
            "attachment",
            ATTACHMENTS,
            "historyEntries",
            "index",
            "logout",
            "menu",
            "next2FA",
            "notification",
            "timeZones",
            "uiSettings",
            "userStatus",
            "webauthn",
            "WebAuthnSetup",
        )

        /** The REST categories below `/rsPublic` of the app itself (login, setup, status, texts, logos): always reachable. */
        val CORE_REST_PUBLIC = setOf(
            "authenticationCredentials",
            "heartbeat",
            "i18nCustomerOverrides",
            "logo.gif",
            "logo.jpg",
            "logo.png",
            "logoDark.gif",
            "logoDark.jpg",
            "logoDark.png",
            "next2FALogin",
            "nextLogin",
            "nextPasswordReset",
            // Called by the phone system (sipgate).
            "phoneLookup",
            "setup",
            "systemStatus",
        )
    }
}

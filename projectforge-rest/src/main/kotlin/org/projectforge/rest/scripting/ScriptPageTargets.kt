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


package org.projectforge.rest.scripting

import org.projectforge.NextMigration
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.rest.core.AbstractEntityRest
import org.projectforge.rest.fibu.OrderEntityRest
import org.projectforge.rest.fibu.OrderStatisticsFilterService
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

/**
 * The pages a script can show its button on (see [org.projectforge.business.scripting.ScriptDO.pageTargets]) and the
 * [ScriptPageContext] of a script started there. A page target is a string:
 * - `list:<category>`: the list page of a projectforge-next entity, e.g. `list:order` (the order book),
 * - `orderStatistics:<tab>`: a tab of the order statistics, `orderStatistics:forecast` or
 *   `orderStatistics:contributionMargin`.
 *
 * The context needs no data from the client: list pages and the order statistics keep the user's current
 * filter on the server.
 */
@Service
class ScriptPageTargets {
    /**
     * A selectable page target with its translated title.
     * @param route The route of the page in projectforge-next, e.g. `/order`.
     */
    class PageTarget(val id: String, val title: String, val route: String)

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var orderStatisticsFilterService: OrderStatisticsFilterService

    @Autowired
    private lateinit var pluginAdminService: PluginAdminService

    /**
     * The list rests of the projectforge-next list pages by category. Looked up on first use and not injected:
     * the script rests are list rests themselves, injecting them all would be a circular dependency.
     */
    private val allListRests: Map<String, AbstractEntityRest<*, *, *>> by lazy {
        applicationContext.getBeansOfType(AbstractEntityRest::class.java).values
            .filter { NextMigration.isMigrated(it.category) }
            .associateBy { it.category }
    }

    /**
     * [allListRests] without those of the plugins not activated: their pages aren't usable, and their i18n bundles
     * aren't even loaded. A rest belongs to a plugin, if it's located in the plugin's package or a sub package.
     */
    private val listRests: Map<String, AbstractEntityRest<*, *, *>>
        get() {
            val inactivePluginPackages = pluginAdminService.availablePlugins
                .filter { !pluginAdminService.isActive(it.javaClass) }
                .map { it.javaClass.packageName }
            return allListRests.filterValues { rest ->
                val restPackage = AopUtils.getTargetClass(rest).packageName
                inactivePluginPackages.none { restPackage == it || restPackage.startsWith("$it.") }
            }
        }

    private val orderEntityRest: OrderEntityRest by lazy { applicationContext.getBean(OrderEntityRest::class.java) }

    /** All page targets, the order statistics first, then the list pages by title. */
    fun getAll(): List<PageTarget> {
        return STATISTICS_TABS.mapNotNull { getPageTarget("$STATISTICS_PREFIX$it") } +
                listRests.keys.mapNotNull { getPageTarget("$LIST_PREFIX$it") }.sortedBy { it.title.lowercase() }
    }

    /** The page target of [id], null if unknown. */
    fun getPageTarget(id: String?): PageTarget? {
        listCategory(id)?.let { category ->
            val rest = listRests[category] ?: return null
            return PageTarget(id!!, listTitle(rest), "/${NextMigration.routeOrCategory(category)}")
        }
        statisticsTab(id)?.let { tab ->
            val tabTitle = translate(if (tab == TAB_FORECAST) "fibu.auftrag.statistics.forecast" else "fibu.auftrag.contributionMargin._")
            return PageTarget(id!!, "${translate("menu.fibu.orderStatistics")}: $tabTitle", "/finance/statistics?tab=$tab")
        }
        return null
    }

    /**
     * The title of the list page: mostly `<i18nKeyPrefix>.list` (e.g. `fibu.auftrag.list`), but some rests have a
     * prefix without the `title` part their list key has (`administration.configuration.title.list`). The
     * category, if neither is translated (a missing key is returned as it is).
     */
    private fun listTitle(rest: AbstractEntityRest<*, *, *>): String {
        return listOf("${rest.i18nKeyPrefix}.list", "${rest.i18nKeyPrefix}.title.list")
            .firstNotNullOfOrNull { key -> translate(key).takeIf { it != key } }
            ?: rest.category
    }

    /** Only the known page targets of [ids], without duplicates. */
    fun sanitize(ids: Collection<String>?): List<String> {
        return ids?.map { it.trim() }?.distinct()?.filter { getPageTarget(it) != null } ?: emptyList()
    }

    /**
     * The context of a script started from the page of [target]; an empty one if [target] is null or unknown.
     */
    fun createContext(target: String?): ScriptPageContext {
        val pageTarget = getPageTarget(target) ?: return ScriptPageContext()
        listCategory(target)?.let { category ->
            val rest = listRests[category] ?: return ScriptPageContext()
            val filter = rest.getCurrentFilter()
            return ScriptPageContext(pageTarget.id, pageTarget.title, filter, asCallingUser {
                rest.getFilteredList(filter.clone()).filterNotNull()
            })
        }
        val filter = orderStatisticsFilterService.getCurrentFilter()
        return ScriptPageContext(pageTarget.id, pageTarget.title, filter, asCallingUser {
            orderEntityRest.getFilteredList(filter.clone())
        })
    }

    /**
     * [block] with the rights of the user starting the script, even if called later on by a script running
     * as `executeAsUser`: the context is the page the user saw.
     */
    private fun asCallingUser(block: () -> List<Any>): () -> List<Any> {
        val userContext = ThreadLocalUserContext.userContext
        return {
            val saved = ThreadLocalUserContext.userContext
            try {
                ThreadLocalUserContext.userContext = userContext
                block()
            } finally {
                ThreadLocalUserContext.userContext = saved
            }
        }
    }

    companion object {
        internal const val LIST_PREFIX = "list:"

        internal const val STATISTICS_PREFIX = "orderStatistics:"

        internal const val TAB_FORECAST = "forecast"

        internal const val TAB_CONTRIBUTION_MARGIN = "contributionMargin"

        private val STATISTICS_TABS = listOf(TAB_FORECAST, TAB_CONTRIBUTION_MARGIN)

        /** The category of a list page target, e.g. `order` of `list:order`, otherwise null. */
        internal fun listCategory(target: String?): String? {
            return target?.takeIf { it.startsWith(LIST_PREFIX) }?.removePrefix(LIST_PREFIX)?.takeIf { it.isNotBlank() }
        }

        /** The tab of an order statistics target, e.g. `forecast`, otherwise null (also for an unknown tab). */
        internal fun statisticsTab(target: String?): String? {
            return target?.takeIf { it.startsWith(STATISTICS_PREFIX) }?.removePrefix(STATISTICS_PREFIX)
                ?.takeIf { it in STATISTICS_TABS }
        }
    }
}

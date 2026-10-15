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

import org.projectforge.framework.persistence.api.MagicFilter
import kotlin.reflect.KClass

/**
 * The page a script was started from by its button (see [org.projectforge.business.scripting.ScriptDO.pageTargets]),
 * available in every script as variable `pageContext`. Started otherwise (e.g. from the script list), [target]
 * is null and [list] is empty.
 *
 * Example (Kotlin), started from the order book or the order statistics:
 * ```
 * val orders = pageContext.list(AuftragDO::class)
 * "${orders.size} orders of ${pageContext.title}"
 * ```
 */
@Suppress("unused")
class ScriptPageContext internal constructor(
    /** The page target, e.g. `list:order` or `orderStatistics:forecast`, null if not started from a page. */
    val target: String? = null,
    /** The title of the page, e.g. "Order book", null if not started from a page. */
    val title: String? = null,
    /**
     * The current filter of the page as the user left it: of a list page the whole list filter, of the
     * order statistics only its business units, customers and projects.
     */
    val filter: MagicFilter? = null,
    private val listSupplier: (() -> List<Any>)? = null,
) {
    /** True, if the script was started from a page by its button. */
    val fromPage: Boolean
        get() = target != null

    /** The entity of a list page, e.g. `order` of `list:order`, otherwise null. */
    val entity: String?
        get() = ScriptPageTargets.listCategory(target)

    /** The tab of the order statistics, e.g. `forecast` of `orderStatistics:forecast`, otherwise null. */
    val tab: String?
        get() = ScriptPageTargets.statisticsTab(target)

    private val cachedList by lazy { listSupplier?.invoke() ?: emptyList() }

    /**
     * The entities the page shows for its [filter], loaded on first call with the access rights of the
     * logged-in user (not of `executeAsUser`): the rows of a list page, the orders of the order statistics.
     */
    fun list(): List<Any> = cachedList

    /** [list] of the given type, e.g. `pageContext.list(AuftragDO::class)`. */
    fun <T : Any> list(clazz: KClass<T>): List<T> = cachedList.filterIsInstance(clazz.java)

    override fun toString(): String = "ScriptPageContext(target=$target, title=$title)"
}

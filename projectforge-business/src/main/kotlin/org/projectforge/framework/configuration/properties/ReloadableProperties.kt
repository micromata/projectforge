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

package org.projectforge.framework.configuration.properties

import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.Environment

/**
 * A bean whose parameters take effect without a restart: after the properties editor saved projectforge.properties,
 * [ProjectForgePropertiesReloader] reloads the file and calls [reload]. Only for parameters nothing else copies at
 * startup (a pool, a filter, a cron, a cache): the bean must read its fields on every use.
 *
 * The keys of all such beans are the whitelist the editor marks as "takes effect immediately".
 */
interface ReloadableProperties {
    /** Exact keys, or prefixes ending with a dot (`mail.session.pfmailsession.`). */
    val reloadableKeys: List<String>

    /** Reads the parameters of [reloadableKeys] again; a parameter removed from the file falls back to its default. */
    fun reload(environment: Environment)

    fun isReloadable(key: String): Boolean =
        reloadableKeys.any { if (it.endsWith(".")) key.startsWith(it) else key == it }
}

/**
 * A new instance of a `@ConfigurationProperties` class bound to [prefix] (without the trailing dot), for its
 * [ReloadableProperties.reload]: unlike binding into the bean itself, a parameter removed from the file gets its default
 * again.
 */
fun <T : Any> Environment.bindFresh(prefix: String, type: Class<T>): T = Binder.get(this).bindOrCreate(prefix, type)

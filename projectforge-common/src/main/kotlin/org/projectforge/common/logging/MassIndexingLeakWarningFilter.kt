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


package org.projectforge.common.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.turbo.TurboFilter
import ch.qos.logback.core.spi.FilterReply
import org.slf4j.Marker

/**
 * Drops Hikari's connection leak warnings (and their "returned to the pool" follow-ups) for the threads of Hibernate
 * Search's mass indexer: it holds one transaction per thread for the whole run of an entity class (a minute for the
 * time sheets), which always exceeds the leak detection threshold, although the connections are returned. A runtime
 * change of the threshold isn't an option, as Hikari applies it only on its next housekeeping run and not to
 * connections already borrowed. Leaks of all other threads are still logged. Registered in logback-spring.xml.
 */
class MassIndexingLeakWarningFilter : TurboFilter() {
    override fun decide(
        marker: Marker?,
        logger: Logger?,
        level: Level?,
        format: String?,
        params: Array<out Any?>?,
        t: Throwable?,
    ): FilterReply {
        if (logger?.name != HIKARI_LEAK_LOGGER || params == null) {
            return FilterReply.NEUTRAL
        }
        return if (params.any { it is String && it.startsWith(MASS_INDEXING_THREAD_PREFIX) }) {
            FilterReply.DENY
        } else {
            FilterReply.NEUTRAL
        }
    }

    companion object {
        internal const val HIKARI_LEAK_LOGGER = "com.zaxxer.hikari.pool.ProxyLeakTask"
        internal const val MASS_INDEXING_THREAD_PREFIX = "Hibernate Search - Mass indexing"
    }
}

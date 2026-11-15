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
 * Drops Flyway's warnings "DB: no data (SQL State: 02000 - Error Code: -1100)": HSQLDB answers every UPDATE or DELETE
 * of a migration that touches no row with this SQL warning, and Flyway logs every warning as WARN. On a fresh
 * installation the data migrations run against empty tables, so there are many of them, although nothing is wrong.
 * Flyway has no option to silence them, and shipped migrations can't be changed (checksums). Other warnings of Flyway
 * are still logged. Registered in logback-spring.xml.
 */
class FlywayNoDataWarningFilter : TurboFilter() {
    override fun decide(
        marker: Marker?,
        logger: Logger?,
        level: Level?,
        format: String?,
        params: Array<out Any?>?,
        t: Throwable?,
    ): FilterReply {
        if (logger?.name != FLYWAY_SQL_SCRIPT_EXECUTOR_LOGGER || format == null) {
            return FilterReply.NEUTRAL
        }
        // Flyway builds the message itself: "DB: <message> (SQL State: <state> - Error Code: <code>)".
        return if (format.contains(NO_DATA_STATE)) FilterReply.DENY else FilterReply.NEUTRAL
    }

    companion object {
        internal const val FLYWAY_SQL_SCRIPT_EXECUTOR_LOGGER =
            "org.flywaydb.core.internal.sqlscript.DefaultSqlScriptExecutor"
        internal const val NO_DATA_STATE = "(SQL State: 02000 "
    }
}

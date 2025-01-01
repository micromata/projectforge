/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import ch.qos.logback.classic.spi.ILoggingEvent
import org.projectforge.common.i18n.I18nEnum

enum class LogLevel(val key: String) : I18nEnum {
    FATAL("fatal"), ERROR("error"), WARN("warn"), INFO("info"), DEBUG("debug"), TRACE("trace");

    fun safeValueOf(name: String): LogLevel? {
        return LogLevel.entries.firstOrNull { it.name == name }
    }

    /**
     * @param threshold
     * @return True, if this log level is equals or higher than given threshold. ERROR is the highest and TRACE the lowest.
     */
    fun matches(threshold: LogLevel?): Boolean {
        return threshold == null || ordinal <= threshold.ordinal
    }

    /**
     * @return The full i18n key including the i18n prefix "fibu.auftrag.status.".
     */
    override val i18nKey: String
        get() = "log.level.$key"

    companion object {
        fun getLevel(event: ILoggingEvent): LogLevel {
            return when (event.level.toInt()) {
                Level.INFO_INT -> INFO
                Level.DEBUG_INT -> DEBUG
                Level.WARN_INT -> WARN
                Level.TRACE_INT -> TRACE
                else -> ERROR
            }
        }
    }
}

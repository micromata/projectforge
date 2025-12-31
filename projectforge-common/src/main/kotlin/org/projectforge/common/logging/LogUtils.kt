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

import mu.KLogger

/**
 * Utility class for logging.
 */
object LogUtils {
    /**
     * Logs a debug message with the given [message] if the logger is enabled for debug.
     * Lazy build a log message if isDebugEnabled is true.
     * @param log the logger to use
     * @param lb the log builder
     */
    fun logDebugFunCall(log: KLogger, builder: (lb: LogBuilder) -> Unit) {
        if (!log.isDebugEnabled) {
            return
        }
        LogBuilder(log).also {
            builder(it)
            it.log()
        }
    }
}

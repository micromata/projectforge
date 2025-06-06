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

package org.projectforge.common

/**
 * This class is used to track the number of active coroutines.
 * This class is used to find any open, non-terminating coroutines.
 * These can be displayed, for example, via system statistics. Example: SseEmitterTool.
 */
class CoroutineTracker {
    var actives = 0

    var created = 0
        private set

    var completed = 0

    var errorMessage: String? = null

    fun increment() {
        synchronized(this) {
            actives++
            created++
        }
    }

    fun decrement() {
        synchronized(this) {
            if (actives <= 0) {
                errorMessage = "actives <= 0: $actives. This should not happen!"
            } else {
                actives--
            }
            completed++
        }
    }
}

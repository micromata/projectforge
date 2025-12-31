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

package org.projectforge.framework.json

/**
 * Thread local context for JSON serialization.
 * This context is used to control the behavior of the [IdOnlySerializer].
 */
class JsonThreadLocalContext(
    val preferEmbeddedSerializers: Boolean = false,
    val ignoreIdOnlySerializers: Boolean = false,
) {
    companion object {
        private val threadLocal = ThreadLocal<JsonThreadLocalContext>()

        fun get(): JsonThreadLocalContext? {
            return threadLocal.get()
        }

        fun set(preferEmbeddedSerializers: Boolean = false, ignoreIdOnlySerializers: Boolean = false) {
            threadLocal.set(
                JsonThreadLocalContext(
                    preferEmbeddedSerializers = preferEmbeddedSerializers,
                    ignoreIdOnlySerializers = ignoreIdOnlySerializers,
                )
            )
        }

        fun clear() {
            threadLocal.remove()
        }
    }
}

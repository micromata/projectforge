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

package org.projectforge.framework.persistence.jpa

/**
 */
class PersistenceCallsStatsBuilder {
    companion object {
        /**
         * Any values of these keys containing one of the values in dataProtectionKeys will be replaced by "xxx" in the log output.
         * This is useful for hiding sensitive data in log output.
         * The check is case-insensitive.
         */
        private val dataProtectionKeys = arrayOf("password", "token")
    }

    val sb = StringBuilder()
    private var firstEntry = true
    fun resultClass(resultClass: Class<*>): PersistenceCallsStatsBuilder {
        appendComma()
        sb.append("resultClass=").append(resultClass.simpleName)
        return this
    }

    fun keyValues(keyValues: Array<out Pair<String, Any?>>): PersistenceCallsStatsBuilder {
        if (keyValues.isEmpty()) {
            return this
        }
        appendComma()
        sb.append("keyValues={")
        keyValues.joinTo(buffer = sb, separator = ",") { keyValueAsString(it) }
        sb.append("}")
        return this
    }

    private fun keyValueAsString(keyValue: Pair<String, Any?>): String {
        if (dataProtectionKeys.any { keyValue.first.contains(it, ignoreCase = true) }) {
            return "${keyValue.first}=xxx"
        }
        return "${keyValue.first}=${keyValue.second}"
    }

    fun param(name: String, value: Any?): PersistenceCallsStatsBuilder {
        if (value != null) {
            appendComma()
            sb.append(name).append("=").append(value)
        }
        return this
    }

    fun text(text: String?): PersistenceCallsStatsBuilder {
        if (!text.isNullOrBlank()) {
            appendComma()
            sb.append(text)
        }
        return this
    }

    override fun toString(): String {
        return sb.toString()
    }

    private fun appendComma() {
        if (!firstEntry) {
            sb.append(",")
        }
        firstEntry = false
    }
}

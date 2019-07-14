/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.api

import com.fasterxml.jackson.annotation.JsonIgnore
import org.projectforge.framework.persistence.api.MagicFilterEntry.MatchType

class MagicFilterEntry(
        /**
         * Optional name of a field for a field specific search. Null for global search.
         */
        var field: String? = null,
        /**
         * Search the given text.
         */
        var search: String? = null,
        /**
         * The match strategy for the string search. [MatchType.STARTS_WITH] is the default.
         */
        var matchType: MatchType? = null,
        /**
         * Find entries where the given field is equals to this given single value.
         */
        var value: Any? = null,
        /**
         * Find entries where the given field is equals or higher than the given fromValue (range search).
         */
        var fromValue: Any? = null,
        /**
         * Find entries where the given field is equals or lower than the given toValue (range search).
         */
        var toValue: Any? = null,
        /**
         * Find entries where the given field has one of the given values).
         */
        var values: MutableList<Any>? = null) {

    enum class MatchType {
        /**
         * '*string*'
         */
        CONTAINS,
        /**
         * 'string'
         */
        EXACT,
        /**
         * 'string*' (default)
         */
        STARTS_WITH,
        /**
         * '*string'
         */
        ENDS_WITH
    }

    internal enum class Type { NONE, STRING_SEARCH, FIELD_STRING_SEARCH, FIELD_RANGE_SEARCH, FIELD_VALUES_SEARCH }

    @JsonIgnore
    private val log = org.slf4j.LoggerFactory.getLogger(MagicFilterEntry::class.java)

    internal fun type(): Type {
        val valuesGiven = !values.isNullOrEmpty()
        if (field == null) {
            if (value != null || fromValue != null || toValue != null || valuesGiven) {
                log.warn("MagicFilterEntry inconsistent: No field given, value, fromValue, toValue and values are ignored.")
            }
            if (search.isNullOrBlank()) {
                return Type.NONE
            }
            return Type.STRING_SEARCH
        }
        if (search.isNullOrBlank()) {
            if (value != null || fromValue != null || toValue != null || valuesGiven) {
                log.warn("MagicFilterEntry inconsistent for field '$field' search ('$search'): value, fromValue, toValue and values are ignored.")
            }
            return Type.FIELD_STRING_SEARCH
        }
        if (value != null && value is String) {
            if (fromValue != null || toValue != null || valuesGiven) {
                log.warn("MagicFilterEntry inconsistent for field '$field' search ('$value'): fromValue, toValue and values are ignored.")
            }
            return Type.FIELD_STRING_SEARCH
        }
        if (fromValue != null || toValue != null) {
            if (valuesGiven) {
                log.warn("MagicFilterEntry inconsistent for field '$field' range search (from '$fromValue' to '$toValue'): values are ignored.")
            }
            return Type.FIELD_RANGE_SEARCH
        }
        if (valuesGiven) {
            return Type.FIELD_VALUES_SEARCH
        }
        return Type.NONE // Nothing given for field search (might be OK).
    }

    internal fun getSearchStringStrategy(): String {
        var str = search
        if (search == null && (value == null || value !is String)) {
            return ""
        }
        if (str == null) {
            str = value as String
        }
        return when (matchType) {
            MatchType.EXACT -> str
            MatchType.ENDS_WITH -> "*$str"
            MatchType.CONTAINS -> "*$str*"
            else -> "$str*"
        }
    }

    fun isModified(other: MagicFilterEntry): Boolean {
        if (this.field != other.field) return true
        if (this.fromValue != other.fromValue) return true
        if (this.matchType != other.matchType) return true
        if (this.search != other.search) return true
        if (this.toValue != other.toValue) return true
        if (this.value != other.value) return true
        val values1 = this.values
        val values2 = other.values
        if (values1 == null) {
            return values2 != null
        }
        if (values2 == null) {
            return true
        }
        if (values1.size != values2.size) {
            return true
        }
        values1.forEachIndexed { i, value ->
            if (values2[i] != value) {
                return true
            }
        }
        return false
    }
}

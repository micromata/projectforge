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
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import java.util.*

class MagicFilterEntry(
        /**
         * Optional name of a field for a field specific search. Null for global search.
         */
        var field: String? = null,
        /**
         * Value representsFind entries where the given field is equals to this given single value, or as search string.
         */
        var value: String? = null) {

    @JsonIgnore
    internal var type: Class<*>? = null

    @JsonIgnore
    internal var dbSearchString: String? = null

    @JsonIgnore
    internal var plainSearchString: String? = null

    @JsonIgnore
    internal var matchType: MatchType? = null

    @JsonIgnore
    internal var searchType: SearchType? = null

    @JsonIgnore
    var fromValue: String? = null
        private set

    @JsonIgnore
    var toValue: String? = null
        private set

    @JsonIgnore
    var values: Array<String>? = null
        private set

    @JsonIgnore
    var fromValueDate: PFDateTime? = null
        private set

    @JsonIgnore
    var toValueDate: PFDateTime? = null
        private set

    @JsonIgnore
    var valueInt: Int? = null
        private set

    @JsonIgnore
    var fromValueInt: Int? = null
        private set

    @JsonIgnore
    var toValueInt: Int? = null
        private set

    /**
     * Only for documentation
     */
    private class Value(
            /**
             * Find entries where the given field is equals to this given single value, or as search string.
             */
            var value: String? = null,
            /**
             * Find entries where the given field is equals or higher than the given fromValue (range search).
             */
            var fromValue: String? = null,
            /**
             * Find entries where the given field is equals or lower than the given toValue (range search).
             */
            var toValue: String? = null,
            /**
             * Find entries where the given field has one of the given values).
             */
            var values: MutableList<String>? = null)

    internal enum class SearchType { NONE, STRING_SEARCH, FIELD_STRING_SEARCH, FIELD_RANGE_SEARCH, FIELD_VALUES_SEARCH }

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

    @JsonIgnore
    private val log = org.slf4j.LoggerFactory.getLogger(MagicFilterEntry::class.java)

    internal fun analyze(entityClass: Class<*>) {
        val fieldType = PropUtils.getField(entityClass, field)?.type ?: String::class.java
        this.type = fieldType
        if (fieldType == String::class.java) {
            searchType = if (field.isNullOrBlank()) SearchType.STRING_SEARCH else SearchType.FIELD_STRING_SEARCH
            val str = value?.trim() ?: ""
            var plainStr = str
            var dbStr: String
            if (str.startsWith("*")) {
                plainStr = plainStr.substring(1)
                if (str.endsWith("*")) {
                    plainStr = plainStr.substring(0, plainStr.lastIndex)
                    dbStr = "%$plainStr%"
                    matchType = MatchType.CONTAINS
                } else {
                    dbStr = "%$plainStr"
                    matchType = MatchType.STARTS_WITH
                }
            } else {
                if (str.endsWith("*")) {
                    plainStr = plainStr.substring(0, plainStr.lastIndex)
                    dbStr = "$plainStr%"
                    matchType = MatchType.ENDS_WITH
                } else {
                    matchType = MatchType.EXACT
                    dbStr = "$plainStr"
                }
            }
            this.plainSearchString = plainStr
            this.dbSearchString = dbStr
        } else if (fieldType == Date::class.java) {
            fromValueDate = PFDateTime.parseUTCDate(fromValue)
            toValueDate = PFDateTime.parseUTCDate(toValue)
        } else if (fieldType == Integer::class.java) {
            valueInt = NumberHelper.parseInteger(value)
            fromValueInt = NumberHelper.parseInteger(fromValue)
            toValueInt = NumberHelper.parseInteger(toValue)
        } else if (BaseDO::class.java.isAssignableFrom(fieldType)) {
            valueInt = NumberHelper.parseInteger(value)
        } else {
            log.warn("Search entry of type '${fieldType.name}' not yet supported for field '$field'.")
        }
    }

    fun isModified(other: MagicFilterEntry): Boolean {
        if (this.field != other.field) return true
        if (this.value != other.value) return true
        return false
    }
}

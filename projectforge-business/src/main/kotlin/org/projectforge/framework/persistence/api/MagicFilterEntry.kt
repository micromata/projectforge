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

class MagicFilterEntry(
        /**
         * Optional name of a field for a field specific search. Null for global search.
         */
        var field: String? = null,
        /**
         * Value representsFind entries where the given field is equals to this given single value, or as search string.
         */
        var value: String? = null) {

    /**
     * Find entries where the given field is equals or higher than the given fromValue (range search).
     */
    var fromValue: String? = null

    /**
     * Find entries where the given field is equals or lower than the given toValue (range search).
     */
    var toValue: String? = null

    /**
     * Find entries where the given field has one of the given values).
     */
    var values: Array<String>? = null
        private set

    @JsonIgnore
    var isNew: Boolean? = false
        private set

    fun isModified(other: MagicFilterEntry): Boolean {
        if (this.field != other.field) return true
        if (this.value != other.value) return true
        return false
    }
}

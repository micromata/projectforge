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

package org.projectforge.framework.persistence.api.impl

import com.fasterxml.jackson.annotation.JsonIgnore
import org.projectforge.framework.time.PFDateTime

/**
 * The MagicFilterEntries will be transformed into DBFilterExpressions.
 */
class DBFilterEntry(
        /**
         * Optional name of a field for a field specific search. Null for global search.
         */
        var field: String? = null,
        /**
         * Value representsFind entries where the given field is equals to this given single value, or as search string.
         */
        var value: String? = null,
        /**
         * If true, then this expression will be processed by Hibernate search otherwise by Hibernate criteria.
         */
        var fulltextSearch: Boolean = false) {

    @JsonIgnore
    internal var type: Class<*>? = null

    /**
     * The search string for data base queries (SQL), '*' will be replaced by '%'.
     */
    @JsonIgnore
    internal var dbSearchString: String? = null

    @JsonIgnore
    internal var plainSearchString: String? = null


    @JsonIgnore
    internal var matchType: MatchType? = null

    @JsonIgnore
    internal var searchType: SearchType? = null

    var fromValue: String? = null
        private set

    var toValue: String? = null
        private set

    var values: Array<String>? = null

    var fromValueDate: PFDateTime? = null

    var toValueDate: PFDateTime? = null

    var valueInt: Int? = null

    var fromValueInt: Int? = null

    var toValueInt: Int? = null

    var isNew: Boolean? = false
}

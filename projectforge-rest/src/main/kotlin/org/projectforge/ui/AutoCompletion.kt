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

package org.projectforge.ui

/**
 * An element for the UI specifying the methods of autocompletion.
 */
class AutoCompletion<T>(
        /**
         * The number of minimum characters before the auto-completion call will be executed.
         * Default is 2. Has no effect, if values are given.
         */
        var minChars: Int? = null,
        /**
         * If given, the frontend gets all values for auto-completion, no server call needed.
         */
        var values: List<Entry<T>>? = null,
        /**
         * The recent or favorite entries, if given, will be shown as favorites for quick select
         * (in rest client as star beside the select input).
         */
        var favorites: List<Entry<T>>? = null,
        /**
         * If given, the url will be called for getting the auto-completion values.
         */
        var url: String? = null) {
    class Entry<T>(val value: T,
                   /**
                    * The title to display.
                    */
                   val label: String,
                   /**
                    * Optional if more fields will be used for the search. If not given, the
                    * frontend should use the title to search.
                    */
                   var allSearchableFields: String? = null)

    @Transient
    private val log = org.slf4j.LoggerFactory.getLogger(AutoCompletion::class.java)


    init {
        if (values == null) {
            if (minChars == null) {
                minChars = 2
            }
        } else {
            if (minChars != null || url != null) {
                log.warn("Attribute values can't be combined with minChars and url.")
                minChars = null
                url = null
            }
        }
    }
}

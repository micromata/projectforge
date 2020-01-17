/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
         * Type of autocompletion values as information for the clients (e. g. user).
         */
        var type: String? = null,
        /**
         * If given, the url will be called for getting the auto-completion values.
         */
        var url: String? = null) {
    /**
     * Pre-defined types of autocompletion objects as information for the clients.
     */
    enum class Type { USER, GROUP }
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

    companion object {
        const val AUTOCOMPLETE_TEXT = "autocomplete"
        const val AUTOCOMPLETE_OBJECT = "autosearch"
        const val SHOW_ALL_PARAM = "showAll"

        /**
         * @return category/autosearch?search=:search)
         */
        fun getAutoCompletionUrl(category: String, additionalParamString: String = ""): String {
            return "$category/${AutoCompletion.AUTOCOMPLETE_OBJECT}?${additionalParamString}search=" // :search"
        }

        /**
         * @return category/autosearch?search=
         */
        fun getAutoCompletion4Users(showOnlyActiveUsers: Boolean = true): AutoCompletion<Int> {
            val additionalParamString = if (showOnlyActiveUsers) "" else "$SHOW_ALL_PARAM=true&"
            return AutoCompletion<Int>(url = getAutoCompletionUrl("user", additionalParamString), type = AutoCompletion.Type.USER.name)
        }

        /**
         * @return category/autosearch?search=
         */
        fun getAutoCompletion4Groups(): AutoCompletion<Int> {
            return AutoCompletion<Int>(url = getAutoCompletionUrl("group"), type = AutoCompletion.Type.GROUP.name)
        }
    }
}

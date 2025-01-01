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

package org.projectforge.ui.filter

import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UIElement
import org.projectforge.ui.UIElementType
import org.projectforge.ui.UILabelledElement
import java.time.LocalDate
import java.util.*

/**
 * An element for the UI specifying a filter attribute which may be added by the user to the search string.
 * Filter attributes are e. g. title or authors for books as well as modifiedInIntervall or modifiedByUser.
 */
open class UIFilterElement(
        /**
         *  The id (property) of the filter to be defined.
         */
        var id: String,
        /**
         * Dependent on this type the ui offers different options. For strings (default) a simple input
         * text field is used, for date ranges date-picker etc.
         */
        var filterType: FilterType? = FilterType.STRING,
        override var label: String? = null,
        override var additionalLabel: String? = null,
        override var tooltip: String? = null,
        @Transient
        override val ignoreAdditionalLabel: Boolean = false,
        @Transient
        override val ignoreTooltip: Boolean = false,
        @Transient
        override val layoutContext: LayoutContext? = null,
        /**
         * If true, this filter should be permanent visible on client's list view.
         */
        var defaultFilter: Boolean? = null
) : UIElement(UIElementType.FILTER_ELEMENT), UILabelledElement {
    enum class FilterType { STRING, DATE, TIMESTAMP, BOOLEAN, OBJECT, LIST }

    init {
        key = id
    }

    fun determine(propertyType: Class<*>) {
        if (BaseDO::class.java.isAssignableFrom(propertyType)) {
            filterType = UIFilterElement.FilterType.OBJECT
            return
        }
        when (propertyType) {
            Boolean::class.java, java.lang.Boolean::class.java ->
                filterType = FilterType.BOOLEAN
            Date::class.java ->
                filterType = FilterType.TIMESTAMP
            LocalDate::class.java ->
                filterType = FilterType.DATE
            java.sql.Timestamp::class.java ->
                filterType = FilterType.TIMESTAMP
        }
    }
}

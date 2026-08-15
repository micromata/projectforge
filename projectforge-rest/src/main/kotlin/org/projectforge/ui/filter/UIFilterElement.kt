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

    /**
     * Translated label of the group this field belongs to, taken from the parent chain of its
     * [org.projectforge.ui.ElementInfo] (e. g. "Kunde" for `kunde.name`), or null for a top level field.
     *
     * A grouping hint for the client: a list offers every search field of its entity, which is a flat
     * list of 40 fields for an order. Where the client groups by this, [shortLabel] is what it shows.
     */
    var group: String? = null

    /**
     * The field's own label without the group prefix ("Name" instead of "Kunde - Name"), or null if
     * [label] already is it.
     *
     * [label] stays the full path on purpose: it is the sort key of the field list, the text of the
     * client's filter pill and the text its field search matches. Shortened, an order would show
     * "Name" three times with no way to tell the fields apart.
     */
    var shortLabel: String? = null

    /**
     * A field the entity indexes but never declares: no `@PropertyInfo`, so it has no translation and
     * its label falls back to the property name (`attachmentsIds`). Offered, because it is searchable,
     * but not worth a place among the fields a user came for.
     */
    var technical: Boolean? = null

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

/**
 * Puts a hand-made filter element into a group, for the elements an
 * [org.projectforge.rest.core.AbstractEntityRest.addMagicFilterElements] adds itself: only the fields
 * derived from a property have a parent chain [LayoutListFilterUtils] could read the group from.
 *
 * @param shortLabel The label to show inside the group; the full label is kept where the group isn't shown.
 */
fun <T : UIFilterElement> T.inGroup(group: String, shortLabel: String? = null): T {
    this.group = group
    this.shortLabel = shortLabel
    return this
}

/** Marks a hand-made element as [UIFilterElement.technical], which is autodetected for property fields. */
fun <T : UIFilterElement> T.asTechnical(): T {
    technical = true
    return this
}

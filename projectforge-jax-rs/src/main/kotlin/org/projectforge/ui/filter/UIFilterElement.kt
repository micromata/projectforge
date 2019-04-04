package org.projectforge.ui.filter

import org.projectforge.ui.UIElement
import org.projectforge.ui.UIElementType

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
        var filterType: FilterType? = FilterType.STRING
) : UIElement(UIElementType.FILTER_ELEMENT) {
    enum class FilterType { STRING, DATE, TIME_STAMP, CHOICE }

    init {
        key = id
    }
}
package org.projectforge.ui.filter

import org.projectforge.ui.AutoCompletion

/**
 * An element for the UI specifying a filter attribute which may be added by the user to the search string.
 * Filter attributes are e. g. title or authors for books as well as modifiedInIntervall or modifiedByUser.
 */
open class UIFilterObjectElement(
        /**
         *  The id (property) of the filter to be defined.
         */
        id: String,
        /**
         * This filter option is an autocompletion field.
         */
        var autoCompletion: AutoCompletion? = null
) :UIFilterElement(id, FilterType.OBJECT) {
    enum class Type { STRING, DATE, CHOICE }
}
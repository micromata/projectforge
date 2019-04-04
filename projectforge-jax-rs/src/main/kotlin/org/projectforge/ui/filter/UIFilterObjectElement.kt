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
        var id: String,
        /**
         * Dependent on this type the ui offers different options. For strings (default) a simple input
         * text field is used, for date ranges date-picker etc.
         */
        var type: Type? = Type.STRING,
        /**
         * This filter option is an autocompletion field.
         */
        var autoCompletion: AutoCompletion? = null
) {
    enum class Type { STRING, DATE, CHOICE }
}
package org.projectforge.ui.filter

import org.projectforge.ui.UISelect

/**
 * An element for the UI specifying a filter attribute which may be added by the user to the search string.
 * Filter attributes are e. g. title or authors for books as well as modifiedInIntervall or modifiedByUser.
 */
open class UIFilterChoicesElement<T>(
        /**
         *  The id (property) of the filter to be defined.
         */
        id: String,
        /**
         * The selection values.
         */
        var select: UISelect<T>? = null
) :UIFilterElement(id, FilterType.CHOICE)

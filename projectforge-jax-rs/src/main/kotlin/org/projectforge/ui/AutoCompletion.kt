package org.projectforge.ui

/**
 * An element for the UI specifying the methods of autocompletion.
 */
class AutoCompletion(
        /**
         * The number of minimum characters before the auto-completion call will be executed.
         * Default is 2. Has no effect, if values are given.
         */
        var minChars: Int? = null,
        /**
         * If given, the frontend gets all values for auto-completion, no server call needed.
         */
        var values: List<Entry>? = null,
        /**
         * The recent entries, if given, will be shown at the top of the drop down for quick select.
         */
        var recent: List<Entry>? = null,
        /**
         * If given, the url will be called for getting the auto-completion values.
         */
        var url: String? = null) {
    class Entry(val id: Int,
                /**
                 * The title to display.
                 */
                val title: String,
                /**
                 * Optional if more fields will be used for the search. If not given, the
                 * frontend should use the title to search.
                 */
                var allSearchableFields: String? = null)

    private val log = org.slf4j.LoggerFactory.getLogger(AutoCompletion::class.java!!)


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
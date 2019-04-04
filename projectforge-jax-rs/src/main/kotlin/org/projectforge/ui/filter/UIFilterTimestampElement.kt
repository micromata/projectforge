package org.projectforge.ui.filter

/**
 * An element for the UI specifying a filter attribute which may be added by the user to the search string.
 * Filter attributes are e. g. title or authors for books as well as modifiedInIntervall or modifiedByUser.
 */
open class UIFilterTimestampElement(id: String,
                                    /**
                                     * openInterval means, that begin or end of interval is nullable.
                                     */
                                    var openInterval: Boolean = true,
                                    /**
                                     * The provided quickselectors for time intervals.
                                     */
                                    vararg selectors: QuickSelector) : UIFilterElement(id, filterType = FilterType.TIME_STAMP) {
    enum class QuickSelector {
        /**
         * Quick select of year (01/01/2019 0:00 until 31/12/2019 24:00) with scrolling buttons.
         */
        YEAR,
        /**
         * Quick select of month (01/03/2019 0:00 until 31/03/2019 24:00) with scrolling buttons.
         */
        MONTH,
        /**
         * Quick select of whole week (sunday until saturday or monday until sunday) with scrolling buttons.
         */
        WEEK,
        /**
         * Quick select of whole day with scrolling buttons.
         */
        DAY,
        /**
         * Quick select of last x minutes, hours, days, weeks, months, ...
         */
        UNTIL_NOW
    }
}
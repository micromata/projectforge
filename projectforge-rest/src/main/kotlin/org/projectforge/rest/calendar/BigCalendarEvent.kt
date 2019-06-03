package org.projectforge.rest.calendar

import java.util.*

class BigCalendarEvent(val title: String?,
                       val start: Date,
                       val end: Date,
                       val allDay: Boolean? = null,
                       val desc: String? = null,
                       val location: String? = null,
                       val tooltip: String? = null,
                       val formattedDuration: String? = null,
                       val outOfRange: Boolean? = null,
                       val fgColor: String? = null,
                       val bgColor: String? = null,
                       val cssClass: String? = null,
                       val category: String,
                       val dbId: Int? = null) {
    /**
     * Must be unique in the list of events. The index of the list will be used: 'e-1', 'e-2', ...
     * Will be set by [CalendarServicesRest].
     */
   internal var key : String? = null
}

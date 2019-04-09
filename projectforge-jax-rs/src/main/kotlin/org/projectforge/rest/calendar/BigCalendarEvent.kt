package org.projectforge.rest.calendar

import java.util.*

class BigCalendarEvent(val id: Int,
                       val title: String,
                       val start: Date,
                       val end: Date,
                       val allDay: Boolean? = null,
                       val desc: String? = null,
                       var location: String? = null,
                       var tooltip: String? = null,
                       var formattedDuration: String? = null,
                       var outOfRange: Boolean? = null,
                       var fgColor: String? = null,
                       var bgColor: String? = null
)

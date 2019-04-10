package org.projectforge.rest.calendar

import java.util.*

class BigCalendarEvent(val id: Int,
                       val title: String,
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
                       val link: String? = null)

package org.projectforge.business.calendar

import com.fasterxml.jackson.annotation.JsonProperty

enum class CalendarView {
    @JsonProperty("month")
    MONTH,
    @JsonProperty("week")
    WEEK,
    @JsonProperty("work_week")
    WORK_WEEK,
    @JsonProperty("day")
    DAY,
    @JsonProperty("agenda")
    AGENDA;

    companion object {
        fun from(name: String?) = if (name == null) null
        else valueOf(name.toUpperCase())
    }
}

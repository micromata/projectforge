package org.projectforge.rest.calendar

import com.fasterxml.jackson.annotation.JsonProperty

enum class CalendarView {
    @JsonProperty("month")
    MONTH,
    @JsonProperty("week")
    WEEK,
    @JsonProperty("workWeek")
    WORK_WEEK,
    @JsonProperty("day")
    DAY,
    @JsonProperty("agenda")
    AGENDA;

    companion object {
        fun from(name: String?) = if (name == null) null else valueOf(name.toUpperCase())
    }
}

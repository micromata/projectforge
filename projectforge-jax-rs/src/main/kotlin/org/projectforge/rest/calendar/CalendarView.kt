package org.projectforge.rest.calendar

import com.google.gson.annotations.SerializedName

enum class CalendarView {
    @SerializedName("month")
    MONTH,
    @SerializedName("week")
    WEEK,
    @SerializedName("workWeek")
    WORK_WEEK,
    @SerializedName("day")
    DAY,
    @SerializedName("agenda")
    AGENDA;

    companion object {
        fun from(name: String?) = if (name == null) null else valueOf(name.toUpperCase())
    }
}

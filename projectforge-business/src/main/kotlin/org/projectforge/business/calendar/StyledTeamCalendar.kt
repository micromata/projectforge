package org.projectforge.business.calendar

/**
 * Team calendar object extended by CalendarStyle and visibility.
 */
class StyledTeamCalendar(teamCalendar: TeamCalendar?,
                         var style: CalendarStyle? = null,
                         val visible: Boolean = true)
    : TeamCalendar(teamCalendar?.id, teamCalendar?.title) {

    companion object {
        /**
         * Add the styles of the styleMap to the returned calendars.
         */
        fun map(calendars : List<TeamCalendar>, styleMap : CalendarStyleMap) : List<StyledTeamCalendar> {
            return calendars.map { cal ->
                StyledTeamCalendar(calendars.find { it.id == cal.id },
                        style = styleMap.get(cal.id)) // Add the styles of the styleMap to the exported calendar.
            }
        }
    }
}

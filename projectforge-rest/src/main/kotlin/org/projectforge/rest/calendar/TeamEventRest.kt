/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

@file:Suppress("DEPRECATION")

package org.projectforge.rest.calendar

import org.projectforge.Const
import org.projectforge.business.calendar.event.model.SeriesModificationMode
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.business.teamcal.externalsubscription.TeamEventExternalSubscriptionCache
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.TimesheetRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.TeamEvent
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@Deprecated("Will be replaced by CalendarEventsRest.")
@RestController
@RequestMapping("${Rest.URL}/teamEvent")
class TeamEventRest() : AbstractDTORest<TeamEventDO, TeamEvent, TeamEventDao>(
        TeamEventDao::class.java,
        "plugins.teamcal.event.title",
        cloneSupported = true) {

    private val log = org.slf4j.LoggerFactory.getLogger(TeamEventRest::class.java)

    @Autowired
    private lateinit var calendarFilterServicesRest: CalendarFilterServicesRest

    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    @Autowired
    private lateinit var teamEventExternalSubscriptionCache: TeamEventExternalSubscriptionCache

    @Autowired
    private lateinit var timesheetRest: TimesheetRest

    override fun transformForDB(dto: TeamEvent): TeamEventDO {
        val teamEventDO = TeamEventDO()
        dto.copyTo(teamEventDO)
        if (dto.selectedSeriesEvent != null) {
            teamEventDO.setTransientAttribute(TeamEventDao.ATTR_SELECTED_ELEMENT, dto.selectedSeriesEvent)
            teamEventDO.setTransientAttribute(TeamEventDao.ATTR_SERIES_MODIFICATION_MODE, dto.seriesModificationMode)
        }
        return teamEventDO
    }

    override fun transformFromDB(obj: TeamEventDO, editMode: Boolean): TeamEvent {
        val teamEvent = TeamEvent()
        teamEvent.copyFrom(obj)
        return teamEvent
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: TeamEvent) {
        if (dto.calendar == null)
            validationErrors.add(ValidationError.createFieldRequired(baseDao.doClass, fieldId = "calendar"))
        if (dto.subject.isNullOrBlank())
            validationErrors.add(ValidationError.createFieldRequired(baseDao.doClass, fieldId = "subject"))
        if (dto.id != null && dto.hasRecurrence && dto.seriesModificationMode == null) {
            validationErrors.add(ValidationError.create("plugins.teamcal.event.recurrence.change.content"))
            validationErrors.add(ValidationError(fieldId = "seriesModificationMode"))
        }
    }

    /**
     * Params startDate and endDate for creating new events with preset dates.
     * For events of a series, startDate as param selects the event of the series.
     */
    override fun onBeforeGetItemAndLayout(request: HttpServletRequest, dto: TeamEvent, userAccess: UILayout.UserAccess) {
        val startDateSeconds = NumberHelper.parseLong(request.getParameter("startDate"))
        val endDateSeconds = NumberHelper.parseLong(request.getParameter("endDate"))
        var origStartDateSeconds = NumberHelper.parseLong(request.getParameter("origStartDate"))
        var origEndDateSeconds = NumberHelper.parseLong(request.getParameter("origEndDate"))
        if (origStartDateSeconds == null)
            origStartDateSeconds = startDateSeconds
        if (origEndDateSeconds == null)
            origEndDateSeconds = endDateSeconds
        if (dto.id != null) {
            if (origStartDateSeconds != null && origEndDateSeconds != null && dto.hasRecurrence) {
                // Seems to be a event of a series:
                dto.selectedSeriesEvent = TeamEvent(startDate = PFDateTime.from(origStartDateSeconds)!!.sqlTimestamp,
                        endDate = PFDateTime.from(origEndDateSeconds)!!.sqlTimestamp,
                        allDay = dto.allDay,
                        sequence = dto.sequence)
            }
        } else {
            val calendarId = NumberHelper.parseInteger(request.getParameter("calendar"))
            if (calendarId != null && calendarId > 0) {
                dto.calendar = teamCalDao.getById(calendarId)
            }
        }
        if (startDateSeconds != null) dto.startDate = PFDateTime.from(startDateSeconds)!!.sqlTimestamp
        if (endDateSeconds != null) dto.endDate = PFDateTime.from(endDateSeconds)!!.sqlTimestamp
    }

    override fun beforeDatabaseAction(request: HttpServletRequest, obj: TeamEventDO, dto: TeamEvent, operation: OperationType) {
        if (obj.calendarId != null) {
            // Calendar from client has only id and title. Get the calendar object from the data base (e. g. owner
            // is needed by the access checker.
            obj.calendar = teamCalDao.getById(obj.calendarId)
        }
    }

    override fun afterEdit(obj: TeamEventDO, dto: TeamEvent): ResponseAction {
        return ResponseAction("/${Const.REACT_APP_PATH}calendar")
                .addVariable("date", dto.startDate)
                .addVariable("id", obj.id ?: -1)
    }

    override fun getById(idString: String?, editMode: Boolean, userAccess: UILayout.UserAccess?): TeamEvent? {
        if (idString.isNullOrBlank())
            return TeamEvent()
        if (idString.contains('-')) { // {calendarId}-{uid}
            val vals = idString.split('-', limit = 2)
            if (vals.size != 2) {
                log.error("Can't get event of subscribed calendar. id must be of form {calId}-{uid} but is '$idString'.")

                return TeamEvent()
            }
            try {
                val calId = vals[0].toInt()
                val uid = vals[1]
                val eventDO = teamEventExternalSubscriptionCache.getEvent(calId, uid)
                if (eventDO == null) {
                    val cal = teamCalDao.getById(calId)
                    if (cal == null) {
                        log.error("Can't get calendar with id #$calId.")
                    } else if (!cal.externalSubscription) {
                        log.error("Calendar with id #$calId is not an external subscription, can't get event by uid.")
                    } else {
                        log.error("Can't find event with uid '$uid' in subscribed calendar with id #$calId.")
                    }
                    return TeamEvent()
                }
                return transformFromDB(eventDO, editMode)
            } catch (ex: NumberFormatException) {
                log.error("Can't get event of subscribed calendar. id must be of form {calId}-{uid} but is '$idString', a NumberFormatException occured.")
                return TeamEvent()
            }
        }
        return super.getById(idString, editMode, userAccess)
    }

    /**
     * Sets uid to null to Force a new creation of an uid.
     */
    override fun prepareClone(dto: TeamEvent): TeamEvent {
        val event = super.prepareClone(dto)
        event.uid = null // Force newly created uid
        return event
    }

    override fun getRestEditPath(): String {
        return "calendar/${super.getRestEditPath()}"
    }

    /**
     * Will be called by clone button. Sets the id of the form data object to null and deleted to false.
     * @return ResponseAction with [TargetType.UPDATE] and variable "initial" with all the initial data of [getItemAndLayout] as given for new objects.
     */
    @RequestMapping("switch2Timesheet")
    fun switch2Timesheet(request: HttpServletRequest, @RequestBody teamEvent: TeamEvent)
            : ResponseAction {
        return timesheetRest.cloneFromTeamEvent(request, teamEvent)
    }

    fun cloneFromTimesheet(request: HttpServletRequest, timesheet: TimesheetDO): ResponseAction {
        val teamEvent = TeamEvent()
        teamEvent.startDate = timesheet.startTime
        teamEvent.endDate = timesheet.stopTime
        teamEvent.location = timesheet.location
        teamEvent.note = timesheet.description
        val calendarId = calendarFilterServicesRest.getCurrentFilter().defaultCalendarId
        if (calendarId != null && calendarId > 0) {
            teamEvent.calendar = TeamCalDO()
            teamEvent.calendar?.id = calendarId
        }
        val editLayoutData = getItemAndLayout(request, teamEvent, UILayout.UserAccess(false, true))
        return ResponseAction(url = "/${Const.REACT_APP_PATH}calendar/${getRestPath(RestPaths.EDIT)}", targetType = TargetType.UPDATE)
                .addVariable("data", editLayoutData.data)
                .addVariable("ui", editLayoutData.ui)
                .addVariable("variables", editLayoutData.variables)
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "subject"))
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: TeamEvent, userAccess: UILayout.UserAccess): UILayout {
        val calendars = teamCalDao.getAllCalendarsWithFullAccess()
        calendars.removeIf { it.externalSubscription } // Remove full access calendars, but subscribed.
        if (dto.calendar != null && calendars.find { it.id == dto.calendar?.id } == null) {
            // Calendar of event is not in the list of editable calendars. Add this non-editable calendar to show
            // the calendar of the event.
            calendars.add(0, dto.calendar)
        }
        val calendarSelectValues = calendars.map {
            UISelectValue<Int>(it.id, it.title!!)
        }
        val subject = UIInput("subject", lc)
        subject.focus = true
        val layout = super.createEditLayout(dto, userAccess)
        if (dto.hasRecurrence && !userAccess.onlySelectAccess()) {
            val masterEvent = baseDao.getById(dto.id)
            val radioButtonGroup = UIGroup()
            if (masterEvent?.startDate?.before(dto.selectedSeriesEvent?.startDate) ?: true) {
                radioButtonGroup.add(UIRadioButton("seriesModificationMode", SeriesModificationMode.FUTURE, label = "plugins.teamcal.event.recurrence.change.future"))
            } else {
                radioButtonGroup.add(UIRadioButton("seriesModificationMode", SeriesModificationMode.ALL, label = "plugins.teamcal.event.recurrence.change.all"))
            }
            radioButtonGroup.add(UIRadioButton("seriesModificationMode", SeriesModificationMode.SINGLE, label = "plugins.teamcal.event.recurrence.change.single"))
            layout.add(UIFieldset(12, title = "plugins.teamcal.event.recurrence.change.text")
                    .add(radioButtonGroup))
        }
        layout.add(UIFieldset(12)
                .add(UIRow()
                        .add(UICol(6)
                                .add(UISelect<Int>("calendar",
                                        values = calendarSelectValues.toMutableList(),
                                        label = "plugins.teamcal.event.teamCal",
                                        labelProperty = "title",
                                        valueProperty = "id"))
                                .add(subject)
                                .add(lc, "location"))
                        .add(UICol(6)
                                .add(lc, "startDate", "endDate", "allDay")
                                .add(lc, "note"))))
                .add(UICustomized("calendar.reminder"))
                .add(UIRow().add(UICol(12).add(UICustomized("calendar.recurrency"))))
        layout.addAction(UIButton("switch",
                title = translate("plugins.teamcal.switchToTimesheetButton"),
                color = UIColor.DARK,
                responseAction = ResponseAction(getRestRootPath("switch2Timesheet"), targetType = TargetType.POST)))
        layout.addTranslations("plugins.teamcal.event.recurrence",
                "plugins.teamcal.event.recurrence.customized",
                "common.recurrence.frequency.yearly",
                "common.recurrence.frequency.monthly",
                "common.recurrence.frequency.weekly",
                "common.recurrence.frequency.daily",
                "common.recurrence.frequency.none",
                "plugins.teamcal.event.reminder",
                "plugins.teamcal.event.reminder.NONE",
                "plugins.teamcal.event.reminder.MESSAGE",
                "plugins.teamcal.event.reminder.MESSAGE_SOUND",
                "plugins.teamcal.event.reminder.MINUTES_BEFORE",
                "plugins.teamcal.event.reminder.HOURS_BEFORE",
                "plugins.teamcal.event.reminder.DAYS_BEFORE")
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}

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

import org.apache.commons.lang3.Validate
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.teamcal.event.TeamEventService
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.business.teamcal.externalsubscription.TeamEventExternalSubscriptionCache
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.TimesheetRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractBaseRest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.TeamEvent
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.sql.Timestamp
import java.util.*
import javax.servlet.http.HttpServletRequest

@Deprecated("Will be replaced by CalendarEventsRest.")
@RestController
@RequestMapping("${Rest.URL}/teamEvent")
class TeamEventRest() : AbstractDTORest<TeamEventDO, TeamEvent, TeamEventDao>(
        TeamEventDao::class.java,
        "plugins.teamcal.event.title") {

    private val log = org.slf4j.LoggerFactory.getLogger(TeamEventRest::class.java)

    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    @Autowired
    private lateinit var timesheetRest: TimesheetRest

    @Autowired
    private lateinit var teamEventService: TeamEventService

    @Autowired
    private lateinit var teamEventExternalSubscriptionCache: TeamEventExternalSubscriptionCache

    override fun transformForDB(dto: TeamEvent): TeamEventDO {
        val teamEventDO = TeamEventDO()
        dto.copyTo(teamEventDO)
        return teamEventDO
    }

    override fun transformFromDB(obj: TeamEventDO, editMode: Boolean): TeamEvent {
        val teamEvent = TeamEvent()
        teamEvent.copyFrom(obj)
        return teamEvent
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: TeamEvent) {
        if (dto.hasRecurrence && dto.modifySerie == null) {
            validationErrors.add(ValidationError.create("plugins.teamcal.event.recurrence.change.content"))
            validationErrors.add(ValidationError(fieldId = "modifySerie"))
        }
    }

    private fun getUntilDate(untilUTC: Timestamp): Date {
        // move one day to past, the TeamEventDO will post process this value while setting
        return Date(untilUTC.time - 24 * 60 * 60 * 1000)
    }


    override fun afterDelete(obj: TeamEventDO, dto: TeamEvent): ResponseAction {
        if (!dto.hasRecurrence || dto.modifySerie == null || dto.modifySerie == TeamEvent.ModifySerie.ALL) {
            return super.afterDelete(obj, dto)
        }
        val masterId = obj.getId() // Store the id of the master entry.
        val masterEvent = teamEventService.getById(masterId)
        if (dto.modifySerie == TeamEvent.ModifySerie.FUTURE) {
            val recurrenceData = obj.getRecurrenceData(ThreadLocalUserContext.getTimeZone())
            val recurrenceUntil = getUntilDate(dto.selectedSeriesElement!!.startDate!!)
            recurrenceData.setUntil(recurrenceUntil)
            masterEvent.setRecurrence(recurrenceData)
            baseDao.update(masterEvent)
        } else if (dto.modifySerie == TeamEvent.ModifySerie.SINGLE) { // only current date
            Validate.notNull(dto.selectedSeriesElement)
            masterEvent.addRecurrenceExDate(dto.selectedSeriesElement!!.startDate!!)
            baseDao.update(masterEvent)
        }
        return super.afterDelete(obj, dto)
    }

    /*
    override fun afterSaveOrUpdate(obj: TeamEventDO, dto: TeamEvent) {
        if (!dto.hasRecurrence || dto.modifySerie == null || dto.modifySerie == TeamEvent.ModifySerie.ALL) {
            return
        }
        val masterId = obj.id // Store the id of the master entry.
        obj.id = null // Clone object.
        val masterEvent = teamEventService.getById(masterId)
        if (masterEvent == null) {
            log.error("masterEvent is null?! Do nothing more after saving team event.")
            return
        }
        if (dto.selectedSeriesElement == null) {
            log.error("Selected series element is null?! Do nothing more after saving team event.")
            return
        }
        if (dto.modifySerie == TeamEvent.ModifySerie.FUTURE) {
            val newEvent = obj.clone()
            val recurrenceData = obj.getRecurrenceData(ThreadLocalUserContext.getTimeZone())
            // Set the end date of the master date one day before current date and save this event.
            val recurrenceUntil = this.getUntilDate(dto.selectedSeriesElement!!.startDate!!)
            recurrenceData.setUntil(recurrenceUntil)
            newEvent.setRecurrence(recurrenceData)
            if (log.isDebugEnabled == true) {
                log.debug("Recurrency until date of master entry will be set to: " + DateHelper.formatAsUTC(recurrenceUntil))
                log.debug("The new event is: $newEvent")
            }
            return
        } else if (recurrencyChangeType == RecurrencyChangeType.ONLY_CURRENT) { // only current date
            // Add current date to the master date as exclusion date and save this event (without recurrence settings).
            masterEvent.addRecurrenceExDate(eventOfCaller.getStartDate())
            newEvent = oldDataObject.clone()
            newEvent.setRecurrenceDate(eventOfCaller.getStartDate(), ThreadLocalUserContext.getTimeZone())
            newEvent.setRecurrenceReferenceId(masterEvent.id)
            if (log.isDebugEnabled == true) {
                log.debug("Recurrency ex date of master entry is now added: "
                        + DateHelper.formatAsUTC(eventOfCaller.getStartDate())
                        + ". The new string is: "
                        + masterEvent.recurrenceExDate)
                log.debug("The new event is: $newEvent")
            }
            return
        }
    }*/

    override fun onGetItemAndLayout(request: HttpServletRequest, dto: TeamEvent, editLayoutData: AbstractBaseRest.EditLayoutData) {
        val startDateAsSeconds = NumberHelper.parseLong(request.getParameter("startDate"))
        if (startDateAsSeconds != null) dto.startDate = PFDateTime.from(startDateAsSeconds)!!.sqlTimestamp
        val endDateSeconds = NumberHelper.parseLong(request.getParameter("endDate"))
        if (endDateSeconds != null) dto.endDate = PFDateTime.from(endDateSeconds)!!.sqlTimestamp
        super.onGetItemAndLayout(request, dto, editLayoutData)
    }

    override fun beforeSaveOrUpdate(request: HttpServletRequest, obj: TeamEventDO, dto: TeamEvent) {
        if (obj.calendarId != null) {
            // Calendar from client has only id and title. Get the calendar object from the data base (e. g. owner
            // is needed by the access checker.
            obj.calendar = teamCalDao.getById(obj.calendarId)
        }
    }

    override fun afterEdit(obj: TeamEventDO, dto: TeamEvent): ResponseAction {
        return ResponseAction("/calendar")
                .addVariable("date", obj.startDate)
                .addVariable("id", obj.id ?: -1)
    }

    override fun getById(idString: String?, editMode: Boolean, userAccess: UILayout.UserAccess?): TeamEvent? {
        if (idString.isNullOrBlank())
            return TeamEvent();
        if (idString.contains('-')) { // {calendarId}-{uid}
            val vals = idString.split('-', limit = 2)
            if (vals.size != 2) {
                log.error("Can't get event of subscribed calendar. id must be of form {calId}-{uid} but is '$idString'.")

                return TeamEvent()
            }
            try {
                val calId = vals[0].toInt()
                val uid = vals[1]
                val cal = teamCalDao.getById(calId)
                if (cal == null) {
                    log.error("Can't get calendar with id #$calId.")

                    return TeamEvent()
                }
                if (!cal.externalSubscription) {
                    log.error("Calendar with id #$calId is not an external subscription, can't get event by uid.")
                    return TeamEvent()
                }
                return TeamEvent()//return teamEventExternalSubscriptionCache.getEvent(calId, uid)
            } catch (ex: NumberFormatException) {
                log.error("Can't get event of subscribed calendar. id must be of form {calId}-{uid} but is '$idString', a NumberFormatException occured.")
                return TeamEvent()
            }
        }
        return super.getById(idString, editMode, userAccess)
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
        val editLayoutData = getItemAndLayout(request, teamEvent, UILayout.UserAccess(false, true))
        return ResponseAction(url = "/calendar/${getRestPath(RestPaths.EDIT)}", targetType = TargetType.UPDATE)
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
        val calendarSelectValues = calendars.map { it ->
            UISelectValue<Int>(it.id, it.title!!)
        }
        val subject = UIInput("subject", lc)
        subject.focus = true
        val layout = super.createEditLayout(dto, userAccess)
        if (dto.hasRecurrence) {
            layout.add(UIFieldset(12, title = "plugins.teamcal.event.recurrence.change.text")
                    .add(UIGroup()
                            .add(UIRadioButton("modifySerie", TeamEvent.ModifySerie.ALL, label = "plugins.teamcal.event.recurrence.change.text.all"))
                            .add(UIRadioButton("modifySerie", TeamEvent.ModifySerie.FUTURE, label = "plugins.teamcal.event.recurrence.change.future"))
                            .add(UIRadioButton("modifySerie", TeamEvent.ModifySerie.SINGLE, label = "plugins.teamcal.event.recurrence.change.single"))
                    ))
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
                                .add(lc, "attendees")
                                .add(lc, "location"))
                        .add(UICol(6)
                                .add(lc, "startDate", "endDate", "allDay")
                                .add(lc, "note"))))
                .add(UICustomized("calendar.reminder"))
                .add(UIRow().add(UICol(12).add(UICustomized("calendar.recurrency"))))
        layout.addAction(UIButton("switch",
                title = translate("plugins.teamcal.switchToTimesheetButton"),
                color = UIColor.SECONDARY,
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

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

package org.projectforge.rest.calendar

import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.business.teamcal.externalsubscription.TeamEventExternalSubscriptionCache
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.TimesheetRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractBaseRest
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/teamEvent")
class TeamEventRest() : AbstractDORest<TeamEventDO, TeamEventDao>(
        TeamEventDao::class.java,
        "plugins.teamcal.event.title") {

    private val log = org.slf4j.LoggerFactory.getLogger(TeamEventRest::class.java)

    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    @Autowired
    private lateinit var timesheetRest: TimesheetRest

    @Autowired
    private lateinit var teamEventExternalSubscriptionCache: TeamEventExternalSubscriptionCache

    override fun onGetItemAndLayout(request: HttpServletRequest, dto: TeamEventDO, editLayoutData: AbstractBaseRest.EditLayoutData) {
        val recurrentDateString = request.getParameter("recurrentDate")
        println("TeamEventRest: recurrentDate=$recurrentDateString")
        val startDateAsSeconds = NumberHelper.parseLong(request.getParameter("startDate"))
        if (startDateAsSeconds != null) dto.startDate = PFDateTime.from(startDateAsSeconds)!!.sqlTimestamp
        val endDateSeconds = NumberHelper.parseLong(request.getParameter("endDate"))
        if (endDateSeconds != null) dto.endDate = PFDateTime.from(endDateSeconds)!!.sqlTimestamp
        super.onGetItemAndLayout(request, dto, editLayoutData)
    }

    override fun afterEdit(obj: TeamEventDO, dto: TeamEventDO): ResponseAction {
        return ResponseAction("/calendar")
                .addVariable("date", obj.startDate)
                .addVariable("id", obj.id ?: -1)
    }

    override fun getById(idString: String?, editMode: Boolean): TeamEventDO? {
        if (idString.isNullOrBlank())
            return TeamEventDO()
        if (idString.contains('-')) { // {calendarId}-{uid}
            val vals = idString.split('-', limit = 2)
            if (vals.size != 2) {
                log.error("Can't get event of subscribed calendar. id must be of form {calId}-{uid} but is '$idString'.")
                return TeamEventDO()
            }
            try {
                val calId = vals[0].toInt()
                val uid = vals[1]
                val cal = teamCalDao.getById(calId)
                if (cal == null) {
                    log.error("Can't get calendar with id #$calId.")
                    return TeamEventDO()
                }
                if (!cal.externalSubscription) {
                    log.error("Calendar with id #$calId is not an external subscription, can't get event by uid.")
                    return TeamEventDO()
                }
                return teamEventExternalSubscriptionCache.getEvent(calId, uid)
            } catch (ex: NumberFormatException) {
                log.error("Can't get event of subscribed calendar. id must be of form {calId}-{uid} but is '$idString', a NumberFormatException occured.")
                return TeamEventDO()
            }
        }
        return super.getById(idString, editMode)
    }

    /**
     * Will be called by clone button. Sets the id of the form data object to null and deleted to false.
     * @return ResponseAction with [TargetType.UPDATE] and variable "initial" with all the initial data of [getItemAndLayout] as given for new objects.
     */
    @RequestMapping("switch2Timesheet")
    fun switch2Timesheet(request: HttpServletRequest, @RequestBody teamEvent: TeamEventDO)
            : ResponseAction {
        return timesheetRest.cloneFromTimesheet(request, teamEvent)
    }

    fun cloneFromTimesheet(request: HttpServletRequest, timesheet: TimesheetDO): ResponseAction {
        val teamEvent = TeamEventDO()
        teamEvent.startDate = timesheet.startTime
        teamEvent.endDate = timesheet.stopTime
        teamEvent.location = timesheet.location
        teamEvent.note = timesheet.description
        val editLayoutData = getItemAndLayout(request, teamEvent)
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
    override fun createEditLayout(dto: TeamEventDO): UILayout {
        val calendars = teamCalDao.getAllCalendarsWithFullAccess()
        val calendarSelectValues = calendars.map { it ->
            UISelectValue<Int>(it.id, it.title!!)
        }
        val subject = UIInput("subject", lc)
        subject.focus = true
        val layout = super.createEditLayout(dto)
        //layout.addAction(UIButton("switchToTimesheet", style = UIStyle.PRIMARY, default = true))
        if (dto.hasRecurrence()) {
            layout.add(UIFieldset(12, title = "plugins.teamcal.event.recurrence.change.text")
                    .add(UIGroup()
                            .add(UIRadioButton("all", "selection", label = "plugins.teamcal.event.recurrence.change.text.all"))
                            .add(UIRadioButton("future", "selection", label = "plugins.teamcal.event.recurrence.change.future"))
                            .add(UIRadioButton("single", "selection", label = "plugins.teamcal.event.recurrence.change.single"))
                    ))
        }
        layout.add(UIFieldset(12)
                .add(UIRow()
                        .add(UICol(6)
                                .add(UISelect<Int>("calendar", values = calendarSelectValues.toMutableList(), label = "plugins.teamcal.event.teamCal"))
                                .add(subject)
                                .add(lc, "attendees")
                                .add(lc, "location", "note"))
                        .add(UICol(6)
                                .add(lc, "startDate", "endDate", "allDay")
                                .add(UIFieldset(12)
                                        .add(UICustomized("reminder")))
                                .add(UIFieldset(12)
                                        .add(UICustomized("recurrence"))))))
        layout.addAction(UIButton("switch",
                title = translate("plugins.teamcal.switchToTimesheetButton"),
                color = UIColor.SECONDARY,
                responseAction = ResponseAction(getRestRootPath("switch2Timesheet"), targetType = TargetType.POST)))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}

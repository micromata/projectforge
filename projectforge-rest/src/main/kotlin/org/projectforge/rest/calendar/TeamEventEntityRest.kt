/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.merlin.utils.ReplaceUtils
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.business.teamcal.ical.ICalGenerator
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDateTimeUtils
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.TimesheetEntityRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.RestButtonEvent
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.TeamEvent
import org.projectforge.rest.dto.Timesheet
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The calendar events, served to the hand built edit page of projectforge-next and its calendar (layout
 * free, see [AbstractDTOEntityRest]). A clone is saved right away ([CloneSupport.AUTOSAVE]).
 */
@RestController
@RequestMapping("${Rest.URL}/teamEvent")
class TeamEventEntityRest() : AbstractDTOEntityRest<TeamEventDO, TeamEvent, TeamEventDao>(
  TeamEventDao::class.java,
  "plugins.teamcal.event.title",
  cloneSupport = CloneSupport.AUTOSAVE
) {

  @Autowired
  private lateinit var calendarFilterServicesRest: CalendarFilterServicesRest

  @Autowired
  private lateinit var teamCalDao: TeamCalDao

  @Autowired
  private lateinit var timesheetRest: TimesheetEntityRest

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
    if (dto.subject.isNullOrBlank())
      validationErrors.add(ValidationError.createFieldRequired(baseDao.doClass, fieldId = "subject"))
    if (dto.id != null && dto.hasRecurrence && dto.seriesModificationMode == null) {
      validationErrors.add(ValidationError.create("plugins.teamcal.event.recurrence.change.content"))
      validationErrors.add(ValidationError(fieldId = "seriesModificationMode"))
    }
  }

  /**
   * The preset of a new event, as the calendar asks for it: the request parameters `startDate`/`endDate`
   * (epoch seconds or an ISO date-time including any zone offset, see [PFDateTimeUtils.parse]) and the
   * `calendar` id.
   *
   * The selected event of a series (`selectedSeriesEvent`) is not preset here: the next calendar builds it
   * itself from the clicked occurrence (`calendar-edit-target.ts`).
   */
  override fun newBaseDTO(request: HttpServletRequest?): TeamEvent {
    val event = TeamEvent()
    request ?: return event
    val calendarId = NumberHelper.parseLong(request.getParameter("calendar"))
    if (calendarId != null && calendarId > 0) {
      event.calendar = teamCalDao.find(calendarId)
    }
    PFDateTimeUtils.parseAndCreateDateTime(
      request.getParameter("startDate"),
      numberFormat = PFDateTime.NumberFormat.EPOCH_SECONDS
    )?.let { event.startDate = it.sqlTimestamp }
    PFDateTimeUtils.parseAndCreateDateTime(
      request.getParameter("endDate"),
      numberFormat = PFDateTime.NumberFormat.EPOCH_SECONDS
    )?.let { event.endDate = it.sqlTimestamp }
    return event
  }

  /**
   * The writable team calendars the calendar select of the edit page offers. External subscriptions are
   * read-only, so they are excluded.
   */
  @AccessChecked("DAO: TeamCalDao.select, calendars with full access only")
  @GetMapping("calendars")
  fun getCalendars(): List<CalendarSelectValue> {
    return teamCalDao.writableCalendars.mapNotNull { cal ->
      cal.id?.let { CalendarSelectValue(it, cal.title ?: "???") }
    }
  }

  /**
   * The stored event as an ics file, for importing it into another calendar app without subscribing the
   * whole calendar. [TeamEventDao.find] checks the select access; with minimal access to the calendar the
   * event's texts are cleared there, so only the busy time is exported.
   */
  @AccessChecked("DAO: TeamEventDao.find (select access)")
  @GetMapping("exportIcs/{id}")
  fun exportIcs(@PathVariable id: Long): ResponseEntity<*> {
    val event = baseDao.find(id) ?: return ResponseEntity.notFound().build<Any>()
    val generator = ICalGenerator()
    generator.add(event)
    val filename = "${ReplaceUtils.encodeFilename(event.subject.takeUnless { it.isNullOrBlank() } ?: "event", true)}.ics"
    return RestUtils.downloadFile(filename, generator.asByteArray ?: ByteArray(0))
  }

  override fun onBeforeDatabaseAction(
    request: HttpServletRequest,
    obj: TeamEventDO,
    postData: PostData<TeamEvent>,
    operation: OperationType
  ) {
    if (obj.calendarId != null) {
      // Calendar from client has only id and title. Get the calendar object from the data base (e. g. owner
      // is needed by the access checker.
      obj.calendar = teamCalDao.find(obj.calendarId)
    }
  }

  override fun onAfterEdit(request: HttpServletRequest, obj: TeamEventDO, postData: PostData<TeamEvent>, event: RestButtonEvent): ResponseAction {
    return CalendarServicesRest.redirectToCalendarWithDate(obj.startDate, event)
  }

  /**
   * Sets uid to null to Force a new creation of an uid.
   */
  override fun prepareClone(dto: TeamEvent): TeamEvent {
    val event = super.prepareClone(dto)
    event.uid = null // Force newly created uid
    return event
  }

  /**
   * Turns the posted (unsaved) event into a new time sheet, see [TimesheetEntityRest.cloneFromCalendarEvent].
   * Only its period, location and subject are carried over.
   */
  @RequestMapping("switch2Timesheet")
  fun switch2Timesheet(request: HttpServletRequest, @Valid @RequestBody postData: PostData<TeamEvent>)
      : ResponseAction {
    val teamEvent = postData.data
    val calendarEvent = TeamEvent(
      startDate = teamEvent.startDate,
      endDate = teamEvent.endDate,
      location = teamEvent.location,
      subject = teamEvent.subject
    )
    return timesheetRest.cloneFromCalendarEvent(request, calendarEvent)
  }

  /**
   * A new event prepared from a time sheet (the switch from the sheet's form, see
   * [TimesheetEntityRest.switch2CalendarEvent]), in the user's default calendar. Nothing is saved; the
   * prepared event travels under `variables.data`, where the client (`convertEntity` in projectforge-next)
   * reads it.
   */
  fun cloneFromTimesheet(request: HttpServletRequest, timesheet: Timesheet): ResponseAction {
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
    return ResponseAction(targetType = TargetType.UPDATE).addVariable("data", teamEvent)
  }

  /** A writable team calendar as the calendar select of the edit page needs it. */
  data class CalendarSelectValue(val id: Long, val title: String)
}

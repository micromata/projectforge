/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.teamcal.event

import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.calendar.event.model.ICalendarEvent
import org.projectforge.business.calendar.event.model.SeriesModificationMode
import org.projectforge.business.teamcal.TeamCalConfig
import org.projectforge.business.teamcal.event.model.CalEventDO
import org.projectforge.business.user.UserRightId
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.and
import org.projectforge.framework.persistence.api.QueryFilter.Companion.between
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.QueryFilter.Companion.ge
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isIn
import org.projectforge.framework.persistence.api.QueryFilter.Companion.le
import org.projectforge.framework.persistence.api.QueryFilter.Companion.or
import org.projectforge.framework.persistence.api.SortProperty.Companion.desc
import org.projectforge.framework.time.PFDateTimeUtils.Companion.getUTCBeginOfDayTimestamp
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class CalEventDao : BaseDao<CalEventDO>(CalEventDO::class.java) {
    init {
        userRightId = UserRightId.CALENDAR_EVENT
    }

    fun getByUid(calendarId: Int?, uid: String?): CalEventDO? {
        return this.getByUid(calendarId, uid, true)
    }

    fun getByUid(calendarId: Int?, uid: String?, excludeDeleted: Boolean): CalEventDO? {
        if (uid == null) {
            return null
        }

        val sqlQuery = StringBuilder()
        val params = mutableListOf<Pair<String, *>>()

        sqlQuery.append("select e from CalEventDO e where e.uid = :uid")

        params.add(Pair("uid", uid))

        if (excludeDeleted) {
            sqlQuery.append(" AND e.deleted = :deleted")
            params.add(Pair("deleted", false))
        }

        // workaround to still handle old requests
        if (calendarId != null) {
            sqlQuery.append(" AND e.calendar.id = :calendarId")
            params.add(Pair("calendarId", calendarId))
        }

        return persistenceService.selectSingleResult(
            sqlQuery.toString(),
            CalEventDO::class.java,
            *params.toTypedArray(),
            nullAllowed = false,
        )
    }

    override fun onChange(obj: CalEventDO, dbObj: CalEventDO) {
        handleSeriesUpdates(obj)
        // only increment sequence if PF has ownership!
        /*if (obj.getOwnership() != null && obj.getOwnership() == false) {
      return;
    }

    // compute diff
    if (obj.mustIncSequence(dbObj)) {
      if (obj.getSequence() == null) {
        obj.setSequence(0);
      } else {
        obj.setSequence(obj.getSequence() + 1);
      }

      if (obj.getDtStamp() == null || obj.getDtStamp().equals(dbObj.getDtStamp())) {
        obj.setDtStamp(new Timestamp(System.currentTimeMillis()));
      }
    }*/
    }

    private fun getUntilDate(untilUTC: Date): Date {
        // move one day to past, the TeamEventDO will post process this value while setting
        return Date(untilUTC.time - 24 * 60 * 60 * 1000)
    }

    /**
     * Handles updates of series element (if any) for future and single events of a series.
     *
     * @param event
     */
    private fun handleSeriesUpdates(event: CalEventDO) {
        val selectedEvent =
            event.removeTransientAttribute(ATTR_SELECTED_ELEMENT) as ICalendarEvent? // Must be removed, otherwise save below will handle this attrs again.
        val mode = event.removeTransientAttribute(ATTR_SERIES_MODIFICATION_MODE) as SeriesModificationMode?
        if (selectedEvent == null || mode == null || mode == SeriesModificationMode.ALL) {
            // Nothing to do.
            return
        }
        // TODO
        /*
    CalEventDO newEvent = event.clone();
    newEvent.setSequence(0);
    CalEventDO masterEvent = getById(event.getId());
    event.copyValuesFrom(masterEvent); // Restore db fields of master event. Do only modify single or future events.
    if (mode == SeriesModificationMode.FUTURE) {
      TeamEventRecurrenceData recurrenceData = masterEvent.getRecurrenceData(ThreadLocalUserContext.getTimeZone());
      // Set the end date of the master date one day before current date and save this event.
      recurrenceData.setUntil(getUntilDate(selectedEvent.getStartDate()));
      event.setRecurrence(recurrenceData);
      save(newEvent);
      if (log.isDebugEnabled() == true) {
        log.debug("Recurrence until date of master entry will be set to: " + DateHelper.formatAsUTC(recurrenceData.getUntil()));
        log.debug("The new event is: " + newEvent);
      }
      return;
    } else if (mode == SeriesModificationMode.SINGLE) { // only current date
      // Add current date to the master date as exclusion date and save this event (without recurrence settings).
      event.addRecurrenceExDate(selectedEvent.getStartDate());
      if (newEvent.hasRecurrence()) {
        log.warn("User tries to modifiy single event of a series, the given recurrence is ignored.");
      }
      newEvent.setRecurrence((RRule) null); // User only wants to modify single event, ignore recurrence.
      save(newEvent);
      if (log.isDebugEnabled() == true) {
        log.debug("Recurrency ex date of master entry is now added: "
                + DateHelper.formatAsUTC(selectedEvent.getStartDate())
                + ". The new string is: "
                + event.getRecurrenceExDate());
        log.debug("The new event is: " + newEvent);
      }
    }*/
    }

    /**
     * Handles deletion of series element (if any) for future and single events of a series.
     */
    override fun internalMarkAsDeleted(obj: CalEventDO) {
        val selectedEvent =
            obj.removeTransientAttribute(ATTR_SELECTED_ELEMENT) as ICalendarEvent? // Must be removed, otherwise update below will handle this attrs again.
        val mode = obj.removeTransientAttribute(ATTR_SERIES_MODIFICATION_MODE) as SeriesModificationMode?
        if (selectedEvent == null || mode == null || mode == SeriesModificationMode.ALL) {
            // Nothing to do special:
            super.internalMarkAsDeleted(obj)
            return
        }
        val masterEvent = getById(obj.id)
        obj.copyValuesFrom(masterEvent!!) // Restore db fields of master event. Do only modify single or future events.
        if (mode == SeriesModificationMode.FUTURE) {
            // TODO
            /*
      TeamEventRecurrenceData recurrenceData = obj.getRecurrenceData(ThreadLocalUserContext.getTimeZone());
      Date recurrenceUntil = getUntilDate(selectedEvent.getStartDate());
      recurrenceData.setUntil(recurrenceUntil);
      obj.setRecurrence(recurrenceData);
      update(obj);*/
        } else if (mode == SeriesModificationMode.SINGLE) { // only current date
            // TODO
            /*
      Validate.notNull(selectedEvent);
      obj.addRecurrenceExDate(selectedEvent.getStartDate());
      update(obj);*/
        }
    }

    override fun newInstance(): CalEventDO {
        return CalEventDO()
    }

    /**
     * This method also returns recurrence events outside the time period of the given filter but affecting the
     * time-period (e. g. older recurrence events without end date or end date inside or after the given time period). If
     * calculateRecurrenceEvents is true, only the recurrence events inside the given time-period are returned, if false
     * only the origin recurrence event (may-be outside the given time-period) is returned.
     *
     * @param filter
     * @param calculateRecurrenceEvents If true, recurrence events inside the given time-period are calculated.
     * @return list of team events (same as [.getList] but with all calculated and matching
     * recurrence events (if calculateRecurrenceEvents is true). Origin events are of type [TeamEventDO],
     * calculated events of type [ICalendarEvent].
     */
    fun getEventList(filter: TeamEventFilter, calculateRecurrenceEvents: Boolean): List<ICalendarEvent?> {
        val result: MutableList<ICalendarEvent?> = ArrayList()
        var list = getList(filter)
        if (CollectionUtils.isNotEmpty(list)) {
            for (eventDO in list!!) {
                result.add(eventDO)
            }
        }
        val teamEventFilter = filter.clone().setOnlyRecurrence(true)
        val qFilter = buildQueryFilter(teamEventFilter)
        list = getList(qFilter)
        list = selectUnique(list)
        for (eventDO in list) {
            if (!calculateRecurrenceEvents) {
                result.add(eventDO)
                continue
            }
        }
        return result
    }

    /**
     * Sets midnight (UTC) of all day events.
     */
    override fun onSaveOrModify(obj: CalEventDO) {
        super.onSaveOrModify(obj)
        requireNotNull(obj.calendar)

        if (obj.allDay) {
            if (obj.endDate!!.time < obj.startDate!!.time) {
                throw UserException("plugins.teamcal.event.duration.error") // "Duration of time sheet must be at minimum 60s!
            }
        } else if (obj.endDate!!.time - obj.startDate!!.time < 60000) {
            throw UserException("plugins.teamcal.event.duration.error") // "Duration of time sheet must be at minimum 60s!

            // Or, end date is before start date.
        }

        // If is all day event, set start and stop to midnight
        if (obj.allDay) {
            val startDate = obj.startDate
            if (startDate != null) {
                obj.setStartDate(getUTCBeginOfDayTimestamp(startDate))
            }
            val endDate = obj.endDate
            if (endDate != null) {
                obj.setEndDate(getUTCBeginOfDayTimestamp(endDate))
            }
        }
    }

    override fun onSave(obj: CalEventDO) {
        // create uid if empty
        if (StringUtils.isBlank(obj.uid)) {
            obj.setUid(TeamCalConfig.get().createEventUid())
        }
    }

    private fun buildQueryFilter(filter: TeamEventFilter): QueryFilter {
        val queryFilter = QueryFilter(filter)
        val cals = filter.teamCals
        if (CollectionUtils.isNotEmpty(cals)) {
            queryFilter.add(isIn<Any>("calendar.id", cals))
        } else if (filter.teamCalId != null) {
            queryFilter.add(eq("calendar.id", filter.teamCalId))
        }
        // Following period extension is needed due to all day events which are stored in UTC. The additional events in the result list not
        // matching the time period have to be removed by caller!
        var startDate = filter.startDate
        if (startDate != null) {
            startDate = Date(startDate.time - ONE_DAY)
        }
        var endDate = filter.endDate
        if (endDate != null) {
            endDate = Date(endDate.time + ONE_DAY)
        }
        // limit events to load to chosen date view.
        if (startDate != null && endDate != null) {
            queryFilter.add(
                or(
                    (or(
                        between("startDate", startDate, endDate),
                        between("endDate", startDate, endDate)
                    )),  // get events whose duration overlap with chosen duration.
                    (and(le("startDate", startDate), ge("endDate", endDate)))
                )
            )
        } else if (endDate != null) {
            queryFilter.add(le("startDate", endDate))
        }
        queryFilter.addOrder(desc("startDate"))
        return queryFilter
    }

    companion object {
        /**
         * For storing the selected element of the series in the transient attribute map for correct handling in [.onDelete]
         * and [.onSaveOrModify] of series (all, future, selected).
         */
        const val ATTR_SELECTED_ELEMENT: String = "selectedSeriesElement"

        /**
         * For series elements: what to modify in [.onDelete] and [.onSaveOrModify] of series (all, future, selected)?
         */
        const val ATTR_SERIES_MODIFICATION_MODE: String = "seriesModificationMode"

        private val log: Logger = LoggerFactory.getLogger(CalEventDao::class.java)

        private const val ONE_DAY = (1000 * 60 * 60 * 24).toLong()
    }
}

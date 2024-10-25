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

import jakarta.persistence.NoResultException
import jakarta.persistence.NonUniqueResultException
import mu.KotlinLogging
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.TimeZone
import net.fortuna.ical4j.model.parameter.Value
import net.fortuna.ical4j.model.property.RRule
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.ToStringBuilder
import org.projectforge.business.address.AddressDO
import org.projectforge.business.calendar.event.model.ICalendarEvent
import org.projectforge.business.calendar.event.model.SeriesModificationMode
import org.projectforge.business.teamcal.TeamCalConfig
import org.projectforge.business.teamcal.admin.TeamCalCache
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.business.teamcal.externalsubscription.TeamEventExternalSubscriptionCache
import org.projectforge.business.user.UserRightId
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.calendar.ICal4JUtils
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.and
import org.projectforge.framework.persistence.api.QueryFilter.Companion.between
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.QueryFilter.Companion.ge
import org.projectforge.framework.persistence.api.QueryFilter.Companion.gt
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isIn
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isNotNull
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isNull
import org.projectforge.framework.persistence.api.QueryFilter.Companion.le
import org.projectforge.framework.persistence.api.QueryFilter.Companion.lt
import org.projectforge.framework.persistence.api.QueryFilter.Companion.or
import org.projectforge.framework.persistence.api.SortProperty.Companion.desc
import org.projectforge.framework.persistence.history.DisplayHistoryConvertContext
import org.projectforge.framework.persistence.history.HistoryEntryDO
import org.projectforge.framework.persistence.history.HistoryFormatUtils
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.timeZone
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDateTime.Companion.from
import org.projectforge.framework.time.PFDateTime.Companion.fromOrNull
import org.projectforge.framework.time.PFDateTime.Companion.now
import org.projectforge.framework.time.PFDateTimeUtils
import org.projectforge.framework.time.PFDateTimeUtils.getUTCBeginOfDayTimestamp
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
@Service
open class TeamEventDao : BaseDao<TeamEventDO>(TeamEventDO::class.java) {
    @Autowired
    private var teamCalDao: TeamCalDao? = null

    @Autowired
    private val teamCalCache: TeamCalCache? = null

    @Autowired
    private lateinit var teamEventExternalSubscriptionCache: TeamEventExternalSubscriptionCache

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    override val additionalHistorySearchDOs: Array<Class<*>> = arrayOf(TeamEventAttendeeDO::class.java)

    init {
        userRightId = UserRightId.PLUGIN_CALENDAR_EVENT
        isForceDeletionSupport = true
    }

    private fun logReminderChange(newObj: TeamEventDO) {
        var reminderHasChanged = false
        val message = StringBuilder()
        val dbObj = persistenceService.find(TeamEventDO::class.java, newObj.id) ?: return
        if ((dbObj.reminderActionType == null && newObj.reminderActionType != null)
            || (dbObj.reminderDuration == null && newObj.reminderDuration != null)
            || (dbObj.reminderDurationUnit == null && newObj.reminderDurationUnit != null)
        ) {
            reminderHasChanged = true
            message.append("DBObj was null -> new values were set; ")
        }
        if (dbObj.reminderActionType != null) {
            if (dbObj.reminderActionType != newObj.reminderActionType) {
                reminderHasChanged = true
                message.append(
                    ("DBObj.getReminderActionType() was " + dbObj.reminderActionType + " NewObj.getReminderActionType() is " + newObj.reminderActionType
                            + "; ")
                )
            }
        }
        if (dbObj.reminderDuration != null) {
            if (dbObj.reminderDuration != newObj.reminderDuration) {
                reminderHasChanged = true
                message.append(
                    "DBObj.getReminderDuration() was " + dbObj.reminderActionType + " NewObj.getReminderDuration() is " + newObj.reminderActionType + "; "
                )
            }
        }
        if (dbObj.reminderDurationUnit != null) {
            if (dbObj.reminderDurationUnit != newObj.reminderDurationUnit) {
                reminderHasChanged = true
                message.append(
                    ("DBObj.getReminderDurationUnit() was " + dbObj.reminderActionType + " NewObj.getReminderDurationUnit() is " + newObj.reminderActionType
                            + "; ")
                )
            }
        }
        if (reminderHasChanged) {
            val stackTrace = Thread.currentThread().stackTrace
            val changedByWebView =
                Arrays.stream(stackTrace)
                    .filter { ste: StackTraceElement -> ste.className.contains("org.projectforge.web.wicket.EditPageSupport") }
                    .count() > 0
            log.info(
                "TeamEventDao.internalUpdate -> Changed reminder of team event. Changed by: " + (if (changedByWebView) "WebView" else "REST") + " Message: " + message
                    .toString()
            )
        }
    }

    fun getCalIdList(teamCals: Collection<TeamCalDO>?): List<Long?> {
        val list: MutableList<Long?> = ArrayList()
        if (teamCals != null) {
            for (cal in teamCals) {
                list.add(cal.id)
            }
        }
        return list
    }

    /**
     * @param teamEvent
     * @param teamCalendarId If null, then team calendar will be set to null;
     * @see BaseDao.findOrLoad
     */
    fun setCalendar(teamEvent: TeamEventDO, teamCalendarId: Long) {
        val teamCal = teamCalDao!!.findOrLoad(teamCalendarId)
        teamEvent.calendar = teamCal
    }

    fun getByUid(calendarId: Long?, uid: String?): TeamEventDO? {
        return this.getByUid(calendarId, uid, true)
    }

    fun getByUid(calendarId: Long?, uid: String?, excludeDeleted: Boolean): TeamEventDO? {
        if (uid == null) {
            return null
        }

        val sqlQuery = StringBuilder()
        val params: MutableList<Pair<String, Any>> = ArrayList()

        sqlQuery.append("select e from TeamEventDO e where e.uid = :uid")

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

        return try {
            persistenceService.selectSingleResult(
                sqlQuery.toString(),
                TeamEventDO::class.java,
                *params.toTypedArray(),
                attached = true,
            )
        } catch (e: NoResultException) {
            null
        } catch (e: NonUniqueResultException) {
            null
        }
    }

    override fun onUpdate(obj: TeamEventDO, dbObj: TeamEventDO) {
        logReminderChange(obj)
        handleSeriesUpdates(obj)
        // only increment sequence if PF has ownership!
        if (obj.ownership != null && !obj.ownership!!) {
            return
        }

        // compute diff
        if (obj.mustIncSequence(dbObj)) {
            if (obj.sequence == null) {
                obj.sequence = 0
            } else {
                obj.sequence = obj.sequence!! + 1
            }

            if (obj.dtStamp == null || obj.dtStamp == dbObj.dtStamp) {
                obj.dtStamp = Date()
            }
        }
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
    private fun handleSeriesUpdates(event: TeamEventDO) {
        val selectedEvent =
            event.removeTransientAttribute(ATTR_SELECTED_ELEMENT) as ICalendarEvent? // Must be removed, otherwise save below will handle this attrs again.
        val mode = event.removeTransientAttribute(ATTR_SERIES_MODIFICATION_MODE) as SeriesModificationMode?
        if (selectedEvent == null || mode == null || mode == SeriesModificationMode.ALL) {
            // Nothing to do.
            return
        }
        val newEvent = event.clone()
        newEvent.sequence = 0
        val masterEvent = find(event.id)
        event.copyValuesFrom(masterEvent!!) // Restore db fields of master event. Do only modify single or future events.
        if (mode == SeriesModificationMode.FUTURE) {
            val recurrenceData = masterEvent.getRecurrenceData(timeZone)
            // Set the end date of the master date one day before current date and save this event.
            recurrenceData.setUntil(getUntilDate(selectedEvent.startDate!!))
            event.setRecurrence(recurrenceData)
            insert(newEvent)
            if (log.isDebugEnabled) {
                log.debug(
                    "Recurrence until date of master entry will be set to: " + DateHelper.formatAsUTC(
                        recurrenceData.until
                    )
                )
                log.debug("The new event is: $newEvent")
            }
            return
        } else if (mode == SeriesModificationMode.SINGLE) { // only current date
            // Add current date to the master date as exclusion date and save this event (without recurrence settings).
            event.addRecurrenceExDate(selectedEvent.startDate)
            if (newEvent.hasRecurrence()) {
                log.warn("User tries to modifiy single event of a series, the given recurrence is ignored.")
            }
            newEvent.setRecurrence(null as RRule?) // User only wants to modify single event, ignore recurrence.
            insert(newEvent)
            if (log.isDebugEnabled) {
                log.debug(
                    ("Recurrency ex date of master entry is now added: "
                            + DateHelper.formatAsUTC(selectedEvent.startDate)
                            + ". The new string is: "
                            + event.recurrenceExDate)
                )
                log.debug("The new event is: $newEvent")
            }
        }
    }

    /**
     * Handles deletion of series element (if any) for future and single events of a series.
     */
    override fun markAsDeleted(obj: TeamEventDO, checkAccess: Boolean) {
        val selectedEvent =
            obj.removeTransientAttribute(ATTR_SELECTED_ELEMENT) as ICalendarEvent? // Must be removed, otherwise update below will handle this attrs again.
        val mode = obj.removeTransientAttribute(ATTR_SERIES_MODIFICATION_MODE) as SeriesModificationMode?
        if (selectedEvent == null || mode == null || mode == SeriesModificationMode.ALL) {
            // Nothing to do special:
            super.markAsDeleted(obj, checkAccess)
            return
        }
        val masterEvent = find(obj.id)
        obj.copyValuesFrom(masterEvent!!) // Restore db fields of master event. Do only modify single or future events.
        if (mode == SeriesModificationMode.FUTURE) {
            val recurrenceData = obj.getRecurrenceData(timeZone)
            val recurrenceUntil = getUntilDate(selectedEvent.startDate!!)
            recurrenceData.setUntil(recurrenceUntil)
            obj.setRecurrence(recurrenceData)
            update(obj)
        } else if (mode == SeriesModificationMode.SINGLE) { // only current date
            requireNotNull(selectedEvent)
            obj.addRecurrenceExDate(selectedEvent.startDate)
            update(obj)
        }
    }

    override fun onInsert(obj: TeamEventDO) {
        // set ownership if empty
        if (obj.ownership == null) {
            obj.ownership = true
        }

        // set DTSTAMP if empty
        if (obj.dtStamp == null) {
            obj.dtStamp = Date(obj.created!!.time)
        }

        // create uid if empty
        if (StringUtils.isBlank(obj.uid)) {
            obj.uid = TeamCalConfig.get().createEventUid()
        }
    }

    /**
     * Sets midnight (UTC) of all day events.
     */
    override fun onInsertOrModify(obj: TeamEventDO, operationType: OperationType) {
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
                obj.startDate = getUTCBeginOfDayTimestamp(startDate)
            }
            val endDate = obj.endDate
            if (endDate != null) {
                obj.endDate = getUTCBeginOfDayTimestamp(endDate)
            }
        }
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
    fun getEventList(filter: TeamEventFilter, calculateRecurrenceEvents: Boolean): List<ICalendarEvent> {
        val result: MutableList<ICalendarEvent> = ArrayList()
        var list = select(filter).toMutableList()
        if (CollectionUtils.isNotEmpty(list)) {
            for (eventDO in list) {
                if (eventDO.hasRecurrence()) {
                    // Added later.
                    continue
                }
                result.add(eventDO)
            }
        }
        val teamEventFilter = filter.clone().setOnlyRecurrence(true)
        val qFilter = buildQueryFilter(teamEventFilter)
        qFilter.add(isNotNull("recurrenceRule"))
        list = select(qFilter).distinct().toMutableList()
        // add all abo events
        val recurrenceEvents = teamEventExternalSubscriptionCache
            .getRecurrenceEvents(teamEventFilter)
        if (recurrenceEvents != null && recurrenceEvents.size > 0) {
            list.addAll(recurrenceEvents)
        }
        val timeZone = timeZone
        for (eventDO in list) {
            if (!eventDO.hasRecurrence()) {
                // This event was handled above.
                continue
            }
            if (!calculateRecurrenceEvents) {
                result.add(eventDO)
                continue
            }
            val events =
                rollOutRecurrenceEvents(teamEventFilter.startDate, teamEventFilter.endDate, eventDO, timeZone)
                    ?: continue
            for (event in events) {
                if (!matches(event.startDate!!, event.endDate!!, event.allDay, teamEventFilter)) {
                    continue
                }
                result.add(event)
            }
        }
        return result
    }

    /**
     * @param checkAccess is ignored, only accessible calendars are used.
     * @see org.projectforge.framework.persistence.api.BaseDao.selectForSearchDao
     */
    override fun selectForSearchDao(filter: BaseSearchFilter, checkAccess: Boolean): List<TeamEventDO> {
        val teamEventFilter = TeamEventFilter(filter) // May-be called by SeachPage
        val allAccessibleCalendars = teamCalCache!!.allAccessibleCalendars
        if (CollectionUtils.isEmpty(allAccessibleCalendars)) {
            // No calendars accessible, nothing to search.
            return ArrayList()
        }
        teamEventFilter.setTeamCals(getCalIdList(allAccessibleCalendars))
        return select(teamEventFilter)
    }

    /**
     * @see org.projectforge.framework.persistence.api.BaseDao.select
     */
    override fun select(filter: BaseSearchFilter): List<TeamEventDO> {
        val teamEventFilter = if (filter is TeamEventFilter) {
            filter.clone()
        } else {
            TeamEventFilter(filter)
        }
        if (CollectionUtils.isEmpty(teamEventFilter.teamCals) && teamEventFilter.teamCalId == null) {
            return ArrayList()
        }
        val qFilter = buildQueryFilter(teamEventFilter)
        val list = select(qFilter)
        val result: MutableList<TeamEventDO> = ArrayList()
        for (event in list) {
            if (matches(event.startDate!!, event.endDate!!, event.allDay, teamEventFilter)) {
                result.add(event)
            }
        }
        // subscriptions
        val alreadyAdded: MutableList<Long> = ArrayList()
        // precondition for abos: existing teamcals in filter
        if (teamEventFilter.teamCals != null) {
            for (calendarId in teamEventFilter.teamCals) {
                if (teamEventExternalSubscriptionCache.isExternalSubscribedCalendar(calendarId)) {
                    addEventsToList(teamEventFilter, result, teamEventExternalSubscriptionCache, calendarId)
                    alreadyAdded.add(calendarId)
                }
            }
        }
        // if the getTeamCalId is not null and we do not added this before, do it now
        val teamCalId = teamEventFilter.teamCalId
        if (teamCalId != null && !alreadyAdded.contains(teamCalId)) {
            if (teamEventExternalSubscriptionCache.isExternalSubscribedCalendar(teamCalId)) {
                addEventsToList(teamEventFilter, result, teamEventExternalSubscriptionCache, teamCalId)
            }
        }
        return result
    }

    /**
     * Get all locations of the user's calendar events (not deleted ones) with modification date within last year.
     *
     * @param searchString
     */
    fun getLocationAutocompletion(searchString: String?, calendars: Array<TeamCalDO>?): List<String>? {
        if (calendars.isNullOrEmpty()) {
            return null
        }
        if (searchString.isNullOrBlank()) {
            return null
        }
        checkLoggedInUserSelectAccess()
        val sql =
            ("select distinct location from ${doClass.simpleName} t where deleted=false and t.calendar.id in :cals and lastUpdate > :lastUpdate and lower(t.location) like :location order by t.location")
        val calIds = mutableListOf<Long>()
        for (i in calendars.indices) {
            calendars[i].id?.let {
                calIds.add(it)
            }
        }
        return persistenceService.executeQuery(
            sql,
            String::class.java,
            Pair("cals", calIds),
            Pair("lastUpdate", now().minusYears(1).utilDate),
            Pair("location", "%${searchString.lowercase()}%"),
        )
    }

    private fun addEventsToList(
        teamEventFilter: TeamEventFilter, result: MutableList<TeamEventDO>,
        aboCache: TeamEventExternalSubscriptionCache, calendarId: Long
    ) {
        val startDate = teamEventFilter.startDate
        val endDate = teamEventFilter.endDate
        val startTime = startDate?.time ?: 0
        val endTime = endDate?.time ?: MAX_DATE_3000
        val events = aboCache.getEvents(calendarId, startTime, endTime)
        if (events != null && events.size > 0) {
            result.addAll(events)
        }
    }

    private fun matches(
        eventStartDate: Date, eventEndDate: Date, allDay: Boolean,
        teamEventFilter: TeamEventFilter
    ): Boolean {
        val startDate = teamEventFilter.startDate
        val endDate = teamEventFilter.endDate
        if (allDay) {
            // Check date match:
            if (startDate != null && eventEndDate.before(startDate)) {
                // Check same day (eventEndDate in UTC and startDate of filter in user's time zone):
                val startDateUserTimeZone = from(startDate) // not null
                val eventEndDateUTC = from(eventEndDate, PFDateTimeUtils.TIMEZONE_UTC)
                return startDateUserTimeZone.isSameDay(eventEndDateUTC)
            }
            if (endDate != null && eventStartDate.after(endDate)) {
                // Check same day (eventStartDate in UTC and endDate of filter in user's time zone):
                val endDateUserTimeZone = from(endDate) // not null
                val eventStartDateUTC = from(eventStartDate, PFDateTimeUtils.TIMEZONE_UTC) // not null
                return endDateUserTimeZone.isSameDay(eventStartDateUTC)
            }
            return true
        } else {
            // Check start and stop date due to extension of time period of buildQueryFilter:
            if (startDate != null && eventEndDate.before(startDate)) {
                return false
            }
            return endDate == null || !eventStartDate.after(endDate)
        }
    }

    /**
     * The time period of the filter will be extended by one day. This is needed due to all day events which are stored in
     * UTC. The additional events in the result list not matching the time period have to be removed by caller!
     *
     * @param filter
     * @return
     */
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
        var date = fromOrNull(filter.startDate)
        var startDate: Date? = null
        if (date != null) {
            startDate = date.beginOfDay.utilDate
        }
        date = fromOrNull(filter.endDate)
        var endDate: Date? = null
        if (date != null) {
            endDate = date.endOfDay.utilDate
        }
        // limit events to load to chosen date view.
        if (startDate != null && endDate != null) {
            if (!filter.isOnlyRecurrence) {
                queryFilter.add(
                    or(
                        between("startDate", startDate, endDate),
                        between(
                            "endDate",
                            startDate,
                            endDate
                        ),  // get events whose duration overlap with chosen duration.
                        and(le("startDate", startDate), ge("endDate", endDate))
                    )
                )
            } else {
                queryFilter.add( // "startDate" < endDate && ("recurrenceUntil" == null || "recurrenceUntil" > startDate)
                    (and(
                        lt("startDate", endDate),
                        or(
                            isNull("recurrenceUntil"),
                            gt("recurrenceUntil", startDate)
                        )
                    ))
                )
            }
        } else if (startDate != null) {
            if (!filter.isOnlyRecurrence) {
                queryFilter.add(ge("startDate", startDate))
            } else {
                // This branch is reached for subscriptions and calendar downloads.
                queryFilter.add( // "recurrenceUntil" == null || "recurrenceUntil" > startDate
                    or(isNull("recurrenceUntil"), gt("recurrenceUntil", startDate))
                )
            }
        } else if (endDate != null) {
            queryFilter.add(le("startDate", endDate))
        }
        queryFilter.addOrder(desc("startDate"))
        if (log.isDebugEnabled) {
            log.debug(ToStringBuilder.reflectionToString(filter))
        }
        return queryFilter
    }

    /**
     * Gets history entries of super and adds all history entries of the TeamEventAttendeeDO children.
     */
    override fun mergeHistoryEntries(
        obj: TeamEventDO,
        list: MutableList<HistoryEntryDO>,
        context: DisplayHistoryConvertContext<*>,
    ) {
        obj.attendees?.forEach { attendee ->
            val entries = historyService.loadHistory(attendee)
            HistoryFormatUtils.setPropertyNameForListEntries(entries,attendee.toString())
            mergeHistoryEntries(list, entries)
        }
    }

    /**
     * Returns also true, if idSet contains the id of any attendee.
     *
     * @see org.projectforge.framework.persistence.api.BaseDao.contains
     */
    override fun contains(idSet: Set<Long>?, entry: TeamEventDO): Boolean {
        idSet ?: return false
        if (super.contains(idSet, entry)) {
            return true
        }
        for (pos in entry.attendees!!) {
            if (idSet.contains(pos.id)) {
                return true
            }
        }
        return false
    }

    override fun newInstance(): TeamEventDO {
        return TeamEventDO()
    }

    /**
     * @param teamCalDao the teamCalDao to set
     */
    fun setTeamCalDao(teamCalDao: TeamCalDao?) {
        this.teamCalDao = teamCalDao
    }

    fun rollOutRecurrenceEvents(
        startDate: Date, endDate: Date,
        event: TeamEventDO, timeZone: java.util.TimeZone
    ): Collection<ICalendarEvent>? {
        if (!event.hasRecurrence()) {
            return null
        }
        val recur = event.recurrenceObject
            ?: // Shouldn't happen:
            return null

        val timeZone4Calc = timeZone
        val eventStartDateString = if (event.allDay)
            DateHelper.formatIsoDate(event.startDate, timeZone)
        else
            DateHelper
                .formatIsoTimestamp(event.startDate, DateHelper.UTC)
        val eventStartDate = event.startDate
        if (log.isDebugEnabled) {
            log.debug(
                ("---------- startDate=" + DateHelper.formatIsoTimestamp(eventStartDate, timeZone) + ", timeZone="
                        + timeZone.id)
            )
        }
        var ical4jTimeZone: TimeZone?
        try {
            ical4jTimeZone = ICal4JUtils.getTimeZone(timeZone4Calc)
        } catch (e: Exception) {
            log.error("Error getting timezone from ical4j.")
            ical4jTimeZone = ICal4JUtils.getUserTimeZone()
        }

        val ical4jStartDate = DateTime(startDate)
        ical4jStartDate.timeZone = ical4jTimeZone
        val ical4jEndDate = DateTime(endDate)
        ical4jEndDate.timeZone = ICal4JUtils.getTimeZone(timeZone4Calc)
        val seedDate = DateTime(eventStartDate)
        seedDate.timeZone = ICal4JUtils.getTimeZone(timeZone4Calc)

        // get ex dates of event
        val exDates = ICal4JUtils.parseCSVDatesAsJavaUtilDates(event.recurrenceExDate, DateHelper.UTC)

        // get events in time range
        val dateList = recur.getDates(seedDate, ical4jStartDate, ical4jEndDate, Value.DATE_TIME)

        // remove ex range values
        val col: MutableCollection<ICalendarEvent> = ArrayList()
        if (dateList != null) {
            OuterLoop@ for (obj in dateList) {
                val dateTime = obj as DateTime
                val isoDateString = if (event.allDay)
                    DateHelper.formatIsoDate(dateTime, timeZone)
                else
                    DateHelper.formatIsoTimestamp(dateTime, DateHelper.UTC)
                if (exDates != null && exDates.size > 0) {
                    for (exDate in exDates) {
                        if (!event.allDay) {
                            val recurDateJavaUtil = Date(dateTime.time)
                            if (recurDateJavaUtil == exDate) {
                                if (log.isDebugEnabled) {
                                    log.debug("= ex-dates equals: $isoDateString == $exDate")
                                }
                                // this date is part of ex dates, so don't use it.
                                continue@OuterLoop
                            }
                        } else {
                            // Allday event.
                            val isoExDateString = DateHelper.formatIsoDate(exDate, DateHelper.UTC)
                            if (isoDateString == isoExDateString) {
                                if (log.isDebugEnabled) {
                                    log.debug(
                                        String.format(
                                            "= ex-dates equals: %s == %s",
                                            isoDateString,
                                            isoExDateString
                                        )
                                    )
                                }
                                // this date is part of ex dates, so don't use it.
                                continue@OuterLoop
                            }
                        }
                        if (log.isDebugEnabled) {
                            log.debug("ex-dates not equals: $isoDateString != $exDate")
                        }
                    }
                }
                if (isoDateString == eventStartDateString) {
                    // Put event itself to the list.
                    col.add(event)
                } else {
                    // Now we need this event as date with the user's time-zone.
                    val date = from(dateTime.time, timeZone.toZoneId(), null, PFDateTime.NumberFormat.EPOCH_MILLIS)
                    val recurEvent = TeamRecurrenceEvent(event, date)
                    col.add(recurEvent)
                }
            }
        }
        if (log.isDebugEnabled) {
            for (ev in col) {
                log.debug(
                    ("startDate="
                            + DateHelper.formatIsoTimestamp(ev.startDate, timeZone)
                            + "; "
                            + DateHelper.formatAsUTC(ev.startDate)
                            + ", endDate="
                            + DateHelper.formatIsoTimestamp(ev.startDate, timeZone)
                            + "; "
                            + DateHelper.formatAsUTC(ev.endDate))
                )
            }
        }
        return col
    }

    /**
     * Will be called, before an address is (forced) deleted. All references in personal address books have to be deleted first.
     *
     * @param addressDO
     */
    fun removeAttendeeByAddressIdFromAllEvents(addressDO: AddressDO) {
        persistenceService.runInTransaction { context ->
            val addressId = addressDO.id ?: return@runInTransaction
            val counter: Int = context.executeNamedUpdate(
                TeamEventAttendeeDO.DELETE_ATTENDEE_BY_ADDRESS_ID_FROM_ALL_EVENTS,
                Pair("addressId", addressId),
            )
            if (counter > 0) {
                log.info("Address #" + addressId + " of '" + addressDO.fullName + "' removed as attendee in " + counter + " events.")
            }
        }
    }

    companion object {
        const val MIN_DATE_1800: Long = -5364662400000L

        const val MAX_DATE_3000: Long = 32535216000000L

        /**
         * For storing the selected element of the series in the transient attribute map for correct handling in [.onDelete]
         * and [.onSaveOrModify] of series (all, future, selected).
         */
        const val ATTR_SELECTED_ELEMENT: String = "selectedSeriesElement"

        /**
         * For series elements: what to modify in [.onDelete] and [.onSaveOrModify] of series (all, future, selected)?
         */
        const val ATTR_SERIES_MODIFICATION_MODE: String = "seriesModificationMode"

        private val ADDITIONAL_SEARCH_FIELDS: Array<String> = arrayOf("calendar.id", "calendar.title")
    }
}

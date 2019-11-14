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

package org.projectforge.business.teamcal.event.model

import de.micromata.genome.db.jpa.history.api.NoHistory
import de.micromata.genome.db.jpa.history.api.WithHistory
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.WeekDay
import net.fortuna.ical4j.model.property.RRule
import org.apache.commons.lang3.StringUtils
import org.hibernate.search.annotations.*
import org.projectforge.business.calendar.event.model.ICalendarEvent
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.event.RecurrenceMonthMode
import org.projectforge.business.teamcal.event.TeamEventRecurrenceData
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.calendar.ICal4JUtils
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.api.Constants
import org.projectforge.framework.persistence.api.PFPersistancyBehavior
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.RecurrenceFrequency
import org.projectforge.framework.time.TimePeriod
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import javax.persistence.*

/**
 * Overview of used (and may-be planned) fields:
 *
 *  * **ATTENDEE**: ATTENDEE;MEMBER="mailto:DEV-GROUP@example.com": mailto:joecool@example.com
 *  * **CONTACT**: CONTACT:Jim Dolittle\, ABC Industries\, +1-919-555-1234
 *  * **DTEND** - End date (DATE-TIME)
 *  * **DTSTAMP** - Time of creation (DATE-TIME)
 *  * **DTSTART** - Start date (DATE-TIME)
 *  * **EXDATE** - exception dates of recurrence (list of DATE-TIME)
 *  * **LAST-MODIFIED** - (DATE-TIME)
 *  * **PARTSTAT**=DECLINED:mailto:jsmith@example.com
 *  * **ORGANIZER**: ORGANIZER;CN=John Smith:mailto:jsmith@example.com
 *  * **RDATE** - Dates of recurrence
 *  * **RRULE** - Rule of recurrence: RRULE:FREQ=DAILY;UNTIL=19971224T000000Z
 *  * **UID**: UID:19960401T080045Z-4000F192713-0052@example.com
 *
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_CALENDAR_EVENT", uniqueConstraints = [UniqueConstraint(name = "unique_t_plugin_calendar_event_uid_calendar_fk", columnNames = ["uid", "calendar_fk"])], indexes = [javax.persistence.Index(name = "idx_fk_t_plugin_calendar_event_calendar_fk", columnList = "calendar_fk"), javax.persistence.Index(name = "idx_fk_t_plugin_calendar_event_tenant_id", columnList = "tenant_id"), javax.persistence.Index(name = "idx_plugin_team_cal_end_date", columnList = "calendar_fk, end_date"), javax.persistence.Index(name = "idx_plugin_team_cal_start_date", columnList = "calendar_fk, start_date"), javax.persistence.Index(name = "idx_plugin_team_cal_time", columnList = "calendar_fk, start_date, end_date")])
@WithHistory(noHistoryProperties = ["lastUpdate", "created"], nestedEntities = [TeamEventAttendeeDO::class])
@AUserRightId(value = "PLUGIN_CALENDAR_EVENT")
open class TeamEventDO : DefaultBaseDO(), ICalendarEvent, Cloneable {
    @PropertyInfo(i18nKey = "plugins.teamcal.event.subject")
    @Field
    @get:Column(length = Constants.LENGTH_SUBJECT)
    override var subject: String? = null

    @PropertyInfo(i18nKey = "plugins.teamcal.event.location")
    @Field
    @get:Column(length = Constants.LENGTH_SUBJECT)
    override var location: String? = null

    @PropertyInfo(i18nKey = "plugins.teamcal.event.allDay")
    @get:Column(name = "all_day")
    override var allDay: Boolean = false

    @PropertyInfo(i18nKey = "plugins.teamcal.event.beginDate")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.MINUTE, encoding = EncodingType.STRING)
    @get:Column(name = "start_date")
    override var startDate: Timestamp? = null

    @PropertyInfo(i18nKey = "plugins.teamcal.event.endDate")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.MINUTE, encoding = EncodingType.STRING)
    @get:Column(name = "end_date")
    override var endDate: Timestamp? = null

    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.SECOND, encoding = EncodingType.STRING)
    @NoHistory
    @get:Column(name = "last_email")
    open var lastEmail: Timestamp? = null

    @get:Column(name = "dt_stamp")
    open var dtStamp: Timestamp? = null

    @PropertyInfo(i18nKey = "plugins.teamcal.calendar")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "calendar_fk", nullable = false)
    open var calendar: TeamCalDO? = null

    @Transient
    private var recurrenceRuleObject: RRule? = null

    /**
     * RRULE (rfc5545)
     */
    @get:Column(name = "recurrence_rule", length = 4000)
    open var recurrenceRule: String? = null
        set(recurrenceRule) {
            this.recurrenceRuleObject = null
            field = if (StringUtils.startsWith(recurrenceRule, "RRULE:")) {
                recurrenceRule!!.substring(6)
            } else recurrenceRule
        }

    /**
     * EXDATE (rfc5545) Ex dates are time stamps of deleted events out of the recurrence events.
     *
     * Expected format is CSV (date,date,date).
     * Supported date formats are **yyyyMMdd** for all day events and **yyyyMMdd'T'HHmmss** otherwise.
     *
     * **Caution:** all timestamps must be represented in UTC!
     */
    @get:Column(name = "recurrence_ex_date", length = 4000)
    open var recurrenceExDate: String? = null

    /**
     * This field is RECURRENCE_ID. Isn't yet used (ex-date is always used instead in master event).
     */
    @get:Column(name = "recurrence_date")
    open var recurrenceReferenceDate: String? = null

    /**
     * Isn't yet used (ex-date is always used instead in master event).
     */
    @get:Column(name = "recurrence_reference_id", length = 255)
    open var recurrenceReferenceId: String? = null

    /**
     * If not given the recurrence will never ends. Identifies the last possible event occurrence.
     *
     * Please note: Do not set this property manually! It's set automatically by the recurrence rule! Otherwise the
     * display of calendar events may be incorrect.
     *
     * This field exist only for data-base query purposes.
     */
    @get:Column(name = "recurrence_until")
    open var recurrenceUntil: Date? = null

    @PropertyInfo(i18nKey = "plugins.teamcal.event.note")
    @Field
    @get:Column(length = 4000)
    override var note: String? = null

    open var attendees: MutableSet<TeamEventAttendeeDO>? = null
        @OneToMany(fetch = FetchType.EAGER)
        @JoinColumn(name = "team_event_fk")
        get() {
            if (field == null) {
                field = HashSet()
            }
            return field
        }

    @get:Column
    open var ownership: Boolean? = null

    @get:Column(length = 1000)
    open var organizer: String? = null

    @get:Column(length = 1000, name = "organizer_additional_params")
    open var organizerAdditionalParams: String? = null

    // See RFC 2445 section 4.8.7.4
    @get:Column
    open var sequence: Int? = 0

    /**
     * Loads or creates the team event uid. Its very important that the uid is always the same in every ics file, which is
     * created. So only one time creation.
     */
    @get:Column(nullable = false)
    override var uid: String? = null

    @get:Column(name = "reminder_duration")
    open var reminderDuration: Int? = null

    @get:Column(name = "reminder_duration_unit")
    @get:Enumerated(EnumType.STRING)
    open var reminderDurationUnit: ReminderDurationUnit? = null

    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "reminder_action_type")
    open var reminderActionType: ReminderActionType? = null

    @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
    @get:OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    @get:JoinColumn(name = "team_event_fk2")
    open var attachments: MutableSet<TeamEventAttachmentDO>? = null
        get() {
            if (field == null) {
                field = TreeSet()
            }
            return field
        }

    open var creator: PFUserDO? = null
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "team_event_fk_creator")
        get() {
            if (this.pk != null && field == null) {
                this.creator = this.calendar!!.owner
            }
            return field
        }

    val calendarId: Int?
        @Transient
        get() = calendar?.id

    /**
     * Will be renewed if [.setRecurrenceRule] is called.
     *
     * @return the recurrenceRuleObject
     */
    val recurrenceObject: Recur?
        @Transient
        get() {
            val rrule = getRecurrenceRuleObject()
            return rrule?.recur
        }

    val timePeriod: TimePeriod
        @Transient
        get() = TimePeriod(startDate, endDate, true)

    /**
     * @return Duration in millis if startTime and stopTime is given and stopTime is after startTime, otherwise 0.
     */
    val duration: Long
        @Transient
        get() = timePeriod.duration

    /**
     * Returns time zone of event owner.
     *
     * @return Returns time zone of event owner.
     */
    val timeZone: TimeZone
        @Transient
        get() {
            val user = this.creator ?: return DateHelper.UTC

            return user.timeZoneObject
        }

    /**
     * Clear fields for viewers with minimal access. If you add new fields don't forget to clear these fields here.
     */
    fun clearFields(): TeamEventDO {
        ownership = null
        note = null
        location = note
        subject = location
        attendees?.clear()
        organizer = null
        organizerAdditionalParams = null
        reminderDuration = null
        reminderDurationUnit = null
        reminderActionType = null
        lastEmail = null
        sequence = null
        dtStamp = null
        attachments?.clear()
        uid = null

        return this
    }

    /**
     * Creates a [TreeSet].
     *
     * @return this for chaining.
     */
    fun ensureAttendees(): MutableSet<TeamEventAttendeeDO> {
        if (this.attendees == null) {
            this.attendees = HashSet()
        }
        return this.attendees!!
    }

    fun addAttendee(attendee: TeamEventAttendeeDO): TeamEventDO {
        ensureAttendees()
        var number: Short = 1
        for (pos in attendees!!) {
            if (pos.number!! >= number) {
                number = pos.number!!
                number++
            }
        }
        attendee.number = number
        this.attendees!!.add(attendee)
        return this
    }

    /**
     * @param rRule
     * @return this for chaining.
     */
    @Transient
    fun setRecurrence(rRule: RRule?): TeamEventDO {
        if (rRule == null || rRule.recur == null) {
            this.recurrenceRuleObject = null
            this.recurrenceRule = null
            this.recurrenceUntil = null

            return this
        }

        val recur = rRule.recur

        if (recur.until != null) {
            this.recurrenceUntil = recur.until
        } else {
            this.recurrenceUntil = null
        }

        this.recurrenceRuleObject = null // do not use rRule param here!
        this.recurrenceRule = rRule.value

        return this
    }

    /**
     * @param recurrenceData
     * @return this for chaining.
     */
    @Transient
    fun setRecurrence(recurrenceData: TeamEventRecurrenceData?): TeamEventDO {
        if (recurrenceData == null || recurrenceData.frequency == null || recurrenceData.frequency == RecurrenceFrequency.NONE) {
            this.recurrenceRuleObject = null
            this.recurrenceRule = null
            this.recurrenceUntil = null

            return this
        }

        if (!recurrenceData.isCustomized) {
            recurrenceData.interval = 1
        }

        val recur = Recur()
        recur.interval = recurrenceData.interval
        recur.frequency = ICal4JUtils.getCal4JFrequencyString(recurrenceData.frequency)

        if (recurrenceData.frequency == RecurrenceFrequency.WEEKLY) {
            val weekdays = recurrenceData.weekdays
            for (i in 0..6) {
                if (weekdays[i]) {
                    when (i) {
                        0 -> recur.dayList.add(WeekDay.MO)
                        1 -> recur.dayList.add(WeekDay.TU)
                        2 -> recur.dayList.add(WeekDay.WE)
                        3 -> recur.dayList.add(WeekDay.TH)
                        4 -> recur.dayList.add(WeekDay.FR)
                        5 -> recur.dayList.add(WeekDay.SA)
                        6 -> recur.dayList.add(WeekDay.SU)
                    }
                }
            }
        } else if (recurrenceData.frequency == RecurrenceFrequency.MONTHLY) {
            if (recurrenceData.monthMode == RecurrenceMonthMode.EACH) {
                val monthdays = recurrenceData.monthdays
                for (i in 0..30) {
                    if (monthdays[i]) {
                        recur.monthDayList.add(i + 1)
                    }
                }
            } else if (recurrenceData.monthMode == RecurrenceMonthMode.ATTHE) {
                val offset = ICal4JUtils.getOffsetForRecurrenceFrequencyModeOne(recurrenceData.modeOneMonth)
                val weekDays = ICal4JUtils.getDayListForRecurrenceFrequencyModeTwo(recurrenceData.modeTwoMonth)
                for (weekDay in weekDays) {
                    recur.dayList.add(WeekDay(weekDay, offset))
                }
            }
        } else if (recurrenceData.frequency == RecurrenceFrequency.YEARLY) {
            val months = recurrenceData.months
            for (i in 0..11) {
                if (months[i]) {
                    recur.monthList.add(i + 1)
                }
            }
            if (recurrenceData.isYearMode) {
                val offset = ICal4JUtils.getOffsetForRecurrenceFrequencyModeOne(recurrenceData.modeOneYear)
                val weekDays = ICal4JUtils.getDayListForRecurrenceFrequencyModeTwo(recurrenceData.modeTwoYear)
                for (weekDay in weekDays) {
                    recur.dayList.add(WeekDay(weekDay, offset))
                }
            }
        }
        // Set until
        if (recurrenceData.until != null) {
            if (this.allDay) {
                // just use date, no time
                val untilICal4J = net.fortuna.ical4j.model.Date(recurrenceData.until)
                recur.until = untilICal4J
                this.recurrenceUntil = recurrenceData.until
            } else {
                this.recurrenceUntil = this.fixUntilInRecur(recur, recurrenceData.until, recurrenceData.timeZone)
            }
        } else {
            this.recurrenceUntil = null
        }

        val rrule = RRule(recur)

        this.recurrenceRuleObject = rrule
        this.recurrenceRule = rrule.value

        return this
    }

    private fun fixUntilInRecur(recur: Recur, until: Date, timezone: TimeZone): Date {
        // until in RecurrenceData is always in UTC!
        val calUntil = Calendar.getInstance(DateHelper.UTC)
        val calStart = Calendar.getInstance(timezone)

        calUntil.time = until
        //    calStart.setTime(this.startDate);

        // update date of start date to until date
        calStart.set(Calendar.YEAR, calUntil.get(Calendar.YEAR))
        calStart.set(Calendar.DAY_OF_YEAR, calUntil.get(Calendar.DAY_OF_YEAR))

        // set until to last limit of day in user time
        calStart.set(Calendar.HOUR_OF_DAY, 23)
        calStart.set(Calendar.MINUTE, 59)
        calStart.set(Calendar.SECOND, 59)
        calStart.set(Calendar.MILLISECOND, 0)

        // update recur until
        val untilICal4J = DateTime(calStart.time)
        untilICal4J.isUtc = true
        recur.until = untilICal4J

        // return new until date for DB usage
        return calStart.time
    }

    /**
     * @return true if any recurrenceRule is given, otherwise false.
     */
    @Transient
    fun hasRecurrence(): Boolean {
        return StringUtils.isNotBlank(this.recurrenceRule)
    }

    @Transient
    fun clearAllRecurrenceFields(): TeamEventDO {
        this.recurrenceRule = null
        this.recurrenceRuleObject = null
        this.recurrenceExDate = null
        this.recurrenceUntil = null

        return this
    }

    @Transient
    fun getRecurrenceData(timezone: TimeZone): TeamEventRecurrenceData {
        val recurrenceData = TeamEventRecurrenceData(timezone)
        val recur = this.recurrenceObject ?: return recurrenceData

        recurrenceData.interval = if (recur.interval == -1) 1 else recur.interval

        if (this.recurrenceUntil != null) {
            // transform until to timezone
            if (this.allDay) {
                recurrenceData.until = this.recurrenceUntil
            } else {
                // determine last possible event in event time zone (owner time zone)
                val calUntil = Calendar.getInstance(this.timeZone)
                val calStart = Calendar.getInstance(this.timeZone)

                calUntil.time = this.recurrenceUntil!!
                calStart.time = this.startDate!!

                calStart.set(Calendar.YEAR, calUntil.get(Calendar.YEAR))
                calStart.set(Calendar.DAY_OF_YEAR, calUntil.get(Calendar.DAY_OF_YEAR))

                if (calStart.after(calUntil)) {
                    calStart.add(Calendar.DAY_OF_YEAR, -1)
                }

                // move to target time zone and transform to UTC
                val calTimeZone = Calendar.getInstance(timezone)
                val calUTC = Calendar.getInstance(DateHelper.UTC)

                calTimeZone.time = calStart.time

                calUTC.set(Calendar.YEAR, calTimeZone.get(Calendar.YEAR))
                calUTC.set(Calendar.DAY_OF_YEAR, calTimeZone.get(Calendar.DAY_OF_YEAR))
                calUTC.set(Calendar.HOUR_OF_DAY, 0)
                calUTC.set(Calendar.MINUTE, 0)
                calUTC.set(Calendar.SECOND, 0)
                calUTC.set(Calendar.MILLISECOND, 0)

                recurrenceData.until = calUTC.time
            }
        }
        recurrenceData.frequency = ICal4JUtils.getFrequency(recur)

        if (recurrenceData.frequency == RecurrenceFrequency.WEEKLY) {
            val weekdays = recurrenceData.weekdays
            for (wd in recur.dayList) {
                recurrenceData.isCustomized = true
                when {
                    wd.day == WeekDay.MO.day -> weekdays[0] = true
                    wd.day == WeekDay.TU.day -> weekdays[1] = true
                    wd.day == WeekDay.WE.day -> weekdays[2] = true
                    wd.day == WeekDay.TH.day -> weekdays[3] = true
                    wd.day == WeekDay.FR.day -> weekdays[4] = true
                    wd.day == WeekDay.SA.day -> weekdays[5] = true
                    wd.day == WeekDay.SU.day -> weekdays[6] = true
                }
            }
            recurrenceData.weekdays = weekdays
        }
        if (recurrenceData.frequency == RecurrenceFrequency.MONTHLY) {
            recurrenceData.monthMode = RecurrenceMonthMode.NONE
            val monthdays = recurrenceData.monthdays
            for (day in recur.monthDayList) {
                recurrenceData.isCustomized = true
                recurrenceData.monthMode = RecurrenceMonthMode.EACH
                monthdays[day!! - 1] = true
            }
            recurrenceData.monthdays = monthdays

            var offset = 0
            if (recur.dayList.size == 1) {
                offset = recur.dayList[0].offset
            } else if (recur.dayList.size > 1 && recur.setPosList.size != 0) {
                offset = recur.setPosList[0]
            }
            if (recur.dayList.size != 0) {
                recurrenceData.isCustomized = true
                recurrenceData.monthMode = RecurrenceMonthMode.ATTHE
                recurrenceData.modeOneMonth = ICal4JUtils.getRecurrenceFrequencyModeOneByOffset(offset)
                recurrenceData.modeTwoMonth = ICal4JUtils.getRecurrenceFrequencyModeTwoForDay(recur.dayList)
            }
        }
        if (recurrenceData.frequency == RecurrenceFrequency.YEARLY) {
            val months = recurrenceData.months
            for (day in recur.monthList) {
                recurrenceData.isCustomized = true

                months[day!! - 1] = true
            }
            recurrenceData.months = months

            var offset = 0
            if (recur.dayList.size == 1) {
                offset = recur.dayList[0].offset
            } else if (recur.dayList.size > 1 && recur.setPosList.size != 0) {
                offset = recur.setPosList[0]
            }
            if (recur.dayList.size != 0) {
                recurrenceData.isCustomized = true
                recurrenceData.isYearMode = true
                recurrenceData.modeOneYear = ICal4JUtils.getRecurrenceFrequencyModeOneByOffset(offset)
                recurrenceData.modeTwoYear = ICal4JUtils.getRecurrenceFrequencyModeTwoForDay(recur.dayList)
            }
        }
        return recurrenceData
    }

    /**
     * Will be renewed if [.setRecurrenceRule] is called.
     *
     * @return the recurrenceRuleObject
     */
    @Transient
    fun getRecurrenceRuleObject(): RRule? {
        if (recurrenceRuleObject == null) {
            try {
                recurrenceRuleObject = ICal4JUtils.calculateRRule(recurrenceRule)
            } catch (ex: IllegalArgumentException) {
                log.error("RecurrenceRule '$recurrenceRule' not parseable: ${ex.message}.", ex)
            }
        }
        return recurrenceRuleObject
    }

    /**
     * Adds a new ExDate to this event.
     *
     * @param date
     * @return this for chaining.
     */
    fun addRecurrenceExDate(date: Date?): TeamEventDO {
        if (date == null) {
            return this
        }
        val exDate: String = ICal4JUtils.asICalDateString(date, DateHelper.UTC, allDay)
        if (recurrenceExDate == null || recurrenceExDate!!.isEmpty()) {
            recurrenceExDate = exDate
        } else if (!recurrenceExDate!!.contains(exDate)) {
            // Add this ExDate only if not yet added:
            recurrenceExDate = "$recurrenceExDate,$exDate"
        }
        return this
    }

    /**
     * @param recurrenceDate the recurrenceDate to set
     * @return this for chaining.
     */
    @Transient
    fun setRecurrenceDate(recurrenceDate: Date, timezone: TimeZone): TeamEventDO {
        val df = SimpleDateFormat(DateFormats.ICAL_DATETIME_FORMAT)
        // Need the user's time-zone for getting midnight of desired date.
        df.timeZone = timezone
        // But print it as UTC date:
        val recurrenceDateString = df.format(recurrenceDate)
        recurrenceReferenceDate = recurrenceDateString
        return this
    }

    /**
     * @param recurrenceReferenceId the recurrenceReferenceId to set
     * @return this for chaining.
     */
    fun setRecurrenceReferenceId(recurrenceReferenceId: Int?): TeamEventDO {
        this.recurrenceReferenceId = recurrenceReferenceId?.toString()
        return this
    }

    /**
     * Creates a [TreeSet].
     *
     * @return this for chaining.
     */
    fun ensureAttachments(): MutableSet<TeamEventAttachmentDO> {
        if (this.attachments == null) {
            this.attachments = TreeSet()
        }
        return this.attachments!!
    }

    fun addAttachment(attachment: TeamEventAttachmentDO): TeamEventDO {
        ensureAttachments()
        this.attachments!!.add(attachment)
        return this
    }

    /**
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + if (allDay) 1231 else 1237
        result = prime * result + if (attachments == null) 0 else attachments!!.hashCode()
        result = prime * result + if (attendees == null) 0 else attendees!!.hashCode()
        result = prime * result + if (calendar == null) 0 else calendar!!.hashCode()
        result = prime * result + if (endDate == null) 0 else endDate!!.hashCode()
        result = prime * result + if (location == null) 0 else location!!.hashCode()
        result = prime * result + if (note == null) 0 else note!!.hashCode()
        result = prime * result + if (recurrenceExDate == null) 0 else recurrenceExDate!!.hashCode()
        result = prime * result + if (recurrenceRule == null) 0 else recurrenceRule!!.hashCode()
        result = prime * result + if (recurrenceUntil == null) 0 else recurrenceUntil!!.hashCode()
        result = prime * result + if (startDate == null) 0 else startDate!!.hashCode()
        result = prime * result + if (subject == null) 0 else subject!!.hashCode()
        result = prime * result + if (organizer == null) 0 else organizer!!.hashCode()
        result = prime * result + if (organizerAdditionalParams == null) 0 else organizerAdditionalParams!!.hashCode()
        result = prime * result + if (dtStamp == null) 0 else dtStamp!!.hashCode()
        return result
    }

    /**
     * @see java.lang.Object.equals
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val o = other as TeamEventDO?
        if (allDay != o!!.allDay) {
            return false
        }

        if (ownership == null && o.ownership != null) {
            return false
        } else if (ownership !== o.ownership) {
            return false
        }

        if (attendees == null) {
            if (o.attendees != null) {
                return false
            }
        } else if (attendees != o.attendees) {
            return false
        }
        if (calendar == null) {
            if (o.calendar != null) {
                return false
            }
        } else if (calendar != o.calendar) {
            return false
        }
        if (endDate == null) {
            if (o.endDate != null) {
                return false
            }
        } else if (!endDate!!.equals(o.endDate)) {
            return false
        }
        if (location == null) {
            if (o.location != null) {
                return false
            }
        } else if (location != o.location) {
            return false
        }
        if (note == null) {
            if (o.note != null) {
                return false
            }
        } else if (note != o.note) {
            return false
        }
        if (recurrenceExDate == null) {
            if (o.recurrenceExDate != null) {
                return false
            }
        } else if (recurrenceExDate != o.recurrenceExDate) {
            return false
        }
        if (recurrenceRule == null) {
            if (o.recurrenceRule != null) {
                return false
            }
        } else if (recurrenceRule != o.recurrenceRule) {
            return false
        }
        if (recurrenceUntil == null) {
            if (o.recurrenceUntil != null) {
                return false
            }
        } else if (recurrenceUntil != o.recurrenceUntil) {
            return false
        }
        if (organizer == null) {
            if (o.organizer != null) {
                return false
            }
        } else if (organizer != o.organizer) {
            return false
        }
        if (organizerAdditionalParams == null) {
            if (o.organizerAdditionalParams != null) {
                return false
            }
        } else if (organizerAdditionalParams != o.organizerAdditionalParams) {
            return false
        }
        if (startDate == null) {
            if (o.startDate != null) {
                return false
            }
        } else if (!startDate!!.equals(o.startDate)) {
            return false
        }
        if (dtStamp == null) {
            if (o.dtStamp != null) {
                return false
            }
        } else if (!dtStamp!!.equals(o.dtStamp)) {
            return false
        }
        if (subject == null) {
            if (o.subject != null) {
                return false
            }
        } else if (subject != o.subject) {
            return false
        }
        if (attachments == null) {
            if (o.attachments != null) {
                return false
            }
        } else if (attachments != o.attachments) {
            return false
        }
        return true
    }

    @Transient
    fun mustIncSequence(other: TeamEventDO): Boolean {
        if (allDay != other.allDay) {
            return true
        }

        if (endDate == null) {
            if (other.endDate != null) {
                return true
            }
        } else if (!endDate!!.equals(other.endDate)) {
            return true
        }
        if (location == null) {
            if (other.location != null) {
                return true
            }
        } else if (location != other.location) {
            return true
        }
        if (note == null) {
            if (other.note != null) {
                return true
            }
        } else if (note != other.note) {
            return true
        }
        if (recurrenceExDate == null) {
            if (other.recurrenceExDate != null) {
                return true
            }
        } else if (recurrenceExDate != other.recurrenceExDate) {
            return true
        }
        if (recurrenceRule == null) {
            if (other.recurrenceRule != null) {
                return true
            }
        } else if (recurrenceRule != other.recurrenceRule) {
            return true
        }
        if (recurrenceUntil == null) {
            if (other.recurrenceUntil != null) {
                return true
            }
        } else if (recurrenceUntil != other.recurrenceUntil) {
            return true
        }
        if (organizer == null) {
            if (other.organizer != null) {
                return true
            }
        } else if (organizer != other.organizer) {
            return true
        }
        if (organizerAdditionalParams == null) {
            if (other.organizerAdditionalParams != null) {
                return true
            }
        } else if (organizerAdditionalParams != other.organizerAdditionalParams) {
            return true
        }
        if (startDate == null) {
            if (other.startDate != null) {
                return true
            }
        } else if (!startDate!!.equals(other.startDate)) {
            return true
        }
        if (subject == null) {
            if (other.subject != null) {
                return true
            }
        } else if (subject != other.subject) {
            return true
        }
        if (attendees == null || attendees!!.isEmpty()) {
            if (other.attendees != null && other.attendees!!.isNotEmpty()) {
                return true
            }
        } else if (attendees != other.attendees) {
            return true
        }
        if (attachments == null || attachments!!.isEmpty()) {
            if (other.attachments != null && other.attachments!!.isNotEmpty()) {
                return true
            }
        } else if (attachments != other.attachments) {
            return true
        }

        return false
    }

    /**
     * @see java.lang.Object.clone
     */
    public override fun clone(): TeamEventDO {
        val clone = TeamEventDO()
        clone.calendar = this.calendar
        clone.creator = this.creator
        clone.startDate = this.startDate
        clone.endDate = this.endDate
        clone.allDay = this.allDay
        clone.subject = this.subject
        clone.location = this.location
        clone.recurrenceExDate = this.recurrenceExDate
        clone.recurrenceRule = this.recurrenceRule
        clone.recurrenceReferenceDate = this.recurrenceReferenceDate
        clone.recurrenceReferenceId = this.recurrenceReferenceId
        clone.recurrenceUntil = this.recurrenceUntil
        clone.ownership = this.ownership
        clone.organizer = this.organizer
        clone.organizerAdditionalParams = this.organizerAdditionalParams
        clone.note = this.note
        clone.lastEmail = this.lastEmail
        clone.dtStamp = this.dtStamp
        clone.sequence = this.sequence
        clone.reminderDuration = this.reminderDuration
        clone.reminderDurationUnit = this.reminderDurationUnit
        clone.reminderActionType = this.reminderActionType
        if (this.attendees != null && this.attendees!!.isNotEmpty()) {
            clone.attendees = clone.ensureAttendees()
            for (attendee in this.attendees!!) {
                val cloneAttendee = TeamEventAttendeeDO()
                cloneAttendee.address = attendee.address
                cloneAttendee.comment = attendee.comment
                cloneAttendee.commentOfAttendee = attendee.commentOfAttendee
                cloneAttendee.loginToken = attendee.loginToken
                cloneAttendee.number = attendee.number
                cloneAttendee.status = attendee.status
                cloneAttendee.url = attendee.url
                cloneAttendee.user = attendee.user
                clone.addAttendee(cloneAttendee)
            }
        }
        if (this.attachments != null && this.attachments!!.isNotEmpty()) {
            clone.attachments = clone.ensureAttachments()
            for (attachment in this.attachments!!) {
                val cloneAttachment = TeamEventAttachmentDO()
                cloneAttachment.setFilename(attachment.filename!!)
                cloneAttachment.setContent(attachment.content!!)
                clone.addAttachment(cloneAttachment)
            }
        }
        return clone
    }

    @Transient
    fun createMinimalCopy(): TeamEventDO {
        val result = TeamEventDO()
        result.id = this.id
        result.calendar = this.calendar
        result.startDate = this.startDate
        result.endDate = this.endDate
        result.dtStamp = this.dtStamp
        result.allDay = this.allDay
        result.recurrenceExDate = this.recurrenceExDate
        result.recurrenceRule = this.recurrenceRule
        result.recurrenceReferenceDate = this.recurrenceReferenceDate
        result.recurrenceReferenceId = this.recurrenceReferenceId
        result.recurrenceUntil = this.recurrenceUntil
        result.sequence = this.sequence
        result.ownership = this.ownership
        result.organizer = this.organizer
        result.organizerAdditionalParams = this.organizerAdditionalParams
        return result
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(TeamEventDO::class.java)
    }
}

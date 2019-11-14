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

package org.projectforge.business.timesheet

import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.lang3.StringUtils
import org.hibernate.search.annotations.*
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.api.UserPrefParameter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateHolder
import org.projectforge.framework.time.DatePrecision
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.time.TimePeriod
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_TIMESHEET",
        indexes = [javax.persistence.Index(name = "idx_fk_t_timesheet_kost2_id", columnList = "kost2_id"),
            javax.persistence.Index(name = "idx_fk_t_timesheet_task_id", columnList = "task_id"),
            javax.persistence.Index(name = "idx_fk_t_timesheet_user_id", columnList = "user_id"),
            javax.persistence.Index(name = "idx_fk_t_timesheet_tenant_id", columnList = "tenant_id"),
            javax.persistence.Index(name = "idx_timesheet_user_time", columnList = "user_id, start_time")])
@NamedQueries(
        NamedQuery(name = TimesheetDO.FIND_START_STOP_BY_TASKID,
                query = "select startTime, stopTime from TimesheetDO where task.id = :taskId and deleted = false"),
        NamedQuery(name = TimesheetDO.SELECT_MIN_MAX_DATE_FOR_USER,
                query = "select min(startTime), max(startTime) from TimesheetDO where user.id=:userId and deleted=false"),
        NamedQuery(name = TimesheetDO.SELECT_USED_LOCATIONS_BY_USER_AND_LOCATION_SEARCHSTRING,
                query = "select distinct location from TimesheetDO where deleted=false and user.id=:userId and lastUpdate>:lastUpdate and lower(location) like :locationSearch order by location"),
        NamedQuery(name = TimesheetDO.SELECT_RECENT_USED_LOCATIONS_BY_USER_AND_LAST_UPDATE,
                query = "select distinct location from TimesheetDO where deleted=false and user.id=:userId and lastUpdate>:lastUpdate and location!=null and location!='' order by lastUpdate desc"))
open class TimesheetDO : DefaultBaseDO(), Comparable<TimesheetDO> {

    @PropertyInfo(i18nKey = "task")
    @UserPrefParameter(i18nKey = "task", orderString = "2")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "task_id", nullable = false)
    open var task: TaskDO? = null

    @PropertyInfo(i18nKey = "user")
    @UserPrefParameter(i18nKey = "user", orderString = "1")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "user_id", nullable = false)
    open var user: PFUserDO? = null

    @get:Column(name = "time_zone", length = 100)
    open var timeZone: String? = null

    @PropertyInfo(i18nKey = "timesheet.startTime")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.MINUTE, encoding = EncodingType.STRING)
    @get:Column(name = "start_time", nullable = false)
    open var startTime: Timestamp? = null

    @PropertyInfo(i18nKey = "timesheet.stopTime")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.MINUTE, encoding = EncodingType.STRING)
    @get:Column(name = "stop_time", nullable = false)
    open var stopTime: Timestamp? = null

    @PropertyInfo(i18nKey = "timesheet.location")
    @UserPrefParameter(i18nKey = "timesheet.location")
    @Field
    @get:Column(length = 100)
    open var location: String? = null

    @PropertyInfo(i18nKey = "timesheet.description")
    @UserPrefParameter(i18nKey = "description", multiline = true)
    @Field
    @get:Column(length = 4000)
    open var description: String? = null

    @PropertyInfo(i18nKey = "fibu.kost2")
    @UserPrefParameter(i18nKey = "fibu.kost2", orderString = "3", dependsOn = "task")
    @IndexedEmbedded(depth = 2)
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "kost2_id", nullable = true)
    open var kost2: Kost2DO? = null

    /**
     * Marker is used to mark this time sheet e. g. as a time sheet with an time period collision.
     */
    @get:Transient
    @JsonIgnore
    open var marked: Boolean = false

    /**
     * @return Duration in millis if startTime and stopTime is given and stopTime is after startTime, otherwise 0.
     */
    @Transient
    fun getDuration(): Long {
        return timePeriod.duration
    }

    /**
     * If this entry has a kost2 with a working time fraction set or a kost2art with a working time fraction set then the
     * fraction of millis will be returned.
     */
    val workFractionDuration: Long
        @Transient
        get() {
            if (kost2 != null) {
                if (kost2!!.workFraction != null) {
                    return (kost2!!.workFraction!!.toDouble() * timePeriod.duration).toLong()
                }
                val kost2Art = kost2!!.kost2Art
                if (kost2Art?.workFraction != null) {
                    return (kost2Art.workFraction!!.toDouble() * timePeriod.duration).toLong()
                }
            }
            return getDuration()
        }

    /**
     * @return The abbreviated description (maximum length is 50 characters).
     */
    @Transient
    fun getShortDescription(): String? {
        return if (this.description == null)
            ""
        else
            StringUtils.abbreviate(description, 50)
    }

    /**
     * @return
     * @see DateTimeFormatter.formatWeekOfYear
     */
    @Transient
    fun getFormattedWeekOfYear(): String {
        return DateTimeFormatter.formatWeekOfYear(startTime)
    }

    val timePeriod: TimePeriod
        @Transient
        get() = TimePeriod(startTime, stopTime, marked)

    val userId: Int?
        @Transient
        get() = if (this.user == null) {
            null
        } else user!!.id

    val taskId: Int?
        @Transient
        get() = if (this.task == null) {
            null
        } else task!!.id

    val kost2Id: Int?
        @Transient
        get() = if (this.kost2 == null) {
            null
        } else kost2!!.id


    /**
     * Rounds the timestamp to DatePrecision.MINUTE_15 before.
     *
     * @param startDate the startTime to set
     * @see DateHolder#DateHolder(Date, DatePrecision)
     */
    @Transient
    fun setStartDate(startDate: Date?): TimesheetDO {
        if (startDate != null) {
            val date = DateHolder(startDate, DatePrecision.MINUTE_15)
            this.startTime = date.timestamp
        } else {
            this.stopTime = null
        }
        return this
    }

    @Transient
    fun setStartDate(millis: Long): TimesheetDO {
        setStartDate(Date(millis))
        return this
    }

    /**
     * Rounds the timestamp to DatePrecision.MINUTE_15 before.
     *
     * @param stopDate the stopTime to set
     * @return this for chaining.
     * @see DateHolder#DateHolder(Date, DatePrecision)
     */
    @Transient
    fun setStopDate(stopDate: Date?): TimesheetDO {
        if (stopDate != null) {
            val date = DateHolder(stopDate, DatePrecision.MINUTE_15)
            this.stopTime = date.timestamp
        } else {
            this.stopTime = null
        }
        return this
    }

    @Transient
    fun setStopDate(millis: Long): TimesheetDO {
        stopTime = Timestamp(millis)
        return this
    }

    override fun compareTo(other: TimesheetDO): Int {
        return startTime?.compareTo(other.startTime) ?: 1
    }

    companion object {
        const val FIND_START_STOP_BY_TASKID = "TimesheetDO_FindStartStopByTaskId"
        internal const val SELECT_MIN_MAX_DATE_FOR_USER = "TimesheetDO_SelectMinMaxDateForUser"
        internal const val SELECT_USED_LOCATIONS_BY_USER_AND_LOCATION_SEARCHSTRING = "TimesheetDO_SelectLocationsByUserAndLocationSearchstring"
        internal const val SELECT_RECENT_USED_LOCATIONS_BY_USER_AND_LAST_UPDATE = "TimesheetDO_SelectRecentUsedLocationsByUserAndLastUpdate"
    }
}

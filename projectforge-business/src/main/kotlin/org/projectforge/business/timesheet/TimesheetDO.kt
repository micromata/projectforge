/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.apache.commons.lang3.StringUtils
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.framework.calendar.DurationUtils
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.api.UserPrefParameter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.*
import java.math.BigDecimal
import java.util.*

/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(
    name = "T_TIMESHEET",
    indexes = [jakarta.persistence.Index(name = "idx_fk_t_timesheet_kost2_id", columnList = "kost2_id"),
        jakarta.persistence.Index(name = "idx_fk_t_timesheet_task_id", columnList = "task_id"),
        jakarta.persistence.Index(name = "idx_fk_t_timesheet_user_id", columnList = "user_id"),
        jakarta.persistence.Index(name = "idx_timesheet_user_time", columnList = "user_id, start_time")]
)
@NamedQueries(
    NamedQuery(
        name = TimesheetDO.FIND_START_STOP_BY_TASKID,
        query = "select startTime, stopTime from TimesheetDO where task.id = :taskId and deleted = false"
    ),
    NamedQuery(
        name = TimesheetDO.SELECT_MIN_MAX_DATE_FOR_USER,
        query = "select min(startTime), max(startTime) from TimesheetDO where user.id=:userId and deleted=false"
    ),
    NamedQuery(
        name = TimesheetDO.SELECT_USED_LOCATIONS_BY_USER_AND_LOCATION_SEARCHSTRING,
        query = "select distinct location from TimesheetDO where deleted=false and user.id=:userId and lastUpdate>:lastUpdate and lower(location) like :locationSearch order by location"
    ),
    NamedQuery(
        name = TimesheetDO.SELECT_RECENT_USED_LOCATIONS_BY_USER_AND_LAST_UPDATE,
        query = "select distinct location from TimesheetDO where deleted=false and user.id=:userId and lastUpdate>:lastUpdate and location!='' order by location"
    ),
    NamedQuery(
        name = TimesheetDO.SELECT_REFERENCES_BY_TASK_ID,
        query = "select distinct reference from TimesheetDO where deleted=false and task.id in :taskIds and reference is not NULL"
    )
)
open class TimesheetDO : DefaultBaseDO(), Comparable<TimesheetDO> {
    enum class TimeSavedByAIUnit(override val i18nKey: String) : I18nEnum {
        PERCENTAGE("percent"),
        HOURS("hours");
    }

    @PropertyInfo(i18nKey = "task")
    @UserPrefParameter(i18nKey = "task", orderString = "2")
    @IndexedEmbedded(includeDepth = 1)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "task_id", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var task: TaskDO? = null

    @PropertyInfo(i18nKey = "user")
    @UserPrefParameter(i18nKey = "user", orderString = "1")
    @IndexedEmbedded(includeDepth = 1, includeEmbeddedObjectId = true)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "user_id", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var user: PFUserDO? = null

    @get:Column(name = "time_zone", length = 100)
    open var timeZone: String? = null

    @PropertyInfo(i18nKey = "timesheet.startTime")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    //@DateBridge(resolution = Resolution.MINUTE, encoding = EncodingType.STRING)
    @get:Column(name = "start_time", nullable = false)
    open var startTime: Date? = null

    @PropertyInfo(i18nKey = "timesheet.stopTime")
    @GenericField // was:  @FullTextField(analyze = Analyze.NO)
    //@DateBridge(resolution = Resolution.MINUTE, encoding = EncodingType.STRING)
    @get:Column(name = "stop_time", nullable = false)
    open var stopTime: Date? = null

    @PropertyInfo(i18nKey = "timesheet.ai.timeSavedByAI", tooltip = "timesheet.ai.timeSavedByAI.info")
    @get:Column(name = "time_saved_by_ai")
    open var timeSavedByAI: BigDecimal? = null

    @PropertyInfo(i18nKey = "timesheet.ai.timeSavedByAIUnit", tooltip = "timesheet.ai.timeSavedByAIUnit.info")
    @get:Column(name = "time_saved_by_ai_unit")
    open var timeSavedByAIUnit: TimeSavedByAIUnit? = null

    @PropertyInfo(
        i18nKey = "timesheet.ai.timeSavedByAIDescription",
        tooltip = "timesheet.ai.timeSavedByAIDescription.info"
    )
    @get:Column(name = "time_saved_by_ai_description", length = 1000)
    open var timeSavedByAIDescription: String? = null

    @get:Transient
    val timeSavedByAIMillis: Long
        get() = AITimeSavings.getTimeSavedByAIMs(this)

    @PropertyInfo(i18nKey = "timesheet.location")
    @UserPrefParameter(i18nKey = "timesheet.location")
    @FullTextField
    @get:Column(length = 100)
    open var location: String? = null

    /**
     * Free multi purpose field.
     */
    @PropertyInfo(i18nKey = "timesheet.reference")
    @FullTextField
    @get:Column(length = 1000)
    open var reference: String? = null

    /**
     *
     */
    @PropertyInfo(i18nKey = "timesheet.tag")
    @FullTextField
    @get:Column(length = 1000)
    open var tag: String? = null

    @PropertyInfo(i18nKey = "timesheet.description")
    @UserPrefParameter(i18nKey = "description", multiline = true)
    @FullTextField
    @get:Column(length = 4000)
    open var description: String? = null

    @PropertyInfo(i18nKey = "fibu.kost2")
    @UserPrefParameter(i18nKey = "fibu.kost2", orderString = "3", dependsOn = "task")
    @IndexedEmbedded(includeDepth = 2)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "kost2_id", nullable = true)
    @JsonSerialize(using = IdOnlySerializer::class)
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
    val duration: Long
        @Transient
        get() = timePeriod.duration

    /**
     * @return hours:minutes (e.g. 2:30, 12:30, ...)
     */
    val durationAsString: String
        @Transient
        get() = DurationUtils.getFormattedHoursAndMinutes(duration)

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
            return duration
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

    val userId: Long?
        @Transient
        get() = if (this.user == null) {
            null
        } else user!!.id

    val taskId: Long?
        @Transient
        get() = if (this.task == null) {
            null
        } else task!!.id

    val kost2Id: Long?
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
            val date = PFDateTime.from(startDate).withPrecision(DatePrecision.MINUTE_5)
            this.startTime = date.sqlTimestamp
        } else {
            this.stopTime = null
        }
        return this
    }

    @Transient
    fun setStartDate(millis: Long): TimesheetDO {
        startTime = Date(millis)
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
            val date = PFDateTime.from(stopDate).withPrecision(DatePrecision.MINUTE_5)
            this.stopTime = date.sqlTimestamp
        } else {
            this.stopTime = null
        }
        return this
    }

    @Transient
    fun setStopDate(millis: Long): TimesheetDO {
        stopTime = Date(millis)
        return this
    }

    override fun compareTo(other: TimesheetDO): Int {
        return startTime?.compareTo(other.startTime) ?: 1
    }

    companion object {
        const val FIND_START_STOP_BY_TASKID = "TimesheetDO_FindStartStopByTaskId"
        internal const val SELECT_MIN_MAX_DATE_FOR_USER = "TimesheetDO_SelectMinMaxDateForUser"
        internal const val SELECT_USED_LOCATIONS_BY_USER_AND_LOCATION_SEARCHSTRING =
            "TimesheetDO_SelectLocationsByUserAndLocationSearchstring"
        internal const val SELECT_RECENT_USED_LOCATIONS_BY_USER_AND_LAST_UPDATE =
            "TimesheetDO_SelectRecentUsedLocationsByUserAndLastUpdate"
        internal const val SELECT_REFERENCES_BY_TASK_ID = "TimesheetDO_SelectReferencesByTaskId"
    }
}

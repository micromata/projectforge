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

package org.projectforge.business.humanresources

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import mu.KotlinLogging
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.history.PersistenceBehavior
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.time.DayHolder
import org.projectforge.framework.time.PFDay
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}

/**
 *
 * @author Mario Groß (m.gross@micromata.de)
 */
@Entity
@Indexed
@Table(
    name = "T_HR_PLANNING",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_fk", "week"])],
    indexes = [jakarta.persistence.Index(name = "idx_fk_t_hr_planning_user_fk", columnList = "user_fk")]
)
//@WithHistory(noHistoryProperties = ["lastUpdate", "created"], nestedEntities = [HRPlanningEntryDO::class])
@NamedQueries(
    NamedQuery(
        name = HRPlanningDO.FIND_BY_USER_AND_WEEK,
        query = "from HRPlanningDO where user.id=:userId and week=:week"
    ),
    NamedQuery(
        name = HRPlanningDO.FIND_OTHER_BY_USER_AND_WEEK,
        query = "from HRPlanningDO where user.id=:userId and week=:week and id!=:id"
    )
)
open class HRPlanningDO : DefaultBaseDO() {

    /**
     * The employee assigned to this planned week.
     */
    @PropertyInfo(i18nKey = "timesheet.user")
    @IndexedEmbedded(includeDepth = 1)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "user_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var user: PFUserDO? = null

    /**
     * @return The first day of the week.
     */
    @PropertyInfo(i18nKey = "calendar.year")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "week", nullable = false)
    open var week: LocalDate? = null

    /**
     * Get the entries for this planned week.
     */
    @PersistenceBehavior(autoUpdateCollectionEntries = true)
    // @get:ContainedIn
    @get:OneToMany(
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH],
        orphanRemoval = false,
        mappedBy = "planning", fetch = FetchType.LAZY, targetEntity = HRPlanningEntryDO::class,
    )
    open var entries: MutableList<HRPlanningEntryDO>? = null

    val formattedWeekOfYear: String
        @Transient
        get() = DateTimeFormatter.formatWeekOfYear(week)

    val userId: Long?
        @Transient
        get() = if (this.user == null) {
            null
        } else user!!.id

    /**
     * @return The total duration of all entries.
     * @see HRPlanningEntryDO.totalHours
     */
    val totalHours: BigDecimal
        @Transient
        get() {
            var duration = BigDecimal.ZERO
            if (entries == null) {
                return duration
            }
            for (entry in entries!!) {
                if (!entry.deleted) {
                    duration = duration.add(entry.totalHours)
                }
            }
            return duration
        }

    /**
     * @return The total hours of all unassigned entries.
     * @see HRPlanningEntryDO.unassignedHours
     */
    val totalUnassignedHours: BigDecimal
        @Transient
        get() {
            var duration = BigDecimal.ZERO
            if (entries == null) {
                return duration
            }
            for (entry in entries!!) {
                if (!entry.deleted) {
                    duration = add(duration, entry.unassignedHours)
                }
            }
            return duration
        }

    /**
     * @return The total hours of all entries for Monday.
     * @see HRPlanningEntryDO.mondayHours
     */
    val totalMondayHours: BigDecimal
        @Transient
        get() {
            var duration = BigDecimal.ZERO
            if (entries == null) {
                return duration
            }
            for (entry in entries!!) {
                if (!entry.deleted) {
                    duration = add(duration, entry.mondayHours)
                }
            }
            return duration
        }

    /**
     * @return The total hours of all entries for Tuesday.
     * @see HRPlanningEntryDO.tuesdayHours
     */
    val totalTuesdayHours: BigDecimal
        @Transient
        get() {
            var duration = BigDecimal.ZERO
            if (entries == null) {
                return duration
            }
            for (entry in entries!!) {
                if (!entry.deleted) {
                    duration = add(duration, entry.tuesdayHours)
                }
            }
            return duration
        }

    /**
     * @return The total hours of all entries for Wednesday.
     * @see HRPlanningEntryDO.wednesdayHours
     */
    val totalWednesdayHours: BigDecimal
        @Transient
        get() {
            var duration = BigDecimal.ZERO
            if (entries == null) {
                return duration
            }
            for (entry in entries!!) {
                if (!entry.deleted) {
                    duration = add(duration, entry.wednesdayHours)
                }
            }
            return duration
        }

    /**
     * @return The total hours of all entries for Thursday.
     * @see HRPlanningEntryDO.thursdayHours
     */
    val totalThursdayHours: BigDecimal
        @Transient
        get() {
            var duration = BigDecimal.ZERO
            if (entries == null) {
                return duration
            }
            for (entry in entries!!) {
                if (!entry.deleted) {
                    duration = add(duration, entry.thursdayHours)
                }
            }
            return duration
        }

    /**
     * @return The total hours of all entries for Friday.
     * @see HRPlanningEntryDO.fridayHours
     */
    val totalFridayHours: BigDecimal
        @Transient
        get() {
            var duration = BigDecimal.ZERO
            if (entries == null) {
                return duration
            }
            for (entry in entries!!) {
                if (!entry.deleted) {
                    duration = add(duration, entry.fridayHours)
                }
            }
            return duration
        }

    /**
     * @return The total hours of all entries for weekend.
     * @see HRPlanningEntryDO.weekendHours
     */
    val totalWeekendHours: BigDecimal
        @Transient
        get() {
            var duration = BigDecimal.ZERO
            if (entries == null) {
                return duration
            }
            for (entry in entries!!) {
                if (!entry.deleted) {
                    duration = add(duration, entry.weekendHours)
                }
            }
            return duration
        }

    /**
     * @param week
     * @see .getFirstDayOfWeek
     */
    fun setFirstDayOfWeek(week: LocalDate) {
        this.week = getFirstDayOfWeek(week)
    }

    fun addEntry(entry: HRPlanningEntryDO) {
        ensureAndGetEntries()
        entry.planning = this
        this.entries!!.add(entry)
    }

    /**
     * Deletes the given entry from the list of entries if not already persisted. If the entry is already persisted then
     * the entry will be marked as deleted. Undelete is possible by adding a entry with the same status/project again.
     *
     * @param entry
     */
    fun deleteEntry(entry: HRPlanningEntryDO) {
        if (this.entries == null) {
            log.error("Can't remove entry because the list of entries is null (do nothing): $entry")
            return
        }
        if (entry.id == null) {
            if (!this.entries!!.remove(entry)) {
                log.error("Can't remove entry because the list of entries does not contain such an entry: $entry")
            }
        } else {
            entry.deleted = true
        }
    }

    fun ensureAndGetEntries(): List<HRPlanningEntryDO>? {
        if (this.entries == null) {
            entries = ArrayList()
        }
        return entries
    }

    /**
     * @param idx
     * @return HRPlanningEntryDO with given index or null, if not exist.
     */
    fun getEntry(idx: Int): HRPlanningEntryDO? {
        if (entries == null) {
            return null
        }
        return if (idx >= entries!!.size) { // Index out of bounds.
            null
        } else entries!![idx]
    }

    fun getProjectEntry(project: ProjektDO): HRPlanningEntryDO? {
        if (entries == null) {
            return null
        }
        for (entry in entries!!) {
            if (project.id == entry.projektId) {
                return entry
            }
        }
        return null
    }

    fun getStatusEntry(status: HRPlanningEntryStatus): HRPlanningEntryDO? {
        if (entries == null) {
            return null
        }
        for (entry in entries!!) {
            if (entry.status == status) {
                return entry
            }
        }
        return null
    }

    private fun add(sum: BigDecimal, value: BigDecimal?): BigDecimal {
        return if (value == null) {
            sum
        } else {
            sum.add(value)
        }
    }

    fun hasDeletedEntries(): Boolean {
        if (this.entries == null) {
            return false
        }
        for (entry in this.entries!!) {
            if (entry.deleted) {
                return true
            }
        }
        return false
    }

    companion object {
        internal const val FIND_BY_USER_AND_WEEK = "HrPlanningDO_FindByUserAndWeek"

        internal const val FIND_OTHER_BY_USER_AND_WEEK = "HrPlanningDO_FindOtherByUserAndWeek"

        /**
         * @param date
         * @return The first day of week (UTC). The first day of the week is monday (use Locale.GERMAN) because monday is the
         * first working day of the week.
         * @see DayHolder.setBeginOfWeek
         */
        fun getFirstDayOfWeek(date: LocalDate): LocalDate {
            var day = PFDay.from(date)
            day = day.beginOfWeek
            return day.localDate
        }

        /**
         * @param date
         * @return The first day of week (UTC). The first day of the week is monday (use Locale.GERMAN) because monday is the
         * first working day of the week.
         * @see DayHolder.setBeginOfWeek
         */
        fun getFirstDayOfWeek(date: Date): Date {
            val day = DayHolder(date, DateHelper.UTC)
            day.setBeginOfWeek()
            return day.utilDate
        }
    }
}

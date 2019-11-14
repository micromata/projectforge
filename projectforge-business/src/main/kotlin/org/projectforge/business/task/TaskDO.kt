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

package org.projectforge.business.task

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlPersist
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.hibernate.search.annotations.*
import org.hibernate.search.bridge.builtin.IntegerBridge
import org.projectforge.business.gantt.GanttObjectType
import org.projectforge.business.gantt.GanttRelationType
import org.projectforge.common.StringHelper
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.i18n.Priority
import org.projectforge.common.task.TaskStatus
import org.projectforge.common.task.TimesheetBookingStatus
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.math.BigDecimal
import java.util.*
import javax.persistence.*
import javax.persistence.Index

/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@ClassBridge(name = "taskpath", impl = HibernateSearchTaskPathBridge::class)
@Table(name = "T_TASK",
        uniqueConstraints = [UniqueConstraint(columnNames = ["parent_task_id", "title"])],
        indexes = [
            Index(name = "idx_fk_t_task_gantt_predecessor_fk", columnList = "gantt_predecessor_fk"),
            Index(name = "idx_fk_t_task_parent_task_id", columnList = "parent_task_id"),
            Index(name = "idx_fk_t_task_responsible_user_id", columnList = "responsible_user_id"),
            Index(name = "idx_fk_t_task_tenant_id", columnList = "tenant_id")])
@JpaXmlPersist(beforePersistListener = [TaskXmlBeforePersistListener::class])
@NamedQueries(
        NamedQuery(name = TaskDO.FIND_OTHER_TASK_BY_PARENTTASKID_AND_TITLE,
                query = "from TaskDO where parentTask.id=:parentTaskId and title=:title and id!=:id"),
        NamedQuery(name = TaskDO.FIND_BY_PARENTTASKID_AND_TITLE,
                query = "from TaskDO where parentTask.id=:parentTaskId and title=:title"))
open class TaskDO : DefaultBaseDO(), ShortDisplayNameCapable, Cloneable// , GanttObject
{

    @PropertyInfo(i18nKey = "task.parentTask")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "parent_task_id")
    open var parentTask: TaskDO? = null

    @PropertyInfo(i18nKey = "task.title")
    @Field
    @get:Column(length = TITLE_LENGTH, nullable = false)
    open var title: String? = null

    @PropertyInfo(i18nKey = "status")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = STATUS_LENGTH)
    open var status = TaskStatus.N

    @PropertyInfo(i18nKey = "priority")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = PRIORITY_LENGTH)
    open var priority: Priority? = null

    @PropertyInfo(i18nKey = "shortDescription")
    @Field
    @get:Column(name = "short_description", length = SHORT_DESCRIPTION_LENGTH)
    open var shortDescription: String? = null

    @PropertyInfo(i18nKey = "description")
    @Field
    @get:Column(name = "description", length = DESCRIPTION_LENGTH)
    open var description: String? = null

    /** -&gt; Gantt  */
    @Deprecated("Properties of Gantt diagram will be refactored some day.")
    @Field(analyze = Analyze.NO, bridge = FieldBridge(impl = IntegerBridge::class))
    @PropertyInfo(i18nKey = "task.progress")
    @get:Column
    open var progress: Int? = null

    @Field(analyze = Analyze.NO, bridge = FieldBridge(impl = IntegerBridge::class))
    @PropertyInfo(i18nKey = "task.maxHours")
    @get:Column(name = "max_hours")
    open var maxHours: Int? = null

    /**
     * @see org.projectforge.business.gantt.GanttTask.getStartDate
     */
    @Deprecated("Properties of Gantt diagram will be refactored some day.")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @PropertyInfo(i18nKey = "gantt.startDate")
    @get:Column(name = "start_date")
    open var startDate: Date? = null

    /**
     * @see org.projectforge.business.gantt.GanttTask.getEndDate
     */
    @Deprecated("Properties of Gantt diagram will be refactored some day.")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @PropertyInfo(i18nKey = "gantt.endDate")
    @get:Column(name = "end_date")
    open var endDate: Date? = null

    /**
     * Duration in days.
     *
     * @see org.projectforge.business.gantt.GanttTask.getDuration
     */
    @Deprecated("Properties of Gantt diagram will be refactored some day.")
    @PropertyInfo(i18nKey = "gantt.duration")
    @get:Column(name = "duration", scale = 2, precision = 10)
    open var duration: BigDecimal? = null

    /**
     * Zu diesem Task können keine Zeitberichte mehr eingegeben werden, die vor diesem Datum liegen (z. B. weil bis zu
     * diesem Datum die Zeitberichte bereits berechnet wurden. Nur die Buchhaltung (PF_Finance) kann noch Änderungen
     * vornehmen. Auch können diese Zeitberichte nicht mehr in der Dauer geändert oder gelöscht bzw. außerhalb des Tasks
     * verschoben werden.
     */
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @PropertyInfo(i18nKey = "task.protectTimesheetsUntil")
    @get:Column(name = "protect_timesheets_until")
    open var protectTimesheetsUntil: Date? = null

    @IndexedEmbedded(depth = 1)
    @PropertyInfo(i18nKey = "task.assignedUser")
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "responsible_user_id")
    open var responsibleUser: PFUserDO? = null

    /**
     * Reference is a free use-able field, which will be inherited to all sibling tasks. The reference is exported e. g.
     * in the time sheet MS Excel export.
     */
    @Field
    @PropertyInfo(i18nKey = "task.reference")
    @get:Column(length = REFERENCE_LENGTH)
    open var reference: String? = null

    @PropertyInfo(i18nKey = "task.timesheetBooking")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "timesheet_booking_status", length = 20, nullable = false)
    open var timesheetBookingStatus: TimesheetBookingStatus? = TimesheetBookingStatus.DEFAULT
        get() {
            return field ?: TimesheetBookingStatus.DEFAULT
        }
        set(timesheetBookingStatus) {
            field = timesheetBookingStatus ?: TimesheetBookingStatus.DEFAULT
        }

    /**
     * Whether this list is a black or a white list depends on isBlackList() value. Rules:
     *
     *  * General
     *
     *  * Multiple entries should be separated by comma, semicolon and/or spaces.
     *  * A Kost2 matches if it ends with at least one entry of the list.
     *
     *
     *  * Examples
     *
     *  * "02" matches 5.123.76.02
     *  * "76.02" matches 5.123.76.02
     *  * "7602" does not match 5.123.76.02
     *  * "123.76.02" does not match 5.123.76.02
     *  * "5.123.76.02" does not match 5.123.76.02
     *
     *
     *  * Black list
     *
     *  * Has only an effect if a project is assigned to the task.
     *  * "*" means, that no cost entry matches.
     *  * Every kost2 entry which matches at list one entry will be removed from the kost2 list.
     *
     *
     *  * White list
     *
     *  * If Kost2 entries are assigned (project is given), the only such entries will match, which ends with at least
     * one entry of the list.
     *  * "*" or empty string means, that all cost entry matches.
     *
     *
     *
     */
    @PropertyInfo(i18nKey = "fibu.kost2")
    @get:Column(name = "kost2_black_white_list", length = 1024)
    open var kost2BlackWhiteList: String? = null

    @get:Column(name = "kost2_is_black_list", nullable = false)
    open var kost2IsBlackList: Boolean = false

    /**
     * If set then normal user are not allowed to select (read) the time sheets of other users of this task and all sub
     * tasks. This is important e. g. for hiding the days of illness of an employee.
     */
    @PropertyInfo(i18nKey = "task.protectionOfPrivacy")
    @get:Column(name = "protectionOfPrivacy", nullable = false, columnDefinition = "BOOLEAN DEFAULT 'false'")
    open var protectionOfPrivacy: Boolean = false

    /** -&gt; Gantt  */
    @Deprecated("Properties of Gantt diagram will be refactored some day.")
    @Field
    @PropertyInfo(i18nKey = "task.parentTask")
    @get:Column(name = "workpackage_code", length = 100)
    open var workpackageCode: String? = null

    /**
     * In days.
     */
    @Deprecated("Properties of Gantt diagram will be refactored some day.")
    @PropertyInfo(i18nKey = "task.parentTask")
    @get:Column(name = "gantt_predecessor_offset")
    open var ganttPredecessorOffset: Int? = null

    /** -&gt; Gantt  */
    @Deprecated("Properties of Gantt diagram will be refactored some day.")
    @PropertyInfo(i18nKey = "task.parentTask")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "gantt_rel_type", length = 15)
    open var ganttRelationType: GanttRelationType? = null

    /** -&gt; Gantt  */
    @Deprecated("Properties of Gantt diagram will be refactored some day.")
    @PropertyInfo(i18nKey = "task.parentTask")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "gantt_type", length = 10)
    open var ganttObjectType: GanttObjectType? = null

    /**
     * Please note: if you use TaskTree as cache then note, that the depend-on-task can be out-dated! Get the id of the
     * depend-on-task and get the every-time up-to-date task from the task tree by this id.
     */
    @Deprecated("Properties of Gantt diagram will be refactored some day.")
    @PropertyInfo(i18nKey = "task.parentTask")
    @get:ManyToOne(cascade = [CascadeType.PERSIST, CascadeType.MERGE], targetEntity = TaskDO::class, fetch = FetchType.LAZY)
    @get:JoinColumn(name = "gantt_predecessor_fk")
    open var ganttPredecessor: TaskDO? = null

    /** -&gt; Gantt  */
    @Suppress("DEPRECATION")
    @Deprecated("Properties of Gantt diagram will be refactored some day.")
    val ganttPredecessorId: Int?
        @Transient
        get() = this.ganttPredecessor?.id

    val parentTaskId: Int?
        @Transient
        get() = if (this.parentTask == null) {
            null
        } else this.parentTask!!.id

    val responsibleUserId: Int?
        @Transient
        get() = if (this.responsibleUser == null) {
            null
        } else responsibleUser!!.id

    /**
     * Get the items of the kost2 black white list as string array.
     *
     * @return The items as string array or null, if black white list is null.
     * @see StringHelper.splitAndTrim
     */
    val kost2BlackWhiteItems: Array<String>?
        @Transient
        get() = getKost2BlackWhiteItems(kost2BlackWhiteList)

    @Deprecated("Please use getTitle() instead. getName() should only be used in Groovy scripts.")
    @Transient
    fun getName(): String? {
        return title
    }

    override fun equals(other: Any?): Boolean {
        if (other is TaskDO) {
            return this.parentTaskId == other.parentTaskId && this.title == other.title
        }
        return false
    }

    override fun hashCode(): Int {
        val hcb = HashCodeBuilder()
        hcb.append(this.parentTaskId).append(this.title)
        return hcb.toHashCode()
    }

    @Transient
    override fun getShortDisplayName(): String {
        return this.title + " (#" + this.id + ")"
    }

    /**
     * Used for building read-only clone in ScriptingTaskNode.
     *
     * @see java.lang.Object.clone
     */
    @Suppress("DEPRECATION")
    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        val clone = super.clone() as TaskDO
        if (this.startDate != null) {
            clone.startDate = this.startDate!!.clone() as Date
        }
        if (this.endDate != null) {
            clone.endDate = this.endDate!!.clone() as Date
        }
        if (this.ganttPredecessor != null) {
            clone.ganttPredecessor = TaskDO()
            clone.ganttPredecessor!!.id = this.ganttPredecessorId
        }
        if (this.parentTask != null) {
            clone.parentTask = TaskDO()
            clone.parentTask!!.id = this.parentTaskId
        }
        if (this.protectTimesheetsUntil != null) {
            clone.protectTimesheetsUntil = this.protectTimesheetsUntil!!.clone() as Date
        }
        if (this.responsibleUser != null) {
            clone.responsibleUser = PFUserDO()
            clone.responsibleUser!!.id = responsibleUserId
        }
        return clone
    }

    companion object {
        private const val KOST2_SEPARATOR_CHARS = ",; "

        /**
         * For detecting child tasks of the given parent task with same title.
         */
        const val FIND_OTHER_TASK_BY_PARENTTASKID_AND_TITLE = "TaskDO_FindOtherTaskByParentTaskIdAndTitle"

        const val FIND_BY_PARENTTASKID_AND_TITLE = "TaskDO_FindByParentTaskIdAndTitle"

        const val TITLE_LENGTH = 40

        const val DESCRIPTION_LENGTH = 4000

        const val SHORT_DESCRIPTION_LENGTH = 255

        const val REFERENCE_LENGTH = 1000

        const val PRIORITY_LENGTH = 7

        const val STATUS_LENGTH = 1

        /**
         * Get the items of the kost2 black white list as string array.
         *
         * @return The items as string array or null, if black white list is null.
         * @see StringHelper.splitAndTrim
         */
        @Transient
        fun getKost2BlackWhiteItems(kost2BlackWhiteList: String?): Array<String>? {
            return StringHelper.splitAndTrim(kost2BlackWhiteList, KOST2_SEPARATOR_CHARS)
        }
    }
}

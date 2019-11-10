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

package org.projectforge.framework.access

import de.micromata.genome.db.jpa.history.api.NoHistory
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.ToStringBuilder
import org.hibernate.Hibernate
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.business.task.TaskDO
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.ModificationStatus
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import java.io.Serializable
import java.util.*
import javax.persistence.*

/**
 * Represents an access entry with the permissions of one group to one task. The persistent data object of
 * GroupTaskAccess.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_GROUP_TASK_ACCESS", uniqueConstraints = [UniqueConstraint(columnNames = ["group_id", "task_id"])], indexes = [javax.persistence.Index(name = "idx_fk_t_group_task_access_group_id", columnList = "group_id"), javax.persistence.Index(name = "idx_fk_t_group_task_access_task_id", columnList = "task_id"), javax.persistence.Index(name = "idx_fk_t_group_task_access_tenant_id", columnList = "tenant_id")])
@NamedQueries(
        NamedQuery(name = GroupTaskAccessDO.FIND_BY_TASK_AND_GROUP,
                query = "from GroupTaskAccessDO a where a.task.id=:taskId and a.group.id=:groupId"))
open class GroupTaskAccessDO : DefaultBaseDO() {

    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(cascade = [CascadeType.MERGE])
    @get:JoinColumn(name = "group_id")
    open var group: GroupDO? = null

    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(cascade = [CascadeType.MERGE], targetEntity = TaskDO::class)
    @get:JoinColumn(name = "task_id")
    open var task: TaskDO? = null

    /**
     * If true then the group rights are also valid for all sub tasks. If false, then each sub task needs its own
     * definition.
     */
    @get:Column
    open var isRecursive = true

    @Field
    @get:Column(name = "description", length = 4000)
    open var description: String? = null

    @NoHistory
    @get:OneToMany(cascade = [CascadeType.MERGE, CascadeType.REMOVE], fetch = FetchType.EAGER, orphanRemoval = true)
    @get:JoinColumn(name = "group_task_access_fk", insertable = true, updatable = true)
    open var accessEntries: MutableSet<AccessEntryDO>? = null

    val orderedEntries: List<AccessEntryDO>
        @Transient
        get() {
            val list = ArrayList<AccessEntryDO>()
            var entry = getAccessEntry(AccessType.TASK_ACCESS_MANAGEMENT)
            if (entry != null) {
                list.add(entry)
            }
            entry = getAccessEntry(AccessType.TASKS)
            if (entry != null) {
                list.add(entry)
            }
            entry = getAccessEntry(AccessType.TIMESHEETS)
            if (entry != null) {
                list.add(entry)
            }
            entry = getAccessEntry(AccessType.OWN_TIMESHEETS)
            if (entry != null) {
                list.add(entry)
            }
            return list
        }

    val groupId: Int?
        @Transient
        get() = if (this.group == null) {
            null
        } else this.group!!.id

    val taskId: Int?
        @Transient
        get() = if (this.task == null) {
            null
        } else this.task!!.id

    /**
     * Returns the specified access.
     *
     * @param accessType TASKS_ACCESS, ...
     * @return The specified access or null if not found.
     */
    @Transient
    fun getAccessEntry(accessType: AccessType?): AccessEntryDO? {
        if (this.accessEntries == null) {
            return null
        }
        for (entry in this.accessEntries!!) {
            if (entry.accessType == accessType) {
                return entry
            }
        }
        return null
    }

    @Transient
    fun hasPermission(accessType: AccessType, opType: OperationType): Boolean {
        val entry = getAccessEntry(accessType)
        return entry?.hasPermission(opType) ?: false
    }

    fun addAccessEntry(entry: AccessEntryDO): GroupTaskAccessDO {
        if (this.accessEntries == null) {
            this.accessEntries = HashSet()
        }
        this.accessEntries!!.add(entry)
        return this
    }

    fun ensureAndGetAccessEntry(accessType: AccessType?): AccessEntryDO {
        if (this.accessEntries == null) {
            this.accessEntries = HashSet()
        }
        var entry = getAccessEntry(accessType)
        if (entry == null) {
            entry = AccessEntryDO(accessType!!)
            this.addAccessEntry(entry)
        }
        return entry
    }

    override fun equals(other: Any?): Boolean {
        if (other is GroupTaskAccessDO) {
            val o = other as GroupTaskAccessDO?
            if (this.groupId != o!!.groupId) {
                return false
            }
            return this.taskId == o.taskId
        }
        return false
    }

    override fun hashCode(): Int {
        val hcb = HashCodeBuilder()
        hcb.append(taskId)
        hcb.append(groupId)
        return hcb.toHashCode()
    }

    /**
     * Copies all values from the given src object excluding the values created and modified. Null values will be
     * excluded.
     *
     * @param source
     */
    override fun copyValuesFrom(source: BaseDO<out Serializable>, vararg ignoreFields: String): ModificationStatus {
        var modificationStatus = super.copyValuesFrom(source, *ignoreFields)
        val src = source as GroupTaskAccessDO
        if (src.accessEntries != null) {
            for (srcEntry in src.accessEntries!!) {
                val destEntry = ensureAndGetAccessEntry(srcEntry.accessType)
                val st = destEntry.copyValuesFrom(srcEntry)
                modificationStatus = AbstractBaseDO.getModificationStatus(modificationStatus, st)
            }
            val iterator = accessEntries!!.iterator()
            while (iterator.hasNext()) {
                val destEntry = iterator.next()
                if (src.getAccessEntry(destEntry.accessType) == null) {
                    iterator.remove()
                }
            }
        }
        return modificationStatus
    }

    override fun toString(): String {
        val tos = ToStringBuilder(this)
        tos.append("id", id)
        tos.append("task", taskId)
        tos.append("group", groupId)
        if (Hibernate.isInitialized(this.accessEntries)) {
            tos.append("entries", this.accessEntries)
        } else {
            tos.append("entries", "LazyCollection")
        }
        return tos.toString()
    }

    fun ensureAndGetTasksEntry(): AccessEntryDO {
        return ensureAndGetAccessEntry(AccessType.TASKS)
    }

    fun ensureAndGetAccessManagementEntry(): AccessEntryDO {
        return ensureAndGetAccessEntry(AccessType.TASK_ACCESS_MANAGEMENT)
    }

    fun ensureAndGetTimesheetsEntry(): AccessEntryDO {
        return ensureAndGetAccessEntry(AccessType.TIMESHEETS)
    }

    fun ensureAndGetOwnTimesheetsEntry(): AccessEntryDO {
        return ensureAndGetAccessEntry(AccessType.OWN_TIMESHEETS)
    }

    /**
     * This template clears all access entries.
     */
    fun clear() {
        ensureAndGetAccessManagementEntry().setAccess(false, false, false, false)
        ensureAndGetTasksEntry().setAccess(false, false, false, false)
        ensureAndGetOwnTimesheetsEntry().setAccess(false, false, false, false)
        ensureAndGetTimesheetsEntry().setAccess(false, false, false, false)
    }

    /**
     * This template is used as default for guests (they have only read access to tasks).
     */
    fun guest() {
        ensureAndGetAccessManagementEntry().setAccess(false, false, false, false)
        ensureAndGetTasksEntry().setAccess(true, false, false, false)
        ensureAndGetOwnTimesheetsEntry().setAccess(false, false, false, false)
        ensureAndGetTimesheetsEntry().setAccess(false, false, false, false)
    }

    /**
     * This template is used as default for employees. The have read access to the access management, full access to tasks
     * and own time sheets and only read-access to foreign time sheets.
     */
    fun employee() {
        ensureAndGetAccessManagementEntry().setAccess(true, false, false, false)
        ensureAndGetTasksEntry().setAccess(true, true, true, true)
        ensureAndGetOwnTimesheetsEntry().setAccess(true, true, true, true)
        ensureAndGetTimesheetsEntry().setAccess(true, false, false, false)
    }

    /**
     * This template is used as default for project managers. Same as employee but with full read-write-access to foreign
     * time-sheets.
     */
    fun leader() {
        ensureAndGetAccessManagementEntry().setAccess(true, false, false, false)
        ensureAndGetTasksEntry().setAccess(true, true, true, true)
        ensureAndGetOwnTimesheetsEntry().setAccess(true, true, true, true)
        ensureAndGetTimesheetsEntry().setAccess(true, true, true, true)
    }

    /**
     * This template is used as default for admins. Full access to access management, task, own and foreign time-sheets.
     */
    fun administrator() {
        ensureAndGetAccessManagementEntry().setAccess(true, true, true, true)
        ensureAndGetTasksEntry().setAccess(true, true, true, true)
        ensureAndGetOwnTimesheetsEntry().setAccess(true, true, true, true)
        ensureAndGetTimesheetsEntry().setAccess(true, true, true, true)
    }

    companion object {
        /**
         * from GroupTaskAccessDO a where a.task.id=:taskId and a.group.id=:groupId
         */
        internal const val FIND_BY_TASK_AND_GROUP = "GroupTaskAccessDO_FindByTaskAndGroup"
    }
}

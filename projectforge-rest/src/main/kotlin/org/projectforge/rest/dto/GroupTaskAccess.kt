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

package org.projectforge.rest.dto

import org.projectforge.framework.access.AccessEntryDO
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.access.GroupTaskAccessDO
import org.projectforge.business.task.TaskDO
import org.projectforge.framework.persistence.user.entities.GroupDO

/**
 * DTO of [GroupTaskAccessDO], the access rights of one group on one task.
 *
 * The DO serializes [GroupTaskAccessDO.group] and [GroupTaskAccessDO.task] id-only (`IdOnlySerializer`),
 * which would leave the group and task autocompletes of the hand built next page blank. The DTO carries
 * them as full [Group]/[Task] references (id + display name) instead, and normalizes the permission matrix
 * into an ordered list of [AccessEntry] the frontend can bind checkboxes to.
 */
class GroupTaskAccess(
    id: Long? = null,
    displayName: String? = null,
    var group: Group? = null,
    var task: Task? = null,
    var recursive: Boolean = true,
    var description: String? = null,
    /**
     * The four access entries in the order [GroupTaskAccessDO.orderedEntries] uses
     * (TASK_ACCESS_MANAGEMENT, TASKS, TIMESHEETS, OWN_TIMESHEETS). The frontend normalizes missing types
     * to all-false rows, so a partially filled entity still shows the full matrix.
     */
    var accessEntries: MutableList<AccessEntry>? = null,
) : BaseDTODisplayObject<GroupTaskAccessDO>(id = id, displayName = displayName), EntityAccessSupport {

    override var writeAccess: Boolean? = null
    override var deleteAccess: Boolean? = null

    override fun copyFrom(src: GroupTaskAccessDO) {
        // super copies id, deleted, recursive, description and the audit fields by name; the collection and
        // the two references are copied here (BaseDTO.copy skips collections and cannot map GroupDO -> Group
        // with a display name).
        super.copyFrom(src)
        group = src.group?.let { Group().apply { copyFromMinimal(it) } }
        task = src.task?.let { Task().apply { copyFromMinimal(it) } }
        accessEntries = src.orderedEntries.map { AccessEntry(it) }.toMutableList()
    }

    override fun copyTo(dest: GroupTaskAccessDO) {
        super.copyTo(dest)
        // Both associations are stored by id; build a minimal DO rather than resolving the whole entity.
        dest.group = group?.id?.let { GroupDO().apply { id = it } }
        dest.task = task?.id?.let { TaskDO().apply { id = it } }
        // Rebuild the matrix as a set of AccessEntryDO; GroupTaskAccessDO.copyValuesFrom later merges these
        // into the persisted entity by accessType and drops any type not present here.
        dest.accessEntries = accessEntries
            ?.filter { it.accessType != null }
            ?.map { it.toDO() }
            ?.toMutableSet()
    }
}

/**
 * A single row of the permission matrix — one [AccessType] with its four operation flags. A plain holder,
 * not a [BaseDTO]: [AccessEntryDO] is owned by its parent and has no lifecycle of its own on the wire.
 */
class AccessEntry(
    var accessType: AccessType? = null,
    var accessSelect: Boolean = false,
    var accessInsert: Boolean = false,
    var accessUpdate: Boolean = false,
    var accessDelete: Boolean = false,
) {
    constructor(src: AccessEntryDO) : this(
        accessType = src.accessType,
        accessSelect = src.accessSelect,
        accessInsert = src.accessInsert,
        accessUpdate = src.accessUpdate,
        accessDelete = src.accessDelete,
    )

    fun toDO(): AccessEntryDO {
        val entry = AccessEntryDO(accessType!!)
        entry.setAccess(accessSelect, accessInsert, accessUpdate, accessDelete)
        return entry
    }
}

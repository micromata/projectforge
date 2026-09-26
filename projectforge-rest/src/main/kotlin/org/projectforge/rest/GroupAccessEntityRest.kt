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

package org.projectforge.rest

import org.projectforge.business.PfCaches
import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.GroupTaskAccessDO
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.dto.GroupTaskAccess
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The access-rights list and edit page (`access.page.tsx` in projectforge-next) — the group/task
 * permission matrix, migrated from the Wicket `AccessListPage`/`AccessEditPage`.
 *
 * Layout free ([AbstractDTOEntityRest]): the hand built next page brings its own layout, so there is no
 * server side `createListLayout`/`createEditLayout` here. The way-back link to the classic Wicket page
 * is driven by `NextMigration` (offerLegacyLink), not by `classicsLinkListUrl`.
 */
@RestController
@RequestMapping("${Rest.URL}/access")
class GroupAccessEntityRest :
    AbstractDTOEntityRest<GroupTaskAccessDO, GroupTaskAccess, AccessDao>(AccessDao::class.java, "access.title") {

    @Autowired
    private lateinit var caches: PfCaches

    override fun transformFromDB(obj: GroupTaskAccessDO, editMode: Boolean): GroupTaskAccess {
        // Resolve the id-only group and task from the caches so the DTO carries their display names for the
        // list columns and the autocompletes (the DO serializes both id-only, see GroupTaskAccessDO).
        obj.group = caches.getGroup(obj.groupId) ?: obj.group
        obj.task = caches.getTask(obj.taskId) ?: obj.task
        val dto = GroupTaskAccess()
        dto.copyFrom(obj)
        return dto
    }

    override fun transformForDB(dto: GroupTaskAccess): GroupTaskAccessDO {
        val obj = GroupTaskAccessDO()
        dto.copyTo(obj)
        return obj
    }
}

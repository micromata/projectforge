/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.GroupTaskAccessDO
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDOPagesRest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/access")
class GroupAccessPagesRest : AbstractDOPagesRest<GroupTaskAccessDO, AccessDao>(AccessDao::class.java, "access.title") {
    /**
     * Initializes new memos for adding.
     */
    override fun newBaseDO(request: HttpServletRequest?): GroupTaskAccessDO {
        val groupTaskAccess = super.newBaseDO(request)
        return groupTaskAccess
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(request: HttpServletRequest, magicFilter: MagicFilter): UILayout {
        val layout = super.createListLayout(request, magicFilter)
                .add(UITable.createUIResultSetTable()
                        .add(lc, "task.title", "group.name", "isRecursive", "description"))
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: GroupTaskAccessDO, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
                .add(lc, "task")
                .add(UISelect.createGroupSelect(lc, "readonlyAccessUsers", false, "user.assignedGroups"))
                .add(lc, "isRecursive")
                .add(UICustomized("access.table"))
                .add(lc, "description")
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}

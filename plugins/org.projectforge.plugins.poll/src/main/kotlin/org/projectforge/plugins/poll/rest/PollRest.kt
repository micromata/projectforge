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

package org.projectforge.plugins.poll.rest

import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.plugins.poll.PollDO
import org.projectforge.plugins.poll.PollDao
import org.projectforge.plugins.poll.dto.Poll
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/poll")
class PollRest : AbstractDTORest<PollDO, Poll, PollDao, BaseSearchFilter>(PollDao::class.java, BaseSearchFilter::class.java, "plugins.poll.title") {

    override fun transformFromDB(obj: PollDO, editMode: Boolean): Poll {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun transformForDB(dto: Poll): PollDO {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Initializes new polls for adding.
     */
    override fun newBaseDO(request: HttpServletRequest?): PollDO {
        val poll = super.newBaseDO(request)
        poll.owner = ThreadLocalUserContext.getUser()
        return poll
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "title", "description", "location", "owner", "lastUpdate"))
        layout.getTableColumnById("owner").formatter = Formatter.USER
        layout.getTableColumnById("lastUpdate").formatter = Formatter.TIMESTAMP_MINUTES
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Poll): UILayout {
        val location = UIInput("location", lc).enableAutoCompletion(this)
        val layout = super.createEditLayout(dto)
                .add(lc, "title")
                .add(location)
                .add(lc, "description")
                .add(UISelect<Int>("assignedItems", lc,
                        multi = true,
                        label = "plugins.poll.attendee.users",
                        //additionalLabel = "access.users",
                        autoCompletion = AutoCompletion<Int>(url = "user/aco"),
                        labelProperty = "fullname",
                        valueProperty = "id"))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}

/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.orga

import jakarta.annotation.PostConstruct
import org.projectforge.business.user.UserRightId
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.history.DisplayHistoryEntryAttr
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.HistoryLoadContext
import org.projectforge.framework.persistence.history.PropertyOpType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class VisitorbookDao protected constructor() : BaseDao<VisitorbookDO>(VisitorbookDO::class.java) {
    @Autowired
    private lateinit var visitorbookService: VisitorbookService

    init {
        userRightId = USER_RIGHT_ID
    }

    @PostConstruct
    private fun postConstruct() {
        visitorbookService.visitorbookDao = this
    }

    override fun select(filter: BaseSearchFilter): List<VisitorbookDO> {
        val myFilter = if (filter is VisitorbookFilter) {
            filter
        } else {
            VisitorbookFilter(filter)
        }

        val queryFilter = createQueryFilter(myFilter)
        val resultList = select(queryFilter)
        return resultList
    }

    override fun getHistoryPropertyPrefix(context: HistoryLoadContext): String? {
        val entry = context.requiredHistoryEntry
        val item = context.findLoadedEntity(entry)
        return if (item is VisitorbookEntryDO) {
            item.dateOfVisit?.toString() ?: "???"
        } else {
            null
        }
    }

    override fun newInstance(): VisitorbookDO {
        return VisitorbookDO()
    }

    companion object {
        val USER_RIGHT_ID: UserRightId = UserRightId.ORGA_VISITORBOOK
    }
}

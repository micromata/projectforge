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

import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.IDao
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class VisitorbookService : IDao<VisitorbookDO?> {
    @Autowired
    private lateinit var visitorbookDao: VisitorbookDao

    override fun select(filter: BaseSearchFilter): List<VisitorbookDO> {
        return visitorbookDao.select(filter)
    }

    override fun isHistorizable(): Boolean {
        return true
    }

    override fun hasInsertAccess(user: PFUserDO): Boolean {
        return visitorbookDao.hasInsertAccess(user)
    }

    fun getAssignedContactPersonsIds(data: VisitorbookDO?): List<Long> {
        val assignedContactPersons = mutableListOf<Long>()
        data?.contactPersons?.forEach { employee ->
            employee.id?.let { assignedContactPersons.add(it) }
        }
        return assignedContactPersons
    }

    fun addNewEntry(visitorBook: VisitorbookDO): VisitorbookEntryDO {
        val entry = VisitorbookEntryDO()
        entry.visitorbook = visitorBook
        visitorBook.addEntry(entry)
        return entry
    }
}

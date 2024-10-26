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

import org.projectforge.framework.persistence.api.BaseDOPersistenceService
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.api.IDao
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class VisitorbookService : IDao<VisitorbookDO?> {
    @Autowired
    private lateinit var baseDOPersistenceService: BaseDOPersistenceService

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var visitorbookCache: VisitorbookCache

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

    /**
     * @param id The id of the attribute to select.
     * @param expectedType If not null, the type of the attribute must match this type, otherwise, an exception will be thrown.
     * @param checkAccess If true, the logged-in user must have access to the attribute.
     * @return The attribute with the given id or null if it does not exist.
     */
    fun findVisitorbookEntry(
        id: Long?,
        checkAccess: Boolean = true
    ): VisitorbookEntryDO? {
        id ?: return null
        if (checkAccess) {
            visitorbookDao.checkLoggedInUserSelectAccess()
        }
        val result = persistenceService.find(VisitorbookEntryDO::class.java, id) ?: return null
        return result
    }

    /**
     * @param visitorbook The visitorbook to select the attribute for.
     * @param deleted If true, only deleted entries will be returned, if false, only not deleted entries will be returned. If null, deleted and not deleted entries will be returned.
     */
    fun selectAllVisitorbookEntries(
        visitorbookId: Long,
        deleted: Boolean? = false,
        checkAccess: Boolean = true,
    ): List<VisitorbookEntryDO> {
        val visitorbook = visitorbookDao.find(visitorbookId)!!
        return selectAllVisitorbookEntries(visitorbook, deleted, checkAccess)
    }

    /**
     * @param visitorbook The visitorbook to select the attribute for.
     * @param deleted If true, only deleted entries will be returned, if false, only not deleted entries will be returned. If null, deleted and not deleted entries will be returned.
     */
    fun selectAllVisitorbookEntries(
        visitorbook: VisitorbookDO,
        deleted: Boolean? = false,
        checkAccess: Boolean = true,
    ): List<VisitorbookEntryDO> {
        requireNotNull(visitorbook.id) { "Visitorbook id must not be null." }
        if (checkAccess) {
            visitorbookDao.checkLoggedInUserSelectAccess(visitorbook)
        }
        val list = persistenceService.executeQuery(
            "from VisitorbookEntryDO t where t.visitorbook.id = :visitorbookId order by t.dateOfVisit desc",
            VisitorbookEntryDO::class.java,
            Pair("visitorbookId", visitorbook.id),
        )
        if (deleted != null) {
            return list.filter { it.deleted == deleted }
        }
        return list
    }


    fun insert(
        visitorbookId: Long,
        entry: VisitorbookEntryDO,
        checkAccess: Boolean = true,
    ): Long? {
        val visitor = visitorbookDao.find(visitorbookId)!!
        return insert(visitor, entry, checkAccess)
    }

    fun insert(
        visitorbook: VisitorbookDO,
        entry: VisitorbookEntryDO,
        checkAccess: Boolean = true,
    ): Long? {
        if (visitorbook.id != entry.visitorbook?.id) {
            throw IllegalArgumentException("Visitorbook id of entry does not match visitorbook id.")
        }
        if (checkAccess) {
            visitorbookDao.checkLoggedInUserInsertAccess(visitorbook)
        }
        val result = baseDOPersistenceService.insert(entry, checkAccess = checkAccess)
        visitorbookCache.setExpired(visitorbook.id)
        return result
    }

    internal fun insertVisitorbookEntry(
        visitorbook: VisitorbookDO,
        dateOfVisit: LocalDate,
        arrived: String,
        departed: String,
        checkAccess: Boolean,
    ): VisitorbookEntryDO {
        val entry = VisitorbookEntryDO()
        entry.visitorbook = visitorbook
        entry.dateOfVisit = dateOfVisit
        entry.arrived = arrived
        entry.departed = departed
        entry.created = Date()
        entry.lastUpdate = entry.created
        val id = insert(visitorbook, entry, checkAccess)
        entry.id = id // Should already be set by insert (but better safe than sorry).
        return entry
    }

    /**
     * @param visitorbookId The visitorbook (by id) to update the entry for. Needed for checkAccess.
     * @param entry: The entry to update.
     * @param checkAccess: If true, the logged-in user must have update access to the visitorbook.
     */
    fun updateVisitorbookEntry(
        visitorbookId: Long?,
        entry: VisitorbookEntryDO,
        checkAccess: Boolean = true,
    ): EntityCopyStatus {
        val visitorbook = visitorbookDao.find(visitorbookId)!!
        return updateVisitorbookEntry(visitorbook, entry, checkAccess)
    }


    /**
     * @param visitorbook The visitorbook to update the entry for. Needed for checkAccess.
     * @param entry: The entry to update.
     * @param checkAccess: If true, the logged-in user must have update access to the visitorbook.
     */
    fun updateVisitorbookEntry(
        visitorbook: VisitorbookDO,
        entry: VisitorbookEntryDO,
        checkAccess: Boolean = true,
    ): EntityCopyStatus {
        if (visitorbook.id != entry.visitorbook?.id) {
            throw IllegalArgumentException("Visitorbook id of entry does not match visitorbook id.")
        }
        if (checkAccess) {
            visitorbookDao.checkLoggedInUserUpdateAccess(visitorbook, visitorbook)
        }
        val result = baseDOPersistenceService.update(entry, checkAccess = checkAccess)
        visitorbookCache.setExpired(visitorbook.id)
        return result
    }

    fun markVisitorbookEntryAsDeleted(
        visitorbookId: Long?,
        entryId: Long?,
        checkAccess: Boolean = true,
    ) {
        val visitorbook = visitorbookDao.find(visitorbookId)!!
        val entry = findVisitorbookEntry(entryId, checkAccess = checkAccess)!!
        markVisitorbookEntryAsDeleted(visitorbook, entry, checkAccess)
        visitorbookCache.setExpired(visitorbook.id)
    }

    fun markVisitorbookEntryAsDeleted(
        visitorbook: VisitorbookDO,
        entry: VisitorbookEntryDO,
        checkAccess: Boolean = true,
    ) {
        require(entry.visitorbook!!.id == visitorbook.id!!) { "Visitorbook id of entry does not match visitorbook id." }
        if (checkAccess) {
            visitorbookDao.checkLoggedInUserUpdateAccess(visitorbook, visitorbook)
        }
        baseDOPersistenceService.markAsDeleted(obj = entry, checkAccess = checkAccess)
        visitorbookCache.setExpired(visitorbook.id)
    }

    fun undeleteVisitorbookEntry(
        visitorbook: VisitorbookDO,
        entry: VisitorbookEntryDO,
        checkAccess: Boolean = true,
    ) {
        if (checkAccess) {
            visitorbookDao.checkLoggedInUserUpdateAccess(visitorbook, visitorbook)
        }
        baseDOPersistenceService.undelete(obj = entry, checkAccess = checkAccess)
        visitorbookCache.setExpired(visitorbook.id)
    }
}

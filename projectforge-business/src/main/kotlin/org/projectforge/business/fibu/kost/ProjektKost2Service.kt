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

package org.projectforge.business.fibu.kost

import org.projectforge.business.fibu.ProjektDao
import org.projectforge.common.i18n.MessageParam
import org.projectforge.common.i18n.MessageParamType
import org.projectforge.common.i18n.UserException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Creates, activates and deactivates the cost 2 units (Kost2) of a project by their cost 2 type (Kost2Art) —
 * shared by the project edit form and the project mass update.
 *
 * A unit is never deleted here: unchecking a type sets its unit non-active, the time sheets and invoices booked
 * on it keep it, only new time sheets can't be booked on it any more ([KostCache.getActiveKost2]).
 *
 * Read from the [KostCache] but written through [Kost2Dao] on a freshly loaded object (rights, history, cache).
 */
@Service
open class ProjektKost2Service {
    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var projektDao: ProjektDao

    /**
     * Gives the project an active cost 2 unit of every given type: a new one, a deleted one is undeleted
     * (inserting would collide with its number), a non-active or ended one is activated again.
     *
     * @return true if any unit was created or changed.
     * @throws UserException if a unit has to be created but the project has no range (bereich): neither a
     * customer nor an internal range (internKost2_4) — the number of a cost 2 unit is made of it.
     */
    open fun activate(projektId: Long, artIds: Collection<Long>): Boolean {
        val kost2ByArtId = kost2ByArtId(projektId)
        var changed = false
        artIds.forEach { artId ->
            val cached = kost2ByArtId[artId]
            if (cached == null) {
                if (projektDao.findOrLoad(projektId)?.bereich == null) {
                    throw UserException(
                        "validation.error.fieldRequired",
                        MessageParam("fibu.projekt.internKost2_4", MessageParamType.I18N_KEY),
                    )
                }
                val kost2 = Kost2DO()
                kost2Dao.setProjekt(kost2, projektId)
                kost2Dao.setKost2Art(kost2, artId)
                kost2Dao.insert(kost2)
                changed = true
                return@forEach
            }
            val kost2 = kost2Dao.find(cached.id) ?: return@forEach
            val activate = !isActive(kost2)
            if (activate) {
                kost2.kostentraegerStatus = KostentraegerStatus.ACTIVE
            }
            if (kost2.deleted) {
                kost2Dao.undelete(kost2) // Takes the status change along.
                changed = true
            } else if (activate) {
                kost2Dao.update(kost2)
                changed = true
            }
        }
        return changed
    }

    /**
     * Sets the project's active cost 2 unit of every given type non-active. A type the project has no active
     * unit of is left alone (no-op).
     *
     * @return true if any unit was changed.
     */
    open fun deactivate(projektId: Long, artIds: Collection<Long>): Boolean {
        val kost2ByArtId = kost2ByArtId(projektId)
        var changed = false
        artIds.forEach { artId ->
            val cached = kost2ByArtId[artId] ?: return@forEach
            if (cached.deleted || !isActive(cached)) {
                return@forEach
            }
            val kost2 = kost2Dao.find(cached.id) ?: return@forEach
            kost2.kostentraegerStatus = KostentraegerStatus.NONACTIVE
            kost2Dao.update(kost2)
            changed = true
        }
        return changed
    }

    /** The cost 2 unit's own status, not the effective one (an ended project ends all its units anyway). */
    open fun isActive(kost2: Kost2DO): Boolean = KostCache.isActive(kost2)

    private fun kost2ByArtId(projektId: Long): Map<Long, Kost2DO> {
        return kostCache.getKost2ForProjekt(projektId, includeDeleted = true)
            .filter { it.kost2Art?.id != null }
            // A deleted unit only counts if there is no other one of the same type (there shouldn't be).
            .sortedBy { !it.deleted }
            .associateBy { it.kost2Art!!.id!! }
    }
}

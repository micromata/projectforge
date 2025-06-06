/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.KostHelper.parseKostString
import org.projectforge.business.user.UserRightId
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isNull
import org.projectforge.framework.persistence.api.QueryFilter.Companion.ne
import org.projectforge.framework.persistence.api.QueryFilter.Companion.or
import org.projectforge.framework.persistence.api.SortProperty.Companion.asc
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
open class Kost2Dao : BaseDao<Kost2DO>(Kost2DO::class.java) {
    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var kost2ArtDao: Kost2ArtDao

    /**
     * @return the kostCache
     */
    @Autowired
    private lateinit var kostCache: KostCache

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    init {
        userRightId = USER_RIGHT_ID
    }

    /**
     * @param kost2
     * @param projektId If null, then projekt will be set to null;
     * @see BaseDao.findOrLoad
     */
    fun setProjekt(kost2: Kost2DO, projektId: Long) {
        val projekt = projektDao.findOrLoad(projektId)
        if (projekt != null) {
            kost2.projekt = projekt
            kost2.nummernkreis = projekt.nummernkreis
            kost2.bereich = projekt.bereich!!
            kost2.teilbereich = projekt.nummer
        }
    }

    /**
     * @param kost2
     * @param kost2ArtId If null, then kost2Art will be set to null;
     * @see BaseDao.findOrLoad
     */
    fun setKost2Art(kost2: Kost2DO, kost2ArtId: Long) {
        val kost2Art = kost2ArtDao.findOrLoad(kost2ArtId)
        kost2.kost2Art = kost2Art
    }

    /**
     * @param kostString Format ######## or #.###.##.## is supported.
     * @see .getKost2
     */
    fun getKost2(kostString: String?): Kost2DO? {
        val kost = parseKostString(kostString) ?: return null
        return getKost2(kost[0], kost[1], kost[2], kost[3].toLong())
    }

    fun getKost2(nummernkreis: Int, bereich: Int, teilbereich: Int, kost2Art: Long): Kost2DO? {
        return persistenceService.selectNamedSingleResult(
            Kost2DO.FIND_BY_NK_BEREICH_TEILBEREICH_KOST2ART,
            Kost2DO::class.java,
            Pair("nummernkreis", nummernkreis),
            Pair("bereich", bereich),
            Pair("teilbereich", teilbereich),
            Pair("kost2ArtId", kost2Art),
        )
    }

    fun getActiveKost2(nummernkreis: Int, bereich: Int, teilbereich: Int): List<Kost2DO> {
        return persistenceService.executeNamedQuery(
            Kost2DO.FIND_ACTIVES_BY_NK_BEREICH_TEILBEREICH,
            Kost2DO::class.java,
            Pair("nummernkreis", nummernkreis),
            Pair("bereich", bereich),
            Pair("teilbereich", teilbereich),
        )
    }

    /**
     * @param projekt
     * @see .getActiveKost2
     */
    fun getActiveKost2(projekt: ProjektDO?): List<Kost2DO>? {
        if (projekt == null) {
            return null
        }
        return getActiveKost2(projekt.nummernkreis, projekt.bereich!!, projekt.teilbereich)
    }

    override fun select(filter: BaseSearchFilter): List<Kost2DO> {
        val myFilter = if (filter is KostFilter) {
            filter
        } else {
            KostFilter(filter)
        }
        val queryFilter = QueryFilter(myFilter)
        queryFilter.createJoin("kost2Art")
        if (myFilter.isActive) {
            queryFilter.add(eq("kostentraegerStatus", KostentraegerStatus.ACTIVE))
        } else if (myFilter.isNonActive) {
            queryFilter.add(eq("kostentraegerStatus", KostentraegerStatus.NONACTIVE))
        } else if (myFilter.isEnded) {
            queryFilter.add(eq("kostentraegerStatus", KostentraegerStatus.ENDED))
        } else if (myFilter.isNotEnded) {
            queryFilter.add(
                or(
                    ne("kostentraegerStatus", KostentraegerStatus.ENDED),
                    isNull("kostentraegerStatus")
                )
            )
        }
        queryFilter.addOrder(asc("nummernkreis")).addOrder(asc("bereich")).addOrder(asc("teilbereich"))
            .addOrder(asc("kost2Art.id"))
        return select(queryFilter)
    }

    override fun onInsertOrModify(obj: Kost2DO, operationType: OperationType) {
        if (obj.projekt?.id != null) {
            // Projekt ist gegeben. Dann müssen auch die Ziffern stimmen:
            val projekt =
                projektDao.find(obj.projekt?.id, checkAccess = false) // Bei Neuanlage ist Projekt nicht wirklich gebunden.
            if (projekt!!.nummernkreis != obj.nummernkreis || projekt.bereich != obj.bereich || projekt.nummer != obj.teilbereich) {
                throw UserException(
                    ("Inkonsistenz bei Kost2: "
                            + obj.nummernkreis
                            + "."
                            + obj.bereich
                            + "."
                            + obj.teilbereich
                            + " != "
                            + projekt.nummernkreis
                            + "."
                            + projekt.bereich
                            + "."
                            + projekt.nummer
                            + " (Projekt)")
                )
            }
        } else if (obj.nummernkreis == 4 || obj.nummernkreis == 5) {
            throw UserException("fibu.kost2.error.projektNeededForNummernkreis")
        }
        val other = if (obj.id == null) {
            // New kost entry
            getKost2(obj.nummernkreis, obj.bereich, obj.teilbereich, obj.kost2Art?.id!!)
        } else {
            // kost entry already exists. Check maybe changed:
            persistenceService.selectNamedSingleResult(
                Kost2DO.FIND_OTHER_BY_NK_BEREICH_TEILBEREICH_KOST2ART,
                Kost2DO::class.java,
                Pair("nummernkreis", obj.nummernkreis),
                Pair("bereich", obj.bereich),
                Pair("teilbereich", obj.teilbereich),
                Pair("kost2ArtId", obj.kost2Art?.id),
                Pair("id", obj.id),
            )
        }
        if (other != null) {
            throw UserException("fibu.kost.error.collision")
        }
    }

    override fun afterInsertOrModify(obj: Kost2DO, operationType: OperationType) {
        kostCache.updateKost2(obj)
    }

    override fun newInstance(): Kost2DO {
        return Kost2DO()
    }

    companion object {
        val USER_RIGHT_ID: UserRightId = UserRightId.FIBU_COST_UNIT

        val ADDITIONAL_SEARCH_FIELDS = arrayOf("projekt.name", "projekt.kunde.name")
    }
}

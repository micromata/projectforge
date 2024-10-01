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

package org.projectforge.business.fibu.kost

import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.KostHelper.parseKostString
import org.projectforge.business.user.UserRightId
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isNull
import org.projectforge.framework.persistence.api.QueryFilter.Companion.ne
import org.projectforge.framework.persistence.api.QueryFilter.Companion.or
import org.projectforge.framework.persistence.api.SortProperty.Companion.asc
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
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
     * @see BaseDao.getOrLoad
     */
    fun setProjekt(kost2: Kost2DO, projektId: Long) {
        return persistenceService.runReadOnly { context ->
            setProjekt(kost2, projektId, context)
        }
    }

    /**
     * @param kost2
     * @param projektId If null, then projekt will be set to null;
     * @see BaseDao.getOrLoad
     */
    fun setProjekt(kost2: Kost2DO, projektId: Long, context: PfPersistenceContext) {
        val projekt = projektDao.getOrLoad(projektId, context)
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
     * @see BaseDao.getOrLoad
     */
    fun setKost2Art(kost2: Kost2DO, kost2ArtId: Long) {
        persistenceService.runReadOnly { context ->
            setKost2Art(kost2, kost2ArtId, context)
        }
    }

    /**
     * @param kost2
     * @param kost2ArtId If null, then kost2Art will be set to null;
     * @see BaseDao.getOrLoad
     */
    fun setKost2Art(kost2: Kost2DO, kost2ArtId: Long, context: PfPersistenceContext) {
        val kost2Art = kost2ArtDao.getOrLoad(kost2ArtId, context)
        kost2.kost2Art = kost2Art
    }

    /**
     * @param kostString Format ######## or #.###.##.## is supported.
     * @see .getKost2
     */
    fun getKost2(kostString: String?): Kost2DO? {
        return persistenceService.runReadOnly { context ->
            getKost2(kostString, context)
        }
    }

    /**
     * @param kostString Format ######## or #.###.##.## is supported.
     * @see .getKost2
     */
    fun getKost2(kostString: String?, context: PfPersistenceContext): Kost2DO? {
        val kost = parseKostString(kostString) ?: return null
        return getKost2(kost[0], kost[1], kost[2], kost[3].toLong(), context)
    }

    fun getKost2(nummernkreis: Int, bereich: Int, teilbereich: Int, kost2Art: Long): Kost2DO? {
        return persistenceService.runReadOnly { context ->
            getKost2(nummernkreis, bereich, teilbereich, kost2Art, context)
        }
    }

    fun getKost2(nummernkreis: Int, bereich: Int, teilbereich: Int, kost2Art: Long, context: PfPersistenceContext): Kost2DO? {
        return context.selectNamedSingleResult(
            Kost2DO.FIND_BY_NK_BEREICH_TEILBEREICH_KOST2ART,
            Kost2DO::class.java,
            Pair("nummernkreis", nummernkreis),
            Pair("bereich", bereich),
            Pair("teilbereich", teilbereich),
            Pair("kost2ArtId", kost2Art),
        )
    }

    fun getActiveKost2(nummernkreis: Int, bereich: Int, teilbereich: Int): List<Kost2DO> {
        return persistenceService.namedQuery(
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

    override fun getList(filter: BaseSearchFilter, context: PfPersistenceContext): List<Kost2DO> {
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
        return getList(queryFilter, context)
    }

    override fun onSaveOrModify(obj: Kost2DO, context: PfPersistenceContext) {
        if (obj.projektId != null) {
            // Projekt ist gegeben. Dann müssen auch die Ziffern stimmen:
            val projekt =
                projektDao.getById(obj.projektId, context) // Bei Neuanlage ist Projekt nicht wirklich gebunden.
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
        var other: Kost2DO? = null
        if (obj.id == null) {
            // New kost entry
            other = getKost2(obj.nummernkreis, obj.bereich, obj.teilbereich, obj.kost2ArtId!!, context)
        } else {
            // kost entry already exists. Check maybe changed:
            other = persistenceService.selectNamedSingleResult(
                Kost2DO.FIND_OTHER_BY_NK_BEREICH_TEILBEREICH_KOST2ART,
                Kost2DO::class.java,
                Pair("nummernkreis", obj.nummernkreis),
                Pair("bereich", obj.bereich),
                Pair("teilbereich", obj.teilbereich),
                Pair("kost2ArtId", obj.kost2ArtId),
                Pair("id", obj.id),
            )
        }
        if (other != null) {
            throw UserException("fibu.kost.error.collision")
        }
    }

    override fun afterSaveOrModify(obj: Kost2DO, context: PfPersistenceContext) {
        super.afterSaveOrModify(obj, context)
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

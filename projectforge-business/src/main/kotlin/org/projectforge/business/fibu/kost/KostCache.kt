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

import jakarta.persistence.LockModeType
import org.apache.commons.collections4.CollectionUtils
import org.projectforge.business.fibu.kost.KostHelper.parseKostString
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.utils.NumberHelper.greaterZero
import org.projectforge.reporting.Kost2Art
import org.projectforge.reporting.impl.Kost2ArtImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

/**
 * The kost2 entries will be cached.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
class KostCache : AbstractCache() {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /**
     * The key is the kost2-id.
     */
    private var kost2Map: MutableMap<Int?, Kost2DO>? = null

    /**
     * The key is the kost2-id.
     */
    private var kost1Map: MutableMap<Int?, Kost1DO>? = null

    private var allKost2Arts: List<Kost2Art>? = null

    private var kost2EntriesExists = false

    fun getKost2(kost2Id: Int?): Kost2DO? {
        if (!greaterZero(kost2Id)) {
            return null
        }
        return getKost2Map()!![kost2Id]
    }

    /**
     * @param kostString Format ######## or #.###.##.## is supported.
     * @see .getKost2
     */
    fun getKost2(kostString: String?): Kost2DO? {
        val kost = parseKostString(kostString) ?: return null
        return getKost2(kost[0], kost[1], kost[2], kost[3])
    }

    fun getKost2(nummernkreis: Int, bereich: Int, teilbereich: Int, kost2art: Int): Kost2DO? {
        for (kost in getKost2Map()!!.values) {
            if (kost.nummernkreis == nummernkreis && kost.bereich == bereich && kost.teilbereich == teilbereich && kost.kost2ArtId == kost2art) {
                return kost
            }
        }
        return null
    }

    fun getActiveKost2(nummernkreis: Int, bereich: Int, teilbereich: Int): List<Kost2DO?>? {
        val list: MutableList<Kost2DO?> = ArrayList()
        for (kost in getKost2Map()!!.values) {
            if (kost.nummernkreis == nummernkreis && kost.bereich == bereich && kost.teilbereich == teilbereich && (kost.kostentraegerStatus == KostentraegerStatus.ACTIVE || kost.kostentraegerStatus == null)) {
                list.add(kost)
            }
        }
        if (CollectionUtils.isEmpty(list)) {
            return null
        }
        return list
    }

    fun getKost1(kost1Id: Int?): Kost1DO? {
        if (!greaterZero(kost1Id)) {
            return null
        }
        return getKost1Map()!![kost1Id]
    }

    /**
     * @param kostString Format ######## or #.###.##.## is supported.
     * @see .getKost2
     */
    fun getKost1(kostString: String?): Kost1DO? {
        val kost = parseKostString(kostString)
        for (kost1 in getKost1Map()!!.values) {
            if (kost!![0] == kost1.nummernkreis && kost[1] == kost1.bereich && kost[2] == kost1.teilbereich && kost[3] == kost1.endziffer) {
                return kost1
            }
        }
        return null
    }

    /**
     * Gibt die für das Projekt definierten, nicht gelöschten Kostenarten zurück.
     *
     * @param projektId
     */
    fun getKost2Arts(projektId: Int?): Set<Kost2ArtDO> {
        checkRefresh()
        val set: MutableSet<Kost2ArtDO> = TreeSet()
        if (projektId == null) {
            return set
        }
        for (kost in getKost2Map()!!.values) {
            if (kost.deleted) {
                continue
            }
            if (projektId == kost.projektId) {
                val kost2Art = kost.kost2Art
                if (kost2Art != null) {
                    set.add(kost2Art)
                }
            }
        }
        return set
    }

    /**
     * Gibt alle nicht gelöschten Kostenarten zurück, wobei die für das Projekt definierten entsprechend markiert sind.
     *
     * @param projektId
     */
    fun getAllKost2Arts(projektId: Int?): List<Kost2Art> {
        checkRefresh()
        val set = getKost2Arts(projektId)
        val result: MutableList<Kost2Art> = ArrayList()
        for (kost2Art in allKost2Arts!!) {
            if (kost2Art.isDeleted) {
                continue
            }
            val kost2ArtDO = Kost2ArtDO()
            kost2ArtDO.copyValuesFrom((kost2Art as Kost2ArtImpl).kost2ArtDO)
            val art = Kost2ArtImpl(kost2ArtDO)
            if (set.contains(kost2Art.kost2ArtDO)) {
                art.isExistsAlready = true
            }
            result.add(art)
        }
        return result
    }

    val allKostArts: List<Kost2Art>
        get() {
            checkRefresh()
            val list: MutableList<Kost2Art> =
                ArrayList()
            if (allKost2Arts != null) {
                for (kost2Art in allKost2Arts!!) {
                    val kost2ArtDO = (kost2Art as Kost2ArtImpl).kost2ArtDO
                    val clone = Kost2ArtDO()
                    clone.copyValuesFrom(kost2ArtDO)
                    list.add(Kost2ArtImpl(clone))
                }
            }
            return list
        }

    fun isKost2EntriesExists(): Boolean {
        checkRefresh()
        return kost2EntriesExists
    }

    /**
     * Should be called after user modifications.
     */
    fun updateKost2(kost2: Kost2DO) {
        getKost2Map()!![kost2.id] = kost2
    }

    /**
     * Should be called after user modifications.
     */
    fun updateKost1(kost1: Kost1DO) {
        getKost1Map()!![kost1.id] = kost1
    }

    fun updateKost2Arts() {
        val result = persistenceService.query(
            "from Kost2ArtDO t where t.deleted = false order by t.id",
            Kost2ArtDO::class.java, lockModeType = LockModeType.NONE
        )
        val list: MutableList<Kost2Art> = ArrayList()
        for (kost2ArtDO in result) {
            val art = Kost2ArtImpl(kost2ArtDO)
            list.add(art)
        }
        // This method must not be synchronized because it works with a new copy of list.
        this.allKost2Arts = list
    }

    private fun getKost2Map(): MutableMap<Int?, Kost2DO>? {
        checkRefresh()
        return kost2Map
    }

    private fun getKost1Map(): MutableMap<Int?, Kost1DO>? {
        checkRefresh()
        return kost1Map
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Initializing KostCache ...")
        // This method must not be synchronized because it works with a new copy of maps.
        val map1: MutableMap<Int?, Kost1DO> = HashMap()
        val list1 = persistenceService.query(
            "from Kost1DO t", Kost1DO::class.java,
            lockModeType = LockModeType.NONE,
        )
        for (kost1 in list1) {
            map1[kost1.id] = kost1
        }
        this.kost1Map = map1
        val map2: MutableMap<Int?, Kost2DO> = HashMap()
        val list2 = persistenceService.query("from Kost2DO t", Kost2DO::class.java, lockModeType = LockModeType.NONE)
        kost2EntriesExists = false
        for (kost2 in list2) {
            if (!kost2EntriesExists && !kost2.deleted) {
                kost2EntriesExists = true
            }
            map2[kost2.id] = kost2
        }
        this.kost2Map = map2
        updateKost2Arts()
        log.info("Initializing of KostCache done.")
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(KostCache::class.java)
    }
}

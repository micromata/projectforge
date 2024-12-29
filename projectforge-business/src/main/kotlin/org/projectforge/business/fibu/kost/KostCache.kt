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

import jakarta.annotation.PostConstruct
import jakarta.persistence.LockModeType
import mu.KotlinLogging
import org.hibernate.Hibernate
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.kost.KostHelper.parseKostString
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.utils.NumberHelper.greaterZero
import org.projectforge.reporting.Kost2Art
import org.projectforge.reporting.impl.Kost2ArtImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * The kost2 entries will be cached.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
class KostCache : AbstractCache() {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var kundeDao: KundeDao

    /**
     * The key is the kost2-id. Must be synchronized because it isn't readonly (see updateKost1)
     */
    private lateinit var kost1Map: MutableMap<Long, Kost1DO>

    /**
     * The key is the kost2-id. Must be synchronized because it isn't readonly (see updateKost2).
     */
    private lateinit var kost2Map: MutableMap<Long, Kost2DO>

    private lateinit var kost2ArtMap: Map<Long, Kost2ArtDO>

    /**
     * Mustn't be synchronized because it is only read.
     */
    private var allKost2Arts: List<Kost2Art>? = null

    private var kost2EntriesExists = false

    @PostConstruct
    private fun postConstruct() {
        kundeDao.register(object : BaseDOModifiedListener<KundeDO> {
            override fun afterInsertOrModify(obj: KundeDO, operationType: OperationType) {
                setExpired()
            }
        })
        instance = this
    }

    fun getKost2(kost2Id: Long?): Kost2DO? {
        kost2Id ?: return null
        if (!greaterZero(kost2Id)) {
            return null
        }
        checkRefresh()
        synchronized(kost2Map) {
            return kost2Map[kost2Id]
        }
    }

    /**
     * Returns the Kost2DO if it is initialized (Hibernate). Otherwise, it will be loaded from the database.
     * Prevents lazy loadings.
     */
    fun getKost2IfNotInitialized(kost2: Kost2DO?): Kost2DO? {
        kost2 ?: return null
        if (Hibernate.isInitialized(kost2)) {
            return kost2
        }
        return getKost2(kost2.id)
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
        checkRefresh()
        synchronized(kost2Map) {
            return kost2Map.values.firstOrNull { kost2 ->
                kost2.nummernkreis == nummernkreis && kost2.bereich == bereich && kost2.teilbereich == teilbereich && kost2.kost2Art?.id == kost2art.toLong()
            }
        }
    }

    fun getActiveKost2(nummernkreis: Int, bereich: Int, teilbereich: Int): List<Kost2DO> {
        checkRefresh()
        synchronized(kost2Map) {
            return kost2Map.values.filter { kost2 ->
                kost2.nummernkreis == nummernkreis && kost2.bereich == bereich && kost2.teilbereich == teilbereich
                        && (kost2.kostentraegerStatus == KostentraegerStatus.ACTIVE || kost2.kostentraegerStatus == null)
            }
        }
    }

    fun getKost1(kost1Id: Long?): Kost1DO? {
        kost1Id ?: return null
        if (!greaterZero(kost1Id)) {
            return null
        }
        checkRefresh()
        synchronized(kost1Map) {
            return kost1Map[kost1Id]
        }
    }

    /**
     * Returns the Kost1DO if it is initialized (Hibernate). Otherwise, it will be loaded from the database.
     * Prevents lazy loadings.
     */
    fun getKost1IfNotInitialized(kost1: Kost1DO?): Kost1DO? {
        kost1 ?: return null
        if (Hibernate.isInitialized(kost1)) {
            return kost1
        }
        return getKost1(kost1.id)
    }

    /**
     * @param kostString Format ######## or #.###.##.## is supported.
     * @see .getKost2
     */
    fun getKost1(kostString: String?): Kost1DO? {
        val kostArray = parseKostString(kostString) ?: return null
        checkRefresh()
        synchronized(kost1Map) {
            return kost1Map.values.firstOrNull { kost1 ->
                kost1.nummernkreis == kostArray[0] && kost1.bereich == kostArray[1] && kost1.teilbereich == kostArray[2] && kost1.endziffer == kostArray[3]
            }
        }
    }

    fun getKost2Art(kost2ArtId: Long?): Kost2ArtDO? {
        kost2ArtId ?: return null
        if (!greaterZero(kost2ArtId)) {
            return null
        }
        checkRefresh()
        return kost2ArtMap[kost2ArtId]
    }

    /**
     * Returns the Kost2ArtDO if it is initialized (Hibernate). Otherwise, it will be loaded from the database.
     * Prevents lazy loadings.
     */
    fun getKost2ArtIfNotInitialized(kost2Art: Kost2ArtDO?): Kost2ArtDO? {
        kost2Art ?: return null
        if (Hibernate.isInitialized(kost2Art)) {
            return kost2Art
        }
        return getKost2Art(kost2Art.id)
    }


    /**
     * Gibt die für das Projekt definierten, nicht gelöschten Kostenarten zurück.
     *
     * @param projektId
     */
    fun getKost2ArtsForProjekt(projektId: Long?): Set<Kost2ArtDO> {
        projektId ?: return emptySet()
        checkRefresh()
        return getKost2ForProjekt(projektId, false)
            .mapNotNull { it.kost2Art }
            .toSet()
    }

    /**
     * Gibt alle nicht gelöschten Kostenarten zurück, wobei die für das Projekt definierten entsprechend markiert sind.
     *
     * @param projektId
     */
    fun getAllKost2ArtsForProjekt(projektId: Long?): List<Kost2Art> {
        checkRefresh()
        synchronized(kost2Map) {
            val set = getKost2ArtsForProjekt(projektId)
            val result = mutableListOf<Kost2Art>()
            allKost2Arts?.filter { !it.isDeleted }?.forEach { kost2Art ->
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
    }

    fun getKost2ForProjekt(projektId: Long?, includeDeleted: Boolean = false): List<Kost2DO> {
        projektId ?: return emptyList()
        checkRefresh()
        synchronized(kost2Map) {
            return kost2Map.values.filter { it.projekt?.id == projektId && (includeDeleted || !it.deleted) }
        }
    }

    val cloneOfAllKost2Arts: List<Kost2Art>
        get() {
            checkRefresh()
            return allKost2Arts?.map { kost2Art ->
                val kost2ArtDO = (kost2Art as Kost2ArtImpl).kost2ArtDO
                val clone = Kost2ArtDO()
                clone.copyValuesFrom(kost2ArtDO)
                Kost2ArtImpl(clone)
            } ?: emptyList()
        }

    fun isKost2EntriesExists(): Boolean {
        checkRefresh()
        return kost2EntriesExists
    }

    /**
     * Should be called after user modifications.
     */
    fun updateKost2(kost2: Kost2DO) {
        val kost2Id = kost2.id ?: return
        checkRefresh()
        synchronized(kost2Map) {
            kost2Map[kost2Id] = kost2
        }
    }

    /**
     * Should be called after user modifications.
     */
    fun updateKost1(kost1: Kost1DO) {
        val kost1Id = kost1.id ?: return
        checkRefresh()
        synchronized(kost2Map) {
            kost1Map[kost1Id] = kost1
        }
    }

    fun updateKost2Arts() {
        // This method must not be synchronized because it works with a new copy of list.
        val kost2Arts = persistenceService.executeQuery(
            "from Kost2ArtDO t where t.deleted = false order by t.id",
            Kost2ArtDO::class.java, lockModeType = LockModeType.NONE
        )
        this.allKost2Arts = kost2Arts.map { Kost2ArtImpl(it) }
        kost2ArtMap = kost2Arts.associateBy { it.id!! }
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Initializing KostCache ...")
        persistenceService.runIsolatedReadOnly { context ->
            // This method must not be synchronized because it works with a new copy of maps.
            this.kost1Map = persistenceService
                .executeQuery("from Kost1DO t", Kost1DO::class.java, lockModeType = LockModeType.NONE)
                .filter { it.id != null }
                .associateBy { it.id!! }
                .toMutableMap()
            this.kost2Map = persistenceService
                .executeQuery("from Kost2DO t", Kost2DO::class.java, lockModeType = LockModeType.NONE)
                .filter { it.id != null }
                .associateBy { it.id!! }
                .toMutableMap()
            kost2EntriesExists = kost2Map.values.any { !it.deleted }
            updateKost2Arts()
            log.info { "Initializing of KostCache done. ${context.formatStats()}" }
        }
    }

    companion object {
        lateinit var instance: KostCache
            private set

        internal fun setForTestCases() {
            instance = KostCache()
        }
    }
}

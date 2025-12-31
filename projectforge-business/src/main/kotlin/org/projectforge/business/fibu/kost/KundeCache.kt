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

import jakarta.annotation.PostConstruct
import jakarta.persistence.LockModeType
import mu.KotlinLogging
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * The kost2 entries will be cached.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
class KundeCache : AbstractCache() {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var kundeDao: KundeDao

    /**
     * The key is the kost2-id. Must be synchronized because it isn't readonly (see updateKost1)
     */
    private lateinit var kundeMap: Map<Long, KundeDO>

    @PostConstruct
    private fun postConstruct() {
        instance = this
        kundeDao.register(object : BaseDOModifiedListener<KundeDO> {
            override fun afterInsertOrModify(obj: KundeDO, operationType: OperationType) {
                setExpired()
            }
        })
    }

    /**
     * @param nummer The pk (don't use the id).
     * Returns the KundeDO by its id.
     */
    fun getKunde(kundeNummer: Long?): KundeDO? {
        kundeNummer ?: return null
        checkRefresh()
        synchronized(kundeMap) {
            return kundeMap[kundeNummer]
        }
    }

    /**
     * Returns the KundeDO if it is initialized (Hibernate). Otherwise, it will be loaded from the database.
     * Prevents lazy loadings.
     */
    fun getKundeIfNotInitialized(kunde: KundeDO?): KundeDO? {
        kunde ?: return null
        if (HibernateUtils.isFullyInitialized(kunde)) {
            return kunde
        }
        return getKunde(kunde.nummer)
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Initializing KundeCache ...")
        persistenceService.runIsolatedReadOnly(recordCallStats = true) { context ->
            this.kundeMap = context
                .executeQuery("from KundeDO t", KundeDO::class.java, lockModeType = LockModeType.NONE)
                .filter { it.nummer != null }
                .associateBy { it.nummer!! }
            log.info { "Initializing of KundeCache done. ${context.formatStats()}" }
        }
    }

    companion object {
        lateinit var instance: KundeCache
            private set
    }
}

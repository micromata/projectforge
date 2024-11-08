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
import org.projectforge.business.fibu.*
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
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
class ProjektCache : AbstractCache() {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var projektDao: ProjektDao

    /**
     * The key is the kost2-id. Must be synchronized because it isn't readonly (see updateKost1)
     */
    private lateinit var projektMap: Map<Long, ProjektDO>

    @PostConstruct
    private fun postConstruct() {
        instance = this
        projektDao.register(object : BaseDOModifiedListener<ProjektDO> {
            override fun afterInsertOrModify(obj: ProjektDO, operationType: OperationType) {
                setExpired()
            }
        })
    }

    fun getProjekt(projektId: Long?): ProjektDO? {
        projektId ?: return null
        checkRefresh()
        synchronized(projektMap) {
            return projektMap[projektId]
        }
    }

    /**
     * Returns the ProjektDO if it is initialized (Hibernate). Otherwise, it will be loaded from the database.
     * Prevents lazy loadings.
     */
    fun getProjektIfNotInitialized(projekt: ProjektDO?): ProjektDO? {
        val projektId = projekt?.id ?: return null
        if (Hibernate.isInitialized(projekt)) {
            return projekt
        }
        return getProjekt(projektId)
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Initializing ProjektCache ...")
        persistenceService.runIsolatedReadOnly(recordCallStats = true) { context ->
            this.projektMap = context
                .executeQuery("from ProjektDO t", ProjektDO::class.java, lockModeType = LockModeType.NONE)
                .filter { it.id != null }
                .associateBy { it.id!! }
            log.info { "Initializing of ProjektCache done. ${context.formatStats()}" }
        }
    }

    companion object {
        lateinit var instance: ProjektCache
            private set
    }
}

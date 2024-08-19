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
package org.projectforge.business.systeminfo

import mu.KotlinLogging
import org.hibernate.query.sqm.tree.SqmNode.log
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Provides some system information in a cache.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
class SystemInfoCache : AbstractCache() {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    private var cost2EntriesExists = false
    private var projectEntriesExists = false
    private var customerEntriesExists = false

    fun isCustomerEntriesExists(): Boolean {
        checkRefresh()
        return customerEntriesExists
    }

    fun isProjectEntriesExists(): Boolean {
        checkRefresh()
        return projectEntriesExists
    }

    fun isCost2EntriesExists(): Boolean {
        checkRefresh()
        return cost2EntriesExists
    }

    override fun refresh() {
        log.info("Refreshing SystemInfoCache...")

        customerEntriesExists = hasTableEntries(KundeDO::class.java)
        projectEntriesExists = hasTableEntries(ProjektDO::class.java)
        cost2EntriesExists = hasTableEntries(Kost2DO::class.java)
        log.info("Refreshing SystemInfoCache done.")
    }

    private fun hasTableEntries(entity: Class<*>): Boolean {
        return persistenceService.selectSingleResult(
            "select count(e) from ${entity.name} e",
            Long::class.java,
        )!! > 0L
    }

    companion object {
        /**
         * SystemInfoCache can be used either over Spring context or with this static method.
         *
         * @return
         */
        @JvmStatic
        fun instance(): SystemInfoCache? {
            return instance
        }

        private var instance: SystemInfoCache? = null

        /**
         * Only for internal usage on start-up of ProjectForge.
         *
         * @param theInstance
         */
        @JvmStatic
        fun internalInitialize(theInstance: SystemInfoCache?) {
            instance = theInstance
        }
    }
}

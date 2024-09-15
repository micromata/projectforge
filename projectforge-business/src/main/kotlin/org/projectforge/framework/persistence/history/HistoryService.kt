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

package org.projectforge.framework.persistence.history

import mu.KotlinLogging
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

/**
 */
@Service
class HistoryService {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /**
     * Save method will be called automatically by the Dao services.
     */
    fun save(master: PfHistoryMasterDO, attrs: Collection<PfHistoryAttrDO>? = null): Long? {
        persistenceService.runInTransaction { context ->
            val em = context.em
            master.createdBy = ThreadLocalUserContext.user?.id?.toString() ?: "anon"
            master.createdAt = Date()
            em.persist(master)
            log.info { "Saving history: $master" }
            attrs?.forEach { attr ->
                attr.master = master
                // N (Null) or V (String) (de.micromata.genome.util.strings.converter.ConvertedStringTypes)
                attr.type = if (attr.value == null) "N" else "V"
                em.persist(attr)
            }
        }
        return master.id
    }
}

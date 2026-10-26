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

package org.projectforge.business.user

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.Tuple
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.database.TupleUtils
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Which users have an avatar, and its version (the time of its last change, for the `?v=` of the image urls).
 * Chat messages and the user menu ask for every user they show, so no bytes and no database access per call.
 */
@Component
open class UserAvatarCache : AbstractCache() {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /** Key is the user id, value the version (epoch millis). Replaced as a whole, so read without locking. */
    private var versionMap = mapOf<Long, Long>()

    /**
     * The version of the avatar of the given user, or null if the user has none.
     */
    fun getVersion(userId: Long?): Long? {
        userId ?: return null
        checkRefresh()
        return versionMap[userId]
    }

    override fun refresh() {
        log.info { "Initializing UserAvatarCache ..." }
        persistenceService.runIsolatedReadOnly(recordCallStats = true) { context ->
            val newMap = mutableMapOf<Long, Long>()
            context.em.createQuery(SELECT_AVATAR_INFO, Tuple::class.java).resultList.forEach { tuple ->
                val userId = TupleUtils.getLong(tuple, "userId") ?: return@forEach
                newMap[userId] = TupleUtils.getDate(tuple, "lastUpdate")?.time ?: 0L
            }
            versionMap = newMap
            log.info { "Initializing of UserAvatarCache done. ${context.formatStats(true)}" }
        }
    }

    companion object {
        private val SELECT_AVATAR_INFO =
            "SELECT user.id as userId,lastUpdate as lastUpdate FROM ${UserAvatarDO::class.simpleName}"
    }
}

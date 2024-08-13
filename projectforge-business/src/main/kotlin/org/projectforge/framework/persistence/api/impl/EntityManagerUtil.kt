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

package org.projectforge.framework.persistence.api.impl

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.TypedQuery
import mu.KotlinLogging
import org.hibernate.Session
import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.framework.persistence.utils.SQLHelper.queryToString

private val log = KotlinLogging.logger {}

object EntityManagerUtil {
    private fun getEntityManager(entityManagerFactory: EntityManagerFactory): EntityManager {
        return entityManagerFactory.createEntityManager()
    }

    fun <T> runInTransaction(
        entityManagerFactory: EntityManagerFactory,
        readonly: Boolean = false,
        run: (em: EntityManager) -> T
    ): T {
        entityManagerFactory.createEntityManager().use { em ->
            if (readonly) {
                em.unwrap(Session::class.java).isDefaultReadOnly = true
            }
            em.transaction.begin()
            try {
                val ret = run(em)
                // em.flush()
                em.transaction.commit()
                return ret
            } catch (ex: Exception) {
                em.transaction.rollback()
                log.error(ex.message, ex)
                throw ex
            }
        }
    }

    fun <T> runInReadOnlyTransaction(entityManagerFactory: EntityManagerFactory, block: (em: EntityManager) -> T): T {
        return runInTransaction(entityManagerFactory, true, run = { em -> block(em) })
    }

    fun <T> selectById(
        entityManagerFactory: EntityManagerFactory,
        entityClass: Class<T>,
        id: Any?,
        detached: Boolean = true
    ): T? {
        id ?: return null
        return runInReadOnlyTransaction(entityManagerFactory) { em ->
            val entity = em.find(entityClass, id)
            if (detached && entity != null) {
                em.detach(entity)
            }
            entity
        }
    }

    fun <T> ensureUniqueResult(
        entityManagerFactory: EntityManagerFactory,
        nullAllowed: Boolean = true,
        errorMessage: String? = null,
        detached: Boolean = true,
        execute: (em: EntityManager) -> TypedQuery<T>
    ): T? {
        entityManagerFactory.createEntityManager().use { em ->
            val query = execute(em)
            val list = query.resultList
            if (nullAllowed && list.isNullOrEmpty())
                return null
            if (list.size != 1) {
                throw InternalErrorException(
                    "Internal error: ProjectForge requires a single entry, but found ${list.size} entries: ${
                        queryToString(
                            query,
                            errorMessage
                        )
                    }"
                )
            }
            val entity = list[0]
            if (detached) {
                em.detach(entity)
            }
            return entity
        }
    }

}

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

package org.projectforge.framework.persistence.api.impl

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.Predicate
import mu.KotlinLogging
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty

private val log = KotlinLogging.logger {}

internal class DBQueryBuilderByCriteria<O : ExtendedBaseDO<Long>>(
    private val baseDao: BaseDao<O>,
    private val entityManager: EntityManager,
    private val queryFilter: QueryFilter
) {
    private var _ctx: DBCriteriaContext<O>? = null
    private val ctx: DBCriteriaContext<O>
        get() {
            if (_ctx == null) {
                val cb = entityManager.criteriaBuilder
                val cr = cb.createQuery(baseDao.doClass)
                _ctx = DBCriteriaContext(cb, cr, cr.from(baseDao.doClass), baseDao.doClass)
                initJoinSets()
            }
            return _ctx!!
        }

    /**
     * predicates for criteria search.
     */
    private val predicates = mutableListOf<Predicate>()
    private val order = mutableListOf<jakarta.persistence.criteria.Order>()

    fun add(matcher: DBPredicate) {
        matcher.asPredicate(ctx)?.let {
            predicates.add(it)
        }
    }

    fun createResultIterator(resultPredicates: List<DBPredicate>, queryFilter: QueryFilter): DBResultIterator<O> {
        return DBCriteriaResultIterator(
            entityManager,
            ctx.cr.select(ctx.root).where(*predicates.toTypedArray()).orderBy(*order.toTypedArray()),
            resultPredicates,
            queryFilter,
        )
    }

    fun addOrder(sortProperty: SortProperty) {
        try {
            order.add(
                if (sortProperty.ascending) {
                    if (log.isDebugEnabled) log.debug("Adding criteria orderBy (${ctx.entityName}): order by ${sortProperty.property}.")
                    ctx.cb.asc(ctx.getField<Any>(sortProperty.property))
                } else {
                    if (log.isDebugEnabled) log.debug("Adding criteria orderBy (${ctx.entityName}): order by ${sortProperty.property} desc.")
                    ctx.cb.desc(ctx.getField<Any>(sortProperty.property))
                }
            )
        } catch (ex: Exception) {
            log.error("Can't add order for property '${ctx.entityName}.${sortProperty.property}: ${ex.message}")
        }
    }

    private fun initJoinSets() {
        queryFilter.joinList.forEach {
            ctx.addJoin(it)
        }
    }
}

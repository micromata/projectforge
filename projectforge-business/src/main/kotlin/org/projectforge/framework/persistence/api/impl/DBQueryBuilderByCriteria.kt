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

package org.projectforge.framework.persistence.api.impl

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Nulls
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty

private val log = KotlinLogging.logger {}

/**
 * @param projection Builds a query of selected properties ([selectDistinct]) instead of the entities
 * ([createResultIterator]).
 */
internal class DBQueryBuilderByCriteria<O : ExtendedBaseDO<Long>>(
    private val baseDao: BaseDao<O>,
    private val entityManager: EntityManager,
    private val queryFilter: QueryFilter,
    private val projection: Boolean = false,
) {
    private val ctx: DBCriteriaContext<O> by lazy {
        val cb = entityManager.criteriaBuilder
        val cr = if (projection) cb.createQuery(Array<Any>::class.java) else cb.createQuery(baseDao.doClass)
        DBCriteriaContext(cb, cr, cr.from(baseDao.doClass), baseDao.doClass).also { context ->
            queryFilter.joinList.forEach { join ->
                // A fetch join needs its owner selected, which a projection doesn't do.
                context.addJoin(if (projection && join.fetch) DBJoin(join.attribute, join.joinType, false, join.parent) else join)
            }
        }
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
        check(!projection) { "A projection selects no entities, see selectDistinct." }
        @Suppress("UNCHECKED_CAST")
        val cr = ctx.cr as CriteriaQuery<O>
        return DBCriteriaResultIterator(
            entityManager,
            cr.select(ctx.root).where(*predicates.toTypedArray()).orderBy(*order.toTypedArray()),
            resultPredicates,
            queryFilter,
        )
    }

    /**
     * The distinct value combinations of the [paths] over the matching rows, unordered. Nested paths are
     * left-joined (see [DBCriteriaContext.getOrderField]), so a row without the association yields null
     * instead of being dropped.
     */
    fun selectDistinct(paths: List<String>): List<Array<Any?>> {
        check(projection) { "Build the query builder with projection = true." }
        @Suppress("UNCHECKED_CAST")
        val cr = ctx.cr as CriteriaQuery<Array<Any?>>
        cr.select(ctx.cb.array(paths.map { ctx.getOrderField<Any>(it) })).distinct(true).where(*predicates.toTypedArray())
        return entityManager.createQuery(cr).resultList
    }

    /**
     * Adds an order by clause that treats entries without a value as the smallest value, so they
     * lead an ascending sort and trail a descending one — reversing the sort brings them into view.
     *
     * Text columns hold two representations of "no value": historically `null`, in some records an
     * empty string. Left alone the two behave differently — ascending leads with the empty strings
     * (the smallest value), descending leads with the nulls (PostgreSQL's default) — so which blank
     * entries surface depends on the record. Mapping empty strings to null makes them
     * interchangeable, and the null precedence follows the sort direction.
     */
    fun addOrder(sortProperty: SortProperty) {
        try {
            val cb = ctx.cb
            val field = ctx.getOrderField<Any>(sortProperty.property)
            val expression: Expression<*> = if (field.javaType == String::class.java) {
                @Suppress("UNCHECKED_CAST")
                cb.nullif(field as Path<String>, "")
            } else {
                field
            }
            // Nulls count as the smallest value, so they flip with the direction.
            val nulls = if (sortProperty.ascending) Nulls.FIRST else Nulls.LAST
            if (log.isDebugEnabled()) {
                log.debug { "Adding criteria orderBy (${ctx.entityName}): order by ${sortProperty.property} ${if (sortProperty.ascending) "asc" else "desc"} nulls ${nulls.name.lowercase()}." }
            }
            order.add(if (sortProperty.ascending) cb.asc(expression, nulls) else cb.desc(expression, nulls))
        } catch (ex: Exception) {
            log.error { "Can't add order for property '${ctx.entityName}.${sortProperty.property}': ${ex.message}. " +
                        "The query goes out without this ORDER BY. If this is a computed/transient column (no " +
                        "database column to sort on), declare it in AbstractEntityRest.computedSortProperties " +
                        "so filterList/sortIds sort by it instead." }
        }
    }
}

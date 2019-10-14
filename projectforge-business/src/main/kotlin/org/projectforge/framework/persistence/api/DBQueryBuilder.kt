/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.api

import org.hibernate.Transaction
import org.hibernate.search.Search
import org.hibernate.search.query.dsl.BooleanJunction
import org.hibernate.search.query.dsl.QueryBuilder
import org.projectforge.business.multitenancy.TenantService
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root


internal interface DBQueryBuilder<O : ExtendedBaseDO<Int>> {
    fun result(): DBResultIterator<O>
    fun equal(field: String, value: Any)
    fun ilike(field: String, value: String)
    fun fulltextSearch(searchString: String)
    /**
     * Should be called after processing the result set. The DBFullTextQueryBuilder must be closed for committing
     * the transaction.
     */
    fun close()
}

internal class DBCriteriaBuilder<O : ExtendedBaseDO<Int>>(
        private val baseDao: BaseDao<O>,
        tenantService: TenantService,
        ignoreTenant: Boolean = false)
    : DBQueryBuilder<O> {
    private val em = baseDao.entityManager
    private val cb: CriteriaBuilder
    private val cr: CriteriaQuery<O>
    private val root: Root<O>
    private val predicates = mutableListOf<Predicate>()

    init {
        cb = baseDao.session.getCriteriaBuilder()
        cr = cb.createQuery(baseDao.doClass)
        root = cr.from(baseDao.doClass)

        if (!ignoreTenant && tenantService.isMultiTenancyAvailable) {
            val userContext = ThreadLocalUserContext.getUserContext()
            val currentTenant = userContext.currentTenant
            if (currentTenant != null) {
                if (currentTenant.isDefault) {
                    predicates.add(cb.or(cb.equal(root.get<Any>("tenant"), userContext.currentTenant),
                            cb.isNull(root.get<Any>("tenant"))))
                } else {
                    predicates.add(cb.equal(root.get<Any>("tenant"), userContext.currentTenant))
                }
            }
        }
    }

    override fun equal(field: String, value: Any) {
        predicates.add(cb.equal(root.get<Any>(field), value))
    }

    override fun ilike(field: String, value: String) {
        predicates.add(cb.like(cb.lower(root.get<String>(field)), value))
    }

    override fun result(): DBResultIterator<O> {
        return DBCriteriaResultIterator(em, cr.select(root).where(*predicates.toTypedArray()))
    }

    /**
     * Not supported.
     */
    override fun fulltextSearch(searchString: String) {
        throw UnsupportedOperationException("Method not supported by DBCriteriaQueryBuilder. Use DBFullTextQueryBuilder instead.")
    }

    override fun close() {
        // Nothing to do.
    }
}

internal class DBFullTextQueryBuilder<O : ExtendedBaseDO<Int>>(
        private val baseDao: BaseDao<O>,
        searchFields: Array<String>? = null)
    : DBQueryBuilder<O> {
    private var usedSearchFields = searchFields ?: baseDao.searchFields
    private var queryBuilder: QueryBuilder
    private var boolJunction: BooleanJunction<*>
    private val transaction: Transaction
    private val fullTextSession = Search.getFullTextSession(baseDao.session)


    init {
        transaction = fullTextSession.beginTransaction()
        queryBuilder = fullTextSession.searchFactory
                .buildQueryBuilder().forEntity(baseDao.doClass).get()
        boolJunction = queryBuilder.bool()
        val fields = searchFields ?: baseDao.searchFields
        val stringFields = mutableListOf<String>()
        fields.forEach {
            val field = PropUtils.getField(baseDao.doClass, it, true)
            if (field?.type?.isAssignableFrom(String::class.java) == true) {
                stringFields.add(it)
            } else {
                System.err.println("***** Field isn't of type String: '$it'")
            }
        }
        usedSearchFields = stringFields.toTypedArray()
    }

    override fun equal(field: String, value: Any) {
        if (usedSearchFields.contains(field)) {
            boolJunction = boolJunction.must(queryBuilder.keyword().onField(field).matching(value).createQuery())
        } else {
            // TODO
            System.err.println("********* Not yet implemented: equal search on non index fields: '$field'")
        }
    }

    override fun ilike(field: String, value: String) {
        search(value, field)
    }

    override fun fulltextSearch(searchString: String) {
        search(searchString, *usedSearchFields)
    }

    override fun result(): DBResultIterator<O> {
        return DBFullTextResultIterator(baseDao, fullTextSession, boolJunction.createQuery())
    }

    override fun close() {
        transaction.commit()
    }

    private fun search(value: String, vararg fields: String) {
        val str = value.replace('%', '*')
        val context = if (str.indexOf('*') >= 0) {
            if (fields.size > 1) {
                queryBuilder.keyword().wildcard().onFields(*fields)
            } else {
                queryBuilder.keyword().wildcard().onField(fields[0])
            }
        } else {
            if (fields.size > 1) {
                queryBuilder.keyword().onFields(*fields)
            } else {
                queryBuilder.keyword().onField(fields[0])
            }
        }
        boolJunction = boolJunction.must(context.matching(str).createQuery())
    }
}

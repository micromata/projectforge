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

package org.projectforge.framework.persistence.api.impl

import org.hibernate.Criteria
import org.hibernate.Transaction
import org.hibernate.search.Search
import org.hibernate.search.query.dsl.BooleanJunction
import org.hibernate.search.query.dsl.QueryBuilder
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO

internal class DBQueryBuilderByFullText<O : ExtendedBaseDO<Int>>(
        val baseDao: BaseDao<O>,
        searchFields: Array<String>? = null) {
    private var usedSearchFields = searchFields ?: baseDao.searchFields
    private var queryBuilder: QueryBuilder
    private var boolJunction: BooleanJunction<*>
    private val transaction: Transaction
    private val fullTextSession = Search.getFullTextSession(baseDao.session)
    private val sortBys = mutableListOf<SortBy>()

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
                stringFields.add(it) // Search only for string fields, if no special field is specified.
            }
        }
        usedSearchFields = stringFields.toTypedArray()
    }

    fun fieldSupported(field: String): Boolean {
        return usedSearchFields.contains(field)
    }

    /**
     * @return true if the given field is indexed, otherwise false (dbMatcher should be used instead).
     */
    fun equal(field: String, value: Any): Boolean {
        if (usedSearchFields.contains(field)) {
            boolJunction = boolJunction.must(queryBuilder.keyword().onField(field).matching(value).createQuery())
            return true
        }
        return false
    }

    fun ilike(field: String, value: String) {
        search(value, field)
    }

    fun fulltextSearch(searchString: String) {
        search(searchString, *usedSearchFields)
    }

    fun createResultIterator(dbResultMatchers: List<DBResultMatcher>, criteria: Criteria?): DBResultIterator<O> {
        return DBFullTextResultIterator(baseDao, fullTextSession, boolJunction.createQuery(), dbResultMatchers, sortBys.toTypedArray(), criteria)
    }

    fun close() {
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

    fun addOrder(sortBy: SortBy) {
        sortBys.add(SortBy(sortBy.field, sortBy.ascending))
    }
}

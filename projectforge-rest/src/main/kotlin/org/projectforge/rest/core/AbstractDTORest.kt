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

package org.projectforge.rest.core

import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.rest.dto.BaseDTO

/**
 * This is the base class for all fronted functionality regarding query, editing etc. It also serves layout
 * data for the frontend.
 * <br>
 * For each entity type such as users, addresses, time sheets etc. an own class is inherited for doing customizations.
 * It's recommended for the frontend to develop generic list and edit pages by using the layout information served
 * by these rest services.
 */
abstract class AbstractDTORest<
        O : ExtendedBaseDO<Int>,
        DTO : BaseDTO<O>,
        B : BaseDao<O>,
        F : BaseSearchFilter>(
        baseDaoClazz: Class<B>,
        filterClazz: Class<F>,
        i18nKeyPrefix: String,
        cloneSupported: Boolean = false)
    : AbstractBaseRest<O, DTO, B, F>(baseDaoClazz, filterClazz, i18nKeyPrefix, cloneSupported) {

    /**
     * @return New result set of dto's, transformed from data base objects.
     */
    override fun processResultSetBeforeExport(resultSet: ResultSet<O>) : ResultSet<*> {
        val newList = resultSet.resultSet.map {
            transformFromDB(it, false)
        }
        return ResultSet(newList, newList.size)
    }

    /**
     * @param dto Expected as DTO
     */
    override fun getId(dto: Any): Int? {
        @Suppress("UNCHECKED_CAST")
        return (dto as DTO).id
    }

    /**
     * @param dto Expected as DTO
     */
    override fun isDeleted(dto: Any): Boolean {
        @Suppress("UNCHECKED_CAST")
        return (dto as DTO).isDeleted
    }

    override fun isHistorizable(): Boolean {
        return AbstractDORest.isHistorizable(baseDao.doClass)
    }
}

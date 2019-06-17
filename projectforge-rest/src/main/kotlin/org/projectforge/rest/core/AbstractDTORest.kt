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
import org.projectforge.ui.UILayout
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

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
        DTO : Any,
        B : BaseDao<O>,
        F : BaseSearchFilter>(
        baseDaoClazz: Class<B>,
        filterClazz: Class<F>,
        i18nKeyPrefix: String,
        cloneSupported: Boolean = false)
    : AbstractBaseRest<O, DTO, B, F>(baseDaoClazz, filterClazz, i18nKeyPrefix, cloneSupported) {

    override fun processResultSetBeforeExport(resultSet: ResultSet<Any>) {
        val orig = resultSet.resultSet
        resultSet.resultSet = orig.map {
            @Suppress("UNCHECKED_CAST")
            transformDO(it as O, false)
        }
    }

    /**
     * Must be overridden if flag [useDTO] is true. Throws [UnsupportedOperationException] at default.
     */
    abstract fun transformDO(obj: O, editMode: Boolean): DTO

    /**
     * Must be overridden if flag [useDTO] is true. Throws [UnsupportedOperationException] at default.
     */
    abstract fun transformDTO(dto: DTO): O

    override fun asDO(dto: DTO): O {
        return transformDTO(dto)
    }

    override fun createEditLayoutData(item: O, layout: UILayout): EditLayoutData {
        return EditLayoutData(transformDO(item, true), layout)
    }

    override fun returnItem(item: O): ResponseEntity<Any> {
        return ResponseEntity<Any>(transformDO(item, true), HttpStatus.OK)
    }
}

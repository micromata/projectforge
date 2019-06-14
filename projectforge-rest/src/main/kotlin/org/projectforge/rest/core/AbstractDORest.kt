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
abstract class AbstractDORest<
        O : ExtendedBaseDO<Int>,
        B : BaseDao<O>,
        F : BaseSearchFilter>(
        private val baseDaoClazz: Class<B>,
        private val filterClazz: Class<F>,
        private val i18nKeyPrefix: String)
    : AbstractBaseRest<O, O, B, F>(baseDaoClazz, filterClazz, i18nKeyPrefix) {

    override fun processResultSetBeforeExport(resultSet: ResultSet<Any>) {
        resultSet.resultSet.forEach { processItemBeforeExport(it) }
    }

    override fun asDO(dto: O): O {
        return dto
    }

    override fun createEditLayoutData(item: O, layout: UILayout): EditLayoutData {
        return EditLayoutData(item, layout)
    }

    override fun returnItem(item: O): ResponseEntity<Any> {
        return ResponseEntity<Any>(item, HttpStatus.OK)
    }
}

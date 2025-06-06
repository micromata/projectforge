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

package org.projectforge.rest.core

import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.dto.BaseDTO
import jakarta.servlet.http.HttpServletRequest

/**
 * This is the base class for all fronted functionality regarding query, editing etc. It also serves layout
 * data for the frontend.
 * <br>
 * For each entity type such as users, addresses, time sheets etc. an own class is inherited for doing customizations.
 * It's recommended for the frontend to develop generic list and edit pages by using the layout information served
 * by these rest services.
 */
abstract class AbstractDTOPagesRest<
    O : ExtendedBaseDO<Long>,
    DTO : BaseDTO<O>,
    B : BaseDao<O>>(
  baseDaoClazz: Class<B>,
  i18nKeyPrefix: String,
  cloneSupport: CloneSupport = CloneSupport.NONE
) : AbstractPagesRest<O, DTO, B>(baseDaoClazz, i18nKeyPrefix, cloneSupport) {

  /**
   * @return New result set of dto's, transformed from database objects.
   */
  override fun postProcessResultSet(
    resultSet: ResultSet<O>,
    request: HttpServletRequest,
    magicFilter: MagicFilter,
  ): ResultSet<*> {
    val newList = resultSet.resultSet.map {
      transformFromDB(it, false)
    }
    val result = ResultSet(
      newList,
      resultSet,
      newList.size,
      selectedEntityIds = resultSet.selectedEntityIds,
      magicFilter = magicFilter,
    )
    resultSet.resultInfo?.let { info ->
      if (info.isNotBlank()) {
        result.resultInfo = info
      }
    }
    return result
  }

  /**
   * @param dto Expected as DTO
   */
  override fun getId(dto: Any): Long? {
    @Suppress("UNCHECKED_CAST")
    return (dto as DTO).id
  }

  /**
   * @param dto Expected as DTO
   */
  override fun isDeleted(dto: Any): Boolean {
    @Suppress("UNCHECKED_CAST")
    return (dto as DTO).deleted
  }

  override fun isHistorizable(): Boolean {
    return AbstractDOPagesRest.isHistorizable(baseDao.doClass)
  }
}

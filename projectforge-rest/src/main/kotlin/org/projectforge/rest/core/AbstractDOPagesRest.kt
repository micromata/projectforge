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
import org.projectforge.framework.persistence.history.HistoryBaseDaoAdapter
import jakarta.servlet.http.HttpServletRequest

/**
 * This is the base class for all fronted functionality regarding query, editing etc. It also serves layout
 * data for the frontend.
 * <br>
 * For each entity type such as users, addresses, time sheets etc. an own class is inherited for doing customizations.
 * It's recommended for the frontend to develop generic list and edit pages by using the layout information served
 * by these rest services.
 */
abstract class AbstractDOPagesRest<
    O : ExtendedBaseDO<Long>,
    B : BaseDao<O>>
@JvmOverloads
constructor(
  baseDaoClazz: Class<B>,
  i18nKeyPrefix: String,
  cloneSupport: CloneSupport = CloneSupport.NONE
) : AbstractPagesRest<O, O, B>(baseDaoClazz, i18nKeyPrefix, cloneSupport) {

  companion object {
    // For caching historizable flag:
    private val historizableMap = mutableMapOf<Class<*>, Boolean>()

    internal fun isHistorizable(clazz: Class<out ExtendedBaseDO<*>>): Boolean {
      var result = historizableMap.get(clazz)
      if (result != null)
        return result
      result = HistoryBaseDaoAdapter.isHistorizable(clazz)
      historizableMap.put(clazz.javaClass, result)
      return result
    }
  }

  override fun postProcessResultSet(
    resultSet: ResultSet<O>,
    request: HttpServletRequest,
    magicFilter: MagicFilter,
  ): ResultSet<*> {
    resultSet.resultSet.forEach { transformFromDB(it) }
    return resultSet
  }

  /**
   * @return dto object itself (it's already of type O)
   */
  override fun transformForDB(dto: O): O {
    return dto
  }

  /**
   * @return obj object itself (it's already of same type)
   */
  override fun transformFromDB(obj: O, editMode: Boolean): O {
    return obj
  }

  /**
   * @param dto Expected as O
   */
  override fun getId(dto: Any): Long? {
    @Suppress("UNCHECKED_CAST")
    return (dto as O).id
  }

  /**
   * @param dto Expected as O
   */
  override fun isDeleted(dto: Any): Boolean {
    @Suppress("UNCHECKED_CAST")
    return (dto as O).deleted
  }

  /**
   * Override this method if your data object isn't historizable.
   */
  override fun isHistorizable(): Boolean {
    return isHistorizable(baseDao.doClass)
  }
}

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

package org.projectforge.rest.core

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.MagicFilter

/**
 * The layout free base class of an entity page that transfers its data objects as they are, without a
 * DTO in between, see [AbstractEntityRest].
 *
 * The counterpart of [AbstractDOPagesRest], whose body this repeats - see [AbstractDTOEntityRest] for
 * why.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
abstract class AbstractDOEntityRest<
        O : ExtendedBaseDO<Long>,
        B : BaseDao<O>>
@JvmOverloads
constructor(
    baseDaoClazz: Class<B>,
    i18nKeyPrefix: String,
    cloneSupport: CloneSupport = CloneSupport.NONE,
) : AbstractEntityRest<O, O, B>(baseDaoClazz, i18nKeyPrefix, cloneSupport) {

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

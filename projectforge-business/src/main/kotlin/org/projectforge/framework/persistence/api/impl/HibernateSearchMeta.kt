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

import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.IDao

object HibernateSearchMeta {
    private val classInfos = mutableMapOf<Class<*>, HibernateSearchClassInfo>()

    fun getSearchFields(dao: IDao<*>): Array<String>? {
        if (dao !is BaseDao) return null
        return getClassInfo(dao).allFieldNames
    }

    fun getClassInfo(baseDao: BaseDao<*>): HibernateSearchClassInfo {
        var result = classInfos[baseDao.doClass]
        if (result == null) {
            result = HibernateSearchClassInfo(baseDao)
            classInfos[baseDao.doClass] = result
        }
        return result
    }
}

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

package org.projectforge.framework.persistence.api.impl

import mu.KotlinLogging
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.IDao

private val log = KotlinLogging.logger {}

object HibernateSearchMeta {
    private val classInfos = mutableMapOf<Class<*>, HibernateSearchClassInfo>()

    /**
     * Used by Wicket list pages to show the search fields in tooltips.
     */
    fun getSearchFields(dao: IDao<*>): Array<String>? {
        if (dao !is BaseDao) return null
        val additionalFields = dao.additionalSearchFields
        return if (additionalFields.isNullOrEmpty()) {
            ensureClassInfo(dao).allFieldNames
        } else {
            // merge and sort
            (ensureClassInfo(dao).allFieldNames + additionalFields)
                .distinct()
                .sorted()
                .toTypedArray()
        }
    }

    fun getClassInfo(baseDO: Class<*>): HibernateSearchClassInfo {
        synchronized(classInfos) {
            return classInfos[baseDO] ?: throw IllegalArgumentException("No HibernateSearchClassInfo found for $baseDO")
        }
    }

    fun ensureClassInfo(baseDao: BaseDao<*>): HibernateSearchClassInfo {
        synchronized(classInfos) {
            return classInfos[baseDao.doClass] ?: HibernateSearchClassInfo(baseDao).also {
                classInfos[baseDao.doClass] = it
            }
        }
    }
}

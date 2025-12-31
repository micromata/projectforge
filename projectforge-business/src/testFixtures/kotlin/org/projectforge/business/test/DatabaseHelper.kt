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

package org.projectforge.business.test

import org.projectforge.framework.persistence.jpa.PfPersistenceService

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object DatabaseHelper {
    @JvmStatic
    fun clearDatabase(persistenceService: PfPersistenceService) {
        persistenceService.runInTransaction { context ->
            context.executeNativeUpdate("SET DATABASE REFERENTIAL INTEGRITY FALSE")
        }
        persistenceService.runInTransaction { context ->
            val em = context.em
            em.createNativeQuery("SELECT TABLE_NAME \n" +
                    "FROM INFORMATION_SCHEMA.TABLES \n" +
                    "WHERE TABLE_TYPE = 'BASE TABLE' \n" +
                    "AND TABLE_SCHEMA = 'PUBLIC'")
                .resultList
                .forEach { tableName ->
                    em.createNativeQuery("DELETE FROM $tableName").executeUpdate()
                }
        }
        persistenceService.runInTransaction { context ->
            context.executeNativeUpdate("SET DATABASE REFERENTIAL INTEGRITY TRUE")
        }
    }
}

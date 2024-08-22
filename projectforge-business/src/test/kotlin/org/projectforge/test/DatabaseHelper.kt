/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.test

import jakarta.persistence.EntityManager

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object DatabaseHelper {
    @JvmStatic
    fun clearDatabase(em: EntityManager) {
        em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate() // Ignore all foreign key constraints etc.
        em.createQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'TABLE' AND TABLE_SCHEMA = 'PUBLIC';")
            .resultList
            .forEach { tableName ->
                em.createNativeQuery("TRUNCATE TABLE $tableName").executeUpdate()
            }
        em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate()
    }
}

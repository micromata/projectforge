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

package org.projectforge.framework.persistence.jpa

object EntityMetaDataRegistry {
    private val columnMetaDataMap = mutableMapOf<Class<*>, EntityMetaData>()

    private val notFoundEntities = mutableSetOf<Class<*>>()

    fun getEntityMetaData(entityClass: Class<*>): EntityMetaData {
        if (notFoundEntities.contains(entityClass)) {
            throw IllegalArgumentException(notFoundExceptionMessage(entityClass))
        }
        columnMetaDataMap[entityClass]?.let { return it }
        try {
            val entityMetaData = EntityMetaData(entityClass)
            columnMetaDataMap[entityClass] = entityMetaData
            return entityMetaData
        } catch (e: Exception) {
            notFoundEntities.add(entityClass)
            throw IllegalArgumentException(notFoundExceptionMessage(entityClass))
        }
    }

    fun getColumnMetaData(entityClass: Class<*>, columnName: String): ColumnMetaData? {
        return getEntityMetaData(entityClass).getColumnMetaData(columnName)
    }

    private fun notFoundExceptionMessage(entityClass: Class<*>): String {
        return "EntityMetaData for class $entityClass not found."
    }
}

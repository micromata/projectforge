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

package org.projectforge.framework.persistence.metamodel

import jakarta.persistence.Column
import jakarta.persistence.metamodel.EntityType
import org.hibernate.query.sqm.tree.SqmNode.log
import org.projectforge.framework.json.JsonUtils
import kotlin.reflect.KClass


/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class EntityInfo(
    val entityClass: Class<*>,
    val name: String,
    val entityType: EntityType<*>,
    val tableName: String? = null,
) {
    private val columnWithoutLength = mutableSetOf<String>()
    private val propertyInfos = mutableListOf<EntityPropertyInfo>()

    init {
        entityType.attributes.forEach { attr ->
            attr.javaMember ?: return@forEach
            propertyInfos.add(
                EntityPropertyInfo(
                    entityClass = entityClass,
                    propertyName = attr.name,
                )
            )
        }
    }

    fun getPropertiesWithAnnotation(annotationClass: KClass<out Annotation>): List<EntityPropertyInfo> {
        return propertyInfos.filter { it.hasAnnotation(annotationClass) }
    }

    fun getColumnAnnotation(propertyName: String): Column? {
        return propertyInfos.find { it.propertyName == propertyName }?.getAnnotation(Column::class)
    }

    fun getColumnLength(propertyName: String): Int? {
        val length = getColumnAnnotation(propertyName)?.length
        if (length != null) {
            return length
        }
        if (columnWithoutLength.contains(propertyName)) {
            return null
        }
        columnWithoutLength.add(propertyName)
        val msg = ("Could not find persistent class for entityName '$propertyName' (OK for non hibernate objects).")
        if (propertyName.endsWith("DO")) {
            log.error(msg)
        } else {
            log.info(msg)
        }
        return null
    }
}

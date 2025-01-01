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

package org.projectforge.framework.persistence.metamodel

import jakarta.persistence.Column
import jakarta.persistence.metamodel.EntityType
import mu.KotlinLogging
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1

private val log = KotlinLogging.logger {}

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
                    attr = attr,
                    propertyName = attr.name,
                )
            )
        }
    }

    fun getIdProperty(): String? {
        return propertyInfos.firstOrNull { it.isIdProperty }?.propertyName
    }

    fun isPersistedProperty(property: KCallable<*>): Boolean {
        return if (property is KMutableProperty1<*, *>) {
            isPersistedProperty(property)
        } else {
            false
        }
    }

    fun isPersistedProperty(property: KMutableProperty1<*, *>): Boolean {
        return propertyInfos.any { it.propertyName == property.name }
        // return entityType.attributes.any { it.javaMember == property.javaField }
    }

    fun isPersistedProperty(propertyName: String): Boolean {
        return propertyInfos.any { it.propertyName == propertyName }
        // return entityType.attributes.any { it.javaMember == property.javaField }
    }

    fun getPropertyInfo(propertyName: String): EntityPropertyInfo? {
        return propertyInfos.find { it.propertyName == propertyName }
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

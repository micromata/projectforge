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

package org.projectforge.framework.persistence.jpa

import jakarta.persistence.Column
import mu.KotlinLogging
import org.projectforge.common.BeanHelper

private val log = KotlinLogging.logger {}

class EntityMetaData(val entityClass: Class<*>) {
    /** Key is the property name */
    private val columns = mutableMapOf<String, ColumnMetaData>()

    fun getColumnMetaData(columnName: String): ColumnMetaData? {
        return columns[columnName]
    }

    init {
        require(entityClass.isAnnotationPresent(jakarta.persistence.Entity::class.java)) {
            "Class $entityClass is not an entity."
        }

        // Durch alle Felder der Klasse iterieren
        for (field in entityClass.declaredFields) {
            // Überprüfen, ob das Feld mit @Column annotiert ist
            if (field.isAnnotationPresent(Column::class.java)) {
                processColumnAnnotation(field.name, field.getAnnotation(Column::class.java), AnnoType.FIELD)
            }
        }

        for (method in entityClass.declaredMethods) {
            if (method.isAnnotationPresent(Column::class.java)) {
                val fieldName = BeanHelper.determinePropertyName(method)
                processColumnAnnotation(fieldName, method.getAnnotation(Column::class.java), AnnoType.METHOD)
            }
        }
    }

    private fun processColumnAnnotation(fieldName: String, columnAnnotation: Column, type: AnnoType) {
        log.debug { "$type $fieldName is annotated with @Column" }
        columns[fieldName] = ColumnMetaData(fieldName, columnAnnotation)
    }

    private enum class AnnoType { FIELD, METHOD }

    companion object {
        fun getEntityMetaData(entityClass: Class<*>): EntityMetaData? {
            if (entityClass.isAnnotationPresent(jakarta.persistence.Entity::class.java)) {
                return EntityMetaData(entityClass)
            }
            log.info { "EntityMetaData for class $entityClass not found. OK, if no Hibernate entity." }
            return null
        }
    }
}

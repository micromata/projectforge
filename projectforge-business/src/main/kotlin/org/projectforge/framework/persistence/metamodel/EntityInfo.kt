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
import java.lang.reflect.Field
import java.lang.reflect.Method


/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class EntityInfo(
    val entityClass: Class<*>,
    val name: String,
    val entityType: EntityType<*>,
    val tableName: String? = null,
) {
    private val columns = mutableListOf<ColumnInfo>()
    private val columnWithoutLength = mutableSetOf<String>()

    init {
        entityType.attributes.forEach { attr ->
            val member = attr.javaMember ?: return@forEach
            val ann = if (member is Field) {
                member.getAnnotation(Column::class.java)
            } else if (member is Method) {
                member.getAnnotation(Column::class.java)
            } else {
                null
            }
            if (ann != null) {
                columns.add(
                    ColumnInfo(
                        propertyName = attr.name,
                        columnName = ann.name,
                        length = ann.length,
                        nullable = ann.nullable,
                        scale = ann.scale,
                        precision = ann.precision,
                    )
                )
            }
        }
    }

    fun getColumnLength(name: String): Int? {
        val length = columns.find { it.propertyName == name || it.columnName == name }?.length
        if (length != null) {
            return length
        }
        if (columnWithoutLength.contains(name)) {
            return null
        }
        columnWithoutLength.add(name)
        val msg = ("Could not find persistent class for entityName '$name' (OK for non hibernate objects).")
        if (name.endsWith("DO")) {
            log.error(msg)
        } else {
            log.info(msg)
        }
        return null
    }

    override fun toString(): String {
        return JsonUtils.toJson(this)
    }
}

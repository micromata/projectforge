package org.projectforge.framework.persistence.metamodel

import jakarta.persistence.Column
import jakarta.persistence.metamodel.EntityType
import org.hibernate.query.sqm.tree.SqmNode.log
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
}

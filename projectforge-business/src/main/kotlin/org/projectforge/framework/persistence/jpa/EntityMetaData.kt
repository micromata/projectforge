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
}

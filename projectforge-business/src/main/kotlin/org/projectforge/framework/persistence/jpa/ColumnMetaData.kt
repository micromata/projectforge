package org.projectforge.framework.persistence.jpa

import jakarta.persistence.Column
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class ColumnMetaData(fieldName: String, columnAnnotation: Column) {
    val name: String

    val length: Int = columnAnnotation.length

    val nullable = columnAnnotation.nullable

    init {
        if (columnAnnotation.name.isBlank()) {
            name = fieldName
        } else {
            name = columnAnnotation.name
        }
    }
}

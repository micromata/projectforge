package org.projectforge.framework.persistence.jpa.candh

import java.lang.reflect.Field

/**
 * Used for java.sql.Date.
 */
class SqlDateHandler : DefaultHandler() {
    override fun accept(field: Field): Boolean {
        return field.type == java.sql.Date::class.java
    }

    override fun fieldValuesEqual(srcFieldValue: Any, destFieldValue: Any): Boolean {
        val srcDay = (srcFieldValue as java.sql.Date).toLocalDate()
        val destDay = (destFieldValue as java.sql.Date).toLocalDate()
        return srcDay == destDay
    }
}

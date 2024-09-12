package org.projectforge.framework.persistence.jpa.candh

import java.lang.reflect.Field

/**
 * Used for java.util.Date.
 */
class UtilDateHandler : DefaultHandler() {
    override fun accept(field: Field): Boolean {
        return field.type == java.util.Date::class.java
    }

    override fun fieldValuesEqual(srcFieldValue: Any, destFieldValue: Any): Boolean {
        val srcTime = (srcFieldValue as java.util.Date).time
        val destTime = (destFieldValue as java.util.Date).time
        return srcTime == destTime
    }
}

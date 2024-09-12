package org.projectforge.framework.persistence.jpa.candh

import java.lang.reflect.Field
import java.math.BigDecimal

/**
 * Used for BigDecimal, ignores the scale on comparison.
 */
class BigDecimalHandler : DefaultHandler() {
    override fun accept(field: Field): Boolean {
        return field.type == BigDecimal::class.java
    }

    override fun fieldValuesEqual(srcFieldValue: Any, destFieldValue: Any): Boolean {
        srcFieldValue as BigDecimal
        destFieldValue as BigDecimal
        return srcFieldValue.compareTo(destFieldValue) == 0
    }
}

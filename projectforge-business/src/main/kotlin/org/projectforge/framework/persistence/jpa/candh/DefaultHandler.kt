package org.projectforge.framework.persistence.jpa.candh

import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.jpa.candh.CandHMaster.setModificationStatusOnChange
import java.io.Serializable
import java.lang.reflect.Field
import java.time.LocalDate

/**
 * Used for primitive types, String, Integer, LocalDate etc. Simply sets the destFieldValue to srcFieldalue if not equals.
 */
open class DefaultHandler : CandHIHandler {
    override fun accept(field: Field): Boolean {
        return true
    }

    override fun <IdType : Serializable> process(
        srcClazz: Class<*>,
        src: BaseDO<IdType>,
        dest: BaseDO<IdType>,
        field: Field,
        fieldName: String,
        srcFieldValue: Any?,
        destFieldValue: Any?,
        context: CandHContext,
    ): Boolean {
        var modified = false
        if (destFieldValue == null || srcFieldValue == null) {
            if (destFieldValue != srcFieldValue) {
                modified = true
            }
        } else if (!fieldValuesEqual(srcFieldValue, destFieldValue)) {
            modified = true
        }
        if (modified) {
            context.debugContext?.add(
                "$srcClazz.$fieldName",
                srcVal = srcFieldValue,
                destVal = destFieldValue,
                msg = "Field of type ${field.type} modified.",
            )
            synchronized(field) {
                val wasAccessible = field.canAccess(dest)
                try {
                    field.isAccessible = true
                    field[dest] = srcFieldValue
                } finally {
                    field.isAccessible = wasAccessible
                }
            }
            setModificationStatusOnChange(context, src, fieldName)
        }
        return true
    }

    open fun fieldValuesEqual(srcFieldValue: Any, destFieldValue: Any): Boolean {
        return srcFieldValue == destFieldValue
    }
}

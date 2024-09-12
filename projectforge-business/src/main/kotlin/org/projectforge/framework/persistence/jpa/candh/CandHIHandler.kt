package org.projectforge.framework.persistence.jpa.candh

import org.projectforge.framework.persistence.api.BaseDO
import java.io.Serializable
import java.lang.reflect.Field

interface CandHIHandler {
    /**
     * Checks if the field is accepted by this handler.
     */
    fun accept(field: Field): Boolean

    /**
     * @return true if the field was process, false if the next handler should be tried.
     */
    fun <IdType : Serializable> process(
        srcClazz: Class<*>,
        src: BaseDO<IdType>,
        dest: BaseDO<IdType>,
        field: Field,
        fieldName: String,
        srcFieldValue: Any?,
        destFieldValue: Any?,
        context: CandHContext,
    ): Boolean
}

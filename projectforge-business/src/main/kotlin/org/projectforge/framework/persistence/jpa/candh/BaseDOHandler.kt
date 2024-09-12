package org.projectforge.framework.persistence.jpa.candh

import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.HibernateUtils
import java.lang.reflect.Field

/**
 * Used for objects of type BaseDO.
 */
class BaseDOHandler : DefaultHandler() {
    override fun accept(field: Field): Boolean {
        return BaseDO::class.java.isAssignableFrom(field.type)
    }

    override fun fieldValuesEqual(srcFieldValue: Any, destFieldValue: Any): Boolean {
        val srcFieldValueId = HibernateUtils.getIdentifier(srcFieldValue)
        val destFieldValueId = HibernateUtils.getIdentifier(destFieldValue)
        return srcFieldValueId == destFieldValueId
    }
}

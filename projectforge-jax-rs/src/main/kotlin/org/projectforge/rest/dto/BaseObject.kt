package org.projectforge.rest.dto

import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.TenantDO
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.util.*

open class BaseObject<T : DefaultBaseDO>(var id: Int? = null,
                                         var created: Date? = null,
                                         var isDeleted: Boolean? = null,
                                         var lastUpdate: Date? = null,
                                         var tenantId: Int? = null) {
    /**
     * Full and deep copy of the object. Should be extended by inherited classes.
     */
    open fun copyFrom(src: T) {
        id = src.id
        created = src.created
        isDeleted = src.isDeleted
        lastUpdate = src.lastUpdate
        tenantId = src.tenantId
        copy(src, this)
    }

    /**
     * Full and deep copy of the object. Should be extended by inherited classes.
     */
    open fun copyTo(dest: T) {
        dest.id = id
        dest.created = created
        dest.isDeleted = isDeleted == true
        dest.lastUpdate = lastUpdate
        if (tenantId != null) {
            dest.tenant = TenantDO()
            dest.tenant.id = tenantId
        }
        copy(this, dest)
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(BaseObject::class.java)

        private fun copy(src: Any, dest: Any) {
            val fields = dest.javaClass.getDeclaredFields()
            AccessibleObject.setAccessible(fields, true)
            val srcClazz = src.javaClass
            fields.forEach { field ->
                val type = field.type
                var srcField: Field? = null
                if (field.name != "log" && field.name != "serialVersionUID" && field.name != "Companion") {
                    try {
                        srcField = srcClazz.getDeclaredField(field.name)
                        if (srcField != null && srcField.type == type && !Collection::class.java.isAssignableFrom(type)) {
                            srcField.setAccessible(true);
                            field.setAccessible(true);
                            field.set(dest, srcField.get(src))
                        }
                    } catch (ex: Exception) {
                        log.error("Error while copiing field '${field.name}' from $srcClazz to ${dest.javaClass}: ${ex.message}")
                    }
                }
            }
        }
    }

    /**
     * Copy only minimal fields. Id at default, if not overridden.
     */
    open fun copyFromMinimal(src: T) {
        id = src.id
    }
}

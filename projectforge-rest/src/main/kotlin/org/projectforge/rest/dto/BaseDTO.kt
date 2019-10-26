/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.rest.dto

import org.projectforge.common.BeanHelper
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO
import org.projectforge.framework.persistence.user.entities.TenantDO
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.util.*

/**
 * BaseHistorizableDTO is a DTO representation of a AbstractHistorizableBaseDO<Int>. It copies most fields automatically by name and type from
 * DTO to  AbstractHistorizableBaseDO<Int> and vice versa.
 */
open class BaseDTO<T : ExtendedBaseDO<Int>>(var id: Int? = null,
                                            var deleted: Boolean = false,
                                            var created: Date? = null,
                                            var lastUpdate: Date? = null,
                                            var tenant: Tenant? = null) {

    /**
     * Full and deep copy of the object. Should be extended by inherited classes.
     */
    open fun copyFrom(src: T) {
        copy(src, this)
        this.tenant = createTenant(src.tenantId)
    }

    /**
     * Full and deep copy of any other object, if needed.
     */
    open fun copyFromAny(src: Any) {
        copy(src, this)
        if (src is BaseDO<*>) {
            this.tenant = createTenant(src.tenantId)
        }
    }

    /**
     * Full and deep copy of the object. Should be extended by inherited classes.
     */
    open fun copyTo(dest: T) {

        val tenantId = tenant?.id
        if (tenantId != null) {
            dest.tenant = TenantDO()
            dest.tenant.id = tenantId
        }
        copy(this, dest)
    }

    /**
     * Copy only minimal fields. Id at default, if not overridden. This method is usally used for embedded objects.
     */
    open fun copyFromMinimal(src: T) {
        id = src.id
        deleted = src.isDeleted
    }

    private fun _copyFromMinimal(src: Any?) {
        if (src == null) {
            // Nothing to copy
            return
        }
        @Suppress("UNCHECKED_CAST")
        copyFromMinimal(src as T)
    }

    private fun createTenant(id: Int?) : Tenant? {
        if (id == null) {
            return null
        }
        val tenant = Tenant()
        tenant.id = id
        return tenant
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(BaseDTO::class.java)

        private fun copy(src: Any, dest: Any) {
            val destClazz = dest.javaClass
            val destFields = BeanHelper.getAllDeclaredFields(destClazz)
            AccessibleObject.setAccessible(destFields, true)
            val srcClazz = src.javaClass
            destFields.forEach { destField ->
                val destType = destField.type
                var srcField: Field? = null
                if (destField.name != "log"
                        && destField.name != "serialVersionUID"
                        && destField.name != "Companion"
                        && !destField.name.startsWith("$")) {
                    // Fields log, serialVersionUID, Companion and $* may result in Exceptions and shouldn't be copied in any case.
                    try {
                        srcField = BeanHelper.getDeclaredField(srcClazz, destField.name)
                    } catch (ex: Exception) {
                        log.debug("srcField named '${destField.name}' not found in class '$srcClazz'. Can't copy it to destination of type '$destClazz'. Ignoring...")
                    }
                    try {
                        if (srcField != null) {
                            if (srcField.type == destType) {
                                if (Collection::class.java.isAssignableFrom(destType)) {
                                    // Do not copy collections automatically (for now).
                                } else {
                                    srcField.isAccessible = true
                                    destField.isAccessible = true
                                    destField.set(dest, srcField.get(src))
                                }
                            } else {
                                if (BaseDTO::class.java.isAssignableFrom(destType) && AbstractHistorizableBaseDO::class.java.isAssignableFrom(srcField.type)) {
                                    // Copy AbstractHistorizableBaseDO -> BaseObject
                                    srcField.isAccessible = true
                                    val srcValue = srcField.get(src)
                                    if (srcValue != null) {
                                        val instance = destType.newInstance()
                                        (instance as BaseDTO<*>)._copyFromMinimal(srcValue)
                                        destField.isAccessible = true
                                        destField.set(dest, instance)
                                    }
                                } else if (BaseDO::class.java.isAssignableFrom(destType) && BaseDTO::class.java.isAssignableFrom(srcField.type)) {
                                    // Copy BaseObject -> AbstractHistorizableBaseDO
                                    srcField.isAccessible = true
                                    val srcValue = srcField.get(src)
                                    if (srcValue != null) {
                                        val instance = destType.newInstance()
                                        (instance as BaseDO<*>).id = (srcValue as BaseDTO<*>).id
                                        destField.isAccessible = true
                                        destField.set(dest, instance)
                                    }
                                } else {
                                    if (srcField.type.isPrimitive) { // boolean, ....
                                        var value: Any? = null
                                        if (srcField.type == kotlin.Boolean::class.java) {
                                            srcField.isAccessible = true
                                            value = (srcField.get(src) == true)
                                        } else {
                                            log.error("Unsupported field to copy from '$srcClazz.${destField.name}' of type '${srcField.type.name}' to '$destClazz.${destField.name}' of type '${destType.name}'.")
                                        }
                                        if (value != null) {
                                            destField.isAccessible = true
                                            destField.set(dest, value)
                                        }
                                    } else {
                                        log.debug("Unsupported field to copy from '$srcClazz.${destField.name}' of type '${srcField.type.name}' to '$destClazz.${destField.name}' of type '${destType.name}'.")
                                    }
                                }
                            }
                        } else {
                            // srcField not found. Can't copy.
                            log.debug("srcField named '${destField.name}' not found in class '$srcClazz'. Can't copy it to destination of type '$destClazz'.")
                        }
                    } catch (ex: Exception) {
                        log.error("Error while copiing field '${destField.name}' from $srcClazz to ${dest.javaClass}: ${ex.message}", ex)
                    }
                }
            }
        }
    }
}

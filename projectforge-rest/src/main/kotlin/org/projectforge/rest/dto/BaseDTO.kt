/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.projectforge.common.BeanHelper
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * BaseHistorizableDTO is a DTO representation of a AbstractHistorizableBaseDO<Long>. It copies most fields automatically by name and type from
 * DTO to  AbstractHistorizableBaseDO<Long> and vice versa.
 */
open class BaseDTO<T : ExtendedBaseDO<Long>>(
    override var id: Long? = null,
    var deleted: Boolean = false,
    var created: Date? = null,
    var lastUpdate: Date? = null,
    /**
     * Needed for updating UILayout for watchfields (uid of "old" layout will be restored.
     */
    var layoutUid: String? = null,
) : IdObject<Long> {

    /**
     * @see org.projectforge.framework.persistence.history.HistoryEntryDO.userComment
     */
    var historyUserComment: String? = null

    /**
     * Full and deep copy of the object. Should be extended by inherited classes.
     */
    open fun copyFrom(src: T) {
        copy(src, this)
    }

    /**
     * Full and deep copy of any other object, if needed.
     */
    open fun copyFromAny(src: Any) {
        copy(src, this)
    }

    /**
     * Full and deep copy of the object. Should be extended by inherited classes.
     */
    open fun copyTo(dest: T) {
        copy(this, dest)
    }

    /**
     * Copy only minimal fields. Id at default, if not overridden. This method is usually used for embedded objects.
     */
    open fun copyFromMinimal(src: T) {
        id = src.id
        deleted = src.deleted
    }

    /**
     * Copy only the fields a list row of the hand built projectforge-next page shows — the third filling of
     * a DTO, between [copyFromMinimal] and [copyFrom].
     *
     * The list of an entity costs what its rows weigh, and a row weighs what the edit form needs: nested
     * DTOs behind a column showing one name, every sum twice (raw and pre-formatted), comments and access
     * flags no column reads. The order book sent 12.5 MB over 7132 rows that way; filling only its 19
     * columns leaves 5.3 MB (1.5 MB -> 549 KB gzipped), and `JsonInclude.Include.NON_NULL` keeps the
     * rest off the wire.
     *
     * Overriding it is what opts an entity into the lean row (see
     * [org.projectforge.rest.core.AbstractPagesRest.createListRow], which calls it for a next client). The
     * default is the full copy, so an entity whose DTO is small enough — a book, a cost unit — needs nothing.
     *
     * Why not a row class of its own (as `ListAddress` is): the client's row type stays one projection of
     * one DTO, `id`/`deleted` stay where the framework reads them, and a column added to a next page is one
     * line here instead of a new class plus its `getId` override.
     *
     * An override has to fill [created] and [lastUpdate] as well as the columns of its page: every next list
     * offers those two as columns of its own, hidden until the user switches them on
     * (`lib/page-def/audit-columns.ts`), so a row leaving them out shows an empty column instead of a date.
     * That is what [copyAuditFieldsFrom] is for.
     */
    open fun copyFrom4ListRow(src: T) {
        copyFrom(src)
    }

    /**
     * The two timestamps every list offers as a column, for an override of [copyFrom4ListRow] — see there.
     */
    protected fun copyAuditFieldsFrom(src: T) {
        created = src.created
        lastUpdate = src.lastUpdate
    }

    private fun _copyFromMinimal(src: Any?) {
        if (src == null) {
            // Nothing to copy
            return
        }
        @Suppress("UNCHECKED_CAST")
        copyFromMinimal(src as T)
    }

    companion object {
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
                    && !destField.name.startsWith("$")
                ) {
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
                                if (BaseDTO::class.java.isAssignableFrom(destType) && AbstractHistorizableBaseDO::class.java.isAssignableFrom(
                                        srcField.type
                                    )
                                ) {
                                    // Copy AbstractHistorizableBaseDO -> BaseObject
                                    srcField.isAccessible = true
                                    val srcValue = srcField.get(src)
                                    if (srcValue != null) {
                                        val instance = destType.getDeclaredConstructor().newInstance()
                                        (instance as BaseDTO<*>)._copyFromMinimal(srcValue)
                                        destField.isAccessible = true
                                        destField.set(dest, instance)
                                    }
                                } else if (BaseDO::class.java.isAssignableFrom(destType) && BaseDTO::class.java.isAssignableFrom(
                                        srcField.type
                                    )
                                ) {
                                    // Copy BaseObject -> AbstractHistorizableBaseDO
                                    srcField.isAccessible = true
                                    val srcValue = srcField.get(src)
                                    if (srcValue != null) {
                                        val instance = destType.getDeclaredConstructor().newInstance()
                                        @Suppress("UNCHECKED_CAST")
                                        (instance as BaseDO<Long>).id = (srcValue as BaseDTO<*>).id
                                        destField.isAccessible = true
                                        destField.set(dest, instance)
                                    }
                                } else {
                                    if (srcField.type.isPrimitive) { // boolean, ....
                                        var value: Any? = null
                                        @Suppress("RemoveRedundantQualifierName")
                                        if (srcField.type == kotlin.Boolean::class.java) { // kotlin.Boolean needed (or not?)
                                            srcField.isAccessible = true
                                            value = (srcField.get(src) == true)
                                        } else {
                                            log.error("Unsupported field to copy from '$srcClazz.${destField.name}' of type '${srcField.type.name}' to '$destClazz.${destField.name}' of type '${destType.name}'.")
                                        }
                                        if (value != null) {
                                            destField.isAccessible = true
                                            destField.set(dest, value)
                                        }
                                    } else if (destField.type.isPrimitive) { // boolean, ....
                                        @Suppress("RemoveRedundantQualifierName")
                                        if (destField.type == kotlin.Boolean::class.java) { // kotlin.Boolean needed (or not?)
                                            srcField.isAccessible = true
                                            val value = srcField.get(src)
                                            destField.isAccessible = true
                                            destField.set(dest, value == true)
                                        } else {
                                            log.error("Unsupported field to copy from '$srcClazz.${destField.name}' of type '${srcField.type.name}' to '$destClazz.${destField.name}' of type '${destType.name}'.")
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
                        log.error(
                            "Error while copying field '${destField.name}' from $srcClazz to ${dest.javaClass}: ${ex.message}",
                            ex
                        )
                    }
                }
            }
        }
    }
}

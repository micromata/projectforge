/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.jpa

import mu.KotlinLogging
import org.apache.commons.lang3.ClassUtils
import org.hibernate.Hibernate
import org.hibernate.collection.spi.PersistentSet
import org.hibernate.proxy.HibernateProxy
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.api.PFPersistancyBehavior
import org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO
import org.projectforge.framework.persistence.history.HistoryService
import org.projectforge.framework.time.PFDay
import java.io.Serializable
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}

object CopyAndHistorySupport {
    class Context(
        var currentCopyStatus: EntityCopyStatus = EntityCopyStatus.NONE,
        createHistory: Boolean = true,
        debug: Boolean = false,
    ) {
        internal val debugContext = if (debug) CopyAndHistoryDebugContext() else null
        internal val historyContext = if (createHistory) HistoryContext() else null
        fun combine(status: EntityCopyStatus): EntityCopyStatus {
            val newStatus = currentCopyStatus.combine(status)
            debugContext?.let {
                if (newStatus != currentCopyStatus) {
                    it.add(msg = "Status changed from $currentCopyStatus to $newStatus")
                }
            }
            currentCopyStatus = newStatus
            return currentCopyStatus
        }
    }

    fun <IdType : Serializable> copyValues(
        src: BaseDO<IdType>,
        dest: BaseDO<IdType>,
        vararg ignoreFields: String,
    ): Context {
        val context = Context()
        copyValues(src = src, dest = dest, context = context, ignoreFields = ignoreFields)
        return context
    }

    fun <T : Serializable> copyValues(
        src: BaseDO<T>,
        dest: BaseDO<T>,
        context: Context,
        vararg ignoreFields: String
    ) {
        if (!ClassUtils.isAssignable(src.javaClass, dest.javaClass)) {
            throw RuntimeException(
                ("Try to copyValues from different BaseDO classes: this from type "
                        + dest.javaClass.name
                        + " and src from type"
                        + src.javaClass.name
                        + "!")
            )
        }
        if (src.id != null && ignoreFields.contains("id")) {
            context.debugContext?.add("id", srcVal = src.id, destVal = dest.id)
            dest.id = src.id
        }
        Hibernate.initialize(src)
        var useSrc: BaseDO<out Serializable> = src
        if (src is HibernateProxy) {
            useSrc = (src as HibernateProxy).hibernateLazyInitializer
                .implementation as BaseDO<*>
        }
        copyDeclaredFields(
            useSrc.javaClass,
            src = src,
            dest = dest,
            context = context,
            ignoreFields = ignoreFields,
        )
    }

    fun <IdType : Serializable> copyDeclaredFields(
        srcClazz: Class<*>,
        src: BaseDO<IdType>,
        dest: BaseDO<IdType>,
        context: Context,
        vararg ignoreFields: String
    ) {
        context.debugContext?.add(msg = "Processing class $srcClazz")
        val fields = srcClazz.declaredFields
        AccessibleObject.setAccessible(fields, true)
        for (field in fields) {
            val fieldName = field.name
            if (ignoreFields.contains(fieldName)) {
                context.debugContext?.add("$srcClazz.$fieldName", msg = "Ignoring field in list of ignoreFields.")
                continue
            }
            if (!accept(field)) {
                context.debugContext?.add("$srcClazz.$fieldName", msg = "Ignoring field, not accepted.")
                continue
            }
            try {
                val srcFieldValue = field[src]
                val destFieldValue = field[dest]
                if (field.type.isPrimitive) {
                    if (destFieldValue != srcFieldValue) {
                        context.debugContext?.add("$srcClazz.$fieldName", msg = "Primitive field modified.")
                        field[dest] = srcFieldValue
                        setModificationStatusOnChange(context, src, fieldName)
                    }
                    continue
                } else if (srcFieldValue == null) {
                    if (field.type == String::class.java) {
                        if (!(destFieldValue as? String).isNullOrEmpty()) {
                            context.debugContext?.add(
                                "$srcClazz.$fieldName",
                                srcVal = srcFieldValue,
                                destVal = destFieldValue,
                                msg = "Setting to null.",
                            )
                            field[dest] = null
                            setModificationStatusOnChange(context, src, fieldName)
                        }
                    } else if (destFieldValue != null) {
                        if (destFieldValue is Collection<*> && destFieldValue.isEmpty()) {
                            // dest is an empty collection, so no MAJOR update.
                            context.debugContext?.add(
                                "$srcClazz.$fieldName",
                                srcVal = srcFieldValue,
                                msg = "destFieldValue is empty, do nothing instead of setting to null."
                            )
                        } else {
                            field[dest] = null
                            context.debugContext?.add(
                                "$srcClazz.$fieldName",
                                srcVal = srcFieldValue,
                                destVal = "<not empty collection>"
                            )
                            setModificationStatusOnChange(context, src, fieldName)
                        }
                    } else {
                        // dest was already null
                    }
                } else if (srcFieldValue is Collection<*>) {
                    var destColl = destFieldValue as? MutableCollection<Any?>
                    val toRemove = mutableListOf<Any>()
                    if (destColl == null) {
                        if (srcFieldValue is TreeSet<*>) {
                            destColl = TreeSet()
                            context.debugContext?.add(
                                "$srcClazz.$fieldName",
                                srcVal = srcFieldValue,
                                msg = "Creating TreeSet as destFieldValue.",
                            )
                        } else if (srcFieldValue is HashSet<*>) {
                            destColl = HashSet()
                            context.debugContext?.add(
                                "$srcClazz.$fieldName",
                                srcVal = srcFieldValue,
                                msg = "Creating HashSet as destFieldValue.",
                            )
                        } else if (srcFieldValue is List<*>) {
                            destColl = ArrayList()
                            context.debugContext?.add(
                                "$srcClazz.$fieldName",
                                srcVal = srcFieldValue,
                                msg = "Creating ArrayList as destFieldValue.",
                            )
                        } else if (srcFieldValue is PersistentSet<*>) {
                            destColl = HashSet()
                            context.debugContext?.add(
                                "$srcClazz.$fieldName",
                                srcVal = srcFieldValue,
                                msg = "Creating HashSet as destFieldValue. srcFieldValue is PersistentSet.",
                            )
                        } else {
                            log.error("Unsupported collection type: " + srcFieldValue.javaClass.name)
                        }
                        field[dest] = destColl
                    }
                    if (destColl != null) { // destColl can be null if the collection type is not supported.
                        destColl.forEach { o ->
                            o?.let {
                                if (!srcFieldValue.contains(it)) {
                                    toRemove.add(it)
                                }
                            }
                        }
                        toRemove.forEach { o ->
                            log.debug { "Removing collection entry: $o" }
                            destColl.remove(o)
                            context.debugContext?.add(
                                "$srcClazz.$fieldName",
                                msg = "Removing entry $o from destFieldValue.",
                            )
                            setModificationStatusOnChange(context, src, fieldName)
                        }
                        srcFieldValue.forEach { srcEntry ->
                            if (!destColl.contains(srcEntry)) {
                                log.debug { "Adding new collection entry: $srcEntry" }
                                destColl.add(srcEntry)
                                context.debugContext?.add(
                                    "$srcClazz.$fieldName",
                                    msg = "Adding entry $srcEntry to destFieldValue.",
                                )
                                setModificationStatusOnChange(context, src, fieldName)
                            } else if (srcEntry is BaseDO<*>) {
                                val behavior = field.getAnnotation(
                                    PFPersistancyBehavior::class.java
                                )
                                context.debugContext?.add("$srcClazz.$fieldName", msg = "srcEntry of src-collection is BaseDO. autoUpdateCollectionEntres = ${behavior?.autoUpdateCollectionEntries == true}")
                                if (behavior != null && behavior.autoUpdateCollectionEntries) {
                                    var destEntry: BaseDO<*>? = null
                                    for (entry in destColl) {
                                        if (entry == srcEntry) {
                                            destEntry = entry as BaseDO<*>
                                            break
                                        }
                                    }
                                    requireNotNull(destEntry)
                                    log.error { "*********** TODO: To migrate" }
                                    // val newContext = copyValues(srcEntry, destEntry)
                                    val st = destEntry.copyValuesFrom(srcEntry)
                                    context.combine(st)
                                }
                            }
                        }
                    }
                } else if (srcFieldValue is BaseDO<*>) {
                    context.debugContext?.add("$srcClazz.$fieldName", msg = "srcFieldValue is BaseDO.")
                    val srcFieldValueId = HibernateUtils.getIdentifier(srcFieldValue)
                    if (srcFieldValueId != null) {
                        if (destFieldValue == null
                            || srcFieldValueId != (destFieldValue as BaseDO<*>).id
                        ) {
                            context.debugContext?.add("$srcClazz.$fieldName", srcVal = srcFieldValue, destVal = destFieldValue)
                            field[dest] = srcFieldValue
                            setModificationStatusOnChange(context, src, fieldName)
                        }
                    } else {
                        log.error(
                            ("Can't get id though can't copy the BaseDO (see error message above about HHH-3502), or id not given for "
                                    + srcFieldValue.javaClass + ": " + ToStringUtil.toJsonString(srcFieldValue))
                        )
                    }
                } else if (srcFieldValue is LocalDate) {
                    if (destFieldValue == null) {
                        context.debugContext?.add("$srcClazz.$fieldName", srcVal = srcFieldValue, destVal = destFieldValue, msg = "LocalDate")
                        field[dest] = srcFieldValue
                        setModificationStatusOnChange(context, src, fieldName)
                    } else {
                        val srcDay = PFDay.from(srcFieldValue)
                        val destDay = PFDay.from(destFieldValue as LocalDate)
                        if (!srcDay.isSameDay(destDay)) {
                            field[dest] = srcDay.localDate
                            context.debugContext?.add("$srcClazz.$fieldName", srcVal = srcDay.localDate, destVal = destDay.localDate, msg = "LocalDate")
                            setModificationStatusOnChange(context, src, fieldName)
                        }
                    }
                } else if (srcFieldValue is java.sql.Date) {
                    if (destFieldValue == null) {
                        context.debugContext?.add("$srcClazz.$fieldName", srcVal = srcFieldValue, destVal = destFieldValue, msg = "java.sql.Date")
                        field[dest] = srcFieldValue
                        setModificationStatusOnChange(context, src, fieldName)
                    } else {
                        val srcDay = PFDay.from(srcFieldValue)
                        val destDay = PFDay.from(destFieldValue as java.util.Date)
                        if (!srcDay.isSameDay(destDay)) {
                            context.debugContext?.add("$srcClazz.$fieldName", srcVal = srcDay, destVal = destDay, msg = "java.sql.Date")
                            field[dest] = srcDay.sqlDate
                            setModificationStatusOnChange(context, src, fieldName)
                        }
                    }
                } else if (srcFieldValue is java.util.Date) {
                    if (destFieldValue == null || srcFieldValue.time != (destFieldValue as java.util.Date).time) {
                        context.debugContext?.add("$srcClazz.$fieldName", srcVal = srcFieldValue, destVal = destFieldValue, msg = "java.util.Date")
                        field[dest] = srcFieldValue
                        setModificationStatusOnChange(context, src, fieldName)
                    }
                } else if (srcFieldValue is BigDecimal) {
                    if (destFieldValue == null || srcFieldValue.compareTo(destFieldValue as BigDecimal) != 0) {
                        context.debugContext?.add("$srcClazz.$fieldName", srcVal = srcFieldValue, destVal = destFieldValue, msg = "BigDecimal")
                        field[dest] = srcFieldValue
                        setModificationStatusOnChange(context, src, fieldName)
                    }
                } else if (destFieldValue != srcFieldValue) {
                    context.debugContext?.add("$srcClazz.$fieldName", srcVal = srcFieldValue, destVal = destFieldValue)
                    field[dest] = srcFieldValue
                    setModificationStatusOnChange(context, src, fieldName)
                }
            } catch (ex: IllegalAccessException) {
                throw InternalError("Unexpected IllegalAccessException: " + ex.message)
            }
        }
        val superClazz = srcClazz.superclass
        if (superClazz != null) {
            copyDeclaredFields(superClazz, src = src, dest = dest, context = context, ignoreFields = ignoreFields)
        }
    }

    /**
     * Field was modified, so set the modification status to MAJOR or, if not historizable to MINOR.
     */
    internal fun <IdType : Serializable> setModificationStatusOnChange(
        context: Context,
        src: BaseDO<IdType>,
        modifiedField: String
    ) {
        if (HistoryService.get().isNoHistoryProperty(src.javaClass, modifiedField)) {
            // This field is not historized, so no major update:
            context.combine(EntityCopyStatus.MINOR)
            return
        }
        if (context.currentCopyStatus == EntityCopyStatus.MAJOR || src !is AbstractHistorizableBaseDO<*> || src !is HibernateProxy) {
            context.combine(EntityCopyStatus.MAJOR) // equals to context.currentCopyStatus=MAJOR.
        }
        context.combine(EntityCopyStatus.NONE)
    }

    /**
     * Returns whether to append the given `Field`.
     *
     *  * Ignore transient fields
     *  * Ignore static fields
     *  * Ignore inner class fields
     *
     *
     * @param field The Field to test.
     * @return Whether to consider the given `Field`.
     */
    internal fun accept(field: Field): Boolean {
        if (field.name.indexOf(ClassUtils.INNER_CLASS_SEPARATOR_CHAR) != -1) {
            // Reject field from inner class.
            return false
        }
        if (Modifier.isTransient(field.modifiers)) {
            // transients.
            return false
        }
        if (Modifier.isStatic(field.modifiers)) {
            // transients.
            return false
        }
        return true
    }
}

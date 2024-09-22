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

package org.projectforge.framework.persistence.jpa.candh

import mu.KotlinLogging
import org.apache.commons.lang3.ClassUtils
import org.hibernate.Hibernate
import org.hibernate.proxy.HibernateProxy
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO
import org.projectforge.framework.persistence.history.HistoryServiceUtils
import java.io.Serializable
import java.lang.reflect.Member
import java.lang.reflect.Modifier
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KVisibility

private val log = KotlinLogging.logger {}

/**
 * Manages copy and history of database objects. Copy is used for merging objects and history is used for tracking changes.
 */
object CandHMaster {
    /**
     * List of all registeredAdapters. For every field the first matching
     */
    private val registeredHandlers = mutableListOf<CandHIHandler>()

    init {
        // Register all handlers here.
        registeredHandlers.add(BigDecimalHandler())
        registeredHandlers.add(SqlDateHandler())
        registeredHandlers.add(UtilDateHandler())
        registeredHandlers.add(CollectionHandler())
        registeredHandlers.add(BaseDOHandler())
        registeredHandlers.add(DefaultHandler()) // Handles everything else.
    }

    fun <IdType : Serializable> copyValues(
        src: BaseDO<IdType>,
        dest: BaseDO<IdType>,
        vararg ignoreFields: String,
    ): CandHContext {
        val context = CandHContext()
        copyValues(src = src, dest = dest, context = context, ignoreFields = ignoreFields)
        return context
    }

    fun <T : Serializable> copyValues(
        src: BaseDO<T>,
        dest: BaseDO<T>,
        context: CandHContext,
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
        Hibernate.initialize(src)
        var useSrc: BaseDO<out Serializable> = src
        if (src is HibernateProxy) {
            useSrc = (src as HibernateProxy).hibernateLazyInitializer
                .implementation as BaseDO<*>
        }
        copyDeclaredFields(
            useSrc.javaClass.kotlin,
            src = src,
            dest = dest,
            context = context,
            ignoreFields = ignoreFields,
        )
    }

    fun <IdType : Serializable> copyDeclaredFields(
        kClass: KClass<*>,
        src: BaseDO<IdType>,
        dest: BaseDO<IdType>,
        context: CandHContext,
        vararg ignoreFields: String
    ) {
        context.debugContext?.add(msg = "Processing class $kClass")
        kClass.members.filterIsInstance<KMutableProperty1<*, *>>().forEach { property ->
            if (property.setter.visibility != KVisibility.PUBLIC || property.getter.visibility != KVisibility.PUBLIC) {
                log.debug { "Getter and/or setter of property $kClass.${property.name} has not visibility 'public', ignoring it." }
                return@forEach
            }
            property as KMutableProperty1<BaseDO<IdType>, Any?>
            val propertyName = property.name
            if (ignoreFields.contains(propertyName)) {
                context.debugContext?.add("$kClass.$propertyName", msg = "Ignoring field in list of ignoreFields.")
                return@forEach
            }

            if (!accept(property)) {
                context.debugContext?.add("$kClass.$propertyName", msg = "Ignoring field, not accepted.")
                return@forEach
            }
            try {
                val srcFieldValue = property.get(src)
                val destFieldValue = property.get(dest)
                val fieldContext = PropertyContext(
                    kClass = kClass,
                    src = src,
                    dest = dest,
                    propertyName = propertyName,
                    property = property,
                    srcPropertyValue = srcFieldValue,
                    destPropertyValue = destFieldValue,
                )
                var processed = false
                for (handler in registeredHandlers) {
                    if (handler.accept(property)) {
                        if (handler.process(fieldContext, context = context)
                        ) {
                            processed = true
                            break
                        }
                    }
                }
                if (!processed) {
                    log.error { "******** Oups, field $kClass.$propertyName not processed!" }
                }
            } catch (ex: Exception) {
                throw InternalError("Unexpected IllegalAccessException for property $kClass.$propertyName " + ex.message)
            }
        }
    }

    /**
     * Field was modified, so set the modification status to MAJOR or, if not historizable to MINOR
     *
     */
    internal fun <IdType : Serializable> setModificationStatusOnChange(
        context: CandHContext,
        src: BaseDO<IdType>,
        modifiedField: String
    ) {
        if (HistoryServiceUtils.get().isNoHistoryProperty(src.javaClass, modifiedField)) {
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
     * @param member The Field to test.
     * @return Whether to consider the given `Field`.
     */
    internal fun accept(member: KCallable<*>): Boolean {
        if (member.name.indexOf(ClassUtils.INNER_CLASS_SEPARATOR_CHAR) != -1) {
            // Reject field from inner class.
            return false
        }
        if (member is Member) {
            if (Modifier.isTransient(member.modifiers)) {
                // transients.
                return false
            }
            if (Modifier.isStatic(member.modifiers)) {
                // transients.
                return false
            }
        }
        return true
    }
}

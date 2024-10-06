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

package org.projectforge.framework.persistence.candh

import mu.KotlinLogging
import org.apache.commons.lang3.ClassUtils
import org.hibernate.Hibernate
import org.hibernate.proxy.HibernateProxy
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.HistoryServiceUtils
import org.projectforge.framework.persistence.history.PropertyOpType
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
 *
 * Please note: CandHMaster is currently functioning only for Kotlin classes. Java classes are not supported. (See comment in code below.)
 */
object CandHMaster {
    /**
     * List of all registeredAdapters. For every property the first matching handler is used.
     */
    private val registeredHandlers = mutableListOf<CandHIHandler>()

    // private val persistenceService: PfPersistenceService

    init {
        // Register all handlers here.
        registeredHandlers.add(BigDecimalHandler())
        registeredHandlers.add(SqlDateHandler())
        registeredHandlers.add(UtilDateHandler())
        registeredHandlers.add(CollectionHandler())
        registeredHandlers.add(BaseDOHandler())
        registeredHandlers.add(DefaultHandler()) // Handles everything else.
        // persistenceService =
        //    ApplicationContextProvider.getApplicationContext().getBean(PfPersistenceService::class.java)
    }

    /**
     * @param ignoreProperties The names of the properties to ignore.
     */
    fun copyValues(
        src: BaseDO<*>,
        dest: BaseDO<*>,
        vararg ignoreProperties: String,
        entityOpType: EntityOpType? = null,
    ): CandHContext {
        val context = CandHContext(src, entityOpType = entityOpType)
        copyValues(
            src = src,
            dest = dest,
            context = context,
            ignoreProperties = ignoreProperties,
        )
        return context
    }

    /**
    /**
     * @param ignoreProperties The names of the properties to ignore.
    */
     */
    fun copyValues(
        src: BaseDO<*>, dest: BaseDO<*>, context: CandHContext, vararg ignoreProperties: String
    ) {
        val srcClass = HibernateUtils.getRealClass(src)
        if (!org.projectforge.common.ClassUtils.isKotlinClass(srcClass)) {
            // Java classes are not supported.
            log.warn { "******* Java classes ($srcClass) are not supported by CandHMaster (most fields are not processed). See code of CandHMaster for details and how to fix it. It's recommended to convert to Kotlin instead." }
        }
        val destClass = HibernateUtils.getRealClass(dest)
        if (!ClassUtils.isAssignable(srcClass, destClass)) {
            throw RuntimeException(
                ("Try to copyValues from different BaseDO classes: this from type ${destClass.name} and src of type ${srcClass.name}!")
            )
        }
        //try {
        Hibernate.initialize(src)
        var useSrc: BaseDO<out Serializable> = src
        //  context.historyContext?.pushHistoryMaster(useSrc as BaseDO<Long>)
        if (src is HibernateProxy) {
            useSrc = (src as HibernateProxy).hibernateLazyInitializer.implementation as BaseDO<*>
        }
        if (ignoreProperties.none { it == "id" } && BaseDOHandler.idModified(src, dest)) {
            @Suppress("UNCHECKED_CAST")
            (dest as BaseDO<Serializable>).id = src.id
        }
        // We have to ignore the id property in the ignoreProperties list. The id of the dest field can't be changed.
        val ignorePropertiesList = ignoreProperties.toMutableList()
        ignorePropertiesList.add("id")
        val ignorePropertiesArray = ignorePropertiesList.toTypedArray()
        val processedProperties =
            mutableSetOf<String>() // Paranoi check for avoiding handling of the same property twice.
        copyProperties(
            useSrc.javaClass.kotlin,
            src = src,
            dest = dest,
            context = context,
            onlyPersistedProperties = true,
            processedProperties = processedProperties,
            ignoreProperties = ignorePropertiesArray,
        )
        copyProperties(
            useSrc.javaClass.kotlin,
            src = src,
            dest = dest,
            context = context,
            onlyPersistedProperties = false,
            processedProperties = processedProperties,
            ignoreProperties = ignorePropertiesArray,
        )
        //} finally {
        //    context.historyContext?.popHistoryMaster()
        //}
    }

    /**
     * Persisted properties must be copied first, because they may be used in other properties (e. g. [org.projectforge.framework.persistence.user.entities.PFUserDO.firstDayOfWeekValue] is set by
     * [org.projectforge.framework.persistence.user.entities.PFUserDO.firstDayOfWeek]).
     */
    private fun copyProperties(
        kClass: KClass<*>,
        src: BaseDO<*>,
        dest: BaseDO<*>,
        context: CandHContext,
        onlyPersistedProperties: Boolean,
        processedProperties: MutableSet<String>,
        vararg ignoreProperties: String,
    ) {
        context.debugContext?.add(msg = "Processing class $kClass")
        kClass.members.filterIsInstance<KMutableProperty1<*, *>>().forEach { property ->
            // The following check does not work for Java classes, because the visibility of the fields is never 'public'.
            // For support of Java classes, the visibility check must be modified.
            if (property.setter.visibility != KVisibility.PUBLIC || property.getter.visibility != KVisibility.PUBLIC) {
                log.debug { "Getter and/or setter of property $kClass.${property.name} has not visibility 'public', ignoring it." }
                return@forEach
            }
            @Suppress("UNCHECKED_CAST")
            property as KMutableProperty1<BaseDO<*>, Any?>
            val propertyName = property.name
            if (processedProperties.contains(propertyName)) {
                // Don't process properties twice.
                return@forEach
            }
            if (onlyPersistedProperties && !HibernateUtils.isPersistedProperty(kClass, propertyName)) {
                return@forEach
            }
            processedProperties.add(propertyName)
            if (ignoreProperties.contains(propertyName)) {
                context.debugContext?.add("Ignoring property '$kClass.$propertyName' in list of ignoreProperties.")
                return@forEach
            }

            if (!accept(property)) {
                context.debugContext?.add("Ignoring property '$kClass.$propertyName', not accepted.")
                return@forEach
            }
            try {
                val srcValue = property.get(src)
                val destValue = property.get(dest)
                val propertyContext = PropertyContext(
                    kClass = kClass,
                    src = src,
                    dest = dest,
                    propertyName = propertyName,
                    property = property,
                    srcPropertyValue = srcValue,
                    destPropertyValue = destValue,
                )
                var processed = false
                for (handler in registeredHandlers) {
                    if (handler.accept(property)) {
                        if (handler.process(propertyContext, context = context)) {
                            processed = true
                            break
                        }
                    }
                }
                if (!processed) {
                    log.error { "******** Oups, property $kClass.$propertyName not processed!" }
                }
            } catch (ex: Exception) {
                log.error(ex) { "Error processing property $kClass.$propertyName: ${ex.message}" }
                throw InternalError("Unexpected IllegalAccessException for property $kClass.$propertyName " + ex.message)
            }
        }
    }

    /**
     * Property was modified, so set the modification status to MAJOR or, if not historizable to MINOR
     * Will also handle the history entries, if required.
     * @param type If null, no history handling is done (was handled by caller).
     */
    internal fun propertyWasModified(
        context: CandHContext,
        propertyContext: PropertyContext,
        type: PropertyOpType?,
    ) {
        propertyContext.apply {
            if (HistoryServiceUtils.get().isNoHistoryProperty(src.javaClass, propertyName)
                || !HibernateUtils.isPersistedProperty(src.javaClass, propertyName)
            ) {
                // This property is not historized, so no major update:
                context.combine(EntityCopyStatus.MINOR)
                return
            }
            if (context.currentCopyStatus == EntityCopyStatus.MAJOR || src !is AbstractHistorizableBaseDO<*> || src !is HibernateProxy) {
                if (type != null) {
                    handleHistoryEntry(context, propertyContext, type)
                }
                context.combine(EntityCopyStatus.MAJOR) // equals to context.currentCopyStatus=MAJOR.
            }
            context.combine(EntityCopyStatus.NONE)
        }
    }

    private fun handleHistoryEntry(
        context: CandHContext,
        propertyContext: PropertyContext,
        type: PropertyOpType,
    ) {
        if (context.historyContext == null) {
            // No history required.
            return
        }
        propertyContext.apply {
            context.historyContext.add(propertyContext, type)
        }
    }

    /**
     * Returns whether to append the given `Property`.
     *
     *  * Ignore transient properties
     *  * Ignore static properties
     *  * Ignore inner class properties
     *
     *
     * @param property The property to test.
     * @return Whether to consider the given `Property`.
     */
    internal fun accept(property: KCallable<*>): Boolean {
        if (property.name.indexOf(ClassUtils.INNER_CLASS_SEPARATOR_CHAR) != -1) {
            // Reject properties from inner class.
            return false
        }
        if (property is Member) {
            if (Modifier.isTransient(property.modifiers)) {
                // transients.
                return false
            }
            if (Modifier.isStatic(property.modifiers)) {
                // transients.
                return false
            }
        }
        return true
    }
}

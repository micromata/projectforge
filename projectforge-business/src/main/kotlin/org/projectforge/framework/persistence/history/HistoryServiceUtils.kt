/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.history

import mu.KotlinLogging
import org.projectforge.common.BeanHelper

private val log = KotlinLogging.logger {}

/**
 */
class HistoryServiceUtils private constructor() {
    private val noHistoryPropertiesByClass = mutableMapOf<Class<*>, Set<String>>()

    fun isNoHistoryProperty(entityClass: Class<*>, propertyName: String): Boolean {
        return getNoHistoryProperties(entityClass).contains(propertyName)
    }

    /**
     * Returns the set of property names which are marked as NoHistory for the given entity class.
     * The result is cached.
     * @param entityClass the entity class
     * @return the set of property names which are marked as NoHistory
     * @see NoHistory
     */
    fun getNoHistoryProperties(entityClass: Class<*>): Set<String> {
        synchronized(noHistoryPropertiesByClass) {
            noHistoryPropertiesByClass[entityClass]?.let { return it }
        }
        val set = determineNoHistoryProperties(entityClass)
        synchronized(noHistoryPropertiesByClass) {
            noHistoryPropertiesByClass[entityClass] = set
        }
        return set
    }

    private fun determineNoHistoryProperties(entityClass: Class<*>): Set<String> {
        val set = mutableSetOf<String>()
        determineNoHistoryProperties(entityClass, set)
        return set
    }

    private fun determineNoHistoryProperties(entityClass: Class<*>, set: MutableSet<String>) {
        log.debug { "Determining NoHistory properties of class ${entityClass.name}" }
        for (field in entityClass.declaredFields) {
            if (field.isAnnotationPresent(NoHistory::class.java)) {
                log.debug { "NoHistory annotation found for field: ${entityClass.name}.${field.name}" }
                set.add(field.name)
            }
        }

        for (method in entityClass.declaredMethods) {
            if (method.isAnnotationPresent(NoHistory::class.java)) {
                // Annotations are named like the field, but in Kotlin with a suffix $annotations.
                val fieldName = BeanHelper.determinePropertyName(method).removeSuffix("\$annotations")
                log.debug { "NoHistory annotation found for field: ${entityClass.name}.$fieldName (method=${method.name})" }
                set.add(fieldName)
            }
        }
        if (log.isDebugEnabled) {
            log.debug { "NoHistory properties for class ${entityClass.name}: $set" }
        }
        entityClass.superclass?.let { determineNoHistoryProperties(it, set) }
    }

    companion object {
        private val instance = HistoryServiceUtils()

        @JvmStatic
        fun get(): HistoryServiceUtils {
            return instance
        }

        @JvmStatic
        fun isHistorizable(bean: Any?): Boolean {
            return HistoryBaseDaoAdapter.isHistorizable(bean)
        }

        @JvmStatic
        fun isHistorizable(entityClass: Class<*>): Boolean {
            return HistoryBaseDaoAdapter.isHistorizable(entityClass)
        }
    }
}

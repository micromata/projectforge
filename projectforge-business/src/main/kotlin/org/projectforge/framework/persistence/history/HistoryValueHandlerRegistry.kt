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

package org.projectforge.framework.persistence.history

import java.math.BigDecimal

/**
 * Registry for [HistoryValueHandler]s.
 */
internal object HistoryValueHandlerRegistry {
    private class Entry(val handler: HistoryValueHandler<*>, vararg properties: String)

    /**
     * Key is the property type.
     */
    private val registeredHandlers = mutableMapOf<String, HistoryValueHandler<*>>()

    private val defaultHistoryValueHandler = DefaultHistoryValueHandler()

    init {
        // Register all handlers here.
        registerHandler(BigDecimalHistoryValueHandler(), BigDecimal::class.java.name)
        registerHandler(BooleanHistoryValueHandler(), "boolean", java.lang.Boolean::class.java.name)
        registerHandler(LongHistoryValueHandler(), "long", java.lang.Long::class.java.name)
        registerHandler(IntHistoryValueHandler(), "int", java.lang.Integer::class.java.name)
        registerHandler(ShortHistoryValueHandler(), java.lang.Short::class.java.name)
        registerHandler(DateHistoryValueHandler(), java.util.Date::class.java.name, "net.fortuna.ical4j.model.DateTime")
        registerHandler(SqlDateHistoryValueHandler(), java.sql.Date::class.java.name)
        registerHandler(LocalDateHistoryValueHandler(), java.time.LocalDate::class.java.name, "net.fortuna.ical4j.model.Date")
        registerHandler(TimestampHistoryValueHandler(), java.sql.Timestamp::class.java.name)
        registerHandler(ByteArrayHistoryValueHandler(), "[B")
        registerHandler(LocaleHistoryValueHandler(), java.util.Locale::class.java.name)
        registerHandler(VoidHistoryValueHandler(), "void")
    }

    /**
     * @param propertyType The type of the property (as String and not as class due to older history entries (e. g. net.fortuna.ical4j.model.DateTime).
     */
    fun getHandler(propertyType: String): HistoryValueHandler<*> {
        return registeredHandlers[propertyType] ?: defaultHistoryValueHandler
    }

    private fun registerHandler(handler: HistoryValueHandler<*>, vararg propertyTypes: String) {
        propertyTypes.forEach { propertyType ->
            if (registeredHandlers.containsKey(propertyType)) {
                throw IllegalArgumentException("Handler for property type '$propertyType' already registered!")
            }
            registeredHandlers[propertyType] = handler
        }
    }
}

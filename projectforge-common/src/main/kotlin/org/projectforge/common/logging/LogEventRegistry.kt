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

package org.projectforge.common.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import java.lang.reflect.Modifier

private val log = KotlinLogging.logger {}

/**
 * The catalog of all known [LogEvent]s. An `object` holding log events is only initialized on its first use, so every
 * module registers its holders on start-up (a Spring component calling [register]). Thread-safe.
 */
object LogEventRegistry {
    private val events = sortedMapOf<String, LogEvent>()

    /**
     * Registers all [LogEvent] fields of the given holders (usually `object`s like `MailLogEvents`). A code already
     * registered by another event is logged as error and ignored (see [findProblems], checked by unit tests).
     */
    fun register(vararg holders: Any) {
        holders.forEach { holder ->
            eventsOf(holder).forEach { event ->
                synchronized(events) {
                    val existing = events.putIfAbsent(event.code, event)
                    if (existing != null && existing !== event) {
                        log.error { "Log event code '${event.code}' of ${holder::class.java.name} is already registered, ignored." }
                    }
                }
            }
        }
    }

    fun get(code: String?): LogEvent? = code?.let { synchronized(events) { events[it] } }

    /** All registered events, sorted by code. */
    val all: List<LogEvent>
        get() = synchronized(events) { events.values.toList() }

    /**
     * Codes used by more than one event of the given holders, for unit tests of every module.
     * @return Descriptions of the problems, empty if none.
     */
    fun findProblems(vararg holders: Any): List<String> {
        val byCode = holders.flatMap { holder -> eventsOf(holder).map { it to holder::class.java.simpleName } }
            .groupBy({ it.first.code }, { it })
        return byCode.filter { (_, list) -> list.map { it.first }.distinct().size > 1 }
            .map { (code, list) -> "Code '$code' used more than once: ${list.joinToString { it.second }}" }
    }

    /** The [LogEvent] fields (static, as in an `object`, or of the instance). */
    fun eventsOf(holder: Any): List<LogEvent> =
        holder.javaClass.declaredFields.filter { LogEvent::class.java.isAssignableFrom(it.type) }.mapNotNull { field ->
            field.isAccessible = true
            field.get(if (Modifier.isStatic(field.modifiers)) null else holder) as? LogEvent
        }
}

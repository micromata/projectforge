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

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

private val log = KotlinLogging.logger {}

class LoggerMemoryAppender : AppenderBase<ILoggingEvent?>() {
    private var lastLogEntryOrderNumber: Long = -1
    private val logSubscriptions = CopyOnWriteArrayList<LogSubscription>()
    private val listeners = CopyOnWriteArrayList<LogEventListener>()

    private val queue = LogQueue(QUEUE_SIZE)

    private var lastSubscriptionGCRun = System.currentTimeMillis()

    override fun append(event: ILoggingEvent?) {
        val eventData = LoggingEventData(event!!, ++lastLogEntryOrderNumber)
        queue.add(eventData)
        logSubscriptions.forEach { subscription ->
            subscription.processEvent(eventData)
        }
        if (listeners.isNotEmpty() && eventData.level.matches(LogLevel.WARN)) {
            listeners.forEach { listener ->
                try {
                    listener.onEvent(eventData)
                } catch (ex: Throwable) {
                    // Not logged: an error logged here would come back to the failing listener.
                    System.err.println("LogEventListener ${listener::class.java.name} failed: ${ex.message}")
                }
            }
        }
        if (System.currentTimeMillis() - lastSubscriptionGCRun > REGISTERED_SUBSCRIPTION_GC_INTERVAL_MS) {
            runSubscriptionsGC()
        }
    }

    /**
     * Registers a listener for every event of level WARN and above, independent of users and subscriptions
     * (e.g. the support error digest). Called in the logging thread, so it must be fast and must not log at
     * WARN or above itself.
     */
    fun addListener(listener: LogEventListener) {
        listeners.addIfAbsent(listener)
    }

    fun removeListener(listener: LogEventListener) {
        listeners.remove(listener)
    }

    internal fun register(subscription: LogSubscription): LogSubscription {
        log.info { "Registering subscription '${subscription.title}' for user '${subscription.user}'." }
        logSubscriptions.add(subscription)
        return subscription
    }

    internal fun ensureSubscription(
        title: String,
        user: String,
        create: (title: String, user: String) -> LogSubscription,
        displayTitle: String = title,
    ): LogSubscription {
        return getSubscription(title = title, user = user, displayTitle = displayTitle) ?: return register(
            create(
                title,
                user
            )
        )
    }

    @Suppress("UNUSED_PARAMETER")
    internal fun getSubscription(title: String, user: String, displayTitle: String = title): LogSubscription? {
        return logSubscriptions.find { it.title == title && it.user == user }
    }

    internal fun getSubscription(id: Int): LogSubscription? {
        return logSubscriptions.find { it.id == id }
    }

    internal val size
        get() = queue.size

    private fun runSubscriptionsGC() {
        val size = logSubscriptions.size
        if (size == 0) {
            return
        }
        val removed = logSubscriptions.removeIf { it.expired }
        if (removed) {
            log.info { "Expired LogSubscription(s) removed. Number of subscriptions=${logSubscriptions.size}" }
        }
        lastSubscriptionGCRun = System.currentTimeMillis()
    }

    fun query(filter: LogFilter, locale: Locale? = null): List<LoggingEventData> {
        return queue.query(filter, locale)
    }

    companion object {
        private const val QUEUE_SIZE = 10000
        private const val REGISTERED_SUBSCRIPTION_GC_INTERVAL_MS = 10 * 60 * 1000 // run GC every 10 minutes
        private var instance: LoggerMemoryAppender? = null

        fun getInstance(): LoggerMemoryAppender {
            return instance!!
        }

        fun isInitialized(): Boolean {
            return instance != null
        }
    }

    /**
     * Initialized by logback on start-up (see logback-spring.xml).
     */
    init {
        if (instance != null) {
            log.warn { "*** LoggerMemoryAppender instantiated twice! Shouldn't occur. ***" }
        } else {
            instance = this
        }
    }
}

/**
 * See [LoggerMemoryAppender.addListener].
 */
fun interface LogEventListener {
    fun onEvent(event: LoggingEventData)
}

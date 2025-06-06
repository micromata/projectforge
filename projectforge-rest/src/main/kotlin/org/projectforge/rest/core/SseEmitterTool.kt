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

package org.projectforge.rest.core

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.projectforge.business.admin.SystemStatistics
import org.projectforge.common.CoroutineTracker
import org.projectforge.framework.json.JsonUtils
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Tool to send Server-Sent Events (SSE) to the client.
 * @param timeout The timeout in milliseconds for the emitter. Default is 60_000L (1 minute).
 */
abstract class SseEmitterTool(timeout: Long = 60_000L) {
    val emitter = SseEmitter(timeout)
    var lastClientUpdate = Date(0L) // epoch in seconds.

    abstract val lastModified: Date?

    abstract fun getData(): Any?

    /**
     * Launch the coroutine to send data to the client.
     */
    fun launch() {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            coroutineTracker.increment() // Increment the counter.
            try {
                for (i in 0..7_200) { // Limit to 7_200 iterations, meaning 3_600 seconds (1 hour).
                    val last = lastModified
                    if (last != null && last > lastClientUpdate) {
                        val data = getData()
                        // New entries available:
                        val json = JsonUtils.toJson(data)
                        try {
                            log.debug { "Sending data to client" }
                            emitter.send(json) // Send the data to the client.
                        } catch (ex: IOException) {
                            // Connection closed?
                            break
                        }
                        lastClientUpdate = Date()
                    } else if (lastClientUpdate.time < System.currentTimeMillis() - 30_000) {
                        log.debug { "Sending ping to client" }
                        // Send a ping every 30 seconds to keep the connection alive.
                        try {
                            emitter.send("ping")
                        } catch (ex: IOException) {
                            // Connection closed?
                            break
                        }
                        lastClientUpdate = Date()
                    }
                    delay(500) // Wait .5 seconds
                }
                emitter.complete() // Signal that the stream is complete
            } catch (e: Exception) {
                emitter.completeWithError(e)
            }
        }.invokeOnCompletion {
            coroutineTracker.decrement()
        }
        emitter.onCompletion {
            log.debug { "Cancelling coroutine onCompletion." }
            coroutineScope.cancel()
        }
        emitter.onError { ex ->
            log.debug { "Cancelling coroutine onError." }
            coroutineScope.cancel()
        }
    }

    companion object {
        internal val coroutineTracker = CoroutineTracker()

        init {
            SystemStatistics.instance.registerStatisticsBuilder(SseEmitterStatisticsBuilder())
        }
    }
}

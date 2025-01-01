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

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.io.entity.EntityUtils
import kotlin.random.Random


fun main() {
    ParallelUserTestClientMain().run()
    // Example with Ok client below.
}

class ParallelUserTestClientMain {
    fun run() {
        runBlocking {
            val nUsers = 10 // Anzahl der Benutzer
            val delayBetweenCalls = 20L * 1000 // 20 Sekunden in Millisekunden
            val maxStartDelay = 10L * 1000 // Maximale zufällige Verzögerung in Millisekunden
            val httpClient = HttpClients.createDefault()

            // Starte n parallele Benutzer-Sessions
            val jobs = (1..nUsers).map { userId ->
                launch {
                    val startDelay = Random.nextLong(0, maxStartDelay)
                    simulateUserSession(httpClient, userId, delayBetweenCalls, startDelay)
                }
            }

            jobs.forEach { it.join() }
        }
    }

    suspend fun simulateUserSession(
        httpClient: CloseableHttpClient,
        userId: Int,
        delayBetweenCalls: Long,
        startDelay: Long,
    ) =
        coroutineScope {
            val urls = listOf(
                "http://example.com/api/resource1",
                "http://example.com/api/resource2",
                "http://example.com/api/resource3"
            )
            delay(startDelay) // Verwende delay statt Thread.sleep für nicht-blockierendes Warten
            for (url in urls) {
                try {
                    val request = HttpGet(url)
                    println("User $userId making request to: $url")

                    httpClient.execute(request).use { response ->
                        val entity = response.entity
                        if (entity != null) {
                            val responseBody = EntityUtils.toString(entity)
                            println("User $userId received response: $responseBody")
                        }
                    }

                    // Wartezeit zwischen den Requests
                    delay(delayBetweenCalls)

                } catch (e: Exception) {
                    println("User $userId encountered an error: ${e.message}")
                }
            }
        }
}

/*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlin.random.Random

fun main() = runBlocking {
    val nUsers = 10 // Anzahl der Benutzer
    val delayBetweenCalls = 20L * 1000 // 20 Sekunden in Millisekunden
    val maxStartDelay = 10L * 1000 // Maximale zufällige Verzögerung in Millisekunden

    // OkHttpClient initialisieren
    val httpClient = OkHttpClient()

    // Starte n parallele Benutzer-Sessions
    val jobs = (1..nUsers).map { userId ->
        launch {
            val startDelay = Random.nextLong(0, maxStartDelay)
            simulateUserSession(httpClient, userId, delayBetweenCalls, startDelay)
        }
    }

    jobs.forEach { it.join() } // Warte, bis alle Koroutinen abgeschlossen sind
}

suspend fun simulateUserSession(httpClient: OkHttpClient, userId: Int, delayBetweenCalls: Long, startDelay: Long) = coroutineScope {
    val urls = listOf(
        "http://example.com/api/resource1",
        "http://example.com/api/resource2",
        "http://example.com/api/resource3"
    )

    try {
        // Zufällige Verzögerung vor dem ersten Request
        println("User $userId waiting for ${startDelay / 1000} seconds before starting")
        delay(startDelay) // Verzögerung ohne Blockieren

        for (url in urls) {
            // Baue die Anfrage
            val request = Request.Builder()
                .url(url)
                .build()

            println("User $userId making request to: $url")

            // Anfrage senden und Antwort verarbeiten
            val response: Response = httpClient.newCall(request).execute()
            response.use {
                if (!it.isSuccessful) throw IOException("Unexpected code $it")

                val responseBody = it.body?.string()
                println("User $userId received response: $responseBody")
            }

            // Wartezeit zwischen den Requests (z.B. 20 Sekunden)
            delay(delayBetweenCalls)
        }
    } catch (e: Exception) {
        println("User $userId encountered an error: ${e.message}")
    }
}
 */

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

package org.projectforge.business.push

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.projectforge.business.user.AppDeviceDO
import org.projectforge.business.user.PushEnvironment
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.util.Base64

private val log = KotlinLogging.logger {}

/**
 * Sends pushes directly to Apple (APNs, HTTP/2) with a token based key (`.p8`, no yearly certificate): configured by
 * `projectforge.push.apns.keyFile` (the `AuthKey_<id>.p8`), `keyId`, `teamId` and `topic` (the bundle id of the app).
 * Without a key file nothing is sent and the apps keep polling.
 */
@Service
class ApnsPushSender : PushSender {
    @Value("\${projectforge.push.apns.keyFile:}")
    internal var keyFile: String = ""

    @Value("\${projectforge.push.apns.keyId:}")
    internal var keyId: String = ""

    @Value("\${projectforge.push.apns.teamId:}")
    internal var teamId: String = ""

    @Value("\${projectforge.push.apns.topic:de.projectforge.mobileapp}")
    internal var topic: String = "de.projectforge.mobileapp"

    private var privateKey: PrivateKey? = null

    private var jwt: String? = null
    private var jwtCreated = 0L

    private val client: HttpClient by lazy {
        HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofSeconds(10)).build()
    }

    override val isAvailable: Boolean
        get() = privateKey != null

    @PostConstruct
    internal fun init() {
        if (keyFile.isBlank()) {
            return
        }
        if (keyId.isBlank() || teamId.isBlank()) {
            log.warn { "Push disabled: projectforge.push.apns.keyId and teamId are required with keyFile." }
            return
        }
        try {
            privateKey = parseKey(File(keyFile).readText())
            log.info { "Push (APNs) enabled: key $keyId, team $teamId, topic $topic." }
        } catch (ex: Exception) {
            log.error { "Push disabled: the APNs key '$keyFile' can't be read: ${ex.message}" }
        }
    }

    override fun send(device: AppDeviceDO, message: PushMessage): PushResult {
        val token = device.pushToken ?: return PushResult.UNREGISTERED
        val host = if (device.pushEnvironment == PushEnvironment.SANDBOX) SANDBOX_HOST else PRODUCTION_HOST
        return try {
            val request = HttpRequest.newBuilder(URI.create("https://$host/3/device/$token"))
                .timeout(Duration.ofSeconds(15))
                .header("authorization", "bearer ${providerToken()}")
                .header("apns-topic", topic)
                .header("apns-push-type", "alert")
                .header("apns-priority", "10")
                .POST(HttpRequest.BodyPublishers.ofString(payload(message, device.id)))
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            result(response.statusCode(), response.body(), device)
        } catch (ex: Exception) {
            log.warn { "Push to app device #${device.id} failed: ${ex.message}" }
            PushResult.ERROR
        }
    }

    internal fun result(status: Int, body: String?, device: AppDeviceDO): PushResult {
        if (status == 200) {
            return PushResult.OK
        }
        val reason = body?.let { runCatching { mapper.readTree(it).path("reason").asText() }.getOrNull() }
        log.warn { "Push to app device #${device.id} refused: $status $reason" }
        if (status == 403) {
            synchronized(this) { jwt = null } // ExpiredProviderToken or a wrong key: sign anew next time.
        }
        return if (status == 410 || reason in UNREGISTERED_REASONS) PushResult.UNREGISTERED else PushResult.ERROR
    }

    /** Apple wants a new provider token at most every hour, but at least every 60 minutes: 50 is in between. */
    @Synchronized
    private fun providerToken(): String {
        val now = System.currentTimeMillis()
        jwt?.let { if (now - jwtCreated < JWT_VALID_MILLIS) return it }
        val created = createJwt(requireNotNull(privateKey), keyId, teamId, now / 1000)
        jwt = created
        jwtCreated = now
        return created
    }

    companion object {
        private const val PRODUCTION_HOST = "api.push.apple.com"
        private const val SANDBOX_HOST = "api.sandbox.push.apple.com"
        private const val JWT_VALID_MILLIS = 50 * 60 * 1000L
        private val UNREGISTERED_REASONS = setOf("BadDeviceToken", "Unregistered", "DeviceTokenNotForTopic")
        private val mapper = ObjectMapper()
        private val base64 = Base64.getUrlEncoder().withoutPadding()

        internal fun parseKey(pem: String): PrivateKey {
            val der = Base64.getMimeDecoder().decode(
                pem.lines().filterNot { it.startsWith("-----") }.joinToString(""),
            )
            return KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(der))
        }

        /** ES256: the signature is the raw r||s of the P1363 format, as JWT wants it (not DER). */
        internal fun createJwt(key: PrivateKey, keyId: String, teamId: String, issuedAtSeconds: Long): String {
            val header = base64.encodeToString("""{"alg":"ES256","kid":"$keyId"}""".toByteArray())
            val claims = base64.encodeToString("""{"iss":"$teamId","iat":$issuedAtSeconds}""".toByteArray())
            val signature = Signature.getInstance("SHA256withECDSAinP1363Format")
            signature.initSign(key)
            signature.update("$header.$claims".toByteArray())
            return "$header.$claims.${base64.encodeToString(signature.sign())}"
        }

        /**
         * @param deviceId The app device the push goes to (`device`): the app, logged in at several servers, tells by it
         * which of its logins the push belongs to.
         */
        internal fun payload(message: PushMessage, deviceId: Long? = null): String {
            val alert = linkedMapOf("title" to message.title, "body" to message.body)
            message.subtitle?.let { alert["subtitle"] = it }
            val aps = linkedMapOf<String, Any>(
                "alert" to alert,
                "badge" to message.badge,
            )
            if (message.sound) {
                aps["sound"] = "default"
            }
            val root = linkedMapOf<String, Any>("aps" to aps)
            message.link?.let { root["link"] = it }
            message.notificationId?.let { root["notificationId"] = it }
            deviceId?.let { root["device"] = it }
            // The ids of the devices are counted per server: the host tells two logins with the same id apart.
            message.subtitle?.let { root["server"] = it }
            return mapper.writeValueAsString(root)
        }
    }
}

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
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.net.URI
import java.net.URLEncoder
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
 * Sends pushes to the Android apps through Firebase Cloud Messaging (HTTP v1). The server authenticates with a service
 * account of the Firebase project of the app (`projectforge.push.fcm.serviceAccountFile`, the JSON key; the project id
 * is taken from it, `projectforge.push.fcm.projectId` overrides it). Without the key file nothing is sent.
 *
 * No Google library: the OAuth2 access token is fetched with a signed JWT (RS256), as for the APNs provider token.
 */
@Service
open class FcmPushSender : PushSender {
    @Value("\${projectforge.push.fcm.serviceAccountFile:}")
    internal var serviceAccountFile: String = ""

    @Value("\${projectforge.push.fcm.projectId:}")
    internal var projectId: String = ""

    private var account: ServiceAccount? = null

    private var accessToken: String? = null
    private var accessTokenExpires = 0L

    private val client: HttpClient by lazy {
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    }

    override val isAvailable: Boolean
        get() = account != null

    @PostConstruct
    internal fun init() {
        if (serviceAccountFile.isBlank()) {
            return
        }
        try {
            val parsed = parseAccount(File(serviceAccountFile).readText(), projectId)
            account = parsed
            log.info { "Push (FCM) enabled: project ${parsed.projectId}, service account ${parsed.clientEmail}." }
        } catch (ex: Exception) {
            log.error { "Push (FCM) disabled: the service account '$serviceAccountFile' can't be read: ${ex.message}" }
        }
    }

    override fun send(device: AppDeviceDO, message: PushMessage): PushResult {
        val token = device.pushToken ?: return PushResult.UNREGISTERED
        val account = account ?: return PushResult.ERROR
        return try {
            val request = HttpRequest.newBuilder(
                URI.create("https://fcm.googleapis.com/v1/projects/${account.projectId}/messages:send"),
            )
                .timeout(Duration.ofSeconds(15))
                .header("authorization", "Bearer ${accessToken(account)}")
                .header("content-type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(payload(message, token, device.id)))
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
        val error = body?.let { runCatching { mapper.readTree(it).path("error") }.getOrNull() }
        val code = error?.path("status")?.asText() ?: ""
        val text = error?.path("message")?.asText() ?: ""
        val detail = error?.path("details")?.firstNotNullOfOrNull {
            it.path("errorCode").asText().takeIf { value -> value.isNotEmpty() }
        } ?: ""
        log.warn { "Push to app device #${device.id} refused: $status $code $detail $text" }
        if (status == 401 || status == 403 && detail != "SENDER_ID_MISMATCH") {
            synchronized(this) { accessToken = null } // Expired or revoked: fetch a new one next time.
        }
        return when {
            status == 404 || detail == "UNREGISTERED" || detail == "SENDER_ID_MISMATCH" -> PushResult.UNREGISTERED
            // A token that isn't one: the same status as a faulty payload, told apart by the text.
            status == 400 && code == "INVALID_ARGUMENT" && text.contains("token", ignoreCase = true) ->
                PushResult.UNREGISTERED

            else -> PushResult.ERROR
        }
    }

    @Synchronized
    private fun accessToken(account: ServiceAccount): String {
        val now = System.currentTimeMillis()
        accessToken?.let { if (now < accessTokenExpires) return it }
        val assertion = createJwt(account, now / 1000)
        val body = "grant_type=${encode("urn:ietf:params:oauth:grant-type:jwt-bearer")}&assertion=${encode(assertion)}"
        val request = HttpRequest.newBuilder(URI.create(account.tokenUri))
            .timeout(Duration.ofSeconds(15))
            .header("content-type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        require(response.statusCode() == 200) { "access token refused: ${response.statusCode()}" }
        val json = mapper.readTree(response.body())
        val token = json.path("access_token").asText()
        require(token.isNotEmpty()) { "no access token in the answer" }
        // A minute early, so a token never expires on the way.
        accessTokenExpires = now + (json.path("expires_in").asLong(3600) - 60) * 1000
        accessToken = token
        return token
    }

    internal class ServiceAccount(
        val projectId: String,
        val clientEmail: String,
        val privateKey: PrivateKey,
        val tokenUri: String,
    )

    companion object {
        /** Android channels of the app (`expoPush.ts`): the sound belongs to the channel since Android 8. */
        internal const val CHANNEL_DEFAULT = "pf_default"
        internal const val CHANNEL_IMPORTANT = "pf_important"

        private const val SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
        private const val DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token"
        private val mapper = ObjectMapper()
        private val base64 = Base64.getUrlEncoder().withoutPadding()

        private fun encode(value: String) = URLEncoder.encode(value, Charsets.UTF_8)

        internal fun parseAccount(json: String, projectIdOverride: String = ""): ServiceAccount {
            val tree = mapper.readTree(json)
            fun text(field: String) = tree.path(field).asText().also { require(it.isNotBlank()) { "'$field' is missing" } }
            val der = Base64.getMimeDecoder().decode(
                text("private_key").lines().filterNot { it.startsWith("-----") }.joinToString(""),
            )
            return ServiceAccount(
                projectId = projectIdOverride.ifBlank { text("project_id") },
                clientEmail = text("client_email"),
                privateKey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(der)),
                tokenUri = tree.path("token_uri").asText().ifBlank { DEFAULT_TOKEN_URI },
            )
        }

        /** The signed request for an access token (OAuth2 JWT bearer grant), valid for the hour Google allows. */
        internal fun createJwt(account: ServiceAccount, issuedAtSeconds: Long): String {
            val header = base64.encodeToString("""{"alg":"RS256","typ":"JWT"}""".toByteArray())
            val claims = base64.encodeToString(
                mapper.writeValueAsString(
                    linkedMapOf(
                        "iss" to account.clientEmail,
                        "scope" to SCOPE,
                        "aud" to account.tokenUri,
                        "iat" to issuedAtSeconds,
                        "exp" to issuedAtSeconds + 3600,
                    ),
                ).toByteArray(),
            )
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initSign(account.privateKey)
            signature.update("$header.$claims".toByteArray())
            return "$header.$claims.${base64.encodeToString(signature.sign())}"
        }

        /**
         * The same content as the APNs payload ([ApnsPushSender.payload]), the ids beside it in `data` (FCM wants
         * strings only): the app reads both the same way. The system shows the `notification` while the app is in the
         * background; the app shows it itself in the foreground.
         *
         * @param deviceId The app device the push goes to (`device`), see [ApnsPushSender.payload].
         */
        internal fun payload(message: PushMessage, token: String, deviceId: Long? = null): String {
            val data = linkedMapOf("badge" to message.badge.toString())
            message.link?.let { data["link"] = it }
            message.notificationId?.let { data["notificationId"] = it.toString() }
            deviceId?.let { data["device"] = it.toString() }
            message.subtitle?.let { data["server"] = it }
            val android = linkedMapOf<String, Any>(
                "priority" to "HIGH",
                "notification" to linkedMapOf(
                    "channel_id" to if (message.sound) CHANNEL_IMPORTANT else CHANNEL_DEFAULT,
                ),
            )
            val root = linkedMapOf<String, Any>(
                "message" to linkedMapOf(
                    "token" to token,
                    "notification" to linkedMapOf("title" to message.title, "body" to message.body),
                    "android" to android,
                    "data" to data,
                ),
            )
            return mapper.writeValueAsString(root)
        }
    }
}

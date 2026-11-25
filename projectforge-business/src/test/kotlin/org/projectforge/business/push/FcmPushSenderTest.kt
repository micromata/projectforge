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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.user.AppDeviceDO
import java.security.KeyPairGenerator
import java.util.Base64

class FcmPushSenderTest {
    @Test
    fun `payload carries the text, the ids as strings and the channel`() {
        val json = ObjectMapper().readTree(
            FcmPushSender.payload(PushMessage("Title", "Text", 3, true, "/chat/5", 42L, "pf.example.org"), "tok:en", 7L),
        ).path("message")
        Assertions.assertEquals("tok:en", json.path("token").asText())
        Assertions.assertEquals("Title", json.path("notification").path("title").asText())
        Assertions.assertEquals("Text", json.path("notification").path("body").asText())
        Assertions.assertEquals("HIGH", json.path("android").path("priority").asText())
        Assertions.assertEquals(
            FcmPushSender.CHANNEL_IMPORTANT,
            json.path("android").path("notification").path("channel_id").asText(),
        )
        val data = json.path("data")
        Assertions.assertEquals("7", data.path("device").asText())
        Assertions.assertEquals("pf.example.org", data.path("server").asText())
        Assertions.assertEquals("3", data.path("badge").asText())
        Assertions.assertEquals("/chat/5", data.path("link").asText())
        Assertions.assertEquals("42", data.path("notificationId").asText())
        data.properties().forEach { Assertions.assertTrue(it.value.isTextual, it.key) }

        val quiet = ObjectMapper().readTree(
            FcmPushSender.payload(PushMessage("T", "B", 0, false, null, null), "x"),
        ).path("message")
        Assertions.assertEquals(
            FcmPushSender.CHANNEL_DEFAULT,
            quiet.path("android").path("notification").path("channel_id").asText(),
        )
        Assertions.assertTrue(quiet.path("data").path("link").isMissingNode)
        Assertions.assertTrue(quiet.path("data").path("device").isMissingNode)
    }

    @Test
    fun `answers of fcm are mapped`() {
        val sender = FcmPushSender()
        val device = AppDeviceDO().also { it.id = 1L }
        fun error(status: String, message: String, errorCode: String? = null): String {
            val details = errorCode?.let { ""","details":[{"errorCode":"$it"}]""" } ?: ""
            return """{"error":{"status":"$status","message":"$message"$details}}"""
        }
        Assertions.assertEquals(PushResult.OK, sender.result(200, "{}", device))
        Assertions.assertEquals(PushResult.UNREGISTERED, sender.result(404, error("NOT_FOUND", "gone", "UNREGISTERED"), device))
        Assertions.assertEquals(PushResult.UNREGISTERED, sender.result(403, error("PERMISSION_DENIED", "x", "SENDER_ID_MISMATCH"), device))
        Assertions.assertEquals(
            PushResult.UNREGISTERED,
            sender.result(400, error("INVALID_ARGUMENT", "The registration token is not a valid FCM registration token"), device),
        )
        Assertions.assertEquals(
            PushResult.ERROR,
            sender.result(400, error("INVALID_ARGUMENT", "Invalid value at 'message.data'"), device),
        )
        Assertions.assertEquals(PushResult.ERROR, sender.result(401, error("UNAUTHENTICATED", "expired"), device))
        Assertions.assertEquals(PushResult.ERROR, sender.result(429, null, device))
        Assertions.assertEquals(PushResult.ERROR, sender.result(503, "not json", device))
    }

    @Test
    fun `service account is read and the request for an access token is signed`() {
        val pair = KeyPairGenerator.getInstance("RSA").also { it.initialize(2048) }.generateKeyPair()
        val pem = "-----BEGIN PRIVATE KEY-----\n" +
            Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(pair.private.encoded) +
            "\n-----END PRIVATE KEY-----\n"
        val json = ObjectMapper().writeValueAsString(
            mapOf("project_id" to "pf-app", "client_email" to "push@pf-app.iam.gserviceaccount.com", "private_key" to pem),
        )
        val account = FcmPushSender.parseAccount(json)
        Assertions.assertEquals("pf-app", account.projectId)
        Assertions.assertEquals("other", FcmPushSender.parseAccount(json, "other").projectId)
        Assertions.assertEquals("https://oauth2.googleapis.com/token", account.tokenUri)

        val parts = FcmPushSender.createJwt(account, 1000L).split(".")
        Assertions.assertEquals(3, parts.size)
        val claims = ObjectMapper().readTree(Base64.getUrlDecoder().decode(parts[1]))
        Assertions.assertEquals("push@pf-app.iam.gserviceaccount.com", claims.path("iss").asText())
        Assertions.assertEquals(4600L, claims.path("exp").asLong())
        val signature = java.security.Signature.getInstance("SHA256withRSA")
        signature.initVerify(pair.public)
        signature.update("${parts[0]}.${parts[1]}".toByteArray())
        Assertions.assertTrue(signature.verify(Base64.getUrlDecoder().decode(parts[2])))
        Assertions.assertThrows(IllegalArgumentException::class.java) { FcmPushSender.parseAccount("{}") }
    }

    @Test
    fun `not available without a service account`() {
        val sender = FcmPushSender()
        sender.init()
        Assertions.assertFalse(sender.isAvailable)
    }
}

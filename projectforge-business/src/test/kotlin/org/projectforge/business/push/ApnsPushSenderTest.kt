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
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class ApnsPushSenderTest {
    @Test
    fun `jwt is signed with ES256 and verifiable`() {
        val pair = KeyPairGenerator.getInstance("EC").also { it.initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val jwt = ApnsPushSender.createJwt(pair.private, "KEY1234567", "TEAM123456", 1_700_000_000L)
        val (header, claims, signature) = jwt.split(".")
        val decoder = Base64.getUrlDecoder()
        val mapper = ObjectMapper()
        Assertions.assertEquals("ES256", mapper.readTree(decoder.decode(header)).path("alg").asText())
        Assertions.assertEquals("KEY1234567", mapper.readTree(decoder.decode(header)).path("kid").asText())
        Assertions.assertEquals("TEAM123456", mapper.readTree(decoder.decode(claims)).path("iss").asText())
        Assertions.assertEquals(64, decoder.decode(signature).size, "Raw r||s, not DER.")
        val verifier = Signature.getInstance("SHA256withECDSAinP1363Format")
        verifier.initVerify(pair.public)
        verifier.update("$header.$claims".toByteArray())
        Assertions.assertTrue(verifier.verify(decoder.decode(signature)))
    }

    @Test
    fun `key is read from pem`() {
        val pair = KeyPairGenerator.getInstance("EC").also { it.initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val pem = "-----BEGIN PRIVATE KEY-----\n" +
            Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(pair.private.encoded) +
            "\n-----END PRIVATE KEY-----\n"
        Assertions.assertEquals(pair.private, ApnsPushSender.parseKey(pem))
    }

    @Test
    fun `payload carries the text and the ids`() {
        val json = ObjectMapper().readTree(
            ApnsPushSender.payload(PushMessage("Title", "Text", 3, true, "/chat/5", 42L, "pf.example.org"), 7L),
        )
        Assertions.assertEquals("Title", json.path("aps").path("alert").path("title").asText())
        Assertions.assertEquals("Text", json.path("aps").path("alert").path("body").asText())
        Assertions.assertEquals("pf.example.org", json.path("aps").path("alert").path("subtitle").asText())
        Assertions.assertEquals(7L, json.path("device").asLong())
        Assertions.assertEquals("pf.example.org", json.path("server").asText())
        Assertions.assertEquals(3, json.path("aps").path("badge").asInt())
        Assertions.assertEquals("default", json.path("aps").path("sound").asText())
        Assertions.assertEquals("/chat/5", json.path("link").asText())
        Assertions.assertEquals(42L, json.path("notificationId").asLong())
        val silent = ObjectMapper().readTree(ApnsPushSender.payload(PushMessage("T", "B", 0, false, null, null)))
        Assertions.assertTrue(silent.path("aps").path("sound").isMissingNode)
        Assertions.assertTrue(silent.path("link").isMissingNode)
        Assertions.assertTrue(silent.path("aps").path("alert").path("subtitle").isMissingNode)
        Assertions.assertTrue(silent.path("device").isMissingNode)
    }

    @Test
    fun `answers of apns are mapped`() {
        val sender = ApnsPushSender()
        val device = AppDeviceDO().also { it.id = 1L }
        Assertions.assertEquals(PushResult.OK, sender.result(200, "", device))
        Assertions.assertEquals(PushResult.UNREGISTERED, sender.result(410, """{"reason":"Unregistered"}""", device))
        Assertions.assertEquals(PushResult.UNREGISTERED, sender.result(400, """{"reason":"BadDeviceToken"}""", device))
        Assertions.assertEquals(PushResult.ERROR, sender.result(400, """{"reason":"PayloadTooLarge"}""", device))
        Assertions.assertEquals(PushResult.ERROR, sender.result(503, null, device))
    }

    @Test
    fun `not available without a key`() {
        val sender = ApnsPushSender()
        sender.init()
        Assertions.assertFalse(sender.isAvailable)
    }
}

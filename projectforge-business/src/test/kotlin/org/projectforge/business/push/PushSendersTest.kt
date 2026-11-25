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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.user.AppDeviceDO
import org.springframework.test.util.ReflectionTestUtils

class PushSendersTest {
    private class Recorder(override val isAvailable: Boolean) : PushSender {
        var sent = 0
        override fun send(device: AppDeviceDO, message: PushMessage): PushResult {
            sent++
            return PushResult.OK
        }
    }

    private class FakeApns(private val delegate: PushSender) : ApnsPushSender() {
        override val isAvailable get() = delegate.isAvailable
        override fun send(device: AppDeviceDO, message: PushMessage) = delegate.send(device, message)
    }

    private class FakeFcm(private val delegate: PushSender) : FcmPushSender() {
        override val isAvailable get() = delegate.isAvailable
        override fun send(device: AppDeviceDO, message: PushMessage) = delegate.send(device, message)
    }

    private val message = PushMessage("T", "B", 0, false, null, null)

    private fun senders(apns: Recorder, fcm: Recorder) = PushSenders().also {
        ReflectionTestUtils.setField(it, "apns", FakeApns(apns))
        ReflectionTestUtils.setField(it, "fcm", FakeFcm(fcm))
    }

    @Test
    fun `push goes to the sender of the platform`() {
        val apns = Recorder(true)
        val fcm = Recorder(true)
        val senders = senders(apns, fcm)
        senders.send(AppDeviceDO().also { it.platform = "ios" }, message)
        senders.send(AppDeviceDO(), message) // Unknown (older app): APNs, as before.
        senders.send(AppDeviceDO().also { it.platform = "android" }, message)
        Assertions.assertEquals(2, apns.sent)
        Assertions.assertEquals(1, fcm.sent)
    }

    @Test
    fun `only android goes to fcm, everything else to apns as before`() {
        Assertions.assertTrue(PushSenders.isAndroid(AppDeviceDO().also { it.platform = "android" }))
        Assertions.assertTrue(PushSenders.isAndroid(AppDeviceDO().also { it.platform = "Android" }))
        Assertions.assertFalse(PushSenders.isAndroid(AppDeviceDO().also { it.platform = "ios" }))
        Assertions.assertFalse(PushSenders.isAndroid(AppDeviceDO().also { it.platform = null }))
        Assertions.assertFalse(PushSenders.isAndroid(AppDeviceDO().also { it.platform = "" }))
    }

    @Test
    fun `available if one sender is, no push through the one that is not`() {
        val apns = Recorder(false)
        val fcm = Recorder(true)
        val senders = senders(apns, fcm)
        Assertions.assertTrue(senders.isAvailable)
        Assertions.assertEquals(PushResult.ERROR, senders.send(AppDeviceDO().also { it.platform = "ios" }, message))
        Assertions.assertEquals(0, apns.sent)
        Assertions.assertFalse(senders(Recorder(false), Recorder(false)).isAvailable)
    }
}

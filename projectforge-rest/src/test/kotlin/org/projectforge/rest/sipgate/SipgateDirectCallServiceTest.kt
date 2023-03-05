/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.sipgate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.sipgate.SipgateDevice
import org.projectforge.business.sipgate.SipgateNumber
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.NumberHelper


class SipgateDirectCallServiceTest {
  @Test
  fun getCallerData() {
    NumberHelper.TEST_COUNTRY_PREFIX_USAGE_IN_TESTCASES_ONLY = "+49"
    val userDevices = listOf(
      createUserDevice("e1", "Berta's phone", routingId = "p1", routingAlias = "Berta"),
      createUserDevice("p2", "Berta's cellular", routingId = "p1", routingAlias = "Berta")
    )
    val numbers = listOf(createNumber("p1", "42"))
    val user = createUser("0987654,42")
    assert(
      callerId = "0123456789", caller = "p1", callee = "55555", deviceId = "e1",
      SipgateDirectCallService.getCallerData(
        userDevices, numbers, "default", "012345", user, "Berta's phone (p1)",
        "0123456789", "55555"
      )
    )
    assert(
      callerId = "01234542", caller = "p1", callee = "55555", deviceId = "e1",
      SipgateDirectCallService.getCallerData(
        userDevices, numbers, "default", "012345", user, "Berta's phone (p1)",
        "", "55555"
      )
    )
    assert(
      callerId = "0777777", caller = "p1", callee = "55555", deviceId = "e1",
      SipgateDirectCallService.getCallerData(
        userDevices, numbers, "default", "012345", user, "Berta's phone (p1)",
        "0777777", "55555"
      )
    )
    assert(
      callerId = "0170123456", caller = "0170123456", callee = "55555", deviceId = "defaultDeviceId",
      SipgateDirectCallService.getCallerData(
        emptyList(), emptyList(), "defaultDeviceId", "012345", user, "0170123456",
        "0170123456", "55555"
      )
    )
    assert(
      callerId = "1111111", caller = "2222222", callee = "55555", deviceId = "defaultDeviceId",
      SipgateDirectCallService.getCallerData(
        emptyList(), emptyList(), "defaultDeviceId", "012345", user, "2222222",
        "1111111", "55555"
      )
    )
    Assertions.assertNull(
      SipgateDirectCallService.getCallerData(
        emptyList(), emptyList(), "", "012345", user, "099999999",
        "0777777", "55555"
      ),
      "Neither device nor defaultDevice found."
    )
  }

  @Test
  fun getCallerDevicesTest() {
    Assertions.assertTrue(SipgateDirectCallService.getCallerDevices(createUser(), null).isEmpty())
    assertList(SipgateDirectCallService.getCallerDevices(createUser("42"), null), "42")
    assertList(SipgateDirectCallService.getCallerDevices(createUser("42, 827"), null), "42", "827")

    Assertions.assertTrue(SipgateDirectCallService.getCallerDevices(createUser(), emptyList()).isEmpty())
    assertList(
      SipgateDirectCallService.getCallerDevices(createUser(), listOf(SipgateDevice("p2", "Kai's phone"))),
      "Kai's phone (p2)"
    )
    assertList(
      SipgateDirectCallService.getCallerDevices(
        createUser(), listOf(
          SipgateDevice("p2", "Kai's phone"),
          SipgateDevice("x2", "External phone of Kai"),
        )
      ), "Kai's phone (p2)", "External phone of Kai (x2)"
    )
    assertList(
      SipgateDirectCallService.getCallerDevices(
        createUser("42, 827"), listOf(
          SipgateDevice("p2", "Kai's phone"),
          SipgateDevice("x2", "External phone of Kai"),
        )
      ), "Kai's phone (p2)", "External phone of Kai (x2)", "42", "827"
    )

  }

  @Test
  fun personalPhoneIdentifiersTest() {
    Assertions.assertNull(SipgateDirectCallService.getNormalizedPersonalPhoneIdentifiers(null))
    Assertions.assertNull(SipgateDirectCallService.getNormalizedPersonalPhoneIdentifiers(""))
    Assertions.assertEquals("42", SipgateDirectCallService.getNormalizedPersonalPhoneIdentifiers("42"))
    Assertions.assertEquals("42,827", SipgateDirectCallService.getNormalizedPersonalPhoneIdentifiers("42, 827"))
    Assertions.assertEquals("42,827", SipgateDirectCallService.getNormalizedPersonalPhoneIdentifiers("42, 827,42"))
  }

  private fun createUser(personalPhoneIdentifiers: String? = null): PFUserDO {
    val user = PFUserDO()
    user.personalPhoneIdentifiers = personalPhoneIdentifiers
    return user
  }

  private fun assertList(list: List<String>, vararg expected: String) {
    Assertions.assertEquals(expected.size, list.size)
    expected.forEachIndexed { index, s ->
      Assertions.assertEquals(s, list[index])
    }
  }

  private fun createUserDevice(id: String, alias: String, routingId: String, routingAlias: String): SipgateDevice {
    val device = SipgateDevice()
    device.id = id
    device.alias = alias
    val routing = SipgateDevice.ActiveRouting()
    routing.id = routingId
    routing.alias = routingAlias
    device.activePhonelines = listOf(routing)
    return device
  }

  private fun createNumber(endpointId: String, localized: String): SipgateNumber {
    val number = SipgateNumber()
    number.endpointId = endpointId
    number.localized = localized
    return number
  }

  private fun assert(
    callerId: String,
    caller: String,
    callee: String,
    deviceId: String,
    callData: SipgateDirectCallService.CallData?,
  ) {
    Assertions.assertNotNull(callData)
    Assertions.assertEquals(callerId, callData!!.callerId, "callerId")
    Assertions.assertEquals(caller, callData.caller, "caller")
    Assertions.assertEquals(callee, callData.callee, "callee")
    Assertions.assertEquals(deviceId, callData.deviceId, "deviceId")
  }
}

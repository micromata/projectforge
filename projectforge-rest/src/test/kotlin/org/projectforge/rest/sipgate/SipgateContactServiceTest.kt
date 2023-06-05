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
import org.projectforge.business.address.AddressDO
import org.projectforge.business.sipgate.SipgateContact
import org.projectforge.business.sipgate.SipgateContactSyncDO
import org.projectforge.framework.utils.NumberHelper
import kotlin.reflect.KMutableProperty


class SipgateContactServiceTest {
  @Test
  fun fixNumbersTest() {
    NumberHelper.TEST_COUNTRY_PREFIX_USAGE_IN_TESTCASES_ONLY = "+49"
    check(AddressDO::privatePhone, SipgateContact::home)
    check(AddressDO::businessPhone, SipgateContact::work)
    check(AddressDO::mobilePhone, SipgateContact::cell)
    check(AddressDO::privateMobilePhone, SipgateContact::cellHome)
    check(AddressDO::fax, SipgateContact::faxWork)
  }

  private fun check(addressField: KMutableProperty<*>, contactField: KMutableProperty<*>) {
    val otherNumber = "01234 56789"
    val number = "+49 123456789"
    val address = AddressDO()
    addressField.setter.call(address, number)
    val contact = SipgateContact()
    contact.other = otherNumber
    SipgateContactService.fixNumbers(contact, address)
    if (contactField != SipgateContact::other) {
      Assertions.assertNull(contact.other)
    }
    Assertions.assertEquals(otherNumber, contactField.getter.call(contact))

    val syncInfo = SipgateContactSyncDO.SyncInfo()
    syncInfo.setFieldsInfo(addressField.name, number)
    SipgateContactService.fixNumbers(contact, address, syncInfo)
    if (contactField != SipgateContact::other) {
      Assertions.assertNull(contact.other)
    }
    Assertions.assertEquals(otherNumber, contactField.getter.call(contact))
  }
}

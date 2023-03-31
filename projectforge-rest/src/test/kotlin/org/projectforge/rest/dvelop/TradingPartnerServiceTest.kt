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

package org.projectforge.rest.dvelop

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.dvelop.TradingPartner


class TradingPartnerServiceTest {
  @Test
  fun buildUpdateEntityTest() {
    val local = TradingPartner()
    local.organization = TradingPartner.Organization("1")
    local.billToStreet = "ABC street 1"
    local.billToCity = "Kassel"
    val remote = TradingPartner()
    remote.id = "remoteId"
    remote.organization = TradingPartner.Organization("1", "5", "Hurzel")
    remote.billToStreet = "New ABC street 1"
    var update = TradingPartnerService().buildUpdateEntity(local, remote)
    Assertions.assertNull(update)
    remote.billToStreet = null
    update = TradingPartnerService().buildUpdateEntity(local, remote)
    Assertions.assertEquals(local.billToStreet, update!!.billToStreet)
    Assertions.assertEquals(local.billToCity, update.billToCity)
  }
}

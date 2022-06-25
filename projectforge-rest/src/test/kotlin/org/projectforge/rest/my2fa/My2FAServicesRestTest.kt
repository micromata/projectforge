/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.my2fa

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class My2FAServicesRestTest {

  @Test
  fun replaceRestUrlByReactUrlTest() {
    Assertions.assertNull(My2FAServicesRest.replaceRestByReactUrl(null))
    Assertions.assertEquals("", My2FAServicesRest.replaceRestByReactUrl(""))
    Assertions.assertEquals("/", My2FAServicesRest.replaceRestByReactUrl("/"))
    Assertions.assertEquals("/react/calendar", My2FAServicesRest.replaceRestByReactUrl("/react/calendar"))
    Assertions.assertEquals("/react/calendar?test=hurz", My2FAServicesRest.replaceRestByReactUrl("/react/calendar?test=hurz"))
    Assertions.assertEquals("/react/user", My2FAServicesRest.replaceRestByReactUrl("/rs/user/initialList"))
    Assertions.assertEquals("/react/user?test=hurz", My2FAServicesRest.replaceRestByReactUrl("/rs/user/initialList?test=hurz"))
    Assertions.assertEquals("/react/user/edit/1", My2FAServicesRest.replaceRestByReactUrl("/rs/user/edit?id=1"))
    Assertions.assertEquals("/react/user/edit/1?test=hurz", My2FAServicesRest.replaceRestByReactUrl("/rs/user/edit?id=1&test=hurz"))
  }
}

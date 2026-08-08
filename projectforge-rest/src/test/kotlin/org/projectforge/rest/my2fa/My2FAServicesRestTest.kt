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

package org.projectforge.rest.my2fa

import org.apache.commons.codec.binary.Base64
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class My2FAServicesRestTest {

  /**
   * The target comes from the client, so a successful 2FA must not redirect to a foreign host: that would be an
   * open redirect on a page the victim demonstrably trusts (they just authenticated on it).
   */
  @Test
  fun redirectUrlFromTargetTest() {
    // Foreign hosts and other schemes are dropped, see LoginServiceRestTest for the full table:
    assertRejected("http://evil.com")
    assertRejected("//evil.com")
    assertRejected("/\\evil.com")
    assertRejected("javascript:alert(1)")
    assertRejected("next/book/42") // Not an absolute path of this application.
    assertRejected("")
    Assertions.assertNull(My2FAServicesRest.redirectUrlFromTarget(null), "Nothing to redirect to.")

    // The legitimate cases, with the rest url replaced as before:
    assertRedirect("/react/calendar", "/react/calendar")
    assertRedirect("/next/book/42", "/next/book/42")
    assertRedirect("/react/user/edit/1", "/rs/user/edit?id=1")
  }

  private fun assertRejected(url: String) {
    Assertions.assertNull(My2FAServicesRest.redirectUrlFromTarget(encode(url)), "Url '$url' has to be rejected.")
  }

  private fun assertRedirect(expected: String, url: String) {
    Assertions.assertEquals(expected, My2FAServicesRest.redirectUrlFromTarget(encode(url)))
  }

  private fun encode(url: String): String {
    return Base64.encodeBase64String(url.toByteArray(StandardCharsets.UTF_8))
  }

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

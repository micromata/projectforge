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

package org.projectforge.web

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.security.LoggingFilter
import javax.servlet.http.HttpServletRequest

class LoggingFilterTest {

  @Test
  fun logSuspiciousURITest() {
    val request = Mockito.mock(HttpServletRequest::class.java)
    val knownUris = arrayOf(null, "/wa", "/wa/", "/react/mypage/dynamic", "/denied/../react/mypage")
    var counter = 0
    Mockito.`when`(request.requestURI).thenAnswer { knownUris[counter] }
    knownUris.forEach {
      assertFalse(LoggingFilter.logSuspiciousURI(request, null), "uri '$it' should be accepted.")
      ++counter
    }

    val unknownUris = arrayOf("/wa/../ini.php", "cmd.exe", "/secure/Logo.png")
    counter = 0
    Mockito.`when`(request.requestURI).thenAnswer { unknownUris[counter] }
    unknownUris.forEach {
      assertTrue(LoggingFilter.logSuspiciousURI(request, null), "uri '$it' shouldn't be accepted.")
      ++counter
    }
  }
}

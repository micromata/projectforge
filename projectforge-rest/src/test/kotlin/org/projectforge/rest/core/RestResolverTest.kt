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

package org.projectforge.rest.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.rest.AddressPagesRest

class RestResolverTest {
  @Test
  fun resolveTest() {
    test("address")
    test("address/edit", "edit")
    test("address/edit", "/edit")

    test("address?q=hurz", params = mapOf("q" to "hurz"))
    test("address?q=hurz&id=null", params = mapOf("q" to "hurz", "id" to null))
    test("address/edit?q=hurz", "edit", params = mapOf("q" to "hurz"))
    test("address/edit?q=hurz&id=null", "edit", params = mapOf("q" to "hurz", "id" to null))
  }

  private fun test(expected: String, subPath: String? = null, params: Map<String, Any?>? = null) {
    assertEquals(expected, RestResolver.getRestUrl(AddressPagesRest::class.java, subPath, true, params = params))
    assertEquals(
      "/rs/$expected",
      RestResolver.getRestUrl(AddressPagesRest::class.java, subPath, false, params = params)
    )
  }
}

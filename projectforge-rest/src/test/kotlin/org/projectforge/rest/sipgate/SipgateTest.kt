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


class SipgateTest {
  @Test
  fun base64Test() {
    val tokenId = "token-FQ1V12"
    val token = "e68ead46-a7db-46cd-8a1a-44aed1e4e372"
    Assertions.assertEquals(
      "dG9rZW4tRlExVjEyOmU2OGVhZDQ2LWE3ZGItNDZjZC04YTFhLTQ0YWVkMWU0ZTM3Mg==",
      SipgateClient.base64Credentials(tokenId, token)
    )
  }
}

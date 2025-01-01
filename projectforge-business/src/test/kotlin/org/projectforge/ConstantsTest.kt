/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ConstantsTest {
  @Test
  fun webDocTest() {
    Assertions.assertEquals("https://projectforge.org/changelog-posts/", Constants.WEB_DOCS_NEWS_LINK)
    Assertions.assertEquals("https://projectforge.org/docs", Constants.WEB_DOCS_LINK)
    Assertions.assertEquals(
      "https://projectforge.org/docs/userguide/#full_indexed_search",
      Constants.WEB_DOCS_LINK_HANDBUCH_LUCENE
    )
    Assertions.assertEquals(
      "https://projectforge.org/docs/adminguide/#securityconfig",
      Constants.WEB_DOCS_ADMIN_GUIDE_SECURITY_CONFIG_LINK
    )
  }
}

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

package org.projectforge.core

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.jpa.impl.HibernateSearchFilterUtils.modifySearchString

class BaseDaoTest {
  @Test
  fun modifySearchString() {
    Assertions.assertEquals("hallo*", modifySearchString("hallo"))
    Assertions.assertEquals("hallo* ProjectForge*", modifySearchString("hallo ProjectForge"))
    Assertions.assertEquals("ha1lo* ProjectForge*", modifySearchString("ha1lo ProjectForge"))
    Assertions.assertEquals("k.reinhard@projectforge*", modifySearchString("k.reinhard@projectforge"))
    Assertions.assertEquals(
      "email:k.reinhard@projectforge",
      modifySearchString("email:k.reinhard@projectforge")
    )
    Assertions.assertEquals("hallo", modifySearchString("'hallo"))
    Assertions.assertEquals("title:hallo", modifySearchString("'title:hallo"))
    Assertions.assertEquals(
      "(hallo* AND test* NOT hurz* OR test*)",
      modifySearchString("hallo AND test NOT hurz OR test")
    )
    Assertions.assertEquals(
      "(hallo* AND 2008-11-21 NOT hurz* OR test*)",
      modifySearchString("hallo AND 2008-11-21 NOT hurz OR test")
    )
    Assertions.assertEquals("-hallo", modifySearchString("-hallo"))
    Assertions.assertEquals("+hallo", modifySearchString("+hallo"))
    Assertions.assertEquals("+hallo", modifySearchString("+hallo"))
    Assertions.assertEquals("h+a-llo", modifySearchString("h+a-llo"))
    Assertions.assertEquals("hu-melder", modifySearchString("hu-melder"))
    Assertions.assertEquals("*h+a-llo*", modifySearchString("*h+a-llo*"))
  }
}

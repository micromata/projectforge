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
    // A word the index holds as several terms is searched either as typed or as those terms - see
    // [SearchStringTokenizer]: '@' separates for the StandardAnalyzer, and a wildcard term is never tokenized
    // by Lucene, so 'k.reinhard@projectforge*' alone could only match a field analyzed as one keyword.
    Assertions.assertEquals(
      "(k.reinhard@projectforge* (+k.reinhard* +projectforge*))",
      modifySearchString("k.reinhard@projectforge")
    )
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
      "(hallo* AND (2008-11-21* (+2008* +11* +21*)) NOT hurz* OR test*)",
      modifySearchString("hallo AND 2008-11-21 NOT hurz OR test")
    )
    // A leading '+'/'-' is Lucene's required/prohibited operator, so those words stay as the user wrote them.
    Assertions.assertEquals("-hallo", modifySearchString("-hallo"))
    Assertions.assertEquals("+hallo", modifySearchString("+hallo"))
    Assertions.assertEquals("(h+a-llo* (+h* +a* +llo*))", modifySearchString("h+a-llo"))
    // The whole word or both of its terms: 'hu-melder' used to be searched without any wildcard at all, so
    // 'hu-meld' found nothing.
    Assertions.assertEquals("(hu-melder* (+hu* +melder*))", modifySearchString("hu-melder"))
    // Wildcards of the user's own: untouched, as before.
    Assertions.assertEquals("*h+a-llo*", modifySearchString("*h+a-llo*"))
  }

  /**
   * The AND search of the global search form: every word required, and a word split by the analyzer required
   * as a whole (either reading of it must match).
   */
  @Test
  fun modifyAndSearchStringTest() {
    Assertions.assertEquals("+hallo* +test*", modifySearchString("hallo test", true))
    Assertions.assertEquals("+(dhl-pop* (+dhl* +pop*)) +test*", modifySearchString("dhl-pop test", true))
    Assertions.assertEquals("(dhl-pop* (+dhl* +pop*))", modifySearchString("dhl-pop", true))
    Assertions.assertEquals(
      "+hallo* -test",
      modifySearchString("hallo -test", true),
      "The prohibited word keeps its operator and its exact term, as before.",
    )
  }
}

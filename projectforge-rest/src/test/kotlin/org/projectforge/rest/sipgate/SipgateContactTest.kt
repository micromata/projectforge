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


class SipgateContactTest {
  @Test
  fun getCompareTest() {
    Assertions.assertTrue(SipgateNumber.compare(null, null))
    Assertions.assertTrue(SipgateNumber.compare(arrayOf(), arrayOf()))
    Assertions.assertTrue(SipgateNumber.compare(arrayOf("work"), arrayOf("work")))
    Assertions.assertTrue(SipgateNumber.compare(arrayOf("work", "cell"), arrayOf("work", "cell")))
    Assertions.assertTrue(SipgateNumber.compare(arrayOf("cell", "work"), arrayOf("work", "cell")))

    Assertions.assertFalse(SipgateNumber.compare(null, arrayOf()))
    Assertions.assertFalse(SipgateNumber.compare(arrayOf(), null))
    Assertions.assertFalse(SipgateNumber.compare(arrayOf(), arrayOf("")))
    Assertions.assertFalse(SipgateNumber.compare(arrayOf("work"), arrayOf("home")))
    Assertions.assertFalse(SipgateNumber.compare(arrayOf("work", "cell"), arrayOf("home", "cell")))

    Assertions.assertTrue(SipgateNumber().setHome().isHome())
    Assertions.assertTrue(SipgateNumber().setWork().isWork())
    Assertions.assertTrue(SipgateNumber().setCell().isCell())
    Assertions.assertTrue(SipgateNumber().setFaxWork().isFaxWork())
    Assertions.assertTrue(SipgateNumber().setFaxHome().isFaxHome())
    Assertions.assertTrue(SipgateNumber().setOther().isOther())
    Assertions.assertTrue(SipgateNumber().setPager().isPager())
  }
}

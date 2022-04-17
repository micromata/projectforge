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

package org.projectforge.framework.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.Const
import org.projectforge.framework.i18n.I18nHelper.addBundleName
import org.projectforge.web.DAY
import org.projectforge.web.HOUR
import org.projectforge.web.MINUTE
import org.projectforge.web.SECOND
import java.util.*

class DurationTest {

  @Test
  fun i18nTest() {
    addBundleName(Const.RESOURCE_BUNDLE_NAME)
    assertEquals("", getMessage(null))
    assertEquals("", getMessage(0))
    assertEquals("1 second", getMessage(1 * SECOND))
    assertEquals("42 seconds", getMessage(42 * SECOND + 500))
    assertEquals("59 seconds", getMessage(1 * MINUTE - 1 ))
    assertEquals("1 minute", getMessage(1 * MINUTE ))
    assertEquals("1 minute", getMessage(1 * MINUTE + 1 ))
    assertEquals("1 minute", getMessage(1 * MINUTE + 999))
    assertEquals("1 minute 1 second", getMessage(1 * MINUTE + 1000))
    assertEquals("59 minutes 59 seconds", getMessage(59 * MINUTE + 59 * SECOND))
    assertEquals("1 hour", getMessage(60 * MINUTE))
    assertEquals("1 day", getMessage(24 * HOUR))
    assertEquals("24 days", getMessage(24 * DAY))
    assertEquals("180 days", getMessage(180 * DAY))
  }

  private fun getMessage(millis: Long?): String {
    return Duration.getMessage(millis, Locale.ENGLISH)
  }
}

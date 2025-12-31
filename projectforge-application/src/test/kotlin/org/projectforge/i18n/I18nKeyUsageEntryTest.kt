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

package org.projectforge.i18n

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.start.ProjectForgeApplication
import org.projectforge.ui.filter.LayoutListFilterUtils
import java.io.File


class I18nKeyUsageEntryTest {
  @Test
  fun getClassByFileTest() {
    Assertions.assertEquals(
      I18nKeysUsage::class.java,
      I18nKeyUsageEntry.getClassByFile(File("projectforge-application/src/main/kotlin/org/projectforge/i18n/I18nKeysUsage.kt"))
    )
    Assertions.assertNull(
      I18nKeyUsageEntry.getClassByFile(File("projectforge-application/src/main/kotlin/org/projectforge/i18n/Unknown.kt"), false)
    )
    Assertions.assertEquals(
      ProjectForgeApplication::class.java,
      I18nKeyUsageEntry.getClassByFile(File("projectforge-application/src/main/java/org/projectforge/start/ProjectForgeApplication.java"))
    )
    Assertions.assertEquals(
      LayoutListFilterUtils::class.java,
      I18nKeyUsageEntry.getClassByFile(
        File("projectforge-application/src/main/kotlin/org/projectforge/ui/filter/LayoutListFilterUtils.kt"),
      )
    )
  }
}

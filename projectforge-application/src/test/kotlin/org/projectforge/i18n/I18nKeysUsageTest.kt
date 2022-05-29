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

package org.projectforge.i18n

import org.junit.jupiter.api.Test


class CreateI18nKeysTest {
  /**
   * Creates the i18n key usage file on each test run (should be committed to Git after modifications).
   * This file is used for download Excel-file with i18 translations and usages from running ProjectForge-App.
   */
  @Test
  fun createI18nKeysUsageFile() {
    I18nKeysSourceAnalyzer().run()
  }
}

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

package org.projectforge.business.user

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.configuration.ConfigurationServiceAccessor
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.test.AbstractTestBase
import java.util.*

class UserLocaleTest {
  @Test
  fun determinLocaleTest() {
    try {
      ThreadLocalUserContext.clear()
      ConfigurationServiceAccessor.internalInitJunitTestMode()
      ConfigurationServiceAccessor.get().defaultLocale = null
      Locale.setDefault(Locale.ITALY)
      // 7. The system's locale
      Assertions.assertEquals(Locale.ITALY, UserLocale.determineUserLocale())
      // 6. The locale configured in ProjectForge config file: projectforge.defaultLocale
      ConfigurationServiceAccessor.get().defaultLocale = Locale.CANADA
      Assertions.assertEquals(Locale.CANADA, UserLocale.determineUserLocale())
      // 5. The given default locale.
      Assertions.assertEquals(Locale.FRANCE, UserLocale.determineUserLocale(defaultLocale = Locale.FRANCE))
      // 4. The locale set in ThreadLocal for public services without given user, if given: [ThreadLocalUserContext.getLocale]
      ThreadLocalUserContext.setLocale(Locale.CHINA)
      Assertions.assertEquals(Locale.CHINA, UserLocale.determineUserLocale(defaultLocale = Locale.FRANCE))
      // 3. For a given user the clientLocale, if given: [PFUserDO.clientLocale]
      val user = PFUserDO()
      user.clientLocale = Locale.KOREA
      Assertions.assertEquals(Locale.KOREA, UserLocale.determineUserLocale(user))
      // 2. For a given user the param defaultLocale, if set (update the field [PFUserDO.clientLocale].
      Assertions.assertEquals(Locale.FRANCE, UserLocale.determineUserLocale(user, Locale.FRANCE))
      Assertions.assertEquals(Locale.FRANCE, user.clientLocale, "The user's client locale should be changed to FRANCE")
      // 1. For a given user return the user locale, if configured: [PFUserDO.locale]
      user.locale = Locale.GERMAN
      Assertions.assertEquals(Locale.GERMAN, UserLocale.determineUserLocale(user, Locale.FRANCE))
    } finally {
      ThreadLocalUserContext.clear()
    }
  }
}

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

import org.projectforge.business.configuration.ConfigurationServiceAccessor
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.util.*

/**
 * Helper method for getting the best fitting user locale.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */

object UserLocale {
  /**
   * Determines the user's locale as best as it could be.
   * The [PFUserDO.clientLocale] is set by this method.
   * 1. For a given user return the user locale, if configured: [PFUserDO.locale]
   * 2. For a given user the param defaultLocale, if set (update the field [PFUserDO.clientLocale].
   * 3. For a given user the clientLocale, if given: [PFUserDO.clientLocale]
   * 4. The locale set in ThreadLocal for public services without given user, if given: [ThreadLocalUserContext.getLocale]
   * 5. The given default locale.
   * 6. The locale configured in ProjectForge config file: projectforge.defaultLocale
   * 7. The system's locale
   * @param The optional defaultLocale
   */
  @JvmStatic
  fun determineUserLocale(user: PFUserDO? = null, defaultLocale: Locale? = null): Locale {
    if (user != null) {
      // 1. For a given user return the user locale, if configured: [PFUserDO.locale]
      user.locale?.let { return it } // The locale configured in the data base for this user (MyAccount).

      // 2. For a given user the param defaultLocale, if set (update the field [PFUserDO.clientLocale].
      defaultLocale?.let {
        if (it != user.clientLocale) {
          user.clientLocale = it
        }
        return it
      }
      // 3. For a given user the clientLocale, if given: [PFUserDO.clientLocale]
      user.clientLocale?.let { return it }
    } else {
      // 4. The locale set in ThreadLocal for public services without given user, if given: [ThreadLocalUserContext.getLocale]
      ThreadLocalUserContext.internalGetThreadLocalLocale()?.let { return it }
    }
    // 5. The given default locale.
    defaultLocale?.let { return it }
    // 6. The locale configured in ProjectForge config file: projectforge.defaultLocale
    // 7. The system's locale
    return ConfigurationServiceAccessor.get().defaultLocale ?: Locale.getDefault()
  }
}

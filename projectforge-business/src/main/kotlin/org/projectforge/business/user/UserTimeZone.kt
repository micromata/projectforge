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

package org.projectforge.business.user

import org.projectforge.framework.configuration.Configuration.Companion.instance
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.util.*

/**
 * Helper method for getting the best fitting user's timezone.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */

object UserTimeZone {
  /**
   * Determines the user's timezone as best as it could be.
   * 1. For a given user (or thread local user) return the user locale, if configured: [PFUserDO.timeZone]
   * 2. Configured time zone in Configuration params.
   * 3. The system's default timeZone
   */
  @JvmStatic
  @JvmOverloads
  fun determineUserTimeZone(user: PFUserDO? = ThreadLocalUserContext.loggedInUser): TimeZone {
    user?.timeZone?.let { timeZone -> return timeZone }
    return instance.defaultTimeZone ?: TimeZone.getDefault()
  }

}

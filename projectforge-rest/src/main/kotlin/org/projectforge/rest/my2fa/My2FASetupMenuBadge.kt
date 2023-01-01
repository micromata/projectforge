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

package org.projectforge.rest.my2fa

import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.builder.MenuCreator
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.security.My2FAService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

/**
 * Menu badge is 1 or empty, depends on whether the user has setup all 2FAs or not.
 * The state of each user will be cached for performance reasons.
 */
@Service
class My2FASetupMenuBadge : AbstractCache(TICKS_PER_HOUR) {
  @Autowired
  private lateinit var menuCreator: MenuCreator

  @Autowired
  private lateinit var my2FAService: My2FAService

  /**
   * State by userId. True: setup finished, false: badge counter is 1.
   */
  private val stateMap = mutableMapOf<Int, Boolean>()

  @PostConstruct
  private fun postConstruct() {
    menuCreator.findById(MenuItemDefId.MY_2FA_SETUP)!!.badgeCounter = { badgeCounter }
  }

  /**
   * The badgeCounter for the logged-in user (will be cached).
   */
  val badgeCounter: Int?
    get() {
      ThreadLocalUserContext.user?.let { user ->
        synchronized(stateMap) {
          var state = stateMap[user.id]
          if (state == null) {
            state = my2FAService.userConfigured2FA
            stateMap[user.id] = state
          }
          return if (state) {
            null // State is OK (everything is configured)
          } else {
            1 // User has to configure 2FA.
          }
        }
      }
      return null // Shouldn't occur (ThreadLocalUser should always be given).
    }

  /**
   * Refresh the state for a single user. Will be called by [My2FASetupPageRest] for having an up-to-date state
   * after any modifications.
   */
  fun refreshUserBadgeCounter() {
    ThreadLocalUserContext.userId?.let {
      synchronized(stateMap) {
        stateMap.remove(it)
      }
    }
  }

  override fun refresh() {
    synchronized(stateMap) {
      stateMap.clear()
    }
  }
}

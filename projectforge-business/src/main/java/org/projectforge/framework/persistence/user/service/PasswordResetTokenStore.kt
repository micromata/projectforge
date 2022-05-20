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

package org.projectforge.framework.persistence.user.service

import org.projectforge.framework.time.TimeUnit
import org.projectforge.framework.utils.NumberHelper

/**
 * Storage for managing password reset token sent by mail. Each token is up to 10 minutes valid before deletion.
 */
internal object PasswordResetTokenStore {
  // Key is user-id.
  private val store = mutableMapOf<Int, ExpiringToken>()

  /**
   * @param Token sent to user (created via [createToken].
   * @return The user id assigned to the given token or null, if no token found.
   */
  fun checkToken(token: String): Int? {
    tidyUp()
    synchronized(store) {
      return store.entries.find { it.value.token == token }?.key
    }
  }

  /**
   * A token should be deleted after password reset is done. If not, it will be deleted after 10 minutes automatically.
   * @param token Token to delete.
   */
  fun deleteToken(token: String) {
    tidyUp()
    synchronized(store) {
      store.entries.removeIf { it.value.token == token }
    }
  }

  fun createToken(userId: Int): String {
    val token = NumberHelper.getSecureRandomAlphanumeric(30)
    tidyUp()
    synchronized(store) {
      store[userId] = ExpiringToken(token)
    }
    return token
  }

  private fun tidyUp() {
    synchronized(store) {
      val currentMillis = System.currentTimeMillis()
      store.entries.removeIf {
        currentMillis - it.value.createdMillis > EXPIRE_TIME
      }
    }
  }

  class ExpiringToken(var token: String) {
    val createdMillis: Long = System.currentTimeMillis()
  }

  private val EXPIRE_TIME = 10 * TimeUnit.MINUTE.millis
}

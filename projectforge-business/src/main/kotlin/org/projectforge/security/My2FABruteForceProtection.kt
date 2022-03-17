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

package org.projectforge.security

import mu.KotlinLogging
import org.projectforge.business.user.UserDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * The number of failed OTP checks per user including last try is stored.
 * If the number of retries exceeds 5, the user is blocked for one hour after each three failed retries.
 * If the number of retries exceeds 10 ([MAX_RETRIES_BEFORE_DEACTIVATING_USER]), the user will be deactivated.
 */
@Service
internal class My2FABruteForceProtection {
  @Autowired
  private lateinit var userDao: UserDao

  private class OTPCheckData {
    var counter: Int = 0
    var lastFailedTry: Long? = null
  }

  private val otpFailures = mutableMapOf<Int, OTPCheckData>()

  fun registerOTPFailure(userId: Int) {
    var counter = 0
    synchronized(otpFailures) {
      var data = otpFailures[userId]
      if (data == null) {
        data = OTPCheckData()
        otpFailures[userId] = data
      }
      data.counter++
      counter = data.counter
      data.lastFailedTry = System.currentTimeMillis()
    }
    if (counter >= MAX_RETRIES_BEFORE_DEACTIVATING_USER) {
      val user = userDao.internalGetOrLoad(userId)
      if (user == null) {
        log.error { "Internal error: Oups, user with id $userId not found in the data base. Can't deactivate user!!!!" }
      } else {
        user.deactivated = true
        log.warn { "Deactivating user '${user.username}' after $MAX_RETRIES_BEFORE_DEACTIVATING_USER OTP failures." }
        userDao.internalUndelete(user)
      }
    }
  }

  fun registerOTPSuccess(userId: Int) {
    synchronized(otpFailures) {
      otpFailures.remove(userId)
    }
  }

  fun getLastFailedTry(userId: Int): Long? {
    return getData(userId)?.lastFailedTry
  }

  fun isRetryAllowed(userId: Int): Boolean {
    val data = getData(userId) ?: return true
    if (data.counter < 5) {
      return true
    }
    data.lastFailedTry?.let {

    }
  }

  fun getNumberOfFailures(userId: Int): Int {
    return getData(userId)?.counter ?: 0
  }

  internal fun waitingMillis(userId: Int): Long {
    val data = getData(userId) ?: return 0

  }

  private fun getData(userId: Int): OTPCheckData? {
    synchronized(otpFailures) {
      return otpFailures[userId]
    }
  }

  companion object{
    const val MAX_RETRIES_BEFORE_DEACTIVATING_USER = 10
  }
}

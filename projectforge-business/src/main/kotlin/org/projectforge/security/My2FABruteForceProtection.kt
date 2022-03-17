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
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDOChangedListener
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.security.My2FABruteForceProtection.Companion.MAX_RETRIES_BEFORE_DEACTIVATING_USER
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

/**
 * The number of failed OTP checks per user including last try is stored.
 * If the number of retries exceeds 3, the user is blocked for one hour after each further three failed retries.
 * If the number of retries exceeds 12 ([MAX_RETRIES_BEFORE_DEACTIVATING_USER]), the user will be deactivated.
 *
 * Please note: This is only an in-memory solution. After a restart of ProjectForge, all failure counters will be reset and
 * the users are able to have up to 12 more tries (if not deactivated).
 */
@Service
internal class My2FABruteForceProtection {
  @Autowired
  internal lateinit var userDao: UserDao

  private class OTPCheckData {
    var counter: Int = 0
    var lastFailedTry: Long? = null
  }

  internal class UserChangeListener(val protection: My2FABruteForceProtection) : BaseDOChangedListener<PFUserDO> {
    override fun afterSaveOrModifify(changedObject: PFUserDO, operationType: OperationType) {
      val data = protection.getData(changedObject.id) ?: return
      if (operationType == OperationType.UPDATE && !changedObject.deactivated && data.counter >= MAX_RETRIES_BEFORE_DEACTIVATING_USER) {
        // User is probably changed from deactivated to activated again (by an admin user).
        // Reset the number of failed OTP retries for the user.
        log.info { "User '${changedObject.username} was modified, so reset the brute force protection for OTPs." }
        protection.registerOTPSuccess(changedObject.id)
      }
    }
  }

  private val otpFailures = mutableMapOf<Int, OTPCheckData>()

  internal val userChangeListener = UserChangeListener(this)

  @PostConstruct
  internal fun initialize() {
    userDao.register(userChangeListener)
  }

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
        userDao.internalUpdate(user)
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
    val lastFailedTry = data.lastFailedTry ?: return true
    val waitingMillis = getWaitingMillis(data.counter)
    return (waitingMillis == 0L || System.currentTimeMillis() - waitingMillis > lastFailedTry)
  }

  fun getNumberOfFailures(userId: Int): Int {
    return getData(userId)?.counter ?: 0
  }

  internal fun getWaitingMillis(counter: Int): Long {
    if (counter < MAX_RETRIES_BEFORE_TIME_PENALTY) {
      return 0
    }
    if (counter >= MAX_RETRIES_BEFORE_DEACTIVATING_USER) {
      return Long.MAX_VALUE
    }
    return if (counter.mod(3) == 0) {
      3_600_000L
    } else {
      0L
    }
  }

  private fun getData(userId: Int): OTPCheckData? {
    synchronized(otpFailures) {
      return otpFailures[userId]
    }
  }

  companion object {
    const val MAX_RETRIES_BEFORE_TIME_PENALTY = 3
    const val MAX_RETRIES_BEFORE_DEACTIVATING_USER = 12
  }
}

/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.business.user.UserDao
import org.projectforge.common.DateFormatType
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.TimeLeft
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.BaseDOChangedListener
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.TimeUnit
import org.projectforge.security.My2FABruteForceProtection.Companion.MAX_RETRIES_BEFORE_DEACTIVATING_USER
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

/**
 * The number of failed OTP checks per user including last try is stored.
 * If the number of retries exceeds 3, the user is blocked for one minute after each further three failed retries.
 * If the number of retries exceeds 12 ([MAX_RETRIES_BEFORE_DEACTIVATING_USER]), the user will be deactivated.
 *
 * Please note: This is only an in-memory solution. After a restart of ProjectForge, all failure counters will be reset and
 * the users are able to have up to 12 more tries (if not deactivated).
 * For external attackers awaiting restarts have to check the restart of ProjectForge after login (e. g. by looking in SystemStatistics
 * or version number and should stop after 11 brute force tries. This risk is acceptable.
 */
@Service
internal class My2FABruteForceProtection {
    @Autowired
    internal lateinit var userDao: UserDao

    @Autowired
    internal lateinit var persistenceService: PfPersistenceService

    private class OTPCheckData {
        var counter: Int = 0
        var lastFailedTry: Long? = null
    }

    /**
     * This listener handles a reset for a deactivated user after more than 12 failed tries. A admin may re-activate
     * an user and this listener will be notified in this case.
     */
    internal class UserChangeListener(val protection: My2FABruteForceProtection) : BaseDOChangedListener<PFUserDO> {
        override fun afterSaveOrModify(
            changedObject: PFUserDO,
            operationType: OperationType,
            context: PfPersistenceContext
        ) {
            val data = protection.getData(changedObject.id!!) ?: return
            if (operationType == OperationType.UPDATE && !changedObject.deactivated && data.counter >= MAX_RETRIES_BEFORE_DEACTIVATING_USER) {
                // User is probably changed from deactivated to activated again (by an admin user).
                // Reset the number of failed OTP retries for the user.
                log.info { "User '${changedObject.username} was modified, so reset the brute force protection for OTPs." }
                protection.registerOTPSuccess(changedObject.id!!)
            }
        }
    }

    private val otpFailures = mutableMapOf<Long, OTPCheckData>()

    internal val userChangeListener = UserChangeListener(this)

    @PostConstruct
    internal fun initialize() {
        userDao.register(userChangeListener)
    }

    /**
     * After an OTP failure, this method should be called.
     */
    fun registerOTPFailure(userId: Long = ThreadLocalUserContext.userId!!) {
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
            val user = userDao.internalGetById(userId)
            if (user == null) {
                log.error { "Internal error: Oups, user with id $userId not found in the data base. Can't deactivate user!!!!" }
            } else {
                user.deactivated = true
                log.warn { "Deactivating user '${user.username}' after $MAX_RETRIES_BEFORE_DEACTIVATING_USER OTP failures." }
                userDao.internalUpdateInTrans(user)
            }
        }
    }

    /**
     * After a successful OTP check, this method should be called. So the user has all retries again.
     */
    fun registerOTPSuccess(userId: Long = ThreadLocalUserContext.userId!!) {
        synchronized(otpFailures) {
            otpFailures.remove(userId)
        }
    }

    /**
     * This method should be called before every OTP check. If not allowed, the OTP check must be skipped and an
     * error message should be returned.
     */
    fun isBlocked(userId: Long = ThreadLocalUserContext.userId!!): Boolean {
        val data = getData(userId) ?: return false
        val lastFailedTry = data.lastFailedTry ?: return false
        val waitingMillis = getWaitingMillis(data.counter)
        return (waitingMillis != 0L && System.currentTimeMillis() - waitingMillis < lastFailedTry)
    }

    /**
     * If retry is not allowed, this method will return a localized message about the reason including the number
     * of failed retries, the risk of beeing deactivated as well as any time penalty.
     */
    fun getBlockedResult(userId: Long = ThreadLocalUserContext.userId!!): OTPCheckResult? {
        getBlockedMessage(userId)?.let { message ->
            return OTPCheckResult.BLOCKED.withMessage(message)
        }
        return null
    }

    /**
     * If retry is not allowed, this method will return a localized message about the reason including the number
     * of failed retries, the risk of beeing deactivated as well as any time penalty.
     */
    fun getBlockedMessage(userId: Long = ThreadLocalUserContext.userId!!): String? {
        if (!isBlocked(userId)) {
            return null
        }
        val data = getData(userId) ?: return "??? no user info found ???"
        val lastFailedTry = data.lastFailedTry ?: return "??? no last failure date found ???"
        val waitingMillis = getWaitingMillis(data.counter)
        val until = PFDateTime.from(lastFailedTry).plus(waitingMillis, ChronoUnit.MILLIS)
        val timeLeft = if (waitingMillis > TimeUnit.HOUR.millis) {
            TimeLeft.getMessage(until.utilDate)
        } else {
            TimeLeft.getMessage(until.utilDate, maxUnit = TimeUnit.SECONDS)
        }
        // You have been blocked until {0} ({1}) after {2} failed code checks. Please note, that your account might be deactivated after {3} failed OTP checks.
        return translateMsg(
            "user.My2FACode.error.timePenalty.message",
            until.format(DateFormatType.TIME_OF_DAY_SECONDS),
            timeLeft,
            data.counter,
            MAX_RETRIES_BEFORE_DEACTIVATING_USER
        )
    }

    fun getNumberOfFailures(userId: Long): Int {
        return getData(userId)?.counter ?: 0
    }

    internal fun getLastFailedTry(userId: Long): Long? {
        return getData(userId)?.lastFailedTry
    }

    internal fun getWaitingMillis(counter: Int): Long {
        if (counter < MAX_RETRIES_BEFORE_TIME_PENALTY) {
            return 0
        }
        if (counter >= MAX_RETRIES_BEFORE_DEACTIVATING_USER) {
            return YEARS_IN_FUTURE
        }
        return if (counter.mod(3) == 0) {
            TimeUnit.MINUTE.millis
        } else {
            0L
        }
    }

    /**
     * For test cases only.
     */
    internal fun setLastFailedTry(userId: Long, millis: Long) {
        getData(userId)?.lastFailedTry = millis
    }

    private fun getData(userId: Long): OTPCheckData? {
        synchronized(otpFailures) {
            return otpFailures[userId]
        }
    }

    companion object {
        const val MAX_RETRIES_BEFORE_TIME_PENALTY = 3
        const val MAX_RETRIES_BEFORE_DEACTIVATING_USER = 12
        val YEARS_IN_FUTURE = 10 * TimeUnit.YEAR.millis
    }
}

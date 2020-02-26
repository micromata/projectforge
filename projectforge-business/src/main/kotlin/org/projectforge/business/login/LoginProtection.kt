/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.login

import org.slf4j.LoggerFactory

/**
 * Class for avoiding brute force attacks by time offsets during login after failed login attempts. Usage:<br></br>
 *
 * <pre>
 * public boolean login(String clientIp, String username, String password)
 * {
 * long offset = LoginProtection.instance().getFailedLoginTimeOffsetIfExists(username, clientIp);
 * if (offset &gt; 0) {
 * final String seconds = String.valueOf(offset / 1000);
 * final int numberOfFailedAttempts = loginProtection.getNumberOfFailedLoginAttempts(username, clientIpAddress);
 * // setResponsePage(MessagePage.class, &quot;Your account is locked for &quot; + seconds +
 * // &quot; seconds due to &quot; + numberOfFailedAttempts + &quot; failed login attempts. Please try again later.&quot;);
 * return false;
 * }
 * boolean success = checkLogin(username, password); // Check the login however you want.
 * if (success == true) {
 * LoginProtection.instance().clearLoginTimeOffset(userId, clientIp);
 * return true;
 * } else {
 * LoginProtection.instance().incrementFailedLoginTimeOffset(userId, clientIp);
 * return false;
 * }
 * }
</pre> *
 *
 *
 * Time offsets for ip addresses should be much smaller (for avoiding penalties for normal usage by a lot of users behind the same NAT
 * system).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class LoginProtection private constructor() {
    /**
     * @return the mapByUserId
     */
    internal val mapByUserString = LoginProtectionMap()
    /**
     * @return the mapByIpAddress
     */
    internal val mapByIpAddress = LoginProtectionMap()

    /**
     * Call this before checking the login credentials. If a long > 0 is returned please don't proceed the login-procedure. Please display a
     * user message that the login was denied due previous failed login attempts. The user should try it later again (after x seconds). <br></br>
     * If a login offset exists for both (for user id and client's ip address) the larger value will be returned.
     *
     * @param userId          May-be null.
     * @param clientIpAddress May-be null.
     * @param authenticationType Type of authentication (or null) for separating time penalties for different types (DAV, REST_CLIENT and normal login).
     * @return the time offset for login if exist, otherwise 0.
     */
    @JvmOverloads
    fun getFailedLoginTimeOffsetIfExists(userId: String?, clientIpAddress: String?, authenticationType: String? = null): Long {
        var userIdOffset: Long = 0
        var ipAddressOffset: Long = 0
        val userString = getUserString(userId, authenticationType)
        if (userString != null) {
            userIdOffset = mapByUserString.getFailedLoginTimeOffsetIfExists(userString)
        }
        if (clientIpAddress != null) {
            ipAddressOffset = mapByIpAddress.getFailedLoginTimeOffsetIfExists(clientIpAddress)
        }
        return if (userIdOffset > ipAddressOffset) userIdOffset else ipAddressOffset
    }

    /**
     * Returns the number of failed login attempts. If failed login attempts exist for both (user id and ip) the larger value will be
     * returned.
     *
     * @param userId          May-be null.
     * @param clientIpAddress May-be null.
     * @param authenticationType Type of authentication (or null) for separating time penalties for different types (DAV, REST_CLIENT and normal login).
     * @return The number of failed login attempts (not expired ones) if exist, otherwise 0.
     */
    @JvmOverloads
    fun getNumberOfFailedLoginAttempts(userId: String?, clientIpAddress: String?, authenticationType: String? = null): Int {
        var failedLoginsForUserId = 0
        var failedLoginsForIpAddress = 0
        val userString = getUserString(userId, authenticationType)
        if (userString != null) {
            failedLoginsForUserId = mapByUserString.getNumberOfFailedLoginAttempts(userString)
        }
        if (clientIpAddress != null) {
            failedLoginsForIpAddress = mapByIpAddress.getNumberOfFailedLoginAttempts(clientIpAddress)
        }
        return if (failedLoginsForUserId > failedLoginsForIpAddress) failedLoginsForUserId else failedLoginsForIpAddress
    }

    /**
     * Call this method after successful authentication. The counter of failed logins will be cleared.
     *
     * @param userId          May-be null.
     * @param clientIpAddress May-be null.
     */
    @JvmOverloads
    fun clearLoginTimeOffset(username: String?, userId: Int?, clientIpAddress: String?, authenticationType: String? = null) {
        var userString = getUserString(username, authenticationType)
        if (userString != null) {
            if (mapByUserString.exists(userString)) {
                log.info("Clearing time penalty for login $userString after successful login.")
            }
            mapByUserString.clearLoginTimeOffset(userString)
        }
        userString = getUserString(userId, authenticationType)
        if (userString != null) {
            if (mapByUserString.exists(userString)) {
                log.info("Clearing time penalty for login $userString after successful login.")
            }
            mapByUserString.clearLoginTimeOffset(userString)
        }
        if (clientIpAddress != null) {
            if (mapByIpAddress.exists(clientIpAddress)) {
                log.info("Clearing time penalty for ip $clientIpAddress after successful login.")
            }
            mapByIpAddress.clearLoginTimeOffset(clientIpAddress)
        }
    }

    /**
     * Clears all entries of failed logins (counter and time stamps).
     */
    fun clearAll() {
        mapByUserString.clearAll()
        mapByIpAddress.clearAll()
    }

    /**
     * Increments the number of login failures.
     *
     * @param userId          May-be null.
     * @param clientIpAddress May-be null.
     * @param authenticationType Type of authentication (or null) for separating time penalties for different types (DAV, REST_CLIENT and normal login).
     * @return Login time offset in ms. If time offsets are given for both, the user id and the ip address, the larger one will be returned.
     */
    @JvmOverloads
    fun incrementFailedLoginTimeOffset(userId: String?, clientIpAddress: String?, authenticationType: String? = null): Long {
        var timeOffsetForUserId: Long = 0
        var timeOffsetForIpAddress: Long = 0
        val userString = getUserString(userId, authenticationType)
        if (userString != null) {
            timeOffsetForUserId = mapByUserString.incrementFailedLoginTimeOffset(userString)
            if (timeOffsetForUserId > 0) {
                log.warn("Time-offset (penalty) for user '$userString' increased: ${timeOffsetForUserId / 1000} seconds.")
            }
        }
        if (clientIpAddress != null) {
            timeOffsetForIpAddress = mapByIpAddress.incrementFailedLoginTimeOffset(clientIpAddress)
            if (timeOffsetForIpAddress > 0) {
                log.warn("Time-offset (penalty) for ip address '$clientIpAddress' increased: ${timeOffsetForIpAddress / 1000} seconds.")
            }
        }
        return if (timeOffsetForUserId > timeOffsetForIpAddress) timeOffsetForUserId else timeOffsetForIpAddress
    }

    private fun getUserString(user: Any?, authenticationType: String?): String? {
        if (user == null) {
            return authenticationType
        }
        if (authenticationType == null) {
            return "$user"
        }
        return "$authenticationType:$user"
    }

    companion object {
        private val log = LoggerFactory.getLogger(LoginProtection::class.java)
        /**
         * After this given number of failed logins (for one specific user id) the account penalty counter will be incremented.
         */
        private const val DEFAULT_NUMBER_OF_FAILED_LOGINS_BEFORE_INCREMENTING_FOR_USER_ID = 1
        /**
         * After this given number of failed logins (for one specific ip address) the account penalty counter will be incremented.
         */
        private const val DEFAULT_NUMBER_OF_FAILED_LOGINS_BEFORE_INCREMENTING_FOR_IP = 1000
        private val instance = LoginProtection()
        @JvmStatic
        fun instance(): LoginProtection {
            return instance
        }
    }

    /**
     * Singleton.
     */
    init {
        mapByUserString.numberOfFailedLoginsBeforeIncrementing = DEFAULT_NUMBER_OF_FAILED_LOGINS_BEFORE_INCREMENTING_FOR_USER_ID
        mapByIpAddress.numberOfFailedLoginsBeforeIncrementing = DEFAULT_NUMBER_OF_FAILED_LOGINS_BEFORE_INCREMENTING_FOR_IP
    }
}

/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.login;

/**
 * Class for avoiding brute force attacks by time offsets during login after failed login attempts. Usage:<br/>
 * 
 * <pre>
 * public boolean login(String clientIp, String username, String password)
 * {
 *   long offset = LoginProtection.instance().getFailedLoginTimeOffsetIfExists(username, clientIp);
 *   if (offset &gt; 0) {
 *     final String seconds = String.valueOf(offset / 1000);
 *     final int numberOfFailedAttempts = loginProtection.getNumberOfFailedLoginAttempts(username, clientIpAddress);
 *     // setResponsePage(MessagePage.class, &quot;Your account is locked for &quot; + seconds +
 *     // &quot; seconds due to &quot; + numberOfFailedAttempts + &quot; failed login attempts. Please try again later.&quot;);
 *     return false;
 *   }
 *   boolean success = checkLogin(username, password); // Check the login however you want.
 *   if (success == true) {
 *     LoginProtection.instance().clearLoginTimeOffset(userId, clientIp);
 *     return true;
 *   } else {
 *     LoginProtection.instance().incrementFailedLoginTimeOffset(userId, clientIp);
 *     return false;
 *   }
 * }
 * </pre>
 * 
 * Time offsets for ip addresses should be much smaller (for avoiding penalties for normal usage by a lot of users behind the same NAT
 * system).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class LoginProtection
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoginProtection.class);

  /**
   * After this given number of failed logins (for one specific user id) the account penalty counter will be incremented.
   */
  private static final int DEFAULT_NUMBER_OF_FAILED_LOGINS_BEFORE_INCREMENTING_FOR_USER_ID = 1;

  /**
   * After this given number of failed logins (for one specific ip address) the account penalty counter will be incremented.
   */
  private static final int DEFAULT_NUMBER_OF_FAILED_LOGINS_BEFORE_INCREMENTING_FOR_IP = 1000;

  private static final LoginProtection instance = new LoginProtection();

  public static LoginProtection instance()
  {
    return instance;
  }

  /**
   * Singleton.
   */
  private LoginProtection()
  {
    mapByUserId = new LoginProtectionMap();
    mapByUserId.setNumberOfFailedLoginsBeforeIncrementing(DEFAULT_NUMBER_OF_FAILED_LOGINS_BEFORE_INCREMENTING_FOR_USER_ID);
    mapByIpAddress = new LoginProtectionMap();
    mapByIpAddress.setNumberOfFailedLoginsBeforeIncrementing(DEFAULT_NUMBER_OF_FAILED_LOGINS_BEFORE_INCREMENTING_FOR_IP);
  }

  private final LoginProtectionMap mapByUserId;

  private final LoginProtectionMap mapByIpAddress;

  /**
   * Call this before checking the login credentials. If a long > 0 is returned please don't proceed the login-procedure. Please display a
   * user message that the login was denied due previous failed login attempts. The user should try it later again (after x seconds). <br/>
   * If a login offset exists for both (for user id and client's ip address) the larger value will be returned.
   * @param userId May-be null.
   * @param clientIpAddress May-be null.
   * @return the time offset for login if exist, otherwise 0.
   */
  public long getFailedLoginTimeOffsetIfExists(final String userId, final String clientIpAddress)
  {
    long userIdOffset = 0;
    long ipAddressOffset = 0;
    if (userId != null) {
      userIdOffset = mapByUserId.getFailedLoginTimeOffsetIfExists(userId);
    }
    if (clientIpAddress != null) {
      ipAddressOffset = mapByIpAddress.getFailedLoginTimeOffsetIfExists(clientIpAddress);
    }
    return (userIdOffset > ipAddressOffset) ? userIdOffset : ipAddressOffset;
  }

  /**
   * Returns the number of failed login attempts. If failed login attempts exist for both (user id and ip) the larger value will be
   * returned.
   * @param userId May-be null.
   * @param clientIpAddress May-be null.
   * @return The number of failed login attempts (not expired ones) if exist, otherwise 0.
   */
  public int getNumberOfFailedLoginAttempts(final String userId, final String clientIpAddress)
  {
    int failedLoginsForUserId = 0;
    int failedLoginsForIpAddress = 0;
    if (userId != null) {
      failedLoginsForUserId = mapByUserId.getNumberOfFailedLoginAttempts(userId);
    }
    if (clientIpAddress != null) {
      failedLoginsForIpAddress = mapByIpAddress.getNumberOfFailedLoginAttempts(clientIpAddress);
    }
    return (failedLoginsForUserId > failedLoginsForIpAddress) ? failedLoginsForUserId : failedLoginsForIpAddress;
  }

  /**
   * Call this method after successful authentication. The counter of failed logins will be cleared.
   * @param userId May-be null.
   * @param clientIpAddress May-be null.
   */
  public void clearLoginTimeOffset(final String userId, final String clientIpAddress)
  {
    if (userId != null) {
      mapByUserId.clearLoginTimeOffset(userId);
    }
    if (clientIpAddress != null) {
      mapByIpAddress.clearLoginTimeOffset(clientIpAddress);
    }
  }

  /**
   * Clears all entries of failed logins (counter and time stamps).
   */
  public void clearAll()
  {
    mapByUserId.clearAll();
    mapByIpAddress.clearAll();
  }

  /**
   * Increments the number of login failures.
   * @param userId May-be null.
   * @param clientIpAddress May-be null.
   * @return Login time offset in ms. If time offsets are given for both, the user id and the ip address, the larger one will be returned.
   */
  public long incrementFailedLoginTimeOffset(final String userId, final String clientIpAddress)
  {
    long timeOffsetForUserId = 0;
    long timeOffsetForIpAddress = 0;
    if (userId != null) {
      timeOffsetForUserId = mapByUserId.incrementFailedLoginTimeOffset(userId);
      if (timeOffsetForUserId > 0) {
        log.warn("Time-offset (penalty) for user '" + userId + "' increased: " + (timeOffsetForUserId / 1000) + " seconds.");
      }
    }
    if (clientIpAddress != null) {
      timeOffsetForIpAddress = mapByIpAddress.incrementFailedLoginTimeOffset(clientIpAddress);
      if (timeOffsetForIpAddress > 0) {
        log.warn("Time-offset (penalty) for ip address '"
            + clientIpAddress
            + "' increased: "
            + (timeOffsetForIpAddress / 1000)
            + " seconds.");
      }
    }
    return (timeOffsetForUserId > timeOffsetForIpAddress) ? timeOffsetForUserId : timeOffsetForIpAddress;
  }

  /**
   * @return the mapByIpAddress
   */
  public LoginProtectionMap getMapByIpAddress()
  {
    return mapByIpAddress;
  }

  /**
   * @return the mapByUserId
   */
  public LoginProtectionMap getMapByUserId()
  {
    return mapByUserId;
  }
}

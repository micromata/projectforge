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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Class used by {@link LoginProtection} for handling maps, time offsets etc.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class LoginProtectionMap
{
  /**
   * Login offset time after failed login attempts expires after 24h.
   */
  private long loginOffsetExpiresAfterMs = 24 * 60 * 60 * 1000;

  /**
   * Login time offset will be number of failed logins multiplied by this value (in ms).
   */
  private long loginTimeOffsetScale = 1000L;

  private int numberOfFailedLoginsBeforeIncrementing;

  /**
   * Number of failed logins per IP address/user id.
   */
  private final Map<String, Integer> loginFailedAttemptsMap = new HashMap<String, Integer>();

  /**
   * Time stamp of last failed login per IP address/user Id in ms since 01/01/1970.
   * @see System#currentTimeMillis()
   */
  private final Map<String, Long> lastFailedLoginMap = new HashMap<String, Long>();

  /**
   * Call this before checking the login credentials. If a long > 0 is returned please don't proceed the login-procedure. Please display a
   * user message that the login was denied due previous failed login attempts. The user should try it later again (after x seconds).
   * @param userId This could be the client's ip address, the login name etc.
   * @return 0 if no active time offset was found, otherwise the time offset left until the account is opened again for login.
   */
  public long getFailedLoginTimeOffsetIfExists(final String id)
  {
    final Long lastFailedLoginInMs = this.lastFailedLoginMap.get(id);
    if (lastFailedLoginInMs == null) {
      return 0;
    }
    final long offset = getFailedLoginTimeOffset(id, false);
    final long currentTimeInMs = System.currentTimeMillis();
    if (lastFailedLoginInMs + offset < currentTimeInMs) {
      return 0;
    }
    return lastFailedLoginInMs + offset - currentTimeInMs;
  }

  /**
   * Increments the number of login failures.
   * @param id This could be the client's ip address, the login name etc.
   * @return Login time offset in ms.
   */
  public long incrementFailedLoginTimeOffset(final String id)
  {
    return getFailedLoginTimeOffset(id, true);
  }

  /**
   * @param id This could be the client's ip address, the login name etc.
   * @param increment If true the login fail counter will be incremented.
   * @return
   */
  private long getFailedLoginTimeOffset(final String id, final boolean increment)
  {
    clearExpiredEntries();
    final long currentTimeInMillis = System.currentTimeMillis();
    Integer numberOfFailedLogins = this.loginFailedAttemptsMap.get(id);
    if (numberOfFailedLogins == null) {
      if (increment == false) {
        return 0;
      }
      numberOfFailedLogins = 0;
    } else {
      final Long lastFailedLoginInMs = this.lastFailedLoginMap.get(id);
      if (lastFailedLoginInMs != null && currentTimeInMillis - lastFailedLoginInMs > loginOffsetExpiresAfterMs) {
        // Last failed login entry is to old, so we'll ignore and clear it:
        clearLoginTimeOffset(id);
        if (increment == false) {
          return 0;
        }
        numberOfFailedLogins = 0;
      }
    }
    if (increment == true) {
      synchronized (this) {
        this.loginFailedAttemptsMap.put(id, ++numberOfFailedLogins);
        this.lastFailedLoginMap.put(id, currentTimeInMillis);
      }
    }
    return (numberOfFailedLogins / numberOfFailedLoginsBeforeIncrementing) * loginTimeOffsetScale;
  }

  /**
   * Call this method after successful authentication. The counter of failed logins will be cleared.
   * @param id This could be the client's ip address, the login name etc.
   */
  public void clearLoginTimeOffset(final String id)
  {
    synchronized (this) {
      this.loginFailedAttemptsMap.remove(id);
      this.lastFailedLoginMap.remove(id);
    }
  }

  /**
   * Clears (removes) all entries for id's (user id's, ip addresses) older than {@link #DEFAULT_LOGIN_OFFSET_EXPIRES_AFTER_MS}.
   */
  public void clearExpiredEntries()
  {
    final long currentTimeInMillis = System.currentTimeMillis();
    synchronized (this) {
      final Iterator<String> it = this.lastFailedLoginMap.keySet().iterator();
      while (it.hasNext() == true) {
        final String key = it.next();
        final Long lastFailedLoginInMs = this.lastFailedLoginMap.get(key);
        if (lastFailedLoginInMs != null && currentTimeInMillis - lastFailedLoginInMs > loginOffsetExpiresAfterMs) {
          // Last failed login entry is to old, so we'll ignore and clear it:
          this.loginFailedAttemptsMap.remove(key);
          it.remove();
        }
      }
    }
  }

  /**
   * Clears all entries of failed logins (counter and time stamps).
   */
  public void clearAll()
  {
    synchronized (this) {
      this.loginFailedAttemptsMap.clear();
      this.lastFailedLoginMap.clear();
    }
  }

  /**
   * For internal use by test cases.
   */
  int getSizeOfLastFailedLoginMap()
  {
    return this.lastFailedLoginMap.size();
  }

  /**
   * For internal use by test cases.
   */
  int getSizeOfLoginFailedAttemptsMap()
  {
    return this.loginFailedAttemptsMap.size();
  }

  /**
   * For internal use by test cases.
   */
  void setEntry(final String id, final int numberOfFailedLoginAttempts, final long lastFailedAttemptTimestamp)
  {
    synchronized (this) {
      this.loginFailedAttemptsMap.put(id, numberOfFailedLoginAttempts);
      this.lastFailedLoginMap.put(id, lastFailedAttemptTimestamp);
    }
  }

  /**
   * @param id This could be the client's ip address, the login name etc.
   * @return The number of failed login attempts (not expired ones) if exist, otherwise 0.
   */
  public int getNumberOfFailedLoginAttempts(final String id)
  {
    final Integer result = this.loginFailedAttemptsMap.get(id);
    return result != null ? result : 0;
  }

  /**
   * After this number of ms (24h is the default value) after the last failed login an entry for a failed login (for both: by user id and by
   * ip) is removed.
   * @return the loginOffsetExpiresAfterMs
   */
  public long getLoginOffsetExpiresAfterMs()
  {
    return loginOffsetExpiresAfterMs;
  }

  /**
   * @param loginOffsetExpiresAfterMs the loginOffsetExpiresAfterMs to set
   * @return this for chaining.
   * @see #getLoginOffsetExpiresAfterMs()
   */
  public LoginProtectionMap setLoginOffsetExpiresAfterMs(final long loginOffsetExpiresAfterMs)
  {
    this.loginOffsetExpiresAfterMs = loginOffsetExpiresAfterMs;
    return this;
  }

  /**
   * After failed logins the login time offset is increased by this value (default is 1 second).
   * @return the loginTimeOffsetScale
   */
  public long getLoginTimeOffsetScale()
  {
    return loginTimeOffsetScale;
  }

  /**
   * @param loginTimeOffsetScale the loginTimeOffsetScale to set
   * @return this for chaining.
   * @see #getLoginTimeOffsetScale()
   */
  public LoginProtectionMap setLoginTimeOffsetScale(final long loginTimeOffsetScale)
  {
    this.loginTimeOffsetScale = loginTimeOffsetScale;
    return this;
  }

  /**
   * This amount contains the number of required failed logins before incrementing the time offset.
   * @return the numberOfFailedLoginsBeforeIncrementing
   */
  public int getNumberOfFailedLoginsBeforeIncrementing()
  {
    return numberOfFailedLoginsBeforeIncrementing;
  }

  /**
   * @param numberOfFailedLoginsBeforeIncrementing the numberOfFailedLoginsBeforeIncrementing to set
   * @return this for chaining.
   * @see #getNumberOfFailedLoginsBeforeIncrementing()
   */
  public LoginProtectionMap setNumberOfFailedLoginsBeforeIncrementing(final int numberOfFailedLoginsBeforeIncrementing)
  {
    this.numberOfFailedLoginsBeforeIncrementing = numberOfFailedLoginsBeforeIncrementing;
    return this;
  }
}

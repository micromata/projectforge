/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.framework.cache;

import java.io.Serializable;

/**
 * This class is usefull, if the stored object of derived classes has to be cached. After reaching expireTime during a
 * request, the method refresh will be called.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class AbstractCache implements Serializable
{
  private static final long serialVersionUID = 7148463321579100086L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractCache.class);

  public static final long TICKS_PER_SECOND = 1000;

  public static final long TICKS_PER_MINUTE = TICKS_PER_SECOND * 60;

  public static final long TICKS_PER_HOUR = TICKS_PER_MINUTE * 60;

  public static final long TICKS_PER_DAY = 24 * TICKS_PER_HOUR;

  protected long expireTime = 60 * TICKS_PER_MINUTE;

  private transient long timeOfLastRefresh = -1;

  private transient boolean isExpired = true;

  private transient boolean refreshInProgress = false;

  protected AbstractCache()
  {
  }

  /**
   * @param expireTime in milliseconds.
   */
  protected AbstractCache(final long expireTime)
  {
    this.expireTime = expireTime;
  }

  public void setExpireTimeInMinutes(final long expireTime)
  {
    this.expireTime = expireTime * TICKS_PER_MINUTE;
  }

  public void setExpireTimeInSeconds(final long expireTime)
  {
    this.expireTime = expireTime * TICKS_PER_SECOND;
  }

  public void setExpireTimeInHours(final long expireTime)
  {
    this.expireTime = expireTime * TICKS_PER_HOUR;
  }

  /**
   * Cache will be refreshed before next use.
   */
  public void setExpired()
  {
    this.isExpired = true;
  }

  /**
   * Sets the cache to expired and calls checkRefresh, which forces refresh.
   */
  public void forceReload()
  {
    setExpired();
    checkRefresh();
  }

  /**
   * Checks the expire time and calls refresh, if cache is expired.
   */
  protected synchronized void checkRefresh()
  {
    if (refreshInProgress == true) {
      // Do nothing because refreshing is already in progress.
      return;
    }
    if (this.isExpired == true || System.currentTimeMillis() - this.timeOfLastRefresh > this.expireTime) {
      try {
        refreshInProgress = true;
        this.timeOfLastRefresh = System.currentTimeMillis();
        try {
          this.refresh();
        } catch (final Throwable ex) {
          log.error(ex.getMessage(), ex);
        }
        this.isExpired = false;
      } finally {
        refreshInProgress = false;
      }
    }
  }

  /**
   * @return true if currently a cache refresh is running, otherwise false.
   */
  public boolean isRefreshInProgress()
  {
    return refreshInProgress;
  }

  /**
   * Please implement this method refreshing the stored object _data. Do not forget to call checkRefresh in your cache
   * methods.
   * 
   * @see #checkRefresh()
   */
  protected abstract void refresh();
}

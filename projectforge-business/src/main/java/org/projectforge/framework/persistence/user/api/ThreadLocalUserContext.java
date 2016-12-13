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

package org.projectforge.framework.persistence.user.api;

import org.apache.commons.lang.ObjectUtils;
import org.joda.time.DateTimeZone;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;

import java.util.Locale;
import java.util.TimeZone;

/**
 * ThreadLocal context.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class ThreadLocalUserContext
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ThreadLocalUserContext.class);

  private static ThreadLocal<UserContext> threadLocalUserContext = new ThreadLocal<UserContext>();

  /**
   * @return The user of ThreadLocal if exists.
   */
  public final static PFUserDO getUser()
  {
    final UserContext userContext = getUserContext();
    if (userContext == null) {
      return null;
    }
    return userContext.getUser();
  }

  public final static UserContext getUserContext()
  {
    return threadLocalUserContext.get();
  }

  public final static void clear()
  {
    threadLocalUserContext.set(null);
  }

  /**
   * If given user is null, {@link #clear()} is called.
   * 
   * @param user
   */
  public final static void setUser(UserGroupCache userGroupCache, final PFUserDO user)
  {
    if (user == null) {
      clear();
      return;
    }
    final UserContext userContext = new UserContext(user, userGroupCache);
    setUserContext(userContext);
  }

  public final static void setUserContext(final UserContext userContext)
  {
    final PFUserDO oldUser = getUser();
    PFUserDO newUser = userContext != null ? userContext.getUser() : null;
    if (log.isDebugEnabled() == true) {
      log.debug("setUserInfo: " + newUser != null ? newUser.getDisplayUsername()
          : "null" + ", was: " + oldUser != null ? oldUser
              .getDisplayUsername() : "null");
    }
    threadLocalUserContext.set(userContext);
    if (log.isDebugEnabled() == true) {
      newUser = getUser();
      log.debug("user is now: " + newUser != null ? newUser.getDisplayUsername() : "null");
    }
  }

  /**
   * @return The user id of the ThreadLocal user if exists.
   * @see #getUser()
   */
  public final static Integer getUserId()
  {
    final PFUserDO user = getUser();
    return user != null ? user.getId() : null;
  }

  /**
   * @return The locale of the user if exists, otherwise default locale.
   * @see #getUser()
   * @see PFUserDO#getLocale()
   */
  public final static Locale getLocale()
  {
    final PFUserDO user = getUser();
    return user != null && user.getLocale() != null ? user.getLocale() : getLocale(null);
  }

  /**
   * If context user's locale is null and the given defaultLocale is not null, then the context user's client locale
   * will be set to given defaultLocale.
   * 
   * @param defaultLocale will be used, if the context user or his user locale does not exist.
   * @return The locale of the user if exists, otherwise the given default locale or if null the system's default
   *         locale.
   * @see #getUser()
   * @see PFUserDO#getLocale()
   */
  public final static Locale getLocale(final Locale defaultLocale)
  {
    final PFUserDO user = getUser();
    final Locale userLocale = user != null ? user.getLocale() : null;
    if (userLocale != null) {
      return userLocale;
    }
    Locale clientLocale = user != null ? user.getClientLocale() : null;
    if (defaultLocale != null && user != null && ObjectUtils.equals(clientLocale, defaultLocale) == false) {
      user.setClientLocale(defaultLocale);
      clientLocale = defaultLocale;
    }
    if (clientLocale != null) {
      return clientLocale;
    }
    if (defaultLocale != null) {
      return defaultLocale;
    }
    final Locale locale = ConfigXml.getInstance().getDefaultLocale();
    return locale != null ? locale : Locale.getDefault();
  }

  /**
   * @return The timeZone of the user if exists, otherwise default timezone of the Configuration
   * @see #getUser()
   * @see PFUserDO#getTimeZoneObject()
   * @see Configuration#getDefaultTimeZone()
   */
  public final static TimeZone getTimeZone()
  {
    if (getUser() != null) {
      return getUser().getTimeZoneObject();
    }
    if (Configuration.getInstance() != null) {
      return Configuration.getInstance().getDefaultTimeZone();
    }
    return TimeZone.getDefault();
  }

  public final static DateTimeZone getDateTimeZone()
  {
    final TimeZone timeZone = getTimeZone();
    return DateTimeZone.forTimeZone(timeZone);
  }

  /**
   * The first day of the week, configured at the given user, if not configured {@link ConfigXml#getFirstDayOfWeek()} is
   * used.
   * 
   * @return
   * @see ConfigXml#getFirstDayOfWeek()
   */
  public final static int getCalendarFirstDayOfWeek()
  {
    final PFUserDO user = getUser();
    if (user != null) {
      final Integer firstDayOfWeek = user.getFirstDayOfWeek();
      if (firstDayOfWeek != null) {
        return firstDayOfWeek;
      }
    }
    return ConfigXml.getInstance().getFirstDayOfWeek();
  }

  public final static int getJodaFirstDayOfWeek()
  {
    return DateHelper.convertCalendarDayOfWeekToJoda(getCalendarFirstDayOfWeek());
  }

  public static String getLocalizedMessage(final String messageKey, final Object... params)
  {
    return I18nHelper.getLocalizedMessage(getLocale(), messageKey, params);
  }

  public static String getLocalizedString(final String key)
  {
    return I18nHelper.getLocalizedString(getLocale(), key);
  }
}

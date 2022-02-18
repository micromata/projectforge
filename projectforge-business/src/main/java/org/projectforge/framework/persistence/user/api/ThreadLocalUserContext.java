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

package org.projectforge.framework.persistence.user.api;

import org.joda.time.DateTimeZone;
import org.projectforge.business.configuration.ConfigurationServiceAccessor;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import java.text.Collator;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

/**
 * ThreadLocal context.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class ThreadLocalUserContext {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ThreadLocalUserContext.class);

  private static ThreadLocal<UserContext> threadLocalUserContext = new ThreadLocal<>();

  private static ThreadLocal<Locale> threadLocalLocale = new ThreadLocal<>();

  /**
   * @return The user of ThreadLocal if exists.
   */
  public static PFUserDO getUser() {
    final UserContext userContext = getUserContext();
    if (userContext == null) {
      return null;
    }
    return userContext.getUser();
  }

  public static UserContext getUserContext() {
    return threadLocalUserContext.get();
  }

  public static void clear() {
    threadLocalUserContext.set(null);
    threadLocalLocale.set(null);
  }

  /**
   * If given user is null, {@link #clear()} is called. Creates a new UserContext object containing the given user.
   *
   * @param user
   * @return UserContext registered or null, if no user given.
   */
  public static UserContext setUser(final PFUserDO user) {
    if (user == null) {
      clear();
      return null;
    }
    final UserContext userContext = new UserContext(user);
    setUserContext(userContext);
    return userContext;
  }

  public static void setUserContext(final UserContext userContext) {
    final PFUserDO oldUser = getUser();
    PFUserDO newUser = userContext != null ? userContext.getUser() : null;
    if (log.isDebugEnabled()) {
      log.debug("setUserInfo: " + newUser != null ? newUser.getUserDisplayName()
          : "null" + ", was: " + oldUser != null ? oldUser
          .getUserDisplayName() : "null");
    }
    threadLocalUserContext.set(userContext);
    threadLocalLocale.set(null);
    if (log.isDebugEnabled()) {
      newUser = getUser();
      log.debug("user is now: " + newUser != null ? newUser.getUserDisplayName() : "null");
    }
  }

  /**
   * @return The user id of the ThreadLocal user if exists.
   * @see #getUser()
   */
  public static Integer getUserId() {
    final PFUserDO user = getUser();
    return user != null ? user.getId() : null;
  }

  /**
   * @return The locale of the user if exists, otherwise default locale.
   * @see #getUser()
   * @see PFUserDO#getLocale()
   */
  public static Locale getLocale() {
    return getLocale(null);
  }

  /**
   * Only for anonymous usage (needed by translations). Will throw an exception, if an user is already attached.
   *
   * @param locale
   */
  public static void setLocale(Locale locale) {
    if (getUser() != null) {
      throw new IllegalStateException("Can't register locale if an user is already registered. setLocale(Locale) should only used for public/anonymous services.");
    }
    threadLocalLocale.set(locale);
  }

  /**
   * If context user's locale is null and the given defaultLocale is not null, then the context user's client locale
   * will be set to given defaultLocale.
   *
   * @param defaultLocale will be used, if the context user or his user locale does not exist. If given, it's the client's
   *                      locale (browser locale) in common.
   * @return The locale of the user if exists, otherwise the given default locale or if null the system's default
   * locale.
   * @see #getUser()
   * @see PFUserDO#getLocale()
   */
  public static Locale getLocale(final Locale defaultLocale) {
    final PFUserDO user = getUser();
    Locale locale;
    if (user != null) {
      // Logged-in user
      locale = user.getLocale(); // The locale configured in the data base for this user (MyAccount).
      if (locale != null) {
        return locale;
      }
      locale = user.getClientLocale(); // The locale given by the client (browser).
      if (defaultLocale != null && !Objects.equals(locale, defaultLocale)) {
        user.setClientLocale(defaultLocale); // client locale changed? So update UserContext.
        return defaultLocale;
      }
    } else {
      // For non logged-in users and public pages, the locale could be set:
      locale = threadLocalLocale.get();
    }
    if (locale != null) {
      return locale;
    }
    if (defaultLocale != null) {
      return defaultLocale;
    }
    locale = ConfigurationServiceAccessor.get().getDefaultLocale();
    return locale != null ? locale : Locale.getDefault();
  }

  /**
   * @return The timeZone of the user if exists, otherwise default timezone of the Configuration
   * @see #getUser()
   * @see PFUserDO#getTimeZone()
   * @see Configuration#getDefaultTimeZone()
   */
  public static TimeZone getTimeZone() {
    if (getUser() != null) {
      return getUser().getTimeZone();
    }
    if (Configuration.getInstance() != null) {
      return Configuration.getInstance().getDefaultTimeZone();
    }
    return TimeZone.getDefault();
  }

  /**
   * @return The timeZone of the user if exists, otherwise default timezone of the Configuration
   * @see #getUser()
   * @see PFUserDO#getTimeZone()
   * @see Configuration#getDefaultTimeZone()
   */
  public static ZoneId getZoneId() {
    return getTimeZone().toZoneId();
  }

  @Deprecated
  public static DateTimeZone getDateTimeZone() {
    final TimeZone timeZone = getTimeZone();
    return DateTimeZone.forTimeZone(timeZone);
  }

  /**
   * The first day of the week, configured at the given user, if not configured {@link org.projectforge.business.configuration.ConfigurationService#getDefaultFirstDayOfWeek()} is
   * used.
   */
  public static DayOfWeek getFirstDayOfWeek() {
    final PFUserDO user = getUser();
    if (user != null) {
      final DayOfWeek firstDayOfWeek = user.getFirstDayOfWeek();
      if (firstDayOfWeek != null) {
        return firstDayOfWeek;
      }
    }
    return ConfigurationServiceAccessor.get().getDefaultFirstDayOfWeek();
  }

  /**
   * @return 1 - Monday, ..., 7 - Sunday
   */
  public static int getFirstDayOfWeekValue() {
    return getFirstDayOfWeek().getValue();
  }

  public static String getLocalizedMessage(final String messageKey, final Object... params) {
    return I18nHelper.getLocalizedMessage(getLocale(), messageKey, params);
  }

  public static String getLocalizedString(final String key) {
    return I18nHelper.getLocalizedMessage(getLocale(), key);
  }

  /**
   * Use this instead of String{@link String#compareTo(String)}, because it uses the user's locale for comparison.
   *
   * @param a
   * @param b
   * @return
   */
  public static int localeCompare(String a, String b) {
    return getLocaleComparator().compare(a, b);
  }

  /**
   * Use this instead of String{@link String#compareTo(String)}, because it uses the user's locale for comparison.
   *
   * @return
   */
  public static Comparator getLocaleComparator() {
    return Collator.getInstance(getLocale());
  }
}

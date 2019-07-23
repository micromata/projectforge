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

package org.projectforge.framework.utils;

import org.projectforge.business.configuration.ConfigurationServiceAccessor;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

import java.text.Collator;
import java.util.Locale;

/**
 * Uses {@link Collator} and the user's locale to compare string.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class StringComparator
{
  private static final StringComparator instance = new StringComparator();

  public static StringComparator getInstance()
  {
    return instance;
  }

  private Collator germanCollator;

  private Collator defaultCollator;

  private final Locale german = new Locale("de");

  /**
   * Uses {@link ThreadLocalUserContext#getLocale()} as locale in ascending order.
   * @param s1
   * @param s2
   * @return The result of {@link Collator#compare(String, String)}.
   * @see #compare(String, String, Locale)
   */
  public int compare(final String s1, final String s2)
  {
    return compare(s1, s2, true);
  }

  /**
   * Uses {@link ThreadLocalUserContext#getLocale()} as locale.
   * @param s1
   * @param s2
   * @param asc
   * @return The result of {@link Collator#compare(String, String)}.
   * @see #compare(String, String, Locale)
   */
  public int compare(final String s1, final String s2, final boolean asc)
  {
    return compare(s1, s2, asc, ThreadLocalUserContext.getLocale());
  }

  /**
   * Using ascending order.
   * @param s1
   * @param s2
   * @param locale
   * @see #compare(String, String, boolean, Locale)
   */
  public int compare(final String s1, final String s2, final Locale locale)
  {
    return compare(s1, s2, true, locale);
  }

  /**
   * Gets the Collator for the given locale and uses this collator for the string comparison.
   * @param s1
   * @param s2
   * @param asc
   * @param locale
   * @return The result of {@link Collator#compare(String, String)} or -1, 0, 1 if one or both string paramters are null.
   */
  public int compare(final String s1, final String s2, final boolean asc, final Locale locale)
  {
    if (s1 == null) {
      if (s2 == null)
        return 0;
      else return (asc == true) ? -1 : 1;
    }
    if (s2 == null) {
      return (asc == true) ? 1 : -1;
    }
    final Collator collator = getCollator(locale);
    if (asc == true) {
      return collator.compare(s1, s2);
    }
    return -collator.compare(s1, s2);
  }

  private Collator getCollator(final Locale locale)
  {
    if (locale != null) {
      if (german.getLanguage().equals(locale.getLanguage()) == true) {
        return getGermanCollator();
      }
    }
    return getDefaultCollator();
  }

  private Collator getDefaultCollator()
  {
    if (defaultCollator == null) {
      Locale locale = ConfigurationServiceAccessor.get().getDefaultLocale();
      if (locale == null) {
        locale = Locale.getDefault();
      }
      defaultCollator = Collator.getInstance(locale);
    }
    return defaultCollator;
  }

  private Collator getGermanCollator()
  {
    if (germanCollator == null) {
      germanCollator = Collator.getInstance(Locale.GERMAN);
      germanCollator.setStrength(Collator.SECONDARY);// a == A, a < Ä
    }
    return germanCollator;
  }
}

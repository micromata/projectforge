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

package org.projectforge.business.converter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class LanguageConverter
{
  /**
   * Contains a map for all used user locales. Each map contains the display name of a locale as key and the locale
   * object as value.
   */
  private final static Map<Locale, Map<String, Locale>> localeMap = new HashMap<Locale, Map<String, Locale>>();

  public static final String getLanguageAsString(final Locale language, final Locale locale)
  {
    if (language == null) {
      return "";
    }
    return language.getDisplayName(locale);
  }

  public static final Locale getLanguage(final String language, final Locale locale)
  {
    synchronized (localeMap) {
      if (localeMap.containsKey(locale) == false) {
        final Map<String, Locale> m = new HashMap<String, Locale>();
        for (final Locale lc : Locale.getAvailableLocales()) {
          m.put(lc.getDisplayName(locale), lc);
        }
        localeMap.put(locale, m);
      }
    }
    return localeMap.get(locale).get(language);
  }

}

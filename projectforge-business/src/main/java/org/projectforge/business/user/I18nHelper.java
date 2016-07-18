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

package org.projectforge.business.user;

import org.projectforge.web.i18n.I18NService;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * ThreadLocal context.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class I18nHelper
{
  public static final Set<String> BUNDLE_NAMES = new HashSet<>();
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(I18nHelper.class);

  public static I18NService i18NService;

  public static void addBundleName(String bundleName)
  {
    BUNDLE_NAMES.add(bundleName);
  }

  public static Set<String> getBundleNames()
  {
    return BUNDLE_NAMES;
  }

  /**
   * Use-ful for using the locale of another user (e. g. the receiver of an e-mail).
   *
   * @param locale If null, then the context user's locale is assumed.
   * @return
   */
  private static ResourceBundle getResourceBundle(final String bundleName, final Locale locale)
  {
    final ResourceBundle resourceBundle = locale != null ? ResourceBundle.getBundle(bundleName, locale) : ResourceBundle
        .getBundle(bundleName);
    return resourceBundle;
  }

  public static String getLocalizedMessage(final Locale locale, final String messageKey, final Object... params)
  {
    if (params == null) {
      return getLocalizedString(locale, messageKey);
    }
    return MessageFormat.format(getLocalizedString(locale, messageKey), params);
  }

  private static String getLocalizedString(final String bundleName, final Locale locale, final String key)
  {
    try {
      final ResourceBundle bundle = getResourceBundle(bundleName, locale);
      if (bundle.containsKey(key) == true) {
        return bundle.getString(key);
      } else {
        return i18NService.getAdditionalString(key, locale);
      }
    } catch (final Exception ex) {
      log.warn("Resource key '" + key + "' not found for locale '" + locale + "'");
    }
    return null;
  }

  public static String getLocalizedString(final Locale locale, final String key)
  {
    try {
      //      String translation = null;
      //      for(String bundleName : BUNDLE_NAMES) {
      //        translation = getLocalizedString(bundleName, locale, key);
      //        if (translation != null) {
      //          return translation;
      //        }
      //      }
      String translation = BUNDLE_NAMES
          .stream()
          .map(bundleName -> getLocalizedString(bundleName, locale, key))
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null);
      if (translation != null) {
        return translation;
      }
    } catch (final Exception ex) { // MissingResourceException or NullpointerException
      log.warn("Resource key '" + key + "' not found for locale '" + locale + "'");
    }
    return "???" + key + "???";
  }

}

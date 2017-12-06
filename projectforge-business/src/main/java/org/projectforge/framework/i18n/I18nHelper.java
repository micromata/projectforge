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

package org.projectforge.framework.i18n;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

/**
 * ThreadLocal context.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class I18nHelper
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(I18nHelper.class);

  private static final Set<String> BUNDLE_NAMES = new HashSet<>();

  private static I18nService i18nService;

  public static void addBundleName(String bundleName)
  {
    BUNDLE_NAMES.add(bundleName);
  }

  public static Set<String> getBundleNames()
  {
    return BUNDLE_NAMES;
  }

  public static I18nService getI18nService()
  {
    return i18nService;
  }

  static void setI18nService(final I18nService i18nService)
  {
    I18nHelper.i18nService = i18nService;
  }

  public static String getLocalizedMessage(final I18nKeyAndParams i18nKeyAndParams)
  {
    return getLocalizedMessage(i18nKeyAndParams.getKey(), i18nKeyAndParams.getParams());
  }

  public static String getLocalizedMessage(final String i18nKey, final Object... params)
  {
    return getLocalizedMessage(ThreadLocalUserContext.getLocale(), i18nKey, params);
  }

  public static String getLocalizedMessage(final Locale locale, final String i18nKey, final Object... params)
  {
    final String localized = getLocalizedString(locale, i18nKey);
    return (params == null || params.length == 0)
        ? localized
        : MessageFormat.format(localized, params);
  }

  private static String getLocalizedString(final Locale locale, final String i18nKey)
  {
    try {
      final Optional<String> translation = BUNDLE_NAMES.stream()
          .map(bundleName -> getLocalizedString(bundleName, locale, i18nKey))
          .filter(Objects::nonNull)
          .findFirst();

      if (translation.isPresent()) {
        return translation.get();
      }
    } catch (final Exception ex) { // MissingResourceException or NullpointerException
      log.warn("Resource key '" + i18nKey + "' not found for locale '" + locale + "'");
    }
    return "???" + i18nKey + "???";
  }

  private static String getLocalizedString(final String bundleName, final Locale locale, final String i18nKey)
  {
    try {
      final ResourceBundle bundle = getResourceBundle(bundleName, locale);
      if (bundle.containsKey(i18nKey)) {
        return bundle.getString(i18nKey);
      } else {
        return i18nService.getAdditionalString(i18nKey, locale);
      }
    } catch (final Exception ex) {
      log.warn("Resource key '" + i18nKey + "' not found for locale '" + locale + "'");
    }
    return null;
  }

  /**
   * Use-ful for using the locale of another user (e. g. the receiver of an e-mail).
   *
   * @param locale If null, then the context user's locale is assumed.
   */
  private static ResourceBundle getResourceBundle(final String bundleName, final Locale locale)
  {
    return (locale != null)
        ? ResourceBundle.getBundle(bundleName, locale)
        : ResourceBundle.getBundle(bundleName);
  }

}

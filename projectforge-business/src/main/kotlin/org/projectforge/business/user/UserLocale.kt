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

package org.projectforge.business.user

import org.projectforge.business.configuration.ConfigurationServiceAccessor
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.util.*
import javax.servlet.http.HttpServletRequest

/**
 * Helper method for getting the best fitting user locale.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */

object UserLocale {
  /**
   * Determines the user's locale as best as it could be.
   * The [PFUserDO.clientLocale] is set by this method.
   * 1. For a given user return the user locale, if configured: [PFUserDO.locale]
   * 2. For a given user the param defaultLocale, if set (update the field [PFUserDO.clientLocale].
   * 3. For a given user the clientLocale, if given: [PFUserDO.clientLocale]
   * 4. The locale set in ThreadLocal for public services without given user, if given: [ThreadLocalUserContext.getLocale]
   * 5. The locale registered via [registerLocale] (if request param is given).
   * 6. The given default locale.
   * 7. The request locale given by the browser (if request param is also given).
   * 8. The locale configured in ProjectForge config file: projectforge.defaultLocale
   * 9. The system's locale
   * @param The optional defaultLocale
   * @param request Optional request for public services. The user locale of [registerLocale] is used, if given.
   */
  @JvmStatic
  @JvmOverloads
  fun determineUserLocale(
    user: PFUserDO? = ThreadLocalUserContext.getUser(),
    defaultLocale: Locale? = null,
    request: HttpServletRequest? = null
  ): Locale {
    if (user != null) {
      // 1. For a given user return the user locale, if configured: [PFUserDO.locale]
      user.locale?.let { return it } // The locale configured in the data base for this user (MyAccount).

      // 2. For a given user the param defaultLocale, if set (update the field [PFUserDO.clientLocale].
      defaultLocale?.let {
        if (it != user.clientLocale) {
          user.clientLocale = it
        }
        return it
      }
      // 3. For a given user the clientLocale, if given: [PFUserDO.clientLocale]
      user.clientLocale?.let { return it }
    } else {
      // 4. The locale set in ThreadLocal for public services without given user, if given: [ThreadLocalUserContext.getLocale]
      ThreadLocalUserContext.internalGetThreadLocalLocale()?.let { return it }
      request?.getSession(false)?.getAttribute(SESSION_LOCALE_KEY)?.let { locale ->
        // 5. The locale registered via [registerLocale] (if request param is given).
        if (locale is Locale) {
          return locale
        }
      }
    }
    // 6. The given default locale.
    defaultLocale?.let { return it }
    // 7. The request locale given by the browser (if request param is also given).
    request?.locale?.let { return it }
    // 8. The locale configured in ProjectForge config file: projectforge.defaultLocale
    // 9. The system's locale
    return ConfigurationServiceAccessor.get().defaultLocale ?: Locale.getDefault()
  }

  fun registerLocale(request: HttpServletRequest, user: PFUserDO?) {
    user?.locale?.let { locale ->
      request.getSession(true).setAttribute(SESSION_LOCALE_KEY, locale)
    }
  }

  /**
   * @return "de" or "en".
   */
  @JvmStatic
  @JvmOverloads
  fun determineUserLocaleAsIdentifier(
    user: PFUserDO? = ThreadLocalUserContext.getUser(),
    defaultLocale: Locale? = null,
    request: HttpServletRequest? = null
  ): String {
    val locale = determineUserLocale(user, defaultLocale, request)
    return if (locale == Locale.GERMAN) {
      "de"
    } else {
      "en"
    }
  }

  // Available Loacles for external i18n-files
  @JvmField
  val I18NSERVICE_LANGUAGES = arrayOf(Locale.GERMAN, Locale.ENGLISH, Locale.ROOT)

  /**
   * Available Localization for the wicket module
   * If you add new languages don't forget to add the I18nResources_##.properties also for all used plugins.
   * You need also to add the language to I18nResources*.properties such as<br></br>
   * locale.de=German<br></br>
   * locale.en=English<br></br>
   * locale.zh=Chinese
   */
  @JvmField
  val LOCALIZATIONS = arrayOf("en", "de")

  private const val SESSION_LOCALE_KEY = "UserLocale.locale"

}

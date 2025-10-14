/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.i18n

import mu.KotlinLogging
import org.projectforge.business.user.UserLocale
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.text.MessageFormat
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * ThreadLocal context.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object I18nHelper {
  private val BUNDLE_NAMES: MutableSet<String> = LinkedHashSet()
  private lateinit var i18nService: I18nService

  @JvmStatic
  fun addBundleName(bundleName: String) {
    BUNDLE_NAMES.add(bundleName)
  }

  /**
   * Adds a bundle name with highest priority (at the beginning of the list).
   * Used for customer-specific bundles that should override all other translations.
   */
  @JvmStatic
  fun addBundleNameWithHighestPriority(bundleName: String) {
    // Create new LinkedHashSet with the new bundle first, then add all existing ones
    val newBundles = LinkedHashSet<String>()
    newBundles.add(bundleName)
    newBundles.addAll(BUNDLE_NAMES)
    BUNDLE_NAMES.clear()
    BUNDLE_NAMES.addAll(newBundles)
  }

  @JvmStatic
  val bundleNames: Set<String>
    get() = BUNDLE_NAMES

  @JvmStatic
  fun getI18nService(): I18nService {
    return i18nService
  }

  @JvmStatic
  fun setI18nService(i18nService: I18nService) {
    I18nHelper.i18nService = i18nService
  }

  @JvmStatic
  fun getLocalizedMessage(i18nKeyAndParams: I18nKeyAndParams): String {
    return getLocalizedMessage(i18nKeyAndParams.key, *i18nKeyAndParams.params)
  }

  @JvmStatic
  fun getLocalizedMessage(i18nKey: String?, vararg params: Any?): String {
    i18nKey ?: return "???"
    return getLocalizedMessage(ThreadLocalUserContext.locale, i18nKey, *params)
  }

  @JvmStatic
  fun getLocalizedMessage(user: PFUserDO?, i18nKey: String?, vararg params: Any?): String {
    return getLocalizedMessage(UserLocale.determineUserLocale(user), i18nKey, *params)
  }

  @JvmStatic
  fun getLocalizedMessage(locale: Locale?, i18nKey: String?, vararg params: Any?): String {
    i18nKey ?: return "???"
    val localized = getLocalizedString(locale, i18nKey)
    if (params.isEmpty()) {
      return localized
    }
    return if (localized.startsWith("???")) {
      // I18n-key not found (e.g. in test cases).
      "$localized (${params.joinToString { it.toString() }})???"
    } else MessageFormat.format(localized, *params)
  }

  private fun getLocalizedString(locale: Locale?, i18nKey: String): String {
    val lc = locale ?: ThreadLocalUserContext.locale!!
    log.debug { "#### I18N LOOKUP: key='$i18nKey', locale='$lc', bundles=[${BUNDLE_NAMES.joinToString()}]" }
    for (bundleName in BUNDLE_NAMES) {
      val translation = getLocalizedString(bundleName, lc, i18nKey)
      if (translation != null) {
        log.debug { "#### I18N FOUND: key='$i18nKey' in bundle='$bundleName' => '$translation'" }
        return translation
      }
    }
    log.debug { "#### I18N NOT FOUND: key='$i18nKey', returning key itself" }
    // Is already translated (or key not found):
    return i18nKey
  }

  private fun getLocalizedString(bundleName: String, locale: Locale, i18nKey: String): String? {
    try {
      val bundle = getResourceBundle(bundleName, locale)
      return if (bundle.containsKey(i18nKey)) {
        bundle.getString(i18nKey)
      } else {
        null
      }
    } catch (ex: Exception) {
      log.warn("Exception while trying to access key '$i18nKey' for locale '$locale' and bundle '$bundleName': ${ex.message}")
    }
    return null
  }

  /**
   * Use-ful for using the locale of another user (e. g. the receiver of an e-mail).
   *
   * @param locale If null, then the context user's locale is assumed.
   */
  private fun getResourceBundle(bundleName: String, locale: Locale?): ResourceBundle {
    val effectiveLocale = locale ?: Locale.getDefault()

    // Only load CustomerI18nResources from resourceDir via I18nService
    // All other bundles are loaded from classpath via standard ResourceBundle mechanism
    if (bundleName == "CustomerI18nResources") {
      log.debug { "#### I18N BUNDLE: '$bundleName' is customer bundle, trying I18nService for locale='$effectiveLocale'" }
      try {
        val bundle = i18nService.getResourceBundleFor(bundleName, effectiveLocale)
        log.debug { "#### I18N BUNDLE: Successfully loaded '$bundleName' from I18nService for locale='$effectiveLocale'" }
        return bundle
      } catch (ex: MissingResourceException) {
        log.debug { "#### I18N BUNDLE: '$bundleName' not found in I18nService: ${ex.message}" }
        throw ex
      }
    }

    // Standard classpath lookup for all built-in bundles
    log.debug { "#### I18N BUNDLE: '$bundleName' is built-in bundle, using standard classpath lookup for locale='$effectiveLocale'" }
    val bundle = ResourceBundle.getBundle(bundleName, effectiveLocale)
    log.debug { "#### I18N BUNDLE: Loaded '$bundleName' from classpath for locale='$effectiveLocale', actual locale='${bundle.locale}'" }
    return bundle
  }
}

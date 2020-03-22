/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.slf4j.LoggerFactory
import java.text.MessageFormat
import java.util.*

/**
 * ThreadLocal context.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object I18nHelper {
    private val log = LoggerFactory.getLogger(I18nHelper::class.java)
    private val BUNDLE_NAMES: MutableSet<String> = HashSet()
    private var i18nService: I18nService? = null
    @JvmStatic
    fun addBundleName(bundleName: String) {
        BUNDLE_NAMES.add(bundleName)
    }

    @JvmStatic
    val bundleNames: Set<String>
        get() = BUNDLE_NAMES

    @JvmStatic
    fun getI18nService(): I18nService? {
        return i18nService
    }

    @JvmStatic
    fun setI18nService(i18nService: I18nService?) {
        I18nHelper.i18nService = i18nService
    }

    @JvmStatic
    fun getLocalizedMessage(i18nKeyAndParams: I18nKeyAndParams): String {
        return getLocalizedMessage(i18nKeyAndParams.key, *i18nKeyAndParams.params)
    }

    @JvmStatic
    fun getLocalizedMessage(i18nKey: String?, vararg params: Any?): String {
        i18nKey ?: return "???"
        return getLocalizedMessage(ThreadLocalUserContext.getLocale(), i18nKey, *params)
    }

    @JvmStatic
    fun getLocalizedMessage(locale: Locale, i18nKey: String?, vararg params: Any?): String {
        i18nKey ?: return "???"
        val localized = getLocalizedString(locale, i18nKey)
        if (params.isNullOrEmpty()) {
            return localized
        }
        return if (localized.startsWith("???")) {
            // I18n-key not found (e. g. in test cases).
            "$localized (${params.joinToString { it.toString() }})???"
        } else MessageFormat.format(localized, *params)
    }

    private fun getLocalizedString(locale: Locale, i18nKey: String): String {
        try {
            val translation = BUNDLE_NAMES.stream()
                    .map { bundleName: String -> getLocalizedString(bundleName, locale, i18nKey) }
                    .filter { obj: String? -> Objects.nonNull(obj) }
                    .findFirst()
            if (translation.isPresent) {
                return translation.get()
            }
        } catch (ex: Exception) { // MissingResourceException or NullpointerException
            log.warn("Resource key '$i18nKey' not found for locale '$locale'")
        }
        return "???$i18nKey???"
    }

    private fun getLocalizedString(bundleName: String, locale: Locale, i18nKey: String): String? {
        try {
            val bundle = getResourceBundle(bundleName, locale)
            return if (bundle.containsKey(i18nKey)) {
                bundle.getString(i18nKey)
            } else {
                i18nService!!.getAdditionalString(i18nKey, locale)
            }
        } catch (ex: Exception) {
            log.warn("Resource key '$i18nKey' not found for locale '$locale'")
        }
        return null
    }

    /**
     * Use-ful for using the locale of another user (e. g. the receiver of an e-mail).
     *
     * @param locale If null, then the context user's locale is assumed.
     */
    private fun getResourceBundle(bundleName: String, locale: Locale?): ResourceBundle {
        return if (locale != null) ResourceBundle.getBundle(bundleName, locale) else ResourceBundle.getBundle(bundleName)
    }
}

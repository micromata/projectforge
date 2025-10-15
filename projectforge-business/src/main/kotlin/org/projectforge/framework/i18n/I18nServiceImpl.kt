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

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.user.UserLocale
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class I18nServiceImpl : I18nService {
    @Autowired
    private lateinit var configurationService: ConfigurationService

    private val resourceBundles = mutableSetOf<String>()

    private var localeResourceBundleMap: MutableMap<Pair<Locale, String>, ResourceBundle>? = null

    @PostConstruct
    fun init() {
        I18nHelper.setI18nService(this)
        // Auto-discover CustomerI18nResources if present (highest priority)
        registerCustomerBundleIfPresent()
        loadResourceBundles()
    }

    /**
     * Auto-discovers and registers customer-specific i18n bundle if present in resourceDir.
     * Convention: CustomerI18nResources*.properties files in resourceDir will be loaded
     * with highest priority, allowing customers to override any translation without code changes.
     */
    private fun registerCustomerBundleIfPresent() {
        val customerBundleName = "CustomerI18nResources"
        val resourceDir = File(configurationService.resourceDirName)

        if (!resourceDir.exists() || !resourceDir.isDirectory) {
            return
        }

        // Check if any CustomerI18nResources*.properties exists
        val hasCustomerBundle = resourceDir.listFiles()?.any { file ->
            val found = file.isFile && (file.name == "$customerBundleName.properties" ||file.name.matches("""$customerBundleName(_.*)?\.properties""".toRegex()))
            if (found) {
                log.info { "Detected customer i18n bundle file: ${file.name}" }
            }
            found
        } ?: false

        if (hasCustomerBundle) {
            // Register as FIRST bundle (highest priority)
            I18nHelper.addBundleNameWithHighestPriority(customerBundleName)
            log.info { "Customer i18n bundle detected and registered with highest priority: $customerBundleName" }
        } else {
            log.info { "No customer i18n bundle found in resourceDir '${resourceDir.absolutePath}' (OK): $customerBundleName{_de|_en}.properties" }
        }
    }

    override fun loadResourceBundles() {
        if (localeResourceBundleMap == null) {
            localeResourceBundleMap = mutableMapOf()
        }
        val file = File(configurationService.resourceDirName)
        var urls = arrayOfNulls<URL>(0)
        try {
            urls = arrayOf(file.toURI().toURL())
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }

        for (iterFile in file.listFiles()) {
            if (iterFile.isFile && iterFile.name.matches(".*i18n(_.*)?.properties".toRegex())) {
                val strings = iterFile.name.split("(_.*)?.prop".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                resourceBundles.add(strings[strings.size - 2])
            }
        }

        val loader: ClassLoader = URLClassLoader(urls)
        // Merge auto-discovered bundles and manually registered bundles (like CustomerI18nResources)
        val allBundleNames = (resourceBundles + I18nHelper.bundleNames).toSet()
        for (locale in UserLocale.I18NSERVICE_LANGUAGES) {
            for (bundleName in allBundleNames) {
                try {
                    if (File(configurationService.resourceDirName + File.separator + bundleName + "_" + locale.toString() + ".properties").exists()) {
                        localeResourceBundleMap!![Pair(locale, bundleName)] =
                            ResourceBundle.getBundle(bundleName, locale, loader)
                    } else {
                        val defaultBundle: ResourceBundle = ResourceBundle.getBundle(
                            bundleName, Locale.ROOT,
                            URLClassLoader(arrayOf<URL>(File(configurationService.resourceDirName).toURI().toURL()))
                        )
                        localeResourceBundleMap!![Pair(locale, bundleName)] = defaultBundle
                    }
                } catch (ignored: MissingResourceException) {
                } catch (e: MalformedURLException) {
                    throw RuntimeException(e)
                }
            }
        }
    }

    override fun getLocalizedStringForKey(i18nKey: String, locale: Locale): String {
        val resourceBundle = getResourceBundleFor("I18NResources", locale)
        return if (resourceBundle != null) {
            resourceBundle.getObject(i18nKey) as String
        } else {
            throw RuntimeException("No ResourceBundle for given locale found$locale")
        }
    }

    override fun getAdditionalString(key: String, locale: Locale): String? {
        return getValueFromBundles(key, locale)
    }

    private fun getValueFromBundles(key: String, locale: Locale): String? {
        // Iterate in priority order: I18nHelper.bundleNames first (includes CustomerI18nResources with highest priority),
        // then auto-discovered bundles
        val allBundleNames = (I18nHelper.bundleNames + resourceBundles).toList()
        for (bundleName in allBundleNames) {
            // Try to get the resource bundle for this bundle name with proper locale fallback
            val resourceBundle = try {
                getResourceBundleFor(bundleName, locale)
            } catch (e: MissingResourceException) {
                // Bundle not found for this locale, try next bundle
                continue
            }

            // Try to get the key from this bundle
            try {
                return resourceBundle.getObject(key) as String
            } catch (ignored: MissingResourceException) {
                // Key not found in this bundle, try next bundle
            }
        }
        return null
    }

    override fun getResourceBundleFor(name: String, locale: Locale): ResourceBundle {
        log.debug { "#### I18N SERVICE: getResourceBundleFor(name='$name', locale='$locale')" }

        // Try exact locale match first (e.g., de_DE)
        localeResourceBundleMap!![Pair(locale, name)]?.let {
            log.debug { "#### I18N SERVICE: Found exact match for ($locale, $name)" }
            return it
        }

        // Fall back to language-only locale (e.g., de from de_DE)
        if (!locale.country.isNullOrBlank() || !locale.variant.isNullOrBlank()) {
            val languageOnlyLocale = Locale(locale.language)
            log.debug { "#### I18N SERVICE: Trying language-only fallback: $languageOnlyLocale" }
            localeResourceBundleMap!![Pair(languageOnlyLocale, name)]?.let {
                log.debug { "#### I18N SERVICE: Found language-only match for ($languageOnlyLocale, $name)" }
                return it
            }
        }

        // Fall back to ROOT locale
        log.debug { "#### I18N SERVICE: Trying ROOT locale fallback" }
        localeResourceBundleMap!![Pair(Locale.ROOT, name)]?.let {
            log.debug { "#### I18N SERVICE: Found ROOT match for (ROOT, $name)" }
            return it
        }

        // No bundle found
        log.debug { "#### I18N SERVICE: No bundle found for name='$name', locale='$locale'. Available keys: ${localeResourceBundleMap!!.keys.filter { it.second == name }}" }
        throw MissingResourceException(
            "No ResourceBundle found for name='$name', locale='$locale'",
            name,
            ""
        )
    }
}

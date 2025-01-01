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
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.user.UserLocale
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.util.*

@Service
class I18nServiceImpl : I18nService {
    @Autowired
    private lateinit var configurationService: ConfigurationService

    private val resourceBundles = mutableSetOf<String>()

    private var localeResourceBundleMap: MutableMap<Pair<Locale, String>, ResourceBundle>? = null

    @PostConstruct
    fun init() {
        I18nHelper.setI18nService(this)
        loadResourceBundles()
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
        for (locale in UserLocale.I18NSERVICE_LANGUAGES) {
            for (bundleName in resourceBundles) {
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
        if (resourceBundle != null) {
            return resourceBundle.getObject(i18nKey) as String
        } else {
            throw RuntimeException("No ResourceBundle for given locale found$locale")
        }
    }

    override fun getAdditionalString(key: String, locale: Locale): String? {
        return getValueFromBundles(key, locale)
    }

    private fun getValueFromBundles(key: String, locale: Locale): String? {
        for (resourceBundle in resourceBundles) {
            // the pair searched for
            var localeStringPair = Pair(locale, resourceBundle)
            if (!localeResourceBundleMap!!.keys.contains(localeStringPair)) {
                for (iterationPair in localeResourceBundleMap!!.keys) {
                    if (!iterationPair.first.equals(Locale.ROOT) && locale.toString()
                            .startsWith(iterationPair.second.toString())
                    ) {
                        // replace searched for with nearest candidate e.g. for de_de use de
                        localeStringPair = iterationPair
                        break
                    }
                }
                // if no candidate was found use default
                if (Pair(locale, resourceBundle).equals(localeStringPair)) {
                    localeStringPair = Pair(Locale(Locale.getDefault().language), resourceBundle)
                }
            }

            val resourceBundleFile = localeResourceBundleMap!![localeStringPair]
            try {
                return resourceBundleFile!!.getObject(key) as String
            } catch (ignored: MissingResourceException) {
                // not found
            }
        }
        return null
    }

    override fun getResourceBundleFor(name: String, locale: Locale): ResourceBundle {
        return localeResourceBundleMap!![Pair(locale, name)]!!
    }
}

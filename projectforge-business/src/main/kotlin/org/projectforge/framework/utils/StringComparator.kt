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

package org.projectforge.framework.utils

import org.projectforge.business.configuration.ConfigurationServiceAccessor
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.text.Collator
import java.util.*

/**
 * Uses [Collator] and the user's locale to compare string.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object StringComparator {
    private val germanCollator: Collator

    private var _defaultCollator: Collator? = null
    private val defaultCollator: Collator
        get() {
            // Late init, because Configuration must already available.
            if (_defaultCollator == null) {
                var locale: Locale? = ConfigurationServiceAccessor.get().defaultLocale
                if (locale == null) {
                    locale = Locale.getDefault()
                }
                _defaultCollator = Collator.getInstance(locale)
            }
            return _defaultCollator!!
        }

    private val german = Locale("de")

    init {
        germanCollator = Collator.getInstance(Locale.GERMAN);
        germanCollator.setStrength(Collator.SECONDARY);// a == A, a < Ä
    }

    /**
     * Using ascending order.
     * @param s1
     * @param s2
     * @param locale
     * @see .compare
     */
    @JvmStatic
    fun compare(s1: String, s2: String, locale: Locale): Int {
        return compare(s1, s2, true, locale)
    }

    /**
     * Gets the Collator for the given locale and uses this collator for the string comparison.
     * @param s1
     * @param s2
     * @param asc
     * @param locale
     * @return The result of [Collator.compare] or -1, 0, 1 if one or both string paramters are null.
     */
    @JvmStatic
    @JvmOverloads
    fun compare(s1: String?, s2: String?, asc: Boolean = true, locale: Locale = ThreadLocalUserContext.getLocale()): Int {
        if (s1 == null) {
            return if (s2 == null)
                0
            else
                if (asc) -1 else 1
        }
        if (s2 == null) {
            return if (asc) 1 else -1
        }
        val collator = getCollator(locale)
        return if (asc) {
            collator.compare(s1, s2)
        } else -collator.compare(s1, s2)
    }

    private fun getCollator(locale: Locale?): Collator {
        if (locale != null) {
            if (german.language == locale.language) {
                return germanCollator
            }
        }
        return defaultCollator
    }
}
/**
 * Uses [ThreadLocalUserContext.getLocale] as locale in ascending order.
 * @param s1
 * @param s2
 * @return The result of [Collator.compare].
 * @see .compare
 */
/**
 * Uses [ThreadLocalUserContext.getLocale] as locale.
 * @param s1
 * @param s2
 * @param asc
 * @return The result of [Collator.compare].
 * @see .compare
 */

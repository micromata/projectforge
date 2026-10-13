/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest

import org.projectforge.business.user.UserLocale
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.TimeNotation
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The choices of a user's personal settings (locale, date formats, time notation), shared by the user edit
 * page (React, [UserPagesRest.createUserSettingsCol]) and the my-account page of projectforge-next
 * ([MyAccountRest]), so both offer the same lists.
 */
object UserSettingsOptions {
    class Option(val value: String, val label: String)

    /**
     * The value of the locale option "default": no locale is stored, the browser's one is used (see
     * [UserLocale.determineUserLocale]).
     */
    const val DEFAULT_LOCALE = "DEFAULT"

    /** The locales, the first one being "default" ([DEFAULT_LOCALE]). */
    fun locales(): List<Option> {
        return listOf(Option(DEFAULT_LOCALE, translate("user.defaultLocale"))) +
                UserLocale.LOCALIZATIONS.map { Option(it, translate("locale.$it")) }
    }

    /** The date formats of the configuration, each labelled with today in that format. */
    fun dateFormats(): List<Option> {
        val today = LocalDate.now()
        return Configuration.instance.dateFormats.map { createOption(it, today) }
    }

    /** The same patterns as [dateFormats] in Excel's notation (`YYYY`, `DD`). */
    fun excelDateFormats(): List<Option> {
        val today = LocalDate.now()
        return Configuration.instance.dateFormats.map { createOption(it, today, excelDateFormat = true) }
    }

    fun timeNotations(): List<Option> {
        return listOf(
            Option(TimeNotation.H12.name, translate("timeNotation.12")),
            Option(TimeNotation.H24.name, translate("timeNotation.24")),
        )
    }

    private fun createOption(pattern: String, today: LocalDate, excelDateFormat: Boolean = false): Option {
        val str = if (excelDateFormat) {
            pattern.replace('y', 'Y').replace('d', 'D')
        } else {
            pattern
        }
        return Option(str, "$str: ${DateTimeFormatter.ofPattern(pattern).format(today)}")
    }
}

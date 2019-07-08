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

package org.projectforge.rest.core

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.stereotype.Component
import java.util.*

/**
 * Language services.
 */
@Component
class LanguageService {
    class Language(var lang: String, var displayName: String) {
        constructor(locale: Locale) : this(locale.toString(), locale.getDisplayName(ThreadLocalUserContext.getLocale()))
    }

    fun getAllLanguages(): List<Language> {
        return getLanguages(Locale.getAvailableLocales().asIterable())
    }

    fun getLanguages(searchString: String?): List<Language> {
        if (searchString.isNullOrBlank()) return getAllLanguages()
        return getAllLanguages().filter { it.displayName.contains(searchString, true) }
    }

    fun getLanguages(locales: Iterable<Locale>): List<Language> {
        val locale = ThreadLocalUserContext.getLocale()
        val languages = locales.map { Language(it.toString(), it.getDisplayName(locale)) }
        languages.sortedBy { it.displayName }
        return languages
    }
}

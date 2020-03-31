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

package org.projectforge.rest.core

import mu.KotlinLogging
import org.projectforge.rest.config.Rest
import org.springframework.web.bind.annotation.RequestMapping

private val log = KotlinLogging.logger {}

/**
 * Helper for getting url of rest calls.
 */
object RestResolver {
    const val REACT_PATH = "react"

    fun getRestUrl(pagesRestClass: Class<*>, subPath: String? = null, withoutPrefix: Boolean = false): String {
        return getUrl(pagesRestClass, Rest.URL, subPath, withoutPrefix)
    }

    fun getPublicRestUrl(pagesRestClass: Class<*>, subPath: String? = null, withoutPrefix: Boolean = false): String {
        return getUrl(pagesRestClass, Rest.PUBLIC_URL, subPath, withoutPrefix)
    }

    private fun getUrl(pagesRestClass: Class<*>, path: String? = null, subPath: String? = null, withoutPrefix: Boolean = false): String {
        val requestMapping = pagesRestClass.annotations.find { it is RequestMapping } as? RequestMapping
        var url = requestMapping?.value?.joinToString("/") { it } ?: "/"
        if (withoutPrefix && url.startsWith("$path/")) {
            url = url.substringAfter("$path/")
        }
        if (subPath.isNullOrBlank()) {
            return url
        }
        if (subPath.startsWith('/') || url.endsWith('/')) {
            return "${url}$subPath"
        }
        return "${url}/$subPath"
    }
}

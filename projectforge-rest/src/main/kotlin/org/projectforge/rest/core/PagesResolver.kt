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

import org.projectforge.rest.config.Rest
import org.springframework.web.bind.annotation.RequestMapping
import java.net.URLEncoder

/**
 * Helper for getting url of list and edit pages.
 */
object PagesResolver {
    const val REACT_PATH = "react"

    private val log = org.slf4j.LoggerFactory.getLogger(PagesResolver::class.java)

    fun getEditPageUrl(pagesRestClass: Class<out AbstractPagesRest<*, *, *>>, id: Int? = null, params: Map<String, Any?>? = null): String {
        val path = getRequestMappingPath(pagesRestClass) ?: return "NOT_FOUND"
        val parameters = if (id == null) {
            params
        } else if (params.isNullOrEmpty()) {
            mapOf("id" to "$id")
        } else {
            val map = mutableMapOf<String, Any?>("id" to "$id")
            map.putAll(params)
            map
        }
        if (path.startsWith('/')) {
            return "$path/edit${getQueryString(parameters)}"
        }
        return "$path/edit${getQueryString(parameters)}"
    }


    /**
     * @return Path of react page.
     */
    fun getBasePageUrl(pagesRestClass: Class<out AbstractPagesRest<*, *, *>>, subPath: String? = null, params: Map<String, Any?>? = null): String {
        val path = getRequestMappingPath(pagesRestClass) ?: return "NOT_FOUND"
        val subPathString = if (subPath != null) "/subPath" else ""
        return "$path$subPathString${getQueryString(params)}"
    }

    /**
     * @return Path of react page.
     */
    fun getListPageUrl(pagesRestClass: Class<out AbstractPagesRest<*, *, *>>, params: Map<String, Any?>? = null): String {
        return getBasePageUrl(pagesRestClass, null, params)
    }

    /**
     * @return Path of react page.
     */
    fun getDynamicPageUrl(pageRestClass: Class<*>, params: Map<String, Any?>? = null): String {
        val path = getRequestMappingPath(pageRestClass, "/dynamic") ?: return "NOT_FOUND"
        return "$path${getQueryString(params)}"
    }

    private fun getRequestMappingPath(clazz: Class<*>, prefix: String = ""): String? {
        val requestMapping = clazz.annotations.find { it is RequestMapping } as? RequestMapping
        if (requestMapping == null) {
            log.error("RequestMapping annotation not found in class '$clazz'.")
            return null
        }
        val subpath = requestMapping.value[0].removePrefix("${Rest.URL}")
        return "/$REACT_PATH$prefix$subpath"
    }

    private fun getQueryString(params: Map<String, Any?>? = null): String {
        if (params.isNullOrEmpty()) {
            return ""
        }
        val sb = StringBuilder()
        var first = true
        params.forEach {
            if (first) {
                sb.append("?")
                first = false
            } else {
                sb.append("&")
            }
            sb.append(it.key).append("=").append(URLEncoder.encode("${it.value}", "UTF-8"))
        }
        return sb.toString()
    }
}

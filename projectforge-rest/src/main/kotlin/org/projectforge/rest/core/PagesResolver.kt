/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.Const
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.rest.config.Rest
import org.springframework.web.bind.annotation.RequestMapping
import java.net.URLEncoder

/**
 * Helper for getting url of list and edit pages.
 */
object PagesResolver {
  const val REACT_PATH = "react"

  private val log = org.slf4j.LoggerFactory.getLogger(PagesResolver::class.java)

  private val pagesRegistry = mutableMapOf<String, AbstractPagesRest<*, *, *>>()

  fun getEditPageUrl(
    pagesRestClass: Class<out AbstractPagesRest<*, *, *>>,
    id: Int? = null,
    params: Map<String, Any?>? = null,
    absolute: Boolean = false
  ): String {
    val path = getRequestMappingPath(pagesRestClass) ?: return "NOT_FOUND"
    val prefix = if (absolute) "/" else ""
    val idString = if (id != null) {
      "/$id"
    } else {
      ""
    }
    if (path.startsWith('/')) {
      return "$path/edit$idString${getQueryString(params)}"
    }
    return "$prefix$path/edit$idString${getQueryString(params)}"
  }


  /**
   * @return Path of react page.
   */
  fun getBasePageUrl(
    pagesRestClass: Class<out AbstractPagesRest<*, *, *>>,
    subPath: String? = null,
    params: Map<String, Any?>? = null,
    absolute: Boolean = false
  ): String {
    val path = getRequestMappingPath(pagesRestClass) ?: return "NOT_FOUND"
    val subPathString = if (subPath != null) "/$subPath" else ""
    val prefix = if (absolute) "/" else ""
    return "$prefix$path$subPathString${getQueryString(params)}"
  }

  /**
   * @return Path of react page.
   */
  fun getListPageUrl(
    pagesRestClass: Class<out AbstractPagesRest<*, *, *>>,
    params: Map<String, Any?>? = null,
    absolute: Boolean = false
  ): String {
    return getBasePageUrl(pagesRestClass, null, params, absolute)
  }

  /**
   * @return Path of react page.
   */
  @JvmStatic
  @JvmOverloads
  fun getDynamicPageUrl(
    pageRestClass: Class<*>,
    params: Map<String, Any?>? = null,
    id: Int? = null,
    absolute: Boolean = false
  ): String {
    val path = getRequestMappingPath(pageRestClass, "/dynamic") ?: return "NOT_FOUND"
    val prefix = if (absolute) "/" else ""
    val idPart = if (id != null) "/$id" else ""
    return "$prefix$path$idPart${getQueryString(params)}"
  }

  /**
   * @return the default url (calendar url).
   */
  fun getDefaultUrl(): String {
    return "/${Const.REACT_APP_PATH}calendar"
  }

  fun register(category: String, pagesRest: AbstractPagesRest<*, *, *>) {
    if (pagesRegistry.containsKey(category)) {
      throw IllegalArgumentException("Category name '$category' is already registered. Can't register ${pagesRest::class.java.name}.")
    }
    pagesRegistry[category] = pagesRest
  }

  fun getPagesRest(category: String): AbstractPagesRest<out ExtendedBaseDO<Int>, *, out BaseDao<*>>? {
    return pagesRegistry[category]
  }

  private fun getRequestMappingPath(clazz: Class<*>, suffix: String = ""): String? {
    val requestMapping = clazz.annotations.find { it is RequestMapping } as? RequestMapping
    if (requestMapping == null) {
      log.error("RequestMapping annotation not found in class '$clazz'.")
      return null
    }
    val path = requestMapping.value[0]
    if (path.startsWith(Rest.PUBLIC_URL)) {
      val subpath = requestMapping.value[0].removePrefix(Rest.PUBLIC_URL)
      return "${REACT_PATH}/public$subpath$suffix"
    } else {
      val subpath = requestMapping.value[0].removePrefix(Rest.URL)
      return "$REACT_PATH$subpath$suffix"
    }
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

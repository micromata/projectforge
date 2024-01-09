/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.SystemStatus
import org.projectforge.rest.config.Rest
import org.springframework.web.bind.annotation.*
import java.net.URLEncoder
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

private val log = KotlinLogging.logger {}

/**
 * Helper for getting url of rest calls.
 */
object RestResolver {
  const val REACT_PATH = "react"

  const val REACT_PUBLIC_PATH = "$REACT_PATH/public"

  /**
   * Uses class annotation [RequestMapping] to determine rest url of given class.
   */
  @JvmStatic
  @JvmOverloads
  fun getRestUrl(
    restClass: Class<*>, subPath: String? = null, withoutPrefix: Boolean = false, params: Map<String, Any?>? = null,
  ): String {
    return getUrl(restClass, Rest.URL, subPath, withoutPrefix, params)
  }

  fun getRestUrl(
    restClass: KClass<*>, subPath: String? = null, withoutPrefix: Boolean = false, params: Map<String, Any?>? = null,
  ): String {
    return getUrl(restClass.java, Rest.URL, subPath, withoutPrefix, params)
  }

  /**
   * @param restClass needed, otherwise for derived classes such as AdminLogViewerPagesRest the declaring class is LogViewerPagesRest.
   */
  fun getRestMethodUrl(
    restClass: Class<*>,
    method: KFunction<*>,
    withoutPrefix: Boolean = false,
    params: Map<String, Any?>? = null,
  ): String {
    try {
      val annotation = method.annotations.single {
        it.annotationClass == PostMapping::class
            || it.annotationClass == GetMapping::class
            || it.annotationClass == PutMapping::class
            || it.annotationClass == DeleteMapping::class
            || it.annotationClass == PatchMapping::class
      }

      @Suppress("UNCHECKED_CAST")
      val methodPath =
        annotation.annotationClass.members.single { it.name == "value" }.call(annotation) as Array<String>
      return getUrl(restClass, Rest.URL, methodPath[0], withoutPrefix, params)
    } catch (ex: Exception) {
      if (SystemStatus.isDevelopmentMode()) {
        throw ex
      } else {
        log.error(ex.message, ex)
        return getRestUrl(method.javaClass, method.name, withoutPrefix, params)
      }
    }
  }

  /**
   * @param restClass needed, otherwise for derived classes such as AdminLogViewerPagesRest the declaring class is LogViewerPagesRest.
   */
  fun getRestMethodUrl(
    method: KFunction<*>,
    withoutPrefix: Boolean = false,
    params: Map<String, Any?>? = null,
  ): String {
    val restClass = method.javaMethod!!.declaringClass
    return getRestMethodUrl(restClass, method, withoutPrefix, params)
  }

  /**
   * Uses class annotation [RequestMapping] to determine rest url of given class.
   */
  @JvmStatic
  @JvmOverloads
  fun getPublicRestUrl(
    restClass: Class<*>, subPath: String? = null, withoutPrefix: Boolean = false, params: Map<String, Any?>? = null,
  ): String {
    return getUrl(restClass, Rest.PUBLIC_URL, subPath, withoutPrefix, params)
  }

  private fun getUrl(
    restClass: Class<*>,
    path: String? = null,
    subPath: String? = null,
    withoutPrefix: Boolean = false,
    params: Map<String, Any?>? = null,
  ): String {
    val requestMapping = restClass.annotations.find { it is RequestMapping } as? RequestMapping
    if (requestMapping == null) {
      if (SystemStatus.isDevelopmentMode()) {
        throw Exception("Can't find @RequestMapping for class ${restClass.name}.")
      }
      log.error { "Can't find @RequestMapping for class ${restClass.name}." }
    }
    var url = requestMapping?.value?.joinToString("/") { it } ?: "/"
    if (withoutPrefix && url.startsWith("$path/")) {
      url = url.substringAfter("$path/")
    }
    val queryString = getQueryString(params)
    if (subPath.isNullOrBlank()) {
      return "$url$queryString"
    }
    if (subPath.startsWith('/') || url.endsWith('/')) {
      return "${url}$subPath$queryString"
    }
    return "${url}/$subPath$queryString"
  }

  fun getQueryString(params: Map<String, Any?>? = null): String {
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

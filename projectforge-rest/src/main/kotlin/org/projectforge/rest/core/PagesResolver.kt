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
import org.projectforge.Constants
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractPagesRest.Companion.USER_PREF_PARAM_HIGHLIGHT_ROW
import org.projectforge.rest.multiselect.MultiSelectionSupport
import org.projectforge.ui.ResponseAction
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping

private val log = KotlinLogging.logger {}

/**
 * Helper for getting url of list and edit pages.
 */
object PagesResolver {
    const val REACT_PATH = "react"

    private val pagesRegistry = mutableMapOf<String, AbstractPagesRest<*, *, *>>()

    @JvmStatic
    fun getEditPageUrl(
        pagesRestClass: Class<out AbstractPagesRest<*, *, *>>,
        id: Long? = null,
        params: Map<String, Any?>? = null,
        absolute: Boolean = false,
        returnToCaller: String? = null,
    ): String {
        val path = getRequestMappingPath(pagesRestClass) ?: return "NOT_FOUND"
        val prefix = if (absolute) "/" else ""
        val idString = if (id != null) {
            "/$id"
        } else {
            ""
        }
        if (path.startsWith('/')) {
            return "$path/edit$idString${getQueryString(params, returnToCaller)}"
        }
        return "$prefix$path/edit$idString${getQueryString(params, returnToCaller)}"
    }


    /**
     * @return Path of react page.
     */
    fun getBasePageUrl(
        pagesRestClass: Class<*>,
        subPath: String? = null,
        params: Map<String, Any?>? = null,
        absolute: Boolean = false,
        returnToCaller: String? = null,
    ): String {
        val path = getRequestMappingPath(pagesRestClass) ?: return "NOT_FOUND"
        val subPathString = if (subPath != null) "/$subPath" else ""
        val prefix = if (absolute) "/" else ""
        return "$prefix$path$subPathString${getQueryString(params, returnToCaller)}"
    }

    /**
     * @return Path of react page.
     */
    @JvmStatic
    @JvmOverloads
    fun getListPageUrl(
        pagesRestClass: Class<out AbstractPagesRest<*, *, *>>,
        params: Map<String, Any?>? = null,
        absolute: Boolean = false,
        forceAGGridReload: Boolean = false,
    ): String {
        var useParams = params
        if (forceAGGridReload) {
            useParams = useParams?.toMutableMap() ?: mutableMapOf()
            (useParams as MutableMap)["hash"] = NumberHelper.getSecureRandomAlphanumeric(4)
        }
        val result = getBasePageUrl(pagesRestClass, null, useParams, absolute)
        return result
    }

    /**
     * @return Path of react page.
     */
    @JvmStatic
    @JvmOverloads
    fun getMultiSelectionPageUrl(
        multiSelectedPageClass: Class<out AbstractPagesRest<*, *, *>>,
        absolute: Boolean = false
    ): String {
        return getBasePageUrl(multiSelectedPageClass, null, MultiSelectionSupport.getMultiSelectionParamMap(), absolute)
    }

    /**
     * @param trailingSlash Only effect if id is null: return .../dynamic/ (default) or .../dynamic if param is false.
     * @return Path of react page.
     */
    @JvmStatic
    @JvmOverloads
    fun getDynamicPageUrl(
        pageRestClass: Class<*>,
        params: Map<String, Any?>? = null,
        id: Any? = null,
        absolute: Boolean = false,
        trailingSlash: Boolean = true,
        returnToCaller: String? = null,
    ): String {
        val path = getRequestMappingPath(pageRestClass, "/dynamic") ?: return "NOT_FOUND"
        val prefix = if (absolute) "/" else ""
        val idPart = if (id != null) "/$id" else if (trailingSlash) "/" else ""
        return "$prefix$path$idPart${getQueryString(params, returnToCaller)}"
    }

    /**
     * @return the default url (calendar url).
     */
    fun getDefaultUrl(): String {
        return "/${Constants.REACT_APP_PATH}calendar"
    }

    fun register(category: String, pagesRest: AbstractPagesRest<*, *, *>) {
        if (pagesRegistry.containsKey(category)) {
            throw IllegalArgumentException("Category name '$category' is already registered. Can't register ${pagesRest::class.java.name}.")
        }
        pagesRegistry[category] = pagesRest
    }

    fun getPagesRest(category: String): AbstractPagesRest<out ExtendedBaseDO<Long>, *, out BaseDao<*>>? {
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

    private fun getQueryString(params: Map<String, Any?>? = null, returnToCaller: String? = null): String {
        if (returnToCaller != null) {
            val queryParams = mutableMapOf<String, Any?>("returnToCaller" to returnToCaller)
            if (params != null) {
                queryParams.putAll(params)
            }
            return RestResolver.getQueryString(queryParams)
        }
        return RestResolver.getQueryString(params)
    }
}

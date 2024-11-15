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

package org.projectforge.rest.multiselect

import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.core.AbstractPagesRest
import org.projectforge.rest.core.ExpiringSessionAttributes
import java.io.Serializable
import jakarta.servlet.http.HttpServletRequest

/**
 * Supports multi selection and updates of list pages.
 */
object MultiSelectionSupport {
  /**
   * @param identifierClazz The name of the class is used as identifier.
   * @param callerUrl The caller url is used for redirect back to the caller after multi selection. This is used, if
   * different sources exist (classical Wicket list page and React list page). It will be stored in the user's session.
   */
  @JvmStatic
  @JvmOverloads
  fun registerEntitiesForSelection(
    request: HttpServletRequest,
    identifierClazz: Class<out Any>,
    entityCollection: Collection<IdObject<*>>,
    callerUrl: String? = null,
    data: Any? = null,
    paginationPageSize: Int? = null,
  ) {
    registerEntitiesForSelection(request, identifierClazz.name, entityCollection, callerUrl, data, paginationPageSize)
  }

  /**
   * @param identifier for registering the entities. Identifier is also needed to get the entities in a later request.
   * @param callerUrl The caller url is used for redirect back to the caller after multi selection. This is used, if
   * different sources exist (classical Wicket list page and React list page). It will be stored in the user's session.
   */
  @JvmStatic
  @JvmOverloads
  fun registerEntitiesForSelection(
    request: HttpServletRequest,
    identifier: String,
    entityCollection: Collection<IdObject<*>>,
    callerUrl: String? = null,
    data: Any? = null,
    paginationPageSize: Int? = null,
  ) {
    val idList = entityCollection.map { it.id }
    registerEntityIdsForSelection(request, identifier, idList, callerUrl, data, paginationPageSize)
  }

  /**
   * @param callerUrl The caller url is used for redirect back to the caller after multi selection. This is used, if
   * different sources exist (classical Wicket list page and React list page). It will be stored in the user's session.
   */
  @JvmStatic
  @JvmOverloads
  fun registerEntityIdsForSelection(
    request: HttpServletRequest,
    identifierClazz: Class<out Any>,
    idList: Collection<*>,
    callerUrl: String? = null,
    data: Any? = null,
    paginationPageSize: Int? = null,
  ) {
    registerEntityIdsForSelection(
      request,
      identifier = identifierClazz.name,
      idList = idList,
      callerUrl = callerUrl,
      data = data,
      paginationPageSize = paginationPageSize,
    )
  }

  /**
   * @param callerUrl The caller url is used for redirect back to the caller after multi selection. This is used, if
   * different sources exist (classical Wicket list page and React list page). It will be stored in the user's session.
   */
  @JvmStatic
  @JvmOverloads
  fun registerEntityIdsForSelection(
    request: HttpServletRequest,
    identifier: String,
    idList: Collection<*>,
    callerUrl: String? = null,
    data: Any? = null,
    paginationPageSize: Int? = null,
  ) {
    setSessionContext(
      request,
      identifier,
      SessionContext(
        idList,
        callerUrl = callerUrl,
        data = data,
        paginationPageSize = paginationPageSize
      ) // Clear session
    )
  }

  /**
   * @return Caller url, if registered.
   */
  fun clear(request: HttpServletRequest, pagesRest: AbstractPagesRest<*, *, *>): String? {
    val identifier = pagesRest::class.java.name
    pagesRest.getCurrentFilter().multiSelection = false // multi selection mode is also stored in magic filter.
    val callerUrl = getRegisteredCallerUrl(request, identifier)
    ExpiringSessionAttributes.removeAttribute(request, "$SESSSION_ATTRIBUTE_CONTEXT:$identifier")
    return callerUrl
  }

  fun getRegisteredEntityIds(request: HttpServletRequest, identifierClazz: Class<out Any>): Collection<Serializable>? {
    return getRegisteredEntityIds(request, identifierClazz.name)
  }

  @Suppress("UNCHECKED_CAST")
  fun getRegisteredEntityIds(request: HttpServletRequest, identifier: String): Collection<Serializable>? {
    return getSessionContext(request, identifier)?.idList as? Collection<Serializable>
  }

  fun getRegisteredCallerUrl(request: HttpServletRequest, identifierClazz: Class<out Any>): String? {
    return getRegisteredCallerUrl(request, identifierClazz.name)
  }

  fun getRegisteredCallerUrl(request: HttpServletRequest, identifier: String): String? {
    return getSessionContext(request, identifier)?.callerUrl
  }

  fun getSessionContext(request: HttpServletRequest, identifierClazz: Class<out Any>): SessionContext? {
    return getSessionContext(request, identifierClazz.name)
  }

  fun getSessionContext(request: HttpServletRequest, identifier: String): SessionContext? {
    return ExpiringSessionAttributes.getAttribute(
      request,
      "$SESSSION_ATTRIBUTE_CONTEXT:$identifier",
      SessionContext::class.java,
    )
  }

  private fun ensureSessionContext(request: HttpServletRequest, identifier: String): SessionContext {
    var context = getSessionContext(request, identifier)
    if (context == null) {
      // I think, this mustn't be thread safe, must it?
      context = SessionContext()
      setSessionContext(request, identifier, context)
    }
    return context
  }

  private fun setSessionContext(request: HttpServletRequest, identifier: String, context: SessionContext) {
    ExpiringSessionAttributes.setAttribute(request, "$SESSSION_ATTRIBUTE_CONTEXT:$identifier", context, TTL_MINUTES)
  }

  fun getRegisteredData(request: HttpServletRequest, identifierClazz: Class<out Any>): Any? {
    return getRegisteredData(request, identifierClazz.name)
  }

  fun getRegisteredData(request: HttpServletRequest, identifier: String): Any? {
    return getSessionContext(request, identifier)?.data
  }

  /**
   * Register the selected entities sent by the client for later recovering (e. g. on reload).
   */
  fun registerSelectedEntityIds(
    request: HttpServletRequest,
    identifierClazz: Class<out Any>,
    idList: Collection<Serializable>?
  ) {
    registerSelectedEntityIds(request, identifierClazz.name, idList)
  }

  /**
   * Register the selected entities sent by the client for later recovering (e. g. on reload).
   */
  fun registerSelectedEntityIds(request: HttpServletRequest, identifier: String, idList: Collection<Serializable>?) {
    val context = if (idList.isNullOrEmpty()) {
      getSessionContext(request, identifier) // idList is null, so if context is also null, do nothing.
    } else {
      ensureSessionContext(request, identifier)
    }
    context?.selectedIdList = idList
  }

  /**
   * Gets the previous selected entities.
   */
  fun getRegisteredSelectedEntityIds(request: HttpServletRequest, clazz: Class<out Any>): Collection<Serializable>? {
    return getRegisteredSelectedEntityIds(request, clazz.name)
  }

  /**
   * Gets the previous selected entities.
   */
  fun getRegisteredSelectedEntityIds(request: HttpServletRequest, identifier: String): Collection<Serializable>? {
    return getSessionContext(request, identifier)?.selectedIdList
  }


  @JvmStatic
  fun getMultiSelectionParamMap(): MutableMap<String, Any> {
    return mutableMapOf(REQUEST_PARAM_MULTI_SELECTION to true)
  }

  fun isMultiSelection(request: HttpServletRequest, magicFilter: MagicFilter): Boolean {
    if (request.getParameter(REQUEST_PARAM_MULTI_SELECTION) == "true") {
      magicFilter.multiSelection = true
    }
    return magicFilter.multiSelection == true
  }

  /**
   * Call this method on [AbstractPagesRest.getInitialList], if you want to force multi selection usage only.
   * This is useful, if the page isn't yet migrated from Wicket, but already used for multi selection.
   */
  fun ensureMultiSelectionOnly(
    request: HttpServletRequest,
    pagesRest: AbstractPagesRest<*, *, *>,
    returnToUrl: String,
  ) {
    if (!isMultiSelection(request, pagesRest.getCurrentFilter())) {
      // Force to use this page only in multi selection mode. No normal usage allowed:
      registerEntitiesForSelection(request, this::class.java, emptyList(), returnToUrl)
    }
    pagesRest.getCurrentFilter().multiSelection = true
    ensureSessionContext(request, pagesRest::class.java.name).callerUrl = returnToUrl // Ensure, caller url is set.
  }

  private const val TTL_MINUTES = 60
  private val SESSSION_ATTRIBUTE_CONTEXT = "{${MultiSelectionSupport::class.java.name}.context"
  private const val REQUEST_PARAM_MULTI_SELECTION = "multiSelectionMode"

  class SessionContext(
    var idList: Collection<*>? = null,
    var selectedIdList: Collection<Serializable>? = null,
    var callerUrl: String? = null,
    var data: Any? = null,
    var paginationPageSize: Int? = null,
  )
}

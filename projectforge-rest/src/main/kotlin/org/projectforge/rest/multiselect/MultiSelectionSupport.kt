/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.rest.core.ExpiringSessionAttributes
import java.io.Serializable
import javax.servlet.http.HttpServletRequest

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
  ) {
    registerEntitiesForSelection(request, identifierClazz.name, entityCollection, callerUrl, data)
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
  ) {
    val idList = entityCollection.map { it.id }
    registerEntityIdsForSelection(request, identifier, idList, callerUrl, data)
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
  ) {
    registerSelectedEntityIds(request, identifier, null) // Clear selection.
    ExpiringSessionAttributes.setAttribute(
      request,
      "$SESSSION_ATTRIBUTE_ENTITIES:$identifier",
      idList.take(MAX_DISPLAYED_ENTRIES),
      TTL_MINUTES
    )
    if (data != null) {
      ExpiringSessionAttributes.setAttribute(
        request,
        "$SESSSION_ATTRIBUTE_ENTITIES:$identifier.data",
        data,
        TTL_MINUTES
      )
    }
    callerUrl?.let {
      ExpiringSessionAttributes.setAttribute(
        request,
        "$SESSSION_ATTRIBUTE_ENTITIES:$identifier.callerUrl",
        it,
        TTL_MINUTES
      )
    }
  }

  fun getRegisteredEntityIds(request: HttpServletRequest, identifierClazz: Class<out Any>): Collection<Serializable>? {
    return getRegisteredEntityIds(request, identifierClazz.name)
  }

  @Suppress("UNCHECKED_CAST")
  fun getRegisteredEntityIds(request: HttpServletRequest, identifier: String): Collection<Serializable>? {
    return ExpiringSessionAttributes.getAttribute(
      request,
      "$SESSSION_ATTRIBUTE_ENTITIES:$identifier"
    ) as? Collection<Serializable>
  }

  fun getRegisteredCallerUrl(request: HttpServletRequest, identifierClazz: Class<out Any>): String? {
    return getRegisteredCallerUrl(request, identifierClazz.name)
  }

  fun getRegisteredCallerUrl(request: HttpServletRequest, identifier: String): String? {
    return ExpiringSessionAttributes.getAttribute(
      request,
      "$SESSSION_ATTRIBUTE_ENTITIES:$identifier.callerUrl"
    ) as? String
  }

  fun getRegisteredData(request: HttpServletRequest, identifierClazz: Class<out Any>): Any? {
    return getRegisteredData(request, identifierClazz.name)
  }

  fun getRegisteredData(request: HttpServletRequest, identifier: String): Any? {
    return ExpiringSessionAttributes.getAttribute(
      request,
      "$SESSSION_ATTRIBUTE_ENTITIES:$identifier.data"
    )
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
    if (idList == null) {
      ExpiringSessionAttributes.removeAttribute(request, "$SESSSION_ATTRIBUTE_SELECTED_ENTITIES:$identifier")
      return
    }
    ExpiringSessionAttributes.setAttribute(
      request,
      "$SESSSION_ATTRIBUTE_SELECTED_ENTITIES:$identifier",
      idList,
      TTL_MINUTES
    )
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
    return ExpiringSessionAttributes.getAttribute(
      request,
      "$SESSSION_ATTRIBUTE_SELECTED_ENTITIES:$identifier"
    ) as? Collection<Serializable>
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

  private const val TTL_MINUTES = 60
  private val SESSSION_ATTRIBUTE_ENTITIES = "{${MultiSelectionSupport::class.java.name}.entities"
  private val SESSSION_ATTRIBUTE_SELECTED_ENTITIES = "{${MultiSelectionSupport::class.java.name}.selected.entities"
  private const val REQUEST_PARAM_MULTI_SELECTION = "multiSelectionMode"
  private const val MAX_DISPLAYED_ENTRIES = 500
}

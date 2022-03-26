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

package org.projectforge.rest.core

import mu.KotlinLogging
import org.projectforge.framework.persistence.api.IdObject
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * Supports mass updates of list pages.
 */
object MassUpdateSupport {
  @JvmStatic
  fun registerEntitiesForSelection(
    request: HttpServletRequest,
    clazz: Class<out Any>,
    entityCollection: Collection<IdObject<*>>
  ) {
    registerEntitiesForSelection(request, clazz.name, entityCollection)
  }

  @JvmStatic
  fun registerEntitiesForSelection(
    request: HttpServletRequest,
    identifier: String,
    entityCollection: Collection<IdObject<*>>
  ) {
    val idList = entityCollection.map { it.id }
    ExpiringSessionAttributes.setAttribute(request, SESSSION_ATTRIBUTE_ENTITIES, idList, TTL_MINUTES)
  }

  @JvmStatic
  fun registerEntityIdsForSelection(request: HttpServletRequest, identifier: String, idList: Collection<*>) {
    ExpiringSessionAttributes.setAttribute(request, SESSSION_ATTRIBUTE_ENTITIES, idList, TTL_MINUTES)
  }

  fun getRegisteredEntityIds(request: HttpServletRequest, clazz: Class<out Any>): Collection<*>? {
    return getRegisteredEntityIds(request, clazz.name)
  }

  fun getRegisteredEntityIds(request: HttpServletRequest, identifier: String): Collection<*>? {
    return ExpiringSessionAttributes.getAttribute(request, SESSSION_ATTRIBUTE_ENTITIES) as? Collection<*>
  }

  private const val TTL_MINUTES = 60
  private val SESSSION_ATTRIBUTE_ENTITIES = "{${MassUpdateSupport::class.java.name}.entities"
}

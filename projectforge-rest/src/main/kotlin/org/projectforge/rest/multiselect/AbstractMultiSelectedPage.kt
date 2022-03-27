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

import mu.KotlinLogging
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.ChangeWlanPasswordPageRest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.AbstractPagesRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.io.Serializable
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * Base class of mass updates after multi selection.
 */
abstract class AbstractMultiSelectedPage : AbstractDynamicPageRest() {
  class MultiSelection {
    var selectedIds: Collection<Serializable>? = null
  }

  protected abstract fun getTitleKey(): String

  protected abstract val pagesRestClass: Class<out AbstractPagesRest<*, *, *>>

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val layout = UILayout(getTitleKey())
    LayoutUtils.process(layout)

    layout.postProcessPageMenu()

    return FormLayoutData(MassUpdateData(), layout, createServerData(request))
  }

  protected fun getLayout(request: HttpServletRequest): UILayout {
    val layout = UILayout(getTitleKey())

    layout.add(
      UIButton.createCancelButton(
        ResponseAction(
          PagesResolver.getListPageUrl(pagesRestClass, absolute = true),
          targetType = TargetType.REDIRECT
        )
      )
    )

    layout.add(
      UIButton.createDefaultButton(
        id = "execute",
        title = "execute",
        responseAction = ResponseAction(
          url = "${getRestPath()}/execute",
          targetType = TargetType.POST
        ),
      )
    )
    return layout
  }

  @PostMapping(URL_PATH_SELECTED)
  fun selected(
    request: HttpServletRequest,
    @RequestBody selectedIds: MultiSelection?
  ): ResponseEntity<*> {
    MultiSelectionSupport.registerSelectedEntityIds(request, pagesRestClass, selectedIds?.selectedIds)
    return ResponseEntity.ok(
      ResponseAction(
        targetType = TargetType.REDIRECT,
        url = PagesResolver.getDynamicPageUrl(this::class.java, absolute = true)
      )
    )
  }

  companion object {
    const val URL_PATH_SELECTED = "selected"
  }

}

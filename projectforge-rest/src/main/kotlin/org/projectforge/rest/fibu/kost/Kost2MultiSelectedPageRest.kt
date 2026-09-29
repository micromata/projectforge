/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.fibu.kost

import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.kost.KostentraegerStatus
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateFieldDeclaration
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UILayout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest

/**
 * Mass update after selection of cost 2 entries: status, description and comment.
 */
@RestController
@RequestMapping("${Rest.URL}/cost2${AbstractMultiSelectedPage.URL_SUFFIX_SELECTED}")
class Kost2MultiSelectedPageRest : AbstractMultiSelectedPage<Kost2DO>() {

  @Autowired
  private lateinit var kost2Dao: Kost2Dao

  @Autowired
  private lateinit var kost2EntityRest: Kost2EntityRest

  override val layoutContext: LayoutContext = LayoutContext(Kost2DO::class.java)

  override fun getTitleKey(): String {
    return "fibu.kost2.multiselected.title"
  }

  override val listPageUrl: String = "/${MenuItemDefId.COST2_LIST.url}"

  @PostConstruct
  private fun postConstruct() {
    pagesRest = kost2EntityRest
  }

  /**
   * The same three fields [fillForm] lays out, for a client that renders the form itself.
   */
  override fun fieldDeclarations(): List<MassUpdateFieldDeclaration> {
    return listOf(
      MassUpdateFieldDeclaration("kostentraegerStatus"),
      MassUpdateFieldDeclaration("description", showAppendOption = true),
      MassUpdateFieldDeclaration("comment", showAppendOption = true),
    )
  }

  override fun fillForm(
    request: HttpServletRequest,
    layout: UILayout,
    massUpdateData: MutableMap<String, MassUpdateParameter>,
    selectedIds: Collection<Serializable>?,
    variables: MutableMap<String, Any>,
  ) {
    val lc = LayoutContext(Kost2DO::class.java)
    createAndAddFields(lc, massUpdateData, layout, "kostentraegerStatus")
    createAndAddFields(lc, massUpdateData, layout, "description", showAppendOption = true)
    createAndAddFields(lc, massUpdateData, layout, "comment", showAppendOption = true)
  }

  override fun proceedMassUpdate(
    request: HttpServletRequest,
    selectedIds: Collection<Serializable>,
    massUpdateContext: MassUpdateContext<Kost2DO>,
  ): ResponseEntity<*>? {
    val kost2List = kost2Dao.select(selectedIds)
    if (kost2List.isNullOrEmpty()) {
      return null
    }
    val params = massUpdateContext.massUpdateParams
    kost2List.forEach { kost2 ->
      massUpdateContext.startUpdate(kost2)
      // Set the real column kostentraegerStatus, not the transient effectiveKostentraegerStatus.
      params["kostentraegerStatus"]?.let { param ->
        if (param.delete == true) {
          kost2.kostentraegerStatus = null
        }
        param.textValue?.let { textValue ->
          kost2.kostentraegerStatus = KostentraegerStatus.valueOf(textValue)
        }
      }
      processTextParameter(kost2, "description", params)
      processTextParameter(kost2, "comment", params)
      massUpdateContext.commitUpdate(
        identifier4Message = kost2.formattedNumber,
        kost2,
        update = { kost2Dao.update(kost2) },
      )
    }
    return null
  }
}

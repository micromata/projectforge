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

import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost1Dao
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
 * Mass update after selection of cost 1 entries: status and description.
 */
@RestController
@RequestMapping("${Rest.URL}/cost1${AbstractMultiSelectedPage.URL_SUFFIX_SELECTED}")
class Kost1MultiSelectedPageRest : AbstractMultiSelectedPage<Kost1DO>() {

  @Autowired
  private lateinit var kost1Dao: Kost1Dao

  @Autowired
  private lateinit var kost1EntityRest: Kost1EntityRest

  override val layoutContext: LayoutContext = LayoutContext(Kost1DO::class.java)

  override val listPageUrl: String = "/${MenuItemDefId.COST1_LIST.url}"

  @PostConstruct
  private fun postConstruct() {
    pagesRest = kost1EntityRest
  }

  /**
   * The same two fields [fillForm] lays out, for a client that renders the form itself.
   */
  override fun fieldDeclarations(): List<MassUpdateFieldDeclaration> {
    return listOf(
      MassUpdateFieldDeclaration("kostentraegerStatus"),
      MassUpdateFieldDeclaration("description", showAppendOption = true),
    )
  }

  override fun fillForm(
    request: HttpServletRequest,
    layout: UILayout,
    massUpdateData: MutableMap<String, MassUpdateParameter>,
    selectedIds: Collection<Serializable>?,
    variables: MutableMap<String, Any>,
  ) {
    val lc = LayoutContext(Kost1DO::class.java)
    createAndAddFields(lc, massUpdateData, layout, "kostentraegerStatus")
    createAndAddFields(lc, massUpdateData, layout, "description", showAppendOption = true)
  }

  override fun proceedMassUpdate(
    request: HttpServletRequest,
    selectedIds: Collection<Serializable>,
    massUpdateContext: MassUpdateContext<Kost1DO>,
  ): ResponseEntity<*>? {
    val kost1List = kost1Dao.select(selectedIds)
    if (kost1List.isNullOrEmpty()) {
      return null
    }
    val params = massUpdateContext.massUpdateParams
    kost1List.forEach { kost1 ->
      massUpdateContext.startUpdate(kost1)
      params["kostentraegerStatus"]?.let { param ->
        if (param.delete == true) {
          kost1.kostentraegerStatus = null
        }
        param.textValue?.let { textValue ->
          kost1.kostentraegerStatus = KostentraegerStatus.valueOf(textValue)
        }
      }
      processTextParameter(kost1, "description", params)
      massUpdateContext.commitUpdate(
        identifier4Message = kost1.formattedNumber,
        kost1,
        update = { kost1Dao.update(kost1) },
      )
    }
    return null
  }
}

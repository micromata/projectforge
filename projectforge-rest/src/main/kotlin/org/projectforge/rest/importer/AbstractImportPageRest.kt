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

package org.projectforge.rest.importer

import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.core.aggrid.AGGridSupport
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.LayoutUtils
import org.projectforge.ui.UIAgGrid
import org.projectforge.ui.UILayout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import javax.servlet.http.HttpServletRequest
import kotlin.reflect.KProperty

abstract class AbstractImportPageRest<O : Any> : AbstractDynamicPageRest() {
  /**
   * Contains the data, layout and filter settings served by [getInitialList].
   */
  class InitialListData(
    val ui: UILayout?,
    val data: ResultSet<*>,
  )

  @Autowired
  protected lateinit var agGridSupport: AGGridSupport

  @GetMapping("initialList")
  fun requestInitialList(request: HttpServletRequest): InitialListData {
    val importStorage = ExpiringSessionAttributes.getAttribute(
      request,
      getSessionAttributeName(this::class.java),
      ImportStorage::class.java,
    )
    @Suppress("UNCHECKED_CAST")
    importStorage as ImportStorage<O>
    // ExpiringSessionAttributes.removeAttribute(request.getSession(false), "${this::class.java.name}.importStorage")
    val result = getInitialList(request, importStorage)
    return result
  }

  protected abstract fun createListLayout(request: HttpServletRequest, layout: UILayout, agGrid: UIAgGrid)

  protected fun getInitialList(request: HttpServletRequest, importStorage: ImportStorage<O>): InitialListData {
    val layout = UILayout("plugins.banking.account.record.import.title")
    val agGrid = agGridSupport.prepareUIGrid4MultiSelectionListPage(
      request,
      layout,
      this,
      pageAfterMultiSelect = this::class.java,
    )
    createListLayout(request, layout, agGrid)
    LayoutUtils.process(layout)
    return InitialListData(
      ui = layout,
      data = ResultSet(importStorage.entries, null, magicFilter = MagicFilter()),
    )
  }

  protected fun addReadColumn(agGrid: UIAgGrid, lc: LayoutContext, property: KProperty<*>) {
    val field = property.name
    agGrid.add(lc, "readEntry.$field", lcField = field)
  }

  protected fun addStoredColumn(agGrid: UIAgGrid, lc: LayoutContext, property: KProperty<*>) {
    val field = property.name
    agGrid.add(lc, "storedEntry.$field", lcField = field)
  }

  companion object {
    fun getSessionAttributeName(importPageRest: Class<*>): String {
      return "${importPageRest.name}.importStorage"
    }
  }
}

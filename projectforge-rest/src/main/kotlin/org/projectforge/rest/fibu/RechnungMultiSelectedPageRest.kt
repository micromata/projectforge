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

package org.projectforge.rest.fibu

import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.common.logging.LogEventLoggerNameMatcher
import org.projectforge.common.logging.LogSubscription
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractPagesRest
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UIAlert
import org.projectforge.ui.UIColor
import org.projectforge.ui.UILayout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable
import javax.servlet.http.HttpServletRequest

/**
 * Mass update after selection.
 */
@RestController
@RequestMapping("${Rest.URL}/invoice${AbstractMultiSelectedPage.URL_SUFFIX_SELECTED}")
class RechnungMultiSelectedPageRest : AbstractMultiSelectedPage() {

  @Autowired
  private lateinit var rechnungDao: RechnungDao


  override fun getTitleKey(): String {
    return "fibu.rechnung.multiselected.title"
  }

  override val listPageUrl: String = "/${MenuItemDefId.OUTGOING_INVOICE_LIST.url}"

  override val pagesRestClass: Class<out AbstractPagesRest<*, *, *>>
    get() = RechnungPagesRest::class.java

  override fun fillForm(
    request: HttpServletRequest,
    layout: UILayout,
    massUpdateData: MutableMap<String, MassUpdateParameter>,
    selectedIds: Collection<Serializable>?,
    variables: MutableMap<String, Any>,
  ) {
    val lc = LayoutContext(RechnungDO::class.java)
    createAndAddFields(
      lc,
      massUpdateData,
      layout,
      "datum",
      "status",
      "bezahlDatum",
    )
    createAndAddFields(lc, massUpdateData, layout, "bemerkung", append = true)
    layout.add(UIAlert("fibu.rechnung.multiselected.info", color = UIColor.INFO, markdown = true))
  }

  override fun proceedMassUpdate(
    request: HttpServletRequest,
    params: Map<String, MassUpdateParameter>,
    selectedIds: Collection<Serializable>
  ): ResponseEntity<*> {
    val invoices = rechnungDao.getListByIds(selectedIds)
    if (invoices.isNullOrEmpty()) {
      return showNoEntriesValidationError()
    }
    invoices.forEach { invoice ->
      processTextParameter(invoice, "bemerkung", params)
      params["datum"]?.let { param ->
        if (param.localDateValue != null || param.delete == true) {
          invoice.datum = param.localDateValue
        }
      }
      if (params["status"]?.delete == true) {
        invoice.status = null
      }
      params["bezahlDatum"]?.let { param ->
        param.localDateValue?.let {
          invoice.bezahlDatum = param.localDateValue
          invoice.zahlBetrag = invoice.grossSum
          invoice.status = RechnungStatus.BEZAHLT
        }
        if (param.delete == true) {
          invoice.bezahlDatum = null
          invoice.zahlBetrag = null
          invoice.status = RechnungStatus.GESTELLT
        }
      }
      params[""]?.let {
        invoice.status = null
      }
      params["status"]?.let { param ->
        param.textValue?.let { textValue ->
          invoice.status = RechnungStatus.valueOf(textValue)
        }
        if (param.delete == true) {
          invoice.status = null
        }
      }
      rechnungDao.update(invoice)
    }
    return showToast(invoices.size)
  }

  override fun ensureUserLogSubscription(): LogSubscription {
    val username = ThreadLocalUserContext.getUser().username ?: throw InternalError("User not given")
    val displayTitle = translate("fibu.rechnung.multiselected.title")
    return LogSubscription.ensureSubscription(
      title = "Debitor invoices",
      displayTitle = displayTitle,
      user = username,
      create = { title, user ->
        LogSubscription(
          title,
          user,
          LogEventLoggerNameMatcher(
            "de.micromata.fibu.RechnungDao",
            "org.projectforge.framework.persistence.api.BaseDaoSupport|RechnungDO"
          ),
          maxSize = 10000,
          displayTitle = displayTitle
        )
      })
  }
}

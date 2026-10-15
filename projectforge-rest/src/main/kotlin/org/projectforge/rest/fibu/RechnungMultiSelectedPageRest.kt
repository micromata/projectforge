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

package org.projectforge.rest.fibu

import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.RechnungsStatistik
import org.projectforge.common.logging.LogEventLoggerNameMatcher
import org.projectforge.common.logging.LogSubscription
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateFieldDeclaration
import org.projectforge.ui.LayoutContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest

/**
 * Mass update after selection.
 */
@RestController
@RequestMapping("${Rest.URL}/invoice${AbstractMultiSelectedPage.URL_SUFFIX_SELECTED}")
class RechnungMultiSelectedPageRest : AbstractMultiSelectedPage<RechnungDO>() {

  @Autowired
  private lateinit var rechnungDao: RechnungDao

  @Autowired
  private lateinit var outgoingInvoiceEntityRest: OutgoingInvoiceEntityRest

  override val layoutContext: LayoutContext = LayoutContext(RechnungDO::class.java)

  override val listPageUrl: String = "/${MenuItemDefId.OUTGOING_INVOICE_LIST.url}"

  @PostConstruct
  private fun postConstruct() {
    pagesRest = outgoingInvoiceEntityRest
  }

  /**
   * The three fields of the mass update, for the next frontend that renders the form itself.
   */
  override fun fieldDeclarations(): List<MassUpdateFieldDeclaration> {
    return listOf(
      MassUpdateFieldDeclaration("status"),
      MassUpdateFieldDeclaration("bezahlDatum"),
      MassUpdateFieldDeclaration("bemerkung", showAppendOption = true),
    )
  }

  override fun infoMessageKey(): String {
    return "fibu.rechnung.multiselected.info"
  }

  override fun getStatistics(selectedIds: Collection<Serializable>?): String {
    return buildStatistics(selectedIds).asMarkdown
  }

  /**
   * The same numbers [getStatistics] renders as markdown, as the values the list serves.
   *
   * The markdown (`MultiSelectMetaData.statistics`) is deprecated: it carries the `<span style="color:blue">`
   * `MarkdownBuilder` colours it with. The hand built page renders its own statistics line and needs the
   * numbers, which is also the only way it can format an amount in the *user's* locale and currency.
   */
  override fun getStatisticsData(selectedIds: Collection<Serializable>?): Any {
    return OutgoingInvoiceEntityRest.InvoiceStatistics(buildStatistics(selectedIds))
  }

  private fun buildStatistics(selectedIds: Collection<Serializable>?): RechnungsStatistik {
    // Cache-based (RechnungCache), so this stays cheap when called live on every debounced selection
    // change - no invoice is hydrated and no position is loaded (see RechnungDao.buildStatistikByIds).
    return rechnungDao.buildStatistikByIds(selectedIds)
  }

  override fun proceedMassUpdate(
    request: HttpServletRequest,
    selectedIds: Collection<Serializable>,
    massUpdateContext: MassUpdateContext<RechnungDO>,
  ): ResponseEntity<*>? {
    val invoices = rechnungDao.select(selectedIds)
    if (invoices.isNullOrEmpty()) {
      return null
    }
    val params = massUpdateContext.massUpdateParams
    invoices.forEach { invoice ->
      massUpdateContext.startUpdate(invoice)
      processTextParameter(invoice, "bemerkung", params)
      params["bezahlDatum"]?.let { param ->
        param.localDateValue?.let { localDate ->
          invoice.bezahlDatum = localDate
          invoice.zahlBetrag = invoice.info.grossSum
          invoice.status = RechnungStatus.BEZAHLT
        }
        if (param.delete == true) {
          invoice.bezahlDatum = null
          invoice.zahlBetrag = null
          invoice.status = RechnungStatus.GESTELLT
        }
      }
      params["status"]?.let { param ->
        if (param.delete == true) {
          invoice.status = null
        }
        param.textValue?.let { textValue ->
          invoice.status = RechnungStatus.valueOf(textValue)
        }
      }
      massUpdateContext.commitUpdate(
        identifier4Message = "${invoice.datum} #${NumberFormatter.format(invoice.nummer)}",
        invoice,
        update = { rechnungDao.update(invoice) },
      )
    }
    return null
  }

  override fun ensureUserLogSubscription(): LogSubscription {
    val username = ThreadLocalUserContext.loggedInUser!!.username ?: throw InternalError("User not given")
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

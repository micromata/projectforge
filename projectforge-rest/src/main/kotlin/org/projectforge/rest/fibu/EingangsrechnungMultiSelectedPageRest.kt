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

import mu.KotlinLogging
import org.projectforge.business.fibu.*
import org.projectforge.common.logging.LogEventLoggerNameMatcher
import org.projectforge.common.logging.LogSubscription
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractPagesRest
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.rest.multiselect.MassUpdateStatistics
import org.projectforge.rest.multiselect.MultiSelectionSupport
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UIAlert
import org.projectforge.ui.UIColor
import org.projectforge.ui.UILayout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * Mass update after selection and SEPA transfer export.
 */
@RestController
@RequestMapping("${Rest.URL}/incomingInvoice${AbstractMultiSelectedPage.URL_SUFFIX_SELECTED}")
class EingangsrechnungMultiSelectedPageRest : AbstractMultiSelectedPage() {
  @Autowired
  private lateinit var SEPATransferGenerator: SEPATransferGenerator

  @Autowired
  private lateinit var eingangsrechnungDao: EingangsrechnungDao

  override fun getTitleKey(): String {
    return "fibu.eingangsrechnung.multiselected.title"
  }

  override val listPageUrl: String = "/${MenuItemDefId.INCOMING_INVOICE_LIST.url}"

  override val pagesRestClass: Class<out AbstractPagesRest<*, *, *>>
    get() = EingangsrechnungPagesRest::class.java

  override fun fillForm(
    request: HttpServletRequest,
    layout: UILayout,
    massUpdateData: MutableMap<String, MassUpdateParameter>,
    selectedIds: Collection<Serializable>?,
    variables: MutableMap<String, Any>,
  ) {
    val lc = LayoutContext(EingangsrechnungDO::class.java)
    createAndAddFields(
      lc,
      massUpdateData,
      layout,
      "kreditor",
      "receiver",
      "iban",
      "bic",
      "paymentType",
      "referenz",
      "bezahlDatum",
      minLengthOfTextArea = 1001,
    )
    createAndAddFields(
      lc,
      massUpdateData,
      layout,
      "bemerkung",
      append = true,
    )
    layout.add(UIAlert("fibu.rechnung.multiselected.info", color = UIColor.INFO, markdown = true))

    if (!selectedIds.isNullOrEmpty()) {
      layout.add(
        MenuItem(
          "transferExport",
          i18nKey = "fibu.rechnung.transferExport",
          url = "${getRestPath()}/exportTransfers",
          type = MenuItemTargetType.DOWNLOAD
        )
      )
    }
  }

  override fun proceedMassUpdate(
    request: HttpServletRequest,
    params: Map<String, MassUpdateParameter>,
    selectedIds: Collection<Serializable>,
    massUpdateStatistics: MassUpdateStatistics,
  ): ResponseEntity<*>? {
    val invoices = eingangsrechnungDao.getListByIds(selectedIds)
    if (invoices.isNullOrEmpty()) {
      return null
    }
    invoices.forEach { invoice ->
      processTextParameter(invoice, "bemerkung", params)
      processTextParameter(invoice, "kreditor", params)
      processTextParameter(invoice, "receiver", params)
      processTextParameter(invoice, "iban", params)
      processTextParameter(invoice, "bic", params)
      processTextParameter(invoice, "referenz", params)
      params["bezahlDatum"]?.let { param ->
        param.localDateValue?.let {
          invoice.bezahlDatum = param.localDateValue
          invoice.zahlBetrag = invoice.grossSum
        }
        if (param.delete == true) {
          invoice.bezahlDatum = null
          invoice.zahlBetrag = null
        }
      }
      params["paymentType"]?.let { param ->
        param.textValue?.let { textValue ->
          invoice.paymentType = PaymentType.valueOf(textValue)
        }
        if (param.delete == true) {
          invoice.paymentType = null
        }
      }
      registerUpdate(
        massUpdateStatistics,
        identifier4Message = "${invoice.datum} ${invoice.kreditor}-${invoice.referenz}",
        update = { eingangsrechnungDao.update(invoice) },
      )
    }
    return null
  }

  /**
   * SEPA export as xml.
   */
  @GetMapping("exportTransfers")
  fun exportTransfers(request: HttpServletRequest): ResponseEntity<*> {
    val invoices =
      eingangsrechnungDao.getListByIds(MultiSelectionSupport.getRegisteredSelectedEntityIds(request, pagesRestClass))
    if (invoices.isNullOrEmpty()) {
      return RestUtils.downloadFile("error.txt", translate("massUpdate.error.noEntriesSelected"))
    }
    val filename = "transfer-${PFDateTime.now().iso4FilenamesFormatterMinutes}.xml"
    val result: SEPATransferResult = this.SEPATransferGenerator.format(invoices)
    if (!result.isSuccessful) {
      if (result.errors.isEmpty()) {
        // unknown error
        log.error("Oups, xml has zero size. Filename: $filename")
        return RestUtils.downloadFile("error.txt", translate("fibu.rechnung.transferExport.error"))
      }
      val sb = StringBuilder()
      sb.appendLine(translateMsg(SEPATransferResult.MISSING_FIELDS_ERROR_I18N_KEY, "..."))
        .appendLine()
      result.errors.forEach { invoice, errors ->
        val fields = listOf(invoice.kreditor, invoice.referenz, PFDay.Companion.fromOrNull(invoice.datum)?.format())
        val missingFields = SEPATransferResult.getMissingFields(errors)
        sb.appendLine("[${fields.filter { it != null }.joinToString()}]: $missingFields")
      }
      return RestUtils.downloadFile("error.txt", sb.toString())
    }
    return RestUtils.downloadFile(
      filename,
      result.xml ?: "internal error".toByteArray()
    ) // result.xml shouldn't be null here.
  }

  override fun ensureUserLogSubscription(): LogSubscription {
    val username = ThreadLocalUserContext.getUser().username ?: throw InternalError("User not given")
    val displayTitle = translate("fibu.eingangsrechnung.multiselected.title")
    return LogSubscription.ensureSubscription(
      title = "Creditor invoices",
      displayTitle = displayTitle,
      user = username,
      create = { title, user ->
        LogSubscription(
          title,
          user,
          LogEventLoggerNameMatcher(
            "de.micromata.fibu.EingangsrechnungDao",
            "org.projectforge.framework.persistence.api.BaseDaoSupport|EingangsrechnungDO"
          ),
          maxSize = 10000,
          displayTitle = displayTitle
        )
      })
  }
}

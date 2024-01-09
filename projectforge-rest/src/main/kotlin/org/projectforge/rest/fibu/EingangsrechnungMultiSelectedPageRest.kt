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
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.multiselect.*
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
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * Mass update after selection and SEPA transfer export.
 */
@RestController
@RequestMapping("${Rest.URL}/incomingInvoice${AbstractMultiSelectedPage.URL_SUFFIX_SELECTED}")
class EingangsrechnungMultiSelectedPageRest : AbstractMultiSelectedPage<EingangsrechnungDO>() {
  @Autowired
  private lateinit var SEPATransferGenerator: SEPATransferGenerator

  @Autowired
  private lateinit var eingangsrechnungDao: EingangsrechnungDao

  @Autowired
  private lateinit var eingangsrechnungPagesRest: EingangsrechnungPagesRest

  override val layoutContext: LayoutContext = LayoutContext(EingangsrechnungDO::class.java)

  override fun getTitleKey(): String {
    return "fibu.eingangsrechnung.multiselected.title"
  }

  override val listPageUrl: String = "/${MenuItemDefId.INCOMING_INVOICE_LIST.url}"

  @PostConstruct
  private fun postConstruct() {
    pagesRest = eingangsrechnungPagesRest
  }

  override fun fillForm(
    request: HttpServletRequest,
    layout: UILayout,
    massUpdateData: MutableMap<String, MassUpdateParameter>,
    selectedIds: Collection<Serializable>?,
    variables: MutableMap<String, Any>,
  ) {
    val lc = LayoutContext(EingangsrechnungDO::class.java)
    val stats = EingangsrechnungsStatistik()
    eingangsrechnungDao.getListByIds(selectedIds)?.forEach { invoice ->
      stats.add(invoice)
    }
    layout.add(UIAlert("'${stats.asMarkdown}", color = UIColor.LIGHT, markdown = true))
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
    selectedIds: Collection<Serializable>,
    massUpdateContext: MassUpdateContext<EingangsrechnungDO>,
  ): ResponseEntity<*>? {
    val invoices = eingangsrechnungDao.getListByIds(selectedIds)
    if (invoices.isNullOrEmpty()) {
      return null
    }
    val params = massUpdateContext.massUpdateData
    val massUpdateData = massUpdateContext.massUpdateData
    invoices.forEach { invoice ->
      massUpdateContext.startUpdate(invoice)
      processTextParameter(invoice, "kreditor", params)
      processTextParameter(invoice, "receiver", params)
      processTextParameter(invoice, "iban", params)
      processTextParameter(invoice, "bic", params)
      processTextParameter(invoice, "referenz", params)
      params["bezahlDatum"]?.let { param ->
        param.localDateValue?.let {
          if (!massUpdateData.containsKey("zahlBetrag")) {
            // Add parameter for excel export:
            val param = MassUpdateParameter()
            massUpdateData["zahlBetrag"] = param
          }
          invoice.bezahlDatum = param.localDateValue
          invoice.zahlBetrag = invoice.grossSumWithDiscount
          if (invoice.discountPercent != null && invoice.grossSumWithDiscount.compareTo(invoice.grossSum) != 0) {
            // Append hint about discount.
            val appendText =
              "${translate("fibu.eingangsrechnung.skonto")}: ${NumberFormatter.format(invoice.discountPercent)}%"
            TextFieldModification.appendText(invoice.bemerkung, appendText)
              ?.let { newValue -> invoice.bemerkung = newValue }
          }
        }
        if (param.delete == true) {
          if (!massUpdateData.containsKey("zahlBetrag")) {
            // Add parameter for excel export:
            val param = MassUpdateParameter()
            massUpdateData["zahlBetrag"] = param
          }
          invoice.bezahlDatum = null
          invoice.zahlBetrag = null
        }
      }
      processTextParameter(invoice, "bemerkung", params) // bemerkung is already modified for discount.
      params["paymentType"]?.let { param ->
        param.textValue?.let { textValue ->
          invoice.paymentType = PaymentType.valueOf(textValue)
        }
        if (param.delete == true) {
          invoice.paymentType = null
        }
      }
      massUpdateContext.commitUpdate(
        identifier4Message = "${invoice.datum} ${invoice.kreditor}-${invoice.referenz}",
        invoice,
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
      eingangsrechnungDao.getListByIds(MultiSelectionSupport.getRegisteredSelectedEntityIds(request, pagesRest::class.java))
    if (invoices.isNullOrEmpty()) {
      return RestUtils.downloadFile("error.txt", translate("massUpdate.error.noEntriesSelected"))
    }
    val filename = "transfer-${PFDateTime.now().format4Filenames()}.xml"
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
    val username = ThreadLocalUserContext.user!!.username ?: throw InternalError("User not given")
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

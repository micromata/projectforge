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

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.business.fibu.EInvoiceData
import org.projectforge.business.fibu.EInvoiceReadService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.utils.FileCheck
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/eInvoiceChecker")
class EInvoiceCheckerPageRest : AbstractDynamicPageRest() {

    companion object {
        private const val SESSION_ATTR = "EInvoiceCheckerPageRest.sessionData"
    }

    @Autowired
    private lateinit var eInvoiceReadService: EInvoiceReadService

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest): FormLayoutData {
        val sessionData = getSessionData(request)
        return if (sessionData != null) {
            createResultLayout(request, sessionData)
        } else {
            createUploadLayout(request)
        }
    }

    @PostMapping("upload")
    fun upload(
        request: HttpServletRequest,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<*> {
        val filename = file.originalFilename ?: "unknown"
        log.info { "User uploads e-invoice file: '$filename', size=${file.size} bytes." }

        if (file.isEmpty) {
            return ResponseEntity.ok(
                ResponseAction(targetType = TargetType.UPDATE)
                    .addVariable("ui", createUploadLayoutWithError(request, translate("file.upload.error.empty")))
            )
        }

        FileCheck.checkFile(filename, file.size, "pdf", "xml", megaBytes = 20)?.let { error ->
            return ResponseEntity.ok(
                ResponseAction(targetType = TargetType.UPDATE)
                    .addVariable("ui", createUploadLayoutWithError(request, error))
            )
        }

        return try {
            val content = file.inputStream.use { it.readBytes() }
            val result = eInvoiceReadService.parseFile(filename, content)

            val sessionData = SessionData(
                invoiceData = result.invoiceData,
                attachmentBytes = result.attachmentBytes,
                filename = filename,
            )
            ExpiringSessionAttributes.setAttribute(request, SESSION_ATTR, sessionData, 30)

            val formLayoutData = createResultLayout(request, sessionData)
            ResponseEntity.ok(
                ResponseAction(targetType = TargetType.UPDATE)
                    .addVariable("ui", formLayoutData.ui)
                    .addVariable("data", formLayoutData.data)
                    .addVariable("variables", formLayoutData.variables)
            )
        } catch (ex: Exception) {
            log.error("Error parsing e-invoice file: $filename", ex)
            ResponseEntity.ok(
                ResponseAction(targetType = TargetType.UPDATE)
                    .addVariable(
                        "ui",
                        createUploadLayoutWithError(request, "${translate("file.upload.error")}: ${ex.message}")
                    )
            )
        }
    }

    @PostMapping("downloadAttachment/{id}")
    fun downloadAttachmentAction(
        @PathVariable id: Int,
        request: HttpServletRequest,
    ): ResponseEntity<ResponseAction> {
        return ResponseEntity.ok(
            ResponseAction(
                RestResolver.getRestUrl(this::class.java, "downloadAttachment/file/$id"),
                targetType = TargetType.DOWNLOAD,
            )
        )
    }

    @GetMapping("downloadAttachment/file/{id}")
    fun downloadAttachmentFile(
        @PathVariable id: Int,
        request: HttpServletRequest,
    ): ResponseEntity<*> {
        val sessionData = getSessionData(request)
            ?: return ResponseEntity.notFound().build<Any>()

        val attachment = sessionData.invoiceData.attachments.find { it.id == id }
            ?: return ResponseEntity.notFound().build<Any>()

        val bytes = sessionData.attachmentBytes[attachment.index]
            ?: return ResponseEntity.notFound().build<Any>()

        return RestUtils.downloadFile(attachment.filename, bytes)
    }

    private fun createUploadLayout(request: HttpServletRequest): FormLayoutData {
        val layout = UILayout("fibu.eInvoiceChecker.title")
        layout.add(UIAlert("fibu.eInvoiceChecker.description", markdown = true, color = UIColor.INFO))
        layout.add(
            UIDropArea(
                "file.upload.dropArea",
                uploadUrl = RestResolver.getRestUrl(this::class.java, "upload"),
            )
        )
        LayoutUtils.process(layout)
        return FormLayoutData(null, layout, createServerData(request))
    }

    private fun createUploadLayoutWithError(request: HttpServletRequest, error: String): UILayout {
        val layout = UILayout("fibu.eInvoiceChecker.title")
        layout.add(UIAlert(message = error, color = UIColor.DANGER))
        layout.add(
            UIDropArea(
                "file.upload.dropArea",
                uploadUrl = RestResolver.getRestUrl(this::class.java, "upload"),
            )
        )
        LayoutUtils.process(layout)
        return layout
    }

    private fun createResultLayout(request: HttpServletRequest, sessionData: SessionData): FormLayoutData {
        val data = sessionData.invoiceData
        val layout = UILayout("fibu.eInvoiceChecker.title")

        // Drop area always visible at the top
        layout.add(
            UIDropArea(
                "file.upload.dropArea",
                uploadUrl = RestResolver.getRestUrl(this::class.java, "upload"),
            )
        )

        // Validation status
        if (data.validationErrors.isEmpty()) {
            layout.add(UIAlert("fibu.eInvoiceChecker.valid", color = UIColor.SUCCESS))
        } else {
            layout.add(UIAlert("fibu.eInvoiceChecker.invalid", color = UIColor.DANGER))
        }

        // Document metadata
        val metaFieldset = UIFieldset(title = "fibu.eInvoiceChecker.metadata")
        metaFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("invoiceNumber", label = "fibu.rechnung.nummer"))))
        metaFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("issueDate", label = "fibu.rechnung.datum"))))
        metaFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("dueDate", label = "fibu.rechnung.faelligkeit"))))
        metaFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("currency", label = "fibu.rechnung.currency"))))
        metaFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("documentTypeCode", label = "fibu.eInvoiceChecker.documentTypeCode"))))
        metaFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("buyerReference", label = "fibu.eInvoiceChecker.buyerReference"))))
        metaFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("format", label = "fibu.eInvoiceChecker.format"))))
        metaFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("profile", label = "fibu.eInvoiceChecker.profile"))))
        layout.add(metaFieldset)

        // Seller
        if (data.seller != null) {
            val sellerFieldset = UIFieldset(title = "fibu.eInvoiceChecker.seller")
            sellerFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("seller.name", label = "name"))))
            sellerFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("seller.street", label = "address.addressText"))))
            sellerFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("seller.zip", label = "address.zipCode"))))
            sellerFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("seller.city", label = "address.city"))))
            sellerFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("seller.country", label = "address.country"))))
            sellerFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("seller.vatId", label = "fibu.konto.vatId"))))
            sellerFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("seller.iban", label = "fibu.rechnung.iban"))))
            layout.add(sellerFieldset)
        }

        // Buyer
        if (data.buyer != null) {
            val buyerFieldset = UIFieldset(title = "fibu.eInvoiceChecker.buyer")
            buyerFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("buyer.name", label = "name"))))
            buyerFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("buyer.street", label = "address.addressText"))))
            buyerFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("buyer.zip", label = "address.zipCode"))))
            buyerFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("buyer.city", label = "address.city"))))
            buyerFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("buyer.country", label = "address.country"))))
            buyerFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("buyer.vatId", label = "fibu.konto.vatId"))))
            layout.add(buyerFieldset)
        }

        // Amounts
        val amountsFieldset = UIFieldset(title = "fibu.eInvoiceChecker.amounts")
        amountsFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("totalNetAmount", label = "fibu.auftrag.nettoSumme"))))
        amountsFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("totalTaxAmount", label = "fibu.rechnung.mehrwertSteuerSatz"))))
        amountsFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("totalGrossAmount", label = "fibu.rechnung.bruttoBetrag"))))
        amountsFieldset.add(UIRow().add(UICol().add(UIReadOnlyField("amountDue", label = "fibu.eInvoiceChecker.amountDue"))))
        layout.add(amountsFieldset)

        // Line items table
        if (data.lineItems.isNotEmpty()) {
            val itemsFieldset = UIFieldset(title = "fibu.eInvoiceChecker.lineItems")
            val table = UITable("lineItems")
                .add(UITableColumn("position", title = "fibu.auftrag.position"))
                .add(UITableColumn("description", title = "description"))
                .add(UITableColumn("quantity", title = "fibu.rechnung.menge", dataType = UIDataType.DECIMAL))
                .add(UITableColumn("unit", title = "fibu.eInvoiceChecker.unit"))
                .add(UITableColumn("unitPrice", title = "fibu.rechnung.position.einzelNetto", dataType = UIDataType.DECIMAL))
                .add(UITableColumn("netAmount", title = "fibu.auftrag.nettoSumme", dataType = UIDataType.DECIMAL))
                .add(UITableColumn("vatPercent", title = "fibu.rechnung.mehrwertSteuerSatz", dataType = UIDataType.DECIMAL))
            itemsFieldset.add(table)
            layout.add(itemsFieldset)
        }

        // Attachments
        if (data.attachments.isNotEmpty()) {
            val attFieldset = UIFieldset(title = "fibu.eInvoiceChecker.attachments")
            val attTable = UITable(
                "attachments",
                rowClickPostUrl = RestResolver.getRestUrl(this::class.java, "downloadAttachment"),
            )
                .add(UITableColumn("filename", title = "attachment.fileName"))
                .add(UITableColumn("mimeType", title = "fibu.eInvoiceChecker.mimeType"))
                .add(UITableColumn("size", title = "attachment.fileSize", dataType = UIDataType.INT))
                .add(UITableColumn("description", title = "description"))
            attFieldset.add(attTable)
            layout.add(attFieldset)
        }

        // Validation errors
        if (data.validationErrors.isNotEmpty()) {
            val valFieldset = UIFieldset(title = "fibu.eInvoiceChecker.validation")
            data.validationErrors.forEach { error ->
                valFieldset.add(UIAlert(message = error, color = UIColor.DANGER))
            }
            layout.add(valFieldset)
        }

        LayoutUtils.process(layout)

        val variables = mutableMapOf<String, Any>()
        if (data.lineItems.isNotEmpty()) {
            variables["lineItems"] = data.lineItems
        }
        if (data.attachments.isNotEmpty()) {
            variables["attachments"] = data.attachments
        }

        return FormLayoutData(data, layout, createServerData(request), variables = variables)
    }

    private fun getSessionData(request: HttpServletRequest): SessionData? {
        return ExpiringSessionAttributes.getAttribute(request, SESSION_ATTR) as? SessionData
    }

    private data class SessionData(
        val invoiceData: EInvoiceData,
        val attachmentBytes: Map<Int, ByteArray>,
        val filename: String,
    )
}

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

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.fibu.AuftragDao
import org.projectforge.business.fibu.EInvoiceData
import org.projectforge.business.fibu.EInvoiceReadService
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.UserRightService
import org.projectforge.framework.utils.FileCheck
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.ExpiringSessionAttributes
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

/**
 * The e-invoice checker of projectforge-next (`/next/eInvoiceChecker`, formerly the dynamic React page
 * `react/eInvoiceChecker/dynamic`): reads an uploaded ZUGFeRD PDF or XRechnung XML back
 * ([EInvoiceReadService]) and answers what is in it.
 *
 * The last result is kept in the session for 30 minutes, with the bytes of the embedded attachments, so the
 * page shows it again after a reload and its attachments can be downloaded.
 */
@RestController
@RequestMapping("${Rest.URL}/eInvoiceChecker")
class EInvoiceCheckerRest {
    /** The answer of an upload and of [getResult]: the parsed invoice and the name of the checked file. */
    class CheckResult(val filename: String, val invoiceData: EInvoiceData)

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var eInvoiceReadService: EInvoiceReadService

    /**
     * Who this page requires (`MenuCreator`, `MenuItemDefId.E_INVOICE_CHECKER`): the FIBU/ORGA groups, or
     * order-book users ([UserRightId.PM_ORDER_BOOK]). The checker parses an uploaded file and exposes no
     * ProjectForge data of its own, so it is opened to the non-finance users who now reach the outgoing
     * invoice list (see `RechnungDao.hasOrderBookSelectAccess`, whose right check this mirrors).
     *
     * Every endpoint checks this itself: there is no DAO behind this page that could do it, and a hidden menu
     * entry keeps nobody out.
     */
    private fun checkAccess() {
        if (accessChecker.isLoggedInUserMemberOfGroup(*UserRightService.FIBU_ORGA_GROUPS)) {
            return
        }
        if (accessChecker.hasLoggedInUserRight(
                AuftragDao.USER_RIGHT_ID, false,
                UserRightValue.READONLY, UserRightValue.PARTLYREADWRITE, UserRightValue.READWRITE,
            )
        ) {
            return
        }
        // Neither — let the group check throw the standard AccessException.
        accessChecker.checkIsLoggedInUserMemberOfGroup(*UserRightService.FIBU_ORGA_GROUPS)
    }

    /** The result of the last upload of this session, 204 if there is none (or it expired). */
    @AccessChecked("FIBU/ORGA group or order book right (checkAccess)")
    @GetMapping("result")
    fun getResult(request: HttpServletRequest): ResponseEntity<CheckResult> {
        checkAccess()
        val sessionData = getSessionData(request) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(sessionData.toResult())
    }

    /**
     * Parses the uploaded file. A file that can't be checked at all (empty, wrong type, too large, unreadable)
     * is answered with 400 and the translated reason as body; a readable e-invoice with errors is a regular
     * result, its errors are part of [EInvoiceData.validationErrors].
     */
    @AccessChecked("FIBU/ORGA group or order book right (checkAccess)")
    @PostMapping("upload")
    fun upload(
        request: HttpServletRequest,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<*> {
        checkAccess()
        val filename = file.originalFilename ?: "unknown"
        log.info { "User uploads e-invoice file: '$filename', size=${file.size} bytes." }
        if (file.isEmpty) {
            return ResponseEntity.badRequest().body(translate("file.upload.error.noFileSelected"))
        }
        FileCheck.checkFile(filename, file.size, "pdf", "xml", megaBytes = 20)?.let { error ->
            return ResponseEntity.badRequest().body(error)
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
            ResponseEntity.ok(sessionData.toResult())
        } catch (ex: Exception) {
            log.error(ex) { "Error parsing e-invoice file: $filename" }
            ResponseEntity.badRequest().body("${translate("file.upload.error")} ${ex.message}")
        }
    }

    /** An attachment embedded in the last checked file, 404 if it is unknown or the result expired. */
    @AccessChecked("FIBU/ORGA group or order book right (checkAccess)")
    @GetMapping("attachment/{id}")
    fun downloadAttachment(
        @PathVariable id: Int,
        request: HttpServletRequest,
    ): ResponseEntity<*> {
        checkAccess()
        val sessionData = getSessionData(request)
            ?: return ResponseEntity.notFound().build<Any>()
        val attachment = sessionData.invoiceData.attachments.find { it.id == id }
            ?: return ResponseEntity.notFound().build<Any>()
        val bytes = sessionData.attachmentBytes[attachment.index]
            ?: return ResponseEntity.notFound().build<Any>()
        return RestUtils.downloadFile(attachment.filename, bytes)
    }

    private fun getSessionData(request: HttpServletRequest): SessionData? {
        return ExpiringSessionAttributes.getAttribute(request, SESSION_ATTR) as? SessionData
    }

    private class SessionData(
        val invoiceData: EInvoiceData,
        val attachmentBytes: Map<Int, ByteArray>,
        val filename: String,
    ) {
        fun toResult() = CheckResult(filename, invoiceData)
    }

    companion object {
        private const val SESSION_ATTR = "EInvoiceCheckerRest.sessionData"
    }
}

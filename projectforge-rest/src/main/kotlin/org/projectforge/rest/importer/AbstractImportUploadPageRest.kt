/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.utils.FileCheck
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.fibu.EingangsrechnungUploadPageRest
import org.projectforge.ui.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile

private val log = KotlinLogging.logger {}

/**
 * Abstract base class for implementing upload and import functionality in a dynamic web page context.
 * Extends the AbstractDynamicPageRest to provide CSRF protection and dynamic React page support.
 *
 * This class provides a structured way to handle file uploads via a drag'n drop,
 * validate them, and process the uploaded files.
 * Afterward, the AbstractImportPageRest is expected to handle the actual import logic.
 */
abstract class AbstractImportUploadPageRest : AbstractDynamicPageRest() {
    class ImportUploadData

    abstract val title: String

    open val description: String? = null

    open val templateInfo: String? = null

    open val downloadTemplateSupported: Boolean = false

    abstract fun callerPage(request: HttpServletRequest): String;

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest): FormLayoutData {
        val layout = createLayout()
        val data = ImportUploadData()
        val formLayoutData = FormLayoutData(data, layout, createServerData(request))
        return formLayoutData
    }

    fun createLayout(
        statusText: String? = null,
        isStatusError: Boolean = false,
    ): UILayout {
        val layout = UILayout(title)
        val fieldset = UIFieldset(title = title)
        layout.add(fieldset)
        if (statusText != null) {
            fieldset.add(
                UIAlert(
                    statusText,
                    markdown = false,
                    color = if (isStatusError) UIColor.DANGER else UIColor.SUCCESS
                )
            )
        }
        description?.let {
            layout.add(UIAlert(description, markdown = true, color = UIColor.INFO))
        }
        // Drop Area for file upload
        layout.add(
            UIDropArea(
                "fibu.eingangsrechnung.import.dropArea",
                uploadUrl = RestResolver.getRestUrl(EingangsrechnungUploadPageRest::class.java, "upload")
            )
        )

        templateInfo?.let {
            layout.add(UIAlert(templateInfo, markdown = true, color = UIColor.SECONDARY))
        }

        layout.addAction(
            UIButton.createCancelButton(
                ResponseAction(
                    RestResolver.getRestUrl(this::class.java, "cancel"),
                    targetType = TargetType.GET,
                )
            )
        )
        if (downloadTemplateSupported) {
            layout.addAction(
                UIButton.createDownloadButton(
                    title = "fibu.eingangsrechnung.import.downloadTemplate",
                    responseAction = ResponseAction(
                        RestResolver.getRestUrl(this.javaClass, "template"),
                        targetType = TargetType.DOWNLOAD
                    )
                )
            )
        }
        LayoutUtils.process(layout)
        return layout
    }

    protected open val fileExtensions = arrayOf("xlsx")

    protected open val maxFileUploadSizeMB = 10L // in MB

    /**
     * Will be called if the user wants to upload a file for import.
     */
    @PostMapping("upload")
    fun upload(
        request: HttpServletRequest,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<*> {
        val filename = file.originalFilename ?: "unknown"
        log.info { "User tries to upload invoice import file: '$filename', size=${file.size} bytes." }

        try {
            if (file.isEmpty) {
                return result(translate("file.upload.error.empty"), isStatusError = true)
            }

            FileCheck.checkFile(filename, file.size, *fileExtensions, megaBytes = maxFileUploadSizeMB)?.let { error ->
                return result(error, isStatusError = true)
            }
            proceedUpload(file).let { importUrl ->
                if (importUrl != null) {
                    log.info("Successfully processed file: $filename, redirecting to: $importUrl")
                    return ResponseEntity.ok(
                        ResponseAction(targetType = TargetType.REDIRECT)
                            .addVariable("url", importUrl)
                    )
                } else {
                    return result(translate("file.upload.error"), isStatusError = true)
                }
            }
        } catch (ex: Exception) {
            log.error("Error processing uploaded file: $filename", ex)
            return result(translate("file.upload.error"), isStatusError = true)
        }
    }

    protected open fun proceedUpload(file: MultipartFile): String? {
        return null
    }

    /**
     * Called from UIButton cancel above.
     */
    @GetMapping("cancel")
    fun cancel(request: HttpServletRequest): ResponseAction {
        val callerPage = callerPage(request)
        return ResponseAction(callerPage)
    }

    protected fun result(
        statusText: String? = null,
        isStatusError: Boolean = false,
    ): ResponseEntity<*> {
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE)
                .addVariable("ui", createLayout(statusText = statusText, isStatusError = isStatusError))
        )
    }
}

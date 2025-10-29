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

package org.projectforge.rest.address.importer

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import mu.KotlinLogging
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressDO
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.utils.FileCheck
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.AddressPagesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.core.aggrid.AGGridSupport
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.KProperty

private val log = KotlinLogging.logger {}

/**
 * Standalone VCF import page that doesn't use AbstractImportPageRest.
 * After upload, VCF data is immediately reconciled with the database and displayed in a list.
 * Users can click on a row to open the Address edit page with VCF field comparison.
 */
@RestController
@RequestMapping("${Rest.URL}/addressImportUpload")
class AddressImportUploadPageRest : AbstractDynamicPageRest() {

    companion object {
        /**
         * Session attribute name for storing import data.
         */
        @JvmStatic
        val SESSION_IMPORT_STORAGE_ATTR = "${AddressImportUploadPageRest::class.java.name}.importStorage"

        /**
         * Calculates which fields have changed (are in oldDiffValues) but are not shown as individual columns.
         * Returns a translated, comma-separated list of additional changed fields.
         */
        @JvmStatic
        fun calculateAdditionalChanges(oldDiffValues: Map<String, Any>?): String? {
            if (oldDiffValues.isNullOrEmpty()) {
                return null
            }

            // Fields that are already shown as individual columns (using "read." prefix from diffCell)
            // Note: Individual address, email and phone fields are included here even though they don't have
            // their own columns anymore - they're part of the formatted columns
            // (formattedBusinessAddress, formattedPrivateAddress, formattedPostalAddress, formattedEmails, formattedPhones)
            val displayedFields = setOf(
                "read.name",
                "read.firstName",
                "read.organization",
                "read.formattedBusinessAddress",
                "read.addressText",
                "read.addressText2",
                "read.zipCode",
                "read.city",
                "read.country",
                "read.state",
                "read.formattedPrivateAddress",
                "read.privateAddressText",
                "read.privateAddressText2",
                "read.privateZipCode",
                "read.privateCity",
                "read.privateCountry",
                "read.privateState",
                "read.formattedPostalAddress",
                "read.postalAddressText",
                "read.postalAddressText2",
                "read.postalZipCode",
                "read.postalCity",
                "read.postalCountry",
                "read.postalState",
                "read.formattedEmails",
                "read.email",
                "read.privateEmail",
                "read.formattedPhones",
                "read.businessPhone",
                "read.mobilePhone",
                "read.privatePhone",
                "read.privateMobilePhone",
                "read.fax",
                "read.comment",
            )

            // Find all changed fields that are NOT displayed as individual columns
            val additionalChangedFields = oldDiffValues.keys
                .filter { !displayedFields.contains(it) }
                .map { fieldKey ->
                    // Remove "read." prefix and translate field name
                    val fieldName = fieldKey.removePrefix("read.")
                    translate("address.$fieldName")
                }
                .sorted()

            return if (additionalChangedFields.isEmpty()) {
                null
            } else {
                additionalChangedFields.joinToString(", ")
            }
        }
    }

    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var agGridSupport: AGGridSupport

    private val title: String = "address.book.vCardsImport.title"

    private val description: String = "address.book.vCardsImport.description"

    private val category: String = "addressImportUpload"

    private val fileExtensions = arrayOf("vcf")

    private val maxFileUploadSizeMB = 10L

    /**
     * Retrieves the import storage from session.
     */
    private fun getImportStorage(request: HttpServletRequest): AddressImportStorage? {
        return ExpiringSessionAttributes.getAttribute(
            request,
            SESSION_IMPORT_STORAGE_ATTR,
            AddressImportStorage::class.java,
        )
    }

    /**
     * Clears the import storage from session.
     */
    private fun clearImportStorage(request: HttpServletRequest) {
        ExpiringSessionAttributes.removeAttribute(request, SESSION_IMPORT_STORAGE_ATTR)
    }

    /**
     * Returns the caller page (address list).
     */
    private fun callerPage(): String {
        return PagesResolver.getListPageUrl(AddressPagesRest::class.java, absolute = true)
    }

    /**
     * Checks user access rights.
     */
    private fun checkRight() {
        addressDao.checkLoggedInUserInsertAccess(AddressDO())
    }

    /**
     * GET endpoint: Shows either upload form or import list (after upload).
     */
    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest): FormLayoutData {
        checkRight()
        val importStorage = getImportStorage(request)

        return if (importStorage != null && importStorage.pairEntries.isNotEmpty()) {
            // Show import list with reconciled data
            createImportListFormLayoutData(request, importStorage)
        } else {
            // Show upload form
            val layout = createUploadLayout()
            FormLayoutData(ImportUploadData(), layout, createServerData(request))
        }
    }

    /**
     * Creates the upload form layout.
     */
    private fun createUploadLayout(): UILayout {
        val layout = UILayout(title)
        val fieldset = UIFieldset(title = title)
        layout.add(fieldset)

        layout.add(UIAlert(description, markdown = true, color = UIColor.INFO))

        // Drop Area for file upload
        layout.add(
            UIDropArea(
                "file.upload.dropArea",
                uploadUrl = RestResolver.getRestUrl(this::class.java, "upload")
            )
        )

        layout.addAction(
            UIButton.createCancelButton(
                ResponseAction(
                    callerPage(),
                    targetType = TargetType.REDIRECT,
                )
            )
        )

        LayoutUtils.process(layout)
        return layout
    }

    /**
     * Creates the import list FormLayoutData with reconciled addresses.
     */
    private fun createImportListFormLayoutData(
        request: HttpServletRequest,
        importStorage: AddressImportStorage
    ): FormLayoutData {
        val layout = UILayout(title)

        // Info fieldset
        val fieldset =
            UIFieldset(title = "${importStorage.filename ?: "Import"} - ${importStorage.pairEntries.size} ${translate("address.title.list")}")
        layout.add(fieldset)

        fieldset.add(
            UIAlert(
                translate("address.book.vCardsImport.clickToEdit"),
                markdown = false,
                color = UIColor.INFO
            )
        )

        // AG Grid for import list - use standard grid setup like AddressPagesRest
        val editPageUrl = PagesResolver.getEditPageUrl(AddressPagesRest::class.java, absolute = true)
        val agGrid = UIAgGrid("importEntries", listPageTable = true)
        layout.add(agGrid)

        // Configure row click to open Address edit page in modal
        // The importIndex will be added from row data (see ImportEntryData below)
        agGrid.withRowClickRedirectUrl("$editPageUrl/:id?importIndex={importIndex}", openModal = true)

        // Enable grid state persistence (column order, width, filters, etc.)
        agGrid.onColumnStatesChangedUrl = RestResolver.getRestUrl(this::class.java, RestPaths.SET_COLUMN_STATES)
        agGrid.resetGridStateUrl = RestResolver.getRestUrl(this::class.java, "resetGridState")

        // IMPORTANT: First create columns, THEN restore user preferences
        createImportListColumns(agGrid)

        // Restore saved grid state (column order, width, filters, etc.)
        agGridSupport.restoreColumnsFromUserPref(category, agGrid)

        // Row styling based on status
        agGrid.withGetRowClass(
            """if (params.node.data.status === 'NEW') {
             return 'ag-row-green';
           } else if (['DELETED', 'FAULTY', 'UNKNOWN', 'UNKNOWN_MODIFICATION'].includes(params.node.data.status)) {
             return 'ag-row-red';
           } else if (params.node.data.status === 'MODIFIED') {
             return 'ag-row-blue';
           } else if (params.node.data.status === 'IMPORTED') {
             return 'ag-row-grey';
        }""".trimMargin()
        )

        agGrid.paginationPageSize = 500

        // Action buttons
        layout.addAction(
            UIButton.createCancelButton(
                ResponseAction(
                    RestResolver.getRestUrl(this::class.java, "cancel"),
                    targetType = TargetType.GET,
                )
            )
        )

        LayoutUtils.process(layout)

        // Add entries to variables with index
        val entries = importStorage.pairEntries.mapIndexed { index, pairEntry ->
            val additionalChanges = calculateAdditionalChanges(pairEntry.oldDiffValues)
            ImportEntryData(pairEntry, index, additionalChanges)
        }
        val formLayoutData = FormLayoutData(ImportUploadData(), layout, createServerData(request))
        formLayoutData.variables = mapOf("importEntries" to entries)

        return formLayoutData
    }

    /**
     * Creates columns for the import list AG Grid.
     */
    private fun createImportListColumns(agGrid: UIAgGrid) {
        val lc = LayoutContext(AddressDO::class.java)

        // Status column
        val statusCol =
            UIAgGridColumnDef.createCol(field = "statusAsString", width = 150, headerName = "status", filter = true)
                .withTooltipField("error")
        statusCol.cellRenderer = "importStatusCell"
        agGrid.add(statusCol)

        // Name
        addReadColumn(agGrid, lc, AddressDO::name, width = 150)

        // First Name
        addReadColumn(agGrid, lc, AddressDO::firstName, width = 150)

        // Organization
        addReadColumn(agGrid, lc, AddressDO::organization, width = 200, wrapText = true)

        // Image preview column
        val imageCol = UIAgGridColumnDef.createCol(
            field = "address.imagePreview",
            headerName = "address.image",
            width = 50,
            sortable = false,
            resizable = false,
            filter = false
        )
        imageCol.cellRenderer = "customized"
        agGrid.add(imageCol)

        // Formatted Business Address
        addFormattedAddressColumn(agGrid, "formattedBusinessAddress", "address.heading.businessAddress", width = 300)

        // Formatted Emails (Business + Private)
        addFormattedAddressColumn(agGrid, "formattedEmails", "address.emails", width = 250)

        // Formatted Phones (Business + Mobile + Private + Private Mobile)
        addFormattedAddressColumn(agGrid, "formattedPhones", "address.phoneNumbers", width = 250)

        // Formatted Private Address
        addFormattedAddressColumn(agGrid, "formattedPrivateAddress", "address.heading.privateAddress", width = 300)

        // Formatted Postal Address
        addFormattedAddressColumn(agGrid, "formattedPostalAddress", "address.heading.postalAddress", width = 300)

        // Comment
        addReadColumn(agGrid, lc, AddressDO::comment, width = 200, wrapText = true)

        // Additional Changes - shows all other changed fields not displayed as individual columns
        val additionalChangesCol = UIAgGridColumnDef.createCol(
            field = "additionalChanges",
            headerName = "common.import.additionalChanges",
            width = 250,
            wrapText = true,
        )
        agGrid.add(additionalChangesCol)
    }

    /**
     * Helper method to add a column showing read (VCF) data with diff rendering.
     */
    private fun addReadColumn(
        agGrid: UIAgGrid,
        lc: LayoutContext,
        property: KProperty<*>,
        width: Int? = null,
        wrapText: Boolean? = null,
    ) {
        val field = property.name
        val col = UIAgGridColumnDef.createCol(
            lc,
            "read.$field",
            lcField = field,
            width = width,
            wrapText = wrapText,
        )
        col.cellRenderer = "diffCell"
        agGrid.add(col)
    }

    /**
     * Helper method to add a formatted address column with diff rendering.
     */
    private fun addFormattedAddressColumn(
        agGrid: UIAgGrid,
        fieldName: String,
        headerNameKey: String,
        width: Int? = null,
    ) {
        val col = UIAgGridColumnDef.createCol(
            field = "read.$fieldName",
            headerName = headerNameKey,
            width = width ?: 300,
            wrapText = true,
        )
        col.cellRenderer = "diffCell"
        agGrid.add(col)
    }

    /**
     * POST endpoint: Handles VCF file upload.
     */
    @PostMapping("upload")
    fun upload(
        request: HttpServletRequest,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<*> {
        checkRight()
        val filename = file.originalFilename ?: "unknown"
        log.info { "User tries to upload VCF import file: '$filename', size=${file.size} bytes." }

        try {
            if (file.isEmpty) {
                return result(translate("file.upload.error.empty"), isStatusError = true)
            }

            FileCheck.checkFile(filename, file.size, *fileExtensions, megaBytes = maxFileUploadSizeMB)?.let { error ->
                return result(error, isStatusError = true)
            }

            file.inputStream.use { inputStream ->
                val bytes = inputStream.readBytes()

                // Parse VCF data
                val importStorage = AddressImportStorage()
                importStorage.filename = filename
                importStorage.parseVcfData(bytes)

                // Check if any addresses were parsed
                if (importStorage.readAddresses.isEmpty()) {
                    return result("No addresses found in VCF file", isStatusError = true)
                }

                // Immediately reconcile with database
                log.info { "Reconciling ${importStorage.readAddresses.size} addresses with database..." }
                importStorage.reconcileImportStorage()

                // Store in session (expires after 30 minutes)
                ExpiringSessionAttributes.setAttribute(request, SESSION_IMPORT_STORAGE_ATTR, importStorage, 30)
                log.info { "VCF parsed and reconciled successfully: ${importStorage.pairEntries.size} entries" }
            }

            // Redirect to same page to show import list
            return ResponseEntity(
                ResponseAction(
                    PagesResolver.getDynamicPageUrl(this::class.java, absolute = true),
                    targetType = TargetType.REDIRECT
                ), HttpStatus.OK
            )

        } catch (ex: Exception) {
            log.error("Error processing uploaded file: $filename", ex)
            return result(translate("file.upload.error") + ": ${ex.message}", isStatusError = true)
        }
    }

    /**
     * POST endpoint: Saves grid state (column order, width, filters, etc.)
     */
    @PostMapping(RestPaths.SET_COLUMN_STATES)
    fun updateColumnStates(@Valid @RequestBody gridStateRequest: org.projectforge.rest.dto.aggrid.AGGridStateRequest): String {
        agGridSupport.storeGridState(
            category,
            gridStateRequest.columnState,
            gridStateRequest.filterModel
        )
        return "OK"
    }

    /**
     * GET endpoint: Resets grid state to defaults.
     * Returns ResponseAction with fresh columnDefs, sortModel, and empty filterModel.
     */
    @GetMapping("resetGridState")
    fun resetGridState(request: HttpServletRequest): ResponseAction {
        agGridSupport.resetGridState(category)

        // Get import storage and rebuild layout to get fresh columnDefs
        val importStorage = getImportStorage(request)
        if (importStorage != null) {
            val formLayoutData = createImportListFormLayoutData(request, importStorage)

            // Extract AG Grid element using AGGridSupport helper
            val agGridElement = agGridSupport.findAgGridElement(formLayoutData.ui)

            // Create ResponseAction using AGGridSupport helper
            return agGridSupport.createResetGridStateResponse(agGridElement)
        }

        // No import storage - return empty response
        return ResponseAction(targetType = TargetType.UPDATE)
    }

    /**
     * GET endpoint: Cancels import and returns to address list.
     */
    @GetMapping("cancel")
    fun cancel(request: HttpServletRequest): ResponseAction {
        checkRight()
        clearImportStorage(request)
        return ResponseAction(callerPage())
    }

    /**
     * GET endpoint: Returns image preview for an imported address by index.
     */
    @GetMapping("imagePreview/{importIndex}")
    fun getImportImagePreview(
        @PathVariable importIndex: Int,
        request: HttpServletRequest
    ): ResponseEntity<org.springframework.core.io.Resource> {
        val importStorage = getImportStorage(request)
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)

        val importEntry = importStorage.getImportEntryByIndex(importIndex)
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)

        val addressImage = importEntry.read?.addressImage
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)

        val resource = org.springframework.core.io.ByteArrayResource(addressImage.bytes)
        return RestUtils.downloadFile("image.${addressImage.imageType.extension}", resource)
    }

    /**
     * Helper method to return a ResponseEntity with status message.
     */
    private fun result(
        statusText: String? = null,
        isStatusError: Boolean = false,
    ): ResponseEntity<*> {
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE)
                .addVariable("ui", createUploadLayoutWithStatus(statusText, isStatusError))
        )
    }

    /**
     * Creates upload layout with status message.
     */
    private fun createUploadLayoutWithStatus(statusText: String?, isStatusError: Boolean): UILayout {
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

        layout.add(UIAlert(description, markdown = true, color = UIColor.INFO))

        // Drop Area for file upload
        layout.add(
            UIDropArea(
                "file.upload.dropArea",
                uploadUrl = RestResolver.getRestUrl(this::class.java, "upload")
            )
        )

        layout.addAction(
            UIButton.createCancelButton(
                ResponseAction(
                    callerPage(),
                    targetType = TargetType.REDIRECT,
                )
            )
        )

        LayoutUtils.process(layout)
        return layout
    }

    /**
     * Data class for upload form.
     */
    class ImportUploadData

    /**
     * Data class for import list entries (for AG Grid).
     */
    data class ImportEntryData(
        val importIndex: Int,
        val status: String,
        val statusAsString: String,
        val error: String?,
        val read: AddressImportDTO?,
        val stored: AddressImportDTO?,
        // Add 'id' field for rowClickRedirectUrl :id placeholder
        val id: Long?,
        // Add oldDiffValues for diff cell visualization (shows old values in modified cells)
        val oldDiffValues: Map<String, Any>?,
        // Translated list of additional changed fields not shown as individual columns
        val additionalChanges: String?,
        // Image preview URL if address has an image (renamed to match ImageDataPreview component expectations)
        val previewImageUrl: String?,
    ) {
        constructor(
            pairEntry: org.projectforge.rest.importer.ImportPairEntry<AddressImportDTO>,
            index: Int,
            additionalChanges: String?,
        ) : this(
            importIndex = index,
            status = pairEntry.status.name,
            statusAsString = pairEntry.statusAsString,
            error = pairEntry.error,
            read = pairEntry.read,
            stored = pairEntry.stored,
            id = pairEntry.stored?.id,
            oldDiffValues = pairEntry.oldDiffValues,
            additionalChanges = additionalChanges,
            previewImageUrl = if (pairEntry.read?.addressImage != null) {
                "addressImportUpload/imagePreview/$index"
            } else {
                null
            },
        )
    }
}

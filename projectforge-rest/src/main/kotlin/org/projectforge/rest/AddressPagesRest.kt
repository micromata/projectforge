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

package org.projectforge.rest

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.projectforge.SystemStatus
import org.projectforge.business.address.*
import org.projectforge.business.address.vcard.VCardUtils
import org.projectforge.business.sipgate.SipgateConfiguration
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.utils.LocaleUtils
import org.projectforge.common.FormatterUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.requiredLoggedInUserId
import org.projectforge.framework.time.DateHelper
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.AddressImageServicesRest.Companion.SESSION_IMAGE_ATTR
import org.projectforge.rest.AddressServicesRest.Companion.SESSION_VCF_IMAGE_ATTR
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.*
import org.projectforge.rest.dto.Address
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.sms.SmsSenderConfig
import org.projectforge.ui.*
import org.projectforge.ui.filter.UIFilterElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.*
import java.util.Base64

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/address")
class AddressPagesRest
    : AbstractDTOPagesRest<AddressDO, Address, AddressDao>(
    AddressDao::class.java,
    i18nKeyPrefix = "address.title",
    cloneSupport = CloneSupport.CLONE
) {
    companion object {
        var carddavServerEnabled = false
    }

    override val useModalEditDialog = true

    /**
     * For exporting list of addresses.
     */
    private class ListAddress(
        val address: Address,
        val id: Long, // Needed for history Service
        val deleted: Boolean,
        var imageUrl: String? = null,
        var previewImageUrl: String? = null
    )

    override fun getId(dto: Any): Long? {
        return when (dto) {
            is ListAddress -> dto.id
            else -> super.getId(dto)
        }
    }

    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    @Autowired
    private lateinit var addressImageCache: AddressImageCache

    @Autowired
    private lateinit var addressExport: AddressExport

    @Autowired
    private lateinit var addressImageDao: AddressImageDao

    @Autowired
    private lateinit var addressServicesRest: AddressServicesRest

    @Autowired
    private lateinit var addressImageServicesRest: AddressImageServicesRest

    @Autowired
    private lateinit var personalAddressCache: PersonalAddressCache

    @Autowired
    private lateinit var personalAddressDao: PersonalAddressDao

    @Autowired
    private lateinit var sipgateConfiguration: SipgateConfiguration

    @Autowired
    private lateinit var smsSenderConfig: SmsSenderConfig

    @Autowired
    private lateinit var persistenceService: org.projectforge.framework.persistence.jpa.PfPersistenceService

    @Autowired
    private lateinit var addressTextImportService: AddressTextImportService

    override fun transformForDB(dto: Address): AddressDO {
        val addressDO = AddressDO()
        dto.copyTo(addressDO)
        return addressDO
    }

    override fun transformFromDB(obj: AddressDO, editMode: Boolean): Address {
        val address = Address()
        address.copyFrom(obj)
        address.isFavoriteCard = personalAddressCache.isPersonalAddress(obj.id)
        return address
    }

    /**
     * Initializes new books for adding.
     */
    override fun newBaseDO(request: HttpServletRequest?): AddressDO {
        val address = super.newBaseDO(request)
        address.addressStatus = AddressStatus.UPTODATE
        address.contactStatus = ContactStatus.ACTIVE
        address.add(addressbookDao.globalAddressbook)
        return address
    }

    override fun onGetItemAndLayout(request: HttpServletRequest, dto: Address, formLayoutData: FormLayoutData) {
        ExpiringSessionAttributes.removeAttribute(request.getSession(false), SESSION_IMAGE_ATTR)

        // Capture modal parameter from request and store in serverData so all button actions can access it
        val modal = request.getParameter("modal")
        if (modal == "true") {
            val params = formLayoutData.serverData?.returnToCallerParams?.toMutableMap() ?: mutableMapOf()
            params["modal"] = "true"
            formLayoutData.serverData?.returnToCallerParams = params
        }

        // Check if this is opened from VCF import context
        val importIndexStr = request.getParameter("importIndex")
        if (!importIndexStr.isNullOrBlank()) {
            try {
                val importIndex = importIndexStr.toInt()
                log.info { "Opening address edit in import context: importIndex=$importIndex, addressId=${dto.id}" }

                // Store importIndex in returnToCallerParams so it persists across saves
                val params = formLayoutData.serverData?.returnToCallerParams?.toMutableMap() ?: mutableMapOf()
                params["importIndex"] = importIndexStr
                formLayoutData.serverData?.returnToCallerParams = params

                // Retrieve import storage from session
                val importStorage = ExpiringSessionAttributes.getAttribute(
                    request,
                    org.projectforge.rest.address.importer.AddressImportUploadPageRest.SESSION_IMPORT_STORAGE_ATTR,
                    org.projectforge.rest.address.importer.AddressImportStorage::class.java
                )

                if (importStorage != null) {
                    val importEntry = importStorage.getImportEntryByIndex(importIndex)
                    if (importEntry != null) {
                        // Extract VCF data (read) and DB data (stored)
                        val vcfData = importEntry.read
                        val dbData = importEntry.stored

                        // Add VCF comparison data to form layout variables
                        val vcfComparisonData = buildVcfComparisonData(vcfData, dbData)
                        val currentVariables = formLayoutData.variables?.toMutableMap() ?: mutableMapOf()
                        currentVariables["vcfComparisonData"] = vcfComparisonData
                        currentVariables["importIndex"] = importIndex
                        formLayoutData.variables = currentVariables

                        log.info { "Added VCF comparison data to form (${vcfComparisonData.size} fields)" }
                    } else {
                        log.warn { "Import entry not found at index $importIndex" }
                    }
                } else {
                    log.warn { "Import storage not found in session (expired?)" }
                }
            } catch (e: NumberFormatException) {
                log.error { "Invalid importIndex parameter: $importIndexStr" }
            }
        }
    }

    /**
     * Builds VCF comparison data structure for AddressTextParser section.
     * Returns a map with field name as key and a map with "vcf" and "db" values.
     */
    private fun buildVcfComparisonData(
        vcfData: org.projectforge.rest.address.importer.AddressImportDTO?,
        dbData: org.projectforge.rest.address.importer.AddressImportDTO?
    ): Map<String, Map<String, String?>> {
        val result = mutableMapOf<String, Map<String, String?>>()

        // Helper function to add field if VCF or DB has a value
        fun addField(fieldName: String, vcfValue: String?, dbValue: String?) {
            if (!vcfValue.isNullOrBlank() || !dbValue.isNullOrBlank()) {
                result[fieldName] = mapOf("vcf" to vcfValue, "db" to dbValue)
            }
        }

        // Personal info
        addField("title", vcfData?.title, dbData?.title)
        addField("firstName", vcfData?.firstName, dbData?.firstName)
        addField("name", vcfData?.name, dbData?.name)
        addField("birthName", vcfData?.birthName, dbData?.birthName)
        addField("birthday", vcfData?.birthday?.toString(), dbData?.birthday?.toString())

        // Organization
        addField("organization", vcfData?.organization, dbData?.organization)
        addField("division", vcfData?.division, dbData?.division)
        addField("positionText", vcfData?.positionText, dbData?.positionText)

        // Business contact
        addField("email", vcfData?.email, dbData?.email)
        addField("businessPhone", vcfData?.businessPhone, dbData?.businessPhone)
        addField("mobilePhone", vcfData?.mobilePhone, dbData?.mobilePhone)
        addField("fax", vcfData?.fax, dbData?.fax)

        // Private contact
        addField("privateEmail", vcfData?.privateEmail, dbData?.privateEmail)
        addField("privatePhone", vcfData?.privatePhone, dbData?.privatePhone)
        addField("privateMobilePhone", vcfData?.privateMobilePhone, dbData?.privateMobilePhone)

        // Business address
        addField("addressText", vcfData?.addressText, dbData?.addressText)
        addField("addressText2", vcfData?.addressText2, dbData?.addressText2)
        addField("zipCode", vcfData?.zipCode, dbData?.zipCode)
        addField("city", vcfData?.city, dbData?.city)
        addField("state", vcfData?.state, dbData?.state)
        addField("country", vcfData?.country, dbData?.country)

        // Private address
        addField("privateAddressText", vcfData?.privateAddressText, dbData?.privateAddressText)
        addField("privateAddressText2", vcfData?.privateAddressText2, dbData?.privateAddressText2)
        addField("privateZipCode", vcfData?.privateZipCode, dbData?.privateZipCode)
        addField("privateCity", vcfData?.privateCity, dbData?.privateCity)
        addField("privateState", vcfData?.privateState, dbData?.privateState)
        addField("privateCountry", vcfData?.privateCountry, dbData?.privateCountry)

        // Postal address
        addField("postalAddressText", vcfData?.postalAddressText, dbData?.postalAddressText)
        addField("postalAddressText2", vcfData?.postalAddressText2, dbData?.postalAddressText2)
        addField("postalZipCode", vcfData?.postalZipCode, dbData?.postalZipCode)
        addField("postalCity", vcfData?.postalCity, dbData?.postalCity)
        addField("postalState", vcfData?.postalState, dbData?.postalState)
        addField("postalCountry", vcfData?.postalCountry, dbData?.postalCountry)

        // Other
        addField("website", vcfData?.website, dbData?.website)
        addField("comment", vcfData?.comment, dbData?.comment)
        addField("fingerprint", vcfData?.fingerprint, dbData?.fingerprint)
        addField("publicKey", vcfData?.publicKey, dbData?.publicKey)

        // Form of address and communication language
        addField("form", vcfData?.form?.toString(), dbData?.form?.toString())
        addField("communicationLanguage", vcfData?.communicationLanguage?.displayName, dbData?.communicationLanguage?.displayName)

        return result
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.add(
            UIFilterElement(
                "myFavorites",
                UIFilterElement.FilterType.BOOLEAN,
                translate("address.filter.myFavorites"),
                defaultFilter = true
            )
        )
        elements.add(
            UIFilterElement(
                "doublets",
                UIFilterElement.FilterType.BOOLEAN,
                translate("address.filter.doublets")
            )
        )
        elements.add(UIFilterElement("images", UIFilterElement.FilterType.BOOLEAN, translate("address.filter.images")))
        elements.add(
            UIFilterElement(
                "myEntries",
                UIFilterElement.FilterType.BOOLEAN,
                translate("address.filter.myEntries"),
                tooltip = translate("address.filter.myEntries.tooltip"),
                defaultFilter = false,
            )
        )
    }

    override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<AddressDO>>? {
        val doubletFilterEntry = source.entries.find { it.field == "doublets" }
        doubletFilterEntry?.synthetic = true
        val myFavoritesFilterEntry = source.entries.find { it.field == "myFavorites" }
        myFavoritesFilterEntry?.synthetic = true
        val imagesFilterEntry = source.entries.find { it.field == "images" }
        imagesFilterEntry?.synthetic = true
        val myEntriesEntry = source.entries.find { it.field == "myEntries" }
        myEntriesEntry?.synthetic = true
        val filters = mutableListOf<CustomResultFilter<AddressDO>>()
        if (doubletFilterEntry?.isTrueValue == true) {
            filters.add(DoubletsResultFilter())
        }
        if (myFavoritesFilterEntry?.isTrueValue == true) {
            filters.add(FavoritesResultFilter(personalAddressDao))
        }
        if (imagesFilterEntry?.isTrueValue == true) {
            filters.add(ImagesResultFilter())
        }
        if (myEntriesEntry?.isTrueValue == true) {
            filters.add(
                AddressMyEntriesResultFilter(
                    persistenceService,
                    org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.requiredLoggedInUserId
                )
            )
        }
        source.paginationPageSize?.let {
            // filter.limitResultSize is a workaround, because this rest page doesn't use Ag-Grid and
            // doesn't support paging.
            target.limitResultSize = it
        }
        return filters
    }

    /**
     * Sets also uid to null.
     */
    override fun prepareClone(dto: Address): Address {
        val clone = super.prepareClone(dto)
        clone.uid = null
        return clone
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: Address) {
        if (StringUtils.isAllBlank(dto.name, dto.firstName, dto.organization)) {
            validationErrors.add(ValidationError(translate("address.form.error.toFewFields"), fieldId = "name"))
        }
        if (dto.addressbookList.isNullOrEmpty()) {
            validationErrors.add(
                ValidationError(
                    translateMsg(
                        "validation.error.fieldRequired",
                        translate("address.addressbooks")
                    ), fieldId = "addressbooks"
                )
            )
        }
    }

    override fun onAfterSaveOrUpdate(request: HttpServletRequest, obj: AddressDO, postData: PostData<Address>) {
        val dto = postData.data
        val address = baseDao.findOrLoad(obj.id!!)
        val personalAddress = PersonalAddressDO()
        personalAddress.address = address
        personalAddress.isFavoriteCard = dto.isFavoriteCard
        personalAddressDao.setOwner(personalAddress, requiredLoggedInUserId) // Set current logged in user as owner.
        personalAddressDao.saveOrUpdate(personalAddress)

        val session = request.getSession(false)

        // Handle image deletion first (check imageDeleted flag before processing session upload)
        if (dto.imageDeleted) {
            // Clear any uploaded image from session (user uploaded then deleted before save)
            ExpiringSessionAttributes.removeAttribute(session, SESSION_IMAGE_ATTR)
            // Delete from database if exists (for existing addresses with images)
            addressImageDao.delete(obj.id!!)
            log.info { "Deleted image for address ${obj.id} on form save (imageDeleted flag)" }
        } else {
            // Handle new image upload from session (only if not deleted)
            val image = ExpiringSessionAttributes.getAttribute(session, SESSION_IMAGE_ATTR)
            if (image != null && image is AddressImageServicesRest.SessionImage) {
                // The user uploaded an image, so save it now.
                addressImageDao.saveOrUpdate(obj.id!!, image.bytes, image.imageType)
                ExpiringSessionAttributes.removeAttribute(session, SESSION_IMAGE_ATTR)
            }
        }

        // If this was opened from VCF import context, reconcile import storage
        val importIndexStr = postData.serverData?.returnToCallerParams?.get("importIndex")
            ?: request.getParameter("importIndex")
        if (!importIndexStr.isNullOrBlank()) {
            try {
                val importIndex = importIndexStr.toInt()
                log.info { "Re-reconciling import storage after save/update (importIndex=$importIndex)" }

                // Retrieve import storage from session
                val importStorage = ExpiringSessionAttributes.getAttribute(
                    request,
                    org.projectforge.rest.address.importer.AddressImportUploadPageRest.SESSION_IMPORT_STORAGE_ATTR,
                    org.projectforge.rest.address.importer.AddressImportStorage::class.java
                )

                if (importStorage != null) {
                    // Re-reconcile to update the list with the new/updated address
                    importStorage.reconcileImportStorage()

                    // Mark this specific entry as IMPORTED
                    val importEntry = importStorage.getImportEntryByIndex(importIndex)
                    if (importEntry != null) {
                        importEntry.status = org.projectforge.rest.importer.ImportEntry.Status.IMPORTED
                        log.info { "Marked import entry at index $importIndex as IMPORTED" }
                    } else {
                        log.warn { "Could not find import entry at index $importIndex to mark as IMPORTED" }
                    }

                    // Save updated import storage back to session
                    ExpiringSessionAttributes.setAttribute(request, org.projectforge.rest.address.importer.AddressImportUploadPageRest.SESSION_IMPORT_STORAGE_ATTR, importStorage, 30)
                    log.info { "Import storage re-reconciled successfully" }
                } else {
                    log.warn { "Import storage not found in session (expired?)" }
                }
            } catch (e: NumberFormatException) {
                log.error { "Invalid importIndex parameter: $importIndexStr" }
            }
        }
    }

    override fun onAfterEdit(request: HttpServletRequest, obj: AddressDO, postData: PostData<Address>, event: RestButtonEvent): ResponseAction {
        // Check if this was opened in modal context (captured from query param in onGetItemAndLayout)
        val modal = postData.serverData?.returnToCallerParams?.get("modal")
        if (modal == "true") {
            // Check if we're in import context
            val importIndexStr = postData.serverData?.returnToCallerParams?.get("importIndex")
            if (!importIndexStr.isNullOrBlank()) {
                // Import context: return updated import list data
                try {
                    val importIndex = importIndexStr.toInt()
                    log.info { "Closing modal in import context (importIndex=$importIndex), returning updated import list" }

                    // Retrieve import storage from session (it was already reconciled in onAfterSaveOrUpdate)
                    val importStorage = ExpiringSessionAttributes.getAttribute(
                        request,
                        org.projectforge.rest.address.importer.AddressImportUploadPageRest.SESSION_IMPORT_STORAGE_ATTR,
                        org.projectforge.rest.address.importer.AddressImportStorage::class.java
                    )

                    if (importStorage != null) {
                        // Build updated import entries list
                        val entries = importStorage.pairEntries.mapIndexed { index, pairEntry ->
                            org.projectforge.rest.address.importer.AddressImportUploadPageRest.ImportEntryData(pairEntry, index)
                        }

                        // Return CLOSE_MODAL with updated import entries
                        val responseAction = ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
                        responseAction.addVariable("importEntries", entries)
                        return responseAction
                    } else {
                        log.warn { "Import storage not found in session when closing modal (expired?)" }
                    }
                } catch (e: NumberFormatException) {
                    log.error { "Invalid importIndex parameter: $importIndexStr" }
                }
            }

            // Close the modal without additional data (normal mode or import storage not available)
            // This handles all button actions: save, update, delete, undelete, cancel, clone
            return ResponseAction(targetType = TargetType.CLOSE_MODAL)
        }
        // Default behavior: redirect to list page
        return super.onAfterEdit(request, obj, postData, event)
    }

    /**
     * @return the address view page.
     */
    override fun getStandardEditPage(): String {
        return "${PagesResolver.getDynamicPageUrl(AddressViewPageRest::class.java)}:id"
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        userAccess: UILayout.UserAccess
    ) {
        val addressLC = LayoutContext(lc)
        addressLC.idPrefix = "address."
        val table = agGridSupport.prepareUIGrid4ListPage(
            request,
            layout,
            magicFilter,
            this,
            AddressMultiSelectedPageRest::class.java,
            userAccess,
            legendText = translate("address.list.legend")
        ).withGetRowClass(
            """if (params.node.data.address.isFavoriteCard) { return 'ag-row-blue'; }"""
        )


        val isMultiSelection = isMultiSelectionMode(request, magicFilter)

        // Add edit icon column first (only in normal mode, not in multiselection mode)
        if (!isMultiSelection) {
            table.add(
                UIAgGridColumnDef(
                    field = "edit",
                    headerName = "",
                    width = 20,
                    sortable = false,
                    filter = false,
                    cellRenderer = "customized",
                    resizable = false,
                ).apply {
                    suppressSizeToFit = true
                    pinned = "left"
                    lockPosition = UIAgGridColumnDef.Orientation.LEFT
                    cellRendererParams = mapOf(
                        "icon" to "edit",
                        "tooltip" to translate("edit"),
                        "onClick" to "history.push('${
                            PagesResolver.getEditPageUrl(
                                AddressPagesRest::class.java,
                                absolute = true
                            )
                        }/' + data.address.id + '?modal=true', { background: history.location });"
                    )
                }
            )
        }
        table.add(
            addressLC,
            "isFavoriteCard",
            width = 20,
            resizable = false,
            pinnedAndLocked = UIAgGridColumnDef.Orientation.LEFT,
            sortable = false,
            headerName = translate("address.columnHead.myFavorites"),
            headerTooltip = translate("address.filter.myFavorites"),
            cellRenderer = "formatter",
        )
        table.add(addressLC, "name", "firstName", pinnedAndLocked = UIAgGridColumnDef.Orientation.LEFT)
        table.add(addressLC, "lastUpdate", formatter = UIAgGridColumnDef.Formatter.DATE)
        table.add("address.addressStatusAsString", headerName = "address.addressStatus", width = 110)
        table.add("address.contactStatusAsString", headerName = "address.contactStatus", width = 110)
        table.add(
            addressLC,
            "imagePreview",
            headerName = "address.image",
            sortable = false,
            cellRenderer = "customized",
            width = 50,
            resizable = false
        )
        table.add(addressLC, "organization", "email")
        table.add(
            addressLC,
            "phoneNumbers",
            headerName = "address.phoneNumbers",
            sortable = false,
            cellRenderer = "customized",
            wrapText = true,
            autoHeight = true
        )
        table.add(lc, "address.addressbookList", sortable = false, formatter = UIAgGridColumnDef.Formatter.ADDRESS_BOOK)
        table.withMultiRowSelection(request, magicFilter)

        // Single click on row opens view page (modal) - only in normal mode, not in multiselection mode
        if (!isMultiSelection) {
            table.rowClickRedirectUrl = PagesResolver.getDynamicPageUrl(
                AddressViewPageRest::class.java,
                absolute = true,
                trailingSlash = false
            ) + "/{id}"
            table.rowClickOpenModal = true
        }
        // Customize columns after adding them
        table.getColumnDefById("address.isFavoriteCard").apply {
            cellRendererParams = mapOf("valueIconMap" to mapOf(true to UIIconType.STAR_REGULAR))
        }
        val exportMenu = MenuItem("address.export", i18nKey = "export")
        if (carddavServerEnabled) {
            exportMenu.add(
                MenuItem(
                    "address.useCardDAVService",
                    i18nKey = "address.cardDAV.infopage.title",
                    type = MenuItemTargetType.MODAL,
                    url = PagesResolver.getDynamicPageUrl(CardDAVInfoPageRest::class.java)
                )
            )
        }
        exportMenu.add(
            MenuItem(
                "address.vCardExport",
                i18nKey = "address.book.vCardExport",
                url = "${getRestPath()}/exportFavoritesVCards",
                tooltip = "address.book.vCardExport.tooltip.content",
                tooltipTitle = "address.book.vCardExport.tooltip.title",
                type = MenuItemTargetType.DOWNLOAD
            )
        )
        layout.excelExportSupported = true
        layout.add(exportMenu, 0)
        if (SystemStatus.isDevelopmentMode()) {
            log.info { "**** vCardsImport is only available on development mode. It's not yet finished." }
            // Add VCF Import menu item
            layout.add(
                MenuItem(
                    "address.vCardsImport",
                    i18nKey = "import",
                    url = PagesResolver.getDynamicPageUrl(
                        org.projectforge.rest.address.importer.AddressImportUploadPageRest::class.java,
                        absolute = false
                    ),
                    type = MenuItemTargetType.REDIRECT
                ),
                0,
            )
        }


        layout.getMenuById(GEAR_MENU)?.add(
            MenuItem(
                "address.exportAppleScript4Notes",
                i18nKey = "address.book.export.appleScript4Notes",
                url = "${getRestPath()}/downloadAppleScript",
                tooltipTitle = "address.book.export.appleScript4Notes",
                tooltip = "address.book.export.appleScript4Notes.tooltip",
                type = MenuItemTargetType.DOWNLOAD
            )
        )
    }

    override val autoCompleteSearchFields = arrayOf("name", "firstName", "organization", "city", "privateCity")

    override fun addVariablesForListPage(): Map<String, Any>? {
        return mutableMapOf(
            "phoneCallEnabled" to sipgateConfiguration.isConfigured(),
            "smsEnabled" to smsSenderConfig.isSmsConfigured()
        )
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Address, userAccess: UILayout.UserAccess): UILayout {
        val communicationLanguage = UISelect(
            "communicationLanguage", lc,
            // The used languages are the values (for quicker select). The current language of the dto is
            // therefore a part of the values as well and is needed for displaying the current value.
            values = addressServicesRest.getUsedLanguages().map { UISelectValue(it.value, it.label) },
            autoCompletion = AutoCompletion<String>(url = "address/acLang?search=:search")
        )
        val layout = super.createEditLayout(dto, userAccess)
            //autoCompletion = AutoCompletion(url = "addressBook/ac?search="))))
            // Add text parser collapse at the top
            // Note: In VCF import mode, this will be expanded initially (controlled by vcfComparisonData variable)
            .add(
                UICustomized(
                    "address.textParser",
                    mutableMapOf(
                        "type" to "collapsePanel",
                        "title" to translate("address.parseText.title"),
                        "buttonText" to translate("address.parseText.button"),
                        "initiallyCollapsed" to true,
                        "buttonIcon" to "paste"
                    )
                )
            )

        // Add duplicate warning alert (shown via watchFields when name/firstName changes)
        if (dto.id == null) {
            layout.add(
                UIRow()
                    .add(
                        UICol(12)
                            .add(
                                UIAlert(
                                    id = "duplicateWarning",
                                    title = translate("address.validation.duplicateFound"),
                                    color = UIColor.WARNING,
                                    markdown = true,
                                    message = ""  // Will be populated dynamically via data.duplicateWarning
                                )
                            )
                    )
            )
        }

        layout.add(
            UIRow()
                .add(
                    UIFieldset(12)
                        .add(
                            UIRow()
                                .add(
                                    UICol(UILength(md = 6))
                                        .add(
                                            UIRow()
                                                .add(
                                                    UICol(UILength(lg = 6))
                                                        .add(lc, "addressStatus")
                                                )
                                                .add(
                                                    UICol(UILength(lg = 6))
                                                        .add(lc, "contactStatus")
                                                )
                                        )
                                )
                                .add(
                                    UICol(UILength(md = 6))
                                        .add(
                                            createFavoriteRow(
                                                "isFavoriteCard",
                                                UISelect<Long>(
                                                    "addressbookList", lc,
                                                    multi = true,
                                                    autoCompletion = AutoCompletion<Int>(
                                                        url = AutoCompletion.getAutoCompletionUrl("addressBook"),
                                                        type = AutoCompletion.Type.USER.name
                                                    )
                                                )
                                            )
                                        )
                                )
                        )
                )
        )
            .add(
                UIRow()
                    .add(
                        UIFieldset(UILength(md = 6, lg = 4))
                            .add(lc, "name", "firstName", "birthName")
                            .add(
                                UIRow()
                                    .add(
                                        UICol(UILength(xl = 6))
                                            .add(lc, "form")
                                    )
                                    .add(
                                        UICol(UILength(xl = 6))
                                            .add(lc, "title")
                                    )
                            )
                            .add(lc, "email", "privateEmail")
                    )
                    .add(
                        UIFieldset(UILength(md = 6, lg = 4))
                            .add(lc, "birthday")
                            .add(communicationLanguage)
                            .add(UIInput("organization", lc).enableAutoCompletion(this))
                            .add(lc, "division", "positionText", "website")
                    )
                    .add(
                        UIFieldset(UILength(md = 6, lg = 4))
                            .add(lc, "businessPhone", "mobilePhone", "fax", "privatePhone", "privateMobilePhone")
                    )
            )
            .add(
                UIRow()
                    .add(
                        UIFieldset(UILength(md = 6, lg = 4), title = "address.heading.businessAddress")
                            .add(UIInput("addressText", lc, ignoreAdditionalLabel = true).enableAutoCompletion(this))
                            .add(UIInput("addressText2", lc, ignoreAdditionalLabel = true).enableAutoCompletion(this))
                            .add(
                                UIRow()
                                    .add(
                                        UICol(UILength(xl = 3))
                                            .add(UIInput("zipCode", lc, ignoreAdditionalLabel = true))
                                    )
                                    .add(
                                        UICol(UILength(xl = 9))
                                            .add(UIInput("city", lc, ignoreAdditionalLabel = true))
                                    )
                            )
                            .add(
                                UIRow()
                                    .add(
                                        UICol(UILength(xl = 6))
                                            .add(UIInput("country", lc, ignoreAdditionalLabel = true))
                                    )
                                    .add(
                                        UICol(UILength(xl = 6))
                                            .add(UIInput("state", lc, ignoreAdditionalLabel = true))
                                    )
                            )
                    )
                    .add(
                        UIFieldset(UILength(md = 6, lg = 4), title = "address.heading.postalAddress")
                            .add(
                                UIInput("postalAddressText", lc, ignoreAdditionalLabel = true).enableAutoCompletion(
                                    this
                                )
                            )
                            .add(
                                UIInput("postalAddressText2", lc, ignoreAdditionalLabel = true).enableAutoCompletion(
                                    this
                                )
                            )
                            .add(
                                UIRow()
                                    .add(
                                        UICol(UILength(xl = 3))
                                            .add(UIInput("postalZipCode", lc, ignoreAdditionalLabel = true))
                                    )
                                    .add(
                                        UICol(UILength(xl = 9))
                                            .add(UIInput("postalCity", lc, ignoreAdditionalLabel = true))
                                    )
                            )
                            .add(
                                UIRow()
                                    .add(
                                        UICol(UILength(xl = 6))
                                            .add(UIInput("postalCountry", lc, ignoreAdditionalLabel = true))
                                    )
                                    .add(
                                        UICol(UILength(xl = 6))
                                            .add(UIInput("postalState", lc, ignoreAdditionalLabel = true))
                                    )
                            )
                    )
                    .add(
                        UIFieldset(UILength(md = 6, lg = 4), title = "address.heading.privateAddress")
                            .add(
                                UIInput("privateAddressText", lc, ignoreAdditionalLabel = true).enableAutoCompletion(
                                    this
                                )
                            )
                            .add(
                                UIInput("privateAddressText2", lc, ignoreAdditionalLabel = true).enableAutoCompletion(
                                    this
                                )
                            )
                            .add(
                                UIRow()
                                    .add(
                                        UICol(UILength(xl = 3))
                                            .add(UIInput("privateZipCode", lc, ignoreAdditionalLabel = true))
                                    )
                                    .add(
                                        UICol(UILength(xl = 9))
                                            .add(UIInput("privateCity", lc, ignoreAdditionalLabel = true))
                                    )
                            )
                            .add(
                                UIRow()
                                    .add(
                                        UICol(UILength(xl = 6))
                                            .add(UIInput("privateCountry", lc, ignoreAdditionalLabel = true))
                                    )
                                    .add(
                                        UICol(UILength(xl = 6))
                                            .add(UIInput("privateState", lc, ignoreAdditionalLabel = true))
                                    )
                            )
                    )
            )
            .add(
                UIRow()
                    .add(
                        UIFieldset(UILength(md = 6, lg = 4), title = "address.image")
                            .add(UICustomized("address.edit.image"))
                    )
                    .add(
                        UIFieldset(UILength(md = 6, lg = 8), title = "address.publicKey")
                            .add(lc, "fingerprint", "publicKey")
                    )
            )
            .add(lc, "comment")

        layout.getInputById("name").focus = true
        layout.getTextAreaById("comment").cssClass = CssClassnames.MT_5

        // Watch name and firstName fields for duplicate detection (only for new addresses)
        if (dto.id == null) {
            layout.watchFields.add("name")
            layout.watchFields.add("firstName")
        }

        layout.addTranslations(
            "apply",
            "delete",
            "parse",
            "cancel",
            "address.parseText.addressBlock",
            "address.parseText.addressType.business",
            "address.parseText.addressType.postal",
            "address.parseText.addressType.private",
            "address.parseText.confidence.high",
            "address.parseText.confidence.legend",
            "address.parseText.confidence.low",
            "address.parseText.confidence.medium",
            "address.parseText.fieldsParsed",
            "address.parseText.info.noChanges",
            "address.parseText.inputLabel",
            "address.parseText.inputPlaceholder",
            "address.parseText.warning.nameDifferent",
            "address.parseText.button.tooltip",
            "address.book.vCardsImport.dataLoaded",
            "address.book.vCardsImport.dropLabel",
            "address.book.vCardsImport.dropHint",
            "address.book.vCardsImport.dropInfo",
            "address.book.vCardsImport.uploading",
            "address.book.vCardsImport.wrongFileType",
            "address.book.vCardsImport.error.noMatch",
            "address.book.vCardsImport.error.parsing",
            "address.title",
            "firstName",
            "name",
            "organization",
            "address.division",
            "address.form",
            "address.positionText",
            "address.phone",
            "address.phoneType.mobile",
            "address.phoneType.fax",
            "email",
            "address.addressText",
            "address.addressText2",
            "address.zipCode",
            "address.city",
            "address.state",
            "address.country",
            "address.website",
            "address.birthName",
            "address.birthday",
            "address.business",
            "address.private",
            "address.postal",
            "comment",
            "common.import.action.selectAll",
            "common.import.action.deselectAll",
        )
        layout.addTranslation(
            "address.image.upload.error",
            translateMsg(
                "address.image.upload.error",
                FormatterUtils.formatBytes(addressImageServicesRest.maxImageSize.toBytes())
            )
        )
        if (dto.id != null) {
            layout.add(
                MenuItem(
                    "address.printView",
                    i18nKey = "printView",
                    url = AddressViewPageRest.getPageUrl(dto.id, absolute = false),
                    type = MenuItemTargetType.REDIRECT,
                )
            )
            layout.add(
                MenuItem(
                    "address.vCardSingleExport",
                    i18nKey = "address.book.vCardSingleExport",
                    url = "${getRestPath()}/exportVCard/${dto.id}",
                    type = MenuItemTargetType.DOWNLOAD
                )
            )
            layout.add(
                MenuItem(
                    "address.directCall",
                    i18nKey = "address.directCall.call",
                    url = "wa/phoneCall?addressId=${dto.id}&callerPage=addressList",
                    type = MenuItemTargetType.REDIRECT
                )
            )
        }
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    /**
     * @return New result set of dto's, transformed from data base objects.
     */
    override fun postProcessResultSet(
        resultSet: ResultSet<AddressDO>,
        request: HttpServletRequest,
        magicFilter: MagicFilter,
    ): ResultSet<*> {
        val newList = resultSet.resultSet.map {
            val image = addressImageCache.getImage(it.id)
            ListAddress(
                transformFromDB(it),
                id = it.id!!,
                deleted = it.deleted,
                imageUrl = if (image != null) "address/image/${it.id}" else null,
                previewImageUrl = if (image != null) "address/imagePreview/${it.id}" else null
            )
        }
        newList.forEach {
            it.address.imageData = null
            it.address.imageDataPreview = null
        }
        return ResultSet(
            newList,
            resultSet,
            newList.size,
            selectedEntityIds = resultSet.selectedEntityIds,
            magicFilter = magicFilter
        )
    }

    private fun createFavoriteRow(id: String, inputElement: UIElement): UIRow {
        return UIRow()
            .add(
                UICol(9)
                    .add(inputElement)
            )
            .add(
                UICol(3)
                    .add(UICheckbox(id, label = "favorite"))
            )
    }

    /**
     * Exports favorites addresses.
     */
    @PostMapping(RestPaths.REST_EXCEL_SUB_PATH)
    fun exportAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        log.info("Exporting addresses as Excel file.")
        @Suppress("UNCHECKED_CAST")
        val list = getObjectList(this, baseDao, filter)
        val personalAddressMap = personalAddressDao.personalAddressByAddressId
        val xls = addressExport.export(list, personalAddressMap)
        if (xls == null || xls.isEmpty()) {
            return RestUtils.downloadFile("empty.txt", "nothing to export.")
        }
        val filename = "ProjectForge-AddressExport_${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx"
        return RestUtils.downloadFile(filename, xls)
    }

    /**
     * Parses address text (email signature) and returns structured field mapping.
     */
    @PostMapping("parseText")
    fun parseText(@RequestBody data: PostData<Map<String, String>>): ResponseEntity<ResponseAction> {
        val inputText = data.data["inputText"] ?: ""
        log.info { "Parsing address text (${inputText.length} characters)" }

        val parsed = addressTextImportService.parseText(inputText)

        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE)
                .addVariable("parsedData", parsed)
        )
    }

    /**
     * DTO for applying parsed address data.
     */
    data class ApplyParsedDataRequest(
        val address: Address,
        val selectedFields: Map<String, String>,
        val applyImage: Boolean = false
    )

    /**
     * Applies parsed data to address form fields.
     */
    @PostMapping("applyParsedData")
    fun applyParsedData(
        @RequestBody request: ApplyParsedDataRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ResponseAction> {
        val address = request.address
        val selectedFields = request.selectedFields
        val applyImage = request.applyImage

        // Handle image from VCF if requested
        var vcfImageBase64: String? = null
        var vcfImageFilename: String? = null
        var vcfImageMimeType: String? = null
        if (applyImage) {
            val session = httpRequest.getSession(false)
            val vcfImage = ExpiringSessionAttributes.getAttribute(session, SESSION_VCF_IMAGE_ATTR)
                as? AddressServicesRest.SessionVcfImage

            if (vcfImage != null && !vcfImage.imageTooLarge) {
                // Send image bytes to frontend (Base64-encoded)
                vcfImageBase64 = Base64.getEncoder().encodeToString(vcfImage.bytes)
                vcfImageFilename = vcfImage.filename
                vcfImageMimeType = vcfImage.imageType.mimeType

                // Clear VCF image from session (frontend will handle upload)
                ExpiringSessionAttributes.removeAttribute(session, SESSION_VCF_IMAGE_ATTR)
                log.info { "Sending VCF image to frontend: ${vcfImage.filename} (${vcfImage.bytes.size} bytes, ${vcfImage.imageType})" }
            }
        }

        // Apply only selected fields
        selectedFields.forEach { (fieldName, value) ->
            when (fieldName) {
                "title" -> address.title = value
                "firstName" -> address.firstName = value
                "name" -> address.name = value
                "organization" -> address.organization = value
                "division" -> address.division = value
                "positionText" -> address.positionText = value
                "businessPhone" -> address.businessPhone = value
                "mobilePhone" -> address.mobilePhone = value
                "fax" -> address.fax = value
                "privatePhone" -> address.privatePhone = value
                "privateMobilePhone" -> address.privateMobilePhone = value
                "email" -> address.email = value
                "privateEmail" -> address.privateEmail = value
                // Business address fields
                "addressText" -> address.addressText = value
                "addressText2" -> address.addressText2 = value
                "zipCode" -> address.zipCode = value
                "city" -> address.city = value
                "state" -> address.state = value
                "country" -> address.country = value
                // Postal address fields
                "postalAddressText" -> address.postalAddressText = value
                "postalAddressText2" -> address.postalAddressText2 = value
                "postalZipCode" -> address.postalZipCode = value
                "postalCity" -> address.postalCity = value
                "postalState" -> address.postalState = value
                "postalCountry" -> address.postalCountry = value
                // Private address fields
                "privateAddressText" -> address.privateAddressText = value
                "privateAddressText2" -> address.privateAddressText2 = value
                "privateZipCode" -> address.privateZipCode = value
                "privateCity" -> address.privateCity = value
                "privateState" -> address.privateState = value
                "privateCountry" -> address.privateCountry = value
                "website" -> address.website = value
                "comment" -> address.comment = value
                // Additional VCard fields
                "birthName" -> address.birthName = value
                "birthday" -> address.birthday = tryParseLocalDate(value)
                "fingerprint" -> address.fingerprint = value
                "publicKey" -> address.publicKey = value
                "form" -> address.form = tryParseFormOfAddress(value)
                "communicationLanguage" -> address.communicationLanguage = tryParseLocale(value)
            }
        }

        log.info { "Applied ${selectedFields.size} parsed fields to address" }

        val response = ResponseAction(targetType = TargetType.UPDATE)
            .addVariable("data", address)
            .addVariable("ui", UIToast.createToast(translate("address.parseText.applied")))

        // Include VCF image data if present (frontend will handle upload)
        if (vcfImageBase64 != null) {
            response.addVariable("vcfImageData", vcfImageBase64)
            response.addVariable("vcfImageFilename", vcfImageFilename)
            response.addVariable("vcfImageMimeType", vcfImageMimeType)
        }

        return ResponseEntity.ok(response)
    }

    /**
     * Called when watched fields (name, firstName) are modified.
     * Checks for duplicate addresses and shows warning if found.
     */
    override fun onWatchFieldsUpdate(
        request: HttpServletRequest,
        dto: Address,
        watchFieldsTriggered: Array<String>?
    ): ResponseEntity<ResponseAction> {
        // Only check for new addresses
        if (dto.id != null) {
            return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
        }

        // Check if name or firstName was triggered
        if (watchFieldsTriggered?.contains("name") != true && watchFieldsTriggered?.contains("firstName") != true) {
            return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
        }

        val name = dto.name
        val firstName = dto.firstName

        // Both fields should have values for meaningful duplicate check
        if (name.isNullOrBlank() && firstName.isNullOrBlank()) {
            // Clear the warning
            dto.duplicateWarning = ""
            return ResponseEntity.ok(
                ResponseAction(targetType = TargetType.UPDATE)
                    .addVariable("data", dto)
            )
        }

        // Search for duplicates using AddressDao
        val duplicates = baseDao.findByNameAndFirstName(name, firstName, checkAccess = true)

        if (duplicates.isEmpty()) {
            // No duplicates: hide alert
            dto.duplicateWarning = ""
        } else {
            // Duplicates found: build warning message
            dto.duplicateWarning = buildDuplicateWarningMessage(duplicates)
        }

        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE)
                .addVariable("data", dto)
        )
    }

    /**
     * Builds markdown-formatted warning message showing duplicate addresses.
     */
    private fun buildDuplicateWarningMessage(duplicates: List<AddressDO>): String {
        val sb = StringBuilder()
        sb.append("**${translate("address.validation.duplicateWarning")}**\n\n")

        duplicates.forEach { addr ->
            sb.append("- ")
            sb.append(addr.fullName)

            // Email (comma separated)
            if (!addr.email.isNullOrBlank()) {
                sb.append(", ${addr.email}")
            }

            sb.append("\n")
        }

        return sb.toString()
    }

    /**
     * Helper function to parse LocalDate from string using PFDayUtils.parseDate().
     * Accepts ISO format (YYYY-MM-DD), user's date format, and other formats.
     * Returns null if parsing fails.
     */
    private fun tryParseLocalDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return PFDayUtils.parseDate(value)
    }

    /**
     * Helper function to parse FormOfAddress from string representation.
     * Uses VCardUtils.parseFormOfAddress() which checks all supported locales.
     * Accepts both enum names (PERSON_MALE) and localized display strings (Herr, Mr.).
     * Returns null if parsing fails.
     */
    private fun tryParseFormOfAddress(value: String?): FormOfAddress? {
        return VCardUtils.parseFormOfAddress(value)
    }

    /**
     * Helper function to parse Locale from language tag string.
     * Uses LocaleUtils.parse() which handles language tags (e.g., "de", "en-US").
     * Returns null if parsing fails.
     */
    private fun tryParseLocale(value: String?): Locale? {
        return LocaleUtils.parse(value)
    }
}

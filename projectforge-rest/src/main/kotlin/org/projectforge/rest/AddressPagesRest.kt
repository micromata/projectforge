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
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.image.ImageService
import org.projectforge.business.sipgate.SipgateConfiguration
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
import java.util.*

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
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var imageService: ImageService

    @Autowired
    private lateinit var languageService: LanguageService

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
        val image = ExpiringSessionAttributes.getAttribute(session, SESSION_IMAGE_ATTR)
        if (image != null && image is AddressImageServicesRest.SessionImage) {
            // The user uploaded an image, so save it now.
            addressImageDao.saveOrUpdate(obj.id!!, image.bytes, image.imageType)
            ExpiringSessionAttributes.removeAttribute(session, SESSION_IMAGE_ATTR)
        }
    }

    override fun onAfterEdit(obj: AddressDO, postData: PostData<Address>, event: RestButtonEvent): ResponseAction {
        // Check if this was opened in modal context (captured from query param in onGetItemAndLayout)
        val modal = postData.serverData?.returnToCallerParams?.get("modal")
        if (modal == "true") {
            // Close the modal instead of redirecting to list page
            // This handles all button actions: save, update, delete, undelete, cancel, clone
            return ResponseAction(targetType = TargetType.CLOSE_MODAL)
        }
        // Default behavior: redirect to list page
        return super.onAfterEdit(obj, postData, event)
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
                        }/' + data.address.id);"
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
                    i18nKey = "address.book.vCardsImport.menu",
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
            .add(
                UIRow()
                    .add(
                        UIFieldset(12)
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
                    )
            )
            .add(
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
        layout.addTranslations(
            "delete",
            "file.upload.dropArea",
            "hide",
            "apply",
            "parse",
            "cancel",
            "address.parseText.addressBlock",
            "address.parseText.addressType.business",
            "address.parseText.addressType.postal",
            "address.parseText.addressType.private",
            "address.parseText.fieldsParsed",
            "address.parseText.info.noChanges",
            "address.parseText.inputLabel",
            "address.parseText.inputPlaceholder",
            "address.parseText.remapTo"
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
        val selectedFields: Map<String, String>
    )

    /**
     * Applies parsed data to address form fields.
     */
    @PostMapping("applyParsedData")
    fun applyParsedData(@RequestBody request: ApplyParsedDataRequest): ResponseEntity<ResponseAction> {
        val address = request.address
        val selectedFields = request.selectedFields

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
            }
        }

        log.info { "Applied ${selectedFields.size} parsed fields to address" }

        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE)
                .addVariable("data", address)
                .addVariable("ui", UIToast.createToast(translate("address.parseText.applied")))
        )
    }
}

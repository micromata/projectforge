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

package org.projectforge.rest

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.NextMigration
import org.projectforge.business.address.*
import org.projectforge.common.FormatterUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.AddressImageServicesRest.Companion.SESSION_IMAGE_ATTR
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.*
import org.projectforge.rest.dto.Address
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The React form of an address (server laid out `UILayout`), kept only for the address campaigns: their value list
 * (`AddressCampaignValuePagesRest`, still React) opens it as a modal under `react/addressLegacy/edit/<id>?modal=true`.
 * Everything else about addresses is served layout-free by [AddressEntityRest] to projectforge-next.
 *
 * A category of its own (`addressLegacy`), not `address`: `address` is migrated, so its React urls are bent onto
 * next by `OrphanedLinkFilter`, the React app marks its pages as legacy, and `PagesResolver` answers next urls for it.
 * This category is none of that, so the modal keeps working - reload included - without any exception elsewhere.
 *
 * To be removed (together with the React address components) once the address campaign values are migrated.
 */
@RestController
@RequestMapping("${Rest.URL}/addressLegacy")
class AddressLegacyPagesRest : AbstractDTOPagesRest<AddressDO, Address, AddressDao>(
    AddressDao::class.java,
    i18nKeyPrefix = "address.title",
    cloneSupport = CloneSupport.CLONE,
) {
    override val useModalEditDialog = true

    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    @Autowired
    private lateinit var addressImageServicesRest: AddressImageServicesRest

    @Autowired
    private lateinit var addressSaveSupport: AddressSaveSupport

    @Autowired
    private lateinit var addressServicesRest: AddressServicesRest

    @Autowired
    private lateinit var personalAddressCache: PersonalAddressCache

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

    override fun newBaseDO(request: HttpServletRequest?): AddressDO {
        val address = super.newBaseDO(request)
        address.addressStatus = AddressStatus.UPTODATE
        address.contactStatus = ContactStatus.ACTIVE
        address.add(addressbookDao.globalAddressbook)
        return address
    }

    override fun onGetItemAndLayout(request: HttpServletRequest, dto: Address, formLayoutData: FormLayoutData) {
        ExpiringSessionAttributes.removeAttribute(request.getSession(false), SESSION_IMAGE_ATTR)
        // The modal parameter goes into the server data, so every button of the form knows it.
        if (request.getParameter("modal") == "true") {
            val params = formLayoutData.serverData?.returnToCallerParams?.toMutableMap() ?: mutableMapOf()
            params["modal"] = "true"
            formLayoutData.serverData?.returnToCallerParams = params
        }
    }

    override fun prepareClone(dto: Address): Address {
        val clone = super.prepareClone(dto)
        clone.uid = null
        return clone
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: Address) {
        addressSaveSupport.validate(validationErrors, dto)
    }

    override fun onAfterSaveOrUpdate(request: HttpServletRequest, obj: AddressDO, postData: PostData<Address>) {
        addressSaveSupport.onAfterSaveOrUpdate(request, obj, postData.data)
    }

    /**
     * In the modal (the only way this form is meant to be opened) every button closes it; otherwise the user lands
     * on the address list of projectforge-next.
     */
    override fun onAfterEdit(
        request: HttpServletRequest,
        obj: AddressDO,
        postData: PostData<Address>,
        event: RestButtonEvent,
    ): ResponseAction {
        if (postData.serverData?.returnToCallerParams?.get("modal") == "true") {
            return ResponseAction(targetType = TargetType.CLOSE_MODAL)
        }
        return super.onAfterEdit(request, obj, postData, event)
    }

    override fun afterOperationRedirectTo(obj: AddressDO, postData: PostData<Address>, event: RestButtonEvent): String {
        return "/${NextMigration.listUrl("address")}"
    }

    /**
     * Required by the base class, but not offered anywhere: the address list is the one of projectforge-next.
     */
    override fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        userAccess: UILayout.UserAccess,
    ) {
        agGridSupport.prepareUIGrid4ListPage(request, layout, magicFilter, this, userAccess = userAccess)
            .add(lc, "name", "firstName", "organization", "email")
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
            "file.upload.dropArea",
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
                    url = "${NextMigration.listUrl("address")}/${dto.id}",
                    type = MenuItemTargetType.REDIRECT,
                )
            )
            layout.add(
                MenuItem(
                    "address.vCardSingleExport",
                    i18nKey = "address.book.vCardSingleExport",
                    url = RestResolver.getRestUrl(AddressServicesRest::class.java, "exportVCard/${dto.id}"),
                    type = MenuItemTargetType.DOWNLOAD
                )
            )
            layout.add(
                MenuItem(
                    "address.directCall",
                    i18nKey = "address.directCall.call",
                    url = "next/phoneCall?addressId=${dto.id}&callerPage=addressList",
                    type = MenuItemTargetType.REDIRECT
                )
            )
        }
        return LayoutUtils.processEditPage(layout, dto, this)
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

}

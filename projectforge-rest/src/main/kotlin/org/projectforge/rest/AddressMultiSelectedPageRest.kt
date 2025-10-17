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

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressStatus
import org.projectforge.business.address.AddressbookDao
import org.projectforge.business.address.ContactStatus
import org.projectforge.business.address.PersonalAddressDao
import org.projectforge.business.address.PersonalAddressDO
import org.projectforge.common.logging.LogEventLoggerNameMatcher
import org.projectforge.common.logging.LogSubscription
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.multiselect.*
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable
import java.util.*

/**
 * Mass update after selection for addresses.
 */
@RestController
@RequestMapping("${Rest.URL}/address${AbstractMultiSelectedPage.URL_SUFFIX_SELECTED}")
class AddressMultiSelectedPageRest : AbstractMultiSelectedPage<AddressDO>() {
    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var addressPagesRest: AddressPagesRest

    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    @Autowired
    private lateinit var personalAddressDao: PersonalAddressDao

    @Autowired
    private lateinit var addressServicesRest: AddressServicesRest

    override val layoutContext: LayoutContext = LayoutContext(AddressDO::class.java)

    override fun getTitleKey(): String {
        return "address.multiselected.title"
    }

    override val listPageUrl: String = "/${MenuItemDefId.ADDRESS_LIST.url}"

    @PostConstruct
    private fun postConstruct() {
        pagesRest = addressPagesRest
    }

    override fun fillForm(
        request: HttpServletRequest,
        layout: UILayout,
        massUpdateData: MutableMap<String, MassUpdateParameter>,
        selectedIds: Collection<Serializable>?,
        variables: MutableMap<String, Any>,
    ) {
        val addresses = addressDao.select(selectedIds)
        val numberOfAddresses = addresses?.size ?: 0

        layout.add(
            UIAlert(
                "'${translate("address.multiselected.info")} $numberOfAddresses",
                color = UIColor.INFO,
                markdown = true
            )
        )

        // Status fields - manually created to ensure they're optional in mass update context
        val addressStatus = UISelect(
            "addressStatus.textValue", layoutContext,
            label = "address.addressStatus",
            values = AddressStatus.values().map { UISelectValue(it.name, translate(it.i18nKey)) }
        )
        layout.add(createInputFieldRow("addressStatus", addressStatus, massUpdateData))

        val contactStatus = UISelect(
            "contactStatus.textValue", layoutContext,
            label = "address.contactStatus",
            values = ContactStatus.values().map { UISelectValue(it.name, translate(it.i18nKey)) }
        )
        layout.add(createInputFieldRow("contactStatus", contactStatus, massUpdateData))

        // Addressbooks
        layout.add(
            createInputFieldRow(
                "addressbookList",
                UISelect<Long>(
                    "addressbookList.id", layoutContext,
                    label = "address.addressbooks",
                    multi = true,
                    autoCompletion = AutoCompletion<Int>(
                        url = AutoCompletion.getAutoCompletionUrl("addressBook"),
                        type = AutoCompletion.Type.USER.name
                    )
                ),
                massUpdateData,
                myOptions = listOf(
                    UICheckbox(
                        "addressbookList.append",
                        label = "massUpdate.field.checkbox4appending",
                        tooltip = "massUpdate.field.checkbox4appending.info",
                    )
                )
            )
        )

        // Favorite
        layout.add(
            createInputFieldRow(
                "isFavoriteCard",
                UICheckbox("isFavoriteCard.booleanValue", label = "favorite"),
                massUpdateData,
                showDeleteOption = false,
            )
        )

        // Communication language
        val communicationLanguage = UISelect(
            "communicationLanguage.textValue", layoutContext,
            label = "address.communicationLanguage",
            values = addressServicesRest.getUsedLanguages().map { UISelectValue(it.value, it.label) },
            autoCompletion = AutoCompletion<String>(url = "address/acLang?search=:search")
        )
        layout.add(createInputFieldRow("communicationLanguage", communicationLanguage, massUpdateData))

        // Organization, division, website - hide delete and replace options
        createAndAddFields(
            layoutContext,
            massUpdateData,
            layout,
            "organization",
            "division",
            "website",
            showDeleteOption = false,
            showReplaceOption = false,
        )

        // Business address fields
        layout.add(UIFieldset(title = "address.heading.businessAddress"))
        createAndAddFields(
            layoutContext,
            massUpdateData,
            layout,
            "addressText",
            "addressText2",
            "zipCode",
            "city",
            "country",
            "state",
            showAppendOption = false,
            showDeleteOption = false,
            showReplaceOption = false,
        )

        // Postal address fields
        layout.add(UIFieldset(title = "address.heading.postalAddress"))
        createAndAddFields(
            layoutContext,
            massUpdateData,
            layout,
            "postalAddressText",
            "postalAddressText2",
            "postalZipCode",
            "postalCity",
            "postalCountry",
            "postalState",
            showAppendOption = false,
            showDeleteOption = false,
            showReplaceOption = false,
        )

        // Comment
        createAndAddFields(
            layoutContext,
            massUpdateData,
            layout,
            "comment",
            minLengthOfTextArea = 1001,
        )
    }

    override fun checkParamHasAction(
        params: Map<String, MassUpdateParameter>,
        param: MassUpdateParameter,
        field: String,
    ): Boolean {
        if (field == "addressbookList") {
            return param.append == true && !param.id.toString().isNullOrBlank()
        }
        return super.checkParamHasAction(params, param, field)
    }

    override fun proceedMassUpdate(
        request: HttpServletRequest,
        selectedIds: Collection<Serializable>,
        massUpdateContext: MassUpdateContext<AddressDO>,
    ): ResponseEntity<*>? {
        val addresses = addressDao.select(selectedIds)
        if (addresses.isNullOrEmpty()) {
            return null
        }
        val params = massUpdateContext.massUpdateParams

        addresses.forEach { address ->
            massUpdateContext.startUpdate(address)

            // Status fields
            params["addressStatus"]?.let { param ->
                if (param.delete == true) {
                    address.addressStatus = AddressStatus.UPTODATE // Default value
                } else {
                    param.textValue?.let { textValue ->
                        address.addressStatus = AddressStatus.valueOf(textValue)
                    }
                }
            }

            params["contactStatus"]?.let { param ->
                if (param.delete == true) {
                    address.contactStatus = ContactStatus.ACTIVE // Default value
                } else {
                    param.textValue?.let { textValue ->
                        address.contactStatus = ContactStatus.valueOf(textValue)
                    }
                }
            }

            // Addressbooks
            params["addressbookList"]?.let { param ->
                if (param.append == true) {
                    param.id?.let { addressbookId ->
                        val addressbook = addressbookDao.find(addressbookId, checkAccess = false)
                        addressbook?.let {
                            if (address.addressbookList == null) {
                                address.addressbookList = mutableSetOf()
                            }
                            if (!address.addressbookList!!.any { it.id == addressbookId }) {
                                address.add(addressbook)
                            }
                        }
                    }
                } else if (param.delete == true) {
                    param.id?.let { addressbookId ->
                        address.addressbookList?.removeIf { it.id == addressbookId }
                    }
                } else {
                    // Do nothing, as only append and delete are supported here
                }
            }

            // Communication language
            params["communicationLanguage"]?.let { param ->
                if (param.delete == true) {
                    address.communicationLanguage = null
                } else {
                    param.textValue?.let { textValue ->
                        address.communicationLanguage = Locale.forLanguageTag(textValue)
                    }
                }
            }

            // Text fields
            TextFieldModification.processTextParameter(address, "organization", params)
            TextFieldModification.processTextParameter(address, "division", params)
            TextFieldModification.processTextParameter(address, "website", params)

            // Business address fields
            TextFieldModification.processTextParameter(address, "addressText", params)
            TextFieldModification.processTextParameter(address, "addressText2", params)
            TextFieldModification.processTextParameter(address, "zipCode", params)
            TextFieldModification.processTextParameter(address, "city", params)
            TextFieldModification.processTextParameter(address, "country", params)
            TextFieldModification.processTextParameter(address, "state", params)

            // Postal address fields
            TextFieldModification.processTextParameter(address, "postalAddressText", params)
            TextFieldModification.processTextParameter(address, "postalAddressText2", params)
            TextFieldModification.processTextParameter(address, "postalZipCode", params)
            TextFieldModification.processTextParameter(address, "postalCity", params)
            TextFieldModification.processTextParameter(address, "postalCountry", params)
            TextFieldModification.processTextParameter(address, "postalState", params)

            // Comment
            TextFieldModification.processTextParameter(address, "comment", params)

            massUpdateContext.commitUpdate(
                identifier4Message = "${address.name ?: ""} ${address.firstName ?: ""} ${address.organization ?: ""}".trim(),
                address,
                update = { addressDao.update(address) },
            )

            // Update favorite status separately through PersonalAddressDao
            params["isFavoriteCard"]?.let { param ->
                val personalAddress = PersonalAddressDO()
                personalAddress.address = address
                personalAddress.isFavoriteCard = param.booleanValue ?: false
                personalAddressDao.setOwner(personalAddress, ThreadLocalUserContext.requiredLoggedInUserId)
                personalAddressDao.saveOrUpdate(personalAddress)
            }
        }
        return null
    }

    override fun ensureUserLogSubscription(): LogSubscription {
        val username = ThreadLocalUserContext.loggedInUser!!.username ?: throw InternalError("User not given")
        val displayTitle = translate("address.multiselected.title")
        return LogSubscription.ensureSubscription(
            title = "Addresses",
            displayTitle = displayTitle,
            user = username,
            create = { title, user ->
                LogSubscription(
                    title,
                    user,
                    LogEventLoggerNameMatcher(
                        "org.projectforge.business.address.AddressDao",
                        "org.projectforge.framework.persistence.api.BaseDaoSupport|AddressDO"
                    ),
                    maxSize = 10000,
                    displayTitle = displayTitle
                )
            })
    }

    override fun customizeExcelIdentifierHeadCells(): Array<String> {
        return arrayOf("${translate("name")}|20", "${translate("firstName")}|20", "${translate("organization")}|30")
    }

    override fun getExcelIdentifierCells(massUpdateObject: MassUpdateObject<AddressDO>): List<Any?> {
        val address = massUpdateObject.modifiedObj
        return listOf(address!!.name, address.firstName, address.organization)
    }
}

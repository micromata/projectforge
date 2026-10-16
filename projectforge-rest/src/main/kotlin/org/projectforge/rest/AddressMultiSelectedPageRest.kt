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

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressStatus
import org.projectforge.business.address.ContactStatus
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
    private lateinit var addressEntityRest: AddressEntityRest

    @Autowired
    private lateinit var addressServicesRest: AddressServicesRest

    override val layoutContext: LayoutContext = LayoutContext(AddressDO::class.java)

    override val listPageUrl: String = "/${MenuItemDefId.ADDRESS_LIST.url}"

    @PostConstruct
    private fun postConstruct() {
        pagesRest = addressEntityRest
    }

    /**
     * The layout-free field set for projectforge-next, which renders the form itself. The communication language
     * offers the languages already in use (as the address form does).
     */
    override fun fieldDeclarations(): List<MassUpdateFieldDeclaration> {
        return listOf(
            MassUpdateFieldDeclaration("addressStatus", showDeleteOption = false),
            MassUpdateFieldDeclaration("contactStatus", showDeleteOption = false),
            MassUpdateFieldDeclaration(
                "communicationLanguage",
                showDeleteOption = true,
                values = addressServicesRest.getUsedLanguages().map { UISelectValue(it.value, it.label) },
            ),
            MassUpdateFieldDeclaration("organization", showDeleteOption = true, showReplaceOption = false),
            MassUpdateFieldDeclaration("division", showDeleteOption = true, showReplaceOption = false),
            MassUpdateFieldDeclaration("website", showDeleteOption = true, showReplaceOption = false),
            // Business address
            *addressFields("addressText", "addressText2", "zipCode", "city", "country", "state"),
            // Postal address
            *addressFields(
                "postalAddressText", "postalAddressText2", "postalZipCode", "postalCity", "postalCountry", "postalState",
            ),
            MassUpdateFieldDeclaration("comment", minLengthOfTextArea = 1001),
        )
    }

    private fun addressFields(vararg fields: String): Array<MassUpdateFieldDeclaration> {
        return fields.map {
            MassUpdateFieldDeclaration(it, showDeleteOption = true, showReplaceOption = false)
        }.toTypedArray()
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
            /*params["isFavoriteCard"]?.let { param ->
                val personalAddress = PersonalAddressDO()
                personalAddress.address = address
                personalAddress.isFavoriteCard = param.booleanValue ?: false
                personalAddressDao.setOwner(personalAddress, ThreadLocalUserContext.requiredLoggedInUserId)
                personalAddressDao.saveOrUpdate(personalAddress)
            }*/
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

/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.PersonalAddressDO
import org.projectforge.business.address.PersonalAddressDao
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.Address
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.sms.SmsSenderConfig
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/addressView")
class AddressViewPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var addressDao: AddressDao

  @Autowired
  private lateinit var configurationService: ConfigurationService

  @Autowired
  private lateinit var personalAddressDao: PersonalAddressDao

  @Autowired
  private lateinit var smsSenderConfig: SmsSenderConfig

  enum class PhoneType { BUSINESS, MOBILE, PRIVATE, PRIVATE_MOBILE }

  class PhoneNumber(
    var addressId: Int,
    var number: String?,
    var phoneCallEnabled: Boolean,
    var phoneType: PhoneType,
    var sms: Boolean,
    var smsEnabled: Boolean
  )

  class EMail(var email: String?)

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest, @RequestParam("id") idString: String?): FormLayoutData {
    val id = NumberHelper.parseInteger(idString) ?: throw IllegalArgumentException("id not given.")
    val addressDO = addressDao.getById(id) ?: AddressDO()
    val address = Address()
    address.copyFrom(addressDO)
    address.isFavoriteCard = personalAddressDao.getByAddressId(id)?.isFavoriteCard ?: false

    val layout = UILayout("address.view.title")
    val organization = if (address.organization.isNullOrBlank()) {
      ""
    } else {
      " - ${address.organization}"
    }
    val fieldSet = UIFieldset(12, title = "'${addressDO.fullNameWithTitleAndForm}$organization")
    layout.add(fieldSet)
    if (addressDO.image == true) {
      fieldSet.add(
        UICustomized(
          "image",
          mutableMapOf("src" to "address/image/${address.id}", "alt" to addressDO.fullNameWithTitleAndForm)
        )
      )
    }
    var row = UIRow()
    fieldSet.add(row)

    val col = UIFieldset(12, "address.phoneNumbers")
    row.add(col)

    val phoneCallEnabled = configurationService.isTelephoneSystemUrlConfigured
    val smsEnabled = smsSenderConfig.isSmsConfigured()

    addPhoneNumber(
      col,
      "address.phoneType.business",
      PhoneNumber(id, address.businessPhone, phoneCallEnabled, PhoneType.BUSINESS, false, smsEnabled)
    )
    addPhoneNumber(
      col,
      "address.phoneType.mobile",
      PhoneNumber(id, address.mobilePhone, phoneCallEnabled, PhoneType.MOBILE, true, smsEnabled)
    )
    addPhoneNumber(
      col,
      "address.phoneType.private",
      PhoneNumber(id, address.privatePhone, phoneCallEnabled, PhoneType.PRIVATE, false, smsEnabled)
    )
    addPhoneNumber(
      col,
      "address.phoneType.mobile",
      PhoneNumber(id, address.privateMobilePhone, phoneCallEnabled, PhoneType.PRIVATE_MOBILE, true, smsEnabled)
    )

    val emailsCol = UIFieldset(12, "address.emails")
    row.add(emailsCol)
    addEMail(emailsCol, "address.business", address.email)
    addEMail(emailsCol, "address.private", address.privateEmail)

    row = UIRow()
    fieldSet.add(row)
    var numberOfAddresses = 0
    if (addressDO.hasDefaultAddress()) ++numberOfAddresses
    if (addressDO.hasPrivateAddress()) ++numberOfAddresses
    if (addressDO.hasPostalAddress()) ++numberOfAddresses

    if (addressDO.hasDefaultAddress()) {
      createAddressCol(
        row,
        numberOfAddresses,
        "address.heading.businessAddress",
        address.addressText,
        address.addressText2,
        address.zipCode,
        address.city,
        address.state,
        address.country
      )
    }
    if (addressDO.hasPrivateAddress()) {
      createAddressCol(
        row,
        numberOfAddresses,
        "address.heading.privateAddress",
        address.privateAddressText,
        address.privateAddressText2,
        address.privateZipCode,
        address.privateCity,
        address.privateState,
        address.privateCountry
      )
    }
    if (addressDO.hasPostalAddress()) {
      createAddressCol(
        row,
        numberOfAddresses,
        "address.heading.postalAddress",
        address.postalAddressText,
        address.postalAddressText2,
        address.postalZipCode,
        address.postalCity,
        address.postalState,
        address.postalCountry
      )
    }
    if (!address.comment.isNullOrBlank()) {
      row.add(
        UIFieldset(12, "comment")
          .add(UILabel("'${address.comment}"))
      )
    }

    fieldSet.add(
      UICheckbox(
        "isFavoriteCard",
        label = "favorite",
        tooltip = "address.favorites.info",
      )
    )

    layout.add(
      UIButton.createBackButton(
        responseAction = ResponseAction(
          PagesResolver.getListPageUrl(AddressPagesRest::class.java, absolute = true),
          targetType = TargetType.REDIRECT
        ),
        default = true
      )
    )

    layout.add(
      MenuItem(
        "EDIT",
        i18nKey = "address.title.edit",
        url = PagesResolver.getEditPageUrl(AddressPagesRest::class.java, address.id),
        type = MenuItemTargetType.REDIRECT
      )
    )
    layout.watchFields.addAll(arrayOf("isFavoriteCard"))
    LayoutUtils.process(layout)
    layout.postProcessPageMenu()
    return FormLayoutData(address, layout, createServerData(request))
  }

  /**
   * Will be called, if the user wants to change his/her observeStatus.
   */
  @PostMapping(RestPaths.WATCH_FIELDS)
  fun watchFields(@Valid @RequestBody postData: PostData<Address>): ResponseEntity<ResponseAction> {
    val id = postData.data.id ?: throw IllegalAccessException("Parameter id not given.")
    val favorite = postData.data.isFavoriteCard
    val address = addressDao.getById(id) ?: throw IllegalAccessException("Address not found.")
    val owner = ThreadLocalUserContext.getUser()
    val personalAddress = personalAddressDao.getByAddressId(id, owner) ?: PersonalAddressDO()
    if (personalAddress.id == null) {
      if (!favorite) {
        // Nothing to-do: favorite is false and now personal address entry found. Shouldn't really occur.
        return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
      }
      personalAddress.address = address
      personalAddressDao.setOwner(personalAddress, owner.id) // Set current logged in user as owner.
      personalAddress.isFavoriteCard = true
      personalAddressDao.saveOrUpdate(personalAddress)
    } else if (favorite != personalAddress.isFavorite) {
      // Update required:
      personalAddress.isFavoriteCard = favorite
      personalAddressDao.saveOrUpdate(personalAddress)
    }
    return ResponseEntity.ok(ResponseAction(targetType = TargetType.UPDATE).addVariable("data", postData.data))
  }


  private fun addPhoneNumber(col: UICol, title: String, number: PhoneNumber) {
    if (number.number.isNullOrBlank()) {
      return
    }
    val phoneNumber = PhoneNumber(
      number.addressId,
      number.number,
      number.phoneCallEnabled,
      number.phoneType,
      number.sms,
      number.smsEnabled
    )
    col.add(
      UIRow()
        .add(UICol(6).add(UILabel(title)))
        .add(UICol(6).add(UICustomized("address.phoneNumber", mutableMapOf("data" to phoneNumber))))
    )
  }

  private fun addEMail(col: UICol, title: String, email: String?) {
    if (email.isNullOrBlank()) {
      return
    }
    col.add(
      UIRow()
        .add(UICol(6).add(UILabel(title)))
        .add(UICol(6).add(UICustomized("email", mutableMapOf("data" to EMail(email)))))
    )
  }

  private fun createAddressCol(
    row: UIRow,
    numberOfAddresses: Int,
    title: String,
    addressText: String?,
    addressText2: String?,
    zipCode: String?,
    city: String?,
    state: String?,
    country: String?
  ) {
    row.add(
      UIFieldset(UILength(md = 12 / numberOfAddresses), title = title)
        .add(
          UICustomized(
            "address.view",
            mutableMapOf(
              "address" to (addressText ?: ""),
              "address2" to (addressText2 ?: ""),
              "zipCode" to (zipCode ?: ""),
              "city" to (city ?: ""),
              "state" to (state ?: ""),
              "country" to (country ?: "")
            )
          )
        )
    )
  }
}

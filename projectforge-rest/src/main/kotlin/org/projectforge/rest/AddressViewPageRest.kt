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

import mu.KotlinLogging
import org.projectforge.business.sipgate.SipgateConfiguration
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
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.projectforge.business.address.*
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.utils.MarkdownBuilder

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/addressView")
class AddressViewPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var addressDao: AddressDao

  @Autowired
  private lateinit var addressImageCache: AddressImageCache

  @Autowired
  private lateinit var personalAddressDao: PersonalAddressDao

  @Autowired
  private lateinit var sipgateConfiguration: SipgateConfiguration

  @Autowired
  private lateinit var smsSenderConfig: SmsSenderConfig

  enum class PhoneType { BUSINESS, MOBILE, PRIVATE, PRIVATE_MOBILE }

  class PhoneNumber(
    var addressId: Long,
    var number: String?,
    var phoneCallEnabled: Boolean,
    var phoneType: PhoneType,
    var sms: Boolean,
    var smsEnabled: Boolean
  )

  class EMail(var email: String?)

  @GetMapping("dynamic")
  fun getForm(
    request: HttpServletRequest,
    @RequestParam("id") idString: String?,
    @RequestParam("returnToCaller") returnToCaller: String?,
  ): FormLayoutData {
    val id = NumberHelper.parseLong(idString) ?: throw IllegalArgumentException("id not given.")
    val addressDO = addressDao.find(id) ?: AddressDO()
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
    val image = addressImageCache.getImage(id)
    if (image != null) {
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

    val phoneCallEnabled = sipgateConfiguration.isConfigured()
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

    val statusCol = UIFieldset(12)
    row.add(statusCol)
    statusCol.add(
      UIRow()
        .add(UICol(6).add(UILabel("address.addressStatus")))
        .add(UICol(6).add(UILabel("'${addressDO.addressStatus.i18nKey?.let { translate(it) } ?: addressDO.addressStatus.name}")))
    )
    statusCol.add(
      UIRow()
        .add(UICol(6).add(UILabel("address.contactStatus")))
        .add(UICol(6).add(UILabel("'${addressDO.contactStatus.i18nKey?.let { translate(it) } ?: addressDO.contactStatus.name}")))
    )

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

    val backUrl = if (returnToCaller.isNullOrEmpty()) {
      PagesResolver.getListPageUrl(AddressPagesRest::class.java, absolute = true)
    } else {
      // Fix doubled encoding:
      returnToCaller.replace("%2F", "/")
    }
    layout.add(
      UIButton.createBackButton(
        responseAction = ResponseAction(
          backUrl,
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
    val formLayoutData = FormLayoutData(address, layout, createServerData(request))
    returnToCaller?.let {
      // Fix doubled encoding:
      formLayoutData.serverData!!.returnToCaller = backUrl
    }
    return formLayoutData
  }

  /**
   * Will be called, if the user wants to change his/her observeStatus.
   */
  @PostMapping(RestPaths.WATCH_FIELDS)
  fun watchFields(@Valid @RequestBody postData: PostData<Address>): ResponseEntity<ResponseAction> {
    val id = postData.data.id ?: throw IllegalAccessException("Parameter id not given.")
    val favorite = postData.data.isFavoriteCard
    val address = addressDao.find(id) ?: throw IllegalAccessException("Address not found.")
    val owner = ThreadLocalUserContext.loggedInUser!!
    val personalAddress = personalAddressDao.getByAddressId(id, owner) ?: PersonalAddressDO()
    if (personalAddress.id == null) {
      if (!favorite) {
        // Nothing to-do: favorite is false and now personal address entry found. Shouldn't really occur.
        return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
      }
      personalAddress.address = address
      personalAddressDao.setOwner(personalAddress, owner.id!!) // Set current logged in user as owner.
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
    // Build MailingAddress and use its formatted output
    val mailingAddress = MailingAddress(
      addressText = addressText,
      addressText2 = addressText2,
      zipCode = zipCode,
      city = city,
      state = state,
      country = country
    )

    // Use MarkdownBuilder to format the address
    val addressCard = MarkdownBuilder()
    addressCard.appendMultilineText(mailingAddress.formattedAddress)

    row.add(
      UIFieldset(UILength(md = 12 / numberOfAddresses), title = title)
        .add(
          UIAlert(
            message = addressCard.toString(),
            color = UIColor.LIGHT,
            markdown = true
          )
        )
    )
  }

  companion object {
    @JvmStatic
    @JvmOverloads
    fun getPageUrl(id: Long?, returnToCaller: String? = null, absolute: Boolean = true): String {
      return PagesResolver.getDynamicPageUrl(
        AddressViewPageRest::class.java,
        id = id,
        absolute = absolute,
        returnToCaller = returnToCaller
      )
    }
  }
}

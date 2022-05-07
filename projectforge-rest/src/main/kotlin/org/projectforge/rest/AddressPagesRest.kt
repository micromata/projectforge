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
import org.apache.commons.lang3.StringUtils
import org.projectforge.SystemStatus
import org.projectforge.business.address.*
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.image.ImageService
import org.projectforge.common.FormatterUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.getUserId
import org.projectforge.framework.time.DateHelper
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
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
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/address")
class AddressPagesRest
  : AbstractDTOPagesRest<AddressDO, Address, AddressDao>(
  AddressDao::class.java,
  i18nKeyPrefix = "address.title",
  cloneSupport = CloneSupport.CLONE
) {

  /**
   * For exporting list of addresses.
   */
  private class ListAddress(
    val address: Address,
    val id: Int, // Needed for history Service
    val deleted: Boolean,
    var imageUrl: String? = null,
    var previewImageUrl: String? = null
  )

  @Autowired
  private lateinit var addressbookDao: AddressbookDao

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
  private lateinit var smsSenderConfig: SmsSenderConfig

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
    address.addressbookList = mutableSetOf()
    address.addressbookList?.add(addressbookDao.globalAddressbook)
    return address
  }

  override fun onGetItemAndLayout(request: HttpServletRequest, dto: Address, formLayoutData: FormLayoutData) {
    ExpiringSessionAttributes.removeAttribute(request.getSession(false), SESSION_IMAGE_ATTR)
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
    elements.add(UIFilterElement("doublets", UIFilterElement.FilterType.BOOLEAN, translate("address.filter.doublets")))
    elements.add(UIFilterElement("images", UIFilterElement.FilterType.BOOLEAN, translate("address.filter.images")))
  }

  override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<AddressDO>>? {
    val doubletFilterEntry = source.entries.find { it.field == "doublets" }
    doubletFilterEntry?.synthetic = true
    val myFavoritesFilterEntry = source.entries.find { it.field == "myFavorites" }
    myFavoritesFilterEntry?.synthetic = true
    val imagesFilterEntry = source.entries.find { it.field == "images" }
    imagesFilterEntry?.synthetic = true
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
    val address = baseDao.getOrLoad(obj.id)
    val personalAddress = PersonalAddressDO()
    personalAddress.address = address
    personalAddress.isFavoriteCard = dto.isFavoriteCard
    personalAddressDao.setOwner(personalAddress, getUserId()) // Set current logged in user as owner.
    personalAddressDao.saveOrUpdate(personalAddress)

    val session = request.getSession(false)
    val bytes = ExpiringSessionAttributes.getAttribute(session, SESSION_IMAGE_ATTR)
    if (bytes != null && bytes is ByteArray) {
      // The user uploaded an image, so
      addressImageDao.saveOrUpdate(obj.id, bytes)
      ExpiringSessionAttributes.removeAttribute(session, SESSION_IMAGE_ATTR)
    }
  }

  override val classicsLinkListUrl: String?
    get() = "wa/addressList"

  /**
   * @return the address view page.
   */
  override fun getStandardEditPage(): String {
    return "${PagesResolver.getDynamicPageUrl(AddressViewPageRest::class.java)}:id"
  }

  /**
   * LAYOUT List page
   */
  override fun createListLayout(request: HttpServletRequest, magicFilter: MagicFilter): UILayout {
    val addressLC = LayoutContext(lc)
    addressLC.idPrefix = "address."
    val layout = super.createListLayout(request, magicFilter)
      .add(
        UITable.createUIResultSetTable()
          .add(addressLC, "isFavoriteCard", "lastUpdate")
          .add(UITableColumn("address.imagePreview", "address.image", dataType = UIDataType.CUSTOMIZED))
          .add(addressLC, "name", "firstName", "organization", "email")
          .add(
            UITableColumn(
              "address.phoneNumbers",
              "address.phoneNumbers",
              dataType = UIDataType.CUSTOMIZED,
              sortable = false
            )
          )
          .add(lc, "address.addressbookList")
      )
    layout.getTableColumnById("address.lastUpdate").formatter = UITableColumn.Formatter.DATE
    layout.getTableColumnById("address.imagePreview").set(sortable = false)
    layout.getTableColumnById("address.addressbookList")
      .set(formatter = UITableColumn.Formatter.ADDRESS_BOOK, sortable = false)
    layout.getTableColumnById("address.isFavoriteCard").set(
      sortable = false,
      title = "address.columnHead.myFavorites",
      tooltip = "address.filter.myFavorites"
    )
      .valueIconMap = mapOf(true to UIIconType.STAR_REGULAR)
    var menuIndex = 0
    if (configurationService.isTelephoneSystemUrlConfigured) {
      layout.add(MenuItem("address.phoneCall", i18nKey = "menu.phoneCall", url = "wa/phoneCall"), menuIndex++)
    }
    if (smsSenderConfig.isSmsConfigured() || SystemStatus.isDevelopmentMode()) {
      val sendSmsUrl = if (SystemStatus.isDevelopmentMode()) {
        log.warn("********** React version of texting messages is only available in development mode.")
        PagesResolver.getDynamicPageUrl(SendTextMessagePageRest::class.java)
      } else {
        "wa/sendSms"
      }
      layout.add(
        MenuItem(
          "address.writeSMS",
          i18nKey = "address.tooltip.writeSMS",
          url = sendSmsUrl
        ),
        menuIndex++
      )
    }
    val exportMenu = MenuItem("address.export", i18nKey = "export")
    if (configurationService.isDAVServicesAvailable) {
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
    layout.add(exportMenu, menuIndex)
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
    return LayoutUtils.processListPage(layout, this)
  }

  override val autoCompleteSearchFields = arrayOf("name", "firstName", "organization", "city", "privateCity")

  override fun addVariablesForListPage(): Map<String, Any>? {
    return mutableMapOf(
      "phoneCallEnabled" to configurationService.isTelephoneSystemUrlConfigured,
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
                          UISelect<Int>(
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
              .add(UIInput("postalAddressText", lc, ignoreAdditionalLabel = true).enableAutoCompletion(this))
              .add(UIInput("postalAddressText2", lc, ignoreAdditionalLabel = true).enableAutoCompletion(this))
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
              .add(UIInput("privateAddressText", lc, ignoreAdditionalLabel = true).enableAutoCompletion(this))
              .add(UIInput("privateAddressText2", lc, ignoreAdditionalLabel = true).enableAutoCompletion(this))
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
    layout.addTranslations("delete", "file.upload.dropArea")
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
          url = "wa/phoneCall?addressId=${dto.id}",
          type = MenuItemTargetType.REDIRECT
        )
      )
    }
    return LayoutUtils.processEditPage(layout, dto, this)
  }

  /**
   * @return New result set of dto's, transformed from data base objects.
   */
  override fun processResultSetBeforeExport(
    resultSet: ResultSet<AddressDO>,
    request: HttpServletRequest,
    magicFilter: MagicFilter,
  ): ResultSet<*> {
    val newList = resultSet.resultSet.map {
      ListAddress(
        transformFromDB(it),
        id = it.id,
        deleted = it.isDeleted,
        imageUrl = if (it.image == true) "address/image/${it.id}" else null,
        previewImageUrl = if (it.image == true) "address/imagePreview/${it.id}" else null
      )
    }
    newList.forEach {
      it.address.imageData = null
      it.address.imageDataPreview = null
    }
    return ResultSet(newList, resultSet, newList.size, magicFilter = magicFilter)
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
  @PostMapping("exportAsExcel")
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
}

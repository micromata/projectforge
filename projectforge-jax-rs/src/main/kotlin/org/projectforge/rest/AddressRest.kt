package org.projectforge.rest

import org.apache.commons.lang3.StringUtils
import org.projectforge.business.address.*
import org.projectforge.business.image.ImageService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.AddressImageServicesRest.Companion.SESSION_IMAGE_ATTR
import org.projectforge.rest.core.AbstractStandardRest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.json.LabelValueTypeAdapter
import org.projectforge.sms.SmsSenderConfig
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Path


@Component
@Path("address")
class AddressRest()
    : AbstractStandardRest<AddressDO, AddressDao, AddressFilter>(AddressDao::class.java, AddressFilter::class.java, "address.title") {

    /**
     * For exporting list of addresses.
     */
    private class Address(val address: AddressDO,
                          val id: Int, // Needed for history Service
                          var imageUrl: String? = null,
                          var previewImageUrl: String? = null)

    private val addressBookTypeAdapter = LabelValueTypeAdapter<AddressbookDO>("id", "title")

    init {
        restHelper.add(AddressbookDO::class.java, addressBookTypeAdapter)
    }

    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    @Autowired
    private lateinit var imageService: ImageService

    @Autowired
    private lateinit var smsSenderConfig: SmsSenderConfig


    /**
     * Initializes new books for adding.
     */
    override fun newBaseDO(request: HttpServletRequest): AddressDO {
        val address = super.newBaseDO(request)
        address.addressStatus = AddressStatus.UPTODATE
        address.contactStatus = ContactStatus.ACTIVE
        address.addressbookList = mutableSetOf()
        address.addressbookList?.add(addressbookDao.globalAddressbook)
        return address
    }

    override fun onGetItemAndLayout(request: HttpServletRequest, item: AddressDO, editLayoutData: EditLayoutData) {
        ExpiringSessionAttributes.removeAttribute(request.session, SESSION_IMAGE_ATTR)
    }

    // TODO Menus: print view, ical export, direct call: see AddressEditPage
    // TODO: onSaveOrUpdate: see AddressEditPage

    /**
     * Clone is supported by addresses.
     */
    override fun prepareClone(obj: AddressDO): Boolean {
        // TODO: Enter here the PersonalAddressDO stuff etc.
        return true
    }

    override fun validate(validationErrors: MutableList<ValidationError>, obj: AddressDO) {
        if (StringUtils.isAllBlank(obj.name, obj.firstName, obj.organization)) {
            validationErrors.add(ValidationError(translate("address.form.error.toFewFields"), fieldId = "name"))
        }
        if (obj.addressbookList.isNullOrEmpty()) {
            validationErrors.add(ValidationError(translateMsg("validation.error.fieldRequired",
                    translate("address.addressbooks")), fieldId = "addressbooks"))
        }
    }

    override fun beforeSaveOrUpdate(request: HttpServletRequest, obj: AddressDO) {
        val session = request.session
        val bytes = ExpiringSessionAttributes.getAttribute(session, SESSION_IMAGE_ATTR)
        if (bytes != null && bytes is ByteArray) {
            obj.imageData = bytes
            obj.imageDataPreview = imageService.resizeImage(bytes)
            ExpiringSessionAttributes.removeAttribute(session, SESSION_IMAGE_ATTR)
        } else {
            if (obj.imageData != null) {
                val dbAddress = baseDao.getById(obj.id)
                obj.imageData = dbAddress.imageData
                obj.imageDataPreview = dbAddress.imageDataPreview
            } else {
                obj.imageDataPreview = null
            }
        }
    }

    override fun afterSaveOrUpdate(obj: AddressDO) {
        // TODO: see AddressEditPage
        val address = baseDao.getOrLoad(obj.getId())
        //val personalAddress = form.addressEditSupport.personalAddress
        //personalAddress.setAddress(address)
        //personalAddressDao.setOwner(personalAddress, getUserId()) // Set current logged in user as owner.
        //personalAddressDao.saveOrUpdate(personalAddress)
        //return null
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val addressLC = LayoutContext(lc)
        addressLC.idPrefix = "address."
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(addressLC, "lastUpdate")
                        .add(UITableColumn("address.imagePreview", "address.image", dataType = UIDataType.CUSTOMIZED))
                        .add(addressLC, "name", "firstName", "organization", "email")
                        .add(UITableColumn("phoneNumbers", "address.phoneNumbers", dataType = UIDataType.CUSTOMIZED))
                        .add(lc, "addressbookList"))
        layout.getTableColumnById("address.lastUpdate").formatter = Formatter.DATE
        LayoutUtils.addListFilterContainer(layout,
                UICheckbox("filter", label = "filter"),
                UICheckbox("newest", label = "filter.newest"),
                UICheckbox("favorites", label = "address.filter.myFavorites"),
                UICheckbox("dublets", label = "address.filter.doublets"))
        var menuIndex = 0
        if (smsSenderConfig.isSmsConfigured()) {
            layout.add(MenuItem("address.writeSMS", i18nKey = "address.tooltip.writeSMS", url = "wa/sendSms"), menuIndex++)
        }
        val exportMenu = MenuItem("address.export", i18nKey = "export")
        exportMenu.add(MenuItem("address.vCardExport",
                i18nKey = "address.book.vCardExport",
                url = "${getRestPath()}/exportFavoritesVCards",
                tooltip = "address.book.vCardExport.tooltip.content",
                tooltipTitle = "address.book.vCardExport.tooltip.title",
                type = MenuItemTargetType.DOWNLOAD))
        exportMenu.add(MenuItem("address.export",
                i18nKey = "address.book.export",
                url = "${getRestPath()}/exportAsExcel",
                tooltipTitle = "address.book.export",
                tooltip = "address.book.export.tooltip",
                type = MenuItemTargetType.DOWNLOAD))
        exportMenu.add(MenuItem("address.exportFavoritePhoneList",
                i18nKey = "address.book.exportFavoritePhoneList",
                url = "${getRestPath()}/exportFavoritePhoneList",
                tooltipTitle = "address.book.exportFavoritePhoneList.tooltip.title",
                tooltip = "address.book.exportFavoritePhoneList.tooltip.content",
                type = MenuItemTargetType.DOWNLOAD))
        layout.add(exportMenu, menuIndex++)
        layout.getMenuById(GEAR_MENU)?.add(MenuItem("address.exportAppleScript4Notes",
                i18nKey = "address.book.export.appleScript4Notes",
                url = "${getRestPath()}/downloadAppleScript",
                tooltipTitle = "address.book.export.appleScript4Notes",
                tooltip = "address.book.export.appleScript4Notes.tooltip",
                type = MenuItemTargetType.DOWNLOAD))
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: AddressDO): UILayout {
        val addressbookDOs = addressbookDao.allAddressbooksWithFullAccess
        val addressbooks = mutableListOf<UISelectValue<Int>>()
        addressbookDOs.forEach {
            addressbooks.add(UISelectValue(it.id, it.title))
        }
        val layout = super.createEditLayout(dataObject)
                //autoCompletion = AutoCompletion(url = "addressBook/ac?search="))))
                .add(UIRow()
                        .add(UIFieldset(6)
                                .add(UIRow()
                                        .add(UICol(length = 8)
                                                .add(UIMultiSelect("addressbookList", lc,
                                                        values = addressbooks,
                                                        labelProperty = "title",
                                                        valueProperty = "id")))
                                        .add(UICol(length = 4)
                                                .add(UICheckbox("favorite", label = "favorite")))))
                        .add(UIFieldset(6)
                                .add(UIRow()
                                        .add(UICol(length = 6)
                                                .add(lc, "addressStatus"))
                                        .add(UICol(length = 6)
                                                .add(lc, "contactStatus")))))
                .add(UIRow()
                        .add(UIFieldset(6)
                                .add(lc, "name", "firstName")
                                .add(UISelect<String>("form", lc).buildValues(FormOfAddress::class.java))
                                .add(lc, "title", "email", "privateEmail"))
                        .add(UIFieldset(6)
                                .add(lc, "birthday", "communicationLanguage", "organization", "division", "positionText", "website")))
                .add(UIFieldset(12)
                        .add(UIRow()
                                .add(UICol(6)
                                        .add(lc, "businessPhone", "mobilePhone", "fax"))
                                .add(UICol(6)
                                        .add(lc, "privatePhone", "privateMobilePhone"))))
                .add(UIRow()
                        .add(UIFieldset(6, title = "address.heading.businessAddress")
                                .add(lc, "addressText")
                                .add(UIRow()
                                        .add(UICol(length = 2)
                                                .add(UIInput("zipCode", lc)))
                                        .add(UICol(length = 10)
                                                .add(UIInput("city", lc))))
                                .add(UIRow()
                                        .add(UICol(length = 6)
                                                .add(UIInput("country", lc)))
                                        .add(UICol(length = 6)
                                                .add(UIInput("state", lc)))))
                        .add(UIFieldset(6, "address.heading.postalAddress")
                                .add(lc, "postalAddressText")
                                .add(UIRow()
                                        .add(UICol(length = 2)
                                                .add(UIInput("postalZipCode", lc)))
                                        .add(UICol(length = 10)
                                                .add(UIInput("postalCity", lc))))
                                .add(UIRow()
                                        .add(UICol(length = 6)
                                                .add(UIInput("postalCountry", lc)))
                                        .add(UICol(length = 6)
                                                .add(UIInput("postalState", lc))))))
                .add(UIFieldset()
                        .add(UIRow()
                                .add(UICol(6)
                                        .add(UILabel("address.heading.privateAddress"))
                                        .add(lc, "privateAddressText")
                                        .add(UIRow()
                                                .add(UICol(length = 2)
                                                        .add(UIInput("privateZipCode", lc)))
                                                .add(UICol(length = 10)
                                                        .add(UIInput("privateCity", lc))))
                                        .add(UIRow()
                                                .add(UICol(length = 6)
                                                        .add(UIInput("privateCountry", lc)))
                                                .add(UICol(length = 6)
                                                        .add(UIInput("privateState", lc)))))
                                .add(UICol(6)
                                        .add(UILabel("address.image"))
                                        .add(UICustomized("address.edit.image"))))
                        .add(lc, "comment"))

        layout.getInputById("name").focus = true
        layout.getTextAreaById("comment").cssClass = CssClassnames.MT_5
        layout.addTranslations("delete", "file.upload.dropArea", "address.image.upload.error")
        if (dataObject?.id != null) {
            layout.add(MenuItem("address.printView",
                    i18nKey = "printView",
                    url = "wa/addressView?id=${dataObject.id}",
                    type = MenuItemTargetType.REDIRECT))
            layout.add(MenuItem("address.vCardSingleExport",
                    i18nKey = "address.book.vCardSingleExport",
                    url = "${getRestPath()}/exportVCard/${dataObject.id}",
                    type = MenuItemTargetType.DOWNLOAD))
            layout.add(MenuItem("address.directCall",
                    i18nKey = "address.directCall.call",
                    url = "wa/phoneCall?addressId=${dataObject.id}",
                    type = MenuItemTargetType.REDIRECT))
        }
        return LayoutUtils.processEditPage(layout, dataObject)
    }

    override fun processResultSetBeforeExport(resultSet: ResultSet<Any>) {
        val list: List<Address> = resultSet.resultSet.map { it ->
            Address(it as AddressDO,
                    id = it.id,
                    imageUrl = if (it.imageData != null) "address/image/${it.id}" else null,
                    previewImageUrl = if (it.imageDataPreview != null) "address/imagePreview/${it.id}" else null)
        }
        resultSet.resultSet = list
        resultSet.resultSet.forEach { it ->
            (it as Address).address.imageData = null
            it.address.imageDataPreview = null
        }
    }

    override fun processItemBeforeExport(item: Any) {
        if ((item as AddressDO).imageData != null || item.imageDataPreview != null) {
            item.imageData = byteArrayOf(1)
            item.imageDataPreview = byteArrayOf(1)
        }
    }
}

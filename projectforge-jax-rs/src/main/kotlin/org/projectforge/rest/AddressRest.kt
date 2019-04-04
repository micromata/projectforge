package org.projectforge.rest

import com.google.gson.*
import org.apache.commons.lang3.StringUtils
import org.glassfish.jersey.media.multipart.FormDataMultiPart
import org.projectforge.business.address.*
import org.projectforge.business.image.ImageService
import org.projectforge.framework.i18n.translate
import org.projectforge.menu.MenuItem
import org.projectforge.rest.AddressImageRest.Companion.SESSION_IMAGE_ATTR
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.ResultSet
import org.projectforge.sms.SmsSenderConfig
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.InputStream
import java.lang.reflect.Type
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


@Component
@Path("address")
class AddressRest()
    : AbstractDORest<AddressDO, AddressDao, AddressFilter>(AddressDao::class.java, AddressFilter::class.java, "address.title") {

    private class Address(val address: AddressDO,
                          val id: Int,
                          var imageUrl: String? = null,
                          var previewImageUrl: String? = null)

    init {
        restHelper.add(AddressbookDO::class.java, AddressbookDOSerializer())
    }

    private val log = org.slf4j.LoggerFactory.getLogger(AddressRest::class.java)

    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    @Autowired
    private lateinit var imageService: ImageService

    @Autowired
    private lateinit var smsSenderConfig: SmsSenderConfig

    override fun onGetItemAndLayout(request: HttpServletRequest) {
        ExpiringSessionAttributes.removeAttribute(request.session, SESSION_IMAGE_ATTR)
    }

    override fun newBaseDO(): AddressDO {
        return AddressDO()
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
                        .add(UITableColumn("imageDataPreview", "address.image", dataType = UIDataType.CUSTOMIZED))
                        .add(addressLC, "name", "firstName", "organization", "email")
                        .add(UITableColumn("phoneNumbers", "address.phoneNumbers", dataType = UIDataType.CUSTOMIZED))
                        .add(lc, "addressbookList"))
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
                url="???",
                tooltip = "address.book.vCardExport.tooltip.content",
                tooltipTitle = "address.book.vCardExport.tooltip.title"))
        exportMenu.add(MenuItem("address.export",
                i18nKey = "address.book.export",
                url="???",
                tooltipTitle = "address.book.export",
                tooltip = "address.book.export.tooltip"))
        exportMenu.add(MenuItem("address.exportFavoritePhoneList",
                i18nKey = "address.book.exportFavoritePhoneList",
                url="???",
                tooltipTitle = "address.book.exportFavoritePhoneList.tooltip.title",
                tooltip = "address.book.exportFavoritePhoneList.tooltip.content"))
        layout.add(exportMenu, menuIndex++)
        layout.getMenuById(GEAR_MENU)?.add(MenuItem("address.exportAppleScript4Notes",
                i18nKey = "address.book.export.appleScript4Notes",
                url="???",
                tooltipTitle = "address.book.export.appleScript4Notes",
                tooltip = "address.book.export.appleScript4Notes.tooltip"))
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: AddressDO?): UILayout {
        val addressbookDOs = addressbookDao.allAddressbooksWithFullAccess
        val addressbooks = mutableListOf<AutoCompletion.Entry>()
        addressbookDOs.forEach {
            addressbooks.add(AutoCompletion.Entry(it.id, it.title))
        }
        val layout = super.createEditLayout(dataObject)
                .add(UIGroup()
                        .add(UIMultiSelect("addressbookList", lc,
                                autoCompletion = AutoCompletion(values = addressbooks))))
                .add(UIRow()
                        .add(UIFieldset(6).add(lc, "contactStatus"))
                        .add(UIFieldset(6)
                                .add(UIRow()
                                        .add(UICol(length = 8)
                                                .add(lc, "addressStatus"))
                                        .add(UICol(length = 4)
                                                .add(UICheckbox("favorite", label = "favorite"))))))
                .add(UIRow()
                        .add(UIFieldset(6)
                                .add(lc, "name", "firstName")
                                .add(UISelect("form", lc).buildValues(FormOfAddress::class.java))
                                .add(lc, "title", "email", "privateEmail"))
                        .add(UIFieldset(6)
                                .add(lc, "birthday", "communicationLanguage", "organization", "division", "positionText", "website")))
                .add(UIRow()
                        .add(UIFieldset(6)
                                .add(lc, "businessPhone", "mobilePhone", "fax"))
                        .add(UIFieldset(6)
                                .add(lc, "privatePhone", "privateMobilePhone")))
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
                .add(UIRow()
                        .add(UIFieldset(6, "address.heading.privateAddress")
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
                        .add(UIFieldset(6,"address.image")
                                .add(UICustomized("addressImage"))))
                .add(UIFieldset(title = "address.image")
                        .add(lc, "comment"))
        layout.getInputById("name").focus = true
        layout.addTranslations("delete", "file.upload.dropArea", "address.image.upload.error")
        layout.add(MenuItem("address.printView",
                i18nKey = "printView",
                url="???"))
        layout.add(MenuItem("address.vCardSingleExport",
                i18nKey = "address.book.vCardSingleExport",
                url="???"))
        layout.add(MenuItem("address.directCall",
                i18nKey = "address.directCall.call",
                url="???"))

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

    /**
     * Needed for json serialization
     */
    class AddressbookDOSerializer : JsonSerializer<org.projectforge.business.address.AddressbookDO> {
        @Synchronized
        override fun serialize(obj: org.projectforge.business.address.AddressbookDO?, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement? {
            if (obj == null) return null
            val result = JsonObject()
            result.add("id", JsonPrimitive(obj.id))
            result.add("title", JsonPrimitive(obj.title))
            return result
        }
    }
}

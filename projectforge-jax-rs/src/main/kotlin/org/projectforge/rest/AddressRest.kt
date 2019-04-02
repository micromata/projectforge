package org.projectforge.rest

import com.google.gson.*
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.address.*
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.rest.core.ResultSet
import org.projectforge.ui.*
import org.springframework.stereotype.Component
import java.lang.reflect.Type
import javax.ws.rs.Path

@Component
@Path("address")
open class AddressRest()
    : AbstractDORest<AddressDO, AddressDao, AddressFilter>(AddressDao::class.java, AddressFilter::class.java, "address.title") {

    private class Address(val address: AddressDO,
                          var imageUrl: String? = null,
                          var previewImageUrl: String? = null)

    init {
        restHelper.add(AddressbookDO::class.java, AddressbookDOSerializer())
    }

    private val log = org.slf4j.LoggerFactory.getLogger(AddressRest::class.java)

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
                        .add(lc, "imageDataPreview")
                        .add(addressLC, "name", "firstName", "organization", "email")
                        .add(UITableColumn("phoneNumbers", "address.phoneNumbers", dataType = UIDataType.CUSTOMIZED))
                        .add(lc, "addressBooks"))
        LayoutUtils.addListFilterContainer(layout,
                UICheckbox("filter", label = "filter"),
                UICheckbox("newest", label = "filter.newest"),
                UICheckbox("favorites", label = "address.filter.myFavorites"),
                UICheckbox("dublets", label = "address.filter.doublets"))
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: AddressDO?): UILayout {
        val layout = super.createEditLayout(dataObject)
                .add(UIGroup()
                        .add(UIMultiSelect("addressbookList", lc)))
                .add(UIRow()
                        .add(UICol(6).add(lc, "contactStatus"))
                        .add(UICol(6).add(lc, "addressStatus")))
                .add(UIRow()
                        .add(UICol(6)
                                .add(lc, "name", "firstName")
                                .add(UIGroup()
                                        .add(UISelect("form", lc).buildValues(FormOfAddress::class.java))
                                        .add(UICheckbox("favorite", label = "favorite")))
                                .add(lc, "title", "email", "privateEmail"))
                        .add(UICol(6)
                                .add(lc, "birthday", "communicationLanguage", "organization", "division", "positionText", "website")))
                .add(UIRow()
                        .add(UICol(6)
                                .add(lc, "businessPhone", "mobilePhone", "fax"))
                        .add(UICol(6)
                                .add(lc, "privatePhone", "privateMobilePhone")))
                .add(UIRow()
                        .add(UICol(6)
                                .add(UILabel("address.heading.businessAddress"))
                                .add(lc, "addressText", "zipCode", "city", "country", "state", "zipCode"))
                        .add(UICol(6)
                                .add(UILabel("address.heading.postalAddress"))
                                .add(lc, "postalAddressText", "postalZipCode", "postalCity", "postalCountry", "postalState", "postalZipCode")))
                .add(UIRow()
                        .add(UICol(6)
                                .add(UILabel("address.heading.privateAddress"))
                                .add(lc, "privateAddressText", "privateZipCode", "privateCity", "privateCountry", "privateState", "privateZipCode"))
                        .add(UICol(6)
                                .add(UILabel("address.image"))
                                .add(UICustomized("addressImage"))))
                .add(lc, "comment")
        layout.getInputById("name").focus = true
        return LayoutUtils.processEditPage(layout, dataObject)
    }

    override fun processResultSetBeforeExport(resultSet: ResultSet<Any>) {
        val list: List<Address> = resultSet.resultSet.map { it ->
            Address(it as AddressDO,
                    imageUrl = if (it.imageData != null) "address/image/${it.id}" else null,
                    previewImageUrl = if (it.imageDataPreview != null) "address/imagePreview/${it.id}" else null)
        }
        resultSet.resultSet = list
        resultSet.resultSet.forEach { it ->
            (it as Address).address.imageData = null
            it.address.imageDataPreview = null
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
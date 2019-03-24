package org.projectforge.rest

import com.google.gson.*
import org.projectforge.business.address.*
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.rest.json.JsonCreator
import org.projectforge.ui.*
import org.springframework.stereotype.Component
import java.lang.reflect.Type
import javax.ws.rs.Path

@Component
@Path("addresses")
open class AddressRest()
    : AbstractDORest<AddressDO, AddressDao, AddressFilter>(AddressDao::class.java, AddressFilter::class.java) {

    private val log = org.slf4j.LoggerFactory.getLogger(AddressRest::class.java)

    override fun newBaseDO(): AddressDO {
        return AddressDO()
    }

    /**
     * Clone is supported by addresses.
     */
    override fun prepareClone(obj: AddressDO): Boolean {
        // TODO: Enter here the PersonalAddressDO stuff etc.
        return true
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val ls = LayoutSettings(AddressDO::class.java)
        val layout = UILayout("address.title.list")
                .add(UITable("result-set")
                        .add(ls, "lastUpdate", "imageDataPreview", "name", "firstName", "organization", "email")
                        .add(UITableColumn("phoneNumbers", "address.phoneNumbers", dataType = UIDataType.CUSTOMIZED))
                        .add(ls, "addressBooks"))
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
    override fun createEditLayout(address: AddressDO?, inlineLabels: Boolean): UILayout {
        val titleKey = if (address?.id != null) "address.title.edit" else "address.title.add"
        val ls = LayoutSettings(AddressDO::class.java, inlineLabels)
        val layout = UILayout(titleKey)
                .add(UIGroup()
                        .add(UIMultiSelect("addressbookList", ls)))
                .add(UIRow()
                        .add(UICol(6).add(ls, "contactStatus"))
                        .add(UICol(6).add(ls, "addressStatus")))
                .add(UIRow()
                        .add(UICol(6)
                                .add(ls, "name", "firstName")
                                .add(UIGroup().add(UISelect("form", ls).buildValues(FormOfAddress::class.java))
                                        .add(UICheckbox("favorite", label = "favorite")))
                                .add(ls, "title", "email", "privateEmail"))
                        .add(UICol(6)
                                .add(ls, "birthday", "communicationLanguage", "organization", "division", "positionText", "website")))
                .add(UIRow()
                        .add(UICol(6)
                                .add(ls, "businessPhone", "mobilePhone", "fax"))
                        .add(UICol(6)
                                .add(ls, "privatePhone", "privateMobilePhone")))
                .add(UIRow()
                        .add(UICol(6)
                                .add(UILabel("address.heading.businessAddress"))
                                .add(ls, "addressText", "zipCode", "city", "country", "state", "zipCode"))
                        .add(UICol(6)
                                .add(UILabel("address.heading.postalAddress"))
                                .add(ls, "postalAddressText", "postalZipCode", "postalCity", "postalCountry", "postalState", "postalZipCode")))
                .add(UIRow()
                        .add(UICol(6)
                                .add(UILabel("address.heading.privateAddress"))
                                .add(ls, "privateAddressText", "privateZipCode", "privateCity", "privateCountry", "privateState", "privateZipCode"))
                        .add(UICol(6)
                                .add(UILabel("address.image"))
                                .add(UICustomized("address-image"))))
                .add(ls, "comment")
        layout.getInputById("name").focus = true
        return LayoutUtils.processEditPage(layout, address)
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

    companion object {
        init {
            JsonCreator.add(AddressbookDO::class.java, AddressbookDOSerializer())
        }
    }
}
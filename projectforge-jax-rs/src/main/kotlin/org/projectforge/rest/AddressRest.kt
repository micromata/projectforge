package org.projectforge.rest

import com.google.gson.*
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.address.*
import org.projectforge.framework.i18n.translate
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

    override fun validate(obj: AddressDO): List<ValidationError>? {
        val errorsList = mutableListOf<ValidationError>()
        if (StringUtils.isBlank(obj.name) == true
                && StringUtils.isBlank(obj.firstName) == true
                && StringUtils.isBlank(obj.organization) == true) {
            errorsList.add(ValidationError(translate("address.form.error.toFewFields"), fieldId = "name"))
        }
        if (errorsList.isEmpty()) return null
        return errorsList
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val lc = LayoutContext(AddressDO::class.java)
        val layout = UILayout("address.title.list")
                .add(UITable("result-set")
                        .add(lc, "lastUpdate", "imageDataPreview", "name", "firstName", "organization", "email")
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
    override fun createEditLayout(dataObject: AddressDO?, inlineLabels: Boolean): UILayout {
        val titleKey = if (dataObject?.id != null) "address.title.edit" else "address.title.add"
        val lc = LayoutContext(AddressDO::class.java, inlineLabels)
        val formFavoriteGroup = UIGroup().add(UISelect("form", lc).buildValues(FormOfAddress::class.java))
                .add(UICheckbox("favorite", label = "favorite"))
        val formFavoriteLabelValue =
                if (inlineLabels) formFavoriteGroup
                else UIGroup().add(UILabel("gender"), formFavoriteGroup)!!
        val layout = UILayout(titleKey)
                .add(UIGroup()
                        .add(UIMultiSelect("addressbookList", lc)))
                .add(UIRow()
                        .add(UICol(6).add(lc, "contactStatus"))
                        .add(UICol(6).add(lc, "addressStatus")))
                .add(UIRow()
                        .add(UICol(6)
                                .add(lc, "name", "firstName")
                                .add(formFavoriteLabelValue)
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
                                .add(UICustomized("address-image"))))
                .add(lc, "comment")
        layout.getInputById("name").focus = true
        return LayoutUtils.processEditPage(layout, dataObject)
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
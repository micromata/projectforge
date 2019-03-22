package org.projectforge.rest.ui

import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressStatus
import org.projectforge.business.address.ContactStatus
import org.projectforge.business.address.FormOfAddress
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.ui.*

class AddressLayout {
    companion object {
        private fun propLength(propertyName: String): Int {
            return HibernateUtils.getPropertyLength(AddressDO::class.java, propertyName)
        }

        fun createListLayout(): UILayout {
            val layout = UILayout("address.title.list")
                    .add(UITable("result-set")
                            .add(UITableColumn("lastUpdate", dataType = UIDataType.DATE))
                            .add("imageDataPreview", "name", "firstName", "organization", "email")
                            .add(UITableColumn("phoneNumbers", "Telefonnummern", dataType = UIDataType.CUSTOMIZED))
                            .add("addressBooks"))
            LayoutUtils.addListFilterContainer(layout,
                    UICheckbox("filter", label = "filter"),
                    UICheckbox("newest", label = "filter.newest"),
                    UICheckbox("filter.favorites", label = "address.filter.myFavorites"),
                    UICheckbox("filter.dublets", label = "address.filter.doublets"))
            return LayoutUtils.processListPage(layout, AddressDO::class.java)
        }

        fun createEditLayout(address: AddressDO?): UILayout {
            val titleKey = if (address?.id != null) "address.title.edit" else "address.title.add"
            val layout = UILayout(titleKey)
                    .add(UIGroup()
                            .add(UIMultiSelect("addressbookList")))
                    .add(UIRow()
                            .add(UICol(6)
                                    .add(UIGroup().add(UISelect("contactStatus", i18nEnum = ContactStatus::class.java))))
                            .add(UICol(6)
                                    .add(UIGroup().add(UISelect("addressStatus", i18nEnum = AddressStatus::class.java)))))
                    .add(UIRow()
                            .add(UICol(6)
                                    .add(UIGroup().add(UIInput("name", required = true, focus = true)))
                                    .add(UIGroup().add(UIInput("firstName")))
                                    .add(UIGroup().add(UISelect("form", i18nEnum = FormOfAddress::class.java))
                                            .add(UICheckbox("favorite", label = "favorite")))
                                    .add("title", "email", "privateEmail"))
                            .add(UICol(6)
                                    .add("birthday", "communicationLanguage", "organization", "division", "positionText", "website")))
                    .add(UIRow()
                            .add(UICol(6)
                                    .add("businessPhone", "mobilePhone", "fax"))
                            .add(UICol(6)
                                    .add("privatePhone", "privateMobilePhone")))
                    .add(UIRow()
                            .add(UICol(6)
                                    .add(UILabel("address.heading.businessAddress"))
                                    .add("addressText", "zipCode", "city", "country", "state", "zipCode"))
                            .add(UICol(6)
                                    .add(UILabel("address.heading.postalAddress"))
                                    .add("postalAddressText", "postalZipCode", "postalCity", "postalCountry", "postalState", "postalZipCode")))
                    .add(UIRow()
                            .add(UICol(6)
                                    .add(UILabel("address.heading.privateAddress"))
                                    .add("privateAddressText","privateZipCode","privateCity","privateCountry","privateState","privateZipCode"))
                            .add(UICol(6)
                                    .add(UILabel("address.image"))
                                    .add(UICustomized("address-image"))))
                    .add(UIGroup().add( UITextarea("comment")))
            return LayoutUtils.processEditPage(layout, AddressDO::class.java, address)
        }
    }
}
package org.projectforge.rest.ui

import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressFilter
import org.projectforge.business.address.FormOfAddress
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.ui.*

class AddressLayout {
    companion object {
        private fun propLength(propertyName: String): Int {
            return HibernateUtils.getPropertyLength(AddressDO::class.java, propertyName)
        }

        fun createListLayout(): UILayout {
            val ls = LayoutSettings(AddressDO::class.java)
            val layout = UILayout("address.title.list")
                    .add(UITable("result-set")
                            .add(ls, "lastUpdate","imageDataPreview", "name", "firstName", "organization", "email")
                            .add(UITableColumn("phoneNumbers", "address.phoneNumbers", dataType = UIDataType.CUSTOMIZED))
                            .add(ls, "addressBooks"))
            LayoutUtils.addListFilterContainer(layout,
                    UICheckbox("filter", label = "filter"),
                    UICheckbox("newest", label = "filter.newest"),
                    UICheckbox("favorites", label = "address.filter.myFavorites"),
                    UICheckbox("dublets", label = "address.filter.doublets"))
            return LayoutUtils.processListPage(layout)
        }

        fun createEditLayout(address: AddressDO?, inlineLabels : Boolean): UILayout {
            val titleKey = if (address?.id != null) "address.title.edit" else "address.title.add"
            val ls = LayoutSettings(AddressDO::class.java, inlineLabels)
            val layout = UILayout(titleKey)
                    .add(UIGroup()
                            .add(UIMultiSelect("addressbookList")))
                    .add(UIRow()
                            .add(UICol(6).add(ls, "contactStatus"))
                            .add(UICol(6).add(ls, "addressStatus")))
                    .add(UIRow()
                            .add(UICol(6)
                                    .add(ls, "name", "firstName")
                                    .add(UIGroup().add(UISelect("form").buildValues(FormOfAddress::class.java))
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
    }
}
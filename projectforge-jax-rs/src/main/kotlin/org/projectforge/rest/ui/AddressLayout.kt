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
            val layout = UILayout("Addressen")
                    .add(UITable("result-set")
                            .add(UITableColumn("lastUpdate", "geändert", dataType = UIDataType.DATE))
                            .add(UITableColumn("imageDataPreview", "Bild"))
                            .add(UITableColumn("name", "Name"))
                            .add(UITableColumn("firstName", "Vorname"))
                            .add(UITableColumn("organization", "Firma"))
                            .add(UITableColumn("email", "E-Mail"))
                            .add(UITableColumn("phoneNumbers", "Telefonnummern", dataType = UIDataType.CUSTOMIZED))
                            .add(UITableColumn("addressBooks", "Addressbücher")))
                    .addAction(UIButton("reset", "Rücksetzen", UIButtonStyle.DANGER))
                    .addAction(UIButton("search", "Suchen", UIButtonStyle.PRIMARY))
                    .add(UINamedContainer("filter-options")
                            .add(UIGroup()
                                    .add(UICheckbox("filter.filter", label = "Filter"))
                                    .add(UICheckbox("filter.newest", label = "die neuesten"))
                                    .add(UICheckbox("filter.favorites", label = "meine Favoriten"))
                                    .add(UICheckbox("filter.dublets", label = "Dupletten"))))
            return LayoutUtils.process(layout, AddressDO::class.java)
        }

        fun createEditLayout(address: AddressDO?): UILayout {
            val titleKey = if (address?.id != null) "address.title.edit" else "address.title.add"
            val layout = UILayout(titleKey)
                    .add(UIGroup()
                            .add("@", UIMultiSelect("addressbookList")))
                    .add(UIRow()
                            .add(UICol(6)
                                    .add(UIGroup().add("@", UISelect("contactStatus", i18nEnum = ContactStatus::class.java))))
                            .add(UICol(6)
                                    .add(UIGroup().add("@", UISelect("addressStatus", i18nEnum = AddressStatus::class.java)))))
                    .add(UIRow()
                            .add(UICol(6)
                                    .add(UIGroup().add("@", UIInput("name", required = true, focus = true)))
                                    .add(UIGroup().add("@", UIInput("firstName")))
                                    .add(UIGroup().add("@", UISelect("form", i18nEnum = FormOfAddress::class.java)))
                                    .add(UIGroup().add("@", UIInput("title")))
                                    .add(UIGroup().add("@", UIInput("email")))
                                    .add(UIGroup().add("@", UIInput("privateEmail"))))
                            .add(UICol(6)
                                    .add(UIGroup().add("@", UIInput("birthday")))
                                    .add(UIGroup().add("@", UIInput("communicationLanguage")))
                                    .add(UIGroup().add("@", UIInput("organization")))
                                    .add(UIGroup().add("@", UIInput("division")))
                                    .add(UIGroup().add("@", UIInput("positionText")))
                                    .add(UIGroup().add("@", UIInput("website")))))
                    .add(UIRow()
                            .add(UICol(6)
                                    .add(UIGroup().add("@", UIInput("businessPhone")))
                                    .add(UIGroup().add("@", UIInput("mobilePhone")))
                                    .add(UIGroup().add("@", UIInput("fax"))))
                            .add(UICol(6)
                                    .add(UIGroup().add("@", UIInput("privatePhone")))
                                    .add(UIGroup().add("@", UIInput("privateMobilePhone")))))
                    .add(UIRow()
                            .add(UICol(6)
                                    // label starting with ^ will be interpreted as i18n-keys and will be translated.
                                    .add(UILabel("^address.heading.businessAddress"))
                                    .add(UIGroup().add("@", UIInput("addressText")))
                                    .add(UIGroup().add("@", UIInput("zipCode")))
                                    .add(UIGroup().add("@", UIInput("city")))
                                    .add(UIGroup().add("@", UIInput("country")))
                                    .add(UIGroup().add("@", UIInput("state")))
                                    .add(UIGroup().add("@", UIInput("zipCode"))))
                            .add(UICol(6)
                                    .add(UILabel("^address.heading.postalAddress"))
                                    .add(UIGroup().add("@", UIInput("postalAddressText")))
                                    .add(UIGroup().add("@", UIInput("postalZipCode")))
                                    .add(UIGroup().add("@", UIInput("postalCity")))
                                    .add(UIGroup().add("@", UIInput("postalCountry")))
                                    .add(UIGroup().add("@", UIInput("postalState")))
                                    .add(UIGroup().add("@", UIInput("postalZipCode")))))
                    .add(UIRow()
                            .add(UICol(6)
                                    .add(UILabel("^address.heading.privateAddress"))
                                    .add(UIGroup().add("@", UIInput("privateAddressText")))
                                    .add(UIGroup().add("@", UIInput("privateZipCode")))
                                    .add(UIGroup().add("@", UIInput("privateCity")))
                                    .add(UIGroup().add("@", UIInput("privateCountry")))
                                    .add(UIGroup().add("@", UIInput("privateState")))
                                    .add(UIGroup().add("@", UIInput("privateZipCode"))))
                            .add(UICol(6)
                                    .add(UILabel("address.image"))
                                    .add(UICustomized("address-image"))))
                    .add(UIGroup()
                            .add("@", UITextarea("comment")))
            return LayoutUtils.processEditPage(layout, AddressDO::class.java, address)
        }
    }
}
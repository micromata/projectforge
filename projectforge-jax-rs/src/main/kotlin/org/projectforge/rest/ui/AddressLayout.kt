package org.projectforge.rest.ui

import org.projectforge.business.address.AddressDO
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

        fun createEditLayout(): UILayout {
            val layout = UILayout("Adresse bearbeiten")
                    .add(UIGroup()
                            .add(UILabel("Adressbücher", "addressbooks"))
                            .add(UIMultiSelect("addressbooks")))
                    .add(UIRow()
                            .add(UICol(6)
                                    .add(UIGroup()
                                            .add(UILabel("Name", "name"))
                                            .add(UIInput("name", propLength("name"), required = true, focus = true)))
                                    .add(UIGroup()
                                            .add(UILabel("Vorname", "firstName"))
                                            .add(UIInput("firstName", propLength("firstName"))))
                                    .add(UIGroup()
                                            .add(UILabel("Anrede", "form"))
                                            .add(UISelect("form")
                                                    .add(UISelectValue("male", "Herr"))
                                                    .add(UISelectValue("male", "Herr"))
                                                    .add(UISelectValue("divers", "divers"))
                                                    .add(UISelectValue("unkown", "unbekannt"))
                                                    .add(UISelectValue("company", "Firma"))))
                                    .add(UIGroup()
                                            .add(UILabel("Titel", "title"))
                                            .add(UIInput("title", 255))))
                            .add(UICol(6)
                                    .add(UIGroup()
                                            .add(UILabel("Kontaktstatus", "contactstatus"))
                                            .add(UISelect("gender")
                                                    .add(UISelectValue("active", "aktiv"))
                                                    .add(UISelectValue("inactive", "inaktiv"))))
                                    .add(UIGroup()
                                            .add(UILabel("Adressstatus", "addressstatus"))
                                            .add(UISelect("gender")
                                                    .add(UISelectValue("uptodate", "aktuell"))
                                                    .add(UISelectValue("outdated", "veraltet"))
                                                    .add(UISelectValue("leaved", "Unternehmen verlassen"))))
                                    .add(UIGroup()
                                            .add(UILabel("Firma", "company"))
                                            .add(UIInput("company", 255)))
                                    .add(UIGroup()
                                            .add(UILabel("Abteilung", "division"))
                                            .add(UIInput("division", 255)))
                                    .add(UIGroup()
                                            .add(UILabel("Position", "position"))
                                            .add(UIInput("position", 255)))))
                    .add(UIGroup()
                            .add(UILabel("Bemerkung", "comment"))
                            .add(UITextarea("comment", 4000)))
                    .addAction(UIButton("cancel", "Abbrechen", UIButtonStyle.DANGER))
                    .addAction(UIButton("markAsDeleted", "Als gelöscht markieren", UIButtonStyle.WARNING))
                    .addAction(UIButton("update", "Ändern", UIButtonStyle.PRIMARY))
            return LayoutUtils.process(layout, AddressDO::class.java)
        }
    }
}
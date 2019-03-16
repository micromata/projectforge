package org.projectforge.rest.ui

import org.projectforge.business.book.BookDO
import org.projectforge.ui.*

/**
 * maxLength = 0 is replace by @Column(length=...) of JPA definition.
 */
class BookLayout {
    companion object {
        fun createListLayout(): UILayout {
            val layout = UILayout("Bücherliste")
                    .add(UITable("result-set")
                            .add(UITableColumn("created", "angelegt", dataType = UIDataType.DATE))
                            .add(UITableColumn("year", "Jahr"))
                            .add(UITableColumn("signature", "Signatur"))
                            .add(UITableColumn("authors", "Autoren"))
                            .add(UITableColumn("title", "Titel"))
                            .add(UITableColumn("keywords", "Schlüsselworte"))
                            .add(UITableColumn("lendOutBy", "Ausgeliehen von"))
                            .add(UITableColumn("year", "Jahr")))
                    .addAction(UIButton("reset", "Rücksetzen", UIButtonStyle.DANGER))
                    .addAction(UIButton("search", "Suchen", UIButtonStyle.PRIMARY))
                    .add(UINamedContainer("filter-options")
                            .add(UIGroup()
                                    .add(UICheckbox("filter.present", label = "vorhanden"))
                                    .add(UICheckbox("filter.missed", label = "vermisst"))
                                    .add(UICheckbox("filter.disposed", label = "entsorgt"))
                                    .add(UICheckbox("filter.onlyDeleted", label = "nur gelöscht"))
                                    .add(UICheckbox("filter.searchHistory", label = "Historie"))))
            LayoutUtils.processAllElements(layout.getAllElements(), BookDO::class.java)
            return layout
        }

        fun createEditLayout(): UILayout {
            val layout = UILayout("Buch bearbeiten")
                    .add(UIGroup().add("Titel", UIInput("title", 0, required = true, focus = true)))
                    .add(UIGroup().add("Autoren", UIInput("authors", 0)))
                    .add(UIRow()
                            .add(UICol(6)
                                    .add(UIGroup()
                                            .add("Typ",
                                                    UISelect("type")
                                                            .add(UISelectValue("book", "Buch"))
                                                            .add(UISelectValue("magazine", "Magazin"))))
                                    .add(UIGroup().add("Veröffentlichungsjahr", UIInput("yearOfPublishing", 0)))
                                    .add(UIGroup()
                                            .add("Status",
                                                    UISelect("status")
                                                            .add(UISelectValue("present", "vorhanden"))
                                                            .add(UISelectValue("missed", "vermisst"))))
                                    .add(UIGroup().add("Signatur", UIInput("signature", 0))))
                            .add(UICol(6)
                                    .add(UIGroup().add("ISBN", UIInput("isbn", 0)))
                                    .add(UIGroup().add("Schlüsselwörter", UIInput("keywords", 0)))
                                    .add(UIGroup()
                                            .add(UILabel("Verlag", "publisher"))
                                            .add(UIInput("publisher", 0)))
                                    .add(UIGroup()
                                            .add(UILabel("Herausgeber", "editor"))
                                            .add(UIInput("editor", 0)))))
                    .add(UIGroup()
                            .add(UILabel("Ausleihe"))
                            .add(UICustomized("lendOut")
                                    .add("lendOutBy", "kai")))
                    .add(UIGroup()
                            .add("Ausleihnotiz (optional)", UITextarea("lendOutComment", 0)))
                    .add(UIGroup()
                            .add("Zusammenfassung", UITextarea("abstractText", 0)))
                    .add(UIGroup()
                            .add("Bemerkung", UITextarea("comment", 0)))
                    .addAction(UIButton("cancel", "Abbrechen", UIButtonStyle.DANGER))
                    .addAction(UIButton("markAsDeleted", "Als gelöscht markieren", UIButtonStyle.WARNING))
                    .addAction(UIButton("update", "Ändern", UIButtonStyle.PRIMARY))
            LayoutUtils.processAllElements(layout.getAllElements(), BookDO::class.java)
            return layout
        }
    }
}
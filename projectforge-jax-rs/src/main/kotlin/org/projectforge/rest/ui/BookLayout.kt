package org.projectforge.rest.ui

import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookStatus
import org.projectforge.business.book.BookType
import org.projectforge.ui.*

/**
 * maxLength = 0 is replace by @Column(length=...) of JPA definition.
 * label = "." will be replaced by @PropertyColumn setting in BookDO.
 */
class BookLayout {
    companion object {
        fun createListLayout(): UILayout {
            val layout = UILayout("Bücherliste")
                    .add(UITable("result-set")
                            .add(UITableColumn("created", ".", dataType = UIDataType.DATE))
                            .add(UITableColumn("year", "."))
                            .add(UITableColumn("signature", "."))
                            .add(UITableColumn("authors", "."))
                            .add(UITableColumn("title", "."))
                            .add(UITableColumn("keywords", "."))
                            .add(UITableColumn("lendOutBy", "."))
                            .add(UITableColumn("year", ".")))
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
                    .add(UIGroup().add(".", UIInput("title", 0, required = true, focus = true)))
                    .add(UIGroup().add(".", UIInput("authors", 0)))
                    .add(UIRow()
                            .add(UICol(6)
                                    .add(UIGroup()
                                            .add(".",
                                                    UISelect("type", i18nEnum = BookType::class.java)))
                                    .add(UIGroup().add(".", UIInput("yearOfPublishing", 0)))
                                    .add(UIGroup()
                                            .add(".",
                                                    UISelect("status", i18nEnum = BookStatus::class.java)))
                                    .add(UIGroup().add(".", UIInput("signature", 0))))
                            .add(UICol(6)
                                    .add(UIGroup().add(".", UIInput("isbn", 0)))
                                    .add(UIGroup().add(".", UIInput("keywords", 0)))
                                    .add(UIGroup().add(".", UIInput("publisher", 0)))
                                    .add(UIGroup().add(".", UIInput("editor", 0)))))
                    .add(UIGroup()
                            .add(UILabel("Ausleihe"))
                            .add(UICustomized("lendOut")
                                    .add("lendOutBy", "kai")))
                    .add(UIGroup()
                            .add(".", UITextarea("lendOutComment", 0)))
                    .add(UIGroup()
                            .add(".", UITextarea("abstractText", 0)))
                    .add(UIGroup()
                            .add(".", UITextarea("comment", 0)))
                    .addAction(UIButton("cancel", ".", UIButtonStyle.DANGER))
                    .addAction(UIButton("markAsDeleted", ".", UIButtonStyle.WARNING))
                    .addAction(UIButton("update", ".", UIButtonStyle.PRIMARY))
            LayoutUtils.processAllElements(layout.getAllElements(), BookDO::class.java)
            return layout
        }
    }
}
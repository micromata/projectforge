package org.projectforge.rest.ui

import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookStatus
import org.projectforge.business.book.BookType
import org.projectforge.ui.*

/**
 * maxLength is automatically set by @Column(length=...) of JPA definition.
 * label = "@" will be replaced by @PropertyColumn setting in BookDO.
 */
class BookLayout {
    companion object {
        fun createListLayout(): UILayout {
            val layout = UILayout("book.title.list")
                    .add(UITable("result-set")
                            .add(UITableColumn("created", "@", dataType = UIDataType.DATE))
                            .add(UITableColumn("year", "@"))
                            .add(UITableColumn("signature", "@"))
                            .add(UITableColumn("authors", "@"))
                            .add(UITableColumn("title", "@"))
                            .add(UITableColumn("keywords", "@"))
                            .add(UITableColumn("lendOutBy", "@"))
                            .add(UITableColumn("year", "@")))
                    .addAction(UIButton("reset", "Rücksetzen", UIButtonStyle.DANGER))
                    .addAction(UIButton("search", "Suchen", UIButtonStyle.PRIMARY))
                    .add(UINamedContainer("filter-options")
                            .add(UIGroup()
                                    .add(UICheckbox("filter.present", label = "vorhanden"))
                                    .add(UICheckbox("filter.missed", label = "vermisst"))
                                    .add(UICheckbox("filter.disposed", label = "entsorgt"))
                                    .add(UICheckbox("filter.onlyDeleted", label = "nur gelöscht"))
                                    .add(UICheckbox("filter.searchHistory", label = "Historie"))))
            return LayoutUtils.process(layout, BookDO::class.java)
        }

        fun createEditLayout(): UILayout {
            val layout = UILayout("book.title.edit") // TODO: book.title.add for new books.
                    .add(UIGroup().add("@", UIInput("title", required = true, focus = true)))
                    .add(UIGroup().add("@", UIInput("authors")))
                    .add(UIRow()
                            .add(UICol(6)
                                    .add(UIGroup()
                                            .add("@",
                                                    UISelect("type", i18nEnum = BookType::class.java)))
                                    .add(UIGroup().add("@", UIInput("yearOfPublishing")))
                                    .add(UIGroup()
                                            .add("@",
                                                    UISelect("status", i18nEnum = BookStatus::class.java)))
                                    .add(UIGroup().add("@", UIInput("signature"))))
                            .add(UICol(6)
                                    .add(UIGroup().add("@", UIInput("isbn")))
                                    .add(UIGroup().add("@", UIInput("keywords"))
                                            .add(UIGroup().add("@", UIInput("publisher")))
                                            .add(UIGroup().add("@", UIInput("editor"))))))
                    .add(UIGroup()
                            .add(UILabel(translate("book.lending")))
                            .add(UICustomized("lendOut")
                                    .add("lendOutBy", "kai")))
                    .add(UIGroup()
                            .add("@", UITextarea("lendOutComment")))
                    .add(UIGroup()
                            .add("@", UITextarea("abstractText")))
                    .add(UIGroup()
                            .add("@", UITextarea("comment")))
                    .addAction(UIButton("cancel", "@", UIButtonStyle.DANGER))
                    .addAction(UIButton("markAsDeleted", "@", UIButtonStyle.WARNING))
                    .addAction(UIButton("update", "@", UIButtonStyle.PRIMARY))
            return LayoutUtils.process(layout, BookDO::class.java)
        }
    }
}
package org.projectforge.rest.ui

import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookFilter
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
                            .add(UITableColumn("created", dataType = UIDataType.DATE, formatter = Formatter.TIMESTAMP_MINUTES))
                            .add("yearOfPublishing", "signature", "authors", "title", "keywords")
                            .add(UITableColumn("lendOutBy", formatter = Formatter.USER)))
            LayoutUtils.addListFilterContainer(layout,
                    UICheckbox("present"), UICheckbox("missed"), UICheckbox("disposed"),
                    filterClass = BookFilter::class.java)
            return LayoutUtils.processListPage(layout, BookDO::class.java)
        }

        fun createEditLayout(book: BookDO?): UILayout {
            val titleKey = if (book?.id != null) "book.title.edit" else "book.title.add"
            val ls = LayoutSettings(BookDO::class.java)
            val layout = UILayout(titleKey)
                    .add(ls, "title", "authors")
                    .add(UIRow()
                            .add(UICol(6)
                                    .add(ls, "type", "yearOfPublishing", "status", "signature"))
                            .add(UICol(6)
                                    .add(ls, "isbn", "keywords", "publisher", "editor")))
                    .add(UIGroup()
                            .add(UILabel("book.lending"))
                            .add(UICustomized("lendOutComponent")))
                    .add(UIGroup().add(UITextarea("lendOutComment")))
                    .add(UIGroup().add(UITextarea("abstractText")))
                    .add(UIGroup().add(UITextarea("comment")))
            (layout.getElementById("title") as UIInput).focus = true
            return LayoutUtils.processEditPage(layout, BookDO::class.java, book)
        }
    }
}

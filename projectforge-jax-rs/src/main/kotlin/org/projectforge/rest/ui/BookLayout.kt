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
            val ls = LayoutSettings(BookDO::class.java)
            val layout = UILayout("book.title.list")
                    .add(UITable("result-set")
                            .add(ls, "created", "yearOfPublishing", "signature", "authors", "title", "keywords", "lendOutBy"))
            layout.getTableColumnById("created").formatter = Formatter.TIMESTAMP_MINUTES
            layout.getTableColumnById("lendOutBy").formatter = Formatter.USER
            LayoutUtils.addListFilterContainer(layout, "present", "missed", "disposed",
                    filterClass = BookFilter::class.java)
            return LayoutUtils.processListPage(layout)
        }

        fun createEditLayout(book: BookDO?, inlineLabels: Boolean): UILayout {
            val titleKey = if (book?.id != null) "book.title.edit" else "book.title.add"
            val ls = LayoutSettings(BookDO::class.java, inlineLabels)
            val layout = UILayout(titleKey)
                    .add(ls, "title", "authors")
                    .add(UIRow()
                            .add(UICol(6)
                                    .add(ls, "type", "yearOfPublishing", "status", "signature"))
                            .add(UICol(6)
                                    .add(ls, "isbn", "keywords", "publisher", "editor")))
                    .add(UIGroup()
                            .add(UILabel( "book.lending", "lendOutComponent"))
                            .add(UICustomized("lendOutComponent")))
                    .add(ls, "lendOutComment", "abstractText", "comment")
            layout.getInputById("title").focus = true
            return LayoutUtils.processEditPage(layout, book)
        }
    }
}

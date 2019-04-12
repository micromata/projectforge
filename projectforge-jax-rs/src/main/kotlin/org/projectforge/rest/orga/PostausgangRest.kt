package org.projectforge.rest.orga

import org.projectforge.business.book.BookFilter
import org.projectforge.business.orga.PostFilter
import org.projectforge.business.orga.PostausgangDO
import org.projectforge.business.orga.PostausgangDao
import org.projectforge.rest.core.AbstractStandardRest
import org.projectforge.ui.*
import org.springframework.stereotype.Component
import javax.ws.rs.Path

@Component
@Path("outgoingMail")
class PostausgangRest() : AbstractStandardRest<PostausgangDO, PostausgangDao, PostFilter>(PostausgangDao::class.java, PostFilter::class.java, "orga.postausgang.title") {
    /**
     * Initializes new outbox mails for adding.
     */
    override fun newBaseDO(): PostausgangDO {
        val outbox = super.newBaseDO()
        return outbox
    }

    override fun validate(validationErrors: MutableList<ValidationError>, obj: PostausgangDO) {
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "datum", "empfaenger", "person", "inhalt", "bemerkung", "type"))
        layout.getTableColumnById("datum").formatter = Formatter.DATE
        LayoutUtils.addListFilterContainer(layout, "present", "missed", "disposed",
                filterClass = BookFilter::class.java)
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: PostausgangDO?): UILayout {
        val layout = super.createEditLayout(dataObject)
                .add(UIRow()
                        .add(UICol(length = 2)
                                .add(lc, "datum"))
                        .add(UICol(length=10)
                                .add(lc, "type")))
                .add(UIInput("empfaenger", lc)) // Input-field instead of text-area (length > 255)
                .add(lc, "person", "inhalt", "bemerkung")

        layout.getInputById("empfaenger").focus = true
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}

package org.projectforge.rest.orga

import org.projectforge.business.book.BookFilter
import org.projectforge.business.orga.PostFilter
import org.projectforge.business.orga.PostType
import org.projectforge.business.orga.PostausgangDO
import org.projectforge.business.orga.PostausgangDao
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDate
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
        outbox.datum = PFDate.now().asSqlDate()
        outbox.type = PostType.BRIEF
        return outbox
    }

    override fun validate(validationErrors: MutableList<ValidationError>, obj: PostausgangDO) {
        val date = PFDate.from(obj.datum)
        val today = PFDate.now()
        if (today.isBefore(date)) { // No dates in the future accepted.
            validationErrors.add(ValidationError(translate("error.dateInFuture"), fieldId = "datum"))
        }
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "datum", "empfaenger", "person", "inhalt", "bemerkung", "type"))
        layout.getTableColumnById("datum").formatter = Formatter.DATE
        LayoutUtils.addListFilterContainer(layout, UILabel("'TODO: date range"),
                filterClass = BookFilter::class.java)
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: PostausgangDO?): UILayout {
        val receiver = UIInput("empfaenger", lc) // Input-field instead of text-area (length > 255)
        receiver.focus = true
        receiver.enableAutoCompletion(this)
        val person = UIInput("person", lc).enableAutoCompletion(this)
        val inhalt = UIInput("inhalt", lc).enableAutoCompletion(this)
        val layout = super.createEditLayout(dataObject)
                .add(UIRow()
                        .add(UICol(length = 2)
                                .add(lc, "datum"))
                        .add(UICol(length = 10)
                                .add(lc, "type")))
                .add(receiver)
                .add(person)
                .add(inhalt)
                .add(lc, "bemerkung")
        layout.getInputById("person").enableAutoCompletion(this)
        layout.getInputById("inhalt").enableAutoCompletion(this)
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}

package org.projectforge.rest.orga

import org.projectforge.business.orga.PostFilter
import org.projectforge.business.orga.PostType
import org.projectforge.business.orga.PosteingangDO
import org.projectforge.business.orga.PosteingangDao
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDate
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractStandardRest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/incomingMail")
class PosteingangRest(): AbstractStandardRest<PosteingangDO, PosteingangDao, PostFilter>(PosteingangDao::class.java, PostFilter::class.java, "orga.posteingang.title") {

    override fun newBaseDO(request: HttpServletRequest): PosteingangDO {
        val inbox = super.newBaseDO(request)
        inbox.datum = PFDate.now().asSqlDate()
        inbox.type = PostType.BRIEF
        return inbox
    }

    override fun validate(validationErrors: MutableList<ValidationError>, obj: PosteingangDO) {
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
                        .add(lc, "datum", "absender", "person", "inhalt", "bemerkung", "type"))
        layout.getTableColumnById("datum").formatter = Formatter.DATE
        LayoutUtils.addListFilterContainer(layout, UILabel("'TODO: date range"),
                filterClass = PostFilter::class.java)
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: PosteingangDO): UILayout {
        val sender = UIInput("absender", lc) // Input-field instead of text-area (length > 255)
        sender.focus = true
        sender.enableAutoCompletion(this)
        val person = UIInput("person", lc).enableAutoCompletion(this)
        val inhalt = UIInput("inhalt", lc).enableAutoCompletion(this)
        val layout = super.createEditLayout(dataObject)
                .add(UIRow()
                        .add(UICol(length = 2)
                                .add(lc, "datum"))
                        .add(UICol(length = 10)
                                .add(lc, "type")))
                .add(sender)
                .add(person)
                .add(inhalt)
                .add(lc, "bemerkung")
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}

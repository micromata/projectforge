/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.rest.orga

import org.projectforge.business.orga.PostFilter
import org.projectforge.business.orga.PostType
import org.projectforge.business.orga.PostausgangDO
import org.projectforge.business.orga.PostausgangDao
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDate
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/outgoingMail")
class PostausgangRest() : AbstractDORest<PostausgangDO, PostausgangDao, PostFilter>(PostausgangDao::class.java, PostFilter::class.java, "orga.postausgang.title") {
    /**
     * Initializes new outbox mails for adding.
     */
    override fun newBaseDO(request: HttpServletRequest?): PostausgangDO {
        val outbox = super.newBaseDO(request)
        outbox.datum = PFDate.now().sqlDate
        outbox.type = PostType.BRIEF
        return outbox
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: PostausgangDO) {
        val date = PFDate.from(dto.datum)
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
                filterClass = PostFilter::class.java)
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: PostausgangDO): UILayout {
        val receiver = UIInput("empfaenger", lc) // Input-field instead of text-area (length > 255)
        receiver.focus = true
        receiver.enableAutoCompletion(this)
        val person = UIInput("person", lc).enableAutoCompletion(this)
        val inhalt = UIInput("inhalt", lc).enableAutoCompletion(this)
        val layout = super.createEditLayout(dto)
                .add(UIRow()
                        .add(UICol(length = 2)
                                .add(lc, "datum"))
                        .add(UICol(length = 10)
                                .add(lc, "type")))
                .add(receiver)
                .add(person)
                .add(inhalt)
                .add(lc, "bemerkung")
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}

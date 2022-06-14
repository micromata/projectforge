/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.orga.PostType
import org.projectforge.business.orga.PostausgangDO
import org.projectforge.business.orga.PostausgangDao
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.time.PFDay
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDOPagesRest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/outgoingMail")
class PostausgangPagesRest() :
  AbstractDOPagesRest<PostausgangDO, PostausgangDao>(PostausgangDao::class.java, "orga.postausgang.title") {
  /**
   * Initializes new outbox mails for adding.
   */
  override fun newBaseDO(request: HttpServletRequest?): PostausgangDO {
    val outbox = super.newBaseDO(request)
    outbox.datum = PFDay.now().localDate
    outbox.type = PostType.BRIEF
    return outbox
  }

  override fun validate(validationErrors: MutableList<ValidationError>, dto: PostausgangDO) {
    val date = PFDay.fromOrNull(dto.datum)
    if (date != null && PFDay.now().isBefore(date)) { // No dates in the future accepted.
      validationErrors.add(ValidationError(translate("error.dateInFuture"), fieldId = "datum"))
    }
  }

  /**
   * LAYOUT List page
   */
  override fun createListLayout(request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess) {
    val table = agGridSupport.prepareUIGrid4ListPage(request, layout, magicFilter, this, userAccess = userAccess)
    table.add(lc, "datum", "empfaenger", "person", "absender", "inhalt", "bemerkung", "type")
  }

  /**
   * LAYOUT Edit page
   */
  override fun createEditLayout(dto: PostausgangDO, userAccess: UILayout.UserAccess): UILayout {
    val receiver = UIInput("empfaenger", lc) // Input-field instead of text-area (length > 255)
    receiver.focus = true
    receiver.enableAutoCompletion(this)
    val layout = super.createEditLayout(dto, userAccess)
      .add(
        UIRow()
          .add(
            UICol(2)
              .add(lc, "datum")
          )
          .add(
            UICol(10)
              .add(lc, "type")
          )
      )
      .add(receiver)
      .add(UIInput("person", lc).enableAutoCompletion(this))
      .add(UIInput("inhalt", lc).enableAutoCompletion(this))
      .add(UIInput("absender", lc).enableAutoCompletion(this))
      .add(lc, "bemerkung")
    return LayoutUtils.processEditPage(layout, dto, this)
  }
}

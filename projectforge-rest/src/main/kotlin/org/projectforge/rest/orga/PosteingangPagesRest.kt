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
import org.projectforge.business.orga.PosteingangDO
import org.projectforge.business.orga.PosteingangDao
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
@RequestMapping("${Rest.URL}/incomingMail")
class PosteingangPagesRest() :
  AbstractDOPagesRest<PosteingangDO, PosteingangDao>(PosteingangDao::class.java, "orga.posteingang.title") {

  override fun newBaseDO(request: HttpServletRequest?): PosteingangDO {
    val inbox = super.newBaseDO(request)
    inbox.datum = PFDay.now().localDate
    inbox.type = PostType.E_MAIL
    return inbox
  }

  override fun validate(validationErrors: MutableList<ValidationError>, dto: PosteingangDO) {
    val date = PFDay.fromOrNull(dto.datum)
    if (date != null && PFDay.now().isBefore(date)) { // No dates in the future accepted.
      validationErrors.add(ValidationError(translate("error.dateInFuture"), fieldId = "datum"))
    }
  }

  override val classicsLinkListUrl: String?
    get() = "wa/incomingMailList"

  /**
   * LAYOUT List page
   */
  override fun createListLayout(request: HttpServletRequest, magicFilter: MagicFilter): UILayout {
    val layout = super.createListLayout(request, magicFilter)
    val table = agGridSupport.prepareUIGrid4ListPage(request, layout, magicFilter, this)
    table.add(lc, "datum", "absender", "person", "inhalt", "bemerkung", "type")
    return LayoutUtils.processListPage(layout, this)
  }

  /**
   * LAYOUT Edit page
   */
  override fun createEditLayout(dto: PosteingangDO, userAccess: UILayout.UserAccess): UILayout {
    val sender = UIInput("absender", lc) // Input-field instead of text-area (length > 255)
    sender.focus = true
    sender.enableAutoCompletion(this)
    val person = UIInput("person", lc).enableAutoCompletion(this)
    val inhalt = UIInput("inhalt", lc).enableAutoCompletion(this)
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
      .add(sender)
      .add(person)
      .add(inhalt)
      .add(lc, "bemerkung")
    return LayoutUtils.processEditPage(layout, dto, this)
  }
}

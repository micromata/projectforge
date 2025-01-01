/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.skillmatrix

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDOPagesRest
import org.projectforge.ui.*
import org.projectforge.ui.filter.UIFilterElement
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/skillentry")
class SkillEntryPagesRest() : AbstractDOPagesRest<SkillEntryDO, SkillEntryDao>(
  SkillEntryDao::class.java,
  "plugins.skillmatrix.title",
  cloneSupport = CloneSupport.CLONE
) {
  /**
   * Initializes new memos for adding.
   */
  override fun newBaseDO(request: HttpServletRequest?): SkillEntryDO {
    val memo = super.newBaseDO(request)
    memo.owner = ThreadLocalUserContext.loggedInUser
    return memo
  }

  /**
   * LAYOUT List page
   */
  override fun createListLayout(request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess) {
    agGridSupport.prepareUIGrid4ListPage(
      request,
      layout,
      magicFilter,
      this,
      userAccess = userAccess,
    )
      .add(lc, "lastUpdate", "skill", "owner")
      .add(lc, "rating", formatter = UIAgGridColumnDef.Formatter.RATING)
      .add(lc, "interest", formatter = UIAgGridColumnDef.Formatter.RATING)
      .add(lc, "comment")
      .withMultiRowSelection(request, magicFilter)

    layout.add(
      MenuItem(
        "skillmatrix.export",
        i18nKey = "exportAsXls",
        url = "${SkillMatrixServicesRest.REST_EXCEL_EXPORT_PATH}",
        type = MenuItemTargetType.DOWNLOAD
      )
    )
  }

  override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
    elements.add(
      UIFilterElement(
        "owner",
        UIFilterElement.FilterType.BOOLEAN,
        translate("plugins.skillmatrix.filter.mySkills"),
        defaultFilter = true
      )
    )
  }

  override fun preProcessMagicFilter(
    target: QueryFilter,
    source: MagicFilter
  ): List<CustomResultFilter<SkillEntryDO>>? {
    val ownerFilterEntry = source.entries.find { it.field == "owner" }
    if (ownerFilterEntry?.isTrueValue == true) {
      ownerFilterEntry.synthetic = true
      target.createJoin("owner")
      target.add(QueryFilter.eq("owner", ThreadLocalUserContext.loggedInUser!!)) // Show only own skills, not from others.
    }
    return null
  }

  /**
   * Sets logged-in-user as owner.
   */
  override fun prepareClone(dto: SkillEntryDO): SkillEntryDO {
    val clone = super.prepareClone(dto)
    clone.owner = ThreadLocalUserContext.loggedInUser
    return clone
  }

  /**
   * LAYOUT Edit page
   */
  override fun createEditLayout(dto: SkillEntryDO, userAccess: UILayout.UserAccess): UILayout {
    val skillRating = UIRatingStars(
      "rating",
      lc,
      Array(SkillEntryDO.MAX_VAL_RATING + 1) { idx -> translate("plugins.skillmatrix.rating.$idx") },
      label = "plugins.skillmatrix.rating"
    )
    val interestRating = UIRatingStars(
      "interest",
      lc,
      Array(SkillEntryDO.MAX_VAL_INTEREST + 1) { idx -> translate("plugins.skillmatrix.interest.$idx") },
      label = "plugins.skillmatrix.interest"
    )
    val skill = UIInput("skill", lc).enableAutoCompletion(this)
    val layout = super.createEditLayout(dto, userAccess)
      .add(skill)
      .add(
        UIRow()
          .add(
            UICol(UILength(md = 4)).add(
              UIReadOnlyField(
                "owner.displayName",
                lc,
                label = "plugins.skillmatrix.owner"
              )
            )
          )
          .add(UICol(UILength(md = 4)).add(skillRating))
          .add(UICol(UILength(md = 4)).add(interestRating))
      )
      .add(lc, "comment")
    return LayoutUtils.processEditPage(layout, dto, this)
  }
}

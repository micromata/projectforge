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

package org.projectforge.plugins.datatransfer.rest

import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.User
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid

@RestController
@RequestMapping("${Rest.URL}/datatransferpersonalfiles")
class DataTransferPersonalBoxPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var userGroupCache: UserGroupCache

  @Autowired
  private lateinit var dataTransferAreaDao: DataTransferAreaDao

  @Autowired
  private lateinit var userPrefService: UserPrefService

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest, @RequestParam("userId") userIdString: String?): FormLayoutData {
    val userId = NumberHelper.parseLong(userIdString) ?: getUserPref().userId
    val dto = DataTransferPersonalBox()
    userId?.let {
      dto.user = User.getUser(userId)
    }
    val id = ensurePersonalBox(dto)
    return FormLayoutData(dto, getLayout(id, userId), createServerData(request))
  }

  /**
   * Will be called, if the user changes the user.
   */
  @PostMapping(RestPaths.WATCH_FIELDS)
  fun watchFields(@Valid @RequestBody postData: PostData<DataTransferPersonalBox>): ResponseEntity<ResponseAction> {
    val id =
      ensurePersonalBox(postData.data) ?: return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
    val userId = postData.data.user?.id
    getUserPref().userId = userId
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE).addVariable("data", postData.data)
        .addVariable("ui", getLayout(id, userId))
    )
  }

  private fun getLayout(id: Long?, userId: Long?): UILayout {
    val layout = UILayout("plugins.datatransfer.title.heading")
    layout.add(
      UIFieldset(title = "plugins.datatransfer.personalBox")
        .add(UIInput("user", label = "user", dataType = UIDataType.USER))
    )
    layout.add(
      UIButton.createBackButton(
        responseAction = ResponseAction(
          PagesResolver.getListPageUrl(
            DataTransferAreaPagesRest::class.java,
            absolute = true
          ), targetType = TargetType.REDIRECT
        )
      )
    )
    if (userId != null && userGroupCache.getUser(userId) != null) {
      layout.add(
        UIButton.createDefaultButton(
          "next",
          responseAction = ResponseAction(
            PagesResolver.getDynamicPageUrl(
              DataTransferPageRest::class.java,
              id = id,
              absolute = true
            ), targetType = TargetType.REDIRECT
          )
        )
      )
    }

    layout.watchFields.addAll(arrayOf("user"))

    LayoutUtils.process(layout)
    layout.addTranslations("tooltip.selectMe", "user")
    return layout
  }

  private fun ensurePersonalBox(dto: DataTransferPersonalBox): Long? {
    val userId = dto.user?.id ?: return null
    val pfUser = userGroupCache.getUser(userId) ?: return null
    val dbo = dataTransferAreaDao.ensurePersonalBox(userId)
    dto.id = dbo?.id ?: return null
    val user = User()
    user.copyFromMinimal(pfUser)
    dto.user = user
    return dbo.id
  }

  class PersonalBoxUserPref(var userId: Long? = null)

  private fun getUserPref(): PersonalBoxUserPref {
    return userPrefService.ensureEntry("datatransfer", "personalbox", PersonalBoxUserPref())
  }
}

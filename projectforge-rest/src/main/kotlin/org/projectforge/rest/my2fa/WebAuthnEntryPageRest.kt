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

package org.projectforge.rest.my2fa

import org.projectforge.framework.i18n.translate
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.security.WebAuthnServicesRest
import org.projectforge.security.dto.WebAuthnEntry
import org.projectforge.security.dto.WebAuthnFinishRequest
import org.projectforge.security.webauthn.WebAuthnEntryDao
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

/**
 * Modal dialog for registering a new token or modifying/deleting an existing one.
 */
@RestController
@RequestMapping("${Rest.URL}/WebAuthnSetup")
class WebAuthnEntryPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var webAuthnEntryDao: WebAuthnEntryDao

  @Autowired
  private lateinit var webAuthnServicesRest: WebAuthnServicesRest

  /**
   * @param id PK of the data base entry.
   */
  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest, @RequestParam("id") id: Int?): FormLayoutData {
    val data = if (id != null) {
      WebAuthnEntry.create(webAuthnEntryDao.getEntryById(id))
    } else {
      WebAuthnEntry()
    }
    val layout = UILayout("webauthn.entry.edit")
    layout
      .add(
        UIInput(
          "displayName",
          label = "webauthn.entry.displayName",
          tooltip = "webauthn.entry.displayName.info"
        )
      )
    layout.addAction(UIButton.createCancelButton(responseAction = ResponseAction(targetType = TargetType.CLOSE_MODAL)))
    if (id == null) {
      // New entry
      layout.add(
        UICustomized(
          "webauthn.register",
          mutableMapOf(
            "registerFinishUrl" to RestResolver.getRestUrl(this::class.java, "registerFinish")
          )
        )
      )
      WebAuthnServicesRest.addRegisterTranslations(layout)
    } else {
      layout.addAction(
        UIButton.createDefaultButton(
          "update",
          title = "update",
          responseAction = ResponseAction(
            RestResolver.getRestUrl(this::class.java, "update"),
            targetType = TargetType.POST
          ),
        )
      )
      layout.addAction(
        UIButton.createDeleteButton(
          layout,
          responseAction = ResponseAction(
            RestResolver.getRestUrl(this::class.java, "delete"),
            targetType = TargetType.POST
          ),
        )
      )
    }


    layout.addTranslations("cancel", "yes")
    LayoutUtils.process(layout)

    return FormLayoutData(data, layout, createServerData(request))
  }

  @PostMapping("registerFinish")
  fun registerFinish(
    request: HttpServletRequest,
    @RequestBody postData: PostData<WebAuthnFinishRequest>
  ): ResponseEntity<ResponseAction> {
    val result = webAuthnServicesRest.doRegisterFinish(request, postData)
    if (result.success) {
      return UIToast.createToastResponseEntity(
        translate("user.My2FA.setup.check.success"),
        color = UIColor.SUCCESS,
        mutableMapOf("data" to postData.data),
        merge = true,
        targetType = TargetType.UPDATE
      )
    }
    // Authentication wasn't successful:
    result.errorMessage!!.let { msg ->
      return UIToast.createToastResponseEntity(
        translate(msg),
        color = UIColor.DANGER,
      )
    }
  }


}

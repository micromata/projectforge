/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.rest.core.PagesResolver
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
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

/**
 * Dialog for registering a new token or modifying/deleting an existing one.
 */
@RestController
@RequestMapping("${Rest.URL}/WebAuthnSetup")
class WebAuthnEntryPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var my2FASetupMenuBadge: My2FASetupMenuBadge

  @Autowired
  private lateinit var webAuthnEntryDao: WebAuthnEntryDao

  @Autowired
  private lateinit var webAuthnServicesRest: WebAuthnServicesRest

  /**
   * @param id PK of the data base entry.
   */
  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest, @RequestParam("id") idString: String?): FormLayoutData {
    val id = idString?.toLongOrNull()
    val data = if (id != null) {
      WebAuthnEntry.create(webAuthnEntryDao.getEntryById(id))
    } else {
      WebAuthnEntry(displayName = translate("untitled"))
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
    data.signCount?.let {
      if (it > 0) {
        layout.add(
          UIReadOnlyField(
            "signCount",
            label = "webauthn.entry.signCount",
            tooltip = "webauthn.entry.signCount.info"
          )
        )
      }
    }
    layout.addAction(
      UIButton.createCancelButton(
        responseAction = ResponseAction(
          url = callerUrl,
          targetType = TargetType.REDIRECT,
        )
      )
    )
    if (id == null) {
      // New entry
      layout.add(
        UICustomized(
          "webauthn.register",
          mutableMapOf(
            "registerFinishUrl" to RestResolver.getRestUrl(this::class.java, "registerFinish"),
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
            targetType = TargetType.POST,
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
    @RequestBody postData: PostData<MyWebAuthnEntry>
  ): ResponseEntity<ResponseAction> {
    val data = postData.data
    val webAuthnFinishRequest = data.webAuthnFinishRequest
    // Not needed (no replay attack possible)
    // sessionCsrfService.validateCsrfToken(request, postData)?.let { return it }
    requireNotNull(webAuthnFinishRequest)
    val result = webAuthnServicesRest.doRegisterFinish(request, webAuthnFinishRequest, displayName = data.displayName)
    if (result.success) {
      my2FASetupMenuBadge.refreshUserBadgeCounter()
      return ResponseEntity.ok(redirectToSetupPage())
    }
    // Authentication wasn't successful:
    result.errorMessage!!.let { msg ->
      return ValidationError.createResponseEntity(msg)
    }
  }

  @PostMapping("delete")
  fun delete(
    request: HttpServletRequest,
    response: HttpServletResponse,
    @RequestBody postData: PostData<MyWebAuthnEntry>
  ): ResponseAction {
    val id = postData.data.id
    requireNotNull(id) { "Can't delete WebAuthn entry without id." }
    webAuthnEntryDao.delete(id)
    my2FASetupMenuBadge.refreshUserBadgeCounter()
    return redirectToSetupPage()
  }

  /**
   * Updates only the displayName.
   */
  @PostMapping("update")
  fun update(@RequestBody postData: PostData<MyWebAuthnEntry>): ResponseAction {
    val id = postData.data.id
    requireNotNull(id) { "Can't update WebAuthn entry without id." }
    val entry = webAuthnEntryDao.getEntryById(id)
    entry.displayName = postData.data.displayName
    webAuthnEntryDao.upsert(entry)
    return redirectToSetupPage()
  }

  private fun redirectToSetupPage(): ResponseAction {
    return ResponseAction(
      callerUrl,
      targetType = TargetType.REDIRECT
    )
  }

  private val callerUrl: String = PagesResolver.getDynamicPageUrl(My2FASetupPageRest::class.java, absolute = true)

  /**
   * Only used for [registerFinish].
   */
  class MyWebAuthnEntry : WebAuthnEntry() {
    var webAuthnFinishRequest: WebAuthnFinishRequest? = null
  }
}

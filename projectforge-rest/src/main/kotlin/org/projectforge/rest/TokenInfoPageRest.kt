/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest

import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.UserAuthenticationsDO
import org.projectforge.rest.calendar.BarcodeServicesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.pub.AuthenticationPublicServicesRest
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/tokenInfo")
class TokenInfoPageRest : AbstractDynamicPageRest() {
    @Autowired
    private lateinit var authenticationsService: UserAuthenticationsService

    @Autowired
    private lateinit var authenticationPublicServicesRest: AuthenticationPublicServicesRest

    class TokenInfoData(var info: String? = null,
                        val token: String? = null)

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest, @RequestParam("token") token: UserTokenType): FormLayoutData {
        val userId = ThreadLocalUserContext.getUserId()

        val data = TokenInfoData(authenticationsService.getUserAccessLogEntries(token, userId)?.asText("\n\n"),
                authenticationsService.getToken(userId, token))

        val layout = UILayout("user.authenticationToken.button.showUsage")

        val tokenId = when (token) {
            UserTokenType.CALENDAR_REST -> "calendarExportToken"
            UserTokenType.DAV_TOKEN -> "davToken"
            UserTokenType.REST_CLIENT -> "restClientToken"
            UserTokenType.STAY_LOGGED_IN_KEY -> "stayLoggedInKey"
        }

        val elementInfo = ElementsRegistry.getElementInfo(UserAuthenticationsDO::class.java, tokenId)

        layout
                .add(UIReadOnlyField("token",
                        label = elementInfo?.i18nKey,
                        canCopy = true, coverUp = true,
                        tooltip = elementInfo?.tooltipI18nKey))
                .add(UIReadOnlyField(id = "info", label = "user.authenticationToken.button.showUsage.tooltip"))

        layout.addAction(UIButton("${token}-cancel",
                title = translate("cancel"),
                color = UIColor.SECONDARY,
                outline = true,
                responseAction = ResponseAction(targetType = TargetType.CLOSE_MODAL)))

        if (token == UserTokenType.REST_CLIENT) {
            val queryURL = authenticationPublicServicesRest.createQueryURL()
            val barcodeUrl = BarcodeServicesRest.getBarcodeGetUrl(queryURL)
            layout.add(UIFieldset(UILength(md = 12, lg = 12))
                    .add(UIRow()
                            .add(UICol()
                                    .add(UICustomized("image", mutableMapOf("src" to barcodeUrl, "alt" to barcodeUrl))))))
        }

        layout.addTranslations("cancel", "yes")
        LayoutUtils.process(layout)

        return FormLayoutData(data, layout, createServerData(request))
    }

}

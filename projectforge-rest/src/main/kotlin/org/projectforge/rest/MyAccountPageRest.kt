/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.projectforge.Const
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserAuthenticationsDO
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/myAccount")
class MyAccountPageRest {
    @Autowired
    private lateinit var authenticationsService: UserAuthenticationsService

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userDao: UserDao

    class MyAccountData(
            var username: String? = null,
            var firstname: String? = null,
            var lastname: String? = null,
            var calendarExportToken: String? = null,
            var davToken: String? = null,
            var restClientToken: String? = null,
            var stayLoggedInKey: String? = null,
            var groups: String? = null,
            var lastLogin: String? = null,
            var locale: Locale? = null
    )

    @GetMapping("dynamic")
    fun getForm(): FormLayoutData {
        val userId = ThreadLocalUserContext.getUserId()
        val user = userDao.getById(userId)
        val data = MyAccountData(user.username, user.firstname, user.lastname)

        val layout = UILayout("user.myAccount.title.edit")
        val userLC = LayoutContext(PFUserDO::class.java)
        val dataLC = LayoutContext(MyAccountData::class.java)
        val authenticationsLC = LayoutContext(UserAuthenticationsDO::class.java)
        data.calendarExportToken = authenticationsService.getToken(userId, UserTokenType.CALENDAR_REST)
        data.davToken = authenticationsService.getToken(userId, UserTokenType.DAV_TOKEN)
        data.restClientToken = authenticationsService.getToken(userId, UserTokenType.REST_CLIENT)
        data.stayLoggedInKey = authenticationsService.getToken(userId, UserTokenType.STAY_LOGGED_IN_KEY)
        data.groups = groupService.getGroupnames(userId)
        data.locale = ThreadLocalUserContext.getLocale() ?: Locale("DEFAULT")

        data.lastLogin = DateTimeFormatter.instance().getFormattedDateTime(user.lastLogin)

        val locales = Const.LOCALIZATIONS.map { UISelectValue(Locale(it), translate("locale.$it")) }.toMutableList()
        locales.add(0, UISelectValue(Locale("DEFAULT"), translate("user.defaultLocale")))

        layout.add(UIRow()
                        .add(UIFieldset(UILength(lg = 6))
                                .add(UIReadOnlyField("username", userLC))
                                .add(UIInput("firstname", userLC))
                                .add(UIInput("lastname", userLC))
                                .add(addAuthenticationToken(authenticationsLC, "stayLoggedInKey"))
                                .add(addAuthenticationToken(authenticationsLC, "calendarExportToken"))
                                .add(addAuthenticationToken(authenticationsLC, "davToken"))
                                .add(addAuthenticationToken(authenticationsLC, "restClientToken"))
                        )
                        .add(UIFieldset(UILength(lg = 6))
                                .add(UIReadOnlyField("lastLogin", dataLC, label = "login.lastLogin"))
                                .add(UISelect("locale", dataLC, label = "user.locale", required = true, values = locales))
                        ))
                .add(UIFieldset(12)
                        .add(UIReadOnlyField("groups", dataLC, label = "user.assignedGroups"))
                )

        // Datumsformat
        //Exceldatumsformat
        //Uhrzeitformat
        //Zeitzone
        //Telefonkennungen
        //MEB Mobilfunknummern

        // Kalender Whitelist für Kalendersoftware

        // Beschreibung
        // SSH public key
        LayoutUtils.process(layout)

        // Passwort ändern
        //WLAN/Samba Passwort ändern

        return FormLayoutData(data, layout, null)
    }

    private fun addAuthenticationToken(lc: LayoutContext, id: String): UIRow {
        return UIRow()
                .add(UICol(9)
                        .add(UIReadOnlyField(id, lc, canCopy = true, coverUp = true, ignoreTooltip = true))
                )
                .add(UICol(3)
                        .add(UIButton("renew",
                                title = translate("user.authenticationToken.renew"),
                                confirmMessage = translate("user.authenticationToken.renew.securityQuestion"),
                                color = UIColor.DANGER,
                                responseAction = ResponseAction("/rs/renew", targetType = TargetType.TOAST)))
                        .add(UIButton("usage",
                                title = translate("user.authenticationToken.button.showUsage"),
                                color = UIColor.LINK,
                                responseAction = ResponseAction("/rs/usage", targetType = TargetType.POST)))
                )
    }
}

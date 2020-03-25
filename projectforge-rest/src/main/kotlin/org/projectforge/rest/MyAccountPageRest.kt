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
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.api.EmployeeService
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserAuthenticationsDO
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.time.TimeNotation
import org.projectforge.rest.config.Rest
import org.projectforge.rest.dto.Employee
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/myAccount")
class MyAccountPageRest {
    @Autowired
    private lateinit var authenticationsService: UserAuthenticationsService

    @Autowired
    private lateinit var employeeService: EmployeeService

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
            var locale: Locale? = null,
            var dateFormat: String? = null,
            var excelDateFormat: String? = null,
            var timeNotation: TimeNotation? = null,
            var timeZone: TimeZone? = null,
            var personalPhoneIdentifiers: String? = null,
            var sshPublicKey: String? = null
    ) {
        val employee = Employee()
    }

    @GetMapping("dynamic")
    fun getForm(): FormLayoutData {
        val userId = ThreadLocalUserContext.getUserId()
        val user = userDao.getById(userId)
        val data = MyAccountData(user.username, user.firstname, user.lastname)

        val layout = UILayout("user.myAccount.title.edit")
        val dataLC = LayoutContext(MyAccountData::class.java)
        val employeeLC = LayoutContext(EmployeeDO::class.java)
        val userLC = LayoutContext(PFUserDO::class.java)
        val authenticationsLC = LayoutContext(UserAuthenticationsDO::class.java)
        data.calendarExportToken = authenticationsService.getToken(userId, UserTokenType.CALENDAR_REST)
        data.davToken = authenticationsService.getToken(userId, UserTokenType.DAV_TOKEN)
        data.restClientToken = authenticationsService.getToken(userId, UserTokenType.REST_CLIENT)
        data.stayLoggedInKey = authenticationsService.getToken(userId, UserTokenType.STAY_LOGGED_IN_KEY)
        data.groups = groupService.getGroupnames(userId)
        data.locale = ThreadLocalUserContext.getLocale() ?: Locale("DEFAULT")
        data.dateFormat = user.dateFormat
        data.excelDateFormat = user.excelDateFormat
        data.timeNotation = user.timeNotation
        data.timeZone = user.timeZone
        data.personalPhoneIdentifiers = user.personalPhoneIdentifiers
        data.sshPublicKey = user.sshPublicKey

        data.lastLogin = DateTimeFormatter.instance().getFormattedDateTime(user.lastLogin)

        val employee = employeeService.getEmployeeByUserId(userId)
        data.employee.let {
            it.street = employee.street
            it.zipCode = employee.zipCode
            it.country = employee.country
            it.state = employee.state
            it.birthday = employee.birthday
            it.accountHolder = employee.accountHolder
            it.iban = employee.iban
            it.bic = employee.bic
        }

        val locales = Const.LOCALIZATIONS.map { UISelectValue(Locale(it), translate("locale.$it")) }.toMutableList()
        locales.add(0, UISelectValue(Locale("DEFAULT"), translate("user.defaultLocale")))

        val today = LocalDate.now()
        val formats = Configuration.getInstance().dateFormats
        val dateFormats = formats.map { createUISelectValue(it, today) }.toMutableList()
        val excelDateFormats = formats.map { createUISelectValue(it, today, true) }.toMutableList()

        val timeNotations = listOf(
                UISelectValue(TimeNotation.H12, translate("timeNotation.12")),
                UISelectValue(TimeNotation.H24, translate("timeNotation.24"))
        )

        layout.add(UIFieldset(12)
                .add(UIRow()
                        .add(UICol(UILength(lg = 6))
                                .add(UIReadOnlyField("username", userLC))
                                .add(userLC, "firstname", "lastname")
                                .add(addAuthenticationToken(authenticationsLC, "stayLoggedInKey", "login.stayLoggedIn.invalidateAllStayLoggedInSessions.tooltip"))
                                .add(addAuthenticationToken(authenticationsLC, "calendarExportToken"))
                                .add(addAuthenticationToken(authenticationsLC, "davToken"))
                                .add(addAuthenticationToken(authenticationsLC, "restClientToken"))
                        )
                        .add(UICol(UILength(lg = 6))
                                .add(UIReadOnlyField("lastLogin", userLC))
                                .add(UISelect("locale", userLC, required = true, values = locales))
                                .add(UISelect("dateFormat", userLC, required = false, values = dateFormats))
                                .add(UISelect("excelDateFormat", userLC, required = false, values = excelDateFormats))
                                .add(UISelect("timeNotation", userLC, required = false, values = timeNotations))
                                .add(userLC, "timeZone", "personalPhoneIdentifiers")
                        )
                )
        )
                .add(UIFieldset(12, "fibu.employee")
                        .add(UIRow()
                                .add(UICol(UILength(md = 4)).add(employeeLC, "employee.street", "employee.zipCode", "employee.city"))
                                .add(UICol(UILength(md = 4)).add(employeeLC, "employee.country", "employee.state", "employee.birthday"))
                                .add(UICol(UILength(md = 4)).add(employeeLC, "employee.accountHolder", "employee.iban", "employee.bic"))
                        )
                )
                .add(UIFieldset(12)
                        .add(UIReadOnlyField("groups", label = "user.assignedGroups"))
                )
                .add(UIFieldset(12)
                        .add(UITextArea("sshPublicKey", userLC))
                )
        /*
            teamCalCache.allFullAccessCalendars
            val teamCalBlackListIds: Array<Int> = userXmlPreferencesDao
                    .getDeserializedUserPreferencesByUserId(userId, TEAMCALRESTBLACKLIST, Array<Int>::class.java)
            if (teamCalBlackListIds != null && teamCalBlackListIds.size > 0) {
                Arrays.stream(teamCalBlackListIds).forEach { calId: Int? -> teamCalRestWhiteList.remove(teamCalCache.getCalendar(calId)) }
            }
            val calendars: org.wicketstuff.select2.Select2MultiChoice<TeamCalDO> = org.wicketstuff.select2.Select2MultiChoice(
                    fieldSet.getSelect2MultiChoiceId(),
                    org.apache.wicket.model.PropertyModel(this, "teamCalRestWhiteList"),
                    TeamCalsProvider(teamCalCache, true))
            calendars.setMarkupId("calenders").setOutputMarkupId(true)
            fieldSet.add(calendars)
            */

        LayoutUtils.process(layout)

        // Passwort ändern
        //WLAN/Samba Passwort ändern

        return FormLayoutData(data, layout, null)
    }

    private fun createUISelectValue(pattern: String, today: LocalDate, excelDateFormat: Boolean = false): UISelectValue<String> {
        val str = if (excelDateFormat) {
            pattern.replace('Y', 'y').replace('D', 'd')
        } else {
            pattern
        }
        return UISelectValue(pattern, "$str: ${java.time.format.DateTimeFormatter.ofPattern(pattern).format(today)}")
    }

    private fun addAuthenticationToken(lc: LayoutContext, id: String, tooltip: String? = null): UIRow {
        return UIRow()
                .add(UICol(9)
                        .add(UIReadOnlyField(id, lc, canCopy = true, coverUp = true, ignoreTooltip = true))
                )
                .add(UICol(3)
                        .add(UIButton("renew",
                                title = translate("user.authenticationToken.renew"),
                                tooltip = tooltip ?: "user.authenticationToken.renew.tooltip",
                                confirmMessage = translate("user.authenticationToken.renew.securityQuestion"),
                                color = UIColor.DANGER,
                                responseAction = ResponseAction("/rs/renew", targetType = TargetType.TOAST)))
                        .add(UIButton("usage",
                                title = translate("user.authenticationToken.button.showUsage"),
                                tooltip = "user.authenticationToken.button.showUsage.tooltip",
                                color = UIColor.LINK,
                                responseAction = ResponseAction("/rs/usage", targetType = TargetType.POST)))
                )
    }
}

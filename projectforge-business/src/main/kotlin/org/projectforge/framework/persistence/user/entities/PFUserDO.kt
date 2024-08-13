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

package org.projectforge.framework.persistence.user.entities

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.persistence.api.IUserRightId
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.history.HibernateSearchPhoneNumberBridge
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.time.TimeNotation
import java.time.DayOfWeek
import java.util.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PF_USER", uniqueConstraints = [UniqueConstraint(columnNames = ["username"])])
@NamedQueries(
    NamedQuery(
        name = PFUserDO.FIND_BY_USERNAME,
        query = "from PFUserDO where username=:username"
    ),
    NamedQuery(
        name = PFUserDO.FIND_OTHER_USER_BY_USERNAME,
        query = "from PFUserDO where username=:username and id<>:id"
    )
)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
open class PFUserDO : DefaultBaseDO(), DisplayNameCapable {

    override val displayName: String
        @Transient
        get() = getFullname()

    @Transient
    private var attributeMap: MutableMap<String, Any>? = null

    /**
     * The unique username.
     */
    @PropertyInfo(i18nKey = "user.username")
    @FullTextField
    @get:Column(length = 255, nullable = false)
    open var username: String? = null

    /**
     * Timesamp of the lastPasswordChange.
     */
    @PropertyInfo(i18nKey = "user.changePassword.password.lastChange")
    @get:Column(name = "last_password_change")
    open var lastPasswordChange: Date? = null

    @PropertyInfo(i18nKey = "user.changeWlanPassword.lastChange")
    @get:Column(name = "last_wlan_password_change")
    open var lastWlanPasswordChange: Date? = null

    /**
     * JIRA user name (if differ from the ProjectForge's user name).
     */
    @PropertyInfo(i18nKey = "user.jiraUsername", tooltip = "user.jiraUsername.tooltip")
    @FullTextField
    @get:Column(name = "jira_username", length = 100)
    open var jiraUsername: String? = null

    /**
     * A local user will not be synchronized with any external user management system.
     */
    @PropertyInfo(i18nKey = "user.localUser", tooltip = "user.localUser.tooltip")
    @get:Column(name = "local_user", nullable = false)
    open var localUser: Boolean = false

    /**
     * A restricted user has only the ability to log-in and to change his password. This is useful if ProjectForge runs in
     * master mode for managing an external LDAP system. Then this user is a LDAP user but has no other functionality than
     * change password in the ProjectForge system itself.
     */
    @PropertyInfo(i18nKey = "user.restrictedUser", tooltip = "user.restrictedUser.tooltip")
    @get:Column(name = "restricted_user", nullable = false)
    open var restrictedUser: Boolean = false

    /**
     * A deactivated user has no more system access.
     */
    @PropertyInfo(i18nKey = "user.deactivated")
    @get:Column(nullable = false)
    open var deactivated: Boolean = false

    @PropertyInfo(i18nKey = "firstName")
    @FullTextField
    @get:Column(length = 255)
    open var firstname: String? = null

    @PropertyInfo(i18nKey = "nickname")
    @FullTextField
    @get:Column(name = "nick_name", length = 255)
    open var nickname: String? = null

    @PropertyInfo(i18nKey = "name")
    @FullTextField
    @get:Column(length = 255)
    open var lastname: String? = null

    @PropertyInfo(i18nKey = "gender")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "gender", length = 100)
    open var gender: Gender? = null

    /**
     * Optional description of the user.
     */
    @PropertyInfo(i18nKey = "description")
    @FullTextField
    @get:Column(length = 255)
    open var description: String? = null

    /**
     * Die E-Mail Adresse des Benutzers, falls vorhanden.
     */
    @PropertyInfo(i18nKey = "email")
    @FullTextField
    @get:Column(length = 255)
    open var email: String? = null

    @PropertyInfo(i18nKey = "user.mobilePhone", tooltip = "user.mobilePhone.info")
    @FieldBridge(impl = HibernateSearchPhoneNumberBridge::class)
    @FullTextField
    @get:Column(name = "mobile_phone", length = 255)
    open var mobilePhone: String? = null

    /**
     * Zeitstempel des letzten erfolgreichen Logins.
     */
    @PropertyInfo(i18nKey = "login.lastLogin")
    //@field:NoHistory
    @get:Column
    open var lastLogin: Date? = null

    /**
     * Die Anzahl der erfolglosen Logins. Dieser Wert wird bei dem nächsten erfolgreichen Login auf 0 zurück gesetzt.
     */
    //@field:NoHistory
    @get:Column
    open var loginFailures: Int = 0

    @PropertyInfo(i18nKey = "user.locale")
    @get:Column
    open var locale: Locale? = null

    /**
     * Ensures time zone. If no time zone is given for the user, the configured default time zone is returned.
     * @see Configuration.getDefaultTimeZone
     */
    private var _timeZone: TimeZone? = null

    @get:PropertyInfo(i18nKey = "timezone")
    @get:Transient
    var timeZone: TimeZone
        get() = _timeZone ?: Configuration.instance.defaultTimeZone
        set(value) {
            _timeZone = value
        }

    /**
     * For example "Europe/Berlin" if time zone is given otherwise empty string.
     */
    @get:PropertyInfo(i18nKey = "timezone")
    @get:Column(name = "time_zone")
    open var timeZoneString: String?
        get() = _timeZone?.id
        set(value) {
            if (!value.isNullOrBlank()) {
                _timeZone = TimeZone.getTimeZone(value)
            } else {
                _timeZone = null
            }
        }

    /**
     * For example "Europe/Berlin" if time zone is given otherwise empty string.
     */
    val timeZoneDisplayName: String
        @Transient
        get() = timeZone.displayName

    val dateTimeZone: PFDateTime
        @Transient
        get() = PFDateTime.now()

    /**
     * The locale given from the client (e. g. from the browser by the http request). This locale is needed by
     * ThreadLocalUserContext for getting the browser locale if the user's locale is null and the request's locale is not
     * available.
     */
    @get:Transient
    open var clientLocale: Locale? = null

    /**
     * Default date format for the user. Examples:
     *
     *  * yyyy-MM-dd: 2011-02-21, ISO format.
     *  * dd.MM.yyyy: 21.02.2011, German format (day of month first)
     *  * dd/MM/yyyy: 21/02/2011, British and French format (day of month first)
     *  * MM/dd/yyyy: 02/21/2011, American format (month first)
     */
    @PropertyInfo(i18nKey = "dateFormat")
    @get:Column(name = "date_format", length = 20)
    open var dateFormat: String? = null

    /**
     * Default excel date format for the user. Examples:
     *
     *  * DD.MM.YYYY: 21.02.2011, German format (day of month first)
     *  * DD/MM/YYYY: 21/02/2011, British and French format (day of month first)
     *  * MM/DD/YYYY: 02/21/2011, American format (month first)
     */
    @PropertyInfo(i18nKey = "dateFormat.xls")
    @get:Column(name = "excel_date_format", length = 20)
    open var excelDateFormat: String? = null

    /**
     * 1 - monday, ..., 6 - saturday, 7 - sunday
     */
    @get:Column(name = "first_day_of_week")
    open var firstDayOfWeekValue: Int? = null

    open var firstDayOfWeek: DayOfWeek?
        @Transient
        get() = PFDayUtils.getISODayOfWeek(firstDayOfWeekValue)
        set(value) {
            firstDayOfWeekValue = PFDayUtils.getISODayOfWeekValue(value)
        }

    @PropertyInfo(i18nKey = "timeNotation")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "time_notation", length = 6)
    open var timeNotation: TimeNotation? = null

    @PropertyInfo(i18nKey = "organization")
    @FullTextField
    @get:Column(length = 255)
    open var organization: String? = null

    /**
     * Eine kommaseparierte Liste mit den Kennungen des/der Telefon(e) des Mitarbeiters an der unterstützten
     * Telefonanlage,  zur Direktwahl aus ProjectForge heraus.
     */
    @PropertyInfo(i18nKey = "user.personalPhoneIdentifiers", tooltip = "user.personalPhoneIdentifiers.tooltip.content")
    @get:Column(name = "personal_phone_identifiers", length = 255)
    open var personalPhoneIdentifiers: String? = null

    @PropertyInfo(i18nKey = "access.rights")
    @get:OneToMany(fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "user")
    //@field:NoHistory
    open var rights: MutableSet<UserRightDO>? = HashSet()

    /**
     * If true (default) then the user is highlighted in the human resource planning page if not planned for the actual
     * week.
     */
    @PropertyInfo(i18nKey = "user.hrPlanningEnabled")
    @get:Column(name = "hr_planning", nullable = false)
    open var hrPlanning = true

    /**
     * LDAP values as key-value-pairs, e. g. gidNumber=1000,uidNumber=1001,homeDirectory="/home/kai",shell="/bin/bash".
     * For handling of the values as xmk see [org.projectforge.business.ldap.PFUserDOConverter]. This field is handled by the
     * ldap package and has no further effect in ProjectForge's core package.
     */
    @PropertyInfo(i18nKey = "user.ldapValues")
    @get:Column(name = "ldap_values", length = 4000)
    open var ldapValues: String? = null

    /**
     * The user's sshPublicKey, if any.
     */
    @PropertyInfo(i18nKey = "user.sshPublicKey")
    @get:Column(name = "ssh_public_key", length = 4096)
    open var sshPublicKey: String? = null

    /**
     * The user's gpgPublicKey, if any.
     */
    @PropertyInfo(i18nKey = "user.gpgPublicKey")
    @get:Column(name = "gpg_public_key", length = 4096)
    open var gpgPublicKey: String? = null

    /**
     * Gibt den Vor- und Nachnamen zurück, falls gegeben. Vor- und Nachname sind durch ein Leerzeichen getrennt.
     *
     * @return first name and last name, separated by space, or username if neither last name nor first name is given.
     */
    @Transient
    fun getFullname(): String {
        val first = this.firstname
        val last = this.lastname
        return if (first.isNullOrBlank()) {
            if (last.isNullOrBlank()) this.username ?: ""
            else last
        } else {
            if (last.isNullOrBlank()) first
            else "$first $last"
        }
    }

    /**
     * @return The JIRA user name or if not given the user name (assuming that the JIRA user name is same as ProjectForge
     * user name).
     */
    val jiraUsernameOrUsername: String?
        @Transient
        get() = if (jiraUsername.isNullOrBlank()) this.username else this.jiraUsername

    val userDisplayName: String?
        @Transient
        get() {
            val str = getFullname()
            return if (str.isBlank() || str.equals(this.username))
                this.username
            else
                "$str (${this.username})"
        }

    override fun equals(other: Any?): Boolean {
        if (other is PFUserDO) {
            return Objects.equals(this.username, other.username)
        }
        return false
    }

    override fun hashCode(): Int {
        return this.username?.hashCode() ?: 0
    }

    /**
     * Do nothing.
     *
     * @see org.projectforge.framework.persistence.api.ExtendedBaseDO.recalculate
     */
    override fun recalculate() {}

    override fun getTransientAttribute(key: String): Any? {
        return if (attributeMap == null) {
            null
        } else attributeMap!![key]
    }

    override fun setTransientAttribute(key: String, value: Any?) {
        synchronized(this) {
            if (value == null) {
                removeAttribute(key)
                return
            }
            if (attributeMap == null) {
                attributeMap = HashMap()
            }
            attributeMap!!.put(key, value)
        }
    }

    fun removeAttribute(key: String) {
        if (attributeMap == null) {
            return
        }
        attributeMap!!.remove(key)
    }


    /**
     * @return this for chaining.
     */
    fun addRight(right: UserRightDO): PFUserDO {
        if (this.rights == null) {
            this.rights = HashSet()
        }
        this.rights!!.add(right)
        right.setUser(this)
        return this
    }

    @Transient
    fun getRight(rightId: IUserRightId): UserRightDO? {
        if (this.rights == null) {
            return null
        }
        for (right in this.rights!!) {
            if (right.rightIdString == rightId.id) {
                return right
            }
        }
        return null
    }

    /**
     * @return true only and only if the user isn't either deleted nor deactivated, otherwise false.
     */
    @Transient
    fun hasSystemAccess(): Boolean {
        return !deleted && !this.deactivated
    }

    companion object {
        const val FIND_BY_USERNAME = "PFUserDO_FindByUsername"

        /**
         * For detecting the existing of given username in the database for other user than given. Avoids duplicate usernames.
         */
        internal const val FIND_OTHER_USER_BY_USERNAME = "PFUserDO_FindOtherUserByUsername"

        /**
         * Converts user list to comma separated int values.
         * @return String with csv or null, if list of user's is null.
         */
        fun toIntList(users: List<PFUserDO>?): String? {
            return users?.filter { it.id != null }?.joinToString { "${it.id}" }
        }

        /**
         * @return A copy of the given user without copying the secret fields (password, passwordSalt).
         */
        @JvmStatic
        fun createCopy(srcUser: PFUserDO?): PFUserDO? {
            if (srcUser == null)
                return null
            val user = PFUserDO()
            user.copyValuesFrom(srcUser)
            return user
        }
    }
}

/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import de.micromata.genome.db.jpa.history.api.NoHistory
import de.micromata.genome.jpa.metainf.EntityDependencies
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.joda.time.DateTimeZone
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.IUserRightId
import org.projectforge.framework.persistence.api.ModificationStatus
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.time.TimeNotation
import java.io.Serializable
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PF_USER", uniqueConstraints = [UniqueConstraint(columnNames = ["username"])], indexes = [Index(name = "idx_fk_t_pf_user_tenant_id", columnList = "tenant_id")])
@EntityDependencies(referencedBy = [TenantDO::class])
@NamedQueries(
        NamedQuery(name = PFUserDO.FIND_BY_USERNAME_AND_STAYLOGGEDINKEY,
                query = "from PFUserDO where username=:username and stayLoggedInKey=:stayLoggedInKey"),
        NamedQuery(name = PFUserDO.FIND_BY_USERNAME,
                query = "from PFUserDO where username=:username"),
        NamedQuery(name = PFUserDO.FIND_OTHER_USER_BY_USERNAME,
                query = "from PFUserDO where username=:username and id<>:id"),
        NamedQuery(name = PFUserDO.FIND_BY_USERID_AND_AUTHENTICATIONTOKEN,
                query = "from PFUserDO where id=:id and authenticationToken=:authenticationToken"),
        NamedQuery(name = PFUserDO.SELECT_ID_MEB_MOBILE_NUMBERS,
                query = "select id, personalMebMobileNumbers from PFUserDO where deleted=false and personalMebMobileNumbers is not null"))
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
open class PFUserDO : DefaultBaseDO(), ShortDisplayNameCapable {

    @Transient
    private var attributeMap: MutableMap<String, Any>? = null

    /**
     * The unique username.
     */
    @PropertyInfo(i18nKey = "user.username")
    @Field
    @get:Column(length = 255, nullable = false)
    open var username: String? = null

    /**
     * JIRA user name (if differ from the ProjectForge's user name) is used e. g. in MEB for creating new issues.
     */
    @PropertyInfo(i18nKey = "user.jiraUsername")
    @Field
    @get:Column(name = "jira_username", length = 100)
    open var jiraUsername: String? = null

    /**
     * Encoded password of the user (SHA-1).
     */
    @PropertyInfo(i18nKey = "password")
    @JsonIgnore
    @field:NoHistory
    @get:Column(length = 50)
    open var password: String? = null

    /**
     * Timesamp of the lastPasswordChange.
     */
    @get:Column(name = "last_password_change")
    open var lastPasswordChange: Date? = null

    @get:Column(name = "last_wlan_password_change")
    open var lastWlanPasswordChange: Date? = null

    /**
     * A local user will not be synchronized with any external user management system.
     */
    @PropertyInfo(i18nKey = "user.localUser")
    @get:Column(name = "local_user", nullable = false)
    open var localUser: Boolean = false

    /**
     * A restricted user has only the ability to log-in and to change his password. This is useful if ProjectForge runs in
     * master mode for managing an external LDAP system. Then this user is a LDAP user but has no other functionality than
     * change password in the ProjectForge system itself.
     */
    @get:Column(name = "restricted_user", nullable = false)
    open var restrictedUser: Boolean = false

    /**
     * A deactivated user has no more system access.
     */
    @PropertyInfo(i18nKey = "user.activated")
    @get:Column(nullable = false)
    open var deactivated: Boolean = false

    /**
     * A super admin is able to administer tenants. For tenants the user must be assigned to PF_Admin if he should be an
     * administrator of the tenant's objects. This flag is therefore independent of the right to administer objects of
     * tenants itself.
     */
    @get:Column(name = "super_admin", nullable = false, columnDefinition = "boolean DEFAULT false")
    open var superAdmin: Boolean = false

    @PropertyInfo(i18nKey = "firstName")
    @Field
    @get:Column(length = 255)
    open var firstname: String? = null

    @PropertyInfo(i18nKey = "name")
    @Field
    @get:Column(length = 255)
    open var lastname: String? = null

    /**
     * Optional description of the user.
     */
    @PropertyInfo(i18nKey = "description")
    @Field
    @get:Column(length = 255)
    open var description: String? = null

    /**
     * Die E-Mail Adresse des Benutzers, falls vorhanden.
     */
    @PropertyInfo(i18nKey = "email")
    @Field
    @get:Column(length = 255)
    open var email: String? = null

    /**
     * Key stored in the cookies for the functionality of stay logged in.
     */
    @JsonIgnore
    @field:NoHistory
    @get:Column(name = "stay_logged_in_key", length = 255)
    open var stayLoggedInKey: String? = null

    /**
     * The authentication token is usable for download links of the user (without further login). This is used e. g. for
     * ics download links of the team calendars.
     */
    @PropertyInfo(i18nKey = "user.authenticationToken")
    @JsonIgnore
    @field:NoHistory
    @get:Column(name = "authentication_token", length = 100)
    open var authenticationToken: String? = null

    /**
     * The saltString for giving salt to hashed password.
     */
    @JsonIgnore
    @field:NoHistory
    @get:Column(name = "password_salt", length = 40)
    open var passwordSalt: String? = null

    /**
     * Zeitstempel des letzten erfolgreichen Logins.
     */
    @PropertyInfo(i18nKey = "login.lastLogin")
    @field:NoHistory
    @get:Column
    open var lastLogin: Timestamp? = null

    /**
     * Die Anzahl der erfolglosen Logins. Dieser Wert wird bei dem nächsten erfolgreichen Login auf 0 zurück gesetzt.
     */
    @field:NoHistory
    @get:Column
    open var loginFailures: Int = 0

    @get:Column
    open var locale: Locale? = null

    /**
     * Ensures time zone. If no time zone is given for the user, the configured default time zone is returned.
     * @see Configuration.getDefaultTimeZone
     */
    @PropertyInfo(i18nKey = "timezone")
    private var _timeZoneObject: TimeZone? = null

    val timeZoneObject: TimeZone
        @Transient
        get() =
            _timeZoneObject ?: Configuration.getInstance().defaultTimeZone

    /**
     * For example "Europe/Berlin" if time zone is given otherwise empty string.
     */
    @Column(name = "time_zone")
    open fun getTimeZone(): String? {
        return _timeZoneObject?.id
    }

    open fun setTimeZone(timeZoneId: String?) {
        if (!timeZoneId.isNullOrBlank()) {
            _timeZoneObject = TimeZone.getTimeZone(timeZoneId)
        } else {
            _timeZoneObject = null
        }
    }

    fun setTimeZone(timeZone: TimeZone?) {
        setTimeZoneObject(timeZone)
    }

    fun setTimeZoneObject(timeZone: TimeZone?) {
        _timeZoneObject = timeZone
    }

    /**
     * For example "Europe/Berlin" if time zone is given otherwise empty string.
     */
    val timeZoneDisplayName: String
        @Transient
        get() = timeZoneObject.displayName

    val dateTimeZone: DateTimeZone
        @Transient
        get() = DateTimeZone.forID(timeZoneObject.id)

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
    @PropertyInfo(i18nKey = "dateformat")
    @get:Column(name = "date_format", length = 20)
    open var dateFormat: String? = null

    /**
     * Default excel date format for the user. Examples:
     *
     *  * DD.MM.YYYY: 21.02.2011, German format (day of month first)
     *  * DD/MM/YYYY: 21/02/2011, British and French format (day of month first)
     *  * MM/DD/YYYY: 02/21/2011, American format (month first)
     */
    @PropertyInfo(i18nKey = "dateformat.xls")
    @get:Column(name = "excel_date_format", length = 20)
    open var excelDateFormat: String? = null

    /**
     * 1 - sunday, 2 - monday etc.
     */
    @get:Column(name = "first_day_of_week")
    open var firstDayOfWeek: Int? = null

    @PropertyInfo(i18nKey = "timeNotation")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "time_notation", length = 6)
    open var timeNotation: TimeNotation? = null

    @PropertyInfo(i18nKey = "organization")
    @Field
    @get:Column(length = 255)
    open var organization: String? = null

    /**
     * Eine kommaseparierte Liste mit den Kennungen des/der Telefon(e) des Mitarbeiters an der unterstützten
     * Telefonanlage,  zur Direktwahl aus ProjectForge heraus.
     */
    @PropertyInfo(i18nKey = "user.personalPhoneIdentifiers")
    @get:Column(name = "personal_phone_identifiers", length = 255)
    open var personalPhoneIdentifiers: String? = null

    /**
     * A comma separated list of all personal mobile numbers from which SMS can be send. Those SMS will be assigned to
     * this user. <br></br>
     * This is a feature from the Mobile Enterprise Blogging.
     */
    @PropertyInfo(i18nKey = "user.personalMebMobileNumbers")
    @get:Column(name = "personal_meb_identifiers", length = 255)
    open var personalMebMobileNumbers: String? = null

    @PropertyInfo(i18nKey = "access.rights")
    @get:OneToMany(fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "user")
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
     * Gibt den Vor- und Nachnamen zurück, falls gegeben. Vor- und Nachname sind durch ein Leerzeichen getrennt.
     *
     * @return first name and last name, separated by space.
     */
    @Transient
    fun getFullname(): String {
        val first = this.firstname
        val last = this.lastname
        return if (first.isNullOrBlank()) {
            if (last.isNullOrBlank()) ""
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
            return if (str.isBlank())
                this.username
            else
                "$str (${this.username})"
        }

    @Transient
    override fun getShortDisplayName(): String? {
        return this.username
    }

    /**
     * PLEASE NOTE: Be very careful of modifying this method and don't remove this method! Otherwise
     * data as password hashes may be displayed in log files etc.
     * Returns string containing all fields (except the password) of given user object (via ReflectionToStringBuilder).
     */
    override fun toString(): String {
        val user = createCopyWithoutSecretFields(this)!!
        if (user.hasSecretFieldValues())
            throw InternalErrorException("Security alert in PFUserDO.toString(): secret fields is given but not allowed here!")
        return ToStringUtil.toJsonString(user)
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
     * Security advice: Please use [PFUserDO.createCopyWithoutSecretFields] instead. This method calls simply
     * super and [checkAndFixPassword]. It's also there for checking security issues of callers.
     */
    override fun copyValuesFrom(src: BaseDO<out Serializable>, vararg ignoreFields: String): ModificationStatus {
        val modificationStatus = super.copyValuesFrom(src, *ignoreFields)
        checkAndFixPassword()
        return modificationStatus
    }

    /**
     * If password is not given as "SHA{..." then it will be set to null due to security reasons.
     */
    fun checkAndFixPassword() {
        val pw = this.password
        if (!pw.isNullOrEmpty() &&
                !pw.startsWith("SHA{") &&
                (pw != NOPASSWORD)) {
            this.password = null
            log.error("Password for user '" + this.username + "' is not given SHA encrypted. Ignoring it.")
        }
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

    override fun setTransientAttribute(key: String, value: Any) {
        synchronized(this) {
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
    fun setNoPassword() {
        this.password = NOPASSWORD
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
        return !isDeleted && !this.deactivated
    }

    /**
     * @return If any of the secret fields is given (password, passwordSalt, stayLoggedInKey or authenticationToken).
     */
    fun hasSecretFieldValues(): Boolean {
        return (!this.password.isNullOrEmpty()
                || !this.passwordSalt.isNullOrEmpty()
                || !this.stayLoggedInKey.isNullOrEmpty()
                || !this.authenticationToken.isNullOrEmpty())
    }

    /**
     * Clears any given secret field (password, passwordSalt, stayLoggedInKey or authenticationToken) by setting them to null.
     */
    fun clearSecretFields() {
        this.password = null
        this.passwordSalt = null
        this.stayLoggedInKey = null
        this.authenticationToken = null
    }


    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(PFUserDO::class.java)

        private const val NOPASSWORD = "--- none ---"

        internal const val FIND_BY_USERNAME_AND_STAYLOGGEDINKEY = "PFUserDO_FindByUsernameAndStayLoggedInKey"

        const val FIND_BY_USERNAME = "PFUserDO_FindByUsername"

        internal const val FIND_BY_USERID_AND_AUTHENTICATIONTOKEN = "PFUserDO_FindByUserIdAndAuthenticationToken"

        const val SELECT_ID_MEB_MOBILE_NUMBERS = "PFUserDO_SelectIdMebMobilenumbers"
        /**
         * For detecting the existing of given username in the database for other user than given. Avoids duplicate usernames.
         */
        internal const val FIND_OTHER_USER_BY_USERNAME = "PFUserDO_FindOtherUserByUsername"

        /**
         * @return A copy of the given user without copying the secret fields (password, passwordSalt, stayLoggedInKey or
         * authenticationToken).
         */
        @JvmStatic
        fun createCopyWithoutSecretFields(srcUser: PFUserDO?): PFUserDO? {
            if (srcUser == null)
                return null
            val user = PFUserDO()
            user.copyValuesFrom(srcUser, "password", "passwordSalt", "stayLoggedInKey", "authenticationToken")
            // Paranoia setting (fields shouldn't be copied):
            user.clearSecretFields()
            return user
        }
    }
}

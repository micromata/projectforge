/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

import com.fasterxml.jackson.annotation.JsonIgnore
import de.micromata.genome.db.jpa.history.api.NoHistory
import de.micromata.genome.jpa.metainf.EntityDependencies
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.ObjectUtils
import org.apache.commons.lang3.StringUtils
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.joda.time.DateTimeZone
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.IUserRightId
import org.projectforge.framework.persistence.api.ModificationStatus
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.utils.ReflectionToString
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
@Table(name = "T_PF_USER", uniqueConstraints = [UniqueConstraint(columnNames = ["username"])], indexes = [javax.persistence.Index(name = "idx_fk_t_pf_user_tenant_id", columnList = "tenant_id")])
@EntityDependencies(referencedBy = [TenantDO::class])
class PFUserDO : DefaultBaseDO(), ShortDisplayNameCapable {

    @Transient
    private var attributeMap: MutableMap<String, Any>? = null

    /**
     * @return Returns the username.
     */
    @Field
    @get:Column(length = 255, nullable = false)
    var username: String? = null

    /**
     * JIRA user name (if differ from the ProjectForge's user name) is used e. g. in MEB for creating new issues.
     */
    @Field
    @get:Column(name = "jira_username", length = 100)
    var jiraUsername: String? = null

    /**
     * Encoded password of the user (SHA-1).
     *
     * @return Returns the password.
     */
    @NoHistory
    @JsonIgnore
    @get:Column(length = 50)
    var password: String? = null

    /**
     * @return the lastPasswordChange.
     */
    @get:Column(name = "last_password_change")
    var lastPasswordChange: Date? = null

    @get:Column(name = "last_wlan_password_change")
    var lastWlanPasswordChange: Date? = null

    /**
     * A local user will not be synchronized with any external user management system.
     *
     * @return the localUser
     */
    @get:Column(name = "local_user", nullable = false)
    var localUser: Boolean = false

    /**
     * A restricted user has only the ability to log-in and to change his password. This is useful if ProjectForge runs in
     * master mode for managing an external LDAP system. Then this user is a LDAP user but has no other functionality than
     * change password in the ProjectForge system itself.
     *
     * @return the restrictedUser
     */
    @get:Column(name = "restricted_user", nullable = false)
    var restrictedUser: Boolean = false

    /**
     * A deactivated user has no more system access.
     *
     * @return the deactivated
     */
    @get:Column(nullable = false)
    var deactivated: Boolean = false

    /**
     * A super admin is able to administer tenants. For tenants the user must be assigned to PF_Admin if he should be an
     * administrator of the tenant's objects. This flag is therefore independent of the right to administer objects of
     * tenants itself.

     */
    @get:Column(name = "super_admin", nullable = false, columnDefinition = "boolean DEFAULT false")
    var superAdmin: Boolean = false

    /**
     * Der Vorname des Benutzer.
     *
     * @return Returns the firstname.
     */
    @Field
    @get:Column(length = 255)
    // TODO: Validate.isTrue(firstname == null || firstname.length <= 255, firstname)
    var firstname: String? = null

    /**
     * @return Returns the lastname.
     */
    @Field
    @get:Column(length = 255)
    var lastname: String? = null

    /**
     * @return Returns the description.
     */
    @Field
    @get:Column(length = 255)
    // TODO: Validate.isTrue(description == null || description.length <= 255, description)
    var description: String? = null

    /**
     * Die E-Mail Adresse des Benutzers, falls vorhanden.
     *
     * @return Returns the email.
     */
    @Field
    @get:Column(length = 255)
    // TODO: Validate.isTrue(email == null || email.length <= 255, email)
    var email: String? = null

    /**
     * Key stored in the cookies for the functionality of stay logged in.
     */
    @NoHistory
    @JsonIgnore
    @get:Column(name = "stay_logged_in_key", length = 255)
    var stayLoggedInKey: String? = null

    /**
     * The authentication token is usable for download links of the user (without further login). This is used e. g. for
     * ics download links of the team calendars.
     *
     * @return the authenticationToken
     */
    @NoHistory
    @JsonIgnore
    @get:Column(name = "authentication_token", length = 100)
    var authenticationToken: String? = null

    /**
     * @return the saltString for giving salt to hashed password.
     */
    @NoHistory
    @JsonIgnore
    @get:Column(name = "password_salt", length = 40)
    var passwordSalt: String? = null

    /**
     * Zeitstempel des letzten erfolgreichen Logins.
     *
     * @return Returns the lastLogin.
     * @param lastLogin The lastLogin to set.
     */
    @NoHistory
    @get:Column
    var lastLogin: Timestamp? = null

    /**
     * Die Anzahl der erfolglosen Logins. Dieser Wert wird bei dem nächsten erfolgreichen Login auf 0 zurück gesetzt.
     *
     * @return Returns the loginFailures.
     * @param loginFailures The loginFailures to set.
     */
    @NoHistory
    @get:Column
    var loginFailures: Int = 0

    @get:Column
    var locale: Locale? = null

    private var timeZone: TimeZone? = null

    /**
     * The locale given from the client (e. g. from the browser by the http request). This locale is needed by
     * ThreadLocalUserContext for getting the browser locale if the user's locale is null and the request's locale is not
     * available.
     *
     * @return
     */
    @get:Transient
    var clientLocale: Locale? = null

    /**
     * Default date format for the user. Examples:
     *
     *  * yyyy-MM-dd: 2011-02-21, ISO format.
     *  * dd.MM.yyyy: 21.02.2011, German format (day of month first)
     *  * dd/MM/yyyy: 21/02/2011, British and French format (day of month first)
     *  * MM/dd/yyyy: 02/21/2011, American format (month first)
     *
     *
     * @return
     */
    @get:Column(name = "date_format", length = 20)
    var dateFormat: String? = null

    /**
     * Default excel date format for the user. Examples:
     *
     *  * DD.MM.YYYY: 21.02.2011, German format (day of month first)
     *  * DD/MM/YYYY: 21/02/2011, British and French format (day of month first)
     *  * MM/DD/YYYY: 02/21/2011, American format (month first)
     *
     *
     * @return
     */
    @get:Column(name = "excel_date_format", length = 20)
    var excelDateFormat: String? = null

    /**
     * 0 - sunday, 1 - monday etc.
     *
     * @return the firstDayOfWeek
     */
    @get:Column(name = "first_day_of_week")
    var firstDayOfWeek: Int? = null

    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "time_notation", length = 6)
    var timeNotation: TimeNotation? = null

    @Field
    @get:Column(length = 255)
    var organization: String? = null

    /**
     * Eine kommaseparierte Liste mit den Kennungen des/der Telefon(e) des Mitarbeiters an der unterstützten
     * Telefonanlage,  zur Direktwahl aus ProjectForge heraus.
     */
    @Field
    @get:Column(name = "personal_phone_identifiers", length = 255)
    var personalPhoneIdentifiers: String? = null

    /**
     * A comma separated list of all personal mobile numbers from which SMS can be send. Those SMS will be assigned to
     * this user. <br></br>
     * This is a feature from the Mobile Enterprise Blogging.
     */
    @Field
    @get:Column(name = "personal_meb_identifiers", length = 255)
    var personalMebMobileNumbers: String? = null

    @get:OneToMany(fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "user")
    var rights: MutableSet<UserRightDO>? = HashSet()

    /**
     * If true (default) then the user is highlighted in the human resource planning page if not planned for the actual
     * week.
     *
     * @return the hrPlanning
     */
    @get:Column(name = "hr_planning", nullable = false)
    var hrPlanning = true

    /**
     * LDAP values as key-value-pairs, e. g. gidNumber=1000,uidNumber=1001,homeDirectory="/home/kai",shell="/bin/bash".
     * For handling of string values see [org.apache.commons.csv.writer.CSVWriter]. This field is handled by the
     * ldap package and has no further effect in ProjectForge's core package.
     *
     * @return the ldapValues
     */
    @Field
    @get:Column(name = "ldap_values", length = 4000)
    var ldapValues: String? = null

    /**
     * @return the sshPublicKey
     */
    @Field
    @get:Column(name = "ssh_public_key", length = 4096)
    var sshPublicKey: String? = null

    /**
     * @return For example "Europe/Berlin" if time zone is given otherwise empty string.
     */
    val timeZoneDisplayName: String
        @Transient
        get() = if (timeZone == null) {
            ""
        } else timeZone!!.displayName

    var timeZoneObject: TimeZone
        @Transient
        get() = if (timeZone != null) {
            this.timeZone!!
        } else {
            Configuration.getInstance().defaultTimeZone
        }
        set(timeZone) {
            this.timeZone = timeZone
        }

    val dateTimeZone: DateTimeZone
        @Transient
        get() {
            val timeZone = timeZoneObject
            return DateTimeZone.forID(timeZone.id)
        }

    val userDisplayname: String?
        @Transient
        get() {
            val str = fullname
            return if (StringUtils.isNotBlank(str)) {
                str + " (" + this.username + ")"
            } else this.username
        }

    /**
     * Gibt den Vor- und Nachnamen zurück, falls gegeben. Vor- und Nachname sind durch ein Leerzeichen getrennt.
     *
     * @return String
     */
    /**
     * This setter does nothing. It's only a nop method for deserialization (do not fail on PFUserDO.fullname).
     * @param ignore
     */
    // Do nothing (only for deserialization.
    var fullname: String
        @Transient
        get() {
            val name = StringBuffer()
            if (this.firstname != null) {
                name.append(this.firstname)
                name.append(" ")
            }
            if (this.lastname != null) {
                name.append(this.lastname)
            }

            return name.toString()
        }
        set(ignore) {}

    /**
     * @return The JIRA user name or if not given the user name (assuming that the JIRA user name is same as ProjectForge
     * user name).
     */
    val jiraUsernameOrUsername: String?
        @Transient
        get() = if (StringUtils.isNotEmpty(jiraUsername)) {
            this.jiraUsername
        } else {
            this.username
        }

    val displayUsername: String?
        @Transient
        get() = shortDisplayName

    @Transient
    override fun getShortDisplayName(): String? {
        return this.username
    }

    /**
     * @return For example "Europe/Berlin" if time zone is given otherwise empty string.
     */
    @Column(name = "time_zone")
    fun getTimeZone(): String {
        return if (timeZone == null) {
            ""
        } else timeZone!!.id
    }

    fun setTimeZone(timeZoneId: String) {
        if (StringUtils.isNotBlank(timeZoneId)) {
            setTimeZone(TimeZone.getTimeZone(timeZoneId))
        }
    }

    /**
     * @param timeZone
     * @return this for chaining.
     */
    fun setTimeZone(timeZone: TimeZone): PFUserDO {
        this.timeZone = timeZone
        return this
    }

    /**
     * PLEASE NOTE: Be very careful of modifying this method and don't remove this method! Otherwise
     * data as password hashes may be displayed in log files etc.
     * Returns string containing all fields (except the password) of given user object (via ReflectionToStringBuilder).
     */
    override fun toString(): String {
        return object : ReflectionToString(this) {
            override fun accept(f: java.lang.reflect.Field): Boolean {
                return (super.accept(f)
                        && "password" != f.name
                        && "stayLoggedInKey" != f.name
                        && "passwordSalt" != f.name
                        && "authenticationToken" != f.name)
            }
        }.toString()
    }

    override fun equals(o: Any?): Boolean {
        if (o is PFUserDO) {
            val other = o as PFUserDO?
            return ObjectUtils.equals(this.username, other!!.username)
        }
        return false
    }

    override fun hashCode(): Int {
        return if (this.username == null) 0 else this.username!!.hashCode()
    }

    override fun copyValuesFrom(src: BaseDO<out Serializable>, vararg ignoreFields: String): ModificationStatus {
        var ignoreFields = ignoreFields
        // TODO BUG too much magic.
        ignoreFields = ArrayUtils.add(ignoreFields, "password") as Array<String> // NPE save considering ignoreFields
        val user = src as PFUserDO
        var modificationStatus = AbstractBaseDO.copyValues(user, this, *ignoreFields)
        if (user.password != null) {
            if (!(user.password == password)) {
                modificationStatus = ModificationStatus.MAJOR
            }
            this.password = user.password
            checkAndFixPassword()
        }
        return modificationStatus
    }

    /**
     * If password is not given as "SHA{..." then it will be set to null due to security reasons.
     *
     *
     * TODO DESIGNBUG
     */
    fun checkAndFixPassword() {
        if (StringUtils.isNotEmpty(this.password) &&
                !this.password!!.startsWith("SHA{") && !
                (this.password == NOPASSWORD)) {
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
    fun setNoPassword(): PFUserDO {
        this.password = NOPASSWORD
        return this
    }

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
            if (right.rightIdString == rightId.id == true) {
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
        return (this.password != null || this.passwordSalt != null || this.stayLoggedInKey != null
                || this.authenticationToken != null)
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(PFUserDO::class.java)

        private val NOPASSWORD = "--- none ---"

        /**
         * @return A copy of the given user without copying the secret fields (password, passwordSalt, stayLoggedInKey or
         * authenticationToken).
         */
        fun createCopyWithoutSecretFields(srcUser: PFUserDO): PFUserDO {
            val user = PFUserDO()
            user.copyValuesFrom(srcUser, "password", "passwordSalt", "stayLoggedInKey", "authenticationToken")
            // password is already ignored.
            // WRONG
            user.password = null

            return user
        }
    }
}

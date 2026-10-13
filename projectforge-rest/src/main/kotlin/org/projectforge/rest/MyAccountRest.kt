/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.login.Login
import org.projectforge.business.sipgate.SipgateConfiguration
import org.projectforge.business.user.StayLoggedInTokenDao
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserTokenData
import org.projectforge.business.user.UserTokenType
import org.projectforge.business.user.filter.CookieService
import org.projectforge.business.user.service.UserService
import org.projectforge.common.DateFormatType
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.TimeNotation
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.calendar.BarcodeServicesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.ValidationUtils
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.pub.AuthenticationPublicServicesRest
import org.projectforge.rest.sipgate.SipgateDirectCallService
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * The my-account page of projectforge-next (/next/myAccount): the logged-in user's own data, settings, access
 * tokens and stay-logged-in devices. Layout-free JSON, the successor of the UILayout pages MyAccountPageRest and
 * TokenInfoPageRest. Everything works on the logged-in user's own data only, so there is nothing to check
 * beyond being logged in (the 2FA is registered for MY_ACCOUNT, see ProjectForge2FAInitialization).
 */
@RestController
@RequestMapping("${Rest.URL}/myAccount")
class MyAccountRest {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var authenticationsService: UserAuthenticationsService

    @Autowired
    private lateinit var authenticationPublicServicesRest: AuthenticationPublicServicesRest

    @Autowired
    private lateinit var cookieService: CookieService

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var sipgateConfiguration: SipgateConfiguration

    @Autowired
    private lateinit var stayLoggedInTokenDao: StayLoggedInTokenDao

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var userService: UserService

    /**
     * The editable fields are [firstname] to [sshPublicKey], everything else is for display only and ignored on
     * saving.
     */
    class MyAccountData(
        var firstname: String? = null,
        var lastname: String? = null,
        /** The locale's tag (`de`), or [UserSettingsOptions.DEFAULT_LOCALE]: the browser's locale. */
        var locale: String? = null,
        var dateFormat: String? = null,
        var excelDateFormat: String? = null,
        var timeNotation: TimeNotation? = null,
        /** The time zone's id, e.g. `Europe/Berlin`. */
        var timeZone: String? = null,
        var personalPhoneIdentifiers: String? = null,
        var gpgPublicKey: String? = null,
        var sshPublicKey: String? = null,
        var username: String? = null,
        var mobilePhone: String? = null,
        var lastLoginFormatted: String? = null,
        var lastPasswordChangeFormatted: String? = null,
        var lastWlanPasswordChangeFormatted: String? = null,
        /** False if the WLAN password can't be changed in this installation (no LDAP master): nothing to show. */
        var wlanPasswordChangeSupported: Boolean = false,
        var groups: String? = null,
        var tokens: List<Token>? = null,
        var devices: List<Device>? = null,
    )

    /** An access token of the user: calendar export, DAV or REST client. */
    class Token(val type: UserTokenType, val token: String?, val creationDateFormatted: String?)

    /**
     * A device with a valid stay-logged-in token. The token itself is never sent: only its hash is stored, see
     * [org.projectforge.business.user.StayLoggedInTokenDO].
     */
    class Device(
        val id: Long?,
        val userAgent: String?,
        val lastAccessIp: String?,
        val lastAccessFormatted: String?,
        val createdFormatted: String?,
        /** The device this request comes from. */
        val current: Boolean,
    )

    class Options(
        val locales: List<UserSettingsOptions.Option>,
        val dateFormats: List<UserSettingsOptions.Option>,
        val excelDateFormats: List<UserSettingsOptions.Option>,
        val timeNotations: List<UserSettingsOptions.Option>,
    )

    class InitData(
        val data: MyAccountData,
        /**
         * False for a restricted or a demo user: they may not change their account (UserDao.updateMyAccount) nor
         * renew a token (UserAuthenticationsDao.renewToken). A hint for the page, which then offers neither; the
         * DAOs refuse it anyway.
         */
        val writeAccess: Boolean,
        val options: Options,
        /**
         * What applies while a format is left empty, so the page can name it: the installation's defaults
         * (Configuration), the time notation by the user's language if the installation sets none.
         */
        val defaults: Defaults,
        /** False without a configured Sipgate: the personal phone identifiers are good for nothing then. */
        val phoneIdentifiersSupported: Boolean,
        /**
         * The menu urls of the password pages and of the 2FA setup (not migrated yet, React); the WLAN password
         * null if it can't be changed in this installation. Offered whatever the menu configuration says: an
         * installation hides the password entries of the menu *because* this page links them (see
         * projectforge.menu.visibility.changePassword).
         */
        val changePasswordUrl: String?,
        val changeWlanPasswordUrl: String?,
        val my2FASetupUrl: String?,
    )

    class Defaults(val dateFormat: String, val excelDateFormat: String, val timeNotation: String?)

    class TokenAccess(val userAgent: String?, val ip: String?, val lastAccessFormatted: String?, val counter: Int)

    class TokenInfo(
        val type: UserTokenType,
        /** The most recent accesses with this token since the start of the server, most recent first. */
        val accesses: List<TokenAccess>,
        /**
         * The url of a QR code for a client app (REST client only): it carries a short-lived link to the user's
         * credentials (see AuthenticationPublicServicesRest.createQueryURL).
         */
        val qrCodeUrl: String? = null,
    )

    @AccessChecked("Own user only (logged-in user's data)")
    @GetMapping
    fun getInitialData(request: HttpServletRequest): InitData {
        val user = userDao.find(ThreadLocalUserContext.loggedInUserId, checkAccess = false)!!
        val wlanSupported = Login.getInstance().isWlanPasswordChangeSupported(user)
        return InitData(
            data = createData(request),
            writeAccess = !accessChecker.isRestrictedOrDemoUser,
            options = Options(
                locales = UserSettingsOptions.locales(),
                dateFormats = UserSettingsOptions.dateFormats(),
                excelDateFormats = UserSettingsOptions.excelDateFormats(),
                timeNotations = UserSettingsOptions.timeNotations(),
            ),
            defaults = Defaults(
                dateFormat = Configuration.instance.defaultDateFormat,
                excelDateFormat = Configuration.instance.defaultExcelDateFormat,
                // Without a user: ensureAndGetDefaultTimeNotation would store the default in the context user.
                timeNotation = DateFormats.ensureAndGetDefaultTimeNotation(null)?.let {
                    translate(if (it == TimeNotation.H12) "timeNotation.12" else "timeNotation.24")
                },
            ),
            phoneIdentifiersSupported = sipgateConfiguration.isConfigured(),
            changePasswordUrl = MenuItemDefId.CHANGE_PASSWORD.url,
            changeWlanPasswordUrl = if (wlanSupported) MenuItemDefId.CHANGE_WLAN_PASSWORD.url else null,
            my2FASetupUrl = MenuItemDefId.MY_2FA_SETUP.url,
        )
    }

    /**
     * Saves the editable fields. A too long key is answered with 406 and the field error (it would fail in the
     * database otherwise). The answer is a ResponseAction without target: the page stays and reloads.
     */
    @AccessChecked("Own user only (logged-in user's data)")
    @PostMapping("save")
    fun save(@RequestBody postData: PostData<MyAccountData>): ResponseEntity<ResponseAction> {
        val data = postData.data
        val timeZoneId = data.timeZone?.trim()?.ifEmpty { null }
        if (timeZoneId != null && timeZoneId !in TimeZone.getAvailableIDs()) {
            // TimeZone.getTimeZone would silently take GMT for an unknown id.
            val error = ValidationError.create("validation.error.generic", fieldId = "timeZone")
            return ResponseEntity(ResponseAction(validationErrors = listOf(error)), HttpStatus.NOT_ACCEPTABLE)
        }
        val user = userDao.find(ThreadLocalUserContext.loggedInUserId, checkAccess = false)!!
        user.firstname = data.firstname ?: user.firstname
        user.lastname = data.lastname ?: user.lastname
        user.locale = data.locale?.trim()?.ifEmpty { null }
            ?.takeIf { it != UserSettingsOptions.DEFAULT_LOCALE }
            ?.let { Locale.forLanguageTag(it) }
        user.dateFormat = data.dateFormat?.ifBlank { null }
        user.excelDateFormat = data.excelDateFormat?.ifBlank { null }
        user.timeNotation = data.timeNotation ?: user.timeNotation
        timeZoneId?.let { user.timeZone = TimeZone.getTimeZone(it) }
        user.personalPhoneIdentifiers =
            SipgateDirectCallService.getNormalizedPersonalPhoneIdentifiers(data.personalPhoneIdentifiers)
        user.gpgPublicKey = data.gpgPublicKey
        user.sshPublicKey = data.sshPublicKey
        // A too long key would fail in the database (HTTP 500), so answer it as a field error. Only the key
        // fields: the other values of the user aren't edited here, so their errors aren't the user's to fix.
        ValidationUtils.validateFields(user)
            .filter { it.fieldId == "gpgPublicKey" || it.fieldId == "sshPublicKey" }
            .takeIf { it.isNotEmpty() }
            ?.let { return ResponseEntity(ResponseAction(validationErrors = it), HttpStatus.NOT_ACCEPTABLE) }
        userService.updateMyAccount(user)
        return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
    }

    /**
     * Renews an access token of the user, the old one is invalid from now on.
     *
     * @return The new token, or 400 for a type that isn't an access token of this page.
     */
    @AccessChecked("Own user only (logged-in user's tokens)")
    @PostMapping("renewToken")
    fun renewToken(@RequestParam("token") token: UserTokenType, request: HttpServletRequest): ResponseEntity<*> {
        checkTokenType(token, request)?.let { return it }
        val userId = ThreadLocalUserContext.loggedInUserId!!
        authenticationsService.renewToken(userId, token)
        return ResponseEntity.ok(createToken(token, authenticationsService.getTokenData(userId, token)))
    }

    /**
     * The usage of an access token (accesses since the start of the server) and, for the REST client token, the
     * QR code for setting up a client app.
     */
    @AccessChecked("Own user only (logged-in user's tokens)")
    @GetMapping("tokenInfo")
    fun tokenInfo(@RequestParam("token") token: UserTokenType, request: HttpServletRequest): ResponseEntity<*> {
        checkTokenType(token, request)?.let { return it }
        val userId = ThreadLocalUserContext.loggedInUserId!!
        val accesses = authenticationsService.getUserAccessLogEntries(token, userId)?.sortedList()?.map {
            TokenAccess(
                userAgent = it.userAgent,
                ip = it.ip,
                lastAccessFormatted = PFDateTime.from(it.lastAccess).format(DateFormatType.DATE_TIME_MINUTES),
                counter = it.counter,
            )
        } ?: emptyList()
        val qrCodeUrl = if (token == UserTokenType.REST_CLIENT) {
            BarcodeServicesRest.getBarcodeGetUrl(authenticationPublicServicesRest.createQueryURL())
        } else {
            null
        }
        return ResponseEntity.ok(TokenInfo(token, accesses, qrCodeUrl))
    }

    /**
     * Logs out one device of the user (deletes its stay-logged-in token): it has to log in again once its
     * session has ended. The session of this request is left alone, even if it is the device itself.
     *
     * @return The remaining devices.
     */
    @AccessChecked("Own user only (deletes only the logged-in user's devices)")
    @PostMapping("logoutDevice")
    fun logoutDevice(@RequestParam("id") id: Long, request: HttpServletRequest): List<Device> {
        val userId = ThreadLocalUserContext.loggedInUserId!!
        log.info { "User #$userId logs out stay-logged-in device #$id." }
        stayLoggedInTokenDao.deleteOfUser(userId, id)
        return getDevices(userId, request)
    }

    /**
     * Logs out all devices of the user, see [logoutDevice].
     *
     * @return The remaining devices (none).
     */
    @AccessChecked("Own user only (logged-in user's devices)")
    @PostMapping("logoutAllDevices")
    fun logoutAllDevices(request: HttpServletRequest): List<Device> {
        val userId = ThreadLocalUserContext.loggedInUserId!!
        log.info { "User #$userId logs out all stay-logged-in devices." }
        stayLoggedInTokenDao.deleteAll(userId)
        return getDevices(userId, request)
    }

    private fun createData(request: HttpServletRequest): MyAccountData {
        val userId = ThreadLocalUserContext.loggedInUserId!!
        val user = userDao.find(userId, checkAccess = false)!!
        return MyAccountData(
            firstname = user.firstname,
            lastname = user.lastname,
            locale = user.locale?.toLanguageTag() ?: UserSettingsOptions.DEFAULT_LOCALE,
            dateFormat = user.dateFormat,
            excelDateFormat = user.excelDateFormat,
            timeNotation = user.timeNotation,
            timeZone = user.timeZone.id,
            personalPhoneIdentifiers = user.personalPhoneIdentifiers,
            gpgPublicKey = user.gpgPublicKey,
            sshPublicKey = user.sshPublicKey,
            username = user.username,
            mobilePhone = user.mobilePhone,
            lastLoginFormatted = TimeAgo.getDateAndMessage(user.lastLogin),
            lastPasswordChangeFormatted = user.lastPasswordChange?.let { TimeAgo.getDateAndMessage(it) },
            lastWlanPasswordChangeFormatted = user.lastWlanPasswordChange?.let { TimeAgo.getDateAndMessage(it) },
            wlanPasswordChangeSupported = Login.getInstance().isWlanPasswordChangeSupported(user),
            groups = groupService.getGroupnames(userId),
            tokens = TOKEN_TYPES.map { createToken(it, authenticationsService.getTokenData(userId, it)) },
            devices = getDevices(userId, request),
        )
    }

    private fun getDevices(userId: Long, request: HttpServletRequest): List<Device> {
        val currentToken = cookieService.getStayLoggedInToken(request)
        return stayLoggedInTokenDao.getEntries(userId).map { entry ->
            Device(
                id = entry.id,
                userAgent = entry.userAgent,
                lastAccessIp = entry.lastAccessIp,
                lastAccessFormatted = entry.lastAccess?.let { TimeAgo.getDateAndMessage(it) },
                createdFormatted = entry.created?.let { TimeAgo.getDateAndMessage(it) },
                current = stayLoggedInTokenDao.isTokenOf(entry, currentToken),
            )
        }
    }

    private fun createToken(type: UserTokenType, tokenData: UserTokenData?): Token {
        return Token(type, tokenData?.token, tokenData?.creationDate?.let { TimeAgo.getDateAndMessage(it) })
    }

    /**
     * The token type comes straight from a request parameter, so an unsupported value is a bad request and not
     * an internal error: an exception here would be an attacker triggerable stack trace in the log (including a
     * mail to the developers, see GlobalExceptionRegistry.sendMailToDevelopers). A stay-logged-in token isn't
     * one to display (one per device, only its hash is stored), an authenticator key is only ever shown as the
     * QR code of the 2FA setup.
     */
    private fun checkTokenType(token: UserTokenType, request: HttpServletRequest): ResponseEntity<*>? {
        if (token in TOKEN_TYPES) {
            return null
        }
        log.warn { "Token of type '$token' isn't an access token of the my-account page (denied): ${request.requestURI}" }
        return RestUtils.badRequest("Token of type '$token' isn't supported.")
    }

    companion object {
        /** The access tokens the user sees, copies and renews here, in the order of the page. */
        private val TOKEN_TYPES = listOf(UserTokenType.CALENDAR_REST, UserTokenType.DAV_TOKEN, UserTokenType.REST_CLIENT)
    }
}

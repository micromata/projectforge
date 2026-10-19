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


package org.projectforge.rest.my2fa

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.configuration.DomainService
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserDao
import org.projectforge.common.StringHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.TimeUnit
import org.projectforge.rest.calendar.BarcodeServicesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.pub.next.TwoFactorRequired
import org.projectforge.security.My2FAService
import org.projectforge.security.TimeBased2FA
import org.projectforge.security.WebAuthnServicesRest
import org.projectforge.security.dto.WebAuthnFinishRequest
import org.projectforge.security.webauthn.WebAuthnEntryDao
import org.projectforge.security.webauthn.WebAuthnSupport
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * The 2FA setup of the logged-in user (projectforge-next page `2FASetup`): mobile phone for text messages,
 * authenticator app (Microsoft or Google Authenticator) and WebAuthn keys (Passkeys, FIDO2). Mails are the fall back.
 *
 * Not behind a 2FA shortcut (see ProjectForge2FAInitialization): showing the page is always allowed, but showing the
 * authenticator key and changing any factor require a 2FA not older than [My2FAService.SETUP_2FA_MINUTES] minutes.
 * Without, the endpoints answer like the RestUserFilter does for next clients (HTTP 403 with [TwoFactorRequired]), so
 * next asks for the second factor (via [My2FANextRest]) and repeats the call.
 *
 * The registration of a WebAuthn key starts with [WebAuthnServicesRest.register] (the challenge).
 */
@RestController
@RequestMapping("${Rest.URL}/2FASetup")
class My2FASetupRest {
    @Autowired
    private lateinit var authenticationsService: UserAuthenticationsService

    @Autowired
    private lateinit var domainService: DomainService

    @Autowired
    private lateinit var my2FAService: My2FAService

    @Autowired
    private lateinit var my2FASetupMenuBadge: My2FASetupMenuBadge

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var webAuthnEntryDao: WebAuthnEntryDao

    @Autowired
    private lateinit var webAuthnServicesRest: WebAuthnServicesRest

    @Autowired
    private lateinit var webAuthnSupport: WebAuthnSupport

    class WebAuthnEntryData(
        val id: Long?,
        val displayName: String?,
        val createdFormatted: String?,
        val lastUpdateFormatted: String?,
        val signCount: Long?,
    )

    class InitData(
        val mobilePhone: String?,
        /** False: no text messages in this installation, so the page offers no mobile phone. */
        val smsConfigured: Boolean,
        /** False: the user has neither an authenticator app, a mobile phone (if sms is configured) nor a WebAuthn key. */
        val userConfigured2FA: Boolean,
        val authenticatorConfigured: Boolean,
        val authenticatorKeyCreated: String?,
        /** The last 2FA of the user as time ago, e.g. "5 minutes ago". */
        val lastSuccessful2FA: String?,
        /** True: the last 2FA isn't older than [My2FAService.SETUP_2FA_MINUTES], so changes need no new 2FA. */
        val fresh2FA: Boolean,
        /** The minutes a 2FA is fresh enough for the setup (only for the message shown to the user). */
        val fresh2FAMinutes: Long,
        val webAuthnEntries: List<WebAuthnEntryData>,
    )

    class AuthenticatorKey(
        val key: String,
        /** The url of a QR code of the key, for the authenticator app. */
        val qrCodeUrl: String,
    )

    class MobilePhoneData(var mobilePhone: String? = null)

    class WebAuthnEntryPostData(
        var id: Long? = null,
        var displayName: String? = null,
        /** Only for [webAuthnRegisterFinish]: the response of the authenticator. */
        var webAuthnFinishRequest: WebAuthnFinishRequest? = null,
    )

    @AccessChecked("Own user only (2FA setup of the logged-in user)")
    @GetMapping
    fun getInitialData(): InitData {
        val user = userDao.find(ThreadLocalUserContext.loggedInUserId, checkAccess = false)!!
        val authenticatorKey = authenticationsService.getAuthenticatorToken()
        return InitData(
            mobilePhone = user.mobilePhone,
            smsConfigured = my2FAService.smsConfigured,
            userConfigured2FA = my2FAService.userConfigured2FA,
            authenticatorConfigured = !authenticatorKey.isNullOrBlank(),
            authenticatorKeyCreated = if (authenticatorKey.isNullOrBlank()) null else {
                formatTimeAgo(authenticationsService.getAuthenticatorTokenCreationDate())
            },
            lastSuccessful2FA = My2FAService.getLastSuccessful2FAAsTimeAgo(),
            fresh2FA = my2FAService.checkLastSuccessful2FAForSetup(),
            fresh2FAMinutes = My2FAService.SETUP_2FA_MINUTES,
            webAuthnEntries = webAuthnSupport.allLoggedInUserCredentials.map {
                WebAuthnEntryData(
                    id = it.id,
                    displayName = it.displayName,
                    createdFormatted = it.created?.let { date -> PFDateTime.from(date).format() },
                    lastUpdateFormatted = it.lastUpdate?.let { date -> PFDateTime.from(date).format() },
                    signCount = it.signCount,
                )
            },
        )
    }

    /**
     * The key of the authenticator app and its QR code, for setting up the app (again).
     */
    @AccessChecked("Own user only (2FA setup of the logged-in user)")
    @GetMapping("authenticatorKey")
    fun getAuthenticatorKey(): ResponseEntity<*> {
        require2FA()?.let { return it }
        val key = authenticationsService.getAuthenticatorToken()
        if (key.isNullOrBlank()) {
            return ResponseEntity.notFound().build<Any>()
        }
        val queryURL = TimeBased2FA.standard.getAuthenticatorUrl(
            key,
            ThreadLocalUserContext.loggedInUser!!.username!!,
            domainService.plainDomain ?: "unknown",
        )
        return ResponseEntity.ok(AuthenticatorKey(key, BarcodeServicesRest.getBarcodeGetUrl(queryURL)))
    }

    /**
     * Creates the key of the authenticator app. Fails, if the authenticator app is already configured.
     */
    @AccessChecked("Own user only (2FA setup of the logged-in user)")
    @PostMapping("enableAuthenticatorApp")
    fun enableAuthenticatorApp(): ResponseEntity<*> {
        if (!authenticationsService.getAuthenticatorToken().isNullOrBlank()) {
            log.error { "User tries to enable authenticator app, but authenticator token is already given!" }
            throw IllegalArgumentException("2FA already configured.")
        }
        require2FA()?.let { return it }
        authenticationsService.createNewAuthenticatorToken()
        my2FASetupMenuBadge.refreshUserBadgeCounter()
        return ResponseEntity.ok(ResponseAction())
    }

    /**
     * Deletes the key of the authenticator app. Fails, if the authenticator app isn't configured.
     */
    @AccessChecked("Own user only (2FA setup of the logged-in user)")
    @PostMapping("disableAuthenticatorApp")
    fun disableAuthenticatorApp(): ResponseEntity<*> {
        if (authenticationsService.getAuthenticatorToken().isNullOrBlank()) {
            log.error { "User tries to disable 2FA, but authenticator token isn't given!" }
            throw IllegalArgumentException("2FA not configured.")
        }
        require2FA()?.let { return it }
        authenticationsService.clearAuthenticatorToken()
        my2FASetupMenuBadge.refreshUserBadgeCounter()
        return ResponseEntity.ok(ResponseAction())
    }

    /**
     * Saves the mobile phone as it is. It must be empty or in a valid phone number format (406 otherwise).
     */
    @AccessChecked("Own user only (2FA setup of the logged-in user)")
    @PostMapping("saveMobilePhone")
    fun saveMobilePhone(@RequestBody postData: PostData<MobilePhoneData>): ResponseEntity<*> {
        val mobilePhone = postData.data.mobilePhone?.trim()?.ifEmpty { null }
        if (mobilePhone != null && !StringHelper.checkPhoneNumberFormat(mobilePhone, false)) {
            return validationError(ValidationError(translate("user.mobilePhone.invalidFormat"), "mobilePhone"))
        }
        require2FA()?.let { return it }
        val user = userDao.find(ThreadLocalUserContext.loggedInUserId, checkAccess = false)!!
        user.mobilePhone = mobilePhone
        userDao.update(user, checkAccess = false)
        ThreadLocalUserContext.loggedInUser?.mobilePhone = mobilePhone // The 2FA via text message uses it.
        my2FASetupMenuBadge.refreshUserBadgeCounter()
        return ResponseEntity.ok(ResponseAction())
    }

    /**
     * Last step of the registration of a WebAuthn key: the response of the authenticator to the challenge of
     * [WebAuthnServicesRest.register]. A failed registration is answered with 406 and the message.
     */
    @AccessChecked("Own user only (2FA setup of the logged-in user)")
    @PostMapping("webAuthnRegisterFinish")
    fun webAuthnRegisterFinish(
        request: HttpServletRequest,
        @RequestBody postData: PostData<WebAuthnEntryPostData>,
    ): ResponseEntity<*> {
        val data = postData.data
        val webAuthnFinishRequest = data.webAuthnFinishRequest ?: return ResponseEntity.badRequest().build<Any>()
        require2FA()?.let { return it }
        val displayName = data.displayName?.trim()?.ifEmpty { null } ?: translate("untitled")
        val result = webAuthnServicesRest.doRegisterFinish(request, webAuthnFinishRequest, displayName = displayName)
        if (!result.success) {
            return validationError(ValidationError(translate(result.errorMessage ?: "webauthn.error.process")))
        }
        my2FASetupMenuBadge.refreshUserBadgeCounter()
        return ResponseEntity.ok(ResponseAction())
    }

    /**
     * Renames a WebAuthn key of the user (the display name is the only field of it to change).
     */
    @AccessChecked("Own user only (WebAuthnEntryDao checks the owner)")
    @PostMapping("webAuthnUpdate")
    fun webAuthnUpdate(@RequestBody postData: PostData<WebAuthnEntryPostData>): ResponseEntity<*> {
        val id = postData.data.id ?: return ResponseEntity.badRequest().build<Any>()
        val entry = webAuthnEntryDao.getEntryById(id)
        entry.displayName = postData.data.displayName?.trim()?.ifEmpty { null } ?: translate("untitled")
        webAuthnEntryDao.upsert(entry)
        return ResponseEntity.ok(ResponseAction())
    }

    /**
     * Deletes a WebAuthn key of the user. Like disabling the authenticator app, this requires a fresh 2FA.
     */
    @AccessChecked("Own user only (WebAuthnEntryDao checks the owner)")
    @PostMapping("webAuthnDelete")
    fun webAuthnDelete(@RequestBody postData: PostData<WebAuthnEntryPostData>): ResponseEntity<*> {
        val id = postData.data.id ?: return ResponseEntity.badRequest().build<Any>()
        require2FA()?.let { return it }
        webAuthnEntryDao.delete(id)
        my2FASetupMenuBadge.refreshUserBadgeCounter()
        return ResponseEntity.ok(ResponseAction())
    }

    /**
     * @return null, if the last 2FA is fresh enough for the setup, otherwise the 403 next answers with its 2FA dialog.
     */
    private fun require2FA(): ResponseEntity<TwoFactorRequired>? {
        if (my2FAService.checkLastSuccessful2FAForSetup()) {
            return null
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(TwoFactorRequired(expiryMillis = My2FAService.SETUP_2FA_MINUTES * TimeUnit.MINUTE.millis))
    }

    private fun validationError(error: ValidationError): ResponseEntity<ResponseAction> {
        return ResponseEntity(ResponseAction(validationErrors = listOf(error)), HttpStatus.NOT_ACCEPTABLE)
    }

    private fun formatTimeAgo(date: Date?): String? {
        return date?.let { PFDateTime.from(it).format(withTimeLeftOrAgo = true) }
    }
}

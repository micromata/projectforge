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


package org.projectforge.plugins.datatransfer.restPublic

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.common.FormatterUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.Attachment
import org.projectforge.plugins.datatransfer.DataTransferAreaDO
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * The external access of a data transfer area for the next client (`/next/public/datatransfer/<token>`): plain JSON,
 * no server-side layout. Links to the former React page (`react/public/datatransfer/dynamic/<token>`) are redirected
 * here by OrphanedLinkFilter.
 *
 * Only login, logout and the page's state are here: the file operations are those of [DataTransferPublicServicesRest],
 * which the page calls directly.
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}/datatransfer/next")
class DataTransferPublicNextRest {
    class LoginData(
        var token: String? = null,
        var password: String? = null,
        /** Optional name or e-mail of the external user, for the audit log only. */
        var userInfo: String? = null,
    )

    /**
     * What the page shows. Without a valid login only [loggedIn] and the generic [areaName] are set: an answer for an
     * unknown token must not differ from that for a known one.
     */
    class State(
        var loggedIn: Boolean = false,
        var areaName: String? = null,
        var id: Long? = null,
        var description: String? = null,
        var externalDownloadEnabled: Boolean? = null,
        var externalUploadEnabled: Boolean? = null,
        /** The largest file that can still be uploaded, in bytes: checked by the client before the transfer. */
        var maxUploadSize: Long? = null,
        var maxUploadSizeFormatted: String? = null,
        /** Download all as one ZIP, offered for 1 byte to 1 GB in total (as the legacy page did). */
        var downloadAllAvailable: Boolean = false,
        /** The most a ZIP of selected files may contain, in bytes ([DataTransferPublicServicesRest.MAX_ZIP_SIZE]). */
        var maxZipSize: Long = DataTransferPublicServicesRest.MAX_ZIP_SIZE,
        var maxZipSizeFormatted: String = FormatterUtils.formatBytes(DataTransferPublicServicesRest.MAX_ZIP_SIZE),
        var userInfo: String? = null,
        /** Who uploaded and changed the files is part of them ([DataTransferPublicServicesRest.isUserInfoVisible]). */
        var userInfoVisible: Boolean = false,
        var attachments: List<Attachment>? = null,
    )

    class Message(val message: String)

    @Autowired
    private lateinit var dataTransferPublicServicesRest: DataTransferPublicServicesRest

    @Autowired
    private lateinit var dataTransferPublicSession: DataTransferPublicSession

    @GetMapping("state")
    @AccessChecked("PUBLIC: area data only with a valid login of the token in the session (checkLogin)")
    fun getState(request: HttpServletRequest, @RequestParam("token") token: String?): State {
        if (token.isNullOrBlank()) {
            return notLoggedIn()
        }
        val (area, sessionData) = dataTransferPublicSession.checkLogin(request, accessToken = token)
            ?: return notLoggedIn()
        return createState(request, area, sessionData.userInfo)
    }

    /**
     * Brute force protection (by token and client ip) and the change of the session id are done by
     * [DataTransferPublicSession.login].
     * @return The state on success, otherwise HTTP 400 with the translated reason.
     */
    @PostMapping("login")
    @AccessChecked("PUBLIC: token and password required, brute force protection by DataTransferPublicSession.login")
    fun login(request: HttpServletRequest, @RequestBody loginData: LoginData): ResponseEntity<*> {
        val userInfo = loginData.userInfo?.trim()?.take(255)?.ifBlank { null }
        val result = dataTransferPublicSession.login(request, loginData.token, loginData.password, userInfo)
        result.failedAccessMessage?.let {
            return ResponseEntity.badRequest().body(Message(it))
        }
        return ResponseEntity.ok(createState(request, result.dataTransferArea!!, userInfo))
    }

    @PostMapping("logout")
    @AccessChecked("PUBLIC: invalidates the own session's login only")
    fun logout(request: HttpServletRequest): Message {
        dataTransferPublicSession.logout(request)
        return Message(translate("logout.successful"))
    }

    private fun createState(request: HttpServletRequest, area: DataTransferAreaDO, userInfo: String?): State {
        val dto = dataTransferPublicServicesRest.convert(request, area, userInfo)
        val maxUploadSize = DataTransferAreaDao.calculateMaxUploadFileSize(area)
        return State(
            loggedIn = true,
            areaName = dto.areaName,
            id = dto.id,
            description = dto.description,
            externalDownloadEnabled = dto.externalDownloadEnabled,
            externalUploadEnabled = dto.externalUploadEnabled,
            maxUploadSize = maxUploadSize,
            maxUploadSizeFormatted = FormatterUtils.formatBytes(maxUploadSize),
            downloadAllAvailable = DataTransferPublicServicesRest.isDownloadAllAvailable(area),
            userInfo = dto.userInfo,
            userInfoVisible = DataTransferPublicServicesRest.isUserInfoVisible(request),
            attachments = dto.attachments ?: emptyList(),
        )
    }

    private fun notLoggedIn() = State(areaName = translate("plugins.datatransfer.title.heading"))
}

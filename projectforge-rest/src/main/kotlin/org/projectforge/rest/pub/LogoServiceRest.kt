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

package org.projectforge.rest.pub

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.io.FileUtils
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.common.CanonicalFileUtils
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.io.IOException

private val log = KotlinLogging.logger {}

/**
 * This rest service should be available without login (public).
 */
@RestController
@RequestMapping(Rest.PUBLIC_URL)
class LogoServiceRest {
    @GetMapping(value = arrayOf("logo.jpg"), produces = arrayOf(MediaType.IMAGE_JPEG_VALUE))
    @ResponseBody
    @Throws(IOException::class)
    fun getJpgLogo(): ByteArray {
        return getLogo()
    }

    @GetMapping(value = arrayOf("logo.png"), produces = arrayOf(MediaType.IMAGE_PNG_VALUE))
    @ResponseBody
    @Throws(IOException::class)
    fun getPngLogo(): ByteArray {
        return getLogo()
    }

    @GetMapping(value = arrayOf("logo.gif"), produces = arrayOf(MediaType.IMAGE_GIF_VALUE))
    @ResponseBody
    @Throws(IOException::class)
    fun getGifLogo(): ByteArray {
        return getLogo()
    }

    @AccessChecked("PUBLIC: serves only the configured logo file")
    @GetMapping(value = arrayOf("logoDark.jpg"), produces = arrayOf(MediaType.IMAGE_JPEG_VALUE))
    @ResponseBody
    @Throws(IOException::class)
    fun getJpgLogoDark(): ByteArray {
        return getLogo(dark = true)
    }

    @AccessChecked("PUBLIC: serves only the configured logo file")
    @GetMapping(value = arrayOf("logoDark.png"), produces = arrayOf(MediaType.IMAGE_PNG_VALUE))
    @ResponseBody
    @Throws(IOException::class)
    fun getPngLogoDark(): ByteArray {
        return getLogo(dark = true)
    }

    @AccessChecked("PUBLIC: serves only the configured logo file")
    @GetMapping(value = arrayOf("logoDark.gif"), produces = arrayOf(MediaType.IMAGE_GIF_VALUE))
    @ResponseBody
    @Throws(IOException::class)
    fun getGifLogoDark(): ByteArray {
        return getLogo(dark = true)
    }

    private fun getLogo(dark: Boolean = false): ByteArray {
        val file = if (dark) logoFileDark else logoFile
        if (file == null) {
            val property = if (dark) "projectforge.logoFileDark" else "projectforge.logoFile"
            log.error { "Logo not configured. Can't download logo. You may configure a logo in projectforge.properties via $property=logo.png." }
            throw IOException("Logo not configured. Refer log files for further information.")
        }
        try {
            return FileUtils.readFileToByteArray(file)
        } catch (ex: IOException) {
            log.error { "Error while reading logo file '${CanonicalFileUtils.absolutePath(file)}': ${ex.message}" }
            throw ex
        }
    }

    companion object {
        /**
         * Rest url for downloading the logo if configured. Determined on every call, as `projectforge.logoFile` takes
         * effect without a restart; the version parameter (the file's modification time) lets browsers load a changed
         * logo instead of the cached one.
         */
        @JvmStatic
        val logoUrl: String?
            get() = configurationService.let { versionedUrl(it.syntheticLogoName, it.logoFileObject, it.isLogoFileValid) }

        /** Rest url for the optional dark-mode logo; null when no dark variant is configured. */
        @JvmStatic
        val logoUrlDark: String?
            get() = configurationService.let {
                versionedUrl(it.syntheticLogoNameDark, it.logoFileObjectDark, it.isLogoFileDarkValid)
            }

        /** Configured height in px of the logo in the header; null where the client's default applies. */
        @JvmStatic
        val logoHeight: Int?
            get() = configurationService.logoHeight

        private val logoFile: File?
            get() = configurationService.logoFileObject

        private val logoFileDark: File?
            get() = configurationService.logoFileObjectDark

        private val configurationService: ConfigurationService
            get() = ApplicationContextProvider.getApplicationContext().getBean(ConfigurationService::class.java)

        /** The last unreadable logo logged, so a broken configuration is logged once, not on every status request. */
        @Volatile
        private var loggedUnreadable: File? = null

        private fun versionedUrl(url: String?, file: File?, valid: Boolean): String? {
            if (url.isNullOrBlank() || file == null) {
                return url
            }
            if (!valid) {
                if (loggedUnreadable != file) {
                    loggedUnreadable = file
                    log.error { "Logo file configured but not readable: '${CanonicalFileUtils.absolutePath(file)}'." }
                }
                return url
            }
            return "$url?v=${file.lastModified()}"
        }
    }
}

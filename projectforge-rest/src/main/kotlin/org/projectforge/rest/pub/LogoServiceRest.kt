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

package org.projectforge.rest.pub

import org.apache.commons.io.FileUtils
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.common.CanonicalFileUtils
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.io.IOException


/**
 * This rest service should be available without login (public).
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}")
class LogoServiceRest {
    @Autowired
    private lateinit var configurationService: ConfigurationService

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

    private fun getLogo(): ByteArray {
        if (logoFile == null) {
            log.error("Logo not configured. Can't download logo. You may configure a logo in projectforge.properties via projectforge.logoFile=logo.png.")
            throw IOException("Logo not configured. Refer log files for further information.")
        }
        try {
            return FileUtils.readFileToByteArray(logoFile)
        } catch(ex: IOException) {
            log.error("Error while reading logo file '${CanonicalFileUtils.absolutePath(logoFile)}': ${ex.message}")
            throw ex
        }
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(LogoServiceRest::class.java)
        private var logoUrlInitialized = false
        private var _logoUrl: String? = null
        internal val logoUrl: String? // Rest url for downloading the logo if configured.
            get() {
                if (!logoUrlInitialized) {
                    val configurationService = ApplicationContextProvider.getApplicationContext().getBean(ConfigurationService::class.java)
                    val configuredFile = configurationService.logoFile
                    _logoUrl = createBaseUrl(configuredFile)
                    if (!_logoUrl.isNullOrBlank() && !isLogoFileValid()) {
                        log.error("Logo file configured but not readable: '${CanonicalFileUtils.absolutePath(logoFile)}'.")
                    }
                    logoUrlInitialized = true
                }
                return if (isLogoFileValid()) _logoUrl else null
            }

        private var logoFileInitialized = false
        private var _logoFile: File? = null
        private val logoFile: File?
            get() {
                if (!logoFileInitialized) {
                    val configurationService = ApplicationContextProvider.getApplicationContext().getBean(ConfigurationService::class.java)
                    var filename: String? = configurationService.logoFile
                    if (!filename.isNullOrBlank()) {
                        if (!File(filename).isAbsolute()) {
                            filename = configurationService.getResourceDir() + "/images/" + filename
                        }
                        val file = File(filename)
                        _logoFile = file
                    }
                    logoFileInitialized = true
                }
                return _logoFile
            }

        private fun isLogoFileValid() : Boolean {
            val file = logoFile
            return file != null && file.canRead() && file.isFile
        }

        fun createBaseUrl(logoPath: String?): String? {
            return if (logoPath.isNullOrBlank()) {
                null
            } else if (logoPath.endsWith(".png")) {
                "logo.png"
            } else if (logoPath.endsWith(".jpg") || logoPath.endsWith(".jpeg")) {
                "logo.jpg"
            } else {
                "logo.gif"
            }
        }
    }
}

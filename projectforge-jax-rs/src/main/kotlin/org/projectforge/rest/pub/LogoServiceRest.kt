package org.projectforge.rest.pub

import org.apache.commons.io.FileUtils
import org.projectforge.business.configuration.ConfigurationService
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
        var filename: String? = getLogoPath()
        if (filename.isNullOrBlank()) {
            log.error("Logo not configured. Can't download logo. You may configure a logo in projectforge.properties via projectforge.logoFile=logo.png.")
            throw IOException("Logo not configured. Refer log files for further information.")
        }
        if (File(logoPath).isAbsolute() == true) {
            filename = logoPath
        } else {
            filename = configurationService.getResourceDir() + "/images/" + logoPath
        }
        val file = File(filename)
        if (file.canRead()) {
            log.debug("Use configured logo: $filename")
        } else {
            log.error("Configured logo not found: '$filename'.")
            throw IOException("Configured logo not found. Refer log files for further information.")
        }
        return FileUtils.readFileToByteArray(file)
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(LogoServiceRest::class.java)
        private var logoUrl: String? = "---" // Rest url for downloading the logo if configured.
        private var logoPath: String? = null // Url in local file system to get logo from.

        internal fun getLogoUrl(): String? {
            if (logoUrl == "---") {
                val configurationService = ApplicationContextProvider.getApplicationContext().getBean(ConfigurationService::class.java)
                logoPath = configurationService.logoFile
                if (logoPath.isNullOrBlank()) {
                    logoUrl = null
                } else {

                    logoUrl = createBaseUrl()
                }
            }
            return logoUrl
        }

        private fun getLogoPath(): String? {
            getLogoUrl() // Force initialization
            return logoPath
        }

        fun createBaseUrl(): String? {
            val path = logoPath
            return if (path.isNullOrBlank()) {
                null
            } else if (path.endsWith(".png")) {
                "logo.png"
            } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                "logo.jpg"
            } else {
                "logo.gif"
            }
        }
    }
}

/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.configuration

import mu.KotlinLogging
import org.projectforge.common.FormatterUtils
import org.projectforge.common.MaxFileSizeExceeded
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Repository
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

/**
 * Configuration values persistet in the data base. Please access the configuration parameters via
 * [AbstractConfiguration].
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
open class ConfigurationChecker {
  @Autowired
  private lateinit var environment: Environment

  private var springServletMultipartMaxFileSize: String? = "???"

  @PostConstruct
  private fun postConstruct() {
    springServletMultipartMaxFileSize = environment.getProperty(SPRING_PROPERTY)
  }

  open fun checkConfiguredSpringUploadFileSize(
    fileSize: Int,
    maxFileSize: Long,
    maxFileSizeSpringProperty: String,
    fileName: String? = null,
    throwException: Boolean = true
  ): String? {
    return checkConfiguredSpringUploadFileSize(
      fileSize.toLong(),
      maxFileSize,
      maxFileSizeSpringProperty,
      fileName,
      throwException
    )
  }

  /**
   * Will not check the maximum upload size of multipart ('spring.servlet.multipart.max-file-size').
   * @param fileSize The size of the file the user tries to upload.
   * @param maxFileSize The maximum file size configured as spring property.
   * @param maxFileSizeSpringProperty The name of the spring property defining the maximum file size.
   * @param fileName The name of the file for logging purposes. If not given '<unknown>' is used.
   * @param throwException If true (default) an Exception will be thrown with an short error message and id of the detailed server log message which is also logged.
   * @return The detailed error message with information of how to enlarge the parameters for admins if the check fails or null, if everything was fine.
   */
  open fun checkConfiguredSpringUploadFileSize(
    fileSize: Long,
    maxFileSize: Long,
    maxFileSizeSpringProperty: String,
    fileName: String? = null,
    throwException: Boolean = true
  ): String? {
    if (fileSize <= maxFileSize) {
      return null
    }
    val msgId = "${System.currentTimeMillis()}"
    val ex = MaxFileSizeExceeded(maxFileSize,"See server log file #$msgId). ", fileSize, fileName, maxFileSizeSpringProperty)
    if (throwException) {
      log.warn { "${ex.message} (#$msgId)" }
      throw ex
    }
    return ex.message
  }

  companion object {
    const val SPRING_PROPERTY = "spring.servlet.multipart.max-file-size"
  }
}

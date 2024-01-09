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

package org.projectforge.framework.configuration

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Repository
import org.springframework.util.unit.DataSize
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

/**
 * Configuration values persistet in the data base. Please access the configuration parameters via
 * [Configuration].
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
open class ConfigurationChecker {
  @Autowired
  private lateinit var environment: Environment

  open lateinit var springServletMultipartMaxFileSize: DataSize
    protected set

  @PostConstruct
  private fun postConstruct() {
    val envProp = environment.getProperty(SPRING_PROPERTY)
    if (envProp == null) {
      log.error { "Oups, can't get environment spring variable '$SPRING_PROPERTY'." }
    }
    springServletMultipartMaxFileSize = DataSize.parse(envProp ?: "100MB")
  }

  companion object {
    const val SPRING_PROPERTY = "spring.servlet.multipart.max-file-size"
  }
}

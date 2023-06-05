/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.setup

import mu.KotlinLogging
import org.projectforge.ProjectForgeApp

private val log = KotlinLogging.logger {}

/**
 * Some context information for setup wizard.
 */
class SetupContext {
  enum class DockerMode { SINGLE, STACK }

  var embeddedDatabaseSupported: Boolean = true

  /**
   * STACK: Autostart setting disabled.
   */
  var dockerMode: DockerMode? = null

  val graphicModeSupported: Boolean
    get() = dockerMode == null

  val runAsDockerContainer: Boolean
    get() = dockerMode != null

  /**
   * For running docker containers, the port 8080 isn't customizable.
   */
  val customizedPortSupported: Boolean
    get() = runAsDockerContainer

  init {
    val setupVal = System.getProperty(ProjectForgeApp.PROJECTFORGE_SETUP) ?: ""
    if (setupVal.contains("postgres", ignoreCase = true)) {
      embeddedDatabaseSupported = false
    }
    val dockerVal = System.getProperty(ProjectForgeApp.DOCKER_MODE)
    dockerMode = if (dockerVal.isNullOrBlank()) {
      null
    } else if (dockerVal.contains("stack", true)) {
      DockerMode.STACK
    } else {
      DockerMode.SINGLE
    }
    log.info { "Setup-mode: embeddedDatabaseSupported=$embeddedDatabaseSupported, dockerMode=$dockerMode" }
  }
}

